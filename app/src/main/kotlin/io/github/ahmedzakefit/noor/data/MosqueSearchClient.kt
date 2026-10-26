package io.github.ahmedzakefit.noor.data

import io.github.ahmedzakefit.noor.data.OverpassClient.Mosque
import io.github.ahmedzakefit.noor.data.OverpassClient.Reason
import io.github.ahmedzakefit.noor.data.OverpassClient.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max

/**
 * Nearby-mosque search orchestrator — the reliability layer behind the
 * "find nearby mosques" button.
 *
 * Field history (each step backed by live probes, not guesswork):
 *  - v1.0.3 retried three Overpass mirrors sequentially — they share one
 *    operational fate, so that failed.
 *  - v1.0.4 put Nominatim first — healthy from datacenter networks, but on
 *    the reporter's phone the search still waited the full deadline and then
 *    failed: Nominatim is a *sequential* gate, and when it is unavailable on
 *    a given mobile network the cascade still has to survive the Overpass
 *    family, which is dead even from datacenter networks (verified: HTTP 000
 *    on all three mirrors while Nominatim and Photon both answer in ~1 s).
 *    Two stalled Overpass read timeouts alone burn 18 s — more than the old
 *    20 s deadline ever allowed.
 *  - Now: **two genuinely independent sources run in parallel** and the first
 *    one to answer with results wins, so the death of either provider costs
 *    nothing. Only when *both* fail does the Overpass family get a
 *    last-chance attempt. One overall deadline bounds everything.
 *
 * Why these two providers:
 *  - **Nominatim** (OpenStreetMap Foundation, Fastly edge, dual-stack):
 *    `q=mosque` is a *special phrase* — a category search that also finds
 *    Arabic-named mosques (verified: 19 mosques incl. Al-Azhar for central
 *    Cairo) and returns localized names via accept-language.
 *  - **Photon** (komoot, Hetzner, IPv4-only — immune to broken-IPv6 mobile
 *    networks): a different company, different datacenter, different
 *    codebase. Free-text keyword search; with the Arabic keyword "مسجد" it
 *    returns the Arabic-named mosques the Latin keyword misses (verified:
 *    50 features, all in-box, Arabic names). Its `lang` parameter must NOT
 *    be used — it answers HTTP 400 for unsupported values such as "ar".
 *
 * Requests are manual (user taps the search button) with a bounded radius
 * and a 50-result cap, respecting both services' usage policies: the app
 * identifies itself by User-Agent and sends only the search area, never any
 * user data.
 */
