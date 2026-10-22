package io.github.ahmedzakefit.noor.ui.qibla

import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import io.github.ahmedzakefit.noor.AppContainer
import io.github.ahmedzakefit.noor.core.qibla.QiblaMath
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Qibla compass state: rotation-vector sensor smoothed azimuth, magnetic
 * declination from the platform GeomagneticField model, and the great-circle
 * qibla bearing for the selected location.
 */
class QiblaViewModel(private val container: AppContainer) : ViewModel() {

    data class CompassState(
        val hasSensor: Boolean = false,
        /** Device azimuth relative to magnetic north, already declination-corrected (true north). */
        val trueAzimuth: Double? = null,
        val accuracy: Int? = null,
        val qiblaBearing: Double? = null,
        val distanceKm: Double? = null,
        val declinationDeg: Double? = null,
    )

    private val sensorFlow = callbackFlow {
        val manager = container.appContext.getSystemService(SensorManager::class.java)
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (manager == null || sensor == null) {
            trySend(SensorTick(azimuth = null, accuracy = null, available = false))
            awaitClose { }
            return@callbackFlow
        }
        val smoothed = doubleArrayOf(Double.NaN)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val r = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(r, event.values)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(r, orientation)
                val azimuth = Math.toDegrees(orientation[0].toDouble())
                // Simple exponential smoothing to steady the needle.
                smoothed[0] = if (smoothed[0].isNaN()) {
                    azimuth
                } else {
                    var delta = azimuth - smoothed[0]
                    if (delta > 180) delta -= 360.0
                    if (delta < -180) delta += 360.0
                    smoothed[0] + 0.25 * delta
                }
                trySend(SensorTick(azimuth = ((smoothed[0] % 360.0) + 360.0) % 360.0, accuracy = event.accuracy, available = true))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Reported with each event as well.
            }
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { manager.unregisterListener(listener) }
    }

    val state: StateFlow<CompassState> =
        kotlinx.coroutines.flow.combine(
            container.settingsRepository.settings,
            sensorFlow,
        ) { settings, tick ->
            val loc = settings.location
            val declination = if (loc != null) {
                GeomagneticField(
                    loc.latitude.toFloat(),
                    loc.longitude.toFloat(),
                    0f,
                    System.currentTimeMillis(),
                ).declination.toDouble()
            } else {
                null
            }
            val trueAzimuth = tick.azimuth?.let { magnetic ->
                var t = magnetic + (declination ?: 0.0)
                t = ((t % 360.0) + 360.0) % 360.0
                t
            }
            CompassState(
                hasSensor = tick.available,
                trueAzimuth = trueAzimuth,
                accuracy = tick.accuracy,
                qiblaBearing = loc?.let { QiblaMath.qiblaBearing(it.latitude, it.longitude) },
                distanceKm = loc?.let { QiblaMath.distanceToKaabaKm(it.latitude, it.longitude) },
                declinationDeg = declination,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompassState())

    /** Signed angular difference between the current heading and the qibla. */
    fun qiblaDelta(state: CompassState): Double? {
        val azimuth = state.trueAzimuth ?: return null
        val bearing = state.qiblaBearing ?: return null
        var delta = bearing - azimuth
        while (delta > 180) delta -= 360.0
        while (delta < -180) delta += 360.0
        return delta
    }

    internal data class SensorTick(val azimuth: Double?, val accuracy: Int?, val available: Boolean)

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[
                        androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
                    ] as io.github.ahmedzakefit.noor.NoorApplication
                    QiblaViewModel(app.container)
                }
            }
    }
}
