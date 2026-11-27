package com.example.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
 * Liquid Glass Backdrop Layer:
 * Kept hardware-buffer-safe across all Android GPUs and emulators (avoids RenderEffect R8
 * AHardwareBuffer allocation failures while liquidGlass provides frosted gradients & specular rims).
 */
fun Modifier.liquidGlassBlurLayer(
    radius: Float = 25f,
    reducedMotion: Boolean = false
): Modifier = this

/**
 * Liquid Glass 2.0 Surface + Dual Specular Bevel Rim:
 * Frosted multi-stop diagonal gradient + outer colored ambient shadow + 1.25dp specular rim.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(28.dp),
    surfaceAlpha: Float = 0.55f,
    tintColor: Color = Color.White,
    applyBlur: Boolean = false,
    reducedMotion: Boolean = false,
    cornerRadius: Dp = 28.dp
): Modifier {
    val topFrost = tintColor.copy(alpha = (surfaceAlpha + 0.14f).coerceAtMost(0.92f))
    val bottomFrost = tintColor.copy(alpha = (surfaceAlpha - 0.12f).coerceAtLeast(0.16f))
    return this
        .shadow(
            elevation = 14.dp,
            shape = shape,
            clip = false,
            ambientColor = StudioTealDeep.copy(alpha = 0.22f),
            spotColor = StudioTealDeep.copy(alpha = 0.26f)
        )
        .clip(shape)
        .then(if (applyBlur) Modifier.liquidGlassBlurLayer(25f, reducedMotion) else Modifier)
        .background(
            brush = Brush.linearGradient(
                colors = listOf(topFrost, bottomFrost)
            )
        )
        .drawBehind {
            if (size.width <= 0f || size.height <= 0f) return@drawBehind
            val strokePx = 1.25.dp.toPx()
            val cornerPx = cornerRadius.toPx().coerceAtMost(minOf(size.width, size.height) / 2f)
            // Specular diagonal glass edge
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.78f),
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.42f)
                    )
                ),
                cornerRadius = CornerRadius(cornerPx, cornerPx),
                style = Stroke(width = strokePx)
            )
        }
}

/**
 * Claymorphism 2.0 Sculpted 3D Toy Surface Formula (100% Pure Compose, zero saveLayer / BlurMaskFilter):
 * - Pillowy outer ambient shadow with surface-tinted glow
 * - Vertical matte-sheen gradient (top slightly brighter, base deeper)
 * - Soft multi-layer inner inset shadow & top-left clay pillow highlight drawn with Compose brushes
 * - Hairline specular rim for ultra-refined physical object depth without R8 AHardwareBuffer allocations
 */
fun Modifier.claymorphic(
    cornerRadius: Dp = 28.dp,
    surfaceColor: Color = Color.White,
    elevation: Dp = 12.dp,
    innerShadowAlpha: Float = 0.16f,
    innerOffset: Dp = 4.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val topLit = lerp(surfaceColor, Color.White, 0.14f)
    val bottomMatte = lerp(surfaceColor, Color.Black, 0.05f)

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = surfaceColor.copy(alpha = 0.35f),
            spotColor = Color(0xFF0F292E).copy(alpha = 0.28f)
        )
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(topLit, surfaceColor, bottomMatte)
            )
        )
        .drawWithContent {
            drawContent()
            if (size.width <= 1f || size.height <= 1f) return@drawWithContent

            val maxRadius = minOf(size.width, size.height) / 2f
            val radiusPx = cornerRadius.toPx().coerceAtMost(maxRadius)
            val insetStroke = innerOffset.toPx().coerceIn(2f, maxRadius)

            // Soft bottom-right inner inset depth (pure GPU gradient, no BlurMaskFilter / saveLayer)
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = innerShadowAlpha * 0.45f),
                        Color.Black.copy(alpha = innerShadowAlpha)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                topLeft = Offset(insetStroke * 0.5f, insetStroke * 0.5f),
                size = Size(
                    (size.width - insetStroke).coerceAtLeast(1f),
                    (size.height - insetStroke).coerceAtLeast(1f)
                ),
                cornerRadius = CornerRadius(
                    (radiusPx - insetStroke * 0.5f).coerceAtLeast(0f),
                    (radiusPx - insetStroke * 0.5f).coerceAtLeast(0f)
                ),
                style = Stroke(width = insetStroke)
            )

            // Top-left pillowy clay highlight sheen
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.52f),
                        Color.White.copy(alpha = 0.14f),
                        Color.Transparent,
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                topLeft = Offset(insetStroke * 0.45f, insetStroke * 0.45f),
                size = Size(
                    (size.width - insetStroke * 0.9f).coerceAtLeast(1f),
                    (size.height - insetStroke * 0.9f).coerceAtLeast(1f)
                ),
                cornerRadius = CornerRadius(
                    (radiusPx - insetStroke * 0.45f).coerceAtLeast(0f),
                    (radiusPx - insetStroke * 0.45f).coerceAtLeast(0f)
                ),
                style = Stroke(width = insetStroke * 0.85f)
            )

            // Crisp 1dp hairline rim
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                style = Stroke(width = 1.dp.toPx())
            )
        }
}

/**
 * Tactile spring bounce for child-friendly clay buttons; respects Reduced Motion accessibility.
 */
fun Modifier.clayPressScale(
    enabled: Boolean = true,
    reducedMotion: Boolean = false
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !reducedMotion) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 520f),
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
