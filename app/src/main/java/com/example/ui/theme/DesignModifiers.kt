package com.example.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Hardware-buffer-safe backdrop pass-through (avoids AHardwareBuffer allocation failures on virtual GPUs).
 */
fun Modifier.liquidGlassBlurLayer(
    radius: Float = 25f,
    reducedMotion: Boolean = false
): Modifier = this

/**
 * Crisp iOS Frosted Glass Surface (for floating bottom dock, overlays, and sleek translucent cards):
 * Bright luminous white glass frost, specular top edge rim, and airy soft drop shadow.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(28.dp),
    surfaceAlpha: Float = 0.92f,
    tintColor: Color = ClayVanillaLight,
    applyBlur: Boolean = false,
    reducedMotion: Boolean = false,
    cornerRadius: Dp = 28.dp
): Modifier {
    val topFrost = lerp(tintColor, Color.White, 0.45f).copy(alpha = (surfaceAlpha + 0.05f).coerceAtMost(0.97f))
    val bottomFrost = tintColor.copy(alpha = (surfaceAlpha - 0.04f).coerceAtLeast(0.80f))
    return this
        .shadow(
            elevation = 16.dp,
            shape = shape,
            clip = false,
            ambientColor = ClayTealShadow.copy(alpha = 0.16f),
            spotColor = ClayTealShadow.copy(alpha = 0.20f)
        )
        .clip(shape)
        .then(if (applyBlur) Modifier.liquidGlassBlurLayer(25f, reducedMotion) else Modifier)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(topFrost, bottomFrost)
            )
        )
        .drawBehind {
            if (size.width <= 0f || size.height <= 0f) return@drawBehind
            val strokePx = 1.35.dp.toPx()
            val cornerPx = cornerRadius.toPx().coerceAtMost(minOf(size.width, size.height) / 2f)
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        Color.White.copy(alpha = 0.55f),
                        ClayVanillaDeep.copy(alpha = 0.40f)
                    )
                ),
                cornerRadius = CornerRadius(cornerPx, cornerPx),
                style = Stroke(width = strokePx)
            )
        }
}

/**
 * VIBRANT 3D CLAY TOY-BLOCK MORPHISM (100% Pure Compose GPU brushes):
 * Matches the plush, saturated 3D rounded tiles in the attached reference images:
 *  - Colored ambient glow shadow tinted naturally by `surfaceColor`
 *  - Pure, rich 3D extruded bottom bevel lip (never muddied with brown!)
 *  - Silky sunlit vertical gradient face with crisp glossy white specular top rim
 */
fun Modifier.claymorphic(
    cornerRadius: Dp = 26.dp,
    surfaceColor: Color = ClayPeach,
    elevation: Dp = 12.dp,
    innerShadowAlpha: Float = 0.14f,
    innerOffset: Dp = 2.dp,
    shadowTint: Color = ClayTealShadow,
    ledgeDepth: Dp = 4.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val isBrightLight = surfaceColor.luminance() > 0.72f
    val refinedLedge = (ledgeDepth * 0.85f).coerceIn(2.dp, 6.dp)
    val topLit = lerp(surfaceColor, Color.White, if (isBrightLight) 0.35f else 0.24f)
    val midMatte = surfaceColor
    val bottomCurve = lerp(surfaceColor, Color(0xFF0E203C), if (isBrightLight) 0.05f else 0.08f)
    val deepBevel = lerp(surfaceColor, Color(0xFF0B1C36), if (isBrightLight) 0.12f else 0.26f)
    val glowShadow = if (isBrightLight) shadowTint else lerp(surfaceColor, shadowTint, 0.35f)

    return this
        .shadow(
            elevation = (elevation * 0.9f).coerceAtLeast(4.dp),
            shape = shape,
            clip = false,
            ambientColor = glowShadow.copy(alpha = if (isBrightLight) 0.18f else 0.34f),
            spotColor = glowShadow.copy(alpha = if (isBrightLight) 0.22f else 0.38f)
        )
        .clip(shape)
        .background(deepBevel)
        .drawBehind {
            if (size.width <= 2f || size.height <= 2f) return@drawBehind
            val maxR = minOf(size.width, size.height) / 2f
            val cr = cornerRadius.toPx().coerceAtMost(maxR)
            val lipPx = refinedLedge.toPx().coerceAtMost(size.height * 0.14f)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(topLit, midMatte, bottomCurve)
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, (size.height - lipPx).coerceAtLeast(1f)),
                cornerRadius = CornerRadius(cr, cr)
            )
        }
        .drawWithContent {
            drawContent()
            if (size.width <= 4f || size.height <= 4f) return@drawWithContent

            val maxR = (minOf(size.width, size.height) / 2f).coerceAtLeast(1f)
            val cr = cornerRadius.toPx().coerceAtMost(maxR)
            val lipPx = refinedLedge.toPx().coerceAtMost(size.height * 0.14f)
            val insetStroke = innerOffset.toPx().coerceIn(1f, maxR)
            val bodyH = (size.height - lipPx).coerceAtLeast(1f)

            // Crisp glossy white specular catchlight along upper rim (3D clay polish)
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isBrightLight) 0.90f else 0.65f),
                        Color.White.copy(alpha = 0.20f),
                        Color.Transparent,
                        deepBevel.copy(alpha = innerShadowAlpha)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, bodyH)
                ),
                topLeft = Offset(insetStroke * 0.35f, insetStroke * 0.35f),
                size = Size(
                    (size.width - insetStroke * 0.7f).coerceAtLeast(1f),
                    (bodyH - insetStroke * 0.7f).coerceAtLeast(1f)
                ),
                cornerRadius = CornerRadius(
                    (cr - insetStroke * 0.35f).coerceAtLeast(0f),
                    (cr - insetStroke * 0.35f).coerceAtLeast(0f)
                ),
                style = Stroke(width = 1.4.dp.toPx())
            )
        }
}

