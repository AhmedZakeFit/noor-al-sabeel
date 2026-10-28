package io.github.ahmedzakefit.noor

import io.github.ahmedzakefit.noor.notifications.AdhanAudio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * Guards the bundled offline adhan assets: both files must exist in
 * app/src/main/res/raw/, be plausible MP3s, and stay lean so the APK
 * remains reasonable. Local unit tests run with the :app module directory
 * as working directory.
 *
 * v1.0.17 adds the ASSET INVENTORY: every bundled recording is pinned by its
 * exact SHA-256. This is the regression guard for the failure mode proven in
 * this project's history, where audio assets were changed SILENTLY:
 *
 *  - v1.0.11's "size optimization" re-compressed the recordings and replaced
 *    `adhan_cairo.mp3` with a copy of the DEFAULT recording — silently
 *    relabelling the default (2-takbir) Fouad Charif recording as the Cairo
 *    voice.
 *  - v1.0.12's release notes claimed "Cairo adhan corrected to 4 takbeers",
 *    but the commit only copied the default fajr file over
 *    `adhan_cairo_fajr.mp3` (hashes prove it); the regular Cairo file was
 *    never touched and no recording ever gained 4 opening takbeers.
 *
 * With the inventory pinned, NO audio file can change again without an
 * explicit, reviewable test update — which must carry the new file's source,
 * license and content verification (ASR transcript of the opening). The
 * `KNOWN ASSET DEFECTS` map below documents, not endorses, the remaining
 * content defects; see NOTICE.md ("Adhan voices") for the full audit.
 */
class AdhanAssetsTest {

    private fun raw(name: String): File = File("src/main/res/raw/$name.mp3")

    private fun assertBundledAdhan(name: String) {
        val file = raw(name)
        assertTrue("missing bundled adhan asset: ${file.path}", file.isFile)
        assertTrue(
            "adhan asset ${file.path} looks empty/truncated (${file.length()} bytes)",
            file.length() > 50_000,
        )
        assertTrue(
            "adhan asset ${file.path} is unexpectedly large (${file.length()} bytes)",
            file.length() < 3_500_000,
        )
        val head = ByteArray(3)
        file.inputStream().use { it.read(head) }
        val hasId3 = head[0] == 'I'.code.toByte() && head[1] == 'D'.code.toByte() && head[2] == '3'.code.toByte()
        val f = head[0].toInt() and 0xFF
        val e = head[1].toInt() and 0xFF
        val hasFrameSync = f == 0xFF && (e and 0xE0) == 0xE0
        assertTrue(
            "adhan asset ${file.path} is not a valid MP3 (no ID3v2 tag or frame sync)",
            hasId3 || hasFrameSync,
        )
    }

    @Test
    fun `standard adhan is bundled as a valid non-empty mp3`() {
        assertBundledAdhan(AdhanAudio.RES_ADHAN)
    }

    @Test
    fun `fajr adhan is bundled as a valid non-empty mp3`() {
        assertBundledAdhan(AdhanAudio.RES_ADHAN_FAJR)
    }

    @Test
    fun `the two adhan recordings are distinct files`() {
        val standard = raw(AdhanAudio.RES_ADHAN)
        val fajr = raw(AdhanAudio.RES_ADHAN_FAJR)
        assertTrue(standard.isFile && fajr.isFile)
        assertTrue(
            "fajr must use a different adhan than the other prayers",
            standard.length() != fajr.length(),
        )
    }

    @Test
    fun `bundled adhan audio stays under 7 MB total`() {
        val total = listOf(AdhanAudio.RES_ADHAN, AdhanAudio.RES_ADHAN_FAJR)
            .map { raw(it).takeIf(File::isFile)?.length() ?: 0L }
            .sum()
        assertTrue("bundled adhan audio totals $total bytes", total in 1 until 7_000_000)
    }

    // --- v1.0.17: every selectable voice has both recordings ----------------

    private val voiceAssets = listOf(
        "adhan_makkah", "adhan_makkah_fajr",
        "adhan_madinah", "adhan_madinah_fajr",
        "adhan_cairo", "adhan_cairo_fajr",
        "adhan_aqsa", "adhan_aqsa_fajr",
    )

    @Test
    fun `every selectable voice bundles a regular and a fajr recording`() {
        // AdhanAudio.rawResourceFor resolves exactly these names; a missing or
        // invalid file would silently fall back to the default recording at
        // prayer time — the fallback hides asset regressions, so assert here.
        for (name in voiceAssets) {
            assertBundledAdhan(name)
        }
    }

