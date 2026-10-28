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
import java.io.FileOutputStream
import java.io.IOException
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
        /** The connection kept dropping before the whole body arrived. */
        TRUNCATED,
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
                // A checksum asset that HEAD-probes without a real body is
                // not committed yet (mid-upload/half-published release).
                if (shaHead.code != 200 || shaHead.contentLength <= 0) continue
                val apkHead = Http.head(apkUrl)
                // Same rule for the APK: no content length, no complete
                // asset — offering it would download truncated bytes that
                // can only fail verification.
                if (apkHead.code != 200 || apkHead.contentLength <= 0) continue
                return CheckResult.Available(
                    ReleaseInfo(
                        tag = "v$version",
                        versionName = version,
                        apkUrl = apkUrl,
                        apkSizeBytes = apkHead.contentLength,
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
     *
     * v1.0.17 TOCTOU hardening (root cause of the "SHA-256 mismatch" wave on
     * v1.0.16): the published checksum and the APK are fetched in two separate
     * HTTP requests, and the v1.0.16 release was re-published with different
     * bytes ~19 minutes after its first publish — users who fetched the
     * checksum before the asset swap and the APK after it (or vice versa)
     * verified two DIFFERENT builds against each other and saw a false
     * mismatch. The fix is [UpdateDownloadPolicy]: on a checksum mismatch or
     * an unavailable checksum, re-fetch the published checksum once and, only
     * if it still disagrees with the downloaded bytes, re-download; a final
     * disagreement still fails. Verification is NOT weakened — the bytes on
     * disk must equal the checksum the release publishes NOW, byte for byte,
     * and every package/version/signature check still applies.
     */
    suspend fun downloadApk(
        info: ReleaseInfo,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): DownloadResult = withContext(Dispatchers.IO) {
        if (!UpdateParser.isAllowedDownloadUrl(info.apkUrl)) {
            return@withContext DownloadResult.Failed(DownloadFailure.DISALLOWED_SOURCE)
        }
        if (!info.sha256Url.isNullOrEmpty() && !UpdateParser.isAllowedDownloadUrl(info.sha256Url!!)) {
            return@withContext DownloadResult.Failed(DownloadFailure.DISALLOWED_SOURCE, "checksum url not allowlisted")
        }

        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        // Clear older cached updates — only one staged file at a time.
        dir.listFiles()?.forEach { runCatching { it.delete() } }
        val target = File(dir, "noor-al-sabeel-${info.versionName}.apk")

        var attempts = 0
        // Hash of the file already on disk from an earlier attempt of THIS
        // call (the file is only re-downloaded when it provably disagrees).
        var downloadedSha256: String? = null
        var verdict: UpdateVerifier.Verdict = UpdateVerifier.Verdict.CHECKSUM_UNKNOWN
        var publishedSha256: String? = null

        try {
            while (true) {
                attempts++

                // 1) The published checksum is always fetched first — without
                //    it there is nothing to verify against. On a retry this
                //    re-fetch picks up a release re-published mid-flight.
                publishedSha256 = fetchPublishedSha256(info)
                if (publishedSha256 == null) {
                    verdict = UpdateVerifier.Verdict.CHECKSUM_UNKNOWN
                    if (!UpdateDownloadPolicy.shouldRetry(verdict, attempts)) break
                    continue
                }

                // 2) A file downloaded by an earlier attempt of this call may
                //    already satisfy the (re-)fetched checksum — the release
                //    was swapped between the two requests of that attempt. No
                //    need to re-download 24 MB to prove the same fact twice.
                if (downloadedSha256 != null && downloadedSha256 == publishedSha256) {
                    verdict = UpdateVerifier.verdict(
                        publishedSha256 = publishedSha256,
                        actualSha256 = downloadedSha256,
                        archive = archiveFacts(target),
                        installedPackageName = context.packageName,
                        installedVersionCode = installedVersionCode(),
                        installedSignerSha256 = installedSignerSha256() ?: "",
                    )
                    if (verdict == UpdateVerifier.Verdict.OK) break
                    if (!UpdateDownloadPolicy.shouldRetry(verdict, attempts)) break
                    continue
                }

                // 3) Download (first attempt, or the disk file is provably not
                //    what the release publishes now).
                when (val fetch = downloadToFile(info, target, onProgress)) {
                    is FetchOutcome.Failed -> {
                        // A partial write may be on disk; never stage it.
                        runCatching { target.delete() }
                        return@withContext DownloadResult.Failed(fetch.failure, fetch.detail)
                    }
                    is FetchOutcome.Swapped -> {
                        // The server itself reports a size that differs from
                        //    the release document — a re-publish mid-swap. Do
                        //    not hash garbage; retry (policy decides when to give up).
                        verdict = UpdateVerifier.Verdict.CHECKSUM_MISMATCH
                        if (!UpdateDownloadPolicy.shouldRetry(verdict, attempts)) {
                            runCatching { target.delete() }
                            return@withContext DownloadResult.Failed(
                                DownloadFailure.CHECKSUM_MISMATCH,
                                "served size differs from release asset size",
                            )
                        }
                        continue
                    }
                    is FetchOutcome.Truncated -> {
                        // The connection dropped mid-body (the standard mobile-
                        //    network failure for a 24 MB download). This is a
                        //    NETWORK problem, not a checksum problem — and
                        //    crucially it must never be hashed and reported as
                        //    a "SHA-256 mismatch" (the v1.0.16-era message that
                        //    made a flaky connection look like a broken
                        //    release). The leg loop in downloadToFile already
                        //    exhausted its resume budget; give the heal policy
                        //    its pass, then fail with the honest reason.
                        runCatching { target.delete() }
                        verdict = UpdateVerifier.Verdict.CHECKSUM_UNKNOWN
                        if (!UpdateDownloadPolicy.shouldRetry(verdict, attempts)) {
                            return@withContext DownloadResult.Failed(
                                DownloadFailure.TRUNCATED,
                                fetch.detail,
                            )
                        }
                        continue
                    }
                    is FetchOutcome.Downloaded -> downloadedSha256 = fetch.sha256
                }

                // 4) Verify the bytes against the currently published checksum.
                verdict = UpdateVerifier.verdict(
                    publishedSha256 = publishedSha256,
                    actualSha256 = downloadedSha256,
                    archive = archiveFacts(target),
                    installedPackageName = context.packageName,
                    installedVersionCode = installedVersionCode(),
                    installedSignerSha256 = installedSignerSha256() ?: "",
                )
                if (verdict == UpdateVerifier.Verdict.OK) break
                if (!UpdateDownloadPolicy.shouldRetry(verdict, attempts)) break
                // else: loop once more — re-fetch the checksum first; the
                // re-download happens only if it still does not match.
            }

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
                    verifyDetail(verdict, publishedSha256, downloadedSha256),
                )
            }
        } catch (e: CancellationException) {
            target.let { runCatching { it.delete() } }
            throw e
        } catch (e: Throwable) {
            target.let { runCatching { it.delete() } }
            DownloadResult.Failed(DownloadFailure.NETWORK, e.message)
        }
    }

    /**
     * Diagnostic line for a failed verify — the verdict plus, for a checksum
     * mismatch, the two hashes it compared (both are public values from the
     * release; prefixes are enough to tell builds apart). Shown as the small
     * detail line under the localized dialog message so a report says WHAT
     * disagreed, not just that something did.
     */
    private fun verifyDetail(
        verdict: UpdateVerifier.Verdict,
        published: String?,
        computed: String?,
    ): String = buildString {
        append(verdict.name)
        if (verdict == UpdateVerifier.Verdict.CHECKSUM_MISMATCH) {
            append(": computed ")
            append(computed?.take(16) ?: "?")
            append("… vs published ")
            append(published?.take(16) ?: "?")
            append("…")
        }
    }

    /** Outcome of a single checksum fetch. Null-checkable wrapper kept out of the public API. */
    private suspend fun fetchPublishedSha256(info: ReleaseInfo): String? = try {
        info.sha256Url?.let { url ->
            val checksumOutcome = Http.call(
                url = url,
                connectTimeoutMs = 6_000,
                readTimeoutMs = 10_000,
            ) as HttpOutcome.Response
            if (checksumOutcome.code != 200) null else checksumOutcome.body
        }?.let { UpdateParser.parseChecksumFile(it) }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Throwable) {
        null
    }

    /**
     * One streamed download of [info] into [target]; hashes while writing.
     *
     * v1.0.19: the transfer is RESUMABLE. A connection that ends before the
     * expected body size — the routine mobile-network failure for a 24 MB
     * download — is no longer hashed and reported as a checksum mismatch;
     * that exact misclassification is why the v1.0.16-era "SHA-256
     * mismatch" message kept reappearing on flaky networks even against a
     * perfectly consistent release. Instead the download re-opens the URL
     * with an HTTP `Range: bytes=<read>-` request, APPENDS to the partial
     * file and CONTINUES the running SHA-256 digest (a resume replays no
     * bytes and wastes no data allowance), for up to
     * [UpdateDownloadPolicy.MAX_TRUNCATION_LEGS] bodies in one attempt.
     * A server that ignores Range and restarts the body from zero resets
     * the file and the digest cleanly. When the legs are exhausted the
     * outcome is [FetchOutcome.Truncated] — a network failure with an
     * honest, detailed message — and the caller's policy decides whether
     * a fresh attempt is worth it. The swap check and every security gate
     * are unchanged.
     */
    private suspend fun downloadToFile(
        info: ReleaseInfo,
        target: File,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit,
    ): FetchOutcome = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        var read = 0L
        var legs = 0
        // Expected total: the release document's size when known, refined by
        // the server's own Content-Length (full, or remaining for 206 legs).
        var expectedTotal = info.apkSizeBytes

        while (true) {
            val resuming = read > 0
            val conn = try {
                Http.openWithRedirects(
                    initialUrl = info.apkUrl,
                    method = "GET",
                    connectTimeoutMs = 15_000,
                    readTimeoutMs = 60_000,
                    urlValidator = UpdateParser::isAllowedDownloadUrl,
                    headers = if (resuming) mapOf("Range" to "bytes=$read-") else emptyMap(),
                )
            } catch (e: SecurityException) {
                return@withContext FetchOutcome.Failed(DownloadFailure.DISALLOWED_SOURCE, e.message)
            } catch (e: Throwable) {
                return@withContext FetchOutcome.Failed(DownloadFailure.NETWORK, e.message)
            }
            try {
                val code = conn.responseCode
                if (code != 200 && code != 206) {
                    return@withContext FetchOutcome.Failed(DownloadFailure.HTTP_ERROR, "HTTP $code")
                }
                if (code == 200) {
                    if (resuming) {
                        // Server ignored the Range request and restarted the
                        // body from byte zero — reset the partial file and the
                        // digest so the copy on disk stays exactly the bytes
                        // that are hashed.
                        read = 0L
                        digest.reset()
                        runCatching { target.delete() }
                    }
                    val servedLen = conn.contentLengthLong
                    // The release document says how many bytes this asset
                    // has. When the server disagrees, the served file is NOT
                    // the release the check resolved (a re-published release
                    // mid-asset-swap) — stop before hashing, retry per policy.
                    if (UpdateDownloadPolicy.sizeProvesAssetSwap(servedLen, info.apkSizeBytes)) {
                        return@withContext FetchOutcome.Swapped
                    }
                    if (expectedTotal <= 0 && servedLen > 0) expectedTotal = servedLen
                } else {
                    // 206: Content-Length is the REMAINING body; the total is
                    // what we already have plus it.
                    val remaining = conn.contentLengthLong
                    if (remaining > 0) {
                        val totalNow = read + remaining
                        if (UpdateDownloadPolicy.sizeProvesAssetSwap(totalNow, info.apkSizeBytes)) {
                            return@withContext FetchOutcome.Swapped
                        }
                        if (expectedTotal <= 0) expectedTotal = totalNow
                    }
                }

                val append = code == 206 && read > 0
                var legEndedCleanly = true
                try {
                    conn.inputStream.use { input ->
                        FileOutputStream(target, append).use { out ->
                            val buf = ByteArray(64 * 1024)
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                digest.update(buf, 0, n)
                                out.write(buf, 0, n)
                                read += n
                                onProgress(read, expectedTotal)
                                // Abandon promptly when the dialog closes mid-download.
                                coroutineContext.ensureActive()
                            }
                        }
                    }
                } catch (e: IOException) {
                    // Transport died mid-body (RST, read timeout): resumable —
                    // keep the partial file and continue from `read` on the
                    // next leg. CancellationException is not an IOException
                    // and still propagates.
                    legEndedCleanly = false
                }
                legs++

                if (UpdateDownloadPolicy.isTruncated(read, expectedTotal)) {
                    // The body ended — cleanly or by error — before the
                    // expected size: RESUME, never hash the partial file.
                    if (UpdateDownloadPolicy.shouldResumeAfterTruncation(legs)) continue
                    return@withContext FetchOutcome.Truncated(
                        detail = "received $read of $expectedTotal bytes across $legs connection legs",
                    )
                }
                if (!legEndedCleanly) {
                    // Transport error where truncation is not provable (the
                    // expected total is unknown); report a network failure
                    // instead of guessing a mismatch.
                    return@withContext FetchOutcome.Failed(
                        DownloadFailure.NETWORK,
                        "connection error mid-download after $read bytes",
                    )
                }
                if (read == 0L) {
                    return@withContext FetchOutcome.Failed(DownloadFailure.EMPTY_DOWNLOAD)
                }
                return@withContext FetchOutcome.Downloaded(sha256Hex(digest.digest()))
            } finally {
                conn.disconnect()
            }
        }
        // Unreachable: the loop above only exits via return@withContext.
        throw IllegalStateException("download leg loop must not fall through")
    }

    /** Internal result of one download leg sequence of [downloadApk]. */
    private sealed interface FetchOutcome {
        data class Downloaded(val sha256: String) : FetchOutcome
        data class Failed(val failure: DownloadFailure, val detail: String? = null) : FetchOutcome
        /** Server-reported size differs from the release asset size (swap in progress). */
        data object Swapped : FetchOutcome
        /** The body ended before the expected size and the resume budget is spent. */
        data class Truncated(val detail: String? = null) : FetchOutcome
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

    /**
     * v1.0.21: opens the release's direct APK download URL in the system
     * browser. This is the ESCAPE HATCH for networks on which the in-app
     * downloader cannot deliver a verifiable file: the browser download runs
     * on Android's system download stack (its own retry, resume and integrity
     * handling) — a genuinely different transport from this app's
     * HttpURLConnection — and the OS then verifies the APK signature at
     * install time exactly as for the in-app path. Purely user-driven: the
     * button that calls this appears only after a failed in-app download.
     */
    fun openDownloadInBrowser(url: String): InstallPromptResult = try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
        InstallPromptResult.Opened
    } catch (e: Throwable) {
        InstallPromptResult.Failed(e.message ?: e.javaClass.simpleName)
    }

    /** Package facts of a downloaded APK, as far as the platform exposes them. */
    private fun archiveFacts(apk: File): UpdateVerifier.ArchiveFacts = runCatching {
        // Basic facts (package name, version code) with 0 flags — works reliably on all API
        // levels without throwing signature parsing errors on uninstalled archive files.
        val basicInfo: PackageInfo? = packageManager.getPackageArchiveInfo(apk.absolutePath, 0)

        // Certificate facts: query with modern GET_SIGNING_CERTIFICATES, fall back to GET_SIGNATURES.
        val signingInfo: PackageInfo? = runCatching {
            if (Build.VERSION.SDK_INT >= 28) {
                packageManager.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
            } else null
        }.getOrNull() ?: runCatching {
            @Suppress("DEPRECATION")
            packageManager.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_SIGNATURES)
        }.getOrNull()

        val info = signingInfo ?: basicInfo
        UpdateVerifier.ArchiveFacts(
            packageName = basicInfo?.packageName ?: info?.packageName,
            versionCode = (basicInfo ?: info)?.let {
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
