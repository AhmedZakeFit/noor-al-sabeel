package io.github.ahmedzakefit.noor.notifications

import android.content.Context
import io.github.ahmedzakefit.noor.core.model.Prayer
import io.github.ahmedzakefit.noor.data.AdhanMode
import io.github.ahmedzakefit.noor.data.AdhanVoice

/**
 * Pure-Kotlin & resource mapping between prayers, reciter voice, and bundled adhan
 * audio resources (app/src/main/res/raw/).
 */
object AdhanAudio {
    const val RES_ADHAN = "adhan"
    const val RES_ADHAN_FAJR = "adhan_fajr"

    /**
     * Resolves the raw audio resource for [voice] and [prayer].
     * Fajr prayers use the dawn adhan containing "as-salatu khayrun min an-nawm".
     * If a specific reciter audio file is not present in res/raw, it gracefully
     * falls back to the standard bundled adhan.
     */
    fun rawResourceFor(context: Context, voice: AdhanVoice, prayer: Prayer): Int {
        val isFajr = prayer == Prayer.FAJR
        val name = when (voice) {
            AdhanVoice.MAKKAH -> if (isFajr) "adhan_makkah_fajr" else "adhan_makkah"
            AdhanVoice.MADINAH -> if (isFajr) "adhan_madinah_fajr" else "adhan_madinah"
            AdhanVoice.CAIRO -> if (isFajr) "adhan_cairo_fajr" else "adhan_cairo"
            AdhanVoice.AQSA -> if (isFajr) "adhan_aqsa_fajr" else "adhan_aqsa"
        }
        val id = context.resources.getIdentifier(name, "raw", context.packageName)
        if (id != 0) return id
        // Graceful fallback to the standard bundled audio files
        return if (isFajr) io.github.ahmedzakefit.noor.R.raw.adhan_fajr else io.github.ahmedzakefit.noor.R.raw.adhan
    }

    /**
     * Legacy helper kept for backward compatibility and unit tests.
     */
    fun resourceNameFor(prayer: Prayer): String =
        if (prayer == Prayer.FAJR) RES_ADHAN_FAJR else RES_ADHAN

    /**
     * Whether an alarm fire should raise the full adhan. Reminders (pre-prayer)
     * never do; SUNRISE is informational and never notifiable; and the user must
     * have chosen the adhan alert style.
     */
    fun shouldPlayAdhan(mode: AdhanMode, isReminder: Boolean, prayer: Prayer): Boolean =
        mode == AdhanMode.ADHAN && !isReminder && prayer.notifiable
}
