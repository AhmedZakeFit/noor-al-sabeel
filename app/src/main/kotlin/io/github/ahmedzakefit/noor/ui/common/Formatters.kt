package io.github.ahmedzakefit.noor.ui.common

import io.github.ahmedzakefit.noor.data.DigitSetting
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formatting helpers shared by all screens: clock times, dates, countdowns and
 * digit localization (Arabic-Indic vs Latin) driven by the user's preference.
 */
object Formatters {

    private const val ARABIC_DIGITS = "٠١٢٣٤٥٦٧٨٩"

    fun useArabicDigits(setting: DigitSetting, locale: Locale): Boolean = when (setting) {
        DigitSetting.ARABIC_INDIC -> true
        DigitSetting.LATIN -> false
        DigitSetting.FOLLOW_LOCALE -> locale.language == "ar"
    }

    fun digits(input: String, arabic: Boolean): String {
        if (!arabic) return input
        val b = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') b.append(ARABIC_DIGITS[ch - '0']) else b.append(ch)
        }
        return b.toString()
    }

    fun latinDigits(input: String): String {
        val b = StringBuilder(input.length)
        for (ch in input) {
            val idx = ARABIC_DIGITS.indexOf(ch)
            if (idx >= 0) b.append(('0' + idx)) else b.append(ch)
        }
        return b.toString()
    }

    fun amPmLabel(hour: Int, locale: Locale): String =
        DateTimeFormatter.ofPattern("a", locale).format(LocalTime.of(hour, 0))

    fun formatClock(hour: Int, minute: Int, use24h: Boolean, amPm: String?, arabic: Boolean): String {
        val base = if (use24h) {
            "%02d:%02d".format(Locale.ROOT, hour, minute)
        } else {
            val h12 = if (hour % 12 == 0) 12 else hour % 12
            "%d:%02d".format(Locale.ROOT, h12, minute) + (amPm?.let { " $it" } ?: "")
        }
        return digits(base, arabic)
    }

    fun formatClock(time: ZonedDateTime, use24h: Boolean, locale: Locale, arabic: Boolean): String =
        formatClock(time.hour, time.minute, use24h, if (use24h) null else amPmLabel(time.hour, locale), arabic)

    fun formatGregorian(date: LocalDate, locale: Locale, arabic: Boolean): String {
        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale)
        return digits(date.format(formatter), arabic)
    }

    fun formatHijri(
        year: Int,
        month: Int,
        day: Int,
        monthNames: List<String>,
        ahSuffix: String,
        arabic: Boolean,
    ): String {
        val name = monthNames.getOrNull(month - 1) ?: ""
        return digits("$day $name $year", arabic) + " " + ahSuffix
    }

    fun formatCountdown(totalMinutes: Long, pattern: (hours: Int, minutes: Int) -> String, arabic: Boolean): String {
        val hours = (totalMinutes / 60).toInt()
        val minutes = (totalMinutes % 60).toInt()
        return digits(pattern(hours, minutes), arabic)
    }

    fun formatDecimal(value: Double, decimals: Int = 1, arabic: Boolean): String =
        digits("%.${decimals}f".format(Locale.ROOT, value), arabic)

    fun formatDegrees(value: Double, arabic: Boolean): String =
        digits("%.1f".format(Locale.ROOT, value), arabic) + "°"
}
