package io.github.ahmedzakefit.noor.ui.mosques

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ahmedzakefit.noor.R
import io.github.ahmedzakefit.noor.core.qibla.QiblaMath
import io.github.ahmedzakefit.noor.data.OverpassClient
import io.github.ahmedzakefit.noor.ui.common.Formatters
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION,
)

/** Holds the MapView across recompositions without triggering them. */
private class MapHolder { var mapView: MapView? = null }

@Composable
fun MosquesScreen() {
    val vm: MosquesViewModel = viewModel(factory = MosquesViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val density = LocalDensity.current.density
    val arabicDigits = Formatters.useArabicDigits(
        io.github.ahmedzakefit.noor.data.DigitSetting.FOLLOW_LOCALE, locale,
    )
    val userMarkerIcon = remember {
        BitmapDrawable(context.resources, circleBitmap(0xFF1565C0.toInt(), 0xFFFFFFFF.toInt()))
    }
    val mosqueMarkerIcon = remember {
        BitmapDrawable(context.resources, circleBitmap(0xFF2E7D32.toInt(), 0xFFFFFFFF.toInt()))
    }

    // Location permission flow, wired into the MAIN search button (same
    // in-context pattern as the settings location picker): a short rationale
    // first, then the system dialog, then the search the user asked for.
    // A denial keeps the guidance on screen — with a way out via the app
    // settings when the system no longer shows the dialog.
    var showLocationRationale by remember { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        vm.refreshLocationPermission()
        if (grants.values.any { it }) vm.search(locale.toLanguageTag()) else vm.markPermissionDenied()
    }

    // Leaving the screen aborts an in-flight fix or search — the socket is
    // disconnected, no spinner is ever stranded, and the map view is detached.
    DisposableEffect(Unit) {
        onDispose { vm.cancelSearch() }
    }

    val mapHolder = remember { MapHolder() }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // The grant may have changed while the screen was covered
                    // (system dialog or app settings): re-read it and, when a
                    // search was waiting on exactly that grant, start it now.
                    vm.onPermissionPossiblyChanged(locale.toLanguageTag())
                    mapHolder.mapView?.onResume()
                }
                Lifecycle.Event.ON_PAUSE -> mapHolder.mapView?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // The MapView itself is detached by AndroidView's onRelease —
            // detaching here too would run MapView.onDetach() twice.
            mapHolder.mapView = null
        }
    }

    // Adaptive map height: a bounded fraction of the screen, so small phones
    // and landscape keep room for the results list below.
    val mapHeight = MosquesViewModel.mapHeightDp(LocalConfiguration.current.screenHeightDp)

    // What the map and label revolve around: the last real search center,
    // falling back to the saved app location before any search.
    val mapCenter = state.activeCenter ?: MosquesViewModel.fallbackCenter(state.location)
    val readyItems = (state.searchState as? MosquesViewModel.SearchState.Ready)?.items
    val inFlight = state.searchState is MosquesViewModel.SearchState.Locating ||
        state.searchState is MosquesViewModel.SearchState.Loading

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        // Radius chips + search button
        val distancePattern = stringResource(R.string.mosque_distance_km)
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
                        Text(MosquesViewModel.radiusChipLabel(option, arabicDigits, distancePattern))
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
                stringResource(
                    R.string.search_radius,
                    Formatters.digits(state.radiusKm.toString(), arabicDigits),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = {
                    // The grant is read at the moment of the tap: with it, a
                    // current-location search; without it, the in-context
                    // permission request — never a silent saved-location
                    // search (2026-10-24 field-report fix).
                    if (vm.locationPermissionGranted()) vm.search(locale.toLanguageTag())
                    else showLocationRationale = true
                },
                enabled = !inFlight,
            ) {
                Text(stringResource(R.string.search_nearby_mosques))
            }
        }

        // Which area the search revolves around — always visible, so a
        // saved-location search is recognized as such (the saved location
        // is only ever searched as an explicit on-screen choice).
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val languageTag = locale.toLanguageTag()
            val centerLabel = when {
                mapCenter != null && mapCenter.isCurrentLocation ->
                    stringResource(R.string.mosques_center_current)
                mapCenter?.saved != null ->
                    stringResource(
                        R.string.mosques_center_saved,
                        mapCenter.saved!!.displayLabel(languageTag),
                    )
                state.hasLocationPermission ->
                    stringResource(R.string.mosques_center_current)
                else -> null
            }
            if (centerLabel != null) {
                Text(
                    centerLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        }

        // Map — bounded box, gestures stay inside it. The zoom fit needs the
        // view's REAL pixel size; flip [mapMeasured] once laid out so the
        // sync effect below re-runs with true dimensions instead of the
        // pre-measurement fallback estimate.
        var mapMeasured by remember { mutableStateOf(false) }
        Box(Modifier.fillMaxWidth().height(mapHeight.dp)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        // Pan/pinch inside the map only; no built-in zoom
                        // buttons overlapping the attribution or the tiles.
                        setMultiTouchControls(true)
                        setBuiltInZoomControls(false)
                        minZoomLevel = MosquesViewModel.MIN_ZOOM
                        maxZoomLevel = MosquesViewModel.MAX_ZOOM
                    }.also { mv ->
                        mapHolder.mapView = mv
                        mv.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                            if (v.width > 0 && v.height > 0 && !mapMeasured) mapMeasured = true
                        }
                    }
                },
                onRelease = { it.onDetach() },
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

        // Map sync — keyed effects run ONLY when the center, the radius, the
        // result set or the first real measurement actually changes. A plain
        // recomposition (theme, language, unrelated state) never re-centers
        // the map again, so the user's pan and zoom survive until they mean
        // something new.
        LaunchedEffect(mapCenter, state.radiusKm, readyItems, mapMeasured) {
            val map = mapHolder.mapView ?: return@LaunchedEffect
            val center = mapCenter ?: return@LaunchedEffect
            val minDimPx = if (map.width > 0 && map.height > 0) {
                minOf(map.width, map.height).toFloat()
            } else {
                360f * density
            }
            map.controller.setCenter(GeoPoint(center.latitude, center.longitude))
            map.controller.setZoom(
                MosquesViewModel.zoomForRadius(state.radiusKm * 1000, center.latitude, minDimPx),
            )
            syncMarkers(map, center, readyItems, userMarkerIcon, mosqueMarkerIcon)
            map.invalidate()
        }

        // Results — exactly the remaining space, so the list always fits the
        // screen and scrolls within its own bounded area.
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (val search = state.searchState) {
                MosquesViewModel.SearchState.Idle -> {
                    if (state.hasLocationPermission) {
                        InfoText(stringResource(R.string.mosques_need_internet))
                    } else {
                        InfoText(stringResource(R.string.mosques_permission_hint))
                    }
                }
                MosquesViewModel.SearchState.PermissionRequired -> {
                    // Location access is missing: explain, offer the in-context
                    // grant path, the app-settings way out of a permanent
                    // denial, and the saved location ONLY as an explicit choice.
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            stringResource(R.string.mosques_permission_needed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        TextButton(onClick = { showLocationRationale = true }) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.mosques_allow_location))
                        }
                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null),
                                    ),
                                )
                            }
                        }) { Text(stringResource(R.string.mosques_open_settings)) }
                        if (state.location != null) {
                            TextButton(onClick = { vm.searchAroundSaved(locale.toLanguageTag()) }) {
                                Text(stringResource(R.string.mosques_fallback_saved))
                            }
                        }
                    }
                }
                MosquesViewModel.SearchState.Locating -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.mosques_locating),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                MosquesViewModel.SearchState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                MosquesViewModel.SearchState.Empty -> InfoText(stringResource(R.string.no_mosques_found))
                MosquesViewModel.SearchState.LocationFailed -> {
                    // GPS fix could not be obtained: say so and offer both a
                    // retry and the explicit saved-location fallback.
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            stringResource(R.string.mosques_location_failed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Row {
                            TextButton(onClick = { vm.search(locale.toLanguageTag()) }) {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.retry))
                            }
                            if (state.location != null) {
                                TextButton(onClick = { vm.searchAroundSaved(locale.toLanguageTag()) }) {
                                    Text(stringResource(R.string.mosques_fallback_saved))
                                }
                            }
                        }
                    }
                }
                is MosquesViewModel.SearchState.Error -> {
                    // Explain what actually went wrong and offer an explicit retry
                    // instead of failing silently after the loading spinner.
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            reasonMessage(search.reason),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        TextButton(onClick = { vm.search(locale.toLanguageTag()) }) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                is MosquesViewModel.SearchState.Ready -> {
                    Column(Modifier.fillMaxSize()) {
                        Text(
                            stringResource(R.string.mosques_results_count, search.items.size),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                            items(search.items, key = { it.mosque.id }) { item ->
                                MosqueRow(item, locale, arabicDigits)
                            }
                        }
                    }
                }
            }
        }
    }

    // Composed AFTER the screen so it layers on top: a short "why do you
    // need this?" explanation shown before the system permission dialog.
    if (showLocationRationale) {
        AlertDialog(
            onDismissRequest = { showLocationRationale = false },
            title = { Text(stringResource(R.string.mosques_use_current_location)) },
            text = { Text(stringResource(R.string.gps_permission_rationale)) },
            confirmButton = {
                TextButton(onClick = {
                    showLocationRationale = false
                    locationPermissionLauncher.launch(LOCATION_PERMISSIONS)
                }) { Text(stringResource(R.string.common_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { showLocationRationale = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

/** Idempotent marker sync: user pin + one marker per result. */
private fun syncMarkers(
    map: MapView,
    center: MosquesViewModel.SearchCenter,
    items: List<MosquesViewModel.MosqueItem>?,
    userIcon: Drawable,
    mosqueIcon: Drawable,
) {
    map.overlays.removeAll { it is Marker }
    val user = Marker(map)
    user.position = GeoPoint(center.latitude, center.longitude)
    user.icon = userIcon
    user.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    map.overlays.add(user)
    if (items != null) {
        for (item in items) {
            val m = Marker(map)
            m.position = GeoPoint(item.mosque.latitude, item.mosque.longitude)
            m.icon = mosqueIcon
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            map.overlays.add(m)
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
private fun reasonMessage(reason: OverpassClient.Reason): String = when (reason) {
    OverpassClient.Reason.NO_NETWORK -> stringResource(R.string.mosque_search_failed_offline)
    OverpassClient.Reason.TIMEOUT -> stringResource(R.string.mosque_search_failed_timeout)
    OverpassClient.Reason.SERVER_BUSY -> stringResource(R.string.mosque_search_failed_busy)
    OverpassClient.Reason.OTHER -> stringResource(R.string.mosques_search_failed)
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
