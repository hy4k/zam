package com.example.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ClayActionButton
import com.example.ui.components.ClayCircleButton
import com.example.ui.components.ClayTextOnLight
import com.example.ui.components.Text3D
import com.example.ui.theme.ClayAqua
import com.example.ui.theme.ClayAquaDeep
import com.example.ui.theme.ClayAquaLight
import com.example.ui.theme.ClayCocoaInk
import com.example.ui.theme.ClayCoral
import com.example.ui.theme.ClayCoralDeep
import com.example.ui.theme.ClayCoralLight
import com.example.ui.theme.ClayDeepTealInk
import com.example.ui.theme.ClayHoney
import com.example.ui.theme.ClayHoneyDeep
import com.example.ui.theme.ClayHoneyLight
import com.example.ui.theme.ClayMutedInk
import com.example.ui.theme.ClayPeach
import com.example.ui.theme.ClayPeachLight
import com.example.ui.theme.ClaySage
import com.example.ui.theme.ClaySageDeep
import com.example.ui.theme.ClayTealShadow
import com.example.ui.theme.ClayTextCream
import com.example.ui.theme.ClayVanilla
import com.example.ui.theme.ClayVanillaLight
import com.example.ui.theme.claymorphic
import com.example.ui.theme.liquidGlass
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private enum class ArcadePhase { TITLE, PLAYING, PAUSED, GAME_OVER }
private enum class DropKind { STAR, SUPER_GEM, HEART, STORM_CLOUD, ZIGZAG_CLOUD }

private class FallingObject(
    var active: Boolean = false,
    var kind: DropKind = DropKind.STAR,
    var x: Float = 0.5f,          // normalized 0..1
    var y: Float = -0.1f,         // normalized 0..1
    var baseX: Float = 0.5f,
    var vy: Float = 0.25f,
    var radius: Float = 0.045f,
    var phase: Float = 0f,
    var telegraphSec: Float = 0f  // >0 means warning chevron before drop!
)

private class Particle(
    var active: Boolean = false,
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var life: Float = 0f,
    var maxLife: Float = 0.5f,
    var size: Float = 8f,
    var color: Color = ClayHoney
)

private class ScorePopup(
    var active: Boolean = false,
    var x: Float = 0.5f,
    var y: Float = 0.5f,
    var life: Float = 0f,
    var label: String = "+10",
    var color: Color = ClayHoney
)

/**
 * Full-Viewport 60fps Interactive Clay Arcade Game ("SKY CATCH"):
 * - Full Game Shell: Live Diorama Title Screen -> 60fps Playing -> Pause / Game Over Modal
 * - Low-Occlusion HUD (85%+ playfield completely clear, tabular-nums readouts)
 * - Clamped dt 60fps simulation with pooled particles, 45ms hitstop, decaying camera shake,
 *   squash-and-stretch clay rocket catcher, visible hazard windup telegraphs, star-magnet fever,
 *   and procedural synth audio with mute toggle!
 */
