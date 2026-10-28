package io.github.ahmedzakefit.noor.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import io.github.ahmedzakefit.noor.ui.theme.BrandBrass
import io.github.ahmedzakefit.noor.ui.theme.BrandIvory
import io.github.ahmedzakefit.noor.ui.theme.BrandPatina
import io.github.ahmedzakefit.noor.ui.theme.MihrabArchShape
import io.github.ahmedzakefit.noor.ui.theme.StarMark
import io.github.ahmedzakefit.noor.ui.theme.zelligeLattice
import io.github.ahmedzakefit.noor.ui.update.UpdateBanner
import io.github.ahmedzakefit.noor.ui.update.UpdateViewModel
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenLocationPicker: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    updateVm: UpdateViewModel = viewModel(
        viewModelStoreOwner = (LocalContext.current as? androidx.lifecycle.ViewModelStoreOwner)
            ?: checkNotNull(androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner.current),
        factory = UpdateViewModel.Factory
    ),
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
            Spacer(Modifier.height(14.dp))
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
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = location?.displayLabel(locale.toLanguageTag())
                    ?: stringResource(R.string.location_not_set),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
        }
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
            ),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = Formatters.formatGregorian(todayAtLocation, locale, arabicDigits),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                shape = CircleShape,
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.location_not_set),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onOpenLocationPicker) {
                Text(stringResource(R.string.set_location_now))
            }
        }
    }
}

/**
 * The next prayer, framed by a pointed mihrab arch on deep verdigris —
 * the one surface that carries the architectural identity, with a faint
 * khatim lattice and a brass keyline. Fixed brand colours (not the
 * scheme's) so the composition reads identically in light and dark.
 */
@Composable
private fun NextPrayerCard(
    state: HomeViewModel.UiState,
    locale: Locale,
    use24h: Boolean,
    arabicDigits: Boolean,
) {
    val prayer = state.nextPrayer ?: return
    val time = state.nextTime ?: return
    val archFraction = 0.32f
    val shape = remember { MihrabArchShape(archFraction = archFraction, corner = 28.dp) }

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(BrandPatina)
            .zelligeLattice(color = Color.White, alpha = 0.055f)
            .border(1.dp, BrandBrass.copy(alpha = 0.55f), shape),
    ) {
        // Keep the headline clear of the arch's shoulders.
        val topInset = (maxWidth.value * archFraction * 0.52f)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(
                    top = topInset.dp,
                    bottom = 24.dp,
                    start = 30.dp,
                    end = 30.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.next_prayer) + if (state.nextIsTomorrow) {
                    " • " + stringResource(R.string.tomorrow)
                } else {
                    ""
                },
                style = MaterialTheme.typography.labelLarge,
                color = BrandBrass.copy(alpha = 0.92f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                prayerLabel(prayer),
                style = MaterialTheme.typography.displayMedium,
                color = BrandIvory,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                Formatters.formatClock(time, use24h, locale, arabicDigits),
                style = MaterialTheme.typography.headlineMedium,
                color = BrandBrass,
                textAlign = TextAlign.Center,
            )
            state.remainingMinutes?.let { minutes ->
                Spacer(Modifier.height(14.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.13f),
                    shape = RoundedCornerShape(50),
                ) {
                    val pattern = stringResource(R.string.remaining_time)
                    Text(
                        Formatters.formatCountdown(minutes, { h, m -> String.format(pattern, h, m) }, arabicDigits),
                        style = MaterialTheme.typography.titleMedium,
                        color = BrandIvory,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
                    )
                }
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
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(Modifier.padding(vertical = 10.dp)) {
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
                val rotation by animateFloatAsState(
                    targetValue = if (showExtras) 180f else 0f,
                    animationSpec = tween(durationMillis = 200),
                    label = "extras-chevron",
                )
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer { rotationZ = rotation },
                )
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
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isNext) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isNext) {
                StarMark(
                    starSize = 11.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(9.dp))
            }
            Text(
                prayerLabel(prayer),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Medium,
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
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
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
        shape = RoundedCornerShape(18.dp),
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
