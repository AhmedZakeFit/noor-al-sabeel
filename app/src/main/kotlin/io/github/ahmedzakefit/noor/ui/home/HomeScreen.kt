package io.github.ahmedzakefit.noor.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ahmedzakefit.noor.R
import io.github.ahmedzakefit.noor.core.hijri.UmmalquraCalendar
import io.github.ahmedzakefit.noor.core.model.Prayer
import io.github.ahmedzakefit.noor.data.AppSettings
import io.github.ahmedzakefit.noor.data.CalculationMethodLabels
import io.github.ahmedzakefit.noor.ui.common.Formatters
import io.github.ahmedzakefit.noor.ui.update.UpdateBanner
import io.github.ahmedzakefit.noor.ui.update.UpdateViewModel
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenLocationPicker: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
) {
    val context = LocalContext.current
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val use24h = remember { DateFormat.is24HourFormat(context) }
    val arabicDigits = Formatters.useArabicDigits(state.settings.digits, locale)
    var showExtras by rememberSaveable { mutableStateOf(false) }

    // One-time notification permission request (Android 13+), asked only
    // after setup completes (location set + alerts on) and at most once per
    // install. If denied, the system dialog is never spammed again; granting
    // later from system settings simply makes future notifications appear —
    // posting always re-checks the live grant, never an assumption.
    LaunchedEffect(
        state.settings.notificationsMaster,
        state.settings.location,
        state.notifPermAsked,
    ) {
        if (Build.VERSION.SDK_INT >= 33 && state.settings.notificationsMaster &&
            state.settings.location != null && !state.notifPermAsked
        ) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                vm.markNotificationPermissionAsked()
                onRequestNotificationPermission()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Header(state, locale, arabicDigits)
        Spacer(Modifier.height(12.dp))

        // Quiet update banner after the automatic check on app open
        // (postponed versions never re-appear).
        val updateVm: UpdateViewModel = viewModel(factory = UpdateViewModel.Factory)
        UpdateBanner(updateVm)
        val notifsEnabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (state.settings.notificationsMaster && !notifsEnabled) {
            NotificationPermissionBanner(onRequestNotificationPermission)
        }

        val location = state.settings.location
        if (location == null) {
            NoLocationCard(onOpenLocationPicker)
        } else {
            NextPrayerCard(
                state = state,
                locale = locale,
                use24h = use24h,
                arabicDigits = arabicDigits,
            )
            Spacer(Modifier.height(12.dp))
            TimesCard(
                state = state,
                locale = locale,
                use24h = use24h,
                arabicDigits = arabicDigits,
                showExtras = showExtras,
                onToggleExtras = { showExtras = !showExtras },
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Header(state: HomeViewModel.UiState, locale: Locale, arabicDigits: Boolean) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth()) {
        val location = state.settings.location
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = stringResource(R.string.current_location),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = location?.displayLabel(locale.toLanguageTag())
                    ?: stringResource(R.string.location_not_set),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        val zoneId = location?.timeZoneId ?: Locale.getDefault().toString()
        val todayAtLocation = remember(location, state.now) {
            state.now?.toLocalDate() ?: LocalDate.now()
        }
        val hijri = remember(todayAtLocation, state.settings.hijriOffsetDays) {
            UmmalquraCalendar.fromGregorian(todayAtLocation.plusDays(state.settings.hijriOffsetDays.toLong()))
        }
        val months = stringArrayResource(R.array.hijri_months)
        Text(
            text = Formatters.formatHijri(
                hijri.year, hijri.month, hijri.day, months.toList(),
                stringResource(R.string.hijri_ah), arabicDigits,
            ) + (if (hijri.isApproximate) "" else ""),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = Formatters.formatGregorian(todayAtLocation, locale, arabicDigits),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        AssistChip(
            onClick = {},
            label = { Text(CalculationMethodLabels.label(context, state.settings.method)) },
            leadingIcon = null,
        )
    }
}

@Composable
private fun NoLocationCard(onOpenLocationPicker: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.location_not_set),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onOpenLocationPicker) {
                Text(stringResource(R.string.set_location_now))
            }
        }
    }
}

