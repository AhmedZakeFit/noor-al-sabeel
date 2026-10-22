package io.github.ahmedzakefit.noor.core.calc

import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Low-precision solar position math based on Jean Meeus, "Astronomical Algorithms"
 * (2nd ed., chapters 25 and 28). Accuracy is about 0.01 degrees, which translates
 * to well under a minute of time for prayer calculations.
 */
object SolarMath {

    /** Julian Day at 12:00 UT of [date] (an integer value by astronomical convention). */
    fun julianDayAtNoonUT(date: LocalDate): Double {
        var y = date.year
        var m = date.monthValue
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = kotlin.math.floor(y / 100.0)
        val b = 2.0 - a + kotlin.math.floor(a / 4.0)
        // The classic Meeus formula returns JD at 0h UT (fraction .5); +0.5 moves it to noon UT.
        return kotlin.math.floor(365.25 * (y + 4716)) + kotlin.math.floor(30.6001 * (m + 1)) +
            date.dayOfMonth + b - 1524.5 + 0.5
    }

    class SolarCoordinates(val declinationDeg: Double, val equationOfTimeMinutes: Double)

    /** Sun declination (degrees) and equation of time (minutes) for the given Julian Day. */
    fun solarCoordinates(jd: Double): SolarCoordinates {
        val t = (jd - 2451545.0) / 36525.0

        val l0Deg = normalizeDegrees(280.46645 + 36000.76983 * t + 0.0003032 * t * t)
        val mRad = Math.toRadians(357.52910 + 35999.05030 * t - 0.0001559 * t * t - 0.00000048 * t * t * t)
        val e = 0.016708617 - 0.000042037 * t - 0.0000001236 * t * t

        // Equation of center (degrees)
        val cDeg = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(mRad) +
            (0.019993 - 0.000101 * t) * sin(2 * mRad) +
            0.000290 * sin(3 * mRad)

        val trueLongDeg = l0Deg + cDeg
        val omegaRad = Math.toRadians(125.04 - 1934.136 * t)
        // Apparent longitude (aberration + nutation in longitude)
        val lambdaDeg = trueLongDeg - 0.00569 - 0.00478 * sin(omegaRad)

        val eps0Deg = 23.0 + 26.0 / 60.0 + 21.448 / 3600.0 -
            (46.8150 * t + 0.00059 * t * t - 0.001813 * t * t * t) / 3600.0
        val epsDeg = eps0Deg + 0.00256 * cos(omegaRad)

        val declinationDeg = Math.toDegrees(asin(sin(Math.toRadians(epsDeg)) * sin(Math.toRadians(lambdaDeg))))

        // Equation of time in radians -> minutes (4 minutes per degree)
        val y = tan(Math.toRadians(epsDeg) / 2.0) * tan(Math.toRadians(epsDeg) / 2.0)
        val eotRad = y * sin(2 * Math.toRadians(l0Deg)) - 2 * e * sin(mRad) +
            4 * e * y * sin(mRad) * cos(2 * Math.toRadians(l0Deg)) -
            0.5 * y * y * sin(4 * Math.toRadians(l0Deg)) -
            1.25 * e * e * sin(2 * mRad)
        val eotMinutes = 4.0 * Math.toDegrees(eotRad)

        return SolarCoordinates(declinationDeg, eotMinutes)
    }

    /**
     * Hour angle (hours from solar noon) at which the sun reaches [altitudeDeg].
     * Returns null when the sun never reaches that altitude at this location on this day.
     */
    fun hourAngleHours(latitudeDeg: Double, declinationDeg: Double, altitudeDeg: Double): Double? {
        val lat = Math.toRadians(latitudeDeg)
        val dec = Math.toRadians(declinationDeg)
        val alt = Math.toRadians(altitudeDeg)
        val cosH = (sin(alt) - sin(lat) * sin(dec)) / (cos(lat) * cos(dec))
        if (cosH < -1.0 || cosH > 1.0) return null
        return Math.toDegrees(acos(cosH)) / 15.0
    }

    /**
     * Solar altitude (degrees) satisfying the Asr shadow definition:
     * shadow length = [shadowFactor] x the height of the object (1 = Standard, 2 = Hanafi).
     */
    fun asrAltitudeDeg(latitudeDeg: Double, declinationDeg: Double, shadowFactor: Double): Double {
        val delta = abs(latitudeDeg - declinationDeg)
        return Math.toDegrees(atan(1.0 / (shadowFactor + tan(Math.toRadians(delta)))))
    }

    private fun normalizeDegrees(d: Double): Double {
        var v = d % 360.0
        if (v < 0) v += 360.0
        return v
    }
}
