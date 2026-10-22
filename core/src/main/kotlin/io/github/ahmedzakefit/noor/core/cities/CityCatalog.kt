package io.github.ahmedzakefit.noor.core.cities

import java.io.Reader

/** A city entry in the offline catalog (GeoNames, CC-BY 4.0 - see NOTICE.md). */
data class City(
    val id: String,
    val nameEn: String,
    val nameAr: String?,
    val countryCode: String,
    val timeZoneId: String,
    val latitude: Double,
    val longitude: Double,
    val population: Long,
) {
    /** Best name for the given language ("ar" falls back to English when no Arabic name exists). */
    fun displayName(languageTag: String): String =
        if (languageTag.startsWith("ar") && !nameAr.isNullOrBlank()) nameAr else nameEn
}

/** Normalization helpers so Arabic and Latin queries match reliably. */
object TextNormalization {

    private val latinFold: Map<Char, Char> = buildMap {
        // Latin extended -> ASCII, so "Aswān" matches "aswan" etc.
        val pairs = listOf(
            'ā' to 'a', 'á' to 'a', 'à' to 'a', 'â' to 'a', 'ä' to 'a', 'å' to 'a', 'ã' to 'a',
            'ō' to 'o', 'ó' to 'o', 'ò' to 'o', 'ô' to 'o', 'ö' to 'o', 'õ' to 'o',
            'ū' to 'u', 'ú' to 'u', 'ù' to 'u', 'û' to 'u', 'ü' to 'u',
            'ī' to 'i', 'í' to 'i', 'ì' to 'i', 'î' to 'i', 'ï' to 'i',
            'ē' to 'e', 'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e',
            'ç' to 'c', 'ñ' to 'n', 'ß' to 's', 'đ' to 'd', 'ł' to 'l', 'š' to 's', 'ž' to 'z',
        )
        for ((k, v) in pairs) {
            put(k, v)
            put(k.uppercaseChar(), v.uppercaseChar())
        }
    }

    fun normalize(input: String): String {
        val b = StringBuilder(input.length)
        for (ch in input) {
            when {
                ch in '\u064B'..'\u0652' || ch == '\u0640' -> Unit // tashkeel + tatweel
                ch == '\u0623' || ch == '\u0625' || ch == '\u0622' -> b.append('ا') // alef variants
                ch == '\u0649' -> b.append('ي') // alef maqsura
                ch == '\u0624' -> b.append('و') // waw hamza
                ch == '\u0626' -> b.append('ي') // ya hamza
                ch == '\u0629' -> b.append('ه') // ta marbuta
                latinFold.containsKey(ch) -> b.append(latinFold.getValue(ch))
                else -> b.append(ch.lowercaseChar())
            }
        }
        return b.toString().trim()
    }
}

/** Offline city catalog: parsing, search and nearest-city lookup. All local, no network. */
object CityCatalog {

    /**
     * Parses the bundled CSV (header:
     * geonameid,name_en,name_ar,country,timezone,latitude,longitude,population).
     */
    fun parse(reader: Reader): List<City> {
        val cities = mutableListOf<City>()
        reader.buffered().useLines { lines ->
            var first = true
            for (line in lines) {
                if (first) {
                    first = false
                    continue
                }
                if (line.isBlank()) continue
                val p = splitCsvLine(line)
                if (p.size < 8) continue
                val ar = p[2].trim()
                try {
                    cities.add(
                        City(
                            id = p[0].trim(),
                            nameEn = p[1].trim(),
                            nameAr = ar.ifBlank { null },
                            countryCode = p[3].trim(),
                            timeZoneId = p[4].trim(),
                            latitude = p[5].trim().toDouble(),
                            longitude = p[6].trim().toDouble(),
                            population = p[7].trim().toLongOrNull() ?: 0L,
                        ),
                    )
                } catch (_: NumberFormatException) {
                    // skip malformed row
                }
            }
        }
        return cities
    }

    /**
     * Rank cities for a query: prefix matches first (Arabic before Latin), then substring
     * matches, larger populations first within each tier.
     */
    fun search(cities: List<City>, rawQuery: String, limit: Int = 12): List<City> {
        val q = TextNormalization.normalize(rawQuery)
        if (q.isEmpty()) return emptyList()
        data class Scored(val city: City, val tier: Int)

        val scored = mutableListOf<Scored>()
        for (c in cities) {
            val en = TextNormalization.normalize(c.nameEn)
            val ar = c.nameAr?.let { TextNormalization.normalize(it) }
            val tier = when {
                ar?.startsWith(q) == true -> 0
                en.startsWith(q) -> 1
                ar?.contains(q) == true -> 2
                en.contains(q) -> 3
                else -> continue
            }
            scored.add(Scored(c, tier))
        }
        return scored
            .sortedWith(compareBy({ it.tier }, { -it.city.population }))
            .take(limit)
            .map { it.city }
    }

    /** Nearest catalog city to the given coordinates (linear scan; catalog is small). */
    fun nearest(cities: List<City>, latitude: Double, longitude: Double): City? {
        var best: City? = null
        var bestDist = Double.MAX_VALUE
        for (c in cities) {
            val d = distanceSqDegrees(c.latitude, c.longitude, latitude, longitude)
            if (d < bestDist) {
                bestDist = d
                best = c
            }
        }
        return best
    }

    private fun distanceSqDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = lat1 - lat2
        val dLon = lon1 - lon2
        return dLat * dLat + dLon * dLon
    }

    /** Minimal CSV splitter handling double-quoted fields with "" escapes. */
    internal fun splitCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        cur.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    cur.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> inQuotes = true
                    ',' -> {
                        out.add(cur.toString())
                        cur.setLength(0)
                    }
                    else -> cur.append(ch)
                }
            }
            i++
        }
        out.add(cur.toString())
        return out
    }
}