/**
 * Plump 3D Candy Clay Circular Medallion (for icon buttons & floating dock medallions).
 */
fun Modifier.clayDisc3D(
    surfaceColor: Color = ClayPeach,
    elevation: Dp = 10.dp,
    shadowTint: Color = ClayTealShadow,
    ledgeDepth: Dp = 3.5.dp
): Modifier {
    val isBrightLight = surfaceColor.luminance() > 0.72f
    val refinedLedge = (ledgeDepth * 0.85f).coerceIn(2.dp, 4.5.dp)
    val topLit = lerp(surfaceColor, Color.White, if (isBrightLight) 0.36f else 0.28f)
    val bottomCurve = lerp(surfaceColor, Color(0xFF0D203C), if (isBrightLight) 0.06f else 0.10f)
    val deepBevel = lerp(surfaceColor, Color(0xFF0A1A33), if (isBrightLight) 0.14f else 0.28f)
    val glowShadow = if (isBrightLight) shadowTint else lerp(surfaceColor, shadowTint, 0.30f)

    return this
        .shadow(
            elevation = (elevation * 0.9f).coerceAtLeast(4.dp),
            shape = CircleShape,
            clip = false,
            ambientColor = glowShadow.copy(alpha = if (isBrightLight) 0.20f else 0.36f),
            spotColor = glowShadow.copy(alpha = if (isBrightLight) 0.24f else 0.42f)
        )
        .clip(CircleShape)
        .background(deepBevel)
        .drawBehind {
            if (size.width <= 2f || size.height <= 2f) return@drawBehind
            val r = minOf(size.width, size.height) / 2f
            val lipPx = refinedLedge.toPx().coerceAtMost(r * 0.18f)
            val topCenter = Offset(size.width / 2f, size.height / 2f - lipPx * 0.42f)
            val faceRadius = (r - lipPx * 0.24f).coerceAtLeast(1f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(topLit, surfaceColor, bottomCurve),
                    center = Offset(size.width * 0.34f, size.height * 0.26f),
                    radius = faceRadius * 1.45f
                ),
                radius = faceRadius,
                center = topCenter
            )

            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.82f),
                        Color.White.copy(alpha = 0.18f),
                        deepBevel.copy(alpha = 0.22f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.85f, size.height * 0.85f)
                ),
                radius = (faceRadius - 1.dp.toPx()).coerceAtLeast(1f),
                center = topCenter,
                style = Stroke(width = 1.45.dp.toPx())
            )
        }
}

fun Modifier.plushFuzzySurface(
    baseColor: Color,
    isCircle: Boolean = false,
    cornerRadius: Dp = 24.dp,
    furLength: Dp = 3.dp,
    elevation: Dp = 10.dp
): Modifier = if (isCircle) {
    clayDisc3D(
        surfaceColor = baseColor,
        elevation = elevation,
        shadowTint = ClayTealShadow,
        ledgeDepth = 3.5.dp
    )
} else {
    claymorphic(
        cornerRadius = cornerRadius,
        surfaceColor = baseColor,
        elevation = elevation,
        shadowTint = ClayTealShadow,
        ledgeDepth = 4.dp
    )
}

fun Modifier.vibrantBlock3D(
    cornerRadius: Dp = 26.dp,
    topColor: Color = ClayPeachLight,
    bodyColor: Color = ClayPeach,
    deepLedgeColor: Color = ClayPeachDeep,
    ledgeDepth: Dp = 4.5.dp,
    elevation: Dp = 12.dp
): Modifier = claymorphic(
    cornerRadius = cornerRadius,
    surfaceColor = bodyColor,
    elevation = elevation,
    shadowTint = ClayTealShadow,
    ledgeDepth = ledgeDepth
)

/**
 * Clean Recessed Pastel Well (for speaker grilles, inputs, and rotary dial sockets).
 */
fun Modifier.pastelRecessedWell(
    cornerRadius: Dp = 20.dp,
    surfaceColor: Color = Color(0xFFE6F2FA),
    shadowTint: Color = ClayTealShadow
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val topWell = lerp(surfaceColor, shadowTint, 0.14f)
    val bottomWell = lerp(surfaceColor, Color.White, 0.36f)

    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(topWell, surfaceColor, bottomWell)))
        .drawWithContent {
            drawContent()
            if (size.width <= 1f || size.height <= 1f) return@drawWithContent
            val radiusPx = cornerRadius.toPx().coerceAtMost(minOf(size.width, size.height) / 2f)
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        shadowTint.copy(alpha = 0.20f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.88f)
                    )
                ),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                style = Stroke(width = 1.35.dp.toPx())
            )
        }
}

/**
 * iPhone-Smooth Non-Consuming Spring Touch Scale + Subtle Haptic Feedback:
 * Observes press/release on [PointerEventPass.Initial] without ever swallowing clicks or ripples,
 * delivering instantaneous 120fps iOS tactile bounce across every button and card.
 */
fun Modifier.clayPressScale(
    enabled: Boolean = true,
    reducedMotion: Boolean = false
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !reducedMotion) 0.955f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 460f),
        label = "ios_clay_press_scale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val anyDown = event.changes.any { it.pressed }
                    if (anyDown && !pressed) {
                        pressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } else if (!anyDown && pressed) {
                        pressed = false
                    }
                }
            }
        }
}
