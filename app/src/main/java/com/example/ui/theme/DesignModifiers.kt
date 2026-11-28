package com.example.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
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
 * Smooth Frosted Clay-Glass Surface (inspired by the frosted translucent glass bar at the bottom
 * of the attached studio clay image):
 * Clean translucent warm-cream glass with crisp top specular catchlight and soft studio shadow.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(30.dp),
    surfaceAlpha: Float = 0.80f,
    tintColor: Color = ClayVanillaLight,
    applyBlur: Boolean = false,
    reducedMotion: Boolean = false,
    cornerRadius: Dp = 30.dp
): Modifier {
    val topFrost = lerp(tintColor, Color.White, 0.35f).copy(alpha = (surfaceAlpha + 0.10f).coerceAtMost(0.94f))
    val bottomFrost = tintColor.copy(alpha = (surfaceAlpha - 0.06f).coerceAtLeast(0.68f))
    return this
        .shadow(
            elevation = 16.dp,
            shape = shape,
            clip = false,
            ambientColor = ClayTealShadow.copy(alpha = 0.34f),
            spotColor = ClayTealShadow.copy(alpha = 0.38f)
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
            val strokePx = 1.6.dp.toPx()
            val cornerPx = cornerRadius.toPx().coerceAtMost(minOf(size.width, size.height) / 2f)
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.92f),
                        Color.White.copy(alpha = 0.42f),
                        ClayTealShadow.copy(alpha = 0.16f)
                    )
                ),
                cornerRadius = CornerRadius(cornerPx, cornerPx),
                style = Stroke(width = strokePx)
            )
        }
}

/**
 * SIGNATURE PIXAR 3D MATTE CLAYMORPHISM (100% Pure Compose GPU brushes, zero saveLayer / BlurMaskFilter):
 * Directly captures the smooth, velvety, physically-thick molded clay discs & objects in the attached image:
 *  - Soft, wide directional studio drop shadow onto the mint-teal wall
 *  - Physical 3D molded bottom bevel lip (`ledgeDepth`) so every button/card feels sculpted out of real clay
 *  - Smooth matte sunlit top-left to warm-curved bottom-right clay dome gradient
 *  - Pillowy soft white top-left inner catchlight + subtle curved bottom-right inner clay rim
 */
fun Modifier.claymorphic(
    cornerRadius: Dp = 30.dp,
    surfaceColor: Color = ClayPeach,
    elevation: Dp = 14.dp,
    innerShadowAlpha: Float = 0.18f,
    innerOffset: Dp = 4.5.dp,
    shadowTint: Color = ClayTealShadow,
    ledgeDepth: Dp = 5.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val topLit = lerp(surfaceColor, Color.White, 0.28f)
    val midMatte = surfaceColor
    val bottomCurve = lerp(surfaceColor, shadowTint, 0.10f)
    val deepBevel = lerp(surfaceColor, Color(0xFF4A2A1E), 0.24f)

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = shadowTint.copy(alpha = 0.42f),
            spotColor = shadowTint.copy(alpha = 0.46f)
        )
        .clip(shape)
        .background(deepBevel)
        .drawBehind {
            if (size.width <= 2f || size.height <= 2f) return@drawBehind
            val maxR = minOf(size.width, size.height) / 2f
            val cr = cornerRadius.toPx().coerceAtMost(maxR)
            val lipPx = ledgeDepth.toPx().coerceAtMost(size.height * 0.18f)

            // Main sculpted matte clay body floating cleanly above its 3D base thickness
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
            val lipPx = ledgeDepth.toPx().coerceAtMost(size.height * 0.18f)
            val insetStroke = innerOffset.toPx().coerceIn(1f, maxR)
            val bodyH = (size.height - lipPx).coerceAtLeast(1f)

            // Pillowy sunlit top-left inner clay rim
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.78f),
                        Color.White.copy(alpha = 0.22f),
                        Color.Transparent,
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.85f, bodyH * 0.85f)
                ),
                topLeft = Offset(insetStroke * 0.4f, insetStroke * 0.4f),
                size = Size(
                    (size.width - insetStroke * 0.8f).coerceAtLeast(1f),
                    (bodyH - insetStroke * 0.8f).coerceAtLeast(1f)
                ),
                cornerRadius = CornerRadius(
                    (cr - insetStroke * 0.4f).coerceAtLeast(0f),
                    (cr - insetStroke * 0.4f).coerceAtLeast(0f)
                ),
                style = Stroke(width = insetStroke * 0.85f)
            )

            // Soft bottom-right curved inner shading
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        deepBevel.copy(alpha = innerShadowAlpha * 0.75f),
                        deepBevel.copy(alpha = innerShadowAlpha * 1.25f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, bodyH)
                ),
                topLeft = Offset(insetStroke * 0.45f, insetStroke * 0.45f),
                size = Size(
                    (size.width - insetStroke * 0.9f).coerceAtLeast(1f),
                    (bodyH - insetStroke * 0.9f).coerceAtLeast(1f)
                ),
                cornerRadius = CornerRadius(
                    (cr - insetStroke * 0.45f).coerceAtLeast(0f),
                    (cr - insetStroke * 0.45f).coerceAtLeast(0f)
                ),
                style = Stroke(width = insetStroke * 0.85f)
            )
        }
}

