package io.github.ahmedzakefit.noor.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Header(state, locale, arabicDigits, onOpenLocationPicker)
        Spacer(Modifier.height(14.dp))

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
            Spacer(Modifier.height(16.dp))
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
private fun Header(
    state: HomeViewModel.UiState,
    locale: Locale,
    arabicDigits: Boolean,
    onOpenLocationPicker: () -> Unit,
) {
    val context = LocalContext.current
    val location = state.settings.location

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenLocationPicker() }
                        .padding(vertical = 4.dp, horizontal = 6.dp)
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = stringResource(R.string.current_location),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = location?.displayLabel(locale.toLanguageTag())
                            ?: stringResource(R.string.location_not_set),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                AssistChip(
                    onClick = onOpenLocationPicker,
                    label = {
                        Text(
                            CalculationMethodLabels.label(context, state.settings.method),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    shape = RoundedCornerShape(12.dp),
                )
            }

            Spacer(Modifier.height(8.dp))

            val todayAtLocation = remember(location, state.now) {
                state.now?.toLocalDate() ?: LocalDate.now()
            }
            val hijri = remember(todayAtLocation, state.settings.hijriOffsetDays) {
                UmmalquraCalendar.fromGregorian(todayAtLocation.plusDays(state.settings.hijriOffsetDays.toLong()))
            }
            val months = stringArrayResource(R.array.hijri_months)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = Formatters.formatHijri(
                        hijri.year, hijri.month, hijri.day, months.toList(),
                        stringResource(R.string.hijri_ah), arabicDigits,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = Formatters.formatGregorian(todayAtLocation, locale, arabicDigits),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NoLocationCard(onOpenLocationPicker: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Filled.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.location_not_set),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onOpenLocationPicker,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(R.string.set_location_now), fontWeight = FontWeight.Bold)
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        stringResource(R.string.next_prayer) + if (state.nextIsTomorrow) {
                            " • " + stringResource(R.string.tomorrow)
                        } else "",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }

                state.remainingMinutes?.let { minutes ->
                    val pattern = stringResource(R.string.remaining_time)
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            Formatters.formatCountdown(minutes, { h, m -> String.format(pattern, h, m) }, arabicDigits),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    prayerLabel(prayer),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )

                Text(
                    Formatters.formatClock(time, use24h, locale, arabicDigits),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
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
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(vertical = 6.dp)) {
            val entries = times.entries
            entries.forEachIndexed { index, (prayer, time) ->
                val isNext = prayer == state.nextPrayer && !state.nextIsTomorrow
                TimeRow(
                    prayer = prayer,
                    time = time,
                    isNext = isNext,
                    locale = locale,
                    use24h = use24h,
                    arabicDigits = arabicDigits,
                )
                if (index < entries.size - 1) {
                    Divider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            if (times.adjustedForHighLatitude) {
                Text(
                    stringResource(R.string.high_lat_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }

            TextButton(
                onClick = onToggleExtras,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
            ) {
                Text(
                    if (showExtras) stringResource(R.string.show_less) else stringResource(R.string.show_more),
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (showExtras) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }

            if (showExtras) {
                Divider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
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
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        Color.Transparent
    }

    Row(
        Modifier
            .fillMaxWidth()
            .background(containerColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isNext) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(10.dp),
                ) {}
                Spacer(Modifier.width(10.dp))
            } else {
                Spacer(Modifier.width(20.dp))
            }
            Text(
                prayerLabel(prayer),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            Formatters.formatClock(time, use24h, locale, arabicDigits),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
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
    val context = LocalContext.current
    Card(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.notif_perm_banner_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= 33) {
                        val granted = ContextCompat.checkSelfPermission(
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.enable_notifications_now), fontWeight = FontWeight.Bold)
            }
        }
    }
}
