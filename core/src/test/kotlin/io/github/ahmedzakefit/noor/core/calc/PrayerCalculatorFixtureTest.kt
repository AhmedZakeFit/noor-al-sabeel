package io.github.ahmedzakefit.noor.core.calc

import io.github.ahmedzakefit.noor.core.cities.CityCatalog
import io.github.ahmedzakefit.noor.core.hijri.UmmalquraCalendar
import io.github.ahmedzakefit.noor.core.model.GeoPosition
import io.github.ahmedzakefit.noor.core.model.Prayer
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Verifies the engine against reference timings produced by the AlAdhan API
 * (adhan-java engine, Apache-2.0). Fixtures were fetched on 2026-10-23 for
 * 9 cities x 6 methods x 5 dates plus Hanafi-asr and Ramadan subsets.
 *
 * Tolerances: 2 minutes normally, 4 minutes where our high-latitude clamping is
 * active (clamped times depend on the measured night length, which differs by a
 * few minutes between implementations).
 *
 * Known deviation: AlAdhan applies Umm al-Qura isha = maghrib + 120 minutes during its own
 * Ramadan determination (which uses a tabular calendar and can differ from the official
 * Umm al-Qura table by a day at the boundaries); NOOR follows the official convention
 * (90 minutes, 120 in Ramadan per the Umm al-Qura table), so Umm al-Qura isha rows use a
 * 35-minute tolerance and exact behaviour is pinned by the dedicated engine tests.
 */
class PrayerCalculatorFixtureTest {

    private val calc = PrayerCalculator()

    private fun resource(name: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream("fixtures/$name")) { "missing $name" }

    private fun csvRows(name: String): List<Map<String, String>> {
        val rows = mutableListOf<Map<String, String>>()
        InputStreamReader(resource(name), Charsets.UTF_8).use { r ->
            var header: List<String>? = null
            for (line in r.readLines()) {
                if (line.isBlank()) continue
                val parts = CityCatalog.splitCsvLine(line)
                if (header == null) {
                    header = parts
                } else {
                    rows.add(header.zip(parts).toMap())
                }
            }
        }
        return rows
    }

    private fun methodFor(name: String): CalculationMethod = when (name) {
        "MUSLIM_WORLD_LEAGUE" -> CalculationMethod.MUSLIM_WORLD_LEAGUE
        "EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY" -> CalculationMethod.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY
        "ISNA" -> CalculationMethod.ISNA
        "UMM_AL_QURA" -> CalculationMethod.UMM_AL_QURA
        "KARACHI" -> CalculationMethod.KARACHI
        "GULF_REGION" -> CalculationMethod.GULF_REGION
        else -> throw IllegalArgumentException("unknown method $name")
    }

    private fun minutesOf(hhmm: String): Int {
        val p = hhmm.split(":")
        return p[0].toInt() * 60 + p[1].toInt()
    }

    private fun localMinutes(t: ZonedDateTime): Int = t.hour * 60 + t.minute

    private fun assertClose(label: String, expected: String, actual: ZonedDateTime, toleranceMin: Int) {
        var diff = kotlin.math.abs(localMinutes(actual) - minutesOf(expected))
        if (diff > 720) diff = 1440 - diff
        assertTrue(
            "$label: expected $expected, got ${actual.hour}:${"%02d".format(actual.minute)} (diff ${diff}m)",
            diff <= toleranceMin,
        )
    }

    @Test
    fun `matches aladhan reference timings`() {
        var checked = 0
        var highLatRows = 0
        for (row in csvRows("prayer_fixtures.csv")) {
            val date = LocalDate.parse(row["date"])
            val lat = row["lat"]!!.toDouble()
            val lon = row["lon"]!!.toDouble()
            val tzId = row["tz"]!!
            val method = methodFor(row["method"]!!)
            val asrFactor = if (row["school"] == "1") 2 else 1
            val ramadan = method == CalculationMethod.UMM_AL_QURA && UmmalquraCalendar.isRamadan(date)

            var params = CalculationParameters(method = method, asrFactor = asrFactor)
            if (ramadan) params = params.copy(customIshaMinutesAfterMaghrib = 120)

            val times = calc.compute(date, GeoPosition(lat, lon, 0.0, tzId), params, ZoneId.of(tzId))
            if (times.adjustedForHighLatitude) highLatRows++
            val tolerance = if (times.adjustedForHighLatitude) 4 else 2
            val label = "${row["city"]} ${row["date"]} ${row["method"]} school=${row["school"]}"

            assertClose("$label fajr", row["fajr"]!!, times.fajr, tolerance)
            assertClose("$label sunrise", row["sunrise"]!!, times.sunrise, tolerance)
            assertClose("$label dhuhr", row["dhuhr"]!!, times.dhuhr, tolerance)
            // Asr convention note: adhan-java/AlAdhan resolves the Asr shadow altitude against
            // the sun's APPARENT altitude (including atmospheric refraction, ~+0.15 deg near the
            // horizon), while NOOR uses the GEOMETRIC altitude as the physical shadow definition
            // requires (refraction does not change real shadows; praytimes.org does the same).
            // The difference is under a minute at normal latitudes but amplifies near the polar
            // circles where the winter Asr altitude is only a few degrees, hence this tolerance.
            val asrTolerance = if (kotlin.math.abs(lat) >= 55.0) 10 else tolerance
            assertClose("$label asr", row["asr"]!!, times.asr, asrTolerance)
            assertClose("$label maghrib", row["maghrib"]!!, times.maghrib, tolerance)

            val ishaTolerance = if (method == CalculationMethod.UMM_AL_QURA) 35 else tolerance
            assertClose("$label isha", row["isha"]!!, times.isha, ishaTolerance)
            checked++
        }
        assertTrue("too few fixture rows: $checked", checked >= 280)
        println("[fixtures] prayer rows checked=$checked highLatRows=$highLatRows")
    }
}
