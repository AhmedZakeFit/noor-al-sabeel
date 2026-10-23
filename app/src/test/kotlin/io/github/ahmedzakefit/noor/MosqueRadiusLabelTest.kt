package io.github.ahmedzakefit.noor

import io.github.ahmedzakefit.noor.ui.mosques.MosquesViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the duplicated/garbled radius chips (v1.0.2): the old
 * implementation cut the localized "N km" label at the first space, so every
 * chip showed only "km"/"كم" with the number stripped. Labels must now keep
 * the number and unit together, in Latin and Arabic digits alike.
 */
class MosqueRadiusLabelTest {

    private val enPattern = "%1\$s km"
    private val arPattern = "%1\$s كم"

    @Test
    fun `radius options are the clean 1 to 10 km ladder`() {
        assertEquals((1..10).toList(), MosquesViewModel.RADIUS_OPTIONS_KM)
        assertEquals(10, MosquesViewModel.RADIUS_OPTIONS_KM.size)
    }

    @Test
    fun `latin labels keep number and unit together`() {
        assertEquals("3 km", MosquesViewModel.radiusChipLabel(3, false, enPattern))
        assertEquals("10 km", MosquesViewModel.radiusChipLabel(10, false, enPattern))
    }

    @Test
    fun `arabic labels keep the number and unit together`() {
        val label = MosquesViewModel.radiusChipLabel(3, true, arPattern)
        assertEquals("٣ كم", label)
        // The digit must survive — the old substring hack dropped it and made
        // every chip read just "كم".
        assertTrue(label.contains("٣"))
        assertTrue(label.contains("كم"))
    }

    @Test
    fun `every option produces a distinct visible label`() {
        val labels = MosquesViewModel.RADIUS_OPTIONS_KM.map {
            MosquesViewModel.radiusChipLabel(it, false, enPattern)
        }
        assertEquals(labels.size, labels.toSet().size)
        assertTrue(labels.all { it.endsWith("km") && it != "km" })
    }
}
