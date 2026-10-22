package io.github.ahmedzakefit.noor.core.qibla

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Qibla direction and great-circle distance math. All computations are local. */
object QiblaMath {

    const val KAABA_LATITUDE = 21.4225
    const val KAABA_LONGITUDE = 39.8262

    /**
     * Great-circle initial bearing (degrees clockwise from true north) from the given
     * position toward the Kaaba. Valid everywhere on Earth except near the antipode.
     */
    fun qiblaBearing(latitude: Double, longitude: Double): Double {
        val phi1 = Math.toRadians(latitude)
        val phi2 = Math.toRadians(KAABA_LATITUDE)
        val deltaLambda = Math.toRadians(KAABA_LONGITUDE - longitude)
        val theta = atan2(
            sin(deltaLambda),
            cos(phi1) * kotlin.math.tan(phi2) - sin(phi1) * cos(deltaLambda),
        )
        val bearing = Math.toDegrees(theta)
        return ((bearing % 360.0) + 360.0) % 360.0
    }

    /**
     * Great-circle initial bearing (degrees clockwise from true north) from point 1 to point 2.
     */
    fun bearingBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val theta = atan2(
            sin(deltaLambda) * cos(phi2),
            cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda),
        )
        return ((Math.toDegrees(theta) % 360.0) + 360.0) % 360.0
    }

    /** Great-circle distance in kilometres from the given position to the Kaaba. */
    fun distanceToKaabaKm(latitude: Double, longitude: Double): Double =
        haversineKm(latitude, longitude, KAABA_LATITUDE, KAABA_LONGITUDE)

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0088 // mean Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }

    /** Compass point index (0=N, 1=NE, 2=E, ... 7=NW) for a bearing, for display purposes. */
    fun compassIndex(bearing: Double): Int = (((bearing + 22.5) % 360.0) / 45.0).toInt()
}
