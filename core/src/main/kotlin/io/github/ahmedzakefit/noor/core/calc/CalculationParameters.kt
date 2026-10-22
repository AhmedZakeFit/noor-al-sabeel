package io.github.ahmedzakefit.noor.core.calc

import io.github.ahmedzakefit.noor.core.model.Prayer

/**
 * All calculation inputs, resolved from a preset method plus user overrides.
 *
 * [offsetsMinutes] lets users align computed times with their local mosque (e.g. +3 minutes to fajr).
 */
data class CalculationParameters(
    val method: CalculationMethod = CalculationMethod.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY,
    /** 1 = Standard (Shafi/Maliki/Hanbali), 2 = Hanafi. */
    val asrFactor: Int = 1,
    val customFajrAngle: Double? = null,
    val customIshaAngle: Double? = null,
    /** Overrides the method's minutes-based isha (used for the Umm al-Qura Ramadan rule). */
    val customIshaMinutesAfterMaghrib: Int? = null,
    val offsetsMinutes: Map<Prayer, Int> = emptyMap(),
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.ANGLE_BASED,
) {
    init {
        require(asrFactor in 1..2) { "asrFactor must be 1 (Standard) or 2 (Hanafi)" }
    }

    val fajrAngle: Double get() = customFajrAngle ?: method.fajrAngle
    val ishaAngle: Double? get() = customIshaAngle ?: method.ishaAngle
    val ishaMinutesAfterMaghrib: Int? get() = customIshaMinutesAfterMaghrib ?: method.ishaMinutesAfterMaghrib

    fun offsetOf(prayer: Prayer): Int = offsetsMinutes[prayer] ?: 0
}
