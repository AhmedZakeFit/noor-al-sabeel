package io.github.ahmedzakefit.noor.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
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
 * Update checks never block or disturb prayer/adhan functions: every failure
 * path is contained and silent in auto mode.
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

    sealed interface DownloadResult {
        /** File verified — ready for the install prompt. */
        data class Verified(val file: File) : DownloadResult
        data class Failed(val reason: String?) : DownloadResult
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
                UpdateParser.Decision.FAILED -> CheckResult.Failed
                UpdateParser.Decision.UP_TO_DATE -> CheckResult.UpToDate
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            failOrUpToDate(manual)
        }
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
     * offered for installation.
     */
    suspend fun downloadApk(
        info: ReleaseInfo,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): DownloadResult = withContext(Dispatchers.IO) {
        var target: File? = null
        try {
            if (!UpdateParser.isAllowedDownloadUrl(info.apkUrl)) {
                return@withContext DownloadResult.Failed("disallowed source")
            }
            // The published checksum is fetched first — without it there is
            // nothing to verify against, so refuse rather than trust blindly.
            val publishedSha256: String = info.sha256Url?.let { url ->
                val checksumOutcome = Http.call(
                    url = url,
                    connectTimeoutMs = 6_000,
                    readTimeoutMs = 10_000,
                ) as HttpOutcome.Response
                if (checksumOutcome.code != 200) null else checksumOutcome.body
            }?.let { UpdateParser.parseChecksumFile(it) } ?: return@withContext DownloadResult.Failed("checksum unavailable")

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
                if (code != 200) return@withContext DownloadResult.Failed("HTTP $code")
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
                if (read == 0L) return@withContext DownloadResult.Failed("empty download")

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
                    DownloadResult.Failed(verdict.name)
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: CancellationException) {
            target?.let { runCatching { it.delete() } }
            throw e
        } catch (e: Throwable) {
            target?.let { runCatching { it.delete() } }
            DownloadResult.Failed(e.message)
        }
    }

    /**
     * Opens the system package-installer flow for the verified file. The
     * user's approval (and the one-time "install unknown apps" grant for this
     * app on Android 8+) is required — there is no silent path and none is
     * attempted.
     */
    fun installPrompt(file: File): Boolean = try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (_: Throwable) {
        false
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
            "https://api.github.com/repos/AhmedZakeFit/noor-al-sabeel/releases/latest"
        internal const val LATEST_RELEASE_PAGE_URL =
            "https://github.com/AhmedZakeFit/noor-al-sabeel/releases/latest"
    }
}
