package io.github.ahmedzakefit.noor.core.calc

/** Adjustment strategy for locations where the sun never reaches the fajr/isha twilight angles. */
enum class HighLatitudeRule {
    /** Clamps fajr/isha to a fraction of the night proportional to the twilight angle (angle/60). */
    ANGLE_BASED,

    /** fajr = sunrise - night/2, isha = sunset + night/2. */
    MIDDLE_OF_NIGHT,

    /** fajr = sunrise - night/7, isha = sunset + night/7. */
    SEVENTH_OF_NIGHT,
}
