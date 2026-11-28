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
    primary = ClayAqua,
    onPrimary = ClayTextCream,
    primaryContainer = Color(0xFFD9F5FF),
    onPrimaryContainer = ClayDeepTealInk,
    secondary = ClayCoral,
    onSecondary = ClayTextCream,
    secondaryContainer = ClayPeach,
    onSecondaryContainer = ClayDeepTealInk,
    tertiary = VibrantViolet,
    onTertiary = ClayTextCream,
    tertiaryContainer = Color(0xFFECE5FF),
    onTertiaryContainer = ClayDeepTealInk,
    error = ClayCoralDeep,
    onError = ClayTextCream,
    background = ClayTealWallMid,
    onBackground = ClayDeepTealInk,
    surface = ClayVanillaLight,
    onSurface = ClayDeepTealInk,
    surfaceVariant = ClayPeachLight,
    onSurfaceVariant = ClayMutedInk,
    outline = ClayVanillaDeep
)

private val KinNestDarkColorScheme = darkColorScheme(
    primary = ClayAquaLight,
    onPrimary = DarkBackgroundTop,
    primaryContainer = Color(0xFF153852),
    onPrimaryContainer = DarkOnSurface,
    secondary = ClayCoralLight,
    onSecondary = DarkBackgroundTop,
    secondaryContainer = Color(0xFF4A2130),
    onSecondaryContainer = DarkOnSurface,
    tertiary = VibrantVioletTop,
    onTertiary = DarkBackgroundTop,
    tertiaryContainer = Color(0xFF33265A),
    onTertiaryContainer = DarkOnSurface,
    error = ClayCoralLight,
    onError = DarkBackgroundTop,
    background = DarkBackgroundTop,
    onBackground = DarkOnSurface,
    surface = DarkSurfaceClay,
    onSurface = DarkOnSurface,
    surfaceVariant = Color(0xFF2B3B5C),
    onSurfaceVariant = DarkOnSurfaceMuted,
    outline = Color(0xFF4E658A)
)

val KinNestShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
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