    @Test
    fun `all bundled adhan audio together stays under 18 MB`() {
        val total = (voiceAssets + listOf(AdhanAudio.RES_ADHAN, AdhanAudio.RES_ADHAN_FAJR))
            .map { raw(it).takeIf(File::isFile)?.length() ?: 0L }
            .sum()
        // Current: 10 files, ~15.2 MB (64 kbps mono since v1.0.11).
        assertTrue("bundled adhan audio totals $total bytes", total in 1 until 18_000_000)
    }

    // --- v1.0.17: asset inventory pinned by SHA-256 --------------------------

    /**
     * The exact bytes shipped in v1.0.16 (and unchanged into v1.0.17).
     *
     * ⚠ KNOWN ASSET DEFECTS, documented — not endorsed (audit 2026-10-28):
     *  - `adhan_cairo` == `adhan` (the default Fouad Charif recording) and
     *    `adhan_cairo_fajr` == `adhan_fajr`: the "Cairo" voice is a
     *    byte-identical copy of the default pair, whose opening recites only
     *    TWO takbirs (ASR-verified: "الله أكبر الله أكبر … أشهد أن لا إله إلا
     *    الله"), not the FOUR that the Egyptian adhan tradition expects.
     *  - `adhan_madinah` == `adhan_madinah_fajr` and `adhan_aqsa` ==
     *    `adhan_aqsa_fajr`: the fajr variants of these voices are the same
     *    recording as the regular ones.
     *
     * Replacing any file below is a CONSCIOUS act: update this map in the same
     * commit and document the new recording's source, license and an ASR
     * transcript of its opening takbirs in NOTICE.md.
     */
    private val inventory: Map<String, String> = mapOf(
        "adhan" to "b738232f9956e057a889fafc50cb69ac99a76c33464c90475ecfcaf206f437fb",
        "adhan_fajr" to "9d7362697ff2d9c94a0fd4dd6934ba17005d7ff081bd80621562e0ecba084d66",
        "adhan_makkah" to "6e84375aa0d2a7db051f4e73c72348fffb8dba8550417df98eeca44c4be83795",
        "adhan_makkah_fajr" to "ec64d39f881573a6279bc4dff7bc3ae4fb3e23f1b865d19b02918db0aa59053e",
        "adhan_madinah" to "9053a1b82df7d163ec5ade7880ff4ee2e3339b2478486997bf6d4257ed3f8dd8",
        "adhan_madinah_fajr" to "9053a1b82df7d163ec5ade7880ff4ee2e3339b2478486997bf6d4257ed3f8dd8",
        "adhan_cairo" to "b738232f9956e057a889fafc50cb69ac99a76c33464c90475ecfcaf206f437fb",
        "adhan_cairo_fajr" to "9d7362697ff2d9c94a0fd4dd6934ba17005d7ff081bd80621562e0ecba084d66",
        "adhan_aqsa" to "51c6abbc0e1b91001a0ea900a20cc6fc4b48948b099c6ded0ae7c2c58ca80fe5",
        "adhan_aqsa_fajr" to "51c6abbc0e1b91001a0ea900a20cc6fc4b48948b099c6ded0ae7c2c58ca80fe5",
    )

    @Test
    fun `bundled adhan recordings match the pinned asset inventory`() {
        val mismatches = mutableListOf<String>()
        for ((name, expected) in inventory) {
            val file = raw(name)
            if (!file.isFile) {
                mismatches += "$name: MISSING"
                continue
            }
            val actual = file.inputStream().use {
                MessageDigest.getInstance("SHA-256").digest(it.readBytes())
            }.joinToString("") { "%02x".format(it) }
            if (actual != expected) {
                mismatches += "$name: pinned $expected, found $actual"
            }
        }
        assertTrue(
            "Adhan audio changed without an inventory update. Every change to a bundled " +
                "recording MUST update this pinned map and document the new file's source, " +
                "license and opening-takbir verification in NOTICE.md. " +
                "Offending assets: $mismatches",
            mismatches.isEmpty(),
        )
    }

    @Test
    fun `the cairo voice defect is documented by the inventory not hidden by it`() {
        // Pins the AUDITED fact this release ships with (see NOTICE.md):
        // the "Cairo" voice is currently a byte-copy of the default pair and
        // opens with two takbirs. When a licensed replacement lands, this
        // assertion flips to the distinctness requirement it guards.
        val cairo = inventory.getValue("adhan_cairo")
        val default = inventory.getValue("adhan")
        val cairoFajr = inventory.getValue("adhan_cairo_fajr")
        val defaultFajr = inventory.getValue("adhan_fajr")
        assertEquals(
            "if the Cairo pair stops being the default pair, update NOTICE.md " +
                "and the KNOWN ASSET DEFECTS note with the new recording's provenance",
            cairo == default && cairoFajr == defaultFajr,
            true,
        )
    }
}
