package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Chunky, bubbly rounded 3D display font (Baloo 2) matching "What is a feeling?"
 * paired with friendly rounded Fredoka for crisp, high-legibility labels and messages.
 */
val Baloo2FontFamily = FontFamily(
    Font(R.font.baloo_2, FontWeight.Normal),
    Font(R.font.baloo_2, FontWeight.Medium),
    Font(R.font.baloo_2, FontWeight.SemiBold),
    Font(R.font.baloo_2, FontWeight.Bold),
    Font(R.font.baloo_2, FontWeight.ExtraBold),
    Font(R.font.baloo_2, FontWeight.Black)
)

val FredokaFontFamily = FontFamily(
    Font(R.font.fredoka, FontWeight.Normal),
    Font(R.font.fredoka, FontWeight.Medium),
    Font(R.font.fredoka, FontWeight.SemiBold),
    Font(R.font.fredoka, FontWeight.Bold)
)

val OutfitFontFamily = Baloo2FontFamily
val PlusJakartaSansFontFamily = FredokaFontFamily

private val SoftClayShadow = Shadow(
    color = Color(0x45184C50),
    offset = Offset(0f, 3.5f),
    blurRadius = 4f
)

val KinNestTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Baloo2FontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.2).sp,
        shadow = SoftClayShadow
    ),
    headlineLarge = TextStyle(
        fontFamily = Baloo2FontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 29.sp,
        lineHeight = 33.sp,
        letterSpacing = (-0.2).sp,
        shadow = SoftClayShadow
    ),
    headlineMedium = TextStyle(
        fontFamily = Baloo2FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        shadow = SoftClayShadow
    ),
    headlineSmall = TextStyle(
        fontFamily = Baloo2FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        shadow = SoftClayShadow
    ),
    titleLarge = TextStyle(
        fontFamily = Baloo2FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 23.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Baloo2FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.5.sp,
        lineHeight = 21.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Baloo2FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.5.sp,
        lineHeight = 19.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.5.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.5.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Baloo2FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.5.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Baloo2FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Baloo2FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp
    )
)
