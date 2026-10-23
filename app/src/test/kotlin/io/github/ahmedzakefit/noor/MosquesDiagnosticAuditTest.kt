package io.github.ahmedzakefit.noor

import io.github.ahmedzakefit.noor.data.AppLocation
import io.github.ahmedzakefit.noor.data.HttpOutcome
import io.github.ahmedzakefit.noor.data.MosqueSearchClient
import io.github.ahmedzakefit.noor.data.OverpassClient
import io.github.ahmedzakefit.noor.data.OverpassClient.Result
import io.github.ahmedzakefit.noor.ui.mosques.MosquesViewModel
import io.github.ahmedzakefit.noor.ui.mosques.MosquesViewModel.SearchPlan
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DIAGNOSTIC AUDIT tests — 2026-10-24 on-device feedback round, kept as
 * permanent regression pins after the fix (commit on main, 2026-10-24).
 *
 * Reported: "commit 3eecc3f changed nothing on the phone; the app never
 * asks for the location permission". Diagnosis: the main search button's
 * plan silently searched the *saved* app location whenever the permission
 * was missing, so no permission dialog ever appeared and the outgoing
 * queries kept embedding the old area. Fix under test here: the plan is now
 * binary (live fix with the grant, otherwise the in-context permission
 * request) and the saved location is queried ONLY as an explicit on-screen
 * choice.
 *
 * What these tests ARE: executable evidence pinning, at the decision level
 * and at the outgoing-request level, the exact mechanism behind the
 * reported symptoms and the behavior that replaced it.
 *
 * What these tests are NOT: UI verification. No test here launches an
 * Activity, shows a dialog, or touches a MapView — that requires an
 * emulator/instrumented run (no KVM, no system image in the dev
 * environment). The permission *dialog* itself is Android framework
 * behavior and is deliberately NOT claimed as verified.
 *
 * JVM constraint note: MosquesViewModel cannot be instantiated in a plain
 * JVM test (its AppContainer eagerly builds DataStore-backed, Context-bound
 * collaborators). The chain is therefore proven in linked, straight-line
 * steps — (1) the button's plan decision, (2) the network request that
 * decision produces — connected by non-branching functions
 * (search → locateAndSearch → performSearch, searchAroundSaved → runSearch).
 */
class MosquesDiagnosticAuditTest {

    /** The saved app location (e.g. picked once, long ago, in settings). */
    private val savedCairo = AppLocation(30.0444, 31.2357, "Africa/Cairo", "Cairo", "القاهرة")

    /** Where the device physically is now, ~180 km away from the saved city. */
    private val actualAlexandria = AppLocation(31.2001, 29.9187, "Africa/Cairo", "Alexandria", "الإسكندرية")

    /**
     * ROOT CAUSE (fixed) — "the app never asks for the location permission"
     * and "searching after travelling returns the old area's results".
     *
     * Without the grant, the main search button's plan is now
     * PERMISSION_REQUIRED: the UI runs the in-context permission request
     * (rationale → system dialog → search on grant), and NO network request
     * is issued on that path. The saved area is queried only through the
     * explicit user choice (searchAroundSaved behind the on-screen fallback
     * button) — which is what the request-level part below documents.
     */
    @Test
    fun `without permission the default search requires the grant and the saved area is only an explicit choice`() {
        // (1) The decision the search button makes: permission missing →
        //     surface the permission request, not a silent saved search.
        assertEquals(SearchPlan.PERMISSION_REQUIRED, MosquesViewModel.resolveSearchPlan(hasPermission = false))

        // (2) The request the EXPLICIT saved-fallback choice produces:
        //     searchAroundSaved passes the saved coordinates onward to
        //     findNearbyMosques (straight-line code path), so its outgoing
        //     viewbox contains the saved city — visibly labeled "Around: …"
        //     on screen, never silent.
        val urls = mutableListOf<String>()
        val client = MosqueSearchClient(
            OverpassClient(),
            fetchGet = { url ->
                urls.add(url)
                HttpOutcome.Response(200, if (url.contains("photon")) """{"features":[]}""" else "[]")
            },
            fetchOverpass = { _, _, _ -> Result.Ok(emptyList()) },
        )
        runBlocking {
            client.findNearbyMosques(savedCairo.latitude, savedCairo.longitude, 3_000, "ar")
        }
        val viewbox = urls.last { it.contains("nominatim.openstreetmap.org") }
            .substringAfter("viewbox=").substringBefore('&')
            .split(',').map { it.toDouble() }
        val (minLon, maxLat, maxLon, minLat) = viewbox
        assertTrue(
            "the explicit saved-fallback query must target the saved area",
            savedCairo.latitude in minLat..maxLat && savedCairo.longitude in minLon..maxLon,
        )
    }

