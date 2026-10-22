package io.github.ahmedzakefit.noor.core.qibla

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStreamReader

/** Verifies qibla bearings against the AlAdhan API reference values. */
class QiblaFixtureTest {

    private fun rows(): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        InputStreamReader(
            requireNotNull(javaClass.classLoader.getResourceAsStream("fixtures/qibla_fixtures.csv")),
            Charsets.UTF_8,
        ).use { r ->
            var first = true
            for (line in r.readLines()) {
                if (first) {
                    first = false
                    continue
                }
                if (line.isNotBlank()) rows.add(line.split(","))
            }
        }
        return rows
    }

    @Test
    fun `bearings match aladhan references within half a degree`() {
        var checked = 0
        for (r in rows()) {
            val city = r[0]
            val lat = r[1].toDouble()
            val lon = r[2].toDouble()
            val expected = r[3].toDouble()
            // Skip the degenerate Makkah row: the qibla from the Kaaba itself is undefined.
            if (QiblaMath.haversineKm(lat, lon, QiblaMath.KAABA_LATITUDE, QiblaMath.KAABA_LONGITUDE) < 2.0) {
                continue
            }
            val actual = QiblaMath.qiblaBearing(lat, lon)
            var diff = kotlin.math.abs(actual - expected)
            if (diff > 180) diff = 360 - diff
            assertTrue("$city qibla: expected $expected got $actual", diff <= 0.5)
            checked++
        }
        assertTrue(checked >= 14)
        println("[fixtures] qibla bearings checked=$checked")
    }

    @Test
    fun `known bearings sanity`() {
        // Cairo ~ 136 deg, Jakarta ~ 295 deg (west-north-west).
        assertEquals(136.1, QiblaMath.qiblaBearing(30.0444, 31.2357), 1.0)
        assertEquals(295.0, QiblaMath.qiblaBearing(-6.2088, 106.8456), 2.0)
        // Distance sanity: Cairo to Kaaba is roughly 1300 km.
        assertEquals(1300.0, QiblaMath.distanceToKaabaKm(30.0444, 31.2357), 60.0)
    }
}
