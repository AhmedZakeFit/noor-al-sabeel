package io.github.ahmedzakefit.noor.core.calc

import io.github.ahmedzakefit.noor.core.model.GeoPosition
import io.github.ahmedzakefit.noor.core.model.Prayer
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.roundToLong
import kotlin.math.sqrt

/** Computed daily times. Midnight and last-third are informational extras. */
data class PrayerTimes(
    val date: LocalDate,
    val zone: ZoneId,
    val fajr: ZonedDateTime,
    val sunrise: ZonedDateTime,
    val dhuhr: ZonedDateTime,
    val asr: ZonedDateTime,
    val maghrib: ZonedDateTime,
    val isha: ZonedDateTime,
    val midnight: ZonedDateTime? = null,
    val lastThirdOfNight: ZonedDateTime? = null,
    val adjustedForHighLatitude: Boolean = false,
) {
    fun timeOf(prayer: Prayer): ZonedDateTime = when (prayer) {
        Prayer.FAJR -> fajr
        Prayer.SUNRISE -> sunrise
        Prayer.DHUHR -> dhuhr
        Prayer.ASR -> asr
        Prayer.MAGHRIB -> maghrib
        Prayer.ISHA -> isha
    }

    val entries: List<Pair<Prayer, ZonedDateTime>> get() = Prayer.entries.map { it to timeOf(it) }

    /** First notifiable prayer strictly after [now], or null when all of today's have passed. */
    fun nextPrayerAfter(now: ZonedDateTime): Pair<Prayer, ZonedDateTime>? =
        entries.filter { it.first.notifiable && it.second.isAfter(now) }.minByOrNull { it.second }
}

/**
 * Self-contained prayer-times engine. All math runs locally; no network access is required.
 *
 * Algorithm: solar transit (true noon) from the equation of time, then hour angles for the
 * twilight altitudes defined by each calculation convention. Times are produced in the
 * requested time zone and correctly handle DST transitions.
 */
class PrayerCalculator {

    fun compute(
        date: LocalDate,
        position: GeoPosition,
        parameters: CalculationParameters = CalculationParameters(),
        zone: ZoneId = ZoneId.of(position.timeZoneId),
    ): PrayerTimes {
        val today = rawDay(date, position, parameters)
        val tomorrow = rawDay(date.plusDays(1), position, parameters)

        val dayBase = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        fun zoned(utMinutes: Double): ZonedDateTime =
            ZonedDateTime.ofInstant(dayBase.plusSeconds((utMinutes * 60.0).roundToLong()), zone)

        fun withOffset(prayer: Prayer, utMinutes: Double): ZonedDateTime =
            zoned(utMinutes + parameters.offsetOf(prayer))

        val sunriseUT = today.sunriseUTmin
        val sunsetUT = today.sunsetUTmin
        val fajrUT = today.fajrUTmin
        val asrUT = today.asrUTmin
        val ishaAngleUT = today.ishaUTmin

        val maghribUT = sunsetUT
        val ishaMinutes = parameters.ishaMinutesAfterMaghrib
        val ishaUT = if (ishaMinutes != null) {
            maghribUT + ishaMinutes
        } else {
            ishaAngleUT
        }

        val maghribWithOffset = maghribUT + parameters.offsetOf(Prayer.MAGHRIB)
        val tomorrowFajrWithOffset = tomorrow.fajrUTmin + parameters.offsetOf(Prayer.FAJR)
        val nightMinutes = tomorrowFajrWithOffset - maghribWithOffset
        val midnightZ = zoned(maghribWithOffset + nightMinutes / 2.0)
        val lastThirdZ = zoned(tomorrowFajrWithOffset - nightMinutes / 3.0)

        return PrayerTimes(
            date = date,
            zone = zone,
            fajr = withOffset(Prayer.FAJR, fajrUT),
            sunrise = withOffset(Prayer.SUNRISE, sunriseUT),
            dhuhr = withOffset(Prayer.DHUHR, today.transitUTmin),
            asr = withOffset(Prayer.ASR, asrUT),
            maghrib = withOffset(Prayer.MAGHRIB, maghribUT),
            isha = withOffset(Prayer.ISHA, ishaUT),
            midnight = midnightZ,
            lastThirdOfNight = lastThirdZ,
            adjustedForHighLatitude = today.highLatAdjusted,
        )
    }