class MosqueSearchClient internal constructor(
    private val overpass: OverpassClient,
    /** Injectable GET transport so the parallel race itself is unit-testable. */
    private val fetchGet: suspend (url: String) -> HttpOutcome = { url ->
        Http.call(
            url,
            connectTimeoutMs = TIER1_CONNECT_TIMEOUT_MS,
            readTimeoutMs = TIER1_READ_TIMEOUT_MS,
        )
    },
    /** Injectable Overpass tier for the same reason. */
    private val fetchOverpass: suspend (
        latitude: Double, longitude: Double, radiusMeters: Int,
    ) -> Result = { latitude, longitude, radiusMeters ->
        overpass.findMosques(latitude, longitude, radiusMeters)
    },
    /** Injectable overall deadline so timeout paths test quickly. */
    private val totalDeadlineMs: Long = TOTAL_DEADLINE_MS,
) {

    /**
     * @param languageTag current UI language, so Nominatim localizes mosque
     * names and Photon searches with the matching keyword.
     */
    suspend fun findNearbyMosques(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        languageTag: String,
    ): Result {
        try {
            return withTimeout(totalDeadlineMs) {
                coroutineScope {
                    val nominatimJob = async {
                        tier1 { searchNominatim(latitude, longitude, radiusMeters, languageTag) }
                    }
                    val photonJob = async {
                        tier1 { searchPhoton(latitude, longitude, radiusMeters, languageTag) }
                    }

                    // The first independent source to answer with mosques
                    // wins; the other request is cancelled and its socket
                    // disconnected — a dead provider costs the user nothing.
                    val first = select<Result?> {
                        nominatimJob.onAwait { it }
                        photonJob.onAwait { it }
                    }
                    if (first is Result.Ok && first.mosques.isNotEmpty()) {
                        nominatimJob.cancel()
                        photonJob.cancel()
                        return@coroutineScope first
                    }

                    // One source failed or found nothing — hear the other
                    // one out before deciding anything.
                    val nominatim = nominatimJob.await()
                    val photon = photonJob.await()

                    // The second source may have brought results after all —
                    // then no third family is needed. Nominatim wins a tie
                    // (documented primary source, localized names).
                    val tier1Winner = listOf(nominatim, photon).firstOrNull {
                        it is Result.Ok && it.mosques.isNotEmpty()
                    }
                    if (tier1Winner != null) return@coroutineScope tier1Winner

                    // Both independent sources failed or returned zero
                    // mosques: give the Overpass family its last chance
                    // before reporting an error or an honest empty answer.
                    val overpassResult = try {
                        fetchOverpass(latitude, longitude, radiusMeters)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Result.Error(Reason.OTHER, e.message)
                    }
                    chooseResult(nominatim, photon, overpassResult)
                }
            }
        } catch (e: TimeoutCancellationException) {
            return Result.Error(Reason.TIMEOUT, "overall deadline exceeded")
        }
    }

    /** Tier-1 wrapper: transport failures become classified errors, never crashes. */
    private suspend fun tier1(search: suspend () -> Result?): Result? = try {
        search()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Error(OverpassClient.classifyException(e), e.message)
    }

    /**
     * Keyword search on Nominatim, bounded to the search radius. Returns
     * null on any HTTP/transport failure (caller keeps the other sources).
     */
    private suspend fun searchNominatim(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        languageTag: String,
    ): Result? {
        val url = "https://nominatim.openstreetmap.org/search?q=$NOMINATIM_KEYWORD&format=jsonv2&limit=50" +
            "&bounded=1&viewbox=${viewbox(latitude, longitude, radiusMeters)}" +
            "&extratags=1&accept-language=${urlEncode(languageTag)}"
        val outcome = fetchGet(url) as HttpOutcome.Response
        if (outcome.code != 200 || outcome.body == null) return null
        return Result.Ok(parseNominatimMosques(outcome.body))
    }

    /**
     * Free-text search on Photon, bounded to the search radius. The keyword
     * follows the UI language: Arabic finds the Arabic-named mosques that a
     * Latin keyword never matches. The `lang` parameter is deliberately NOT
     * sent (Photon rejects unsupported values with HTTP 400).
     */
    private suspend fun searchPhoton(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        languageTag: String,
    ): Result? {
        val keyword = if (languageTag.startsWith("ar")) KEYWORD_AR else KEYWORD_EN
        val url = "$PHOTON_ENDPOINT?q=${urlEncode(keyword)}" +
            "&bbox=${photonBbox(latitude, longitude, radiusMeters)}&limit=50"
        val outcome = fetchGet(url) as HttpOutcome.Response
        if (outcome.code != 200 || outcome.body == null) return null
        return Result.Ok(parsePhotonMosques(outcome.body))
    }

    companion object {
        /** The whole search — all sources included — must finish within this budget. */
        internal const val TOTAL_DEADLINE_MS = 15_000L

        /** Per-request budgets for the two parallel tier-1 sources. */
        internal const val TIER1_CONNECT_TIMEOUT_MS = 4_000
        internal const val TIER1_READ_TIMEOUT_MS = 5_000

        internal const val NOMINATIM_KEYWORD = "mosque"
        internal const val KEYWORD_AR = "مسجد"
        internal const val KEYWORD_EN = "mosque"
        internal const val PHOTON_ENDPOINT = "https://photon.komoot.io/api/"

        /**
         * Final decision across all three tiers — pure so every combination
         * is unit-testable:
         *  1. any source that answered with mosques wins;
         *  2. if every source failed, report the most actionable reason;
         *  3. otherwise at least one source answered authoritatively with
         *     zero mosques — the honest empty answer.
         */
        internal fun chooseResult(
            nominatim: Result?,
            photon: Result?,
            overpass: Result?,
        ): Result {
            for (candidate in listOf(nominatim, photon, overpass)) {
                if (candidate is Result.Ok && candidate.mosques.isNotEmpty()) return candidate
            }
            val reasons = listOfNotNull(
                (nominatim as? Result.Error)?.reason,
                (photon as? Result.Error)?.reason,
                (overpass as? Result.Error)?.reason,
            )
            if (nominatim !is Result.Ok && photon !is Result.Ok && overpass !is Result.Ok) {
                val reason = if (reasons.isEmpty()) Reason.OTHER
                else reasons.reduce { a, b -> OverpassClient.worseReason(a, b) }
                return Result.Error(reason, "all sources failed")
            }
            return Result.Ok(emptyList())
        }

        private fun urlEncode(v: String): String =
            java.net.URLEncoder.encode(v, "UTF-8")

        /**
         * Nominatim viewbox (left, top, right, bottom) for a radius in meters.
         * US locale forces a dot decimal separator — a comma would corrupt
         * the URL. Longitude delta is widened near the poles.
         */
        internal fun viewbox(latitude: Double, longitude: Double, radiusMeters: Int): String {
            val latDelta = radiusMeters / 111_320.0
            val cosLat = max(cos(Math.toRadians(latitude)), 0.01)
            val lonDelta = radiusMeters / (111_320.0 * cosLat)
            fun f(v: Double) = String.format(Locale.US, "%.6f", v)
            return "${f(longitude - lonDelta)},${f(latitude + latDelta)}," +
                "${f(longitude + lonDelta)},${f(latitude - latDelta)}"
        }

        /**
         * Photon bbox in the service's documented order
         * (minLon, minLat, maxLon, maxLat) — verified live to strictly
         * restrict results to the box.
         */
        internal fun photonBbox(latitude: Double, longitude: Double, radiusMeters: Int): String {
            val latDelta = radiusMeters / 111_320.0
            val cosLat = max(cos(Math.toRadians(latitude)), 0.01)
            val lonDelta = radiusMeters / (111_320.0 * cosLat)
            fun f(v: Double) = String.format(Locale.US, "%.6f", v)
            return "${f(longitude - lonDelta)},${f(latitude - latDelta)}," +
                "${f(longitude + lonDelta)},${f(latitude + latDelta)}"
        }

        private val ARABIC_LETTER = Regex("[\\u0600-\\u06FF]")
        private val MOSQUE_NAME_HINTS = listOf("مسجد", "جامع", "mosque", "masjid", "مصلى", "زاويا")

        /**
         * Converts a Nominatim jsonv2 array into mosque models. Only Islamic
         * places of worship are kept: an explicit muslim religion tag always
         * decides; without one the name must look like a mosque (a plain
         * "religious" building with a generic name is not worth risking).
         */
        internal fun parseNominatimMosques(body: String): List<Mosque> {
            val arr = JSONArray(body)
            val out = mutableListOf<Mosque>()
            val seenKeys = mutableSetOf<String>()
            val seenCoords = mutableSetOf<Long>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                // Probe fixtures show both amenity/place_of_worship and
                // landuse/religious containers — the type + religion/name
                // checks below decide, not the container category.
                val type = obj.optString("type")
                if (type != "place_of_worship" && type != "mosque" && type != "religious") continue

                val extra = obj.optJSONObject("extratags")
                val religion = extra?.optString("religion")?.lowercase(Locale.ROOT) ?: ""
                val name = obj.optString("name").trim()
                val displayName = obj.optString("display_name")
                val nameLooksIslamic = MOSQUE_NAME_HINTS.any {
                    name.contains(it, ignoreCase = true) || displayName.contains(it, ignoreCase = true)
                }
                when {
                    religion == "muslim" || religion == "islam" -> Unit // keep
                    religion.isEmpty() && nameLooksIslamic -> Unit // keep
                    else -> continue
                }

                val lat = obj.optString("lat").toDoubleOrNull() ?: continue
                val lon = obj.optString("lon").toDoubleOrNull() ?: continue
                if (lat == 0.0 && lon == 0.0) continue
                val osmType = obj.optString("osm_type")
                val osmId = obj.optLong("osm_id")
                if (!seenKeys.add("$osmType/$osmId")) continue
                // Node + its way can both match; drop pin-duplicates at ~1 m.
                if (!seenCoords.add((Math.round(lat * 1e5) * 100_003L) + Math.round(lon * 1e5))) continue

                val localized = name.ifBlank { displayName.substringBefore(',') }
                val nameAr = extra?.optString("name:ar")?.takeIf { it.isNotBlank() }
                    ?: localized.takeIf { ARABIC_LETTER.containsMatchIn(it) }
                val nameEn = extra?.optString("name:en")?.takeIf { it.isNotBlank() }
                    ?: localized.takeIf { !ARABIC_LETTER.containsMatchIn(it) }
                out.add(Mosque(id = osmId, nameAr = nameAr, nameEn = nameEn, latitude = lat, longitude = lon))
            }
            return out
        }

        /**
         * Converts a Photon GeoJSON FeatureCollection into mosque models.
         *
         * Photon has no religion facet, so the filter runs on the OSM
         * category plus the name: an explicit mosque category
         * (building=mosque / amenity=mosque) is always kept; a generic
         * place_of_worship or landuse=religious container only when its
         * name looks Islamic. Everything else — streets named after a
         * mosque, localities, shops — is dropped (a live Cairo probe
         * returned 25 residential streets among 50 features; all are
         * filtered out by this rule).
         */
        internal fun parsePhotonMosques(body: String): List<Mosque> {
            val features = JSONObject(body).optJSONArray("features") ?: return emptyList()
            val out = mutableListOf<Mosque>()
            val seenKeys = mutableSetOf<String>()
            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val props = feature.optJSONObject("properties") ?: continue
                val key = props.optString("osm_key")
                val value = props.optString("osm_value")
                val explicitMosque = (key == "amenity" && value == "mosque") ||
                    (key == "building" && value == "mosque")
                val maybeReligious = (key == "amenity" && value == "place_of_worship") ||
                    (key == "landuse" && value == "religious")
                if (!explicitMosque && !maybeReligious) continue

                val name = props.optString("name").trim()
                val place = listOfNotNull(
                    name.takeIf { it.isNotBlank() },
                    props.optString("locality").takeIf { it.isNotBlank() },
                    props.optString("district").takeIf { it.isNotBlank() },
                    props.optString("city").takeIf { it.isNotBlank() },
                ).joinToString(", ")
                val nameLooksIslamic = MOSQUE_NAME_HINTS.any { place.contains(it, ignoreCase = true) }
                if (!explicitMosque && !nameLooksIslamic) continue

                val coords = feature.optJSONObject("geometry")
                    ?.optJSONArray("coordinates") ?: continue
                if (coords.length() < 2) continue
                val lon = coords.optDouble(0)
                val lat = coords.optDouble(1)
                if (lat == 0.0 && lon == 0.0) continue
                // Photon osm_type is N / W / R — with osm_id it is unique.
                val osmType = props.optString("osm_type")
                val osmId = props.optLong("osm_id")
                if (!seenKeys.add("$osmType/$osmId")) continue

                val localized = name.ifBlank { props.optString("city").trim() }
                val nameAr = localized.takeIf { it.isNotBlank() && ARABIC_LETTER.containsMatchIn(it) }
                val nameEn = localized.takeIf { it.isNotBlank() && !ARABIC_LETTER.containsMatchIn(it) }
                out.add(Mosque(id = osmId, nameAr = nameAr, nameEn = nameEn, latitude = lat, longitude = lon))
            }
            return out
        }

        /** Exposed for tests: independent-source fixture sanity. */
        internal fun mosqueNameHintsContain(candidate: String): Boolean =
            MOSQUE_NAME_HINTS.any { candidate.contains(it, ignoreCase = true) }
    }
}
