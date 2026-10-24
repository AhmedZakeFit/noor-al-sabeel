package io.github.ahmedzakefit.noor

import io.github.ahmedzakefit.noor.data.UpdateParser
import io.github.ahmedzakefit.noor.data.UpdateVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the GitHub-Releases update check: newer-version detection,
 * no-update case, connection-failure containment (silent in auto mode),
 * checksum parsing, host allowlist, and the pre-install file verification.
 */
class UpdateParserTest {

    // --- version/tag parsing -------------------------------------------------

    @Test
    fun `tags parse into version names`() {
        assertEquals("1.0.4", UpdateParser.versionNameFromTag("v1.0.4"))
        assertEquals("1.0.4", UpdateParser.versionNameFromTag("1.0.4"))
        assertEquals("2.0", UpdateParser.versionNameFromTag("v2.0"))
        assertNull(UpdateParser.versionNameFromTag("beta"))
        assertNull(UpdateParser.versionNameFromTag(""))
        assertNull(UpdateParser.versionNameFromTag("v1.0.x"))
    }

    @Test
    fun `redirect target of releases-latest yields the version`() {
        // The rate-limit-free pre-check path: resolve /releases/latest by
        // redirect and read the tag out of the final URL.
        assertEquals(
            "1.0.4",
            UpdateParser.versionFromRedirectUrl("https://github.com/AhmedZakeFit/noor-al-sabeel/releases/tag/v1.0.4"),
        )
        assertEquals(
            "2.0",
            UpdateParser.versionFromRedirectUrl("https://github.com/o/r/releases/tag/2.0/"),
        )
        assertNull(UpdateParser.versionFromRedirectUrl("https://github.com/o/r/releases/latest"))
        assertNull(UpdateParser.versionFromRedirectUrl("https://github.com/o/r/releases/tag/v1.0-beta"))
        assertNull(UpdateParser.versionFromRedirectUrl("https://example.com/no-tag-here"))
    }

    @Test
    fun `newer version detection across shapes`() {
        assertTrue(UpdateParser.isNewer("1.0.4", "1.0.3"))
        assertTrue(UpdateParser.isNewer("1.1", "1.0.9"))
        assertTrue(UpdateParser.isNewer("2.0", "1.9.9"))
        assertTrue(UpdateParser.isNewer("1.0.10", "1.0.9")) // numeric, not lexicographic
        assertFalse(UpdateParser.isNewer("1.0.3", "1.0.3")) // same -> no update
        assertFalse(UpdateParser.isNewer("1.0.2", "1.0.3")) // older -> no update
        assertFalse(UpdateParser.isNewer("x", "1.0.3")) // unusable -> no update
    }

    // --- release JSON ---------------------------------------------------------

    private val releaseJson = """
    {
      "tag_name": "v1.0.4",
      "name": "v1.0.4",
      "body": "**Noor Al-Sabeel v1.0.4** — signed release APK. See [CHANGELOG](https://github.com/AhmedZakeFit/noor-al-sabeel/blob/main/CHANGELOG.md).\n\n- Fixed notification scheduling\n- Live countdown reminders\n\n**Full Changelog**: https://github.com/AhmedZakeFit/noor-al-sabeel/compare/v1.0.3...v1.0.4",
      "assets": [
        {"name": "noor-al-sabeel-1.0.4.apk", "size": 12850128,
         "browser_download_url": "https://github.com/AhmedZakeFit/noor-al-sabeel/releases/download/v1.0.4/noor-al-sabeel-1.0.4.apk"},
        {"name": "noor-al-sabeel-1.0.4.apk.sha256", "size": 91,
         "browser_download_url": "https://github.com/AhmedZakeFit/noor-al-sabeel/releases/download/v1.0.4/noor-al-sabeel-1.0.4.apk.sha256"}
      ]
    }
    """.trimIndent()

    @Test
    fun `release json parses into apk url checksum url and summary`() {
        val info = UpdateParser.parseRelease(releaseJson)
        assertNotNull(info)
        assertEquals("v1.0.4", info!!.tag)
        assertEquals("1.0.4", info.versionName)
        assertTrue(info.apkUrl.endsWith("noor-al-sabeel-1.0.4.apk"))
        assertTrue(info.sha256Url!!.endsWith(".apk.sha256"))
        assertEquals(12850128L, info.apkSizeBytes)
        // Markdown links degrade to their text; bullets survive.
        assertTrue(info.changelogSummary.contains("Fixed notification scheduling"))
        assertFalse(info.changelogSummary.contains("]("))
        assertTrue(info.changelogSummary.contains("CHANGELOG"))
    }

    @Test
    fun `releases without an apk asset or with a bad tag are unusable`() {
        assertNull(UpdateParser.parseRelease("""{"tag_name":"v1.0.4","assets":[]}"""))
        assertNull(UpdateParser.parseRelease("""{"tag_name":"weird","assets":[{"name":"a.apk","browser_download_url":"https://github.com/x/a.apk"}]}"""))
        assertNull(UpdateParser.parseRelease("""{"assets":[{"name":"a.apk","browser_download_url":"https://github.com/x/a.apk"}]}"""))
    }

