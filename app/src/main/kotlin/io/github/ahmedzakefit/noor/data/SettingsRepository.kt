package io.github.ahmedzakefit.noor.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.ahmedzakefit.noor.core.calc.CalculationMethod
import io.github.ahmedzakefit.noor.core.calc.HighLatitudeRule
import io.github.ahmedzakefit.noor.core.model.Prayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "noor_settings")

/**
 * Local-only settings storage (DataStore Preferences). Nothing ever leaves the device.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val LAT = doublePreferencesKey("latitude")
        val LON = doublePreferencesKey("longitude")
        val TZ = stringPreferencesKey("timezone")
        val LABEL_EN = stringPreferencesKey("label_en")
        val LABEL_AR = stringPreferencesKey("label_ar")
        val SOURCE = stringPreferencesKey("location_source")
        val METHOD = stringPreferencesKey("method")
        val ASR_FACTOR = intPreferencesKey("asr_factor")
        val FAJR_ANGLE = doublePreferencesKey("custom_fajr_angle")
        val ISHA_ANGLE = doublePreferencesKey("custom_isha_angle")
        val HLR = stringPreferencesKey("high_latitude_rule")
        val NOTIF_MASTER = booleanPreferencesKey("notif_master")
        val NOTIF_PRAYERS = stringSetPreferencesKey("notif_prayers")
        val REMINDER_MINUTES = intPreferencesKey("reminder_minutes")
        val ADHAN_MODE = intPreferencesKey("adhan_mode")
        val ADHAN_VOICE = intPreferencesKey("adhan_voice")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val SOUND_URI = stringPreferencesKey("sound_uri")
        val VIBRATE = booleanPreferencesKey("vibrate")
        val THEME = intPreferencesKey("theme")
        val LANGUAGE = intPreferencesKey("language")
        val DIGITS = intPreferencesKey("digits")
        val HIJRI_OFFSET = intPreferencesKey("hijri_offset_days")
        val NOTIF_ASKED = booleanPreferencesKey("notif_permission_asked")
        val UPDATE_POSTPONED_VERSION = stringPreferencesKey("update_postponed_version")
        fun offset(prayer: Prayer) = intPreferencesKey("offset_${prayer.key}")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map(::read)

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val next = transform(read(prefs))
            write(prefs, next)
        }
    }

    suspend fun setNotificationPermissionAsked() {
        context.dataStore.edit { it[Keys.NOTIF_ASKED] = true }
    }

    val notificationPermissionAsked: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.NOTIF_ASKED] ?: false }

    /** Version the user postponed — auto update checks stay quiet about it. */
    val updatePostponedVersion: Flow<String?> =
        context.dataStore.data.map { it[Keys.UPDATE_POSTPONED_VERSION] }

    suspend fun setUpdatePostponedVersion(version: String) {
        context.dataStore.edit { it[Keys.UPDATE_POSTPONED_VERSION] = version }
    }

    private fun read(prefs: androidx.datastore.preferences.core.Preferences): AppSettings {
        val location = if (prefs[Keys.LAT] != null && prefs[Keys.LON] != null && prefs[Keys.TZ] != null) {
            AppLocation(
                latitude = prefs[Keys.LAT]!!,
                longitude = prefs[Keys.LON]!!,
                timeZoneId = prefs[Keys.TZ]!!,
                labelEn = prefs[Keys.LABEL_EN] ?: "",
                labelAr = prefs[Keys.LABEL_AR],
                source = when (prefs[Keys.SOURCE]) {
                    "auto" -> AppLocation.Source.AUTO
                    "city" -> AppLocation.Source.CITY
                    else -> AppLocation.Source.MANUAL
                },
            )
        } else {
            null
        }
        val offsets = Prayer.entries.associateWith { prefs[Keys.offset(it)] ?: 0 }
        val method = prefs[Keys.METHOD]?.let { name ->
            CalculationMethod.entries.firstOrNull { it.name == name }
        } ?: CalculationMethod.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY
        val hlr = prefs[Keys.HLR]?.let { name ->
            HighLatitudeRule.entries.firstOrNull { it.name == name }
        } ?: HighLatitudeRule.ANGLE_BASED
        return AppSettings(
            location = location,
            method = method,
            asrFactor = prefs[Keys.ASR_FACTOR] ?: 1,
            customFajrAngle = prefs[Keys.FAJR_ANGLE],
            customIshaAngle = prefs[Keys.ISHA_ANGLE],
            offsetsMinutes = offsets,
            highLatitudeRule = hlr,
            notificationsMaster = prefs[Keys.NOTIF_MASTER] ?: true,
            notificationsPerPrayer = prefs[Keys.NOTIF_PRAYERS]
                ?.mapNotNull { Prayer.fromKey(it) }?.toSet()
                ?: setOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA),
            reminderMinutes = prefs[Keys.REMINDER_MINUTES] ?: 10,
            adhanMode = io.github.ahmedzakefit.noor.data.AdhanMode.entries
                .getOrElse(prefs[Keys.ADHAN_MODE] ?: 0) { io.github.ahmedzakefit.noor.data.AdhanMode.REGULAR },
            adhanVoice = io.github.ahmedzakefit.noor.data.AdhanVoice.entries
                .getOrElse(prefs[Keys.ADHAN_VOICE] ?: 0) { io.github.ahmedzakefit.noor.data.AdhanVoice.MAKKAH },
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            soundUri = prefs[Keys.SOUND_URI],
            vibrate = prefs[Keys.VIBRATE] ?: true,
            theme = ThemeSetting.entries.getOrElse(prefs[Keys.THEME] ?: 0) { ThemeSetting.SYSTEM },
            language = LanguageSetting.entries.getOrElse(prefs[Keys.LANGUAGE] ?: 0) { LanguageSetting.SYSTEM },
            digits = DigitSetting.entries.getOrElse(prefs[Keys.DIGITS] ?: 0) { DigitSetting.FOLLOW_LOCALE },
            hijriOffsetDays = prefs[Keys.HIJRI_OFFSET] ?: 0,
        )
    }

    private fun write(
        prefs: androidx.datastore.preferences.core.MutablePreferences,
        s: AppSettings,
    ) {
        val loc = s.location
        if (loc != null) {
            prefs[Keys.LAT] = loc.latitude
            prefs[Keys.LON] = loc.longitude
            prefs[Keys.TZ] = loc.timeZoneId
            prefs[Keys.LABEL_EN] = loc.labelEn
            if (loc.labelAr != null) prefs[Keys.LABEL_AR] = loc.labelAr else prefs.remove(Keys.LABEL_AR)
            prefs[Keys.SOURCE] = when (loc.source) {
                AppLocation.Source.AUTO -> "auto"
                AppLocation.Source.CITY -> "city"
                AppLocation.Source.MANUAL -> "manual"
            }
        } else {
            prefs.remove(Keys.LAT)
            prefs.remove(Keys.LON)
            prefs.remove(Keys.TZ)
            prefs.remove(Keys.LABEL_EN)
            prefs.remove(Keys.LABEL_AR)
            prefs.remove(Keys.SOURCE)
        }
        prefs[Keys.METHOD] = s.method.name
        prefs[Keys.ASR_FACTOR] = s.asrFactor
        if (s.customFajrAngle != null) prefs[Keys.FAJR_ANGLE] = s.customFajrAngle else prefs.remove(Keys.FAJR_ANGLE)
        if (s.customIshaAngle != null) prefs[Keys.ISHA_ANGLE] = s.customIshaAngle else prefs.remove(Keys.ISHA_ANGLE)
        for (prayer in Prayer.entries) {
            val off = s.offsetsMinutes[prayer] ?: 0
            if (off != 0) prefs[Keys.offset(prayer)] = off else prefs.remove(Keys.offset(prayer))
        }
        prefs[Keys.HLR] = s.highLatitudeRule.name
        prefs[Keys.NOTIF_MASTER] = s.notificationsMaster
        prefs[Keys.NOTIF_PRAYERS] = s.notificationsPerPrayer.map { it.key }.toSet()
        prefs[Keys.REMINDER_MINUTES] = s.reminderMinutes
        prefs[Keys.ADHAN_MODE] = s.adhanMode.ordinal
        prefs[Keys.ADHAN_VOICE] = s.adhanVoice.ordinal
        prefs[Keys.SOUND_ENABLED] = s.soundEnabled
        if (s.soundUri != null) prefs[Keys.SOUND_URI] = s.soundUri else prefs.remove(Keys.SOUND_URI)
        prefs[Keys.VIBRATE] = s.vibrate
        prefs[Keys.THEME] = s.theme.ordinal
        prefs[Keys.LANGUAGE] = s.language.ordinal
        prefs[Keys.DIGITS] = s.digits.ordinal
        prefs[Keys.HIJRI_OFFSET] = s.hijriOffsetDays
    }
}
