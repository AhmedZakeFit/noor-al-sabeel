package io.github.ahmedzakefit.noor.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import io.github.ahmedzakefit.noor.data.UpdateParser.ReleaseInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * In-app update delivery on top of the project's existing GitHub Releases
 * distribution — no extra server, no extra dependency, nothing installed
 * without explicit user consent.
 *
 * Flow: check /releases/latest against the installed version (auto once per
 * app open, silent on failure; manual re-check from settings with visible
 * feedback) -> show version + changelog + postpone -> optional download into
 * the app cache -> verify SHA-256 against the published checksum asset AND
 * the archive's package name / versionCode / signing certificate against the
 * installed app -> only then offer the standard Android APK install prompt
 * (the user must approve "install unknown apps" for this app once; the system
 * refuses mismatched signatures — we simply detect it earlier).
 *
 * Version discovery has TWO paths so a rate-limited API can never hide an
 * existing update (proven failure mode on shared mobile IPs, which share the
 * 60 req/h unauthenticated quota):
 *  1. the plain website redirect github.com/…/releases/latest (not
 *     rate-limited) resolves the latest version;
 *  2. the API (api.github.com) provides the full release document with
 *     changelog and exact asset URLs. When the API is unusable (403/429 rate
 *     limit, 5xx, offline) but the redirect already proved a NEWER version
 *     exists, the release info is built from the deterministic asset URLs
 *     the release workflow always publishes (versioned pair preferred, stable
 *     pair fallback) and confirmed with cheap HEAD probes against github.com.
 *     The downloaded APK is then verified against the published .sha256
 *     asset exactly as in the API path — no security check is skipped.
 *
 * Update checks never block or disturb prayer/adhan functions: every failure
 * path is contained and silent in auto mode (except a redirect-proven newer
 * version, which must surface).
 */
