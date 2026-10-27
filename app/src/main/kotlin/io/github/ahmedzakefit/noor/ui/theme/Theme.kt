package io.github.ahmedzakefit.noor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.ahmedzakefit.noor.data.ThemeSetting

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F5A36),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC7F3D6),
    onPrimaryContainer = Color(0xFF002110),
    secondary = Color(0xFF3B664F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD6ECE0),
    onSecondaryContainer = Color(0xFF0D2117),
    tertiary = Color(0xFFB8860B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFF1C6),
    onTertiaryContainer = Color(0xFF2B1C00),
    background = Color(0xFFF3FAF5),
    onBackground = Color(0xFF131D17),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF131D17),
    surfaceVariant = Color(0xFFE2EFE7),
    onSurfaceVariant = Color(0xFF3F4B43),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF6F7B73),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5BD892),
    onPrimary = Color(0xFF003920),
    primaryContainer = Color(0xFF08482A),
    onPrimaryContainer = Color(0xFF98F5BA),
    secondary = Color(0xFFA2D3B4),
    onSecondary = Color(0xFF1A3728),
    secondaryContainer = Color(0xFF2C4C3B),
    onSecondaryContainer = Color(0xFFCDF0DA),
    tertiary = Color(0xFFE5C158),
    onTertiary = Color(0xFF3E2E00),
    tertiaryContainer = Color(0xFF5A4400),
    onTertiaryContainer = Color(0xFFFFE08B),
    background = Color(0xFF0C1410),
    onBackground = Color(0xFFE0E9E3),
    surface = Color(0xFF121B16),
    onSurface = Color(0xFFE0E9E3),
    surfaceVariant = Color(0xFF1C2821),
    onSurfaceVariant = Color(0xFFC0CDC4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF88958D),
)

private val NOORTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
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
        content = content,
    )
}
