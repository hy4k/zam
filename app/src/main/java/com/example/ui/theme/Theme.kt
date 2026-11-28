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
    primary = ClayCoral,
    onPrimary = Color.White,
    primaryContainer = ClayPeach,
    onPrimaryContainer = ClayDeepTealInk,
    secondary = ClayAqua,
    onSecondary = Color.White,
    secondaryContainer = ClayVanilla,
    onSecondaryContainer = ClayDeepTealInk,
    tertiary = ClayHoney,
    onTertiary = Color.White,
    tertiaryContainer = ClayPeachLight,
    onTertiaryContainer = ClayCocoaInk,
    error = ClayCoralDeep,
    onError = Color.White,
    background = ClayTealWallMid,
    onBackground = ClayTextCream,
    surface = ClayPeach,
    onSurface = ClayDeepTealInk,
    surfaceVariant = ClayVanilla,
    onSurfaceVariant = ClayMutedInk,
    outline = ClayPeachDeep
)

private val KinNestDarkColorScheme = darkColorScheme(
    primary = ClayCoralLight,
    onPrimary = Color(0xFF42120E),
    primaryContainer = Color(0xFF6B2823),
    onPrimaryContainer = ClayPeachLight,
    secondary = ClayAquaLight,
    onSecondary = Color(0xFF0A3234),
    secondaryContainer = Color(0xFF1F585C),
    onSecondaryContainer = ClayVanillaLight,
    tertiary = ClayHoneyLight,
    onTertiary = Color(0xFF3E2204),
    tertiaryContainer = Color(0xFF693E0A),
    onTertiaryContainer = ClayVanillaLight,
    error = ClayCoralLight,
    onError = Color(0xFF42120E),
    background = DarkBackgroundTop,
    onBackground = DarkOnSurface,
    surface = DarkSurfaceClay,
    onSurface = DarkOnSurface,
    surfaceVariant = Color(0xFF2B6469),
    onSurfaceVariant = DarkOnSurfaceMuted,
    outline = Color(0xFF569599)
)

val KinNestShapes = Shapes(
    small = RoundedCornerShape(20.dp),
    medium = RoundedCornerShape(28.dp),
    large = RoundedCornerShape(34.dp),
    extraLarge = RoundedCornerShape(40.dp)
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
