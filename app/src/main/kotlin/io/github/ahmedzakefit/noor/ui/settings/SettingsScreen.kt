package io.github.ahmedzakefit.noor.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ahmedzakefit.noor.R
import io.github.ahmedzakefit.noor.core.calc.CalculationMethod
import io.github.ahmedzakefit.noor.core.calc.HighLatitudeRule
import io.github.ahmedzakefit.noor.core.model.Prayer
import io.github.ahmedzakefit.noor.data.AppSettings
import io.github.ahmedzakefit.noor.data.AdhanMode
import io.github.ahmedzakefit.noor.data.CalculationMethodLabels
import io.github.ahmedzakefit.noor.data.DigitSetting
import io.github.ahmedzakefit.noor.data.LanguageSetting
import io.github.ahmedzakefit.noor.data.ThemeSetting
import io.github.ahmedzakefit.noor.ui.common.Formatters
import io.github.ahmedzakefit.noor.ui.home.prayerLabel
import java.util.Locale

@Composable
fun SettingsScreen(onFirstLocationSet: (() -> Unit)? = null) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val settings by vm.settings.collectAsStateWithLifecycle()
    var showLocationPicker by remember { mutableStateOf(false) }
    var showMethodPicker by remember { mutableStateOf(false) }
    var showOffsets by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        LocationSection(settings, onChange = { showLocationPicker = true })
        CalculationSection(settings, vm, onPickMethod = { showMethodPicker = true }, onOffsets = { showOffsets = true })
        NotificationsSection(settings, vm)
        AppearanceSection(settings, vm)
        CalendarSection(settings, vm)
        AboutSection()
    }

    if (showLocationPicker) {
        LocationPickerDialog(
            onDismiss = { showLocationPicker = false },
            onLocationSelected = { location ->
                val firstSetup = settings.location == null
                vm.setLocation(location)
                // First-run flow: once the very first location is saved, leave
                // setup behind and land straight on the prayer-times screen —
                // no need to close and reopen the app.
                if (firstSetup) onFirstLocationSet?.invoke()
            },
        )
    }
    if (showMethodPicker) {
        MethodPickerDialog(
            current = settings.method,
            onDismiss = { showMethodPicker = false },
            onSelect = { method ->
                vm.update { it.copy(method = method, customFajrAngle = null, customIshaAngle = null) }
                showMethodPicker = false
            },
        )
    }
    if (showOffsets) {
        OffsetsDialog(
            settings = settings,
            onDismiss = { showOffsets = false },
            onChange = { prayer, value ->
                vm.update { s ->
                    s.copy(offsetsMinutes = s.offsetsMinutes + (prayer to value))
                }
            },
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun LocationSection(settings: AppSettings, onChange: () -> Unit) {
    SectionCard(stringResource(R.string.section_location)) {
        val locale = Locale.getDefault()
        ListItem(
            headlineContent = {
                Text(
                    settings.location?.displayLabel(locale.toLanguageTag())
                        ?: stringResource(R.string.location_not_set),
                )
            },
            supportingContent = {
                settings.location?.let {
                    val digits = Formatters.useArabicDigits(settings.digits, locale)
                    Text(
                        Formatters.formatDecimal(it.latitude, 4, digits) + ", " +
                            Formatters.formatDecimal(it.longitude, 4, digits) + " • " + it.timeZoneId,
                    )
                }
            },
            leadingContent = {
                androidx.compose.material3.Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            trailingContent = { TextButton(onClick = onChange) { Text(stringResource(R.string.change_location)) } },
        )
    }
}

@Composable
private fun CalculationSection(
    settings: AppSettings,
    vm: SettingsViewModel,
    onPickMethod: () -> Unit,
    onOffsets: () -> Unit,
) {
    val context = LocalContext.current
    SectionCard(stringResource(R.string.section_calculation)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.calculation_method)) },
            supportingContent = { Text(CalculationMethodLabels.label(context, settings.method)) },
            modifier = Modifier.fillMaxWidth(),
            trailingContent = { TextButton(onClick = onPickMethod) { Text(stringResource(R.string.change_location)) } },
        )
        HorizontalDivider()
        RadioRow(
            title = stringResource(R.string.asr_method),
            options = listOf(
                stringResource(R.string.asr_standard) to 1,
                stringResource(R.string.asr_hanafi) to 2,
            ),
            selected = settings.asrFactor,
            onSelect = { factor -> vm.update { it.copy(asrFactor = factor) } },
        )
        if (settings.method == CalculationMethod.CUSTOM) {
            HorizontalDivider()
            AngleSlider(
                label = stringResource(R.string.fajr_angle),
                value = settings.customFajrAngle ?: 18.0,
                onChange = { v -> vm.update { it.copy(customFajrAngle = v) } },
            )
            AngleSlider(
                label = stringResource(R.string.isha_angle),
                value = settings.customIshaAngle ?: 17.0,
                onChange = { v -> vm.update { it.copy(customIshaAngle = v) } },
            )
        }
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.prayer_offsets)) },
            supportingContent = { Text(stringResource(R.string.offsets_explanation)) },
            trailingContent = { TextButton(onClick = onOffsets) { Text(stringResource(R.string.show_more)) } },
        )
        HorizontalDivider()
        Text(
            stringResource(R.string.high_latitude_rule),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        RadioRow(
            title = "",
            options = listOf(
                stringResource(R.string.hlr_angle) to HighLatitudeRule.ANGLE_BASED,
                stringResource(R.string.hlr_middle) to HighLatitudeRule.MIDDLE_OF_NIGHT,
                stringResource(R.string.hlr_seventh) to HighLatitudeRule.SEVENTH_OF_NIGHT,
            ),
            selected = settings.highLatitudeRule,
            onSelect = { rule -> vm.update { it.copy(highLatitudeRule = rule) } },
        )
    }
}

