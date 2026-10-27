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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val qibla = state.qiblaBearing
        val azimuth = state.trueAzimuth

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(
                Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.qibla_bearing_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (qibla != null) Formatters.formatDegrees(qibla, arabicDigits) else "—",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                state.distanceKm?.let { dist ->
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            stringResource(R.string.distance_to_makkah_km, Formatters.formatInteger(dist.toInt(), arabicDigits)),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        CompassDial(state) {
            if (qibla == null || azimuth == null) null
            else {
                var d = qibla - azimuth
                while (d > 180.0) d -= 360.0
                while (d < -180.0) d += 360.0
                d
            }
        }

        Spacer(Modifier.height(16.dp))

        state.magneticDeclination?.let { decl ->
            val signed = if (decl >= 0.0) "+${Formatters.formatDegrees(decl, arabicDigits)}"
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
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.sensor_missing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
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
    val alignedColor = Color(0xFF1B8A44)

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
            val radius = size.minDimension / 2f - 16f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Outer dial background
            drawCircle(color = lineColor.copy(alpha = 0.12f), radius = radius + 10f, center = center)
            drawCircle(color = accent.copy(alpha = 0.04f), radius = radius, center = center)
            drawCircle(color = lineColor.copy(alpha = 0.6f), radius = radius, center = center, style = Stroke(3f))

            // Rotating cardinal ring
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
                            ((radius - 20f) * cos(angleRad)).toFloat(),
                            ((radius - 20f) * sin(angleRad)).toFloat(),
                        ),
                        end = outer,
                        strokeWidth = if (i % 2 == 0) 6f else 3f,
                        cap = StrokeCap.Round,
                    )
                }
            }

            // Ticks
            rotate(degrees = ringRotation, pivot = center) {
                for (deg in 0 until 360 step 15) {
                    if (deg % 45 == 0) continue
                    val angleRad = Math.toRadians(deg.toDouble())
                    drawLine(
                        color = textColor.copy(alpha = 0.25f),
                        start = center + Offset(
                            ((radius - 8f) * cos(angleRad)).toFloat(),
                            ((radius - 8f) * sin(angleRad)).toFloat(),
                        ),
                        end = center + Offset(
                            ((radius - 14f) * cos(angleRad)).toFloat(),
                            ((radius - 14f) * sin(angleRad)).toFloat(),
                        ),
                        strokeWidth = 2f,
                    )
                }
            }

            // Qibla needle
            rotate(degrees = needleAngle, pivot = center) {
                drawLine(
                    color = if (aligned) alignedColor else gold,
                    start = center,
                    end = Offset(center.x, center.y - radius + 28f),
                    strokeWidth = 10f,
                    cap = StrokeCap.Round,
                )
                val tip = Offset(center.x, center.y - radius + 28f)
                drawRect(
                    color = if (aligned) alignedColor else gold,
                    topLeft = Offset(tip.x - 14f, tip.y - 14f),
                    size = androidx.compose.ui.geometry.Size(28f, 28f),
                )
            }

            // Center hub
            drawCircle(color = if (aligned) alignedColor else accent, radius = 14f, center = center)
            drawCircle(color = Color.White, radius = 6f, center = center)
        }

        CardinalRing(rotation = ringRotation, labels = cardinalLabels)
    }

    Spacer(Modifier.height(12.dp))

    if (aligned) {
        Surface(
            color = Color(0xFF1B8A44),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = stringResource(R.string.aligned_with_qibla),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }

    AccuracyBadge(state)
}

@Composable
private fun CardinalRing(rotation: Float, labels: List<String>) {
    Box(Modifier.fillMaxSize()) {
        for (i in 0 until 8) {
            val angleDeg = i * 45f + rotation
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val x = 0.5f + 0.38f * cos(angleRad).toFloat()
            val y = 0.5f + 0.38f * sin(angleRad).toFloat()
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
        Spacer(Modifier.height(6.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                stringResource(it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun CalibrationCard(state: QiblaViewModel.CompassState) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.CompassCalibration,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    stringResource(R.string.compass_calibration_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(R.string.compass_calibration_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