    // --- new asset layout: versioned + stable-named copies -----------------

    @Test
    fun `versioned apk asset wins over the stable copy regardless of order`() {
        // Both assets exist (v1.0.5 release layout); whichever order the API
        // returns them in, the in-app updater must deterministically pick the
        // versioned pair — same bytes, unambiguous in caches and logs.
        val versioned = """{"name": "noor-al-sabeel-1.0.5.apk", "size": 12900000,
             "browser_download_url": "https://github.com/AhmedZakeFit/noor-al-sabeel/releases/download/v1.0.5/noor-al-sabeel-1.0.5.apk"},
            {"name": "noor-al-sabeel-1.0.5.apk.sha256", "size": 91,
             "browser_download_url": "https://github.com/AhmedZakeFit/noor-al-sabeel/releases/download/v1.0.5/noor-al-sabeel-1.0.5.apk.sha256"}"""
        val stable = """{"name": "noor-al-sabeel.apk", "size": 12900000,
             "browser_download_url": "https://github.com/AhmedZakeFit/noor-al-sabeel/releases/download/v1.0.5/noor-al-sabeel.apk"},
            {"name": "noor-al-sabeel.apk.sha256", "size": 91,
             "browser_download_url": "https://github.com/AhmedZakeFit/noor-al-sabeel/releases/download/v1.0.5/noor-al-sabeel.apk.sha256"}"""
        for (order in listOf("$stable,$versioned", "$versioned,$stable")) {
            val info = UpdateParser.parseRelease("""{"tag_name": "v1.0.5", "assets": [$order]}""")!!
            assertTrue(info.apkUrl.endsWith("noor-al-sabeel-1.0.5.apk"))
            assertTrue(info.sha256Url!!.endsWith("noor-al-sabeel-1.0.5.apk.sha256"))
            assertEquals(12900000L, info.apkSizeBytes)
            assertEquals("1.0.5", info.versionName)
        }
    }

    @Test
    fun `a stable-asset-only release still yields a usable update`() {
        // Safety net: even if a future release somehow carries only the
        // stable-named asset, the updater still finds an APK to download.
        val info = UpdateParser.parseRelease("""
        {
          "tag_name": "v1.0.6",
          "assets": [
            {"name": "noor-al-sabeel.apk", "size": 13000000,
             "browser_download_url": "https://github.com/AhmedZakeFit/noor-al-sabeel/releases/download/v1.0.6/noor-al-sabeel.apk"},
            {"name": "noor-al-sabeel.apk.sha256", "size": 91,
             "browser_download_url": "https://github.com/AhmedZakeFit/noor-al-sabeel/releases/download/v1.0.6/noor-al-sabeel.apk.sha256"}
          ]
        }
        """.trimIndent())!!
        assertTrue(info.apkUrl.endsWith("/noor-al-sabeel.apk"))
        assertTrue(info.sha256Url!!.endsWith("noor-al-sabeel.apk.sha256"))
        assertEquals("1.0.6", info.versionName)
        assertEquals(UpdateParser.STABLE_APK_ASSET, "noor-al-sabeel.apk")
    }

    // --- checksum file --------------------------------------------------------

    @Test
    fun `sha256sum asset format parses to lowercase hex`() {
        val sha = "e129a3cdab12ef34deadbeefcafe1234ab56cd78ef90ab12cd34ef56ab78cd90"
        assertEquals(sha, UpdateParser.parseChecksumFile("$sha  noor-al-sabeel-1.0.3.apk\n"))
        assertEquals(sha, UpdateParser.parseChecksumFile(sha.uppercase()))
        assertNull(UpdateParser.parseChecksumFile("not-a-hash  file"))
        assertNull(UpdateParser.parseChecksumFile(""))
        assertNull(UpdateParser.parseChecksumFile("e129a3  file")) // too short
    }

    // --- host allowlist -------------------------------------------------------

    @Test
    fun `only https release-asset hosts are allowed downloads`() {
        assertTrue(UpdateParser.isAllowedDownloadUrl("https://github.com/AhmedZakeFit/noor-al-sabeel/releases/download/v1.0.4/a.apk"))
        assertTrue(UpdateParser.isAllowedDownloadUrl("https://objects.githubusercontent.com/x"))
        assertTrue(UpdateParser.isAllowedDownloadUrl("https://release-assets.githubusercontent.com/x"))
        assertTrue(UpdateParser.isAllowedDownloadUrl("https://api.github.com/repos/x/releases"))
        // Anything else — http, other hosts, garbage — is refused.
        assertFalse(UpdateParser.isAllowedDownloadUrl("http://github.com/x.apk"))
        assertFalse(UpdateParser.isAllowedDownloadUrl("https://evil.example.com/a.apk"))
        assertFalse(UpdateParser.isAllowedDownloadUrl("https://github.com.evil.example.com/a.apk"))
        assertFalse(UpdateParser.isAllowedDownloadUrl("not a url"))
    }

