package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Immutable
data class KinNestAccessibilityConfig(
    val reducedMotion: Boolean = false,
    val quietHoursActive: Boolean = false,
    val malayalamVoiceEnabled: Boolean = true
)

val LocalKinNestAccessibility = staticCompositionLocalOf { KinNestAccessibilityConfig() }

private val KinNestLightColorScheme = lightColorScheme(
    primary = StudioTealDeep,
    onPrimary = Color.White,
    primaryContainer = StudioTealSoft,
    onPrimaryContainer = Color(0xFF073B3E),
    secondary = CoralSalmon,
    onSecondary = Color.White,
    secondaryContainer = CoralSalmonSoft,
    onSecondaryContainer = Color(0xFF6B1D17),
    tertiary = PeriwinklePop,
    onTertiary = Color.White,
    tertiaryContainer = PowderSky,
    onTertiaryContainer = Color(0xFF152754),
    error = CrimsonDecline,
    onError = Color.White,
    background = LightBackgroundTop,
    onBackground = LightOnSurface,
    surface = LightSurfaceClay,
    onSurface = LightOnSurface,
    surfaceVariant = Color(0xFFECE6DA),
    onSurfaceVariant = LightOnSurfaceMuted,
    outline = Color(0xFFD5CEBF)
)

private val KinNestDarkColorScheme = darkColorScheme(
    primary = StudioTeal,
    onPrimary = Color(0xFF042628),
    primaryContainer = StudioTealDeep,
    onPrimaryContainer = StudioTealSoft,
    secondary = CoralSalmon,
    onSecondary = Color(0xFF2D0A07),
    secondaryContainer = Color(0xFF5D221D),
    onSecondaryContainer = CoralSalmonSoft,
    tertiary = PowderSky,
    onTertiary = Color(0xFF0F223D),
    tertiaryContainer = Color(0xFF293572),
    onTertiaryContainer = PowderSky,
    error = Color(0xFFFF8A80),
    onError = Color(0xFF450A0A),
    background = DarkBackgroundTop,
    onBackground = DarkOnSurface,
    surface = DarkSurfaceClay,
    onSurface = DarkOnSurface,
    surfaceVariant = Color(0xFF1F383C),
    onSurfaceVariant = DarkOnSurfaceMuted,
    outline = Color(0xFF335257)
)

val KinNestShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun KinNestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    accessibilityConfig: KinNestAccessibilityConfig = KinNestAccessibilityConfig(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> KinNestDarkColorScheme
        else -> KinNestLightColorScheme
    }

    CompositionLocalProvider(LocalKinNestAccessibility provides accessibilityConfig) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KinNestTypography,
            shapes = KinNestShapes,
            content = content
        )
    }
}
