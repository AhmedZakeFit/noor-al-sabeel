package io.github.ahmedzakefit.noor.data

import io.github.ahmedzakefit.noor.core.calc.CalculationMethod
import io.github.ahmedzakefit.noor.core.calc.CalculationParameters
import io.github.ahmedzakefit.noor.core.calc.HighLatitudeRule
import io.github.ahmedzakefit.noor.core.hijri.UmmalquraCalendar
import io.github.ahmedzakefit.noor.core.model.Prayer
import java.time.LocalDate

enum class ThemeSetting { SYSTEM, LIGHT, DARK }

enum class LanguageSetting { SYSTEM, ARABIC, ENGLISH }

enum class DigitSetting { FOLLOW_LOCALE, ARABIC_INDIC, LATIN }

/** The location the user selected (auto-detected, city search, or manual coordinates). */
data class AppLocation(
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
    val labelEn: String,
    val labelAr: String?,
    val source: Source = Source.MANUAL,
) {
    enum class Source { AUTO, CITY, MANUAL }

    fun displayLabel(languageTag: String): String =
        if (languageTag.startsWith("ar") && !labelAr.isNullOrBlank()) labelAr else labelEn
}

/**
 * All persisted app settings. Defaults target Egypt (Egyptian General Authority of Survey),
 * the app's primary audience; everything is user-adjustable.
 */
data class AppSettings(
    val location: AppLocation? = null,
    val method: CalculationMethod = CalculationMethod.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY,
    val asrFactor: Int = 1,
    val customFajrAngle: Double? = null,
    val customIshaAngle: Double? = null,
    val offsetsMinutes: Map<Prayer, Int> = emptyMap(),
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.ANGLE_BASED,
    val notificationsMaster: Boolean = true,
    val notificationsPerPrayer: Set<Prayer> = setOf(
        Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA,
    ),
    /** Minutes before each prayer for the reminder notification; 0 disables it. */
    val reminderMinutes: Int = 10,
    val soundEnabled: Boolean = true,
    val soundUri: String? = null,
    val vibrate: Boolean = true,
    val theme: ThemeSetting = ThemeSetting.SYSTEM,
    val language: LanguageSetting = LanguageSetting.SYSTEM,
    val digits: DigitSetting = DigitSetting.FOLLOW_LOCALE,
    val hijriOffsetDays: Int = 0,
)

/** Builds engine parameters for a date, applying the Umm al-Qura Ramadan isha rule. */
fun AppSettings.calculationParametersFor(date: LocalDate): CalculationParameters {
    var params = CalculationParameters(
        method = method,
        asrFactor = asrFactor,
        customFajrAngle = customFajrAngle,
        customIshaAngle = customIshaAngle,
        offsetsMinutes = offsetsMinutes,
        highLatitudeRule = highLatitudeRule,
    )
    if (method == CalculationMethod.UMM_AL_QURA && UmmalquraCalendar.isRamadan(date)) {
        params = params.copy(customIshaMinutesAfterMaghrib = 120)
    }
    return params
}
