package io.github.ahmedzakefit.noor.core.model

/**
 * The six daily time entries computed by Noor Al-Sabeel.
 * SUNRISE is informational (and the anchor for high-latitude adjustments) but is not a notifiable prayer.
 */
enum class Prayer(val key: String, val notifiable: Boolean) {
    FAJR("fajr", true),
    SUNRISE("sunrise", false),
    DHUHR("dhuhr", true),
    ASR("asr", true),
    MAGHRIB("maghrib", true),
    ISHA("isha", true);

    companion object {
        fun fromKey(key: String): Prayer? = entries.firstOrNull { it.key == key }
    }
}