    /** Raw per-day values expressed as minutes elapsed since 00:00 UT of that date. */
    private class RawDay(
        val transitUTmin: Double,
        var fajrUTmin: Double = 0.0,
        var sunriseUTmin: Double = 0.0,
        var sunsetUTmin: Double = 0.0,
        var asrUTmin: Double = 0.0,
        var ishaUTmin: Double = 0.0,
        var highLatAdjusted: Boolean = false,
    )

    private fun rawDay(date: LocalDate, position: GeoPosition, p: CalculationParameters): RawDay {
        val jd0 = SolarMath.julianDayAtNoonUT(date)
        var sc = SolarMath.solarCoordinates(jd0)
        var transit = 720.0 - 4.0 * position.longitude - sc.equationOfTimeMinutes

        // Refine solar coordinates at the actual transit instant (one iteration is plenty).
        sc = SolarMath.solarCoordinates(jd0 + (transit - 720.0) / 1440.0)
        transit = 720.0 - 4.0 * position.longitude - sc.equationOfTimeMinutes

        val decl = sc.declinationDeg
        val lat = position.latitude
        // Standard sunrise/sunset altitude: refraction + solar semi-diameter, adjusted for elevation.
        val riseSetAltDeg = -0.833 - 0.0347 * sqrt(position.elevationMeters)

        val day = RawDay(transit)

        var sunrise: Double? = null
        var sunset: Double? = null
        SolarMath.hourAngleHours(lat, decl, riseSetAltDeg)?.let {
            sunrise = transit - it * 60.0
            sunset = transit + it * 60.0
        }
        var fajr: Double? = SolarMath.hourAngleHours(lat, decl, -p.fajrAngle)?.let { transit - it * 60.0 }
        var asr: Double? = SolarMath.hourAngleHours(
            lat, decl, SolarMath.asrAltitudeDeg(lat, decl, p.asrFactor.toDouble()),
        )?.let { transit + it * 60.0 }
        var ishaByAngle: Double? = null
        if (p.ishaMinutesAfterMaghrib == null) {
            ishaByAngle = SolarMath.hourAngleHours(lat, decl, -(p.ishaAngle ?: 18.0))?.let { transit + it * 60.0 }
        }

        // High-latitude handling
        if (sunrise == null || sunset == null) {
            // Above the polar circles the sun may not rise/set at all; approximate with a 24h day.
            // This is a documented approximation for extreme latitudes only.
            sunrise = transit - 720.0
            sunset = transit + 720.0
            day.highLatAdjusted = true
        }
        val nightHours = (1440.0 - (sunset!! - sunrise!!)) / 60.0
        val fajrPortion: Double
        val ishaPortion: Double
        when (p.highLatitudeRule) {
            HighLatitudeRule.ANGLE_BASED -> {
                fajrPortion = p.fajrAngle / 60.0
                ishaPortion = (p.ishaAngle ?: p.fajrAngle) / 60.0
            }
            HighLatitudeRule.MIDDLE_OF_NIGHT -> {
                fajrPortion = 0.5
                ishaPortion = 0.5
            }
            HighLatitudeRule.SEVENTH_OF_NIGHT -> {
                fajrPortion = 1.0 / 7.0
                ishaPortion = 1.0 / 7.0
            }
        }
        val safeFajr = sunrise!! - fajrPortion * nightHours * 60.0
        val safeIsha = sunset!! + ishaPortion * nightHours * 60.0
        if (fajr == null || fajr!! < safeFajr) {
            fajr = safeFajr
            day.highLatAdjusted = true
        }
        if (ishaByAngle == null || ishaByAngle!! > safeIsha) {
            ishaByAngle = safeIsha
            day.highLatAdjusted = true
        }

        day.fajrUTmin = fajr!!
        day.sunriseUTmin = sunrise!!
        day.sunsetUTmin = sunset!!
        if (asr == null) {
            // Polar night fallback: the sun never reaches the Asr altitude.
            asr = transit + (sunset!! - transit) / 2.0
            day.highLatAdjusted = true
        }
        day.asrUTmin = asr!!
        day.ishaUTmin = ishaByAngle ?: (sunset!! + (p.ishaMinutesAfterMaghrib ?: 90))
        return day
    }
}
