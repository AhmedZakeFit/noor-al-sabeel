package io.github.ahmedzakefit.noor.data

import io.github.ahmedzakefit.noor.data.OverpassClient.Mosque
import io.github.ahmedzakefit.noor.data.OverpassClient.Reason
import io.github.ahmedzakefit.noor.data.OverpassClient.Result
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max

/**
 * Nearby-mosque search orchestrator — the reliability layer behind the
 * "find nearby mosques" button.
 *
 * v1.0.3 retried three Overpass mirrors sequentially, but all Overpass
 * endpoints share one operational fate: on several mobile networks every one
 * of them stalls or drops the connection, so the search ground through ~30 s
 * of per-socket timeouts and then failed. Root cause: no independent second
 * source and no overall deadline.
 *
 * This client now:
 *  1. asks Nominatim first — independent infrastructure the app already uses
 *     successfully for city search (different servers, different codebase,
 *     plain GET, ~1 s typical). The "mosque" keyword query inside a bounded
 *     viewbox returns OSM places of worship;
 *  2. falls back to the Overpass mirrors only when Nominatim fails, or when
 *     Nominatim legitimately returns zero results (a second chance before
 *     declaring "no mosques nearby");
 *  3. enforces one overall deadline ([TOTAL_DEADLINE_MS]) so the user never
 *     waits longer than that, whatever the networks do — cancellable HTTP
 *     ([Http]) makes the deadline real by disconnecting sockets;
 *  4. leaves the loading state the moment the user leaves the screen —
 *     coroutine cancellation propagates into the same disconnect.
 *
 * Nominatim usage policy: manual, user-initiated searches only, app
 * identified by User-Agent, one request per search, results used locally.
 */
class MosqueSearchClient(private val overpass: OverpassClient) {

    /**
     * @param languageTag current UI language, so Nominatim localizes mosque
     * names where OSM has translations.
     */
    suspend fun findNearbyMosques(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        languageTag: String,
    ): Result {
        try {
            return withTimeout(TOTAL_DEADLINE_MS) {
                var nominatimReason: Reason? = null
                val nominatim = try {
                    searchNominatim(latitude, longitude, radiusMeters, languageTag)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    nominatimReason = OverpassClient.classifyException(e)
                    null
                }

                if (nominatim is Result.Ok && nominatim.mosques.isNotEmpty()) {
                    return@withTimeout nominatim
                }

                // Nominatim failed or found nothing — try the Overpass family.
                val overpassResult = overpass.findMosques(latitude, longitude, radiusMeters)
                when {
                    overpassResult is Result.Ok -> return@withTimeout overpassResult
                    nominatim is Result.Ok ->
                        // Nominatim answered authoritatively with zero mosques and
                        // Overpass is unavailable: report the honest empty answer.
                        return@withTimeout nominatim
                    else -> {
                        // Both tiers failed — present the most actionable reason.
                        val overpassReason = (overpassResult as? Result.Error)?.reason ?: Reason.OTHER
                        val reason = nominatimReason?.let { OverpassClient.worseReason(it, overpassReason) }
                            ?: overpassReason
                        return@withTimeout Result.Error(reason, "all sources failed")
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            return Result.Error(Reason.TIMEOUT, "overall deadline exceeded")
        }
    }

    /**
     * Keyword search on Nominatim, bounded to the search radius. Returns
     * null on any transport/HTTP failure (caller falls back to Overpass).
     */
    private suspend fun searchNominatim(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        languageTag: String,
    ): Result? {
        val url = "https://nominatim.openstreetmap.org/search?q=mosque&format=jsonv2&limit=50" +
            "&bounded=1&viewbox=${viewbox(latitude, longitude, radiusMeters)}" +
            "&extratags=1&accept-language=${urlEncode(languageTag)}"
        val outcome = Http.call(
            url = url,
            connectTimeoutMs = NOMINATIM_CONNECT_TIMEOUT_MS,
            readTimeoutMs = NOMINATIM_READ_TIMEOUT_MS,
            extraHeaders = mapOf("Accept" to "application/json"),
        ) as HttpOutcome.Response
        if (outcome.code != 200 || outcome.body == null) return null
        return Result.Ok(parseNominatimMosques(outcome.body))
    }

    companion object {
        /** The whole search — all sources included — must finish within this budget. */
        internal const val TOTAL_DEADLINE_MS = 20_000L

        private const val NOMINATIM_CONNECT_TIMEOUT_MS = 6_000
        private const val NOMINATIM_READ_TIMEOUT_MS = 12_000

        private fun urlEncode(v: String): String =
            java.net.URLEncoder.encode(v, "UTF-8")

        /**
         * Bounding box (left, top, right, bottom) for a radius in meters.
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

        /** Exposed for tests: independent-source fixture sanity. */
        internal fun mosqueNameHintsContain(candidate: String): Boolean =
            MOSQUE_NAME_HINTS.any { candidate.contains(it, ignoreCase = true) }
    }
}