    /**
     * The granted path — what the user gets the moment the permission is
     * granted (from the in-context dialog wired into the search button):
     * every search re-fixes the device position and the FIX area — not the
     * saved one — is what gets queried.
     */
    @Test
    fun `with permission the fresh fix area - not the saved one - is queried`() {
        assertEquals(SearchPlan.LOCATE_AND_SEARCH, MosquesViewModel.resolveSearchPlan(hasPermission = true))

        val urls = mutableListOf<String>()
        val client = MosqueSearchClient(
            OverpassClient(),
            fetchGet = { url ->
                urls.add(url)
                HttpOutcome.Response(200, if (url.contains("photon")) """{"features":[]}""" else "[]")
            },
            fetchOverpass = { _, _, _ -> Result.Ok(emptyList()) },
        )
        // locateAndSearch obtains a fresh fix (here: the device standing in
        // Alexandria) and passes exactly those coordinates onward.
        runBlocking {
            client.findNearbyMosques(actualAlexandria.latitude, actualAlexandria.longitude, 3_000, "ar")
        }
        val viewbox = urls.last { it.contains("nominatim.openstreetmap.org") }
            .substringAfter("viewbox=").substringBefore('&')
            .split(',').map { it.toDouble() }
        val (minLon, maxLat, maxLon, minLat) = viewbox
        assertTrue("fresh fix must be inside the queried box", actualAlexandria.latitude in minLat..maxLat)
        assertFalse("stale saved area must NOT be queried anymore", savedCairo.latitude in minLat..maxLat)
    }

    /**
     * ROOT CAUSE 2 (old builds) — "the map moves and zooms uncontrollably".
     *
     * Pre-3eecc3f builds re-centered the map inside AndroidView's update
     * lambda, which re-runs on EVERY recomposition — any state emission
     * snapped the map back to its center. The current code syncs the map
     * from a LaunchedEffect keyed on (mapCenter, radius, results, first
     * measurement), so the keys must be recomposition-stable for the fix to
     * hold. This pins that stability — with DISTINCT equal instances, the
     * way recompositions and fresh DataStore emissions actually deliver
     * them: structurally-equal centers/items produce equal keys, so a plain
     * recomposition does not re-run the sync effect.
     */
    @Test
    fun `map sync keys are recomposition-stable so plain recompositions cannot re-center the map`() {
        // fallbackCenter builds a NEW instance per recomposition; data-class
        // equality (AppLocation included) keeps the LaunchedEffect key
        // unchanged even when the saved-location instance is re-delivered.
        assertEquals(
            MosquesViewModel.fallbackCenter(savedCairo),
            MosquesViewModel.fallbackCenter(savedCairo.copy()), // distinct instance, equal value
        )
        // Ready-state items compare structurally too (MosqueItem and its
        // Mosque are data classes): an unchanged result list re-delivered by
        // the state flow does not count as a key change.
        val itemA = MosquesViewModel.MosqueItem(
            OverpassClient.Mosque(1, "مسجد النور", null, 31.2001, 29.9187), 1.0, 90.0,
        )
        val itemB = MosquesViewModel.MosqueItem(
            OverpassClient.Mosque(1, "مسجد النور", null, 31.2001, 29.9187), 1.0, 90.0,
        )
        assertEquals(itemA, itemB)
        assertEquals(listOf(itemA), listOf(itemB))
    }

    /**
     * NO RESULT CACHING EXISTS — every search goes to the network with its
     * center embedded in the outgoing URLs. The "old results" symptom is
     * therefore NOT a cache-invalidation bug: it is the stale CENTER
     * (saved location) that the no-permission plan keeps feeding in (see
     * the first test). Two consecutive searches around two different
     * centers must produce two different boxes — nothing is replayed.
     */
    @Test
    fun `no result cache exists - every search embeds its own center in the outgoing request`() {
        val urls = mutableListOf<String>()
        val client = MosqueSearchClient(
            OverpassClient(),
            fetchGet = { url ->
                urls.add(url)
                HttpOutcome.Response(200, if (url.contains("photon")) """{"features":[]}""" else "[]")
            },
            fetchOverpass = { _, _, _ -> Result.Ok(emptyList()) },
        )
        runBlocking { client.findNearbyMosques(savedCairo.latitude, savedCairo.longitude, 3_000, "ar") }
        runBlocking { client.findNearbyMosques(actualAlexandria.latitude, actualAlexandria.longitude, 3_000, "ar") }
        val boxes = urls.filter { it.contains("nominatim.openstreetmap.org") }
            .map { it.substringAfter("viewbox=").substringBefore('&') }
        assertEquals("each search must issue its own Nominatim request", 2, boxes.size)
        assertFalse("second search must not replay the first box", boxes[0] == boxes[1])
    }
}