    // --- changelog summary ----------------------------------------------------

    @Test
    fun `changelog summary is capped and stripped`() {
        val longBody = (1..200).joinToString("\n") { "line $it with **bold** and [link](https://x)" }
        val summary = UpdateParser.changelogSummary(longBody, maxChars = 120)
        assertTrue(summary.length <= 121) // 120 + ellipsis
        assertTrue(summary.endsWith("…"))
        assertFalse(summary.contains("**"))
        assertFalse(summary.contains("](https"))
    }

    // --- the decision (newer / no update / connection failure / postponed) ----

    @Test
    fun `newer release is available with parsed info`() {
        val (decision, info) = UpdateParser.decide(releaseJson, "1.0.3", null, manual = false)
        assertEquals(UpdateParser.Decision.AVAILABLE, decision)
        assertEquals("1.0.4", info!!.versionName)
    }

    @Test
    fun `same or older release means no update`() {
        assertEquals(
            UpdateParser.Decision.UP_TO_DATE,
            UpdateParser.decide(releaseJson, "1.0.4", null, manual = false).first,
        )
        assertEquals(
            UpdateParser.Decision.UP_TO_DATE,
            UpdateParser.decide(releaseJson, "2.0.0", null, manual = true).first,
        )
    }

    @Test
    fun `connection failure is silent for auto checks and visible for manual ones`() {
        // body null/blank == offline / failed fetch / non-200.
        assertEquals(
            UpdateParser.Decision.UP_TO_DATE,
            UpdateParser.decide(null, "1.0.3", null, manual = false).first,
        )
        assertEquals(
            UpdateParser.Decision.UP_TO_DATE,
            UpdateParser.decide("", "1.0.3", null, manual = false).first,
        )
        assertEquals(
            UpdateParser.Decision.FAILED,
            UpdateParser.decide(null, "1.0.3", null, manual = true).first,
        )
    }

    @Test
    fun `postponed version is quiet in auto mode but manual check still shows it`() {
        assertEquals(
            UpdateParser.Decision.UP_TO_DATE,
            UpdateParser.decide(releaseJson, "1.0.3", "1.0.4", manual = false).first,
        )
        assertEquals(
            UpdateParser.Decision.AVAILABLE,
            UpdateParser.decide(releaseJson, "1.0.3", "1.0.4", manual = true).first,
        )
        // A postponed OLDER version does not silence a newer release.
        assertEquals(
            UpdateParser.Decision.AVAILABLE,
            UpdateParser.decide(releaseJson, "1.0.3", "1.0.3", manual = false).first,
        )
    }
}

class UpdateVerifierTest {

    private val sha = "aa11".repeat(16)
    private val installed = "io.github.ahmedzakefit.noor"

    private fun facts(
        pkg: String? = installed,
        code: Long? = 5,
        signer: String? = sha,
    ) = UpdateVerifier.ArchiveFacts(pkg, code, signer)

    @Test
    fun `matching checksum package version and signature verifies`() {
        assertEquals(
            UpdateVerifier.Verdict.OK,
            UpdateVerifier.verdict(sha, sha, facts(), installed, 4, sha),
        )
    }

    @Test
    fun `missing or mismatched checksum blocks install`() {
        assertEquals(
            UpdateVerifier.Verdict.CHECKSUM_UNKNOWN,
            UpdateVerifier.verdict(null, sha, facts(), installed, 4, sha),
        )
        assertEquals(
            UpdateVerifier.Verdict.CHECKSUM_MISMATCH,
            UpdateVerifier.verdict(sha, "ff".repeat(32), facts(), installed, 4, sha),
        )
    }

    @Test
    fun `wrong package or non-newer version blocks install`() {
        assertEquals(
            UpdateVerifier.Verdict.PACKAGE_MISMATCH,
            UpdateVerifier.verdict(sha, sha, facts(pkg = "com.other.app"), installed, 4, sha),
        )
        assertEquals(
            UpdateVerifier.Verdict.NOT_NEWER,
            UpdateVerifier.verdict(sha, sha, facts(code = 4), installed, 4, sha),
        )
        assertEquals(
            UpdateVerifier.Verdict.NOT_NEWER,
            UpdateVerifier.verdict(sha, sha, facts(code = null), installed, 4, sha),
        )
    }

    @Test
    fun `mismatched signing certificate blocks install`() {
        assertEquals(
            UpdateVerifier.Verdict.SIGNATURE_MISMATCH,
            UpdateVerifier.verdict(sha, sha, facts(signer = "bb".repeat(32)), installed, 4, sha),
        )
    }

    @Test
    fun `platforms that cannot expose archive signatures still verify via checksum`() {
        // signerSha256 null (e.g. extractor unavailable) -> checksum + OS
        // install-time signature enforcement carry the verification.
        assertEquals(
            UpdateVerifier.Verdict.OK,
            UpdateVerifier.verdict(sha, sha, facts(signer = null), installed, 4, sha),
        )
    }
}
