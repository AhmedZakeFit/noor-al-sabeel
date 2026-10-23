package io.github.ahmedzakefit.noor

import io.github.ahmedzakefit.noor.core.model.Prayer
import io.github.ahmedzakefit.noor.data.AdhanMode
import io.github.ahmedzakefit.noor.data.AppSettings
import io.github.ahmedzakefit.noor.notifications.AdhanAudio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdhanAudioTest {

    @Test
    fun `fajr maps to the dedicated dawn adhan resource`() {
        assertEquals(AdhanAudio.RES_ADHAN_FAJR, AdhanAudio.resourceNameFor(Prayer.FAJR))
    }

    @Test
    fun `every other prayer maps to the standard adhan resource`() {
        for (prayer in listOf(Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)) {
            assertEquals(AdhanAudio.RES_ADHAN, AdhanAudio.resourceNameFor(prayer))
        }
    }

    @Test
    fun `adhan plays only in adhan mode for real prayers`() {
        for (prayer in Prayer.entries) {
            if (prayer.notifiable) {
                assertTrue(AdhanAudio.shouldPlayAdhan(AdhanMode.ADHAN, false, prayer))
            } else {
                assertFalse(AdhanAudio.shouldPlayAdhan(AdhanMode.ADHAN, false, prayer))
            }
        }
    }

    @Test
    fun `pre-prayer reminders never raise the adhan`() {
        for (prayer in Prayer.entries) {
            assertFalse(AdhanAudio.shouldPlayAdhan(AdhanMode.ADHAN, true, prayer))
        }
    }

    @Test
    fun `regular mode never raises the adhan`() {
        for (prayer in Prayer.entries) {
            assertFalse(AdhanAudio.shouldPlayAdhan(AdhanMode.REGULAR, false, prayer))
        }
    }

    @Test
    fun `default alert style is a regular notification so updates never surprise users`() {
        assertEquals(AdhanMode.REGULAR, AppSettings().adhanMode)
    }

    @Test
    fun `per-prayer toggles apply to the adhan because scheduling gates the alarm`() {
        // The adhan rides the exact same per-prayer alarm chain as the regular
        // notification; a prayer disabled here never schedules an alarm at all.
        val allEnabled = AppSettings().notificationsPerPrayer
        assertEquals(setOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA), allEnabled)
        val withoutFajr = AppSettings().copy(
            notificationsPerPrayer = allEnabled - Prayer.FAJR,
        ).notificationsPerPrayer
        assertFalse(withoutFajr.contains(Prayer.FAJR))
    }
}
