package io.github.ahmedzakefit.noor.core.calc

/**
 * Well-known prayer-time calculation conventions.
 *
 * Angles are degrees below the horizon. When [ishaMinutesAfterMaghrib] is set, isha is a fixed
 * number of minutes after maghrib instead of an angle (e.g. Umm al-Qura: 90 minutes, 120 in Ramadan).
 */
enum class CalculationMethod(
    val fajrAngle: Double,
    val ishaAngle: Double?,
    val ishaMinutesAfterMaghrib: Int?,
) {
    MUSLIM_WORLD_LEAGUE(18.0, 17.0, null),
    EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY(19.5, 17.5, null),
    ISNA(15.0, 15.0, null),
    UMM_AL_QURA(18.5, null, 90),
    KARACHI(18.0, 18.0, null),
    GULF_REGION(19.5, null, 90),
    KUWAIT(18.0, 17.5, null),
    QATAR(18.0, null, 90),
    SINGAPORE_MUIS(20.0, 18.0, null),
    TURKEY_DIYANET(18.0, 17.0, null),
    FRANCE_UOIF(12.0, 12.0, null),
    CUSTOM(18.0, 17.0, null),
}