class UpdateManager(
    private val context: Context,
    private val repository: SettingsRepository,
) {

    sealed interface CheckResult {
        data object UpToDate : CheckResult
        data class Available(val info: ReleaseInfo) : CheckResult
        data object Failed : CheckResult
    }

    /** Why a download/verification/install attempt failed — user-visible. */
    enum class DownloadFailure {
        /** Network error while fetching the checksum or the APK. */
        NETWORK,
        /** The server answered a non-200 code. */
        HTTP_ERROR,
        /** The download produced zero bytes. */
        EMPTY_DOWNLOAD,
        /** The published .sha256 asset could not be fetched/parsed. */
        CHECKSUM_UNAVAILABLE,
        /** Downloaded bytes do not match the published checksum. */
        CHECKSUM_MISMATCH,
        /** The archive is a different app. */
        PACKAGE_MISMATCH,
        /** The archive is not newer than the installed versionCode. */
        NOT_NEWER,
        /** The archive is signed with a different certificate. */
        SIGNATURE_MISMATCH,
        /** The URL is not on the release-asset allowlist. */
        DISALLOWED_SOURCE,
    }

    sealed interface DownloadResult {
        /** File verified — ready for the install prompt. */
        data class Verified(val file: File) : DownloadResult
        data class Failed(
            val reason: DownloadFailure,
            /** Human-readable detail for logs, e.g. "HTTP 403" or the exception message. */
            val detail: String? = null,
        ) : DownloadResult
    }

    /** Outcome of trying to hand a verified APK to the system installer. */
    sealed interface InstallPromptResult {
        /** The system installer activity was started. */
        data object Opened : InstallPromptResult
        /** Could not open the installer (no handler, provider failure, …). */
        data class Failed(val detail: String? = null) : InstallPromptResult
    }

    private val packageManager: PackageManager get() = context.packageManager

    fun installedVersionName(): String =
        runCatching { packageManager.getPackageInfo(context.packageName, 0).versionName ?: "" }
            .getOrDefault("")

    fun installedVersionCode(): Long =
        runCatching {
            val info = packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
        }.getOrDefault(0L)

    /** SHA-256 hex of the certificate this very app was signed with. */
    fun installedSignerSha256(): String? = runCatching {
        val info = packageManager.getPackageInfo(context.packageName, signingFlags())
        val bytes = firstSignatureBytes(info) ?: return@runCatching null
        sha256Hex(bytes)
    }.getOrNull()

    suspend fun check(manual: Boolean): CheckResult {
        // Rate-limit-aware pre-check: the plain website redirect resolves the
        // latest version without the 60 req/h API quota, so up-to-date
        // clients (the overwhelming majority of checks) never touch the API.
        // When it resolves a NEWER version it is remembered — that fact is
        // the input of the API-free fallback below.
        var redirectProvedNewer: String? = null
        try {
            val latestVersion = Http.finalRedirectUrl(LATEST_RELEASE_PAGE_URL)
                ?.let { UpdateParser.versionFromRedirectUrl(it) }
            if (latestVersion != null) {
                if (!UpdateParser.isNewer(latestVersion, installedVersionName())) {
                    return CheckResult.UpToDate
                }
                if (!manual && repository.updatePostponedVersion.first() == latestVersion) {
                    // User postponed exactly this version — no nagging.
                    return CheckResult.UpToDate
                }
                redirectProvedNewer = latestVersion
            }
            // Unknown/unreachable redirect or a genuinely newer version:
            // fall through and fetch the full release document.
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            // Pre-check is best-effort; the API path decides.
        }

        return try {
            val outcome = Http.call(
                url = LATEST_RELEASE_URL,
                connectTimeoutMs = 6_000,
                readTimeoutMs = 12_000,
                extraHeaders = mapOf("Accept" to "application/vnd.github+json"),
            ) as HttpOutcome.Response
            // Non-200 or a failed body both collapse to "no document", which
            // decide() maps to FAILED (manual) / silent UP_TO_DATE (auto).
            val body = if (outcome.code == 200) outcome.body else null
            val postponed = repository.updatePostponedVersion.first()
            val (decision, info) = UpdateParser.decide(body, installedVersionName(), postponed, manual)
            when (decision) {
                UpdateParser.Decision.AVAILABLE ->
                    CheckResult.Available(info ?: return CheckResult.UpToDate)
                else -> {
                    // The API produced no usable document (offline, 403/429
                    // rate limit, 5xx) — but the redirect already PROVED a
                    // newer version exists. Build the release from the
                    // deterministic asset URLs so a rate-limited IP can never
                    // hide an existing update. When the API DID answer and
                    // said "up to date", it is trusted as before.
                    if (body == null && redirectProvedNewer != null) {
                        return releaseFromDeterministicAssets(redirectProvedNewer)
                            ?: if (decision == UpdateParser.Decision.FAILED) {
                                CheckResult.Failed
                            } else {
                                CheckResult.UpToDate
                            }
                    }
                    if (decision == UpdateParser.Decision.FAILED) CheckResult.Failed
                    else CheckResult.UpToDate
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            // Even the API call itself failed (connection reset, DNS, …):
            // the deterministic-asset path still applies when the redirect
            // proved a newer version. (No runCatching here — cancellation
            // must propagate; the helper contains its own per-candidate
            // error handling.)
            if (redirectProvedNewer != null) {
                return releaseFromDeterministicAssets(redirectProvedNewer)
                    ?: failOrUpToDate(manual)
            }
            failOrUpToDate(manual)
        }
    }

    /**
     * API-free fallback: HEAD-probe the deterministic versioned/stable asset
     * pairs published under the resolved tag and build a ReleaseInfo from
     * the first pair whose APK and checksum both answer 200. Security is
     * unchanged: https + github.com allowlist only, and the download is
     * still verified against the published checksum asset before any
     * install prompt. Null when no pair is reachable.
     */
    private suspend fun releaseFromDeterministicAssets(version: String): CheckResult? {
        val candidates = UpdateParser.fallbackAssetUrls(version) ?: return null
        for ((apkUrl, sha256Url) in candidates) {
            if (!UpdateParser.isAllowedDownloadUrl(apkUrl)) continue
            if (!UpdateParser.isAllowedDownloadUrl(sha256Url)) continue
            try {
                val shaHead = Http.head(sha256Url)
                if (shaHead.code != 200) continue
                val apkHead = Http.head(apkUrl)
                if (apkHead.code != 200) continue
                return CheckResult.Available(
                    ReleaseInfo(
                        tag = "v$version",
                        versionName = version,
                        apkUrl = apkUrl,
                        apkSizeBytes = apkHead.contentLength.takeIf { it > 0 } ?: 0L,
                        sha256Url = sha256Url,
                        // Changelog needs the API; the dialog already falls
                        // back to the source note when the summary is blank.
                        changelogSummary = "",
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // Try the next candidate pair.
            }
        }
        return null
    }

    private fun failOrUpToDate(manual: Boolean): CheckResult =
        if (manual) CheckResult.Failed else CheckResult.UpToDate

    suspend fun postpone(info: ReleaseInfo) {
        repository.setUpdatePostponedVersion(info.versionName)
    }

    /**
     * Downloads the APK into the app's private cache (no permissions needed),
     * hashing while streaming, then verifies checksum + package + version +
     * signature. Any mismatch deletes the file — a bad update is never
     * offered for installation. The failure reason is typed so the UI can
     * tell the user WHAT failed, not just that something did.
     */
    suspend fun downloadApk(
        info: ReleaseInfo,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): DownloadResult = withContext(Dispatchers.IO) {
        var target: File? = null
        try {
            if (!UpdateParser.isAllowedDownloadUrl(info.apkUrl)) {
                return@withContext DownloadResult.Failed(DownloadFailure.DISALLOWED_SOURCE)
            }
            // The published checksum is fetched first — without it there is
            // nothing to verify against, so refuse rather than trust blindly.
            val publishedSha256: String = try {
                info.sha256Url?.let { url ->
                    val checksumOutcome = Http.call(
                        url = url,
                        connectTimeoutMs = 6_000,
                        readTimeoutMs = 10_000,
                    ) as HttpOutcome.Response
                    if (checksumOutcome.code != 200) null else checksumOutcome.body
                }?.let { UpdateParser.parseChecksumFile(it) }
                    ?: return@withContext DownloadResult.Failed(DownloadFailure.CHECKSUM_UNAVAILABLE)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                return@withContext DownloadResult.Failed(DownloadFailure.NETWORK, "checksum fetch failed")
            }

            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            // Clear older cached updates — only one staged file at a time.
            dir.listFiles()?.forEach { runCatching { it.delete() } }
            target = File(dir, "noor-al-sabeel-${info.versionName}.apk")

            val conn = java.net.URL(info.apkUrl).openConnection() as java.net.HttpURLConnection
            try {
                conn.connectTimeout = 10_000
                conn.readTimeout = 30_000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", Http.USER_AGENT)
                val code = conn.responseCode
                if (code != 200) {
                    return@withContext DownloadResult.Failed(DownloadFailure.HTTP_ERROR, "HTTP $code")
                }
                val total = if (conn.contentLengthLong > 0) conn.contentLengthLong else info.apkSizeBytes
                val digest = MessageDigest.getInstance("SHA-256")
                var read = 0L
                conn.inputStream.use { input ->
                    target.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            digest.update(buf, 0, n)
                            out.write(buf, 0, n)
                            read += n
                            onProgress(read, total)
                            // Abandon promptly when the dialog closes mid-download.
                            coroutineContext.ensureActive()
                        }
                    }
                }
                if (read == 0L) return@withContext DownloadResult.Failed(DownloadFailure.EMPTY_DOWNLOAD)

                val actualSha256 = sha256Hex(digest.digest())
                val archive = archiveFacts(target)
                val verdict = UpdateVerifier.verdict(
                    publishedSha256 = publishedSha256,
                    actualSha256 = actualSha256,
                    archive = archive,
                    installedPackageName = context.packageName,
                    installedVersionCode = installedVersionCode(),
                    installedSignerSha256 = installedSignerSha256() ?: "",
                )
                if (verdict == UpdateVerifier.Verdict.OK) {
                    DownloadResult.Verified(target)
                } else {
                    runCatching { target.delete() }
                    DownloadResult.Failed(
                        when (verdict) {
                            UpdateVerifier.Verdict.CHECKSUM_UNKNOWN -> DownloadFailure.CHECKSUM_UNAVAILABLE
                            UpdateVerifier.Verdict.CHECKSUM_MISMATCH -> DownloadFailure.CHECKSUM_MISMATCH
                            UpdateVerifier.Verdict.PACKAGE_MISMATCH -> DownloadFailure.PACKAGE_MISMATCH
                            UpdateVerifier.Verdict.NOT_NEWER -> DownloadFailure.NOT_NEWER
                            UpdateVerifier.Verdict.SIGNATURE_MISMATCH -> DownloadFailure.SIGNATURE_MISMATCH
                            UpdateVerifier.Verdict.OK -> DownloadFailure.NETWORK // unreachable
                        },
                        verdict.name,
                    )
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: CancellationException) {
            target?.let { runCatching { it.delete() } }
            throw e
        } catch (e: Throwable) {
            target?.let { runCatching { it.delete() } }
            DownloadResult.Failed(DownloadFailure.NETWORK, e.message)
        }
    }

    /**
     * Opens the system package-installer flow for the verified file. The
     * user's approval (and the one-time "install unknown apps" grant for this
     * app on Android 8+) is required — there is no silent path and none is
     * attempted. The outcome is REPORTED (not swallowed) so the UI can show
     * what happened and offer retry / the permission settings screen.
     */
    fun installPrompt(file: File): InstallPromptResult = try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        InstallPromptResult.Opened
    } catch (e: Throwable) {
        InstallPromptResult.Failed(e.message ?: e.javaClass.simpleName)
    }

    /**
     * Whether this app currently holds the Android 8+ "install unknown apps"
     * grant. Null on Android < 8, where the legacy "unknown sources" setting
     * governs the install flow instead.
     */
    fun canRequestInstalls(): Boolean? =
        if (Build.VERSION.SDK_INT >= 26) packageManager.canRequestPackageInstalls() else null

    /**
     * Opens this app's page in the system "install unknown apps" settings so
     * the user can grant the one-time permission themselves. Purely
     * user-driven — nothing is granted silently.
     */
    fun openInstallPermissionSettings(): InstallPromptResult = try {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
        InstallPromptResult.Opened
    } catch (e: Throwable) {
        InstallPromptResult.Failed(e.message ?: e.javaClass.simpleName)
    }

    /** Package facts of a downloaded APK, as far as the platform exposes them. */
    private fun archiveFacts(apk: File): UpdateVerifier.ArchiveFacts = runCatching {
        val info: PackageInfo? = packageManager.getPackageArchiveInfo(apk.absolutePath, signingFlags())
        UpdateVerifier.ArchiveFacts(
            packageName = info?.packageName,
            versionCode = info?.let {
                if (Build.VERSION.SDK_INT >= 28) it.longVersionCode
                else @Suppress("DEPRECATION") it.versionCode.toLong()
            },
            signerSha256 = info?.let { firstSignatureBytes(it) }?.let { sha256Hex(it) },
        )
    }.getOrDefault(UpdateVerifier.ArchiveFacts(null, null, null))

    private fun signingFlags(): Int =
        if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES
        else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES

    /** Signer certificate bytes of a parsed package (installed or archive). */
    private fun firstSignatureBytes(info: PackageInfo): ByteArray? =
        if (Build.VERSION.SDK_INT >= 28) {
            info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
        } else {
            @Suppress("DEPRECATION") info.signatures?.firstOrNull()?.toByteArray()
        }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    companion object {
        internal const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/${UpdateParser.REPO_PATH}/releases/latest"
        internal const val LATEST_RELEASE_PAGE_URL =
            "https://github.com/${UpdateParser.REPO_PATH}/releases/latest"
    }
}
