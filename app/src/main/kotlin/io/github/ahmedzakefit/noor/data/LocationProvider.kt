package io.github.ahmedzakefit.noor.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicReference

/**
 * Location acquisition using the platform LocationManager only (no Google Play
 * services dependency), so the app works on F-Droid and de-Googled devices.
 */
class LocationProvider(private val context: Context) {

    data class Fix(val latitude: Double, val longitude: Double, val accuracyM: Float?)

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Returns the best fix obtainable within [timeoutMs], or null. */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentFix(timeoutMs: Long = 15_000L): Fix? {
        if (!hasPermission()) return null
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        ).filter {
            try {
                lm.isProviderEnabled(it)
            } catch (_: SecurityException) {
                false
            } catch (_: IllegalArgumentException) {
                false
            }
        }
        if (providers.isEmpty()) {
            // Last-known location fallback.
            return lastKnown(lm)
        }
        val best = AtomicReference<Location?>(null)
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val main = Looper.getMainLooper()
                var finished = false
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        val current = best.get()
                        if (current == null ||
                            location.accuracy < current.accuracy ||
                            location.time > current.time
                        ) {
                            best.set(location)
                        }
                        // Good enough fix: finish early.
                        if (location.hasAccuracy() && location.accuracy <= 30f && !finished) {
                            finished = true
                            lm.removeUpdates(this)
                            if (cont.isActive) cont.resumeWith(Result.success(fixOf(location)))
                        }
                    }
                }
                try {
                    for (p in providers) {
                        lm.requestLocationUpdates(p, 0L, 0f, listener, main)
                    }
                } catch (e: SecurityException) {
                    if (!finished) {
                        finished = true
                        if (cont.isActive) cont.resumeWith(Result.success(null))
                    }
                }
                cont.invokeOnCancellation {
                    try {
                        lm.removeUpdates(listener)
                    } catch (_: Exception) {
                    }
                }
            } ?: run {
                best.get()?.let { fixOf(it) } ?: lastKnown(lm)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnown(lm: LocationManager): Fix? {
        for (p in listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)) {
            try {
                val loc = lm.getLastKnownLocation(p) ?: continue
                return fixOf(loc)
            } catch (_: SecurityException) {
            } catch (_: IllegalArgumentException) {
            }
        }
        return null
    }

    private fun fixOf(location: Location): Fix =
        Fix(location.latitude, location.longitude, if (location.hasAccuracy()) location.accuracy else null)
}