@Composable
private fun AngleSlider(label: String, value: Double, onChange: (Double) -> Unit) {
    val locale = Locale.getDefault()
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = value.toFloat(),
                onValueChange = { onChange((it * 2).toInt() / 2.0) },
                valueRange = 10f..21f,
                steps = 21,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.angle_degrees, Formatters.formatDegrees(value, Formatters.useArabicDigits(DigitSetting.FOLLOW_LOCALE, locale))))
        }
    }
}

@Composable
private fun NotificationsSection(settings: AppSettings, vm: SettingsViewModel) {
    SectionCard(stringResource(R.string.section_notifications)) {
        ToggleRow(stringResource(R.string.notifications_master), settings.notificationsMaster) { on ->
            vm.update { it.copy(notificationsMaster = on) }
        }
        if (settings.notificationsMaster) {
            HorizontalDivider()
            Text(
                stringResource(R.string.adhan_style),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            RadioRow(
                title = "",
                options = listOf(
                    stringResource(R.string.adhan_style_regular) to AdhanMode.REGULAR,
                    stringResource(R.string.adhan_style_adhan) to AdhanMode.ADHAN,
                ),
                selected = settings.adhanMode,
                onSelect = { mode -> vm.update { it.copy(adhanMode = mode) } },
            )
            if (settings.adhanMode == AdhanMode.ADHAN) {
                Text(
                    stringResource(R.string.adhan_fajr_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.adhan_volume_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Toggle: while the adhan (test or real) is reciting, the button
                // becomes an immediate stop — always visible here, even when the
                // notification (and its stop action) is hidden by a missing
                // POST_NOTIFICATIONS grant on Android 13+.
                val adhanPlaying by vm.adhanPlaying.collectAsStateWithLifecycle()
                if (adhanPlaying) {
                    TextButton(onClick = { vm.stopAdhan() }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.notif_adhan_stop))
                    }
                } else {
                    TextButton(onClick = { vm.testAdhan() }) {
                        Text(stringResource(R.string.test_adhan))
                    }
                }
            }
            HorizontalDivider()
            Text(stringResource(R.string.per_prayer_toggles), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            for (prayer in listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)) {
                ToggleRow(
                    prayerLabel(prayer),
                    settings.notificationsPerPrayer.contains(prayer),
                ) { on ->
                    vm.update { s ->
                        val next = if (on) {
                            s.notificationsPerPrayer + prayer
                        } else {
                            s.notificationsPerPrayer - prayer
                        }
                        s.copy(notificationsPerPrayer = next)
                    }
                }
            }
            HorizontalDivider()
            ReminderRow(settings.reminderMinutes) { minutes ->
                vm.update { it.copy(reminderMinutes = minutes) }
            }
            HorizontalDivider()
            ToggleRow(stringResource(R.string.vibrate), settings.vibrate) { on ->
                vm.update { it.copy(vibrate = on) }
            }
            HorizontalDivider()
            TextButton(onClick = { vm.sendTestNotification() }) {
                Text(stringResource(R.string.test_notification))
            }
            ExactAlarmStatus(vm)
            BatteryReliability()
        }
    }
}

@Composable
private fun ExactAlarmStatus(vm: SettingsViewModel) {
    val context = LocalContext.current
    var canExact by remember { mutableStateOf(true) }
    androidx.compose.runtime.LaunchedEffect(Unit) { canExact = vm.canScheduleExactAlarms() }
    if (Build.VERSION.SDK_INT >= 31 && !canExact) {
        Column {
            Text(
                stringResource(R.string.exact_alarm_blocked),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
            TextButton(onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            .setData(Uri.parse("package:" + context.packageName)),
                    )
                }
            }) { Text(stringResource(R.string.allow_exact_alarms)) }
        }
    } else {
        Text(
            stringResource(R.string.exact_alarm_ok),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BatteryReliability() {
    val context = LocalContext.current
    val pm = context.getSystemService(PowerManager::class.java)
    val ignoring = pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    if (!ignoring) {
        Column(Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.battery_reliability_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.battery_reliability_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            .setData(Uri.parse("package:" + context.packageName)),
                    )
                }
            }) { Text(stringResource(R.string.disable_battery_optimization)) }
        }
    }
}