@Composable
fun SkyHopArcadeGame(
    playerName: String,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // UI Shell State (only updated on discrete events — NEVER on every frame!)
    var phase by remember { mutableStateOf(ArcadePhase.TITLE) }
    var score by remember { mutableIntStateOf(0) }
    var bestScore by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var combo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }
    var starsCaught by remember { mutableIntStateOf(0) }
    var level by remember { mutableIntStateOf(1) }
    var isMuted by remember { mutableStateOf(ArcadeAudioEngine.isMuted) }

    // Isolated 60fps Simulation Mutable Engine State (zero Compose allocations per frame!)
    val engine = remember {
        object {
            var playerX = 0.5f
            var targetX = 0.5f
            var playerTilt = 0f
            var squashX = 1f
            var squashY = 1f
            var invulnSec = 0f
            var magnetSec = 0f
            var hitstopSec = 0f
            var shakeIntensity = 0f
            var hitFlashAlpha = 0f
            var spawnTimer = 0.45f
            var elapsedWorld = 0f
            var frameTick by mutableLongStateOf(0L)

            val drops = Array(18) { FallingObject() }
            val particles = Array(54) { Particle() }
            val popups = Array(8) { ScorePopup() }

            fun resetRun() {
                playerX = 0.5f
                targetX = 0.5f
                playerTilt = 0f
                squashX = 1f
                squashY = 1f
                invulnSec = 0f
                magnetSec = 0f
                hitstopSec = 0f
                shakeIntensity = 0f
                hitFlashAlpha = 0f
                spawnTimer = 0.35f
                drops.forEach { it.active = false }
                particles.forEach { it.active = false }
                popups.forEach { it.active = false }
            }

            fun spawnParticles(nx: Float, ny: Float, count: Int, primary: Color, secondary: Color) {
                var spawned = 0
                for (p in particles) {
                    if (!p.active) {
                        val angle = Random.nextFloat() * (2f * PI.toFloat())
                        val speed = 0.14f + Random.nextFloat() * 0.38f
                        p.active = true
                        p.x = nx
                        p.y = ny
                        p.vx = cos(angle) * speed
                        p.vy = sin(angle) * speed - 0.12f
                        p.maxLife = 0.38f + Random.nextFloat() * 0.28f
                        p.life = p.maxLife
                        p.size = 5f + Random.nextFloat() * 7f
                        p.color = if (spawned % 2 == 0) primary else secondary
                        spawned++
                        if (spawned >= count) break
                    }
                }
            }

            fun spawnPopup(nx: Float, ny: Float, text: String, color: Color) {
                for (pop in popups) {
                    if (!pop.active) {
                        pop.active = true
                        pop.x = nx
                        pop.y = ny
                        pop.life = 0.72f
                        pop.label = text
                        pop.color = color
                        break
                    }
                }
            }

            fun spawnDrop(currentLevel: Int, currentLives: Int) {
                val slot = drops.firstOrNull { !it.active } ?: return
                val roll = Random.nextFloat()
                val hazardChance = (0.18f + (currentLevel - 1) * 0.045f).coerceAtMost(0.40f)
                val kind = when {
                    currentLives < 3 && roll < 0.06f -> DropKind.HEART
                    roll < 0.14f -> DropKind.SUPER_GEM
                    roll < 0.14f + hazardChance -> {
                        if (currentLevel >= 3 && Random.nextFloat() < 0.42f) DropKind.ZIGZAG_CLOUD
                        else DropKind.STORM_CLOUD
                    }
                    else -> DropKind.STAR
                }
                val laneX = 0.12f + Random.nextFloat() * 0.76f
                val speedRamp = 1f + (currentLevel - 1) * 0.11f

                slot.active = true
                slot.kind = kind
                slot.x = laneX
                slot.baseX = laneX
                slot.phase = Random.nextFloat() * 6.28f
                when (kind) {
                    DropKind.STAR -> {
                        slot.y = -0.05f
                        slot.vy = (0.28f + Random.nextFloat() * 0.08f) * speedRamp
                        slot.radius = 0.048f
                        slot.telegraphSec = 0f
                    }
                    DropKind.SUPER_GEM -> {
                        slot.y = -0.05f
                        slot.vy = 0.34f * speedRamp
                        slot.radius = 0.050f
                        slot.telegraphSec = 0f
                    }
                    DropKind.HEART -> {
                        slot.y = -0.05f
                        slot.vy = 0.25f * speedRamp
                        slot.radius = 0.048f
                        slot.telegraphSec = 0f
                    }
                    DropKind.STORM_CLOUD -> {
                        slot.y = 0.07f
                        slot.vy = (0.27f + Random.nextFloat() * 0.07f) * speedRamp
                        slot.radius = 0.054f
                        slot.telegraphSec = 0.62f // Visible windup telegraph!
                    }
                    DropKind.ZIGZAG_CLOUD -> {
                        slot.y = 0.07f
                        slot.vy = 0.29f * speedRamp
                        slot.radius = 0.052f
                        slot.telegraphSec = 0.68f
                    }
                }
            }
        }
    }

    fun startNewGame() {
        engine.resetRun()
        score = 0
        lives = 3
        combo = 0
        maxCombo = 0
        starsCaught = 0
        level = 1
        phase = ArcadePhase.PLAYING
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        ArcadeAudioEngine.playSuperGem()
    }

    // 60fps Frame Loop with Clamped dt (drives live title diorama AND real-time gameplay!)
    LaunchedEffect(phase) {
        var lastNanos = 0L
        while (phase == ArcadePhase.TITLE || phase == ArcadePhase.PLAYING) {
            withFrameNanos { now ->
                if (lastNanos == 0L) {
                    lastNanos = now
                    return@withFrameNanos
                }
                val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastNanos = now

                engine.elapsedWorld += dt

                if (phase == ArcadePhase.TITLE) {
                    // Calm animated diorama pan & hovering clay rocket catcher on Title Screen
                    engine.playerX = 0.5f + sin(engine.elapsedWorld * 1.4f) * 0.18f
                    engine.playerTilt = cos(engine.elapsedWorld * 1.4f) * 8f
                    engine.frameTick = now
                    return@withFrameNanos
                }

                // Brief simulation hitstop on heavy impact/fever
                if (engine.hitstopSec > 0f) {
                    engine.hitstopSec = (engine.hitstopSec - dt).coerceAtLeast(0f)
                    engine.frameTick = now
                    return@withFrameNanos
                }

                // Smooth responsive player glide + bank tilt + squash-and-stretch recovery
                val dx = engine.targetX - engine.playerX
                val step = dx * (dt * 15f).coerceAtMost(1f)
                engine.playerX = (engine.playerX + step).coerceIn(0.11f, 0.89f)
                engine.playerTilt = (dx * 58f).coerceIn(-18f, 18f)
                engine.squashX += (1f - engine.squashX) * (dt * 14f).coerceAtMost(1f)
                engine.squashY += (1f - engine.squashY) * (dt * 14f).coerceAtMost(1f)

                if (engine.invulnSec > 0f) engine.invulnSec = (engine.invulnSec - dt).coerceAtLeast(0f)
                if (engine.magnetSec > 0f) engine.magnetSec = (engine.magnetSec - dt).coerceAtLeast(0f)
                if (engine.hitFlashAlpha > 0f) engine.hitFlashAlpha = (engine.hitFlashAlpha - dt * 2.8f).coerceAtLeast(0f)
                if (engine.shakeIntensity > 0f) {
                    engine.shakeIntensity = (engine.shakeIntensity - dt * 28f).coerceAtLeast(0f)
                }

                // Spawn cadence ramps gently with level
                engine.spawnTimer -= dt
                if (engine.spawnTimer <= 0f) {
                    engine.spawnDrop(level, lives)
                    val baseGap = (0.98f - (level - 1) * 0.075f).coerceAtLeast(0.46f)
                    engine.spawnTimer = baseGap * (0.85f + Random.nextFloat() * 0.3f)
                }

                val catcherY = 0.835f
                val catcherRadius = 0.088f

                // Update falling collectibles & telegraphed hazards
                for (d in engine.drops) {
                    if (!d.active) continue

                    if (d.telegraphSec > 0f) {
                        d.telegraphSec = (d.telegraphSec - dt).coerceAtLeast(0f)
                        continue
                    }

                    d.phase += dt * 3.8f
                    d.y += d.vy * dt

                    if (d.kind == DropKind.ZIGZAG_CLOUD) {
                        d.x = (d.baseX + sin(d.phase) * 0.10f).coerceIn(0.10f, 0.90f)
                    }

                    // Star Magnet pull during Super Gem fever!
                    if (engine.magnetSec > 0f && (d.kind == DropKind.STAR || d.kind == DropKind.SUPER_GEM)) {
                        val pullX = engine.playerX - d.x
                        val pullY = catcherY - d.y
                        d.x += pullX * dt * 4.5f
                        d.y += pullY * dt * 1.4f
                    }

                    // Collision check with Clay Rocket Catcher
                    val distX = d.x - engine.playerX
                    val distY = d.y - catcherY
                    val hitDist = catcherRadius + d.radius
                    if (distX * distX + distY * distY <= hitDist * hitDist) {
                        d.active = false
                        when (d.kind) {
                            DropKind.STAR -> {
                                combo += 1
                                if (combo > maxCombo) maxCombo = combo
                                starsCaught += 1
                                val mult = (1 + combo / 5).coerceAtMost(4)
                                val pts = 10 * mult
                                score += pts
                                if (score > bestScore) bestScore = score
                                level = (1 + score / 140).coerceAtMost(10)
                                engine.squashX = 1.24f
                                engine.squashY = 0.80f
                                engine.spawnParticles(d.x, d.y, 10, ClayHoney, ClayVanillaLight)
                                engine.spawnPopup(d.x, d.y - 0.04f, if (mult > 1) "+$pts x$mult" else "+$pts", ClayHoney)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                ArcadeAudioEngine.playStarCatch(combo)
                            }
                            DropKind.SUPER_GEM -> {
                                combo += 2
                                if (combo > maxCombo) maxCombo = combo
                                starsCaught += 1
                                score += 30
                                if (score > bestScore) bestScore = score
                                level = (1 + score / 140).coerceAtMost(10)
                                engine.magnetSec = 4.5f
                                engine.hitstopSec = if (reducedMotion) 0f else 0.04f
                                engine.squashX = 1.32f
                                engine.squashY = 0.74f
                                engine.spawnParticles(d.x, d.y, 18, ClayAquaLight, ClayCoralLight)
                                engine.spawnPopup(d.x, d.y - 0.04f, "MAGNET +30!", ClayAqua)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                ArcadeAudioEngine.playSuperGem()
                            }
                            DropKind.HEART -> {
                                lives = (lives + 1).coerceAtMost(3)
                                engine.squashX = 1.22f
                                engine.squashY = 0.82f
                                engine.spawnParticles(d.x, d.y, 12, ClayCoralLight, ClayVanillaLight)
                                engine.spawnPopup(d.x, d.y - 0.04f, "+1 HEART!", ClayCoral)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                ArcadeAudioEngine.playHeartPickup()
                            }
                            DropKind.STORM_CLOUD, DropKind.ZIGZAG_CLOUD -> {
                                if (engine.invulnSec <= 0f) {
                                    combo = 0
                                    lives -= 1
                                    engine.invulnSec = 1.15f
                                    engine.hitstopSec = if (reducedMotion) 0f else 0.055f
                                    engine.shakeIntensity = if (reducedMotion) 0f else 11f
                                    engine.hitFlashAlpha = 0.42f
                                    engine.squashX = 0.76f
                                    engine.squashY = 1.28f
                                    engine.spawnParticles(d.x, d.y, 14, ClayCoral, ClayDeepTealInk)
                                    engine.spawnPopup(d.x, d.y - 0.04f, "ZAP!", ClayCoral)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (lives <= 0) {
                                        phase = ArcadePhase.GAME_OVER
                                        ArcadeAudioEngine.playGameOver()
                                    } else {
                                        ArcadeAudioEngine.playHazardBump()
                                    }
                                }
                            }
                        }
                        continue
                    }

                    // Missed past bottom
                    if (d.y > 1.08f) {
                        if (d.kind == DropKind.STAR && combo > 0) {
                            combo = 0
                        }
                        d.active = false
                    }
                }

                // Update pooled particles
                for (p in engine.particles) {
                    if (!p.active) continue
                    p.life -= dt
                    if (p.life <= 0f) {
                        p.active = false
                        continue
                    }
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                    p.vy += 0.65f * dt
                }

                // Update floating score popups
                for (pop in engine.popups) {
                    if (!pop.active) continue
                    pop.life -= dt
                    pop.y -= 0.14f * dt
                    if (pop.life <= 0f) pop.active = false
                }

                engine.frameTick = now
            }
        }
    }

    // Main Full-Viewport Game Shell Frame (Framed in Sculpted Matte Clay Bezel)
    Box(
        modifier = modifier
            .fillMaxSize()
            .claymorphic(
                cornerRadius = 32.dp,
                surfaceColor = ClayPeach,
                elevation = 16.dp,
                shadowTint = ClayTealShadow,
                ledgeDepth = 7.dp
            )
            .padding(6.dp)
            .clip(RoundedCornerShape(26.dp))
            .testTag("sky_catch_game_viewport")
    ) {
        // 1. ISOLATED 60FPS CLAY SKY VIEWPORT CANVAS
        val shakeX = if (engine.shakeIntensity > 0.2f) (Random.nextFloat() - 0.5f) * 2f * engine.shakeIntensity else 0f
        val shakeY = if (engine.shakeIntensity > 0.2f) (Random.nextFloat() - 0.5f) * 2f * engine.shakeIntensity else 0f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = shakeX
                    translationY = shakeY
                }
                .pointerInput(phase) {
                    if (phase != ArcadePhase.PLAYING) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            engine.targetX = (offset.x / size.width).coerceIn(0.11f, 0.89f)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            engine.targetX = (change.position.x / size.width).coerceIn(0.11f, 0.89f)
                        }
                    )
                }
                .pointerInput(phase) {
                    if (phase != ArcadePhase.PLAYING) return@pointerInput
                    detectTapGestures { offset ->
                        engine.targetX = (offset.x / size.width).coerceIn(0.11f, 0.89f)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Subscribe cleanly to 60fps ticker
                val tick = engine.frameTick
                val worldT = engine.elapsedWorld

                // Warm Golden-Hour Atelier Sky Gradient (Honey-Oak Dusk -> Warm Oat Sand -> Cream Cloud Floor)
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFFB58C5A),
                            0.44f to Color(0xFFDAB98E),
                            0.78f to Color(0xFFF3E2C8),
                            1.0f to ClayVanilla
                        )
                    )
                )

                // Parallax Drifting Fluffy Matte-Clay Clouds in the Background
                drawClayCloud(
                    cx = ((worldT * 0.035f + 0.18f) % 1.3f - 0.15f) * size.width,
                    cy = size.height * 0.20f,
                    scale = 1.0f
                )
                drawClayCloud(
                    cx = ((worldT * 0.022f + 0.72f) % 1.3f - 0.15f) * size.width,
                    cy = size.height * 0.36f,
                    scale = 0.78f
                )
                drawClayCloud(
                    cx = ((worldT * 0.018f + 0.45f) % 1.3f - 0.15f) * size.width,
                    cy = size.height * 0.56f,
                    scale = 0.64f
                )

                // Soft Clay Rolling Hills along the very bottom horizon
                drawCircle(
                    color = Color(0xFFDDC7AA).copy(alpha = 0.75f),
                    radius = size.width * 0.52f,
                    center = Offset(size.width * 0.22f, size.height * 1.15f)
                )
                drawCircle(
                    color = ClayPeach.copy(alpha = 0.72f),
                    radius = size.width * 0.48f,
                    center = Offset(size.width * 0.80f, size.height * 1.18f)
                )

                // Title Screen Ambient Orbiting Golden Clay Stars
                if (phase == ArcadePhase.TITLE) {
                    for (i in 0 until 5) {
                        val sx = size.width * (0.16f + i * 0.17f)
                        val sy = size.height * (0.24f + sin(worldT * 2.1f + i * 1.3f) * 0.04f)
                        drawClayStar(Offset(sx, sy), 14.dp.toPx(), worldT * 28f + i * 22f, ClayHoney, ClayHoneyLight)
                    }
                }

                // Active Collectibles & Telegraphed Hazards
                for (d in engine.drops) {
                    if (!d.active) continue
                    val px = d.x * size.width
                    val py = d.y * size.height
                    val rPx = d.radius * size.minDimension

                    if (d.telegraphSec > 0f) {
                        // Clear Visible Windup Telegraph Chevron at Top Edge!
                        val blink = ((sin(worldT * 24f) + 1f) * 0.5f)
                        val warnY = 66.dp.toPx()
                        drawCircle(
                            color = ClayCoral.copy(alpha = 0.25f + blink * 0.45f),
                            radius = 18.dp.toPx(),
                            center = Offset(px, warnY)
                        )
                        drawCircle(
                            color = ClayCoral,
                            radius = 11.dp.toPx(),
                            center = Offset(px, warnY)
                        )
                        drawLine(
                            color = ClayTextCream,
                            start = Offset(px, warnY - 5.dp.toPx()),
                            end = Offset(px, warnY + 1.dp.toPx()),
                            strokeWidth = 2.8.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            color = ClayTextCream,
                            radius = 1.6.dp.toPx(),
                            center = Offset(px, warnY + 5.dp.toPx())
                        )
                        continue
                    }

                    when (d.kind) {
                        DropKind.STAR -> {
                            drawClayStar(Offset(px, py), rPx * 1.08f, d.phase * 22f, ClayHoney, ClayHoneyLight)
                        }
                        DropKind.SUPER_GEM -> {
                            drawSuperClayGem(Offset(px, py), rPx * 1.12f, d.phase * 30f)
                        }
                        DropKind.HEART -> {
                            drawClayHeart(Offset(px, py), rPx * 1.05f)
                        }
                        DropKind.STORM_CLOUD, DropKind.ZIGZAG_CLOUD -> {
                            drawStormClayCloud(
                                center = Offset(px, py),
                                r = rPx * 1.15f,
                                isZigZag = d.kind == DropKind.ZIGZAG_CLOUD,
                                pulse = (sin(d.phase * 2.5f) + 1f) * 0.5f
                            )
                        }
                    }
                }

                // Player 3D Clay Rocket Basket Catcher (with Squash & Stretch + Magnet Aura + Mercy Blink!)
                val showPlayer = engine.invulnSec <= 0f || ((engine.invulnSec * 14f).toInt() % 2 == 0)
                if (showPlayer) {
                    val cx = engine.playerX * size.width
                    val cy = size.height * 0.835f
                    if (engine.magnetSec > 0f) {
                        val pulseR = 46.dp.toPx() + sin(worldT * 10f) * 6.dp.toPx()
                        drawCircle(
                            color = ClayAquaLight.copy(alpha = 0.28f),
                            radius = pulseR,
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = ClayAqua.copy(alpha = 0.72f),
                            radius = pulseR,
                            center = Offset(cx, cy),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                    drawClayCatcherShip(
                        center = Offset(cx, cy),
                        tiltDeg = engine.playerTilt,
                        squashX = engine.squashX,
                        squashY = engine.squashY,
                        flamePulse = (sin(worldT * 26f) + 1f) * 0.5f
                    )
                }

                // Pooled Particle Bursts
                for (p in engine.particles) {
                    if (!p.active) continue
                    val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
                    drawCircle(
                        color = p.color.copy(alpha = alpha),
                        radius = p.size * (0.4f + alpha * 0.6f),
                        center = Offset(p.x * size.width, p.y * size.height)
                    )
                }

                // Brief Hit Flash overlay on damage
                if (engine.hitFlashAlpha > 0.01f) {
                    drawRect(color = ClayCoral.copy(alpha = engine.hitFlashAlpha))
                }

                // Prevent dead-read warning on tick
                if (tick < 0L) drawCircle(Color.Transparent, 0f)
            }

            // Floating Score Popups (container-scaled so popups align on every screen size)
            if (phase == ArcadePhase.PLAYING) {
                androidx.compose.foundation.layout.BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val containerW = maxWidth
                    val containerH = maxHeight
                    engine.popups.forEach { pop ->
                        if (pop.active) {
                            val alpha = (pop.life / 0.72f).coerceIn(0f, 1f)
                            Text3D(
                                text = pop.label,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                ),
                                textColor = ClayTextCream,
                                extrudeColor = pop.color,
                                depthDp = 1.6.dp,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .graphicsLayer {
                                        translationX = ((pop.x - 0.08f).coerceIn(0.04f, 0.72f) * containerW.toPx())
                                        translationY = (pop.y.coerceIn(0.08f, 0.85f) * containerH.toPx())
                                        this.alpha = alpha
                                    }
                            )
                        }
                    }
                }
            }
        }

        // 2. LOW-OCCLUSION PERIMETER HUD (Playing / Paused — keeps >88% of viewport open!)
        if (phase == ArcadePhase.PLAYING || phase == ArcadePhase.PAUSED) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Top-Left: Score + Combo Cluster
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .claymorphic(
                                cornerRadius = 18.dp,
                                surfaceColor = ClayVanillaLight,
                                elevation = 6.dp,
                                ledgeDepth = 3.dp
                            )
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        ClayTextOnLight(
                            text = "★ $score",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFeatureSettings = "tnum"
                            ),
                            textColor = ClayDeepTealInk,
                            depthDp = 0.9.dp
                        )
                    }
                    if (combo >= 3) {
                        Box(
                            modifier = Modifier
                                .claymorphic(
                                    cornerRadius = 16.dp,
                                    surfaceColor = ClayHoney,
                                    elevation = 6.dp,
                                    ledgeDepth = 2.5.dp
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text3D(
                                text = "x${(1 + combo / 5).coerceAtMost(4)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                depthDp = 1.dp
                            )
                        }
                    }
                }

                // Top-Center: Hearts + Level
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .claymorphic(
                            cornerRadius = 18.dp,
                            surfaceColor = ClayPeach,
                            elevation = 6.dp,
                            ledgeDepth = 3.dp
                        )
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    for (h in 1..3) {
                        Icon(
                            imageVector = if (h <= lives) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Life $h",
                            tint = if (h <= lives) ClayCoral else ClayMutedInk.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    ClayTextOnLight(
                        text = "L$level",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontFeatureSettings = "tnum"
                        ),
                        textColor = ClayDeepTealInk,
                        depthDp = 0.6.dp
                    )
                }

                // Top-Right: Mute + Pause Clay Discs
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ClayCircleButton(
                        icon = if (isMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                        contentDescription = if (isMuted) "Unmute game sound" else "Mute game sound",
                        clayColor = ClayVanillaLight,
                        iconTint = ClayDeepTealInk,
                        size = 38.dp,
                        testTag = "sky_catch_mute_button",
                        onClick = {
                            isMuted = !isMuted
                            ArcadeAudioEngine.isMuted = isMuted
                        }
                    )
                    ClayCircleButton(
                        icon = if (phase == ArcadePhase.PAUSED) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = "Pause game",
                        clayColor = ClayAqua,
                        iconTint = ClayTextCream,
                        size = 38.dp,
                        testTag = "sky_catch_pause_button",
                        onClick = {
                            phase = if (phase == ArcadePhase.PLAYING) ArcadePhase.PAUSED else ArcadePhase.PLAYING
                        }
                    )
                }
            }
        }

        // 3. TITLE SCREEN OVERLAY (Clean Negative Space over Live Clay-Sky Diorama!)
        AnimatedVisibility(
            visible = phase == ArcadePhase.TITLE,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp, vertical = 20.dp)
            ) {
                // Mute disc in top-right on Title screen
                ClayCircleButton(
                    icon = if (isMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                    contentDescription = "Toggle game audio",
                    clayColor = ClayVanillaLight,
                    iconTint = ClayDeepTealInk,
                    size = 42.dp,
                    testTag = "sky_catch_title_mute",
                    modifier = Modifier.align(Alignment.TopEnd),
                    onClick = {
                        isMuted = !isMuted
                        ArcadeAudioEngine.isMuted = isMuted
                    }
                )

                // Bespoke 3D Molded Title Lockup + Primary CTA + Single Flat Control-Hint Line
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .widthIn(max = 320.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text3D(
                        text = "SKY CATCH",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 42.sp,
                            letterSpacing = 0.5.sp
                        ),
                        textColor = ClayTextCream,
                        extrudeColor = ClayDeepTealInk,
                        depthDp = 3.2.dp,
                        textAlign = TextAlign.Center
                    )

                    if (bestScore > 0) {
                        Box(
                            modifier = Modifier
                                .claymorphic(
                                    cornerRadius = 18.dp,
                                    surfaceColor = ClayVanillaLight,
                                    elevation = 7.dp,
                                    ledgeDepth = 3.dp
                                )
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            ClayTextOnLight(
                                text = "BEST SCORE • $bestScore",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFeatureSettings = "tnum"
                                ),
                                textColor = ClayDeepTealInk,
                                depthDp = 0.8.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    ClayActionButton(
                        text = "PLAY NOW",
                        containerColor = ClayCoral,
                        icon = Icons.Rounded.PlayArrow,
                        testTag = "sky_catch_start_button",
                        minHeight = 58.dp,
                        modifier = Modifier.fillMaxWidth(0.78f),
                        onClick = { startNewGame() }
                    )

                    Box(
                        modifier = Modifier
                            .liquidGlass(
                                shape = RoundedCornerShape(18.dp),
                                surfaceAlpha = 0.82f,
                                tintColor = ClayVanillaLight,
                                cornerRadius = 18.dp
                            )
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        ClayTextOnLight(
                            text = "Slide to glide • Catch ★ Stars • Dodge Clouds",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            textColor = ClayDeepTealInk,
                            textAlign = TextAlign.Center,
                            depthDp = 0.5.dp
                        )
                    }
                }
            }
        }

        // 4. PAUSE MODAL
        AnimatedVisibility(
            visible = phase == ArcadePhase.PAUSED,
            enter = fadeIn() + scaleIn(initialScale = 0.92f),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ClayDeepTealInk.copy(alpha = 0.42f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .fillMaxWidth(0.84f)
                        .claymorphic(
                            cornerRadius = 30.dp,
                            surfaceColor = ClayVanilla,
                            elevation = 18.dp,
                            ledgeDepth = 7.dp
                        )
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ClayTextOnLight(
                        text = "PAUSED",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        textColor = ClayDeepTealInk,
                        depthDp = 1.6.dp
                    )
                    ClayActionButton(
                        text = "Resume",
                        containerColor = ClaySage,
                        icon = Icons.Rounded.PlayArrow,
                        testTag = "sky_catch_resume_button",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { phase = ArcadePhase.PLAYING }
                    )
                    ClayActionButton(
                        text = "Restart",
                        containerColor = ClayHoney,
                        icon = Icons.Rounded.Refresh,
                        testTag = "sky_catch_pause_restart_button",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { startNewGame() }
                    )
                }
            }
        }

        // 5. GAME OVER / VICTORY CELEBRATION MODAL
        AnimatedVisibility(
            visible = phase == ArcadePhase.GAME_OVER,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ClayDeepTealInk.copy(alpha = 0.48f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .fillMaxWidth(0.86f)
                        .claymorphic(
                            cornerRadius = 32.dp,
                            surfaceColor = ClayPeach,
                            elevation = 18.dp,
                            ledgeDepth = 8.dp
                        )
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(ClayHoney)
                            .border(2.5.dp, ClayVanillaLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = "Trophy",
                            tint = ClayTextCream,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    ClayTextOnLight(
                        text = "Awesome Run, $playerName!",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        textColor = ClayDeepTealInk,
                        textAlign = TextAlign.Center,
                        depthDp = 1.4.dp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .claymorphic(
                                cornerRadius = 20.dp,
                                surfaceColor = ClayVanillaLight,
                                elevation = 6.dp,
                                ledgeDepth = 3.dp
                            )
                            .padding(vertical = 12.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatChip(label = "SCORE", value = "$score")
                        StatChip(label = "BEST", value = "$bestScore")
                        StatChip(label = "STARS", value = "$starsCaught")
                        StatChip(label = "STREAK", value = "${maxCombo}x")
                    }

                    ClayActionButton(
                        text = "Play Again",
                        containerColor = ClayCoral,
                        icon = Icons.Rounded.Refresh,
                        testTag = "sky_catch_play_again_button",
                        modifier = Modifier.fillMaxWidth(),
                        minHeight = 54.dp,
                        onClick = { startNewGame() }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ClayTextOnLight(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontFeatureSettings = "tnum"
            ),
            textColor = ClayCoralDeep,
            depthDp = 0.8.dp
        )
        ClayTextOnLight(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            textColor = ClayMutedInk,
            depthDp = 0.4.dp
        )
    }
}

// =============================================================================
// BESPOKE PIXAR 3D MATTE CLAY CANVAS SPRITES (Zero external bitmaps, 60fps!)
// =============================================================================

private fun DrawScope.drawClayCloud(cx: Float, cy: Float, scale: Float) {
    val r = 24.dp.toPx() * scale
    val tint = ClayVanillaLight.copy(alpha = 0.72f)
    val shadow = ClayAquaDeep.copy(alpha = 0.10f)
    drawCircle(shadow, r * 1.1f, Offset(cx, cy + 5f * scale))
    drawCircle(tint, r * 0.85f, Offset(cx - r * 0.9f, cy + r * 0.15f))
    drawCircle(tint, r * 1.12f, Offset(cx, cy - r * 0.1f))
    drawCircle(tint, r * 0.82f, Offset(cx + r * 0.9f, cy + r * 0.18f))
}

private fun DrawScope.drawClayStar(center: Offset, radius: Float, rotationDeg: Float, body: Color, highlight: Color) {
    rotate(degrees = rotationDeg, pivot = center) {
        val path = Path()
        val points = 5
        val innerR = radius * 0.48f
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) radius else innerR
            val angle = (i * PI / points - PI / 2.0)
            val x = center.x + (cos(angle) * r).toFloat()
            val y = center.y + (sin(angle) * r).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        // 3D depth shadow
        drawCircle(ClayTealShadow.copy(alpha = 0.22f), radius * 0.72f, Offset(center.x, center.y + 4.dp.toPx()))
        drawPath(path, color = ClayHoneyDeep, style = Stroke(width = 3.dp.toPx()))
        drawPath(
            path,
            brush = Brush.radialGradient(
                listOf(highlight, body, ClayHoneyDeep),
                center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f),
                radius = radius * 1.4f
            )
        )
        drawCircle(Color.White.copy(alpha = 0.78f), radius * 0.2f, Offset(center.x - radius * 0.2f, center.y - radius * 0.2f))
    }
}

