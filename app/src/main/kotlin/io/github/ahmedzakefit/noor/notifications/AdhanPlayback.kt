package io.github.ahmedzakefit.noor.notifications

import io.github.ahmedzakefit.noor.data.AdhanVoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory playback state kept in sync by [AdhanService], so any screen (and
 * especially the settings screen that launched a test adhan) can show a live
 * "stop the adhan" control and indicate which specific voice/prayer is playing.
 *
 * v1.0.17 regression fix: the state now also records whether the running adhan
 * is a user-initiated TEST (preview) or the REAL prayer-time adhan. Before this,
 * the settings screen labelled every playing adhan — including the real one that
 * starts by itself at prayer time — as "test adhan", which users reported as
 * "the test adhan starts automatically after the real adhan". The preview flag
 * lets the UI tell the two cases apart and refuse to disturb a running real
 * adhan with a test.
 */
object AdhanPlayback {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentVoice = MutableStateFlow<AdhanVoice?>(null)
    val currentVoice: StateFlow<AdhanVoice?> = _currentVoice.asStateFlow()

    private val _currentForFajr = MutableStateFlow(false)
    val currentForFajr: StateFlow<Boolean> = _currentForFajr.asStateFlow()

    /** True while the running adhan is a test/preview, false while it is the
     * real prayer-time adhan. Meaningful only while [isPlaying] is true. */
    private val _currentIsPreview = MutableStateFlow(false)
    val currentIsPreview: StateFlow<Boolean> = _currentIsPreview.asStateFlow()

    fun setPlaying(
        playing: Boolean,
        voice: AdhanVoice? = null,
        forFajr: Boolean = false,
        isPreview: Boolean = false,
    ) {
        _isPlaying.value = playing
        _currentVoice.value = if (playing) voice else null
        _currentForFajr.value = if (playing) forFajr else false
        _currentIsPreview.value = if (playing) isPreview else false
    }
}