/**
 * Smooth 3D Molded Circular Matte Clay Disc (exact match for the 6 floating Peach-Cream
 * circular clay discs and floating round buttons in the attached image).
 */
fun Modifier.clayDisc3D(
    surfaceColor: Color = ClayPeach,
    elevation: Dp = 14.dp,
    shadowTint: Color = ClayTealShadow,
    ledgeDepth: Dp = 4.5.dp
): Modifier {
    val topLit = lerp(surfaceColor, Color.White, 0.30f)
    val bottomCurve = lerp(surfaceColor, Color(0xFF4F2B1D), 0.14f)
    val deepBevel = lerp(surfaceColor, Color(0xFF3E2218), 0.28f)

    return this
        .shadow(
            elevation = elevation,
            shape = CircleShape,
            clip = false,
            ambientColor = shadowTint.copy(alpha = 0.44f),
            spotColor = shadowTint.copy(alpha = 0.50f)
        )
        .clip(CircleShape)
        .background(deepBevel)
        .drawBehind {
            if (size.width <= 2f || size.height <= 2f) return@drawBehind
            val r = minOf(size.width, size.height) / 2f
            val lipPx = ledgeDepth.toPx().coerceAtMost(r * 0.20f)
            val topCenter = Offset(size.width / 2f, size.height / 2f - lipPx * 0.48f)
            val faceRadius = (r - lipPx * 0.35f).coerceAtLeast(1f)

            // Smooth matte sculpted clay disc face
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(topLit, surfaceColor, bottomCurve),
                    center = Offset(size.width * 0.34f, size.height * 0.28f),
                    radius = faceRadius * 1.45f
                ),
                radius = faceRadius,
                center = topCenter
            )

            // Soft sunlit top-left specular clay rim
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.82f),
                        Color.White.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.78f, size.height * 0.78f)
                ),
                radius = (faceRadius - 1.5.dp.toPx()).coerceAtLeast(1f),
                center = topCenter,
                style = Stroke(width = 2.6.dp.toPx())
            )
        }
}

/**
 * Compatibility delegate: routes plushFuzzySurface calls cleanly to smooth Pixar Matte Claymorphism
 * so every button across all screens matches the attached image's sleek sculpted clay aesthetic!
 */
fun Modifier.plushFuzzySurface(
    baseColor: Color,
    isCircle: Boolean = false,
    cornerRadius: Dp = 26.dp,
    furLength: Dp = 4.dp,
    elevation: Dp = 12.dp
): Modifier = if (isCircle) {
    clayDisc3D(
        surfaceColor = baseColor,
        elevation = elevation,
        shadowTint = ClayTealShadow,
        ledgeDepth = 4.dp
    )
} else {
    claymorphic(
        cornerRadius = cornerRadius,
        surfaceColor = baseColor,
        elevation = elevation,
        shadowTint = ClayTealShadow,
        ledgeDepth = 4.5.dp
    )
}

/**
 * Compatibility delegate: routes vibrantBlock3D to our signature Matte 3D Claymorphism slab!
 */
fun Modifier.vibrantBlock3D(
    cornerRadius: Dp = 30.dp,
    topColor: Color = ClayPeachLight,
    bodyColor: Color = ClayPeach,
    deepLedgeColor: Color = ClayPeachDeep,
    ledgeDepth: Dp = 6.dp,
    elevation: Dp = 16.dp
): Modifier = claymorphic(
    cornerRadius = cornerRadius,
    surfaceColor = bodyColor,
    elevation = elevation,
    shadowTint = ClayTealShadow,
    ledgeDepth = ledgeDepth
)

/**
 * Recessed / Debossed Clay Well (for speaker grilles, inputs, and push-to-talk sockets).
 */
fun Modifier.pastelRecessedWell(
    cornerRadius: Dp = 24.dp,
    surfaceColor: Color = Color(0xFFF3CBB0),
    shadowTint: Color = ClayTealShadow
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val topWell = lerp(surfaceColor, shadowTint, 0.16f)
    val bottomWell = lerp(surfaceColor, Color.White, 0.35f)

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
                        shadowTint.copy(alpha = 0.28f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.88f)
                    )
                ),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                style = Stroke(width = 1.6.dp.toPx())
            )
        }
}

/**
 * Tactile spring bounce for kid-friendly 3D clay buttons; respects Reduced Motion accessibility.
 */
fun Modifier.clayPressScale(
    enabled: Boolean = true,
    reducedMotion: Boolean = false
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !reducedMotion) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.54f, stiffness = 500f),
        label = "clay_press_scale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                }
            )
        }
}
