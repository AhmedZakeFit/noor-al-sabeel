package io.github.ahmedzakefit.noor

import io.github.ahmedzakefit.noor.data.DigitSetting
import io.github.ahmedzakefit.noor.ui.common.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class FormattersTest {

    @Test
    fun `digit localization maps latin to arabic and back`() {
        assertEquals("٠١٢٣٤٥٦٧٨٩", Formatters.digits("0123456789", arabic = true))
        assertEquals("0123456789", Formatters.digits("0123456789", arabic = false))
        assertEquals("١٢:٣٤", Formatters.digits("12:34", arabic = true))
        assertEquals("12:34", Formatters.latinDigits("١٢:٣٤"))
    }

    @Test
    fun `clock formatting 24h and 12h`() {
        assertEquals("14:05", Formatters.formatClock(14, 5, use24h = true, amPm = null, arabic = false))
        assertEquals("١٤:٠٥", Formatters.formatClock(14, 5, use24h = true, amPm = null, arabic = true))
        assertEquals("2:30 PM", Formatters.formatClock(14, 30, use24h = false, amPm = "PM", arabic = false))
        assertEquals("12:00 AM", Formatters.formatClock(0, 0, use24h = false, amPm = "AM", arabic = false))
        assertEquals("12:45", Formatters.formatClock(12, 45, use24h = false, amPm = null, arabic = false))
    }

    @Test
    fun `digit setting follows locale`() {
        assertTrue(Formatters.useArabicDigits(DigitSetting.FOLLOW_LOCALE, Locale("ar")))
        assertFalse(Formatters.useArabicDigits(DigitSetting.FOLLOW_LOCALE, Locale("en")))
        assertTrue(Formatters.useArabicDigits(DigitSetting.ARABIC_INDIC, Locale("en")))
        assertFalse(Formatters.useArabicDigits(DigitSetting.LATIN, Locale("ar")))
    }

    @Test
    fun `countdown formatting uses pattern with digits`() {
        val out = Formatters.formatCountdown(95, { h, m -> "${h}h ${m}m" }, arabic = true)
        assertEquals("١h ٣٥m", out)
    }

    @Test
    fun `hijri formatting composes day month year`() {
        val months = listOf(
            "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Awwal",
            "Jumada al-Thani", "Rajab", "Shaban", "Ramadan", "Shawwal", "Dhul-Qadah", "Dhul-Hijjah",
        )
        assertEquals("15 Ramadan 1447 AH", Formatters.formatHijri(1447, 9, 15, months, "AH", arabic = false))
        val monthsAr = listOf(
            "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
            "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة",
        )
        assertEquals("١٥ رمضان ١٤٤٧ هـ", Formatters.formatHijri(1447, 9, 15, monthsAr, "هـ", arabic = true))
    }

    @Test
    fun `decimal and degrees formatting`() {
        assertEquals("1.2", Formatters.formatDecimal(1.234, 1, arabic = false))
        assertEquals("١٬٢".let { Formatters.formatDecimal(1.2, 1, arabic = true) }, Formatters.formatDecimal(1.2, 1, arabic = true))
        assertEquals("136.1°", Formatters.formatDegrees(136.14, arabic = false))
    }
}
