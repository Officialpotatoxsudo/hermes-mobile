package com.hermes.mobile.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography
import com.hermes.mobile.core.settings.LiquidGlassConfig
import com.hermes.mobile.core.settings.ThemeMode
import com.hermes.mobile.core.settings.VisualStyle
import com.hermes.mobile.ui.components.LocalHermesLiquidGlassConfig
import com.hermes.mobile.ui.components.LocalHermesVisualStyle

private val LightColors = lightColorScheme(
    primary = Color(0xFF2D5BDB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBE1FF),
    onPrimaryContainer = Color(0xFF001849),
    secondary = Color(0xFF585E71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE2F9),
    onSecondaryContainer = Color(0xFF151B2C),
    tertiary = Color(0xFF6B5778),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3DAFF),
    onTertiaryContainer = Color(0xFF251432),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE2E1EC),
    onSurfaceVariant = Color(0xFF45464F),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF5F3FB),
    surfaceContainer = Color(0xFFEFEDF5),
    surfaceContainerHigh = Color(0xFFE9E7F0),
    surfaceContainerHighest = Color(0xFFE4E1EA),
    outline = Color(0xFF767680),
    outlineVariant = Color(0xFFC6C5D0),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF303036),
    inverseOnSurface = Color(0xFFF2F0F7),
    inversePrimary = Color(0xFFB4C5FF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4C5FF),
    onPrimary = Color(0xFF002C72),
    primaryContainer = Color(0xFF0A43A0),
    onPrimaryContainer = Color(0xFFDBE1FF),
    secondary = Color(0xFFC0C6DC),
    onSecondary = Color(0xFF2A3042),
    secondaryContainer = Color(0xFF404659),
    onSecondaryContainer = Color(0xFFDCE2F9),
    tertiary = Color(0xFFD7BDE4),
    onTertiary = Color(0xFF3B2948),
    tertiaryContainer = Color(0xFF533F5F),
    onTertiaryContainer = Color(0xFFF3DAFF),
    background = Color(0xFF121318),
    onBackground = Color(0xFFE4E1EA),
    surface = Color(0xFF121318),
    onSurface = Color(0xFFE4E1EA),
    surfaceVariant = Color(0xFF45464F),
    onSurfaceVariant = Color(0xFFC6C5D0),
    surfaceContainerLowest = Color(0xFF0D0E13),
    surfaceContainerLow = Color(0xFF1B1B21),
    surfaceContainer = Color(0xFF1F1F25),
    surfaceContainerHigh = Color(0xFF292A2F),
    surfaceContainerHighest = Color(0xFF34343A),
    outline = Color(0xFF90909A),
    outlineVariant = Color(0xFF45464F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFE4E1EA),
    inverseOnSurface = Color(0xFF303036),
    inversePrimary = Color(0xFF2D5BDB),
)

private val SepiaColors = lightColorScheme(
    primary = Color(0xFF7A5C2E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDB0),
    onPrimaryContainer = Color(0xFF2A1800),
    secondary = Color(0xFF6D5D3F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF8E0BC),
    onSecondaryContainer = Color(0xFF261A04),
    background = Color(0xFFFFF8F0),
    onBackground = Color(0xFF1F1B13),
    surface = Color(0xFFFFF8F0),
    onSurface = Color(0xFF1F1B13),
    surfaceVariant = Color(0xFFEDE0CF),
    onSurfaceVariant = Color(0xFF4E4539),
    surfaceContainer = Color(0xFFF3E8D8),
    surfaceContainerHigh = Color(0xFFEDE2D2),
    outline = Color(0xFF807567),
    outlineVariant = Color(0xFFD1C4B3),
)

private val NordColors = darkColorScheme(
    primary = Color(0xFF88C0D0),
    onPrimary = Color(0xFF1A3540),
    primaryContainer = Color(0xFF2E5460),
    onPrimaryContainer = Color(0xFFC0E8F0),
    secondary = Color(0xFF81A1C1),
    onSecondary = Color(0xFF1A2E40),
    secondaryContainer = Color(0xFF3B5166),
    onSecondaryContainer = Color(0xFFBBD4EC),
    background = Color(0xFF2E3440),
    onBackground = Color(0xFFECEFF4),
    surface = Color(0xFF2E3440),
    onSurface = Color(0xFFECEFF4),
    surfaceVariant = Color(0xFF3B4252),
    onSurfaceVariant = Color(0xFFD8DEE9),
    surfaceContainer = Color(0xFF353C49),
    surfaceContainerHigh = Color(0xFF3E4554),
    outline = Color(0xFF5C6577),
    outlineVariant = Color(0xFF4C566A),
)

private val CatppuccinColors = darkColorScheme(
    primary = Color(0xFFCBA6F7),
    onPrimary = Color(0xFF321F4A),
    primaryContainer = Color(0xFF4A3168),
    onPrimaryContainer = Color(0xFFE8D5FF),
    secondary = Color(0xFFF5C2E7),
    onSecondary = Color(0xFF3E2138),
    secondaryContainer = Color(0xFF5A3750),
    onSecondaryContainer = Color(0xFFFFDBF0),
    background = Color(0xFF1E1E2E),
    onBackground = Color(0xFFCDD6F4),
    surface = Color(0xFF1E1E2E),
    onSurface = Color(0xFFCDD6F4),
    surfaceVariant = Color(0xFF313244),
    onSurfaceVariant = Color(0xFFA6ADC8),
    surfaceContainer = Color(0xFF282839),
    surfaceContainerHigh = Color(0xFF333347),
    outline = Color(0xFF6C7086),
    outlineVariant = Color(0xFF585B70),
)

