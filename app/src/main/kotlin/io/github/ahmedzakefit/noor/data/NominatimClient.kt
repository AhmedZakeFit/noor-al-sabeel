package io.github.ahmedzakefit.noor.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Optional online city search via Nominatim (OpenStreetMap).
 *
 * Usage policy: identifies the app, low request volume (only when the user types a
 * query that the offline catalog cannot satisfy). No API key, no tracking, results
 * are used locally only.
 */
class NominatimClient {

    data class OnlinePlace(
        val displayName: String,
        val latitude: Double,
        val longitude: Double,
    )

    suspend fun search(query: String, languageTag: String): List<OnlinePlace> =
        withContext(Dispatchers.IO) {
            val q = URLEncoder.encode(query, "UTF-8")
            val lang = URLEncoder.encode(languageTag, "UTF-8")
            val url = URL(
                "https://nominatim.openstreetmap.org/search?q=$q&format=jsonv2&limit=12" +
                    "&accept-language=$lang",
            )
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.setRequestProperty("Accept", "application/json")
                if (conn.responseCode != 200) return@withContext emptyList()
                val body = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
                parse(body)
            } finally {
                conn.disconnect()
            }
        }

    internal fun parse(body: String): List<OnlinePlace> {
        val arr = JSONArray(body)
        val out = mutableListOf<OnlinePlace>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            out.add(
                OnlinePlace(
                    displayName = obj.optString("display_name", ""),
                    latitude = obj.optDouble("lat"),
                    longitude = obj.optDouble("lon"),
                ),
            )
        }
        return out
    }

    companion object {
        private const val USER_AGENT = Http.USER_AGENT
    }
}
