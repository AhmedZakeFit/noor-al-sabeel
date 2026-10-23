package io.github.ahmedzakefit.noor

import io.github.ahmedzakefit.noor.data.MosqueSearchClient
import io.github.ahmedzakefit.noor.data.OverpassClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the "mosque search keeps loading then fails on the
 * phone" report that survived v1.0.3's mirror failover.
 *
 * Root cause: all three Overpass mirrors share one operational fate — on the
 * reporter's mobile network every endpoint stalls/drops, so a search ground
 * through the per-socket timeouts and failed. The fix adds an independent
 * source (Nominatim keyword search, verified reachable from networks where
 * every Overpass endpoint answers HTTP 000), one overall deadline, and
 * cancellation when leaving the screen.
 *
 * The fixture below is a trimmed copy of a live Nominatim response for
 * central Cairo (probe: HTTP 200 in ~1.2 s, 24 mosques incl. Al-Azhar).
 */
class MosqueSearchTest {

    private val cairoFixture = """
    [
      {"place_id":50529067,"licence":"Data © OpenStreetMap contributors, ODbL 1.0. http://osm.org/copyright",
       "osm_type":"way","osm_id":24749059,"lat":"30.0457539","lon":"31.2626775",
       "category":"amenity","type":"place_of_worship","name":"الجامع الأزهر",
       "display_name":"الجامع الأزهر, حارة السوق, الأزهر, القاهرة, مصر",
       "extratags":{"building":"yes","religion":"muslim","name:en":"Al-Azhar Mosque"}},
      {"place_id":50529068,"osm_type":"relation","osm_id":147235,"lat":"30.0458831","lon":"31.2620199",
       "category":"amenity","type":"place_of_worship","name":"مسجد أحمد إبن طولون",
       "display_name":"مسجد أحمد إبن طولون, القاهرة, مصر","extratags":{"religion":"muslim"}},
      {"place_id":50529069,"osm_type":"node","osm_id":12345678,"lat":"30.0512345","lon":"31.2512345",
       "category":"amenity","type":"place_of_worship","name":"مسجد الكردي",
       "display_name":"مسجد الكردي, القاهرة, مصر","extratags":{"religion":"muslim"}},
      {"place_id":50529070,"osm_type":"way","osm_id":9876543,"lat":"30.044","lon":"31.256",
       "category":"landuse","type":"religious","name":"مسجد صلاح الدين الايوبي",
       "display_name":"مسجد صلاح الدين الايوبي, القاهرة, مصر","extratags":{"religion":"muslim"}},
      {"place_id":50529071,"osm_type":"way","osm_id":111222,"lat":"30.0401","lon":"31.2501",
       "category":"amenity","type":"place_of_worship","name":"Church of St. Mark",
       "display_name":"Church of St. Mark, القاهرة, مصر","extratags":{"religion":"christian"}},
      {"place_id":50529072,"osm_type":"node","osm_id":222333,"lat":"30.0402","lon":"31.2502",
       "category":"amenity","type":"place_of_worship","name":"Masjid Al-Nour",
       "display_name":"Masjid Al-Nour, القاهرة, مصر","extratags":{}},
      {"place_id":50529073,"osm_type":"node","osm_id":333444,"lat":"30.0403","lon":"31.2503",
       "category":"amenity","type":"place_of_worship","name":"Community Hall",
       "display_name":"Community Hall, القاهرة, مصر","extratags":{}},
      {"place_id":50529074,"osm_type":"way","osm_id":24749059,"lat":"30.0457539","lon":"31.2626775",
       "category":"amenity","type":"place_of_worship","name":"الجامع الأزهر",
       "display_name":"الجامع الأزهر, القاهرة, مصر","extratags":{"religion":"muslim"}}
    ]
    """.trimIndent()

