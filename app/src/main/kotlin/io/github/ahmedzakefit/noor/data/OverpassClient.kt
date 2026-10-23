package io.github.ahmedzakefit.noor.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException

/**
 * Nearby-mosque search via the Overpass API (OpenStreetMap data, ODbL).
 *
 * Queries are manual (user taps "find nearby mosques") with a bounded radius,
 * bounded timeouts and a 50-result cap, respecting the public instances'
 * usage policy. The primary public instance regularly answers rate-limited
 * (HTTP 429) or briefly overloaded (5xx) status codes, which used to fail the
 * search outright; transient failures are now retried against official mirror
 * endpoints before giving up, and every failure is classified into a
 * user-presentable [Result.Error.reason] so the UI can explain what happened
 * and offer a retry. No API key and no user data is sent beyond the search
 * center.
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

    suspend fun findMosques(latitude: Double, longitude: Double, radiusMeters: Int): Result =
        withContext(Dispatchers.IO) {
            val query = "[out:json][timeout:20];" +
                "(node(around:$radiusMeters,$latitude,$longitude)[amenity=place_of_worship]" +
                "[religion=islam];way(around:$radiusMeters,$latitude,$longitude)" +
                "[amenity=place_of_worship][religion=islam];);out center tags 50;"
            var lastError = Result.Error(Reason.OTHER, null)
            for ((index, endpoint) in ENDPOINTS.withIndex()) {
                if (index > 0) delay(MIRROR_PAUSE_MS)
                var conn: HttpURLConnection? = null
                try {
                    conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 10_000
                        readTimeout = 25_000
                        requestMethod = "POST"
                        doOutput = true
                        setRequestProperty("User-Agent", USER_AGENT)
                        setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    }
                    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { w ->
                        w.write("data=" + URLEncoder.encode(query, "UTF-8"))
                    }
                    val code = conn.responseCode
                    if (code == 200) {
                        return@withContext Result.Ok(parse(conn.inputStream.bufferedReader(Charsets.UTF_8).readText()))
                    }
                    // 4xx other than 429 means our request is wrong — mirrors
                    // will answer the same, so fail fast instead of hammering.
                    if (!isRetryableHttp(code)) {
                        return@withContext Result.Error(classifyHttp(code), "HTTP $code")
                    }
                    lastError = Result.Error(classifyHttp(code), "HTTP $code")
                } catch (e: Exception) {
                    // Network-level failures may be specific to one endpoint
                    // (DNS/routing/TLS hiccup) — fall through to the next mirror.
                    lastError = Result.Error(classifyException(e), e.message)
                } finally {
                    conn?.disconnect()
                }
            }
            lastError
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
        private const val USER_AGENT =
            "NoorAlSabeel/1.0.3 (open-source prayer times app; github.com/AhmedZakeFit/noor-al-sabeel)"

        /** Public Overpass endpoints: primary first, then official mirrors. */
        internal val ENDPOINTS = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
            "https://overpass.private.coffee/api/interpreter",
        )

        /** politeness pause between mirror attempts (ms). */
        private const val MIRROR_PAUSE_MS = 500L

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
    }
}
