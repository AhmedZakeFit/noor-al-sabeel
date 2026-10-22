package io.github.ahmedzakefit.noor.core.hijri

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStreamReader
import java.time.LocalDate

/** Verifies the Umm al-Qura conversion against 731 reference pairs from hijridate (MIT). */
class UmmalquraCalendarTest {

    private fun rows(): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        InputStreamReader(
            requireNotNull(javaClass.classLoader.getResourceAsStream("fixtures/hijri_fixtures.csv")),
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
    fun `matches reference conversions and round-trips`() {
        var checked = 0
        for (r in rows()) {
            val g = LocalDate.parse(r[0])
            val hy = r[1].toInt()
            val hm = r[2].toInt()
            val hd = r[3].toInt()
            val h = UmmalquraCalendar.fromGregorian(g)
            assertEquals("$g", hy, h.year)
            assertEquals("$g", hm, h.month)
            assertEquals("$g", hd, h.day)
            assertFalse("$g should not be approximate", h.isApproximate)
            assertEquals("$g round-trip", g, UmmalquraCalendar.toGregorian(hy, hm, hd))
            checked++
        }
        assertTrue("too few rows: $checked", checked >= 700)
        println("[fixtures] hijri conversions checked=$checked")
    }

    @Test
    fun `known islamic events`() {
        // 1 Ramadan 1446
        assertEquals(1446, UmmalquraCalendar.fromGregorian(LocalDate.of(2025, 3, 1)).let { it.year * 0 + it.year })
        val ramadan1446 = UmmalquraCalendar.fromGregorian(LocalDate.of(2025, 3, 1))
        assertEquals(9, ramadan1446.month)
        assertEquals(1, ramadan1446.day)
        assertTrue(UmmalquraCalendar.isRamadan(LocalDate.of(2025, 3, 10)))
        // Eid al-Fitr 1446
        val eid = UmmalquraCalendar.fromGregorian(LocalDate.of(2025, 3, 30))
        assertEquals(10, eid.month)
        assertEquals(1, eid.day)
        // Eid al-Adha 1446
        val adha = UmmalquraCalendar.fromGregorian(LocalDate.of(2025, 6, 6))
        assertEquals(12, adha.month)
        assertEquals(10, adha.day)
    }

    @Test
    fun `out of range dates are approximate`() {
        assertTrue(UmmalquraCalendar.fromGregorian(LocalDate.of(1900, 1, 1)).isApproximate)
        assertTrue(UmmalquraCalendar.fromGregorian(LocalDate.of(2100, 1, 1)).isApproximate)
        assertNull(UmmalquraCalendar.toGregorian(1501, 1, 1))
        assertNotNull(UmmalquraCalendar.toGregorian(1447, 1, 1))
    }

    @Test
    fun `month lengths are sane`() {
        for (y in 1343..1499) {
            for (m in 1..12) {
                val len = UmmalquraCalendar.monthLength(y, m)
                assertTrue("($y,$m)=$len", len in 28..31)
            }
        }
    }
}
