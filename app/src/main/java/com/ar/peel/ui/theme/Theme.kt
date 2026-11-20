package com.ar.peel.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.ar.peel.R
import com.ar.peel.data.ThemeMode

object PeelColors {
    val Accent = Color(0xFFFFB800)
    val Ink = Color(0xFF111111)
    val LightBackground = Color(0xFFF2F2F7)
    val LightCard = Color(0xFFFFFFFF)
    val LightSecondary = Color(0xFF8E8E93)
    val DarkBackground = Color(0xFF000000)
    val DarkCard = Color(0xFF1C1C1E)
    val Danger = Color(0xFFFF3B30)
    val Guide = Color(0xFFFFB800)
}

object PeelDimens {
    val screenPadding = 20.dp
    val cardRadius = 24.dp
    val controlRadius = 18.dp
    val rowHeight = 52.dp
    val touchTarget = 48.dp
}

val Nippo = FontFamily(
    Font(R.font.nippo_regular, FontWeight.Normal),
    Font(R.font.nippo_medium, FontWeight.Medium),
    Font(R.font.nippo_bold, FontWeight.Bold),
)

private val PeelTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Nippo,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Nippo,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Nippo,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Nippo,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Nippo,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Nippo,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Nippo,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Nippo,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Nippo,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Nippo,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Nippo,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 17.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Nippo,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    ),
)

private val PeelShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
)

private val LightScheme = lightColorScheme(
    primary = PeelColors.Accent,
    onPrimary = PeelColors.Ink,
    secondary = PeelColors.LightSecondary,
    onSecondary = Color.White,
    background = PeelColors.LightBackground,
    onBackground = PeelColors.Ink,
    surface = PeelColors.LightCard,
    onSurface = PeelColors.Ink,
    surfaceVariant = Color(0xFFE9E9EE),
    onSurfaceVariant = Color(0xFF6C6C70),
    outline = Color(0xFFD1D1D6),
    error = PeelColors.Danger,
)

private val DarkScheme = darkColorScheme(
    primary = PeelColors.Accent,
    onPrimary = PeelColors.Ink,
    secondary = PeelColors.LightSecondary,
    onSecondary = Color.Black,
    background = PeelColors.DarkBackground,
    onBackground = Color.White,
    surface = PeelColors.DarkCard,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFAEAEB2),
    outline = Color(0xFF3A3A3C),
    error = PeelColors.Danger,
)

@Composable
fun PeelTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkScheme else LightScheme
    MaterialTheme(
        colorScheme = colors,
        typography = PeelTypography,
        shapes = PeelShapes,
    ) {
        // Bare Text() calls otherwise keep the platform font instead of Nippo.
        CompositionLocalProvider(LocalTextStyle provides PeelTypography.bodyLarge) {
            content()
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
}
