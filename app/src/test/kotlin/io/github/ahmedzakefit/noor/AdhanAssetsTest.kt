package io.github.ahmedzakefit.noor

import io.github.ahmedzakefit.noor.notifications.AdhanAudio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the bundled offline adhan assets: both files must exist in
 * app/src/main/res/raw/, be plausible MP3s, and stay lean so the APK
 * remains reasonable. Local unit tests run with the :app module directory
 * as working directory.
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
}