@Composable
private fun AppearanceSection(settings: AppSettings, vm: SettingsViewModel) {
    SectionCard(stringResource(R.string.section_appearance)) {
        Text(stringResource(R.string.theme_label), style = MaterialTheme.typography.titleMedium)
        RadioRow(
            title = "",
            options = listOf(
                stringResource(R.string.theme_system) to ThemeSetting.SYSTEM,
                stringResource(R.string.theme_light) to ThemeSetting.LIGHT,
                stringResource(R.string.theme_dark) to ThemeSetting.DARK,
            ),
            selected = settings.theme,
            onSelect = { theme -> vm.update { it.copy(theme = theme) } },
        )
        HorizontalDivider()
        Text(stringResource(R.string.language_label), style = MaterialTheme.typography.titleMedium)
        RadioRow(
            title = "",
            options = listOf(
                stringResource(R.string.lang_system) to LanguageSetting.SYSTEM,
                stringResource(R.string.lang_ar) to LanguageSetting.ARABIC,
                stringResource(R.string.lang_en) to LanguageSetting.ENGLISH,
            ),
            selected = settings.language,
            onSelect = { lang -> vm.setLanguage(lang) },
        )
        HorizontalDivider()
        Text(stringResource(R.string.digits_label), style = MaterialTheme.typography.titleMedium)
        RadioRow(
            title = "",
            options = listOf(
                stringResource(R.string.digits_follow) to DigitSetting.FOLLOW_LOCALE,
                stringResource(R.string.digits_arabic) to DigitSetting.ARABIC_INDIC,
                stringResource(R.string.digits_latin) to DigitSetting.LATIN,
            ),
            selected = settings.digits,
            onSelect = { digits -> vm.update { it.copy(digits = digits) } },
        )
    }
}

@Composable
private fun CalendarSection(settings: AppSettings, vm: SettingsViewModel) {
    SectionCard(stringResource(R.string.section_calendar)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.hijri_offset_label), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    if (settings.hijriOffsetDays > -2) {
                        vm.update { it.copy(hijriOffsetDays = it.hijriOffsetDays - 1) }
                    }
                }) { Text("−", style = MaterialTheme.typography.titleLarge) }
                Text(
                    stringResource(R.string.hijri_offset_value, settings.hijriOffsetDays),
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = {
                    if (settings.hijriOffsetDays < 2) {
                        vm.update { it.copy(hijriOffsetDays = it.hijriOffsetDays + 1) }
                    }
                }) { Text("+", style = MaterialTheme.typography.titleLarge) }
            }
        }
        Text(
            stringResource(R.string.hijri_offset_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    SectionCard(stringResource(R.string.section_about)) {
        val version = remember {
            runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull() ?: "?"
        }
        Text(stringResource(R.string.about_version, version), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.about_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/AhmedZakeFit/noor")),
                )
            }
        }) { Text(stringResource(R.string.about_github)) }
        TextButton(onClick = {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/AhmedZakeFit/noor/blob/main/PRIVACY.md")),
                )
            }
        }) { Text(stringResource(R.string.about_privacy)) }
        TextButton(onClick = {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/AhmedZakeFit/noor/blob/main/docs/PRAYER_TIMES.md")),
                )
            }
        }) { Text(stringResource(R.string.about_accuracy)) }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun <T> RadioRow(
    title: String,
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column {
        if (title.isNotEmpty()) {
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        options.forEach { (label, value) ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = value == selected, onClick = { onSelect(value) })
                Text(label, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun ReminderRow(current: Int, onChange: (Int) -> Unit) {
    val options = listOf(0, 5, 10, 15, 20, 30)
    var expanded by remember { mutableStateOf(false) }
    val label = if (current == 0) stringResource(R.string.reminder_none)
    else stringResource(R.string.reminder_minutes, current)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.pre_prayer_reminder), style = MaterialTheme.typography.bodyLarge)
        Column {
            TextButton(onClick = { expanded = true }) { Text(label) }
            androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { minutes ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = {
                            Text(
                                if (minutes == 0) stringResource(R.string.reminder_none)
                                else stringResource(R.string.reminder_minutes, minutes),
                            )
                        },
                        onClick = {
                            onChange(minutes)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MethodPickerDialog(
    current: CalculationMethod,
    onDismiss: () -> Unit,
    onSelect: (CalculationMethod) -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.calculation_method)) },
        text = {
            Column {
                CalculationMethod.entries.forEach { method ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = method == current, onClick = { onSelect(method) })
                        Text(CalculationMethodLabels.label(context, method))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun OffsetsDialog(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onChange: (Prayer, Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.prayer_offsets)) },
        text = {
            Column {
                Text(stringResource(R.string.offsets_explanation), style = MaterialTheme.typography.bodySmall)
                for (prayer in Prayer.entries) {
                    val value = settings.offsetsMinutes[prayer] ?: 0
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(prayerLabel(prayer), style = MaterialTheme.typography.bodyLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { if (value > -30) onChange(prayer, value - 1) }) {
                                Text("−", style = MaterialTheme.typography.titleLarge)
                            }
                            Text(value.toString(), fontWeight = FontWeight.Bold)
                            TextButton(onClick = { if (value < 30) onChange(prayer, value + 1) }) {
                                Text("+", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
        },
    )
}
