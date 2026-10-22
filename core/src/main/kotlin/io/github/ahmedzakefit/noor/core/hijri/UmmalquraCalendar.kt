package io.github.ahmedzakefit.noor.core.hijri

import io.github.ahmedzakefit.noor.core.calc.SolarMath
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Umm al-Qura (Saudi civil) Hijri calendar conversion covering 1343-1499 AH (1924 CE onward).
 *
 * The month-length table encodes the official Umm al-Qura calendar published by KACST.
 * It is derived from the open-source hijri-converter project (MIT license) - see NOTICE.md.
 * Each character encodes one month's length: '0' = 28, '1' = 29, '2' = 30, '3' = 31 days
 * (the historical 1343-1364 AH era contains a few irregular months).
 *
 * Dates outside the covered range fall back to an arithmetic (tabular civil) approximation
 * flagged with [HijriDate.isApproximate]; users may additionally apply a manual day adjustment
 * in the UI to align with local moon sighting.
 */
object UmmalquraCalendar {

    private const val FIRST_YEAR = 1343
    private const val LAST_YEAR = 1499

    /** 1 Muharram 1343 AH. */
    private val ANCHOR: LocalDate = LocalDate.of(1924, 8, 1)

    private val TABLE: String = "212212220222112121212121212131202212112212211221112212212211212121221130212121222032121211212212212121112212212121212121212212122112121212212121212121212212112121221121212121212122221211211221222121121121212121212122212121212121212121212121212121212122212121202221212121212122212121212121212121212121212121212122212121221221212121212121212112121222121212112122121212121212212121221122212121211221121122212121211212122122212121212121121212121212212121212121212212112121212212211212121221212121212121212121212211212221221121212122112121212122122121212121212121212122212121221211212121212122112121212122212111212122212121211221212212112121212221211212121221212121212121221212121212121221221211212122121212112121222121211212122212121121122122212112112212212121212121212212121212121212212121211212212212121121212221212112121221221211211221222121121122122122112112122212121211212212212121211212212122121211212122212121121212212212121121222212112111222212211211122212212121121212212212112121212212122121121212212212112121221221211211222122121121122122122112121212122121212112122212121211212212212121121212212211212121212212121212121212212211212112212221121121212221212112121222121211212122122121121212122121212121212122121221121212122212112112122212211211212221212121121221221212112121221221212112121221221211212121222121121212122211212121122212121212112212122121211212122212121121122122212112112212221211211221221212121122121221212121212121212212121121221221212112121222121211211222212121121212212212112121212212121212112212212211211221212221121121221221212121121221212212112121221221211212121222121121121222122112112122122211211212212212121121212212122112121212122122121121212212212112112212221211211221221221121122121222112121212122121212112122122121211212122212121121212212212112121212212122112121212212211212112212221121121212221212112121222121211212122122112121212122121212121212121221"

    private val TABLE_END: LocalDate = run {
        var days = 0L
        for (y in FIRST_YEAR..LAST_YEAR) days += yearLength(y)
        ANCHOR.plusDays(days - 1)
    }

    data class HijriDate(val year: Int, val month: Int, val day: Int, val isApproximate: Boolean)

    /** Convert a Gregorian date to the Umm al-Qura Hijri calendar. */
    fun fromGregorian(date: LocalDate): HijriDate {
        if (date < ANCHOR || date > TABLE_END) return arithmeticFromGregorian(date)
        var days = ChronoUnit.DAYS.between(ANCHOR, date).toInt()
        var y = FIRST_YEAR
        while (y <= LAST_YEAR) {
            val ylen = yearLength(y)
            if (days < ylen) break
            days -= ylen
            y++
        }
        if (y > LAST_YEAR) return arithmeticFromGregorian(date)
        var m = 1
        while (m <= 12) {
            val mlen = monthLengthAt(y, m)
            if (days < mlen) break
            days -= mlen
            m++
        }
        return HijriDate(y, m, days + 1, false)
    }

    /** Convert a Hijri date to Gregorian; returns null when outside the table's range. */
    fun toGregorian(year: Int, month: Int, day: Int): LocalDate? {
        if (year !in FIRST_YEAR..LAST_YEAR || month !in 1..12) return null
        val mlen = monthLengthAt(year, month)
        if (day !in 1..mlen) return null
        var days = 0L
        for (y in FIRST_YEAR until year) days += yearLength(y)
        for (m in 1 until month) days += monthLengthAt(year, m)
        return ANCHOR.plusDays(days + day - 1)
    }

    /** True when [date] falls in Ramadan according to the official table (not approximate). */
    fun isRamadan(date: LocalDate): Boolean {
        val h = fromGregorian(date)
        return !h.isApproximate && h.month == 9
    }

    fun monthLength(year: Int, month: Int): Int = monthLengthAt(year, month)

    private fun monthLengthAt(year: Int, month: Int): Int =
        28 + (TABLE[(year - FIRST_YEAR) * 12 + (month - 1)] - '0')

    private fun yearLength(year: Int): Int {
        val start = (year - FIRST_YEAR) * 12
        var len = 0
        for (i in 0 until 12) len += 28 + (TABLE[start + i] - '0')
        return len
    }

    /**
     * Arithmetic (tabular civil) Islamic calendar used outside the table's range.
     * Classic "Kuwaiti algorithm" - accurate to about one day.
     */
    private fun arithmeticFromGregorian(date: LocalDate): HijriDate {
        val jd = SolarMath.julianDayAtNoonUT(date)
        val days = (jd - 1948440.0 + 10632.0).toInt()
        val n = (days - 1) / 10631
        var d = days - 10631 * n + 354
        val j = ((10985 - d) / 5316) * ((50 * d) / 17719) + (d / 5670) * ((43 * d) / 15238)
        d = d - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        val m = (24 * d) / 709
        val day = d - (709 * m) / 24
        val y = 30 * n + j - 30
        return HijriDate(y, m, day, true)
    }
}
