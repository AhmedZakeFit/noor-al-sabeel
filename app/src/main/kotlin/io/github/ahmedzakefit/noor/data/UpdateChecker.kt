package io.github.ahmedzakefit.noor.data

import org.json.JSONObject
import java.net.URL

/**
 * Pure, JVM-testable parsing and decision logic for the in-app update check
 * against GitHub Releases. No Android, no network — the transport lives in
 * [UpdateManager] and [Http].
 *
 * Security posture:
 *  - release metadata is read from api.github.com over TLS for this exact repo;
 *  - the APK URL must be one of the release's own asset URLs on an allowed host;
 *  - the downloaded file's SHA-256 must equal the published ".apk.sha256" asset;
 *  - the archive's package name, versionCode and signing certificate must match
 *    the installed app — the OS enforces the same on install; we check first so
 *    a broken or tampered file never even reaches the install prompt.
 */
object UpdateParser {

    data class ReleaseInfo(
        val tag: String,
        val versionName: String,
        val apkUrl: String,
        val apkSizeBytes: Long,
        val sha256Url: String?,
        val changelogSummary: String,
    )

    /** "v1.0.4" / "1.0.4" -> "1.0.4"; anything malformed -> null. */
    fun versionNameFromTag(tag: String): String? {
        val t = tag.trim().removePrefix("v").removePrefix("V")
        if (!Regex("""^\d+(\.\d+)*$""").matches(t)) return null
        return t
    }

    /**
     * Version read from where the "/releases/latest" redirect landed, e.g.
     * "https://github.com/o/r/releases/tag/v1.0.4" -> "1.0.4". Lets the app
     * learn the latest version without a (rate-limited) API call.
     */
    fun versionFromRedirectUrl(url: String): String? {
        val m = Regex("""/releases/tag/(v?\d+(?:\.\d+)*)/?$""").find(url.trim()) ?: return null
        return versionNameFromTag(m.groupValues[1])
    }

    /** Segment-wise "is candidate strictly newer than installed". */
    fun isNewer(candidate: String, installed: String): Boolean {
        val a = candidate.split('.').mapNotNull { it.toIntOrNull() }
        val b = installed.split('.').mapNotNull { it.toIntOrNull() }
        if (a.isEmpty() || b.isEmpty()) return false
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** "ab12…  filename\n" (sha256sum format) -> lowercase hex; invalid -> null. */
    fun parseChecksumFile(text: String): String? {
        val clean = text.trim()
        val match = Regex("[0-9a-fA-F]{64}").find(clean)
        return match?.value?.lowercase()
    }

    /** Release body markdown -> compact plain-text summary, capped for a dialog. */
    fun changelogSummary(body: String, maxChars: Int = 600): String {
        var s = body
            .replace(Regex("!\\[[^]]*]\\([^)]*\\)"), "")          // images out
            .replace(Regex("\\[([^]]*)]\\([^)]*\\)"), "$1")       // links -> text
            .replace(Regex("[*_`#>]+"), "")                        // emphasis noise
            .replace(Regex("\\s*\\n\\s*\\n\\s*"), "\n")            // blank lines
            .trim()
        if (s.length > maxChars) s = s.take(maxChars).trimEnd() + "…"
        return s
    }

    /** Stable asset names — the landing page's fixed direct-download link
     * points at the stable APK name under /releases/latest/download/.
     */
    internal const val STABLE_APK_ASSET = "noor-al-sabeel.apk"

    /**
     * Only assets that are FULLY uploaded and have a real byte size are
     * usable. GitHub lists an asset in the release document from the moment
     * its upload STARTS (state "starter", size 0) until it is committed
     * (state "uploaded") — during that window the API can show a FINAL
     * checksum next to an APK whose bytes are not committed yet, and
     * downloading the pair fails the SHA-256 comparison on genuinely
     * incomplete bytes. v1.0.16's eager-updater wave hit exactly this
     * window (the .sha256 assets upload in ~0.1 s, the 24 MB APKs take
     * minutes); a half-published release must yield NO update at all — the
     * next check finds it complete.
     */
    fun assetUsable(a: JSONObject): Boolean {
        val state = a.optString("state")
        if (state.isNotEmpty() && state != "uploaded") return false
        return a.optLong("size", 0L) > 0L
    }

    /** Parses the /releases/latest JSON; null when unusable (no tag/APK).
     *
     * Releases carry up to four APK-related assets: the versioned APK
     * (noor-al-sabeel-<version>.apk) + checksum, and the stable-named copies
     * (noor-al-sabeel.apk + checksum) that the landing page links to. The
     * versioned pair is preferred — identical bytes, unambiguous in caches
     * and logs — with the stable pair as fallback, deterministically instead
     * of whatever order the assets happen to come in. Assets that are still
     * mid-upload (state "starter" / size 0) are never accepted (see
     * [assetUsable]): a checksum may only be paired with a fully published
     * APK of the same release.
     */
    fun parseRelease(json: String): ReleaseInfo? {
        val obj = JSONObject(json)
        val tag = obj.optString("tag_name")
        val version = versionNameFromTag(tag) ?: return null
        val assets = obj.optJSONArray("assets") ?: return null
        val versionedApk = "noor-al-sabeel-$version.apk"
        var apkUrl: String? = null
        var apkSize = 0L
        var shaUrl: String? = null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            val name = a.optString("name")
            val url = a.optString("browser_download_url")
            if (!assetUsable(a)) continue
            when {
                name == "$versionedApk.sha256" -> shaUrl = url
                name == "$STABLE_APK_ASSET.sha256" -> if (shaUrl == null) shaUrl = url
                name == versionedApk -> {
                    apkUrl = url
                    apkSize = a.optLong("size")
                }
                name == STABLE_APK_ASSET -> if (apkUrl == null) {
                    apkUrl = url
                    apkSize = a.optLong("size")
                }
                name.endsWith(".apk.sha256") -> if (shaUrl == null) shaUrl = url
                name.endsWith(".apk") -> if (apkUrl == null) {
                    apkUrl = url
                    apkSize = a.optLong("size")
                }
            }
        }
        val apk = apkUrl ?: return null
        return ReleaseInfo(
            tag = tag,
            versionName = version,
            apkUrl = apk,
            apkSizeBytes = apkSize,
            sha256Url = shaUrl,
            changelogSummary = changelogSummary(obj.optString("body")),
        )
    }

