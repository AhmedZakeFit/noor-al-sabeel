package io.github.ahmedzakefit.noor.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory playback flag kept in sync by [AdhanService], so any screen (and
 * especially the settings screen that launched a test adhan) can show a live
 * "stop the adhan" control.
 *
 * This exists because on Android 13+ the service's notification — and with it
 * its Stop action — is invisible when POST_NOTIFICATIONS has not been granted,
 * which previously left no in-app way to stop a test adhan.
 *
 * Process-scoped by design: if the process dies, playback is gone too, so
 * `false` is always the correct fresh-install value.
 */
object AdhanPlayback {

    private val _isPlaying = MutableStateFlow(false)

    /** True while the adhan service is reciting (real or test). */
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }
}
