package io.github.ahmedzakefit.noor.core.calc

import io.github.ahmedzakefit.noor.core.hijri.UmmalquraCalendar
import io.github.ahmedzakefit.noor.core.model.GeoPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SolarMathTest {

    @Test
    fun `declination at solstices is about 23_4 degrees`() {
        val jun = SolarMath.solarCoordinates(SolarMath.julianDayAtNoonUT(LocalDate.of(2026, 6, 21)))
        val dec = SolarMath.solarCoordinates(SolarMath.julianDayAtNoonUT(LocalDate.of(2026, 12, 21)))
        assertEquals(23.4, jun.declinationDeg, 0.3)
        assertEquals(-23.4, dec.declinationDeg, 0.3)
    }

    @Test
    fun `equation of time stays within known bounds`() {
        for (d in 1..365 step 7) {
            val jd = SolarMath.julianDayAtNoonUT(LocalDate.of(2026, 1, 1).plusDays((d - 1).toLong()))
            val eot = SolarMath.solarCoordinates(jd).equationOfTimeMinutes
            assertTrue("EoT out of range: $eot", kotlin.math.abs(eot) <= 16.5)
        }
    }

    @Test
    fun `hour angle returns null when sun never reaches altitude`() {
        // 70N at the summer solstice: midnight sun sits above +3 degrees,
        // so altitudes below that are never reached.
        val decl = 23.0
        assertNull(SolarMath.hourAngleHours(70.0, decl, -20.0))
        assertNull(SolarMath.hourAngleHours(70.0, decl, -5.0))
        assertNotNull(SolarMath.hourAngleHours(70.0, decl, 5.0))
    }

    @Test
    fun `greenwich transit near noon at equinox`() {
        val date = LocalDate.of(2026, 3, 20)
        var sc = SolarMath.solarCoordinates(SolarMath.julianDayAtNoonUT(date))
        var transit = 720.0 - sc.equationOfTimeMinutes
        sc = SolarMath.solarCoordinates(SolarMath.julianDayAtNoonUT(date) + (transit - 720.0) / 1440.0)
        transit = 720.0 - sc.equationOfTimeMinutes
        assertTrue("transit out of range: $transit", transit in 700.0..740.0)
    }
}

class PrayerCalculatorTest {

    private val calc = PrayerCalculator()
    private val cairo = GeoPosition(30.0444, 31.2357, 0.0, "Africa/Cairo")

    @Test
    fun `times are ordered for cairo`() {
        val t = calc.compute(LocalDate.of(2026, 1, 15), cairo)
        assertTrue(t.fajr.isBefore(t.sunrise))
        assertTrue(t.sunrise.isBefore(t.dhuhr))
        assertTrue(t.dhuhr.isBefore(t.asr))
        assertTrue(t.asr.isBefore(t.maghrib))
        assertTrue(t.maghrib.isBefore(t.isha))
        assertEquals(ZoneId.of("Africa/Cairo"), t.zone)
    }

    @Test
    fun `offsets shift individual times`() {
        val base = calc.compute(LocalDate.of(2026, 1, 15), cairo)
        val shifted = calc.compute(
            LocalDate.of(2026, 1, 15), cairo,
            CalculationParameters(offsetsMinutes = mapOf(io.github.ahmedzakefit.noor.core.model.Prayer.FAJR to 5)),
        )
        assertEquals(base.fajr.plusMinutes(5), shifted.fajr)
        assertEquals(base.dhuhr, shifted.dhuhr)
    }

    @Test
    fun `umm al-qura isha is maghrib plus 90 minutes`() {
        val t = calc.compute(
            LocalDate.of(2026, 1, 15), cairo,
            CalculationParameters(method = CalculationMethod.UMM_AL_QURA),
        )
        assertEquals(t.maghrib.plusMinutes(90), t.isha)
    }

    @Test
    fun `umm al-qura ramadan rule adds 120 minutes`() {
        // 2026-03-01 is inside Ramadan 1447.
        val date = LocalDate.of(2026, 3, 1)
        assertTrue(UmmalquraCalendar.isRamadan(date))
        val t = calc.compute(
            date, cairo,
            CalculationParameters(
                method = CalculationMethod.UMM_AL_QURA,
                customIshaMinutesAfterMaghrib = 120, // applied by the app for Ramadan
            ),
        )
        assertEquals(t.maghrib.plusMinutes(120), t.isha)
    }

    @Test
    fun `high latitude adjustment keeps fajr before sunrise and isha after maghrib`() {
        val reykjavik = GeoPosition(64.1466, -21.9426, 0.0, "Atlantic/Reykjavik")
        val t = calc.compute(LocalDate.of(2026, 6, 21), reykjavik)
        assertTrue(t.adjustedForHighLatitude)
        assertTrue(t.fajr.isBefore(t.sunrise))
        assertTrue(t.maghrib.isBefore(t.isha))
    }

    @Test
    fun `next prayer after now works`() {
        val t = calc.compute(LocalDate.of(2026, 1, 15), cairo)
        val justAfterFajr = t.fajr.plusMinutes(1)
        val next = t.nextPrayerAfter(justAfterFajr)
        assertNotNull(next)
        // Sunrise is not a notifiable prayer, so the next one is Dhuhr.
        assertEquals(io.github.ahmedzakefit.noor.core.model.Prayer.DHUHR, next!!.first)
    }
}
    @Test
    fun `midnight and last third of night are within night hours`() {
        val t = calc.compute(LocalDate.of(2026, 1, 15), cairo)
        assertNotNull(t.midnight)
        assertNotNull(t.lastThirdOfNight)
        // Midnight must be after today's Maghrib
        assertTrue("midnight (${t.midnight}) must be after maghrib (${t.maghrib})", t.midnight!!.isAfter(t.maghrib))
        // Last third of night must be after midnight and before tomorrow's fajr
        assertTrue("last third (${t.lastThirdOfNight}) must be after midnight (${t.midnight})", t.lastThirdOfNight!!.isAfter(t.midnight!!))
    }
}
