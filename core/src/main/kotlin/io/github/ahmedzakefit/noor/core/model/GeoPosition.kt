package io.github.ahmedzakefit.noor.core.model

/** A geographic position used for prayer-time and qibla calculations. */
data class GeoPosition(
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double = 0.0,
    val timeZoneId: String = "UTC",
) {
    init {
        require(latitude in -90.0..90.0) { "latitude must be within [-90, 90]: $latitude" }
        require(longitude in -180.0..180.0) { "longitude must be within [-180, 180]: $longitude" }
    }
}
