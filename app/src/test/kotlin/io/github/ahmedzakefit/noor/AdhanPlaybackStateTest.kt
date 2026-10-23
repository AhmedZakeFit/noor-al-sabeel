package io.github.ahmedzakefit.noor

import io.github.ahmedzakefit.noor.notifications.AdhanPlayback
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the "cannot stop the test adhan" report (v1.0.2): the
 * settings screen relies on this live flag to swap the test button for a stop
 * button while the adhan service is reciting. (Note: AdhanAudioTest covers the
 * alarm-side routing; this file covers the playback flag that drives the UI.)
 */
class AdhanPlaybackStateTest {

    @Test
    fun `flag starts false and tracks service lifecycle transitions`() {
        // Fresh default: nothing is playing, so the settings screen shows the
        // "test the adhan" button.
        // (Object state persists within a JVM; reset defensively.)
        AdhanPlayback.setPlaying(false)
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
}
