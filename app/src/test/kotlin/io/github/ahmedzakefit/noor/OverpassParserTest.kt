package io.github.ahmedzakefit.noor

import io.github.ahmedzakefit.noor.data.OverpassClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverpassParserTest {

    private val sample = """
        {
          "version": 0.6,
          "generator": "Overpass API",
          "elements": [
            {"type":"node","id":101,"lat":30.05,"lon":31.24,
             "tags":{"amenity":"place_of_worship","religion":"islam","name":"Masjid al-Rahman","name:ar":"مسجد الرحمن"}},
            {"type":"way","id":202,
             "center":{"lat":30.06,"lon":31.25},
             "tags":{"amenity":"place_of_worship","religion":"islam","name":"Nour Mosque"}},
            {"type":"node","id":303,"lat":30.07,"lon":31.26,
             "tags":{"amenity":"place_of_worship","religion":"islam"}},
            {"type":"way","id":404,"tags":{"amenity":"place_of_worship","religion":"islam"}},
            {"type":"node","id":505,"lat":30.08,"lon":31.27,
             "tags":{"amenity":"place_of_worship","religion":"christian","name":"St Mary"}}
          ]
        }
    """.trimIndent()

    @Test
    fun `parses nodes ways and unnamed mosques, skipping malformed entries`() {
        val client = OverpassClient()
        val mosques = client.parse(sample)
        assertEquals(3, mosques.size)

        val first = mosques[0]
        assertEquals(101L, first.id)
        assertEquals("مسجد الرحمن", first.nameAr)
        assertEquals("Masjid al-Rahman", first.nameEn)
        assertEquals(30.05, first.latitude, 1e-9)
        assertEquals(31.24, first.longitude, 1e-9)

        val second = mosques[1]
        assertEquals(202L, second.id)
        assertEquals("Nour Mosque", second.nameEn)
        assertEquals(null, second.nameAr)
        assertEquals(30.06, second.latitude, 1e-9)

        val third = mosques[2]
        assertEquals(null, third.nameEn)
        assertEquals(null, third.nameAr)
    }

    @Test
    fun `empty body parses to empty list`() {
        assertTrue(OverpassClient().parse("{}").isEmpty())
    }
}
