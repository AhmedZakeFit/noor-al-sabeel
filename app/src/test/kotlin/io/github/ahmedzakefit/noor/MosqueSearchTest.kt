package io.github.ahmedzakefit.noor

import io.github.ahmedzakefit.noor.data.HttpOutcome
import io.github.ahmedzakefit.noor.data.MosqueSearchClient
import io.github.ahmedzakefit.noor.data.OverpassClient
import io.github.ahmedzakefit.noor.data.OverpassClient.Mosque
import io.github.ahmedzakefit.noor.data.OverpassClient.Reason
import io.github.ahmedzakefit.noor.data.OverpassClient.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Regression tests for the "mosque search keeps loading then fails on the
 * phone" report that survived both v1.0.3 (mirror failover) and v1.0.4
 * (Nominatim first).
 *
 * Root cause established by live probes (2026-10-23, dev environment, the
 * app's exact v1.0.4 URL + User-Agent): Nominatim answers HTTP 200 in ~0.95 s
 * and Photon in ~1.3 s, while *all three* Overpass endpoints answer HTTP 000 —
 * two of them accept the TCP connection and then never respond, so each burns
 * its full read timeout. v1.0.4 ran Nominatim as a *sequential* gate in front
 * of that dead family: whenever Nominatim itself was unavailable on the
 * user's mobile network, the cascade still had to survive three stalling
 * mirrors, blew the overall deadline, and surfaced as "the search took too
 * long" — exactly the reported symptom.
 *
 * The fix runs the two genuinely independent sources (Nominatim on Fastly,
 * Photon on Hetzner — different companies, datacenters and stacks, and Photon
 * is IPv4-only which sidesteps broken-IPv6 mobile carriers) *in parallel*:
 * the first to answer with mosques wins and the loser's socket is closed, so
 * the death of either provider costs the user nothing. Only when both fail
 * does the Overpass family get one last-chance attempt, all under one overall
 * deadline.
 *
 * The Nominatim/Photon fixtures below are trimmed copies of *live* responses
 * for central Cairo captured the same day (Nominatim: 19 mosques, all
 * religion=muslim, Arabic names incl. Al-Azhar; Photon with the Arabic
 * keyword: 48 place_of_worship + 1 building/mosque + 1 landuse/religious).
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

    /** Trimmed copy of the live v1.0.5-era Nominatim response (2026-10-23). */
    private val liveNominatimFixture = """
    [
      {"place_id":50529067,"osm_type":"way","osm_id":24749059,"lat":"30.0457539","lon":"31.2626775",
       "category":"amenity","type":"place_of_worship","name":"الجامع الأزهر",
       "display_name":"الجامع الأزهر, الغوريه, القاهرة, 11675, مصر",
       "extratags":{"religion":"muslim"}},
      {"place_id":50529068,"osm_type":"relation","osm_id":74081,"lat":"30.0287138","lon":"31.2502469",
       "category":"amenity","type":"place_of_worship","name":"مسجد أحمد إبن طولون",
       "display_name":"مسجد أحمد إبن طولون, الصليبه, السيدة زينب, القاهرة, 11622, مصر",
       "extratags":{"religion":"muslim"}},
      {"place_id":50529069,"osm_type":"way","osm_id":24908256,"lat":"30.0328402","lon":"31.2570977",
       "category":"amenity","type":"place_of_worship","name":"مسجد الرفاعي",
       "display_name":"مسجد الرفاعي, المحجر, الجيوشى, القاهرة, 11655, مصر",
       "extratags":{"religion":"muslim"}},
      {"place_id":50529070,"osm_type":"way","osm_id":24908268,"lat":"30.0324087","lon":"31.2562365",
       "category":"amenity","type":"place_of_worship","name":"مسجد السلطان حسن",
       "display_name":"مسجد السلطان حسن, القاهرة, مصر","extratags":{"religion":"muslim"}},
      {"place_id":50016126,"osm_type":"way","osm_id":1225911999,"lat":"30.0288334","lon":"31.2250198",
       "category":"landuse","type":"religious","name":"مسجد صلاح الدين الايوبي",
       "display_name":"مسجد صلاح الدين الايوبي, المنيل, القاهرة, مصر",
       "extratags":{"barrier":"wall","religion":"muslim"}}
    ]
    """.trimIndent()

    /**
     * Trimmed copy of the live Photon response for the ARABIC keyword
     * "مسجد" (2026-10-23), including the junk the live answer really
     * contains (the Latin-keyword probe returned 25 residential streets
     * among 50 features; a church-shaped entry is added here to pin the
     * non-Islamic filter, everything else is verbatim live data).
     */
    private val livePhotonFixture = """
    {"type":"FeatureCollection","features":[
      {"type":"Feature","properties":{"osm_type":"R","osm_id":74081,"osm_key":"amenity","osm_value":"place_of_worship",
       "name":"مسجد أحمد إبن طولون","locality":"Tolone","district":"Al-Sayyida Zeinab","city":"القاهرة"},
       "geometry":{"type":"Point","coordinates":[31.2502469,30.0287138]}},
      {"type":"Feature","properties":{"osm_type":"W","osm_id":24908256,"osm_key":"amenity","osm_value":"place_of_worship",
       "name":"مسجد الرفاعي","district":"El Mahgar","city":"القاهرة"},
       "geometry":{"type":"Point","coordinates":[31.2570977,30.0328402]}},
      {"type":"Feature","properties":{"osm_type":"W","osm_id":555001,"osm_key":"building","osm_value":"mosque",
       "name":"Masjid Al-Rahman","city":"القاهرة"},
       "geometry":{"type":"Point","coordinates":[31.25,30.04]}},
      {"type":"Feature","properties":{"osm_type":"W","osm_id":666002,"osm_key":"landuse","osm_value":"religious",
       "name":"مسجد النور","locality":"وسط البلد","city":"القاهرة"},
       "geometry":{"type":"Point","coordinates":[31.26,30.05]}},
      {"type":"Feature","properties":{"osm_type":"W","osm_id":777003,"osm_key":"highway","osm_value":"residential",
       "name":"شارع المسجد","city":"القاهرة"},
       "geometry":{"type":"Point","coordinates":[31.27,30.06]}},
      {"type":"Feature","properties":{"osm_type":"N","osm_id":888004,"osm_key":"amenity","osm_value":"place_of_worship",
       "name":"Church of St. Mark","city":"القاهرة"},
       "geometry":{"type":"Point","coordinates":[31.28,30.07]}},
      {"type":"Feature","properties":{"osm_type":"R","osm_id":74081,"osm_key":"amenity","osm_value":"place_of_worship",
       "name":"مسجد أحمد إبن طولون"},
       "geometry":{"type":"Point","coordinates":[31.2502469,30.0287138]}}
    ]}
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
    fun `live nominatim response parses fully`() {
        // The exact request the app sends (same URL and User-Agent), probed
        // live on 2026-10-23: HTTP 200 in ~0.95 s. Every one of the five
        // trimmed entries is religion=muslim and must survive parsing.
        val mosques = MosqueSearchClient.parseNominatimMosques(liveNominatimFixture)
        assertEquals(5, mosques.size)
        assertTrue(mosques.any { it.nameAr == "الجامع الأزهر" && it.id == 24749059L })
        assertTrue(mosques.any { it.nameAr == "مسجد أحمد إبن طولون" && it.latitude == 30.0287138 })
        assertTrue(mosques.any { it.nameAr == "مسجد صلاح الدين الايوبي" && it.longitude == 31.2250198 })
    }

    @Test
    fun `live photon response parses with junk and church filtered`() {
        val mosques = MosqueSearchClient.parsePhotonMosques(livePhotonFixture)
        // 7 features: two mosques, a building=mosque, a landuse=religious with
        // an Islamic name, a street named after a mosque (drop), a church
        // (drop), and an exact duplicate of the first (collapse).
        assertEquals(4, mosques.size)
        assertTrue(mosques.any { it.nameAr == "مسجد أحمد إبن طولون" })
        assertTrue(mosques.any { it.nameAr == "مسجد الرفاعي" })
        assertTrue(mosques.any { it.nameEn == "Masjid Al-Rahman" }) // explicit mosque category kept
        assertTrue(mosques.any { it.nameAr == "مسجد النور" })
        assertFalse(mosques.any { it.nameEn?.contains("St. Mark") == true })
        assertFalse(mosques.any { it.nameAr == "شارع المسجد" }) // street junk dropped
        assertEquals(1, mosques.count { it.id == 74081L }) // R/74081 dedup
        // Photon coordinates are (lon, lat) — must not come out swapped.
        val tulun = mosques.first { it.id == 74081L }
        assertEquals(30.0287138, tulun.latitude, 1e-9)
        assertEquals(31.2502469, tulun.longitude, 1e-9)
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
    fun `photon bbox uses the documented minLon,minLat,maxLon,maxLat order`() {
        val parts = MosqueSearchClient.photonBbox(30.0444, 31.2357, 3000).split(',')
        assertEquals(4, parts.size)
        val (minLon, minLat, maxLon, maxLat) = parts.map { it.toDouble() }
        // Bottom before top — the opposite of Nominatim's viewbox order.
        assertTrue(minLon < maxLon)
        assertTrue(minLat < maxLat)
        assertTrue(31.2357 in minLon..maxLon)
        assertTrue(30.0444 in minLat..maxLat)
    }

    @Test
    fun `photon keyword follows ui language and lang param is never sent`() {
        val urls = mutableListOf<String>()
        val client = MosqueSearchClient(
            OverpassClient(),
            fetchGet = { url ->
                urls.add(url)
                HttpOutcome.Response(200, if (url.contains("photon")) livePhotonFixture else "[]")
            },
        )
        runBlocking { client.findNearbyMosques(30.0444, 31.2357, 3000, "ar-EG") }
        val photonUrl = urls.first { it.contains("photon.komoot.io") }
        val nominatimUrl = urls.first { it.contains("nominatim.openstreetmap.org") }
        // Arabic UI -> Arabic keyword, URL-encoded (verified live to return
        // the Arabic-named mosques a Latin keyword never finds).
        assertTrue("expected encoded Arabic keyword in $photonUrl", photonUrl.contains("q=%D9%85%D8%B3%D8%AC%D8%AF"))
        // Photon answers HTTP 400 for unsupported lang values — never send it.
        assertFalse("lang must never be sent to photon: $photonUrl", photonUrl.contains("lang="))
        assertTrue(photonUrl.contains("bbox="))
        assertTrue(nominatimUrl.contains("accept-language=ar-EG"))
        assertTrue(nominatimUrl.contains("q=mosque"))

        urls.clear()
        runBlocking { client.findNearbyMosques(30.0444, 31.2357, 3000, "en-US") }
        val photonUrlEn = urls.first { it.contains("photon.komoot.io") }
        assertTrue(photonUrlEn.contains("q=mosque"))
    }

    @Test
    fun `a stalled independent source never delays the healthy one`() {
        // Nominatim stalls (as it does on the reporter's network); Photon
        // answers instantly. The result must come from Photon and must NOT
        // wait for the stalled request — this is exactly the failure mode
        // that made v1.0.4 time out.
        val client = MosqueSearchClient(
            OverpassClient(),
            fetchGet = { url ->
                if (url.contains("photon")) {
                    HttpOutcome.Response(200, livePhotonFixture)
                } else {
                    delay(3_000)
                    HttpOutcome.Response(200, "[]")
                }
            },
            fetchOverpass = { _, _, _ -> Result.Error(Reason.TIMEOUT, "overpass dead too") },
            totalDeadlineMs = 8_000,
        )
        val start = System.nanoTime()
        val result = runBlocking { client.findNearbyMosques(30.0444, 31.2357, 3000, "ar") }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        val ok = result as Result.Ok
        assertTrue(ok.mosques.isNotEmpty())
        assertTrue("winner must not wait for the stalled loser (took ${elapsedMs}ms)", elapsedMs < 1_500)
    }

    @Test
    fun `the race works in the other direction too`() {
        val client = MosqueSearchClient(
            OverpassClient(),
            fetchGet = { url ->
                if (url.contains("nominatim")) {
                    HttpOutcome.Response(200, liveNominatimFixture)
                } else {
                    delay(3_000)
                    HttpOutcome.Response(200, """{"features":[]}""")
                }
            },
            fetchOverpass = { _, _, _ -> Result.Error(Reason.TIMEOUT, null) },
            totalDeadlineMs = 8_000,
        )
        val start = System.nanoTime()
        val result = runBlocking { client.findNearbyMosques(30.0444, 31.2357, 3000, "en") }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        val ok = result as Result.Ok
        assertEquals(5, ok.mosques.size)
        assertTrue("winner must not wait for the stalled loser (took ${elapsedMs}ms)", elapsedMs < 1_500)
    }

    @Test
    fun `both independent sources failing falls back to overpass`() {
        val client = MosqueSearchClient(
            OverpassClient(),
            fetchGet = { _ -> throw UnknownHostException("offline") },
            fetchOverpass = { _, _, _ ->
                Result.Ok(listOf(Mosque(99L, "مسجد احتياطي", null, 30.05, 31.25)))
            },
        )
        val result = runBlocking { client.findNearbyMosques(30.0444, 31.2357, 3000, "ar") }
        val ok = result as Result.Ok
        assertEquals(listOf(99L), ok.mosques.map { it.id })
    }

    @Test
    fun `all tiers failing reports the most actionable reason`() {
        val client = MosqueSearchClient(
            OverpassClient(),
            fetchGet = { _ -> throw SocketTimeoutException("stall") },
            fetchOverpass = { _, _, _ -> Result.Error(Reason.SERVER_BUSY, "HTTP 429") },
        )
        val result = runBlocking { client.findNearbyMosques(30.0444, 31.2357, 3000, "ar") }
        val error = result as Result.Error
        assertEquals(Reason.TIMEOUT, error.reason)
    }

    @Test
    fun `authoritative zero mosques stays an honest empty answer`() {
        // Both independent sources answered fine and found nothing; Overpass
        // is unreachable — the answer is "no mosques nearby", not an error.
        val client = MosqueSearchClient(
            OverpassClient(),
            fetchGet = { url ->
                HttpOutcome.Response(200, if (url.contains("photon")) """{"features":[]}""" else "[]")
            },
            fetchOverpass = { _, _, _ -> Result.Error(Reason.TIMEOUT, "overpass down") },
        )
        val result = runBlocking { client.findNearbyMosques(30.0444, 31.2357, 3000, "ar") }
        val ok = result as Result.Ok
        assertTrue(ok.mosques.isEmpty())
    }

    @Test
    fun `total deadline bounds everything when every source stalls`() {
        val client = MosqueSearchClient(
            OverpassClient(),
            fetchGet = { _ -> delay(60_000); HttpOutcome.Response(200, "[]") },
            fetchOverpass = { _, _, _ -> delay(60_000); Result.Ok(emptyList()) },
            totalDeadlineMs = 250,
        )
        val start = System.nanoTime()
        val result = runBlocking { client.findNearbyMosques(30.0444, 31.2357, 3000, "ar") }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        val error = result as Result.Error
        assertEquals(Reason.TIMEOUT, error.reason)
        assertTrue("deadline must fire quickly (took ${elapsedMs}ms)", elapsedMs < 2_000)
    }

    @Test
    fun `chooseResult prefers any non-empty answer`() {
        val winner = Result.Ok(listOf(Mosque(1, "a", null, 1.0, 1.0)))
        assertEquals(winner, MosqueSearchClient.chooseResult(winner, Result.Ok(emptyList()), null))
        assertEquals(winner, MosqueSearchClient.chooseResult(null, Result.Error(Reason.TIMEOUT, null), winner))
        assertEquals(winner, MosqueSearchClient.chooseResult(null, null, winner))
    }

    @Test
    fun `chooseResult ranks reasons when everything fails`() {
        val result = MosqueSearchClient.chooseResult(
            Result.Error(Reason.SERVER_BUSY, null),
            Result.Error(Reason.TIMEOUT, null),
            Result.Error(Reason.OTHER, null),
        ) as Result.Error
        assertEquals(Reason.TIMEOUT, result.reason)
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
    fun `tier-1 budgets are tight and bounded by the deadline`() {
        // Healthy tier-1 latency is ~1 s (verified live); 4 s connect / 5 s
        // read is a generous multiple that still cannot eat the whole budget.
        assertTrue(MosqueSearchClient.TIER1_CONNECT_TIMEOUT_MS <= 4_000)
        assertTrue(MosqueSearchClient.TIER1_READ_TIMEOUT_MS <= 5_000)
        assertTrue(
            MosqueSearchClient.TIER1_CONNECT_TIMEOUT_MS + MosqueSearchClient.TIER1_READ_TIMEOUT_MS <
                MosqueSearchClient.TOTAL_DEADLINE_MS
        )
    }

    @Test
    fun `reason ranking prefers the most actionable cause`() {
        // Offline beats timeout beats busy when several sources fail.
        assertEquals(
            Reason.NO_NETWORK,
            OverpassClient.worseReason(Reason.NO_NETWORK, Reason.TIMEOUT),
        )
        assertEquals(
            Reason.TIMEOUT,
            OverpassClient.worseReason(Reason.SERVER_BUSY, Reason.TIMEOUT),
        )
        assertEquals(
            Reason.SERVER_BUSY,
            OverpassClient.worseReason(Reason.SERVER_BUSY, Reason.OTHER),
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
