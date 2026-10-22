package io.github.ahmedzakefit.noor.core.cities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStreamReader

class CityCatalogTest {

    private fun catalog(): List<City> =
        CityCatalog.parse(
            InputStreamReader(
                requireNotNull(javaClass.classLoader.getResourceAsStream("fixtures/cities.csv")),
                Charsets.UTF_8,
            ),
        )

    @Test
    fun `parses the full catalog`() {
        val cities = catalog()
        assertTrue("expected >= 4000 cities, got ${cities.size}", cities.size >= 4000)
        val cairo = cities.first { it.nameEn == "Cairo" && it.countryCode == "EG" }
        assertEquals("القاهرة", cairo.nameAr)
        assertEquals("Africa/Cairo", cairo.timeZoneId)
        assertTrue(cairo.population > 1_000_000)
    }

    @Test
    fun `search matches arabic and latin queries`() {
        val cities = catalog()
        val arabic = CityCatalog.search(cities, "القاه")
        assertTrue(arabic.isNotEmpty())
        assertTrue(arabic.take(3).any { it.nameEn == "Cairo" })

        val latin = CityCatalog.search(cities, "cairo")
        assertTrue(latin.take(3).any { it.nameEn == "Cairo" })

        val ny = CityCatalog.search(cities, "new york")
        assertTrue(ny.take(5).any { it.nameEn.startsWith("New York") })

        assertTrue(CityCatalog.search(cities, "").isEmpty())
    }

    @Test
    fun `search normalizes alef variants and diacritics`() {
        val cities = catalog()
        // "أسوان" with alef-hamza vs catalog's plain form should still match.
        val aswan = CityCatalog.search(cities, "أسوان")
        assertTrue(aswan.take(3).any { it.nameEn == "Aswan" })
    }

    @Test
    fun `nearest city lookup works`() {
        val cities = catalog()
        val nearCairo = CityCatalog.nearest(cities, 30.05, 31.24)
        assertNotNull(nearCairo)
        // Any district of Cairo is acceptable; it must be close to the city centre.
        assertTrue(
            "nearest to Cairo query was ${nearCairo!!.nameEn}",
            io.github.ahmedzakefit.noor.core.qibla.QiblaMath.haversineKm(
                nearCairo!!.latitude, nearCairo.longitude, 30.0444, 31.2357,
            ) < 15.0,
        )
        val nearMakkah = CityCatalog.nearest(cities, 21.4225, 39.8262)
        assertNotNull(nearMakkah)
        assertTrue(
            "nearest to Makkah query was ${nearMakkah!!.nameEn}",
            io.github.ahmedzakefit.noor.core.qibla.QiblaMath.haversineKm(
                nearMakkah!!.latitude, nearMakkah.longitude, 21.4225, 39.8262,
            ) < 15.0,
        )
    }

    @Test
    fun `csv splitter handles quotes`() {
        assertEquals(listOf("a", "b,c", "d\"e"), CityCatalog.splitCsvLine("a,\"b,c\",\"d\"\"e\""))
        assertEquals(listOf("x", "y"), CityCatalog.splitCsvLine("x,y"))
    }

    @Test
    fun `display name respects language with fallback`() {
        val cities = catalog()
        val cairo = cities.first { it.nameEn == "Cairo" && it.countryCode == "EG" }
        assertEquals("القاهرة", cairo.displayName("ar"))
        assertEquals("Cairo", cairo.displayName("en"))
        // A city without an Arabic name falls back to English.
        val noAr = cities.first { it.nameAr == null }
        assertEquals(noAr.nameEn, noAr.displayName("ar"))
    }
}