@Composable
private fun NextPrayerCard(
    state: HomeViewModel.UiState,
    locale: Locale,
    use24h: Boolean,
    arabicDigits: Boolean,
) {
    val prayer = state.nextPrayer ?: return
    val time = state.nextTime ?: return
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.next_prayer) + if (state.nextIsTomorrow) {
                    " • " + stringResource(R.string.tomorrow)
                } else {
                    ""
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    prayerLabel(prayer),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    Formatters.formatClock(time, use24h, locale, arabicDigits),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            state.remainingMinutes?.let { minutes ->
                Spacer(Modifier.height(6.dp))
                val pattern = stringResource(R.string.remaining_time)
                Text(
                    Formatters.formatCountdown(minutes, { h, m -> String.format(pattern, h, m) }, arabicDigits),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun TimesCard(
    state: HomeViewModel.UiState,
    locale: Locale,
    use24h: Boolean,
    arabicDigits: Boolean,
    showExtras: Boolean,
    onToggleExtras: () -> Unit,
) {
    val times = state.times ?: return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            for ((prayer, time) in times.entries) {
                TimeRow(
                    prayer = prayer,
                    time = time,
                    isNext = prayer == state.nextPrayer && !state.nextIsTomorrow,
                    locale = locale,
                    use24h = use24h,
                    arabicDigits = arabicDigits,
                )
            }
            if (times.adjustedForHighLatitude) {
                Text(
                    stringResource(R.string.high_lat_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            TextButton(
                onClick = onToggleExtras,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(
                    if (showExtras) stringResource(R.string.show_less) else stringResource(R.string.show_more),
                )
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
            }
            if (showExtras) {
                times.midnight?.let {
                    ExtraRow(stringResource(R.string.midnight), it, locale, use24h, arabicDigits)
                }
                times.lastThirdOfNight?.let {
                    ExtraRow(stringResource(R.string.last_third), it, locale, use24h, arabicDigits)
                }
            }
        }
    }
}

@Composable
private fun TimeRow(
    prayer: Prayer,
    time: java.time.ZonedDateTime,
    isNext: Boolean,
    locale: Locale,
    use24h: Boolean,
    arabicDigits: Boolean,
) {
    val containerColor = if (isNext) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(containerColor)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isNext) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(8.dp),
                ) {}
                Spacer(Modifier.width(8.dp))
            }
            Text(
                prayerLabel(prayer),
                style = MaterialTheme.typography.titleMedium,
                color = if (prayer == Prayer.SUNRISE) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
        Text(
            Formatters.formatClock(time, use24h, locale, arabicDigits),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
            color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ExtraRow(
    label: String,
    time: java.time.ZonedDateTime,
    locale: Locale,
    use24h: Boolean,
    arabicDigits: Boolean,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            Formatters.formatClock(time, use24h, locale, arabicDigits),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun prayerLabel(prayer: Prayer): String = when (prayer) {
    Prayer.FAJR -> stringResource(R.string.prayer_fajr)
    Prayer.SUNRISE -> stringResource(R.string.prayer_sunrise)
    Prayer.DHUHR -> stringResource(R.string.prayer_dhuhr)
    Prayer.ASR -> stringResource(R.string.prayer_asr)
    Prayer.MAGHRIB -> stringResource(R.string.prayer_maghrib)
    Prayer.ISHA -> stringResource(R.string.prayer_isha)
}

@Composable
private fun NotificationPermissionBanner(
    onRequestNotificationPermission: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.notif_perm_banner_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.notif_perm_banner_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(10.dp))
            androidx.compose.material3.Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= 33) {
                        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            onRequestNotificationPermission()
                            return@Button
                        }
                    }
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    runCatching { context.startActivity(intent) }.onFailure {
                        runCatching {
                            context.startActivity(
                                android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(android.net.Uri.parse("package:" + context.packageName))
                            )
                        }
                    }
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(stringResource(R.string.enable_notifications_now))
            }
        }
    }
}
