package io.github.ahmedzakefit.noor.notifications

import io.github.ahmedzakefit.noor.data.AdhanVoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory playback state kept in sync by [AdhanService], so any screen (and
 * especially the settings screen that launched a test adhan) can show a live
 * "stop the adhan" control and indicate which specific voice/prayer is playing.
 */
object AdhanPlayback {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentVoice = MutableStateFlow<AdhanVoice?>(null)
    val currentVoice: StateFlow<AdhanVoice?> = _currentVoice.asStateFlow()

    private val _currentForFajr = MutableStateFlow(false)
    val currentForFajr: StateFlow<Boolean> = _currentForFajr.asStateFlow()

    fun setPlaying(playing: Boolean, voice: AdhanVoice? = null, forFajr: Boolean = false) {
        _isPlaying.value = playing
        _currentVoice.value = if (playing) voice else null
        _currentForFajr.value = if (playing) forFajr else false
    }
}