private val PureBlackColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1A1A1A),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFE0E0E0),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF262626),
    onSecondaryContainer = Color(0xFFE8E8E8),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF141414),
    onSurfaceVariant = Color(0xFFB0B0B0),
    surfaceContainer = Color(0xFF0A0A0A),
    surfaceContainerHigh = Color(0xFF141414),
    outline = Color(0xFF2A2A2A),
    outlineVariant = Color(0xFF1A1A1A),
)

private val PureWhiteColors = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0F0F0),
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFF333333),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E8E8),
    onSecondaryContainer = Color(0xFF1A1A1A),
    background = Color(0xFFFAFAFA),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF666666),
    surfaceContainer = Color(0xFFF5F5F5),
    surfaceContainerHigh = Color(0xFFEEEEEE),
    outline = Color(0xFFD5D5D5),
    outlineVariant = Color(0xFFE5E5E5),
)

// Centralized font families. Using FontFamily.Default lets manufacturer-
// customized system fonts (Samsung One UI Sans, OnePlus Sans, Pixel's Roboto Flex,
// etc.) come through, instead of forcing the generic sans-serif fallback.
// Swap these vals to a `GoogleFont` / bundled `Font(...)` family when adopting a
// custom typeface (e.g. Inter, Nunito, DM Sans).
private val HermesDisplayFontFamily: FontFamily = FontFamily.Default
private val HermesBodyFontFamily: FontFamily = FontFamily.Default

private val HermesTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = HermesDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = 0.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = HermesDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = HermesDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = HermesDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = HermesDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = HermesDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = HermesDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = HermesBodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = HermesBodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = HermesBodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = HermesBodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = HermesBodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = HermesBodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = HermesBodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = HermesBodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

@Composable
fun HermesTheme(
    themeMode: ThemeMode = ThemeMode.System,
    visualStyle: VisualStyle = VisualStyle.Normal,
    liquidGlassConfig: LiquidGlassConfig = LiquidGlassConfig(),
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light, ThemeMode.Sepia, ThemeMode.PureWhite -> false
        ThemeMode.Dark, ThemeMode.Nord, ThemeMode.Catppuccin, ThemeMode.PureBlack -> true
    }
    val colors: ColorScheme = when (themeMode) {
        ThemeMode.System -> if (darkTheme) DarkColors else LightColors
        ThemeMode.Light -> LightColors
        ThemeMode.Dark -> DarkColors
        ThemeMode.PureBlack -> PureBlackColors
        ThemeMode.PureWhite -> PureWhiteColors
        ThemeMode.Sepia -> SepiaColors
        ThemeMode.Nord -> NordColors
        ThemeMode.Catppuccin -> CatppuccinColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = HermesTypography,
    ) {
        val view = LocalView.current
        DisposableEffect(darkTheme) {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            onDispose { }
        }
        val backgroundBrush = if (visualStyle == VisualStyle.LiquidGlass) {
            liquidGlassBackgroundBrush(darkTheme)
        } else {
            when (themeMode) {
                ThemeMode.Sepia -> Brush.verticalGradient(listOf(Color(0xFFFFF8F0), Color(0xFFF5EBDA)))
                ThemeMode.Nord -> Brush.verticalGradient(listOf(Color(0xFF353C49), Color(0xFF2E3440)))
                ThemeMode.Catppuccin -> Brush.verticalGradient(listOf(Color(0xFF24243A), Color(0xFF1E1E2E)))
                ThemeMode.Light -> Brush.verticalGradient(listOf(Color(0xFFFBF8FF), Color(0xFFF0EDF8)))
                ThemeMode.Dark -> Brush.verticalGradient(listOf(Color(0xFF181820), Color(0xFF121318)))
                ThemeMode.PureBlack -> SolidColor(Color.Black)
                ThemeMode.PureWhite -> Brush.verticalGradient(listOf(Color(0xFFFCFCFA), Color(0xFFF5F5F0)))
                ThemeMode.System -> if (darkTheme) {
                    Brush.verticalGradient(listOf(Color(0xFF181820), Color(0xFF121318)))
                } else {
                    Brush.verticalGradient(listOf(Color(0xFFFBF8FF), Color(0xFFF0EDF8)))
                }
            }
        }

        CompositionLocalProvider(
            LocalHermesVisualStyle provides visualStyle,
            LocalHermesLiquidGlassConfig provides liquidGlassConfig.coerced(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundBrush),
            ) {
                content()
            }
        }
    }
}

private fun liquidGlassBackgroundBrush(darkTheme: Boolean): Brush {
    return if (darkTheme) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF10131A),
                Color(0xFF172018),
                Color(0xFF111521),
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFF9FBFF),
                Color(0xFFEFF8F3),
                Color(0xFFF8F2FF),
            ),
        )
    }
}
