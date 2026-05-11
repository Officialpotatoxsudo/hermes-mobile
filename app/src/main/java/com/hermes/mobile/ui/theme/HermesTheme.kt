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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography
import com.hermes.mobile.core.settings.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = Color(0xFF2A2A2A),
    onSecondary = Color.White,
    background = Color(0xFFF7F7F5),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFEDEDEA),
    onSurfaceVariant = Color(0xFF4C4C4C),
    outline = Color(0xFFD4D4D0),
)

private val DarkColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = Color(0xFFDADADA),
    onSecondary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF111111),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFC7C7C7),
    outline = Color(0xFF333333),
)


private val SepiaColors = lightColorScheme(
    primary = Color(0xFF5A4634),
    onPrimary = Color(0xFFF4ECD8),
    secondary = Color(0xFF7A614A),
    onSecondary = Color(0xFFF4ECD8),
    background = Color(0xFFF4ECD8),
    onBackground = Color(0xFF433422),
    surface = Color(0xFFEAE0C8),
    onSurface = Color(0xFF433422),
    surfaceVariant = Color(0xFFDED0B6),
    onSurfaceVariant = Color(0xFF5A4634),
    outline = Color(0xFFC4B49E),
)

private val NordColors = darkColorScheme(
    primary = Color(0xFF88C0D0),
    onPrimary = Color(0xFF2E3440),
    secondary = Color(0xFF81A1C1),
    onSecondary = Color(0xFF2E3440),
    background = Color(0xFF2E3440),
    onBackground = Color(0xFFECEFF4),
    surface = Color(0xFF3B4252),
    onSurface = Color(0xFFECEFF4),
    surfaceVariant = Color(0xFF434C5E),
    onSurfaceVariant = Color(0xFFD8DEE9),
    outline = Color(0xFF4C566A),
)

private val CatppuccinColors = darkColorScheme(
    primary = Color(0xFFCBA6F7),
    onPrimary = Color(0xFF1E1E2E),
    secondary = Color(0xFFF5C2E7),
    onSecondary = Color(0xFF1E1E2E),
    background = Color(0xFF1E1E2E),
    onBackground = Color(0xFFCDD6F4),
    surface = Color(0xFF313244),
    onSurface = Color(0xFFCDD6F4),
    surfaceVariant = Color(0xFF45475A),
    onSurfaceVariant = Color(0xFFA6ADC8),
    outline = Color(0xFF585B70),
)

private val HermesTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
)

@Composable
fun HermesTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light, ThemeMode.Sepia -> false
        ThemeMode.Dark, ThemeMode.Nord, ThemeMode.Catppuccin -> true
    }
    val colors: ColorScheme = when (themeMode) {
        ThemeMode.System -> if (darkTheme) DarkColors else LightColors
        ThemeMode.Light -> LightColors
        ThemeMode.Dark -> DarkColors
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
        val backgroundBrush = when (themeMode) {
            ThemeMode.Sepia -> Brush.verticalGradient(listOf(Color(0xFFF9F5EC), Color(0xFFEBE0C8)))
            ThemeMode.Nord -> Brush.verticalGradient(listOf(Color(0xFF3B4252), Color(0xFF2E3440)))
            ThemeMode.Catppuccin -> Brush.verticalGradient(listOf(Color(0xFF313244), Color(0xFF1E1E2E)))
            ThemeMode.Light -> Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF1F1EF)))
            ThemeMode.Dark -> Brush.verticalGradient(listOf(Color(0xFF171717), Color(0xFF050505)))
            ThemeMode.System -> if (darkTheme) {
                Brush.verticalGradient(listOf(Color(0xFF171717), Color(0xFF050505)))
            } else {
                Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF1F1EF)))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush),
        ) {
            content()
        }
    }
}
