package io.github.ahmedzakefit.noor.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.ahmedzakefit.noor.R
import io.github.ahmedzakefit.noor.appContainer
import io.github.ahmedzakefit.noor.core.cities.City
import io.github.ahmedzakefit.noor.core.cities.CityCatalog
import io.github.ahmedzakefit.noor.core.qibla.QiblaMath
import io.github.ahmedzakefit.noor.data.AppLocation
import io.github.ahmedzakefit.noor.data.NominatimClient
import io.github.ahmedzakefit.noor.ui.common.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.util.Locale

private val COMMON_TIMEZONES = listOf(
    "Africa/Cairo", "Africa/Casablanca", "Africa/Khartoum", "Africa/Nairobi",
    "Asia/Riyadh", "Asia/Dubai", "Asia/Kuwait", "Asia/Qatar", "Asia/Baghdad",
    "Asia/Amman", "Asia/Jerusalem", "Asia/Istanbul", "Asia/Tehran", "Asia/Karachi",
    "Asia/Dhaka", "Asia/Jakarta", "Asia/Kuala_Lumpur", "Europe/London", "Europe/Paris",
    "Europe/Berlin", "Europe/Moscow", "America/New_York", "America/Chicago",
    "America/Los_Angeles", "Australia/Sydney",
)

private enum class GpsState { Idle, Locating, Done, Failed, Denied }

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerDialog(
    onDismiss: () -> Unit,
    onLocationSelected: (AppLocation) -> Unit,
) {
    val context = LocalContext.current
    val container = context.appContainer
    val scope = rememberCoroutineScope()
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()

    var tab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var onlineResults by remember { mutableStateOf<List<NominatimClient.OnlinePlace>>(emptyList()) }
    var gpsState by remember { mutableStateOf(GpsState.Idle) }
    var showGpsRationale by remember { mutableStateOf(false) }
    var latText by remember { mutableStateOf("") }
    var lonText by remember { mutableStateOf("") }
    var tzSelection by remember { mutableStateOf(ZoneId.systemDefault().id) }
    var tzExpanded by remember { mutableStateOf(false) }

    val catalog = remember { container.cityCatalog }
    val offlineResults = remember(query, catalog) {
        if (query.isBlank()) emptyList() else CityCatalog.search(catalog, query, 8)
    }

    // Debounced online search when the offline catalog cannot satisfy the query.
    LaunchedEffect(query) {
        if (query.length < 3) {
            onlineResults = emptyList()
            return@LaunchedEffect
        }
        delay(600)
        val results = withContext(Dispatchers.IO) {
            runCatching { container.nominatimClient.search(query, locale.toLanguageTag()) }
                .getOrDefault(emptyList())
        }
        onlineResults = results
    }

    fun selectCity(city: City) {
        onLocationSelected(
            AppLocation(
                latitude = city.latitude,
                longitude = city.longitude,
                timeZoneId = city.timeZoneId,
                labelEn = city.nameEn,
                labelAr = city.nameAr,
                source = AppLocation.Source.CITY,
            ),
        )
        onDismiss()
    }

    fun selectOnline(place: NominatimClient.OnlinePlace) {
        val short = place.displayName.substringBefore(",").trim()
        val nearest = CityCatalog.nearest(catalog, place.latitude, place.longitude)
        val tz = nearest
            ?.takeIf {
                QiblaMath.haversineKm(place.latitude, place.longitude, it.latitude, it.longitude) < 300.0
            }
            ?.timeZoneId
            ?: ZoneId.systemDefault().id
        onLocationSelected(
            AppLocation(
                latitude = place.latitude,
                longitude = place.longitude,
                timeZoneId = tz,
                labelEn = short,
                labelAr = short,
                source = AppLocation.Source.MANUAL,
            ),
        )
        onDismiss()
    }

    fun startLocating() {
        gpsState = GpsState.Locating
        scope.launch {
            val fix = withContext(Dispatchers.IO) { container.locationProvider.getCurrentFix() }
            if (fix == null) {
                gpsState = if (container.locationProvider.hasPermission()) GpsState.Failed else GpsState.Denied
            } else {
                val nearest = CityCatalog.nearest(catalog, fix.latitude, fix.longitude)
                val near = nearest != null &&
                    QiblaMath.haversineKm(fix.latitude, fix.longitude, nearest!!.latitude, nearest!!.longitude) < 60.0
                onLocationSelected(
                    AppLocation(
                        latitude = fix.latitude,
                        longitude = fix.longitude,
                        timeZoneId = ZoneId.systemDefault().id,
                        labelEn = if (near) nearest!!.nameEn
                        else "%.4f, %.4f".format(fix.latitude, fix.longitude),
                        labelAr = if (near) nearest!!.nameAr else null,
                        source = AppLocation.Source.AUTO,
                    ),
                )
                gpsState = GpsState.Done
                onDismiss()
            }
        }
    }

    // The result callback re-checks the grant and resumes the flow the user
    // actually asked for: granting the dialog starts locating immediately, so
    // first-run setup never dead-ends until the app is restarted.
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) {
            startLocating()
        } else {
            gpsState = GpsState.Denied
        }
    }

    fun useGps() {
        // Never assume the permission: check at the moment of use, ask in the
        // system dialog (with a short in-app explanation first), and only run
        // the location lookup while a grant is actually held.
        if (container.locationProvider.hasPermission()) {
            startLocating()
        } else {
            showGpsRationale = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pick_location)) },
        text = {
            Column {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.tab_search)) })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.tab_coordinates)) })
                }
                Spacer(Modifier.height(8.dp))
                when (tab) {
                    0 -> {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text(stringResource(R.string.search_city_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Text(
                            stringResource(R.string.city_search_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                        if (offlineResults.isEmpty() && onlineResults.isEmpty() && query.length >= 2) {
                            Text(
                                stringResource(R.string.no_results),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                        LazyColumn(Modifier.height(240.dp)) {
                            items(offlineResults, key = { "c" + it.id }) { city ->
                                ListItem(
                                    headlineContent = { Text(city.displayName(locale.toLanguageTag())) },
                                    supportingContent = { Text(city.countryCode + " • " + city.timeZoneId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingContent = {
                                        Icon(
                                            Icons.Filled.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    },
                                )
                            }
                            items(
                                onlineResults,
                                key = { "o" + it.latitude.toString() + "," + it.longitude.toString() },
                            ) { place ->
                                ListItem(
                                    headlineContent = { Text(place.displayName.substringBefore(",")) },
                                    supportingContent = { Text(place.displayName) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        when (gpsState) {
                            GpsState.Locating -> Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.width(20.dp).height(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.locating))
                            }
                            GpsState.Denied -> Text(
                                stringResource(R.string.gps_permission_denied),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            GpsState.Failed -> Text(
                                stringResource(R.string.gps_failed),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            else -> {}
                        }
                        TextButton(onClick = { useGps() }) {
                            Text(stringResource(R.string.use_gps))
                        }
                    }
                    1 -> {
                        OutlinedTextField(
                            value = latText,
                            onValueChange = { latText = it },
                            label = { Text(stringResource(R.string.latitude)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = lonText,
                            onValueChange = { lonText = it },
                            label = { Text(stringResource(R.string.longitude)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.timezone_label))
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { tzExpanded = true }) { Text(tzSelection) }
                            androidx.compose.material3.DropdownMenu(
                                expanded = tzExpanded,
                                onDismissRequest = { tzExpanded = false },
                            ) {
                                val zones = remember {
                                    listOf(ZoneId.systemDefault().id) +
                                        COMMON_TIMEZONES.filter { it != ZoneId.systemDefault().id }
                                }
                                zones.forEach { zone ->
                                    DropdownMenuItem(
                                        text = { Text(zone) },
                                        onClick = {
                                            tzSelection = zone
                                            tzExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                        val lat = latText.toDoubleOrNull()
                        val lon = lonText.toDoubleOrNull()
                        val valid = lat != null && lon != null &&
                            lat in -90.0..90.0 && lon in -180.0..180.0
                        TextButton(
                            onClick = {
                                if (valid) {
                                    val nearest = CityCatalog.nearest(catalog, lat!!, lon!!)
                                    val near = nearest != null &&
                                        QiblaMath.haversineKm(lat, lon, nearest!!.latitude, nearest!!.longitude) < 60.0
                                    onLocationSelected(
                                        AppLocation(
                                            latitude = lat,
                                            longitude = lon!!,
                                            timeZoneId = tzSelection,
                                            labelEn = if (near) nearest!!.nameEn
                                            else Formatters.formatDecimal(lat, 3, false) + ", " +
                                                Formatters.formatDecimal(lon, 3, false),
                                            labelAr = if (near) nearest!!.nameAr else null,
                                            source = AppLocation.Source.MANUAL,
                                        ),
                                    )
                                    onDismiss()
                                }
                            },
                            enabled = valid,
                        ) { Text(stringResource(R.string.common_save)) }
                        if (!valid && latText.isNotEmpty() && lonText.isNotEmpty()) {
                            Text(
                                stringResource(R.string.invalid_coordinates),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
        },
    )

    // Composed AFTER the picker so it layers on top of it: a short "why do
    // you need this?" explanation shown before the system permission dialog.
    if (showGpsRationale) {
        AlertDialog(
            onDismissRequest = { showGpsRationale = false },
            title = { Text(stringResource(R.string.use_gps)) },
            text = { Text(stringResource(R.string.gps_permission_rationale)) },
            confirmButton = {
                TextButton(onClick = {
                    showGpsRationale = false
                    locationPermissionLauncher.launch(LOCATION_PERMISSIONS)
                }) { Text(stringResource(R.string.common_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { showGpsRationale = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}
