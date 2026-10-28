package io.github.ahmedzakefit.noor

import io.github.ahmedzakefit.noor.data.AdhanVoice
import io.github.ahmedzakefit.noor.notifications.AdhanPlayback
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the "cannot stop the test adhan" report (v1.0.2) AND the
 * v1.0.17 report "after the real adhan ends at prayer time, the test adhan
 * starts automatically": the settings screen relies on this live state to swap
 * the test button for a stop button while the adhan service is reciting, and
 * it must be able to tell a user-initiated TEST apart from the REAL prayer-time
 * adhan — before v1.0.17 every playing adhan (including the real one the alarm
 * scheduler starts at prayer time) was labelled "test adhan", which users read
 * as the test starting by itself. (AdhanAudioTest covers the alarm-side
 * routing; this file covers the playback flag that drives the UI.)
 */
class AdhanPlaybackStateTest {

    private fun reset() {
        AdhanPlayback.setPlaying(false)
    }

    @Test
    fun `flag starts false and tracks service lifecycle transitions`() {
        // Fresh default: nothing is playing, so the settings screen shows the
        // "test the adhan" button.
        // (Object state persists within a JVM; reset defensively.)
        reset()
        assertFalse(AdhanPlayback.isPlaying.value)

        AdhanPlayback.setPlaying(true)
        assertTrue(AdhanPlayback.isPlaying.value)

        AdhanPlayback.setPlaying(false)
        assertFalse(AdhanPlayback.isPlaying.value)
    }

    @Test
    fun `is playing is a state flow so screens collect it reactively`() {
        assertTrue(AdhanPlayback.isPlaying is StateFlow<Boolean>)
    }

    // --- v1.0.17: preview vs real adhan --------------------------------------

    @Test
    fun `a test adhan reports itself as preview`() {
        reset()
        AdhanPlayback.setPlaying(true, voice = AdhanVoice.CAIRO, forFajr = false, isPreview = true)
        assertTrue(AdhanPlayback.isPlaying.value)
        assertTrue("a user-initiated test must be flagged as preview", AdhanPlayback.currentIsPreview.value)
        assertEquals(AdhanVoice.CAIRO, AdhanPlayback.currentVoice.value)
        assertFalse(AdhanPlayback.currentForFajr.value)
        reset()
    }

    @Test
    fun `the real prayer-time adhan reports itself as NOT preview`() {
        // The alarm scheduler's start at prayer time: preview=false. The
        // settings screen must show the honest "live adhan" state for this —
        // NOT the "test adhan" card users saw before v1.0.17.
        reset()
        AdhanPlayback.setPlaying(true, voice = AdhanVoice.CAIRO, forFajr = false, isPreview = false)
        assertTrue(AdhanPlayback.isPlaying.value)
        assertFalse("the real adhan is not a test", AdhanPlayback.currentIsPreview.value)
        assertEquals(AdhanVoice.CAIRO, AdhanPlayback.currentVoice.value)
        reset()
    }

    @Test
    fun `stopping clears the preview flag so a later real adhan starts from a clean state`() {
        // Sequence that used to mislabel state: user plays a TEST, stops it,
        // then the prayer-time alarm starts the REAL adhan.
        reset()
        AdhanPlayback.setPlaying(true, voice = AdhanVoice.MAKKAH, forFajr = false, isPreview = true)
        AdhanPlayback.setPlaying(false)
        assertFalse(AdhanPlayback.isPlaying.value)
        assertFalse(AdhanPlayback.currentIsPreview.value)

        AdhanPlayback.setPlaying(true, voice = AdhanVoice.MAKKAH, forFajr = true, isPreview = false)
        assertTrue(AdhanPlayback.isPlaying.value)
        assertFalse(AdhanPlayback.currentIsPreview.value)
        assertTrue(AdhanPlayback.currentForFajr.value)
        reset()
    }

    @Test
    fun `a second start without explicit preview args defaults to real not test`() {
        // AdhanService.onStartCommand reads EXTRA_PREVIEW with a false default;
        // a real prayer-time start can never inherit a stale preview=true.
        reset()
        AdhanPlayback.setPlaying(true, voice = AdhanVoice.AQSA, forFajr = false, isPreview = true)
        AdhanPlayback.setPlaying(true, voice = AdhanVoice.AQSA, forFajr = false) // preview omitted
        assertFalse(AdhanPlayback.currentIsPreview.value)
        reset()
    }
}
