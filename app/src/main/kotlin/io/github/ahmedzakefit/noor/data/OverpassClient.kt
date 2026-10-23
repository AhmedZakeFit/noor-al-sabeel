package io.github.ahmedzakefit.noor.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.net.UnknownHostException

/**
 * Nearby-mosque search via the Overpass API (OpenStreetMap data, ODbL).
 *
 * Queries are manual (user taps "find nearby mosques") with a bounded radius,
 * bounded timeouts and a 50-result cap, respecting the public instances'
 * usage policy. The primary public instance regularly answers rate-limited
 * (HTTP 429) or briefly overloaded (5xx) status codes, which used to fail the
 * search outright; transient failures are retried against official mirror
 * endpoints before giving up, and every failure is classified into a
 * user-presentable [Result.Error.reason] so the UI can explain what happened
 * and offer a retry. No API key and no user data is sent beyond the search
 * center.
 *
 * Overpass shares one operational fate across its mirrors — on some mobile
 * networks every endpoint stalls or drops the connection — so this client is
 * now only the *fallback* tier: [MosqueSearchClient] tries the independent
 * Nominatim infrastructure first and falls back to these mirrors here.
 */
class OverpassClient {

    data class Mosque(
        val id: Long,
        val nameAr: String?,
        val nameEn: String?,
        val latitude: Double,
        val longitude: Double,
    )

    sealed interface Result {
        data class Ok(val mosques: List<Mosque>) : Result
        data class Error(val reason: Reason, val message: String?) : Result
    }

    /** Failure categories mapped to localized, actionable messages in the UI. */
    enum class Reason { NO_NETWORK, TIMEOUT, SERVER_BUSY, OTHER }

    suspend fun findMosques(latitude: Double, longitude: Double, radiusMeters: Int): Result {
        val query = "[out:json][timeout:15];" +
            "(node(around:$radiusMeters,$latitude,$longitude)[amenity=place_of_worship]" +
            "[religion=islam];way(around:$radiusMeters,$latitude,$longitude)" +
            "[amenity=place_of_worship][religion=islam];);out center tags 50;"
        var lastError = Result.Error(Reason.OTHER, null)
        for ((index, endpoint) in ENDPOINTS.withIndex()) {
            if (index > 0) delay(MIRROR_PAUSE_MS)
            try {
                val outcome = Http.call(
                    url = endpoint,
                    method = "POST",
                    formBody = "data=" + URLEncoder.encode(query, "UTF-8"),
                    connectTimeoutMs = CONNECT_TIMEOUT_MS,
                    readTimeoutMs = READ_TIMEOUT_MS,
                )
                val code = (outcome as HttpOutcome.Response).code
                if (code == 200 && outcome.body != null) {
                    return Result.Ok(parse(outcome.body))
                }
                // 4xx other than 429 means our request is wrong — mirrors
                // will answer the same, so fail fast instead of hammering.
                if (!isRetryableHttp(code)) {
                    return Result.Error(classifyHttp(code), "HTTP $code")
                }
                lastError = Result.Error(classifyHttp(code), "HTTP $code")
            } catch (e: CancellationException) {
                throw e // leaving the screen / overall deadline — stop immediately
            } catch (e: Exception) {
                // Network-level failures may be specific to one endpoint
                // (DNS/routing/TLS hiccup) — fall through to the next mirror.
                lastError = Result.Error(classifyException(e), e.message)
            }
        }
        return lastError
    }

    internal fun parse(body: String): List<Mosque> {
        val elements: JSONArray = JSONObject(body).optJSONArray("elements") ?: return emptyList()
        val out = mutableListOf<Mosque>()
        val seen = mutableSetOf<Long>()
        for (i in 0 until elements.length()) {
            val e = elements.getJSONObject(i)
            val type = e.optString("type")
            val lat: Double
            val lon: Double
            if (type == "node") {
                lat = e.optDouble("lat")
                lon = e.optDouble("lon")
            } else {
                val center = e.optJSONObject("center") ?: continue
                lat = center.optDouble("lat")
                lon = center.optDouble("lon")
            }
            if (lat == 0.0 && lon == 0.0) continue
            val id = e.optLong("id")
            if (!seen.add(id)) continue
            val tags = e.optJSONObject("tags")
            // Defensive: the query already filters, but keep non-Islamic entries out anyway.
            val religion = tags?.optString("religion")
            if (religion != null && religion != "islam" && religion != "muslim") continue
            out.add(
                Mosque(
                    id = id,
                    nameAr = tags?.optString("name:ar")?.takeIf { it.isNotBlank() },
                    nameEn = tags?.optString("name:en")?.takeIf { it.isNotBlank() }
                        ?: tags?.optString("name")?.takeIf { it.isNotBlank() },
                    latitude = lat,
                    longitude = lon,
                ),
            )
        }
        return out
    }

    companion object {
        /** Public Overpass endpoints: primary first, then official mirrors. */
        internal val ENDPOINTS = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
            "https://overpass.private.coffee/api/interpreter",
        )

        /** politeness pause between mirror attempts (ms). */
        private const val MIRROR_PAUSE_MS = 300L

        /**
         * Tight per-mirror budgets — this client is now only the LAST-resort
         * tier behind two parallel independent sources ([MosqueSearchClient]),
         * and the overall deadline is the hard stop, so these budgets must
         * never let one dead mirror eat the user's patience.
         */
        internal const val CONNECT_TIMEOUT_MS = 3_000
        internal const val READ_TIMEOUT_MS = 6_000

        /** 429 (rate limit) and 5xx (overload) are transient — a mirror may serve. */
        internal fun isRetryableHttp(code: Int): Boolean = code == 429 || code >= 500

        internal fun classifyHttp(code: Int): Reason = when {
            code == 429 || code >= 500 -> Reason.SERVER_BUSY
            else -> Reason.OTHER
        }

        internal fun classifyException(t: Throwable): Reason = when (t) {
            is UnknownHostException, is java.net.ConnectException -> Reason.NO_NETWORK
            is SocketTimeoutException -> Reason.TIMEOUT
            else -> Reason.OTHER
        }

        /**
         * Priority for presenting one reason when several sources failed:
         * "offline?" is the most actionable, then "too slow", then "busy".
         */
        internal fun worseReason(a: Reason, b: Reason): Reason =
            if (reasonRank(a) >= reasonRank(b)) a else b

        private fun reasonRank(r: Reason): Int = when (r) {
            Reason.NO_NETWORK -> 3
            Reason.TIMEOUT -> 2
            Reason.SERVER_BUSY -> 1
            Reason.OTHER -> 0
        }
    }
}