    /** Only our own release-asset hosts are ever contacted for an APK. */
    fun isAllowedDownloadUrl(url: String): Boolean {
        val u = runCatching { URL(url) }.getOrNull() ?: return false
        if (u.protocol != "https") return false
        val host = u.host
        return host == "github.com" || host == "api.github.com" ||
            host == "objects.githubusercontent.com" ||
            host == "release-assets.githubusercontent.com" ||
            host.endsWith(".githubusercontent.com")
    }

    /** GitHub owner/repo this updater is bound to (constant, no user input). */
    internal const val REPO_PATH = "AhmedZakeFit/noor-al-sabeel"

    /**
     * Deterministic release-asset URL pairs for the API-free update path.
     *
     * The release workflow always publishes, under the tag's download
     * directory, the versioned pair `noor-al-sabeel-<version>.apk` +
     * `.apk.sha256` and the stable-named pair `noor-al-sabeel.apk` +
     * `.apk.sha256` (identical bytes). Given the version resolved from the
     * `/releases/latest` redirect (github.com — NOT the rate-limited API),
     * these are exactly the URLs the API would have returned. The caller
     * HEAD-probes a pair and only uses it when both assets answer 200; the
     * download is then verified against the published checksum exactly as in
     * the API path, so the security posture is unchanged.
     *
     * Versioned pair first, stable pair as fallback. Invalid version -> null.
     */
    fun fallbackAssetUrls(version: String): List<Pair<String, String>>? {
        val v = versionNameFromTag(version) ?: return null
        val base = "https://github.com/$REPO_PATH/releases/download/v$v"
        return listOf(
            "$base/noor-al-sabeel-$v.apk" to "$base/noor-al-sabeel-$v.apk.sha256",
            "$base/$STABLE_APK_ASSET" to "$base/$STABLE_APK_ASSET.sha256",
        )
    }

    /** Outcome of evaluating a fetched release document. */
    enum class Decision { AVAILABLE, UP_TO_DATE, FAILED }