private fun DrawScope.drawSuperClayGem(center: Offset, radius: Float, rotationDeg: Float) {
    rotate(degrees = rotationDeg, pivot = center) {
        val path = Path().apply {
            moveTo(center.x, center.y - radius)
            lineTo(center.x + radius * 0.82f, center.y)
            lineTo(center.x, center.y + radius)
            lineTo(center.x - radius * 0.82f, center.y)
            close()
        }
        drawCircle(ClayAquaLight.copy(alpha = 0.38f), radius * 1.28f, center)
        drawPath(
            path,
            brush = Brush.linearGradient(
                listOf(ClayAquaLight, ClayAqua, ClayCoralLight),
                start = Offset(center.x - radius, center.y - radius),
                end = Offset(center.x + radius, center.y + radius)
            )
        )
        drawPath(path, color = ClayVanillaLight, style = Stroke(width = 2.4.dp.toPx()))
    }
}

private fun DrawScope.drawClayHeart(center: Offset, radius: Float) {
    drawCircle(ClayCoralLight.copy(alpha = 0.35f), radius * 1.25f, center)
    val path = Path().apply {
        moveTo(center.x, center.y + radius * 0.78f)
        cubicTo(
            center.x - radius * 1.3f, center.y + radius * 0.1f,
            center.x - radius * 0.9f, center.y - radius * 0.95f,
            center.x, center.y - radius * 0.28f
        )
        cubicTo(
            center.x + radius * 0.9f, center.y - radius * 0.95f,
            center.x + radius * 1.3f, center.y + radius * 0.1f,
            center.x, center.y + radius * 0.78f
        )
        close()
    }
    drawPath(path, brush = Brush.verticalGradient(listOf(ClayCoralLight, ClayCoral, ClayCoralDeep)))
    drawCircle(Color.White.copy(alpha = 0.75f), radius * 0.18f, Offset(center.x - radius * 0.32f, center.y - radius * 0.22f))
}

