package io.github.ahmedzakefit.noor.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Nearby-mosque search via the Overpass API (OpenStreetMap data, ODbL).
 *
 * Queries are manual (user taps "find nearby mosques") with a bounded radius,
 * a 20-second timeout and a 50-result cap, respecting the public instance's
 * usage policy. No API key and no user data is sent beyond the search center.
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
        data class Error(val message: String?) : Result
    }

    suspend fun findMosques(latitude: Double, longitude: Double, radiusMeters: Int): Result =
        withContext(Dispatchers.IO) {
            val query = "[out:json][timeout:20];" +
                "(node(around:$radiusMeters,$latitude,$longitude)[amenity=place_of_worship]" +
                "[religion=islam];way(around:$radiusMeters,$latitude,$longitude)" +
                "[amenity=place_of_worship][religion=islam];);out center tags 50;"
            try {
                val conn = URL("https://overpass-api.de/api/interpreter").openConnection() as HttpURLConnection
                try {
                    conn.connectTimeout = 10_000
                    conn.readTimeout = 25_000
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.setRequestProperty("User-Agent", USER_AGENT)
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { w ->
                        w.write("data=" + URLEncoder.encode(query, "UTF-8"))
                    }
                    if (conn.responseCode != 200) {
                        return@withContext Result.Error("HTTP ${conn.responseCode}")
                    }
                    val body = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
                    Result.Ok(parse(body))
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                Result.Error(e.message)
            }
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
            "NOOR/1.0 (open-source prayer times app; github.com/AhmedZakeFit/noor)"
    }
}
