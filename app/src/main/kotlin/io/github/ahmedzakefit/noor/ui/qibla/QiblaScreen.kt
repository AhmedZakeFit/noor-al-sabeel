package io.github.ahmedzakefit.noor.ui.qibla

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ahmedzakefit.noor.R
import io.github.ahmedzakefit.noor.ui.common.Formatters
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun QiblaScreen() {
    val vm: QiblaViewModel = viewModel(factory = QiblaViewModel.Factory)
    val state by vm.state.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val arabicDigits = Formatters.useArabicDigits(
        io.github.ahmedzakefit.noor.data.DigitSetting.FOLLOW_LOCALE, locale,
    )

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompassDial(state = state, onDelta = { vm.qiblaDelta(state) })

        Spacer(Modifier.height(16.dp))

        state.qiblaBearing?.let { bearing ->
            Text(
                stringResource(
                    R.string.qibla_bearing_value,
                    Formatters.formatDegrees(bearing, arabicDigits),
                ),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        state.distanceKm?.let { km ->
            Text(
                stringResource(
                    R.string.qibla_distance_from_kaaba,
                    Formatters.formatDecimal(km, 0, arabicDigits),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.declinationDeg?.let { decl ->
            val signed = if (decl >= 0) "+${Formatters.formatDegrees(decl, arabicDigits)}"
            else Formatters.formatDegrees(decl, arabicDigits)
            Text(
                stringResource(R.string.declination_applied, signed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(16.dp))
        CalibrationCard(state)
        if (!state.hasSensor) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.sensor_missing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CompassDial(state: QiblaViewModel.CompassState, onDelta: () -> Double?) {
    val alignedThreshold = 4.0
    val delta = onDelta()
    val aligned = delta != null && abs(delta) <= alignedThreshold
    val azimuth = state.trueAzimuth
    val needleAngle by animateFloatAsState(
        targetValue = delta?.toFloat() ?: 0f,
        animationSpec = tween(durationMillis = 120),
        label = "qibla-needle",
    )
    val ringRotation by animateFloatAsState(
        targetValue = (-(azimuth ?: 0.0)).toFloat(),
        animationSpec = tween(durationMillis = 120),
        label = "compass-ring",
    )

    val lineColor = MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    val gold = MaterialTheme.colorScheme.tertiary
    val alignedColor = MaterialTheme.colorScheme.primary

    val cardinalLabels = listOf(
        stringResource(R.string.dir_n), stringResource(R.string.dir_ne),
        stringResource(R.string.dir_e), stringResource(R.string.dir_se),
        stringResource(R.string.dir_s), stringResource(R.string.dir_sw),
        stringResource(R.string.dir_w), stringResource(R.string.dir_nw),
    )

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f - 12f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Dial background
            drawCircle(color = lineColor.copy(alpha = 0.15f), radius = radius + 8f, center = center)
            drawCircle(color = Color.White.copy(alpha = 0.03f), radius = radius, center = center)
            drawCircle(color = lineColor, radius = radius, center = center, style = Stroke(2f))

            // Rotating cardinal ring (relative to device heading).
            rotate(degrees = ringRotation, pivot = center) {
                for (i in 0 until 8) {
                    val angleRad = Math.toRadians(i * 45.0)
                    val outer = Offset(
                        center.x + (radius * cos(angleRad)).toFloat(),
                        center.y + (radius * sin(angleRad)).toFloat(),
                    )
                    drawLine(
                        color = if (i % 2 == 0) textColor.copy(alpha = 0.85f) else textColor.copy(alpha = 0.4f),
                        start = center + Offset(
                            ((radius - 18f) * cos(angleRad)).toFloat(),
                            ((radius - 18f) * sin(angleRad)).toFloat(),
                        ),
                        end = outer,
                        strokeWidth = if (i % 2 == 0) 6f else 3f,
                        cap = StrokeCap.Round,
                    )
                }
            }

            // Tick marks every 15 degrees (fixed to the rotating ring).
            rotate(degrees = ringRotation, pivot = center) {
                for (deg in 0 until 360 step 15) {
                    if (deg % 45 == 0) continue
                    val angleRad = Math.toRadians(deg.toDouble())
                    drawLine(
                        color = textColor.copy(alpha = 0.25f),
                        start = center + Offset(
                            ((radius - 6f) * cos(angleRad)).toFloat(),
                            ((radius - 6f) * sin(angleRad)).toFloat(),
                        ),
                        end = center + Offset(
                            ((radius - 14f) * cos(angleRad)).toFloat(),
                            ((radius - 14f) * sin(angleRad)).toFloat(),
                        ),
                        strokeWidth = 2f,
                    )
                }
            }

            // Qibla needle: points where the Kaaba is relative to the device's heading.
            rotate(degrees = needleAngle, pivot = center) {
                drawLine(
                    color = if (aligned) alignedColor else gold,
                    start = center,
                    end = Offset(center.x, center.y - radius + 26f),
                    strokeWidth = 10f,
                    cap = StrokeCap.Round,
                )
                // Kaaba marker at the needle tip.
                val tip = Offset(center.x, center.y - radius + 26f)
                drawRect(
                    color = if (aligned) alignedColor else gold,
                    topLeft = Offset(tip.x - 14f, tip.y - 14f),
                    size = androidx.compose.ui.geometry.Size(28f, 28f),
                )
            }

            // Center hub
            drawCircle(color = accent, radius = 12f, center = center)
            drawCircle(color = Color.White, radius = 5f, center = center)
        }

        // Cardinal letters placed inside the rotating ring.
        CardinalRing(rotation = ringRotation, labels = cardinalLabels)
    }

    Spacer(Modifier.height(8.dp))
    Text(
        text = if (aligned) stringResource(R.string.aligned_with_qibla) else "",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )
    AccuracyBadge(state)
}

@Composable
private fun CardinalRing(rotation: Float, labels: List<String>) {
    Box(Modifier.fillMaxSize()) {
        for (i in 0 until 8) {
            val angleDeg = i * 45f + rotation
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val x = 0.5f + 0.40f * cos(angleRad).toFloat()
            val y = 0.5f + 0.40f * sin(angleRad).toFloat()
            Text(
                text = labels[i],
                style = MaterialTheme.typography.titleMedium,
                color = if (i == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier
                    .fillMaxSize()
                    .qiblaPosition(x, y),
            )
        }
    }
}

/** Places text at a fractional position inside its container. */
private fun Modifier.qiblaPosition(x: Float, y: Float): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.place(
            ((constraints.maxWidth * x) - placeable.width / 2).toInt(),
            ((constraints.maxHeight * y) - placeable.height / 2).toInt(),
        )
    }
}

@Composable
private fun AccuracyBadge(state: QiblaViewModel.CompassState) {
    val textRes = when (state.accuracy) {
        null -> null
        in 0..1 -> R.string.accuracy_unreliable
        2 -> R.string.accuracy_low
        3 -> R.string.accuracy_medium
        else -> R.string.accuracy_high
    }
    textRes?.let {
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(it),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CalibrationCard(state: QiblaViewModel.CompassState) {
    Card(
        Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.compass_calibration_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.compass_calibration_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
