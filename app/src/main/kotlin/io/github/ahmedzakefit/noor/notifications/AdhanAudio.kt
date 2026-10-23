package io.github.ahmedzakefit.noor.notifications

import io.github.ahmedzakefit.noor.core.model.Prayer
import io.github.ahmedzakefit.noor.data.AdhanMode

/**
 * Pure-Kotlin mapping between prayers and the bundled adhan audio resources
 * (app/src/main/res/raw/). Deliberately free of Android imports so the
 * routing logic stays unit-testable on the JVM.
 */
object AdhanAudio {

    /** Raw-resource base name of the standard full adhan (adhan.mp3). */
    const val RES_ADHAN = "adhan"

    /** Raw-resource base name of the dawn adhan (adhan_fajr.mp3). */
    const val RES_ADHAN_FAJR = "adhan_fajr"

    /**
     * Fajr uses its own adhan which adds "as-salatu khayrun min an-nawm"
     * (prayer is better than sleep); every other prayer shares the standard one.
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
