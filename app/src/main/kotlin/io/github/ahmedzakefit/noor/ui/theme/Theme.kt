package io.github.ahmedzakefit.noor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ahmedzakefit.noor.R
import io.github.ahmedzakefit.noor.data.ThemeSetting

/*
 * ─────────────────────────────────────────────────────────────────────────
 *  Noor Al-Sabeel design identity — «الزليج الفيرديغريسي»
 *
 *  The palette is drawn from Islamic architecture itself, not from the
 *  usual app-store green or the generic blue/purple gradient:
 *
 *    · Verdigris teal  — the patina of aged copper domes and zellige tile
 *    · Parchment       — manuscript paper warm ivory
 *    · Brass           — mosque lamps, door fittings, inlaid calligraphy
 *    · Clay            — Mamluk brick and terracotta
 *    · Ink             — calligraphy ink, a warm deep charcoal-teal
 *
 *  Typography pairs Amiri (the classical Bulaq-press Naskh revival) for
 *  display moments with IBM Plex Sans Arabic for UI text — heritage and
 *  modernity in one voice, bundled offline so the app stays network-free.
 * ─────────────────────────────────────────────────────────────────────────
 */

/** Deep verdigris used for the fixed hero surfaces (day & night). */
val BrandPatina = Color(0xFF0E423B)

/** Polished brass for accents on dark surfaces. */
val BrandBrass = Color(0xFFE3C368)

/** Manuscript ivory for text on patina. */
val BrandIvory = Color(0xFFF6F1E4)

/** Public font stacks (Amiri for display, Plex for UI). */
val NoorAmiri = FontFamily(
    Font(R.font.noor_amiri_regular, FontWeight.Normal),
    Font(R.font.noor_amiri_bold, FontWeight.Bold),
)

val NoorSans = FontFamily(
    Font(R.font.noor_ui_regular, FontWeight.Normal),
    Font(R.font.noor_ui_medium, FontWeight.Medium),
    Font(R.font.noor_ui_semibold, FontWeight.SemiBold),
    Font(R.font.noor_ui_bold, FontWeight.Bold),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF11564E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDE9E0),
    onPrimaryContainer = Color(0xFF062A24),
    secondary = Color(0xFF77574A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF1DED3),
    onSecondaryContainer = Color(0xFF2A1711),
    tertiary = Color(0xFF8A6A24),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF0E3BC),
    onTertiaryContainer = Color(0xFF271E04),
    background = Color(0xFFFAF6EC),
    onBackground = Color(0xFF1C2624),
    surface = Color(0xFFFDFAF2),
    onSurface = Color(0xFF1C2624),
    surfaceVariant = Color(0xFFDFE5DD),
    onSurfaceVariant = Color(0xFF414A47),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F1E5),
    surfaceContainer = Color(0xFFF0EBDF),
    surfaceContainerHigh = Color(0xFFEAE5D9),
    surfaceContainerHighest = Color(0xFFE4E0D4),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF6F7974),
    outlineVariant = Color(0xFFC4CCC6),
    inverseSurface = Color(0xFF2F3835),
    inverseOnSurface = Color(0xFFF0F1EB),
    inversePrimary = Color(0xFF8CD3C4),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8CD3C4),
    onPrimary = Color(0xFF00332D),
    primaryContainer = Color(0xFF0E423B),
    onPrimaryContainer = Color(0xFFBEE9DD),
    secondary = Color(0xFFE0C0B0),
    onSecondary = Color(0xFF44291F),
    secondaryContainer = Color(0xFF5D4035),
    onSecondaryContainer = Color(0xFFF1DED3),
    tertiary = Color(0xFFD5BC75),
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF5B4718),
    onTertiaryContainer = Color(0xFFF0E3BC),
    background = Color(0xFF101917),
    onBackground = Color(0xFFDFE6E2),
    surface = Color(0xFF131D1A),
    onSurface = Color(0xFFDFE6E2),
    surfaceVariant = Color(0xFF3F4944),
    onSurfaceVariant = Color(0xFFBFC9C3),
    surfaceContainerLowest = Color(0xFF0A1311),
    surfaceContainerLow = Color(0xFF161F1D),
    surfaceContainer = Color(0xFF1A2422),
    surfaceContainerHigh = Color(0xFF252F2C),
    surfaceContainerHighest = Color(0xFF303B37),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF89938D),
    outlineVariant = Color(0xFF3F4944),
    inverseSurface = Color(0xFFDFE6E2),
    inverseOnSurface = Color(0xFF2F3835),
    inversePrimary = Color(0xFF11564E),
)

/*
 * Amiri needs generous leading (tall Naskh ascenders and vowel marks);
 * Plex carries the UI with its own tighter, screen-native rhythm.
 */
private val NOORTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = NoorAmiri,
        fontWeight = FontWeight.Bold,
        fontSize = 46.sp,
        lineHeight = 62.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = NoorAmiri,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 52.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = NoorAmiri,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 44.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = NoorAmiri,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 40.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = NoorAmiri,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 34.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = NoorSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = NoorSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = NoorSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = NoorSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = NoorSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = NoorSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = NoorSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = NoorSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = NoorSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    ),
)

/* Soft, calm radii — nothing shouts; the mihrab arch carries the drama. */
private val NOORShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun NOORTheme(
    themeSetting: ThemeSetting = ThemeSetting.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeSetting) {
        ThemeSetting.SYSTEM -> isSystemInDarkTheme()
        ThemeSetting.LIGHT -> false
        ThemeSetting.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = NOORTypography,
        shapes = NOORShapes,
        content = content,
    )
}