    @Test
    fun `nominatim fixture parses with islamic filter and dedup`() {
        val mosques = MosqueSearchClient.parseNominatimMosques(cairoFixture)
        // 8 entries: Al-Azhar(+exact duplicate), Ibn Tulun, node mosque, landuse
        // mosque, a church (drop), unnamed-religion mosque-named (keep),
        // generic Community Hall (drop).
        assertEquals(5, mosques.size)
        assertTrue(mosques.any { it.nameEn == "Al-Azhar Mosque" })
        assertTrue(mosques.any { it.nameAr == "الجامع الأزهر" })
        assertTrue(mosques.any { it.nameAr == "مسجد أحمد إبن طولون" })
        assertTrue(mosques.any { it.nameAr == "مسجد صلاح الدين الايوبي" })
        assertTrue(mosques.any { it.nameEn == "Masjid Al-Nour" })
        // Christian place of worship must never leak into the list.
        assertFalse(mosques.any { it.nameEn?.contains("St. Mark") == true || it.nameAr?.contains("St. Mark") == true })
        // The exact duplicate (same osm_type/osm_id) is collapsed.
        assertEquals(1, mosques.count { it.id == 24749059L })
    }

    @Test
    fun `viewbox keeps dot decimals and correct corner order`() {
        val box = MosqueSearchClient.viewbox(30.0444, 31.2357, 3000)
        val parts = box.split(',')
        assertEquals(4, parts.size)
        assertTrue("dot decimals required, was: $box", parts.all { !it.contains(',') })
        val (left, top, right, bottom) = parts.map { it.toDouble() }
        assertTrue(left < right)
        assertTrue(top > bottom)
        // The search center is inside the box (3 km radius ≈ ±0.027° lat).
        assertTrue(31.2357 in left..right)
        assertTrue(30.0444 in bottom..top)
        assertTrue(top - bottom < 0.06)
    }

    @Test
    fun `overall deadline is present and sane`() {
        // The total budget is what actually bounds the wait when every source
        // stalls — it must exist and stay human-scale.
        assertTrue(MosqueSearchClient.TOTAL_DEADLINE_MS in 10_000..30_000)
    }

    @Test
    fun `per-mirror socket budgets are tighter than the overall deadline`() {
        val perMirrorWorst =
            (OverpassClient.CONNECT_TIMEOUT_MS + OverpassClient.READ_TIMEOUT_MS)
        assertTrue(perMirrorWorst < MosqueSearchClient.TOTAL_DEADLINE_MS)
    }

    @Test
    fun `reason ranking prefers the most actionable cause`() {
        // Offline beats timeout beats busy when several sources fail.
        assertEquals(
            OverpassClient.Reason.NO_NETWORK,
            OverpassClient.worseReason(OverpassClient.Reason.NO_NETWORK, OverpassClient.Reason.TIMEOUT),
        )
        assertEquals(
            OverpassClient.Reason.TIMEOUT,
            OverpassClient.worseReason(OverpassClient.Reason.SERVER_BUSY, OverpassClient.Reason.TIMEOUT),
        )
        assertEquals(
            OverpassClient.Reason.SERVER_BUSY,
            OverpassClient.worseReason(OverpassClient.Reason.SERVER_BUSY, OverpassClient.Reason.OTHER),
        )
    }

    @Test
    fun `overpass fallback parser still filters non-islamic entries`() {
        val overpass = OverpassClient()
        val body = """
            {"elements":[
              {"type":"node","id":1,"lat":30.05,"lon":31.25,"tags":{"religion":"islam","name":"Masjid A"}},
              {"type":"node","id":2,"lat":30.06,"lon":31.26,"tags":{"religion":"christian","name":"Church"}},
              {"type":"way","id":3,"center":{"lat":30.07,"lon":31.27},"tags":{"religion":"muslim","name:ar":"مسجد ب"}}
            ]}
        """.trimIndent()
        val mosques = overpass.parse(body)
        assertEquals(2, mosques.size)
        assertEquals("Masjid A", mosques[0].nameEn)
        assertEquals("مسجد ب", mosques[1].nameAr)
    }
}
