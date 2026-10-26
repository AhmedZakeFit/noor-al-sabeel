package io.github.ahmedzakefit.noor

import io.github.ahmedzakefit.noor.data.AppLocation
import io.github.ahmedzakefit.noor.data.HttpOutcome
import io.github.ahmedzakefit.noor.data.MosqueSearchClient
import io.github.ahmedzakefit.noor.data.OverpassClient
import io.github.ahmedzakefit.noor.data.OverpassClient.Reason
import io.github.ahmedzakefit.noor.data.OverpassClient.Result
import io.github.ahmedzakefit.noor.ui.mosques.MosquesViewModel
import io.github.ahmedzakefit.noor.ui.mosques.MosquesViewModel.SearchPlan
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the on-device test reports:
 *
 *  v1.0.5 report — "map covers the UI / results list doesn't fit / same old
 *  results after travelling": adaptive layout bounds, zoom fitting and
 *  request-level evidence that a new center reaches the outgoing queries.
 *
 *  2026-10-24 report — "the app never asks for the location permission":
 *  the search button used to silently search the *saved* app location when
 *  the permission was missing (SEARCH_SAVED), so no permission dialog ever
 *  appeared and travelling never changed the queried area. The plan is now
 *  binary — a live fix with the grant, otherwise PERMISSION_REQUIRED with
 *  the in-context grant flow — and the saved location is reachable ONLY as
 *  the explicit on-screen choice (searchAroundSaved).
 */
class MosquesLocationAndLayoutTest {

    private val cairo = AppLocation(30.0444, 31.2357, "Africa/Cairo", "Cairo", "القاهرة")
    private val alexandria = AppLocation(31.2001, 29.9187, "Africa/Cairo", "Alexandria", "الإسكندرية")

    // ------------------------------------------------------------------
    // Search-plan decisions (location change → new results)
    // ------------------------------------------------------------------

    @Test
    fun `search plan prefers the live device location whenever permission is granted`() {
        // With the permission, the saved location is irrelevant: every search
        // must re-fix the device position, so travelling yields fresh areas.
        assertEquals(SearchPlan.LOCATE_AND_SEARCH, MosquesViewModel.resolveSearchPlan(true))
    }

    @Test
    fun `search plan asks for permission instead of silently searching the saved location`() {
        // 2026-10-24 fix: without the grant the button must surface the
        // in-context permission request — never a silent saved-location
        // search (the old SEARCH_SAVED plan is gone by design).
        assertEquals(SearchPlan.PERMISSION_REQUIRED, MosquesViewModel.resolveSearchPlan(false))
    }

    @Test
    fun `map falls back to the saved location before any search and never claims it is current`() {
        val center = MosquesViewModel.fallbackCenter(cairo)!!
        assertEquals(cairo.latitude, center.latitude, 1e-9)
        assertEquals(cairo.longitude, center.longitude, 1e-9)
        assertFalse(center.isCurrentLocation)
        assertSame(cairo, center.saved)
        assertNull(MosquesViewModel.fallbackCenter(null))
    }

    @Test
    fun `a new search center actually reaches the outgoing queries`() {
        // No result caching exists (each search goes to the network with the
        // center embedded in the URL); this pins that the second search
        // queries the NEW area and the old one is gone from the request.
        val urls = mutableListOf<String>()
        val client = MosqueSearchClient(
            OverpassClient(),
            fetchGet = { url ->
                urls.add(url)
                HttpOutcome.Response(200, if (url.contains("photon")) """{"features":[]}""" else "[]")
            },
            fetchOverpass = { _, _, _ -> Result.Ok(emptyList()) },
        )
        runBlocking { client.findNearbyMosques(cairo.latitude, cairo.longitude, 3000, "ar") }
        runBlocking {
            client.findNearbyMosques(alexandria.latitude, alexandria.longitude, 3000, "ar")
        }
        val lastPhoton = urls.last { it.contains("photon.komoot.io") }
        val bbox = lastPhoton.substringAfter("bbox=").substringBefore('&')
            .split(',').map { it.toDouble() }
        val (minLon, minLat, maxLon, maxLat) = bbox
        // Alexandria is inside the requested box…
        assertTrue(alexandria.latitude in minLat..maxLat)
        assertTrue(alexandria.longitude in minLon..maxLon)
        // …and Cairo's latitude is outside it: the old area is not queried again.
        assertFalse(cairo.latitude in minLat..maxLat)

        val lastNominatim = urls.last { it.contains("nominatim.openstreetmap.org") }
        val viewbox = lastNominatim.substringAfter("viewbox=").substringBefore('&')
            .split(',').map { it.toDouble() }
        assertTrue(alexandria.longitude in viewbox[0]..viewbox[2])
        assertFalse(cairo.longitude in viewbox[0]..viewbox[2])
    }

    // ------------------------------------------------------------------
    // Adaptive map bounds (small-phone layout)
    // ------------------------------------------------------------------

    @Test
    fun `zoom shrinks as the search radius grows and stays sane`() {
        val z1 = MosquesViewModel.zoomForRadius(1_000, 30.0444, 360f)
        val z3 = MosquesViewModel.zoomForRadius(3_000, 30.0444, 360f)
        val z10 = MosquesViewModel.zoomForRadius(10_000, 30.0444, 360f)
        assertTrue("zoom must be monotonic in radius: $z1, $z3, $z10", z1 >= z3 && z3 >= z10)
        // 3 km around Cairo on a ~360 px viewport works out to ≈ 12.8.
        assertTrue("3 km zoom out of expected band: $z3", z3 in 11.0..14.0)
    }

    @Test
    fun `zoom is clamped to the tile source range`() {
        assertEquals(MosquesViewModel.MAX_ZOOM, MosquesViewModel.zoomForRadius(10, 30.0, 360f), 1e-9)
        assertEquals(MosquesViewModel.MIN_ZOOM, MosquesViewModel.zoomForRadius(5_000_000, 30.0, 360f), 1e-9)
    }

    @Test
    fun `map height adapts to the screen and leaves room for the results list`() {
        val smallPhone = MosquesViewModel.mapHeightDp(560)
        val regular = MosquesViewModel.mapHeightDp(800)
        val landscape = MosquesViewModel.mapHeightDp(360)
        assertTrue(smallPhone in 150..260)
        assertTrue(regular in 150..260)
        // Landscape is clamped to the floor…
        assertEquals(MosquesViewModel.MAP_HEIGHT_MIN_DP, landscape)
        // …height grows with the screen…
        assertTrue(smallPhone < regular)
        // …and even on a small phone the map + header chrome leave a usable
        // results area (chips + search + label rows ≈ 120 dp) that the
        // weight-based list fills exactly.
        assertTrue("small-phone map leaves too little room: $smallPhone", 560 - smallPhone - 120 >= 200)
    }

    @Test
    fun `locate budget is human-scale`() {
        // The fix phase must not outlast the user's patience before the
        // LocationFailed state with its explicit fallback appears.
        assertTrue(MosquesViewModel.LOCATE_TIMEOUT_MS in 5_000..20_000L)
    }

    // ------------------------------------------------------------------
    // Reason classification parity for the new states (sanity)
    // ------------------------------------------------------------------

    @Test
    fun `error reasons still rank offline above timeout for the retry message`() {
        // The retry path reuses the same classification; keep it pinned.
        assertEquals(Reason.NO_NETWORK, OverpassClient.worseReason(Reason.NO_NETWORK, Reason.TIMEOUT))
    }
}