private fun DrawScope.drawStormClayCloud(center: Offset, r: Float, isZigZag: Boolean, pulse: Float) {
    val darkCloud = if (isZigZag) Color(0xFF4A3426) else Color(0xFF5C4231)
    val rimCloud = if (isZigZag) Color(0xFF785A45) else Color(0xFF8C6B53)

    // Little golden-coral lightning bolt peeking beneath the storm cloud!
    val bolt = Path().apply {
        moveTo(center.x + r * 0.08f, center.y + r * 0.35f)
        lineTo(center.x - r * 0.18f, center.y + r * 0.85f)
        lineTo(center.x + r * 0.06f, center.y + r * 0.85f)
        lineTo(center.x - r * 0.10f, center.y + r * 1.35f)
    }
    drawPath(
        bolt,
        color = ClayHoneyLight.copy(alpha = 0.65f + pulse * 0.35f),
        style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
    )

    // Puffy dark clay storm cloud lobes
    drawCircle(darkCloud, r * 0.68f, Offset(center.x - r * 0.56f, center.y + r * 0.12f))
    drawCircle(darkCloud, r * 0.68f, Offset(center.x + r * 0.56f, center.y + r * 0.12f))
    drawCircle(rimCloud, r * 0.82f, Offset(center.x, center.y - r * 0.08f))
    drawCircle(darkCloud, r * 0.76f, Offset(center.x, center.y + r * 0.04f))

    // Cute grumpy eyebrows / eyes so a 6-year-old immediately reads it as a playful obstacle!
    drawCircle(ClayVanillaLight, r * 0.13f, Offset(center.x - r * 0.26f, center.y + r * 0.05f))
    drawCircle(ClayVanillaLight, r * 0.13f, Offset(center.x + r * 0.26f, center.y + r * 0.05f))
    drawCircle(ClayCocoaInk, r * 0.07f, Offset(center.x - r * 0.25f, center.y + r * 0.07f))
    drawCircle(ClayCocoaInk, r * 0.07f, Offset(center.x + r * 0.25f, center.y + r * 0.07f))
}

