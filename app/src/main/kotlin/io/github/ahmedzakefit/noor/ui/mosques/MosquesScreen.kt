package io.github.ahmedzakefit.noor.ui.mosques

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ahmedzakefit.noor.R
import io.github.ahmedzakefit.noor.core.qibla.QiblaMath
import io.github.ahmedzakefit.noor.ui.common.Formatters
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

@Composable
fun MosquesScreen() {
    val vm: MosquesViewModel = viewModel(factory = MosquesViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val arabicDigits = Formatters.useArabicDigits(
        io.github.ahmedzakefit.noor.data.DigitSetting.FOLLOW_LOCALE, locale,
    )
    val userMarkerIcon = remember {
        BitmapDrawable(context.resources, circleBitmap(0xFF1565C0.toInt(), 0xFFFFFFFF.toInt()))
    }
    val mosqueMarkerIcon = remember {
        BitmapDrawable(context.resources, circleBitmap(0xFF2E7D32.toInt(), 0xFFFFFFFF.toInt()))
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        // Radius chips + search button
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (option in vm.radiusOptions) {
                FilterChip(
                    selected = state.radiusKm == option,
                    onClick = { vm.setRadius(option) },
                    label = {
                        val km = stringResource(R.string.mosque_distance_km, option.toString())
                        Text(km.substringAfter(" ").ifBlank { option.toString() })
                    },
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.search_radius, state.radiusKm),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { vm.search() }, enabled = state.location != null) {
                Text(stringResource(R.string.search_nearby_mosques))
            }
        }

        // Map
        Box(Modifier.fillMaxWidth().height(280.dp)) {
            val mapCenter = state.location?.let { GeoPoint(it.latitude, it.longitude) }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(14.5)
                    }
                },
                update = { map ->
                    if (mapCenter != null) {
                        map.controller.setCenter(mapCenter)
                        map.overlays.removeAll { it is Marker }
                        val user = Marker(map)
                        user.position = mapCenter
                        user.icon = userMarkerIcon
                        user.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        map.overlays.add(user)
                        val search = state.searchState
                        if (search is MosquesViewModel.SearchState.Ready) {
                            for (item in search.items) {
                                val m = Marker(map)
                                m.position = GeoPoint(item.mosque.latitude, item.mosque.longitude)
                                m.icon = mosqueMarkerIcon
                                m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                map.overlays.add(m)
                            }
                        }
                        map.invalidate()
                    }
                },
            )
            Text(
                stringResource(R.string.osm_attribution),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp),
            )
        }

        // Results
        when (val search = state.searchState) {
            MosquesViewModel.SearchState.Idle -> {
                if (state.location == null) {
                    InfoText(stringResource(R.string.location_not_set))
                } else {
                    InfoText(stringResource(R.string.mosques_need_internet))
                }
            }
            MosquesViewModel.SearchState.Loading -> {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            MosquesViewModel.SearchState.Empty -> InfoText(stringResource(R.string.no_mosques_found))
            is MosquesViewModel.SearchState.Error -> InfoText(stringResource(R.string.mosques_search_failed))
            is MosquesViewModel.SearchState.Ready -> {
                Text(
                    stringResource(R.string.mosques_results_count, search.items.size),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                LazyColumn(Modifier.fillMaxSize()) {
                    items(search.items, key = { it.mosque.id }) { item ->
                        MosqueRow(item, locale, arabicDigits)
                    }
                }
            }
        }
    }
}

@Composable
private fun MosqueRow(
    item: MosquesViewModel.MosqueItem,
    locale: Locale,
    arabicDigits: Boolean,
) {
    val context = LocalContext.current
    val mosque = item.mosque
    val name = if (locale.language == "ar") {
        mosque.nameAr ?: mosque.nameEn ?: stringResource(R.string.mosque_unnamed)
    } else {
        mosque.nameEn ?: mosque.nameAr ?: stringResource(R.string.mosque_unnamed)
    }
    val dirLabel = compassLabel(QiblaMath.compassIndex(item.bearing))
    val distanceLabel = if (item.distanceKm < 1.0) {
        stringResource(
            R.string.mosque_distance_m,
            Formatters.digits("${(item.distanceKm * 1000).toInt()}", arabicDigits),
        )
    } else {
        stringResource(
            R.string.mosque_distance_km,
            Formatters.formatDecimal(item.distanceKm, 1, arabicDigits),
        )
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(
                "$distanceLabel • $dirLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = {
            val uri = Uri.parse(
                "geo:${mosque.latitude},${mosque.longitude}?q=${mosque.latitude},${mosque.longitude}" +
                    "(${Uri.encode(name)})",
            )
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        }) {
            Icon(
                Icons.Filled.Send,
                contentDescription = stringResource(R.string.navigate),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun compassLabel(index: Int): String = when (index % 8) {
    0 -> stringResource(R.string.dir_n)
    1 -> stringResource(R.string.dir_ne)
    2 -> stringResource(R.string.dir_e)
    3 -> stringResource(R.string.dir_se)
    4 -> stringResource(R.string.dir_s)
    5 -> stringResource(R.string.dir_sw)
    6 -> stringResource(R.string.dir_w)
    else -> stringResource(R.string.dir_nw)
}

@Composable
private fun InfoText(text: String) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun circleBitmap(fillColor: Int, strokeColor: Int): Bitmap {
    val size = 56
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6f, fill)
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6f, stroke)
    return bitmap
}