    /**
     * Pure decision for a (possibly failed) release fetch — the heart of the
     * update check, kept free of Android and network so every branch is
     * unit-testable:
     *  - null/blank body (connection failed, offline, non-200): manual checks
     *    report FAILED, automatic checks stay silently UP_TO_DATE so prayer
     *    functions are never disturbed;
     *  - unusable documents (bad tag, no APK asset, off allowlist): UP_TO_DATE;
     *  - not newer than installed: UP_TO_DATE;
     *  - postponed version in automatic mode: UP_TO_DATE (no nagging);
     *  - otherwise AVAILABLE with the parsed release info.
     */
    fun decide(
        body: String?,
        installedVersionName: String,
        postponedVersion: String?,
        manual: Boolean,
    ): Pair<Decision, ReleaseInfo?> {
        if (body.isNullOrBlank()) {
            return if (manual) Decision.FAILED to null else Decision.UP_TO_DATE to null
        }
        val info = parseRelease(body) ?: return Decision.UP_TO_DATE to null
        if (!isAllowedDownloadUrl(info.apkUrl)) return Decision.UP_TO_DATE to null
        if (!isNewer(info.versionName, installedVersionName)) {
            return Decision.UP_TO_DATE to null
        }
        if (!manual && info.versionName == postponedVersion) {
            return Decision.UP_TO_DATE to null
        }
        return Decision.AVAILABLE to info
    }
}

/**
 * Verdict logic for a downloaded update APK, applied before any install
 * prompt: checksum must match, package must be ours, versionCode must be
 * newer, and the signing certificate must match the installed app's.
 */
object UpdateVerifier {

    enum class Verdict { OK, CHECKSUM_UNKNOWN, CHECKSUM_MISMATCH, PACKAGE_MISMATCH, NOT_NEWER, SIGNATURE_MISMATCH }

    data class ArchiveFacts(
        val packageName: String?,
        val versionCode: Long?,
        /** SHA-256 hex of the archive's signer certificate, or null when the
         * platform could not extract one (then the checksum + OS checks carry
         * the verification). */
        val signerSha256: String?,
    )

    fun verdict(
        publishedSha256: String?,
        actualSha256: String?,
        archive: ArchiveFacts,
        installedPackageName: String,
        installedVersionCode: Long,
        installedSignerSha256: String,
    ): Verdict = when {
        publishedSha256 == null || actualSha256 == null -> Verdict.CHECKSUM_UNKNOWN
        publishedSha256 != actualSha256 -> Verdict.CHECKSUM_MISMATCH
        archive.packageName != installedPackageName -> Verdict.PACKAGE_MISMATCH
        (archive.versionCode ?: 0L) <= installedVersionCode -> Verdict.NOT_NEWER
        archive.signerSha256 != null && archive.signerSha256 != installedSignerSha256 -> Verdict.SIGNATURE_MISMATCH
        else -> Verdict.OK
    }
}

/**
 * Pure retry policy for the update download+verify sequence (v1.0.17).
 *
 * Root cause it guards: the published .sha256 checksum and the APK are two
 * separate HTTP requests, and a release that is RE-PUBLISHED (assets swapped)
 * between the two requests makes an in-flight update verify two different
 * builds against each other — a false "SHA-256 mismatch" that is not
 * corruption at all. Proven in the wild on v1.0.16: the release was re-run
 * ~19 minutes after the first publish, replacing both files under live users.
 *
 * The policy allows exactly ONE re-verify pass: re-fetch the published
 * checksum first (cheap), and only re-download when the bytes on disk still
 * disagree with it. Security is unchanged — the file must equal the checksum
 * the release publishes NOW, and a final disagreement still fails the update.
 */
object UpdateDownloadPolicy {

    /** Download+verify passes per user request: the original plus one retry. */
    const val MAX_ATTEMPTS = 2

    /**
     * Whether one more pass can help after this verdict. Only checksum-family
     * verdicts are retryable (a re-publish race looks exactly like this);
     * package/version/signature verdicts are properties of the bytes
     * themselves and cannot change with a re-fetch.
     */
    fun shouldRetry(verdict: UpdateVerifier.Verdict, attemptsDone: Int): Boolean =
        attemptsDone < MAX_ATTEMPTS && (
            verdict == UpdateVerifier.Verdict.CHECKSUM_MISMATCH ||
                verdict == UpdateVerifier.Verdict.CHECKSUM_UNKNOWN
            )

    /**
     * The release document records the APK asset's byte size. When the server
     * reports a different non-unknown body size, the served file provably is
     * not the release the check resolved (an asset swap in progress) — abort
     * before hashing. Unknown sizes (-1 / 0) never prove anything.
     */
    fun sizeProvesAssetSwap(servedContentLength: Long, releaseAssetSizeBytes: Long): Boolean =
        servedContentLength > 0 && releaseAssetSizeBytes > 0 &&
            servedContentLength != releaseAssetSizeBytes
}