private fun DrawScope.drawClayCatcherShip(
    center: Offset,
    tiltDeg: Float,
    squashX: Float,
    squashY: Float,
    flamePulse: Float
) {
    val w = 78.dp.toPx() * squashX
    val h = 38.dp.toPx() * squashY

    rotate(degrees = tiltDeg, pivot = center) {
        // Thruster flame glow underneath
        val flameH = (12.dp.toPx() + flamePulse * 8.dp.toPx()) * squashY
        drawOval(
            brush = Brush.verticalGradient(listOf(ClayHoneyLight, ClayCoral, Color.Transparent)),
            topLeft = Offset(center.x - w * 0.18f, center.y + h * 0.38f),
            size = Size(w * 0.36f, flameH)
        )

        // Drop shadow
        drawOval(
            color = ClayTealShadow.copy(alpha = 0.22f),
            topLeft = Offset(center.x - w * 0.52f, center.y + h * 0.62f),
            size = Size(w * 1.04f, 10.dp.toPx())
        )

        // Cute Molded Aqua Side Wings
        drawRoundRect(
            color = ClayAquaDeep,
            topLeft = Offset(center.x - w * 0.60f, center.y - h * 0.05f),
            size = Size(w * 1.20f, h * 0.44f),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(ClayAquaLight, ClayAqua)),
            topLeft = Offset(center.x - w * 0.58f, center.y - h * 0.12f),
            size = Size(w * 1.16f, h * 0.40f),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )

        // Main Sculpted Watermelon-Coral Clay Saucer / Star Basket Hull
        drawRoundRect(
            color = ClayCoralDeep,
            topLeft = Offset(center.x - w * 0.50f, center.y - h * 0.38f + 4.dp.toPx()),
            size = Size(w, h),
            cornerRadius = CornerRadius(h * 0.5f, h * 0.5f)
        )
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(ClayCoralLight, ClayCoral, ClayCoralDeep)),
            topLeft = Offset(center.x - w * 0.50f, center.y - h * 0.42f),
            size = Size(w, h),
            cornerRadius = CornerRadius(h * 0.5f, h * 0.5f)
        )

        // Frosted Cream Glass Catcher Rim & Golden Star Emblem
        drawRoundRect(
            color = ClayVanillaLight,
            topLeft = Offset(center.x - w * 0.36f, center.y - h * 0.32f),
            size = Size(w * 0.72f, h * 0.34f),
            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
        )
        drawCircle(
            color = ClayHoneyLight,
            radius = 6.dp.toPx(),
            center = Offset(center.x, center.y + h * 0.22f)
        )
    }
}
