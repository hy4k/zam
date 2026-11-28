package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.UserProfileEntity
import com.example.data.model.FamilyMemberId
import com.example.data.model.MemberPresenceStatus
import com.example.data.model.PresenceAvailability
import com.example.data.model.UserSessionState
import com.example.data.model.statusColor
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
import com.example.ui.theme.ClayPeachDeep
import com.example.ui.theme.ClayPeachLight
import com.example.ui.theme.ClaySage
import com.example.ui.theme.ClaySageDeep
import com.example.ui.theme.ClaySageLight
import com.example.ui.theme.ClayTealShadow
import com.example.ui.theme.ClayTextCream
import com.example.ui.theme.ClayVanilla
import com.example.ui.theme.ClayVanillaDeep
import com.example.ui.theme.ClayVanillaLight
import com.example.ui.theme.EmeraldAccept
import com.example.ui.theme.LocalKinNestAccessibility
import com.example.ui.theme.claymorphic
import com.example.ui.theme.pastelRecessedWell
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Page 1 — Authentic Retro Pixar 3D Matte Clay Portable Radio with Kid-Friendly
 * Vintage Landline Rotary Dial Unlock Puzzle & Physical 3D Pop-Up Hold-To-Talk Plunger!
 * No top page title; clean live family presence pills + Honey-Gold 3D Gear button.
 */
@Composable
fun HomeTreehouseScreen(
    session: UserSessionState,
    profiles: List<UserProfileEntity>,
    memberPresence: Map<FamilyMemberId, MemberPresenceStatus>,
    isWalkieRecording: Boolean,
    onSelectPresenceStatus: (PresenceAvailability) -> Unit,
    onWalkiePressStart: () -> Unit,
    onWalkiePressRelease: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val a11y = LocalKinNestAccessibility.current
    val selfMember = session.deviceOwner ?: session.activeMember ?: FamilyMemberId.MITHUN
    val selfPresence = memberPresence[selfMember]
    val selfIsOnline = selfPresence?.isOnline == true
    val selfAvailability = selfPresence?.availability ?: PresenceAvailability.OFFLINE
    var showStatusPicker by remember { mutableStateOf(false) }

    val audioPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Row (NO page name!) — Live Supabase Realtime Presence Cards (Mithun, Mafitha, Zayn) + Gear Button
        Row(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                FamilyMemberId.entries.forEach { member ->
                    val status = memberPresence[member] ?: MemberPresenceStatus(member)
                    val availability = status.availability
                    val dotColor = availability.statusColor()
                    val isSelf = member == selfMember
                    val shortName = member.displayName.substringBefore(" ")
                    val statusText = status.shortBadgeLabel()

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .claymorphic(
                                cornerRadius = 18.dp,
                                surfaceColor = if (isSelf) ClayVanillaLight else ClayPeach,
                                elevation = 7.dp,
                                ledgeDepth = 3.dp
                            )
                            .then(
                                if (isSelf) {
                                    Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        showStatusPicker = !showStatusPicker
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 5.dp)
                            .testTag("presence_card_${member.name.lowercase()}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.5.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                                    .border(1.2.dp, Color.White, CircleShape)
                            )
                            ClayTextOnLight(
                                text = shortName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.5.sp
                                ),
                                textColor = if (isSelf) ClayDeepTealInk else ClayCocoaInk,
                                depthDp = 0.7.dp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        ClayTextOnLight(
                            text = if (isSelf) "$statusText ▾" else statusText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.5.sp
                            ),
                            textColor = when (availability) {
                                PresenceAvailability.ONLINE -> ClaySageDeep
                                PresenceAvailability.AWAY -> ClayHoneyDeep
                                PresenceAvailability.BUSY -> ClayCoralDeep
                                PresenceAvailability.OFFLINE -> ClayMutedInk
                            },
                            depthDp = 0.4.dp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Sculpted 3D Matte Clay Honey-Gold Gear Button
            ClayCircleButton(
                icon = Icons.Rounded.Settings,
                contentDescription = "Open settings and profile editor",
                clayColor = ClayHoney,
                iconTint = ClayTextCream,
                size = 48.dp,
                testTag = "home_settings_button",
                onClick = onOpenSettings
            )
        }

        // Expandable 1-Tap Self Status Switcher (Online / Away / Busy) synced live via Supabase Realtime
        AnimatedVisibility(
            visible = showStatusPicker,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 2.dp, start = 2.dp, end = 2.dp)
                    .claymorphic(
                        cornerRadius = 20.dp,
                        surfaceColor = ClayVanillaLight,
                        elevation = 8.dp,
                        ledgeDepth = 3.5.dp
                    )
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .testTag("presence_status_picker_bar"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    PresenceAvailability.ONLINE,
                    PresenceAvailability.AWAY,
                    PresenceAvailability.BUSY
                ).forEach { option ->
                    val selected = selfAvailability == option
                    val pillColor = when {
                        selected && option == PresenceAvailability.ONLINE -> ClaySage
                        selected && option == PresenceAvailability.AWAY -> ClayHoney
                        selected && option == PresenceAvailability.BUSY -> ClayCoral
                        else -> ClayPeach
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .claymorphic(
                                cornerRadius = 15.dp,
                                surfaceColor = pillColor,
                                elevation = if (selected) 7.dp else 4.dp,
                                ledgeDepth = if (selected) 3.dp else 2.dp
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectPresenceStatus(option)
                                showStatusPicker = false
                            }
                            .padding(horizontal = 8.dp, vertical = 7.dp)
                            .testTag("set_status_${option.name.lowercase()}"),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (selected) ClayTextCream else option.statusColor())
                                .border(1.dp, Color.White, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        if (selected) {
                            Text3D(
                                text = option.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.5.sp
                                ),
                                depthDp = 1.dp
                            )
                        } else {
                            ClayTextOnLight(
                                text = option.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.5.sp
                                ),
                                textColor = ClayDeepTealInk,
                                depthDp = 0.6.dp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Full-Size Authentic Vintage 3D Clay Portable Radio with Rotary Dial Puzzle
        AuthenticRetroClayRadio(
            selfMember = selfMember,
            isOnline = selfIsOnline,
            isRecording = isWalkieRecording,
            reducedMotion = a11y.reducedMotion,
            onPressStart = {
                val hasPerm = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasPerm) {
                    audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    false
                } else {
                    onWalkiePressStart()
                    true
                }
            },
            onPressRelease = onWalkiePressRelease,
            modifier = Modifier
                .widthIn(max = 450.dp)
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 2.dp)
        )
    }
}

@Composable
private fun AuthenticRetroClayRadio(
    selfMember: FamilyMemberId,
    isOnline: Boolean,
    isRecording: Boolean,
    reducedMotion: Boolean,
    onPressStart: () -> Boolean,
    onPressRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Radio Mechanical Puzzle State
    var puzzleModeEnabled by remember { mutableStateOf(true) }
    var isDialUnlocked by remember { mutableStateOf(false) }
    var isSpinningAuto by remember { mutableStateOf(false) }
    var justSentBurst by remember { mutableStateOf(false) }
    var channelIndex by remember { mutableIntStateOf(0) }
    var knobAngleTarget by remember { mutableFloatStateOf(0f) }
    var elapsedSec by remember { mutableIntStateOf(0) }

    val rotaryAngle = remember { Animatable(0f) }
    val lockedShake = remember { Animatable(0f) }

    val channelPresets = remember {
        listOf(
            Triple("103.5", "FAMILY FM", 0.72f),
            Triple("96.8", "ZAYN CLUB", 0.44f),
            Triple("106.2", "SUPER HERO", 0.86f)
        )
    }
    val activePreset = channelPresets[channelIndex % channelPresets.size]
    val effectiveUnlocked = isDialUnlocked || !puzzleModeEnabled

    // Live timer while transmitting
    LaunchedEffect(isRecording) {
        if (isRecording) {
            justSentBurst = false
            elapsedSec = 0
            while (true) {
                delay(1000L)
                elapsedSec += 1
            }
        } else {
            elapsedSec = 0
        }
    }

    // Gentle auto-reset back to Rotary Puzzle 3.2 seconds after sending a clip
    LaunchedEffect(justSentBurst, puzzleModeEnabled, isRecording) {
        if (justSentBurst && puzzleModeEnabled && !isRecording) {
            delay(3200L)
            justSentBurst = false
            isDialUnlocked = false
        }
    }

    // Smooth ambient animations
    val infiniteTransition = rememberInfiniteTransition(label = "retro_radio_anim")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 760, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radio_wave"
    )
    val hintPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dial_hint_pulse"
    )
    val radioWaveRing by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "broadcast_ring"
    )

    // Analog Tuning Needle position (sweeps live as the child rotates the landline wheel!)
    val spinFraction = (rotaryAngle.value / 120f).coerceIn(0f, 1f)
    val targetNeedlePos = when {
        effectiveUnlocked -> activePreset.third
        spinFraction > 0.02f -> 0.14f + spinFraction * (activePreset.third - 0.14f)
        else -> 0.14f
    }
    val animatedNeedlePos by animateFloatAsState(
        targetValue = targetNeedlePos,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 260f),
        label = "analog_needle_pos"
    )
    val liveMhz = 88.0f + animatedNeedlePos * 20.0f

    // Real 3D mechanical plunger elevation height (pops UP when unlocked, sinks DEEP when pressed!)
    val plungerLiftDp by animateDpAsState(
        targetValue = when {
            isRecording -> 2.dp
            effectiveUnlocked -> 13.dp
            else -> 3.dp
        },
        animationSpec = spring(dampingRatio = 0.46f, stiffness = 420f),
        label = "plunger_3d_lift"
    )
    val plungerScale by animateFloatAsState(
        targetValue = when {
            isRecording && !reducedMotion -> 0.94f
            effectiveUnlocked && !reducedMotion -> 1.04f
            else -> 0.96f
        },
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 420f),
        label = "plunger_scale"
    )
    val knobAngle by animateFloatAsState(
        targetValue = knobAngleTarget,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 360f),
        label = "channel_knob_angle"
    )

    // Helper: Run a joyful animated rotary spin (for 6-year-old tap-assist!)
    fun triggerGuidedRotarySpin() {
        if (isSpinningAuto || effectiveUnlocked) return
        scope.launch {
            isSpinningAuto = true
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            // Spin clockwise to finger stop with tactile clicks
            val steps = 5
            for (s in 1..steps) {
                rotaryAngle.animateTo(
                    targetValue = (s / steps.toFloat()) * 124f,
                    animationSpec = tween(durationMillis = 72, easing = LinearEasing)
                )
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            // Unlock right as it hits the golden finger stop!
            isDialUnlocked = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            // Realistic vintage landline spring-recoil back to 0°!
            rotaryAngle.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.58f,
                    stiffness = 140f
                )
            )
            isSpinningAuto = false
        }
    }

    // Helper: Playful 3D locked wiggle if the child presses the center button before spinning
    fun triggerLockedWiggle() {
        scope.launch {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lockedShake.snapTo(0f)
            lockedShake.animateTo(11f, tween(55))
            lockedShake.animateTo(-11f, tween(70))
            lockedShake.animateTo(7f, tween(60))
            lockedShake.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 500f))
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // =========================================================================
        // 1. AUTHENTIC TOP HARDWARE: Telescopic Angled Radio Antenna + Carry Handle
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(horizontal = 24.dp)
        ) {
            // Angled Telescopic Retro Whip Antenna + Animated Broadcast Ripples on Left
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(width = 110.dp, height = 42.dp)
            ) {
                val baseX = 24.dp.toPx()
                val baseY = size.height
                val tipX = 76.dp.toPx()
                val tipY = 8.dp.toPx()

                // Radio broadcast arcs radiating from the antenna tip when talking or unlocked!
                if ((isRecording || effectiveUnlocked) && !reducedMotion) {
                    val arcColor = if (isRecording) ClayCoral else ClayHoneyLight
                    listOf(radioWaveRing, (radioWaveRing + 0.5f) % 1f).forEach { p ->
                        val r = (8.dp.toPx() + p * 22.dp.toPx())
                        drawArc(
                            color = arcColor.copy(alpha = (1f - p) * 0.85f),
                            startAngle = -75f,
                            sweepAngle = 110f,
                            useCenter = false,
                            topLeft = Offset(tipX - r, tipY - r),
                            size = Size(r * 2f, r * 2f),
                            style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                // Telescopic rod shadow
                drawLine(
                    color = ClayTealShadow.copy(alpha = 0.32f),
                    start = Offset(baseX + 2f, baseY + 1f),
                    end = Offset(tipX + 2f, tipY + 3f),
                    strokeWidth = 7.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Main sculpted Turquoise-Aqua telescopic rod
                drawLine(
                    brush = Brush.linearGradient(
                        listOf(ClayAquaDeep, ClayAquaLight, ClayAqua),
                        start = Offset(baseX, baseY),
                        end = Offset(tipX, tipY)
                    ),
                    start = Offset(baseX, baseY),
                    end = Offset(tipX, tipY),
                    strokeWidth = 6.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Glossy top-edge highlight on rod
                drawLine(
                    color = Color.White.copy(alpha = 0.65f),
                    start = Offset(baseX, baseY - 1.5f),
                    end = Offset(tipX - 3f, tipY - 1.5f),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Molded antenna base collar
                drawRoundRect(
                    color = ClayAquaDeep,
                    topLeft = Offset(baseX - 9.dp.toPx(), baseY - 8.dp.toPx()),
                    size = Size(18.dp.toPx(), 10.dp.toPx()),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )
                // Glowing Coral/Honey Bead Tip
                drawCircle(
                    color = ClayTealShadow.copy(alpha = 0.35f),
                    radius = 7.5.dp.toPx(),
                    center = Offset(tipX, tipY + 2f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = if (isRecording) {
                            listOf(ClayHoneyLight, ClayCoral, ClayCoralDeep)
                        } else {
                            listOf(ClayCoralLight, ClayCoral, ClayCoralDeep)
                        },
                        center = Offset(tipX - 2.dp.toPx(), tipY - 2.dp.toPx()),
                        radius = 9.dp.toPx()
                    ),
                    radius = 7.5.dp.toPx(),
                    center = Offset(tipX, tipY)
                )
            }

            // Sculpted Portable Radio Carrying Arch Handle (centered/right like classic Braun/Smeg radio)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(176.dp)
                    .height(26.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val archStroke = 10.dp.toPx()
                    val pad = archStroke / 2f + 2.dp.toPx()
                    val path = Path().apply {
                        moveTo(pad, size.height)
                        lineTo(pad, 11.dp.toPx())
                        quadraticTo(pad, 2.dp.toPx(), pad + 14.dp.toPx(), 2.dp.toPx())
                        lineTo(size.width - pad - 14.dp.toPx(), 2.dp.toPx())
                        quadraticTo(size.width - pad, 2.dp.toPx(), size.width - pad, 11.dp.toPx())
                        lineTo(size.width - pad, size.height)
                    }
                    // Handle drop shadow
                    drawPath(
                        path = path,
                        color = ClayTealShadow.copy(alpha = 0.28f),
                        style = Stroke(width = archStroke + 2f, cap = StrokeCap.Round)
                    )
                    // Main Warm Honey-Gold Matte Clay Carrying Handle
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            listOf(ClayHoneyLight, ClayHoney, ClayHoneyDeep)
                        ),
                        style = Stroke(width = archStroke, cap = StrokeCap.Round)
                    )
                    // Specular clay rim along top of handle
                    drawLine(
                        color = Color.White.copy(alpha = 0.65f),
                        start = Offset(pad + 14.dp.toPx(), 0.dp.toPx()),
                        end = Offset(size.width - pad - 14.dp.toPx(), 0.dp.toPx()),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // =========================================================================
        // 2. AUTHENTIC RETRO 3D MATTE CLAY RADIO CABINET CHASSIS
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .claymorphic(
                    cornerRadius = 34.dp,
                    surfaceColor = ClayPeach,
                    elevation = 18.dp,
                    shadowTint = ClayTealShadow,
                    ledgeDepth = 9.dp
                )
        ) {
            // Subtle molded vintage cabinet frame trim & 4 corner clay rivets
            Canvas(modifier = Modifier.fillMaxSize()) {
                val inset = 9.dp.toPx()
                drawRoundRect(
                    color = ClayPeachDeep.copy(alpha = 0.36f),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2 - 4.dp.toPx()),
                    cornerRadius = CornerRadius(26.dp.toPx(), 26.dp.toPx()),
                    style = Stroke(width = 1.6.dp.toPx())
                )
                // 4 charming molded brass-honey corner rivets
                val rivetInset = 18.dp.toPx()
                val corners = listOf(
                    Offset(rivetInset, rivetInset),
                    Offset(size.width - rivetInset, rivetInset),
                    Offset(rivetInset, size.height - rivetInset - 4.dp.toPx()),
                    Offset(size.width - rivetInset, size.height - rivetInset - 4.dp.toPx())
                )
                corners.forEach { c ->
                    drawCircle(color = ClayPeachDeep.copy(alpha = 0.65f), radius = 3.2.dp.toPx(), center = c)
                    drawCircle(color = ClayVanillaLight, radius = 1.4.dp.toPx(), center = Offset(c.x - 0.8f, c.y - 0.8f))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // -----------------------------------------------------------------
                // A. UPPER HALF: VINTAGE PERFORATED SPEAKER GRILLE + NAMEPLATE
                // -----------------------------------------------------------------
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Top LED jewels + Classic Chrome-Clay Nameplate
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioJewelLed(
                                label = "ON AIR",
                                isLit = isRecording,
                                litColor = ClayCoral,
                                unlitColor = Color(0xFFC4D7E8)
                            )
                            RadioJewelLed(
                                label = if (effectiveUnlocked) "READY" else "LOCKED",
                                isLit = effectiveUnlocked,
                                litColor = EmeraldAccept,
                                unlitColor = ClayHoney
                            )
                        }

                        // Vintage Molded Radio Crest Plate
                        Box(
                            modifier = Modifier
                                .claymorphic(
                                    cornerRadius = 14.dp,
                                    surfaceColor = ClayAqua,
                                    elevation = 5.dp,
                                    ledgeDepth = 2.5.dp
                                )
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text3D(
                                text = "HOLY MOLY • FM",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp,
                                    fontSize = 10.5.sp
                                ),
                                depthDp = 1.dp
                            )
                        }
                    }

                    // Authentic Perforated Dot-Matrix Vintage Speaker Chamber!
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .pastelRecessedWell(
                                cornerRadius = 20.dp,
                                surfaceColor = Color(0xFF14324A),
                                shadowTint = ClayDeepTealInk
                            )
                            .border(1.8.dp, ClayVanillaLight.copy(alpha = 0.82f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cols = 19
                            val rows = 4
                            val dx = size.width / cols
                            val dy = size.height / rows
                            val dotR = 2.6.dp.toPx()

                            for (r in 0 until rows) {
                                for (c in 0 until cols) {
                                    // Taper corners like a real retro oval/capsule speaker grille
                                    if ((r == 0 || r == rows - 1) && (c == 0 || c == cols - 1)) continue
                                    val cx = c * dx + dx / 2f
                                    val cy = r * dy + dy / 2f

                                    // When broadcasting, warm acoustic energy pulses behind the speaker holes!
                                    val waveEnergy = if (isRecording) {
                                        ((sin(wavePhase + c * 0.55f) + 1f) * 0.5f)
                                    } else 0f

                                    val holeColor = if (isRecording && waveEnergy > 0.45f) {
                                        ClayHoneyLight.copy(alpha = 0.55f + waveEnergy * 0.45f)
                                    } else {
                                        Color(0xFF0B2033)
                                    }

                                    // Sunlit bottom-right bevel of each speaker perforation
                                    drawCircle(
                                        color = ClayHoneyLight.copy(alpha = 0.22f),
                                        radius = dotR,
                                        center = Offset(cx + 0.8f, cy + 1f)
                                    )
                                    drawCircle(
                                        color = holeColor,
                                        radius = dotR,
                                        center = Offset(cx, cy)
                                    )
                                }
                            }
                        }
                    }

                    // -----------------------------------------------------------------
                    // B. ILLUMINATED ANALOG RADIO GLASS TUNING DIAL + LIVE NEEDLE!
                    // -----------------------------------------------------------------
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                            .claymorphic(
                                cornerRadius = 20.dp,
                                surfaceColor = ClayVanillaLight,
                                elevation = 8.dp,
                                shadowTint = ClayTealShadow,
                                ledgeDepth = 4.dp
                            )
                            .border(2.2.dp, ClayAqua, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top row inside glass dial: Live MHz Frequency + Preset Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    ClayTextOnLight(
                                        text = if (effectiveUnlocked) "${activePreset.first} FM" else "%.1f FM".format(liveMhz),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp
                                        ),
                                        textColor = if (isRecording) ClayCoralDeep else ClayDeepTealInk,
                                        depthDp = 0.8.dp
                                    )
                                    ClayTextOnLight(
                                        text = "•",
                                        style = MaterialTheme.typography.labelMedium,
                                        textColor = ClayMutedInk,
                                        depthDp = 0.dp
                                    )
                                    ClayTextOnLight(
                                        text = when {
                                            isRecording -> "ON AIR %02d:%02d".format(elapsedSec / 60, elapsedSec % 60)
                                            justSentBurst -> "SENT TO FAMILY!"
                                            effectiveUnlocked -> activePreset.second
                                            else -> "SPIN DIAL TO TUNE"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp
                                        ),
                                        textColor = when {
                                            isRecording -> ClayCoralDeep
                                            effectiveUnlocked -> ClaySageDeep
                                            else -> ClayHoneyDeep
                                        },
                                        depthDp = 0.6.dp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                ClayTextOnLight(
                                    text = if (isOnline) selfMember.title.uppercase() else "OFFLINE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp
                                    ),
                                    textColor = ClayAquaDeep,
                                    depthDp = 0.5.dp
                                )
                            }

                            // Authentic Vintage Analog Scale Ticks + Glowing Family Sweet-Spot + Gliding Coral Needle!
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp)
                            ) {
                                val trackY = size.height * 0.62f
                                val padX = 6.dp.toPx()
                                val usableW = (size.width - padX * 2).coerceAtLeast(1f)

                                // Warm amber glass track line
                                drawLine(
                                    color = ClayPeachDeep.copy(alpha = 0.5f),
                                    start = Offset(padX, trackY),
                                    end = Offset(size.width - padX, trackY),
                                    strokeWidth = 2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )

                                // Glowing Sweet-Spot Target Zone on the FM scale
                                val targetX = padX + activePreset.third * usableW
                                drawRoundRect(
                                    color = if (effectiveUnlocked) ClaySage.copy(alpha = 0.35f) else ClayHoney.copy(alpha = 0.32f),
                                    topLeft = Offset(targetX - 16.dp.toPx(), 2.dp.toPx()),
                                    size = Size(32.dp.toPx(), size.height - 4.dp.toPx()),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                )

                                // Precision vintage radio frequency ticks (major & minor)
                                val ticks = 28
                                for (i in 0..ticks) {
                                    val tx = padX + (i / ticks.toFloat()) * usableW
                                    val isMajor = i % 4 == 0
                                    val tickTop = if (isMajor) 3.dp.toPx() else 9.dp.toPx()
                                    val tickBottom = trackY
                                    drawLine(
                                        color = if (isMajor) ClayDeepTealInk.copy(alpha = 0.72f) else ClayMutedInk.copy(alpha = 0.45f),
                                        start = Offset(tx, tickTop),
                                        end = Offset(tx, tickBottom),
                                        strokeWidth = if (isMajor) 2.dp.toPx() else 1.2.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }

                                // Live VU pulse bars right along the bottom when broadcasting
                                if (isRecording) {
                                    for (i in 0..16) {
                                        val bx = padX + (i / 16f) * usableW
                                        val amp = (sin(wavePhase + i * 0.8f) + 1f) * 0.5f
                                        drawCircle(
                                            color = ClayCoral.copy(alpha = 0.35f + amp * 0.65f),
                                            radius = (1.6f + amp * 2.2f).dp.toPx(),
                                            center = Offset(bx, size.height - 3.dp.toPx())
                                        )
                                    }
                                }

                                // Gliding Watermelon-Coral Analog Tuning Needle!
                                val needleX = padX + animatedNeedlePos * usableW
                                // Needle shadow
                                drawLine(
                                    color = ClayTealShadow.copy(alpha = 0.3f),
                                    start = Offset(needleX + 1.5f, 1.dp.toPx()),
                                    end = Offset(needleX + 1.5f, size.height - 1.dp.toPx()),
                                    strokeWidth = 3.5.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                // Crisp Coral Needle
                                drawLine(
                                    color = ClayCoral,
                                    start = Offset(needleX, 1.dp.toPx()),
                                    end = Offset(needleX, size.height - 1.dp.toPx()),
                                    strokeWidth = 3.2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                // Needle top ruby cap
                                drawCircle(
                                    color = ClayCoralDeep,
                                    radius = 4.dp.toPx(),
                                    center = Offset(needleX, 4.dp.toPx())
                                )
                            }
                        }
                    }
                }

                // -----------------------------------------------------------------
                // C. HERO INTERACTIVE CENTERPIECE:
                //    VINTAGE LANDLINE ROTARY DIAL PUZZLE + 3D POP-UP PTT PLUNGER!
                // -----------------------------------------------------------------
                Box(
                    modifier = Modifier.size(234.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Broadcast Expanding Energy Rings when Talking
                    if (isRecording && !reducedMotion) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val maxR = size.minDimension / 2f
                            listOf(radioWaveRing, (radioWaveRing + 0.5f) % 1f).forEach { p ->
                                drawCircle(
                                    color = ClayHoney.copy(alpha = (1f - p) * 0.72f),
                                    radius = maxR * (0.64f + p * 0.36f),
                                    style = Stroke(width = 4.dp.toPx())
                                )
                            }
                        }
                    }

                    // Recessed Vintage Rotary Dial Well + Outer Progress Ring
                    Box(
                        modifier = Modifier
                            .size(226.dp)
                            .pastelRecessedWell(
                                cornerRadius = 113.dp,
                                surfaceColor = Color(0xFFF2D6BE),
                                shadowTint = ClayTealShadow
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer Spin-Progress Halo Arc
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeW = 5.dp.toPx()
                            val inset = 6.dp.toPx()
                            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                            if (effectiveUnlocked) {
                                drawArc(
                                    color = ClaySage.copy(alpha = 0.85f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = Offset(inset, inset),
                                    size = arcSize,
                                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                )
                            } else if (spinFraction > 0.01f) {
                                drawArc(
                                    color = ClayHoney,
                                    startAngle = -140f,
                                    sweepAngle = spinFraction * 260f,
                                    useCenter = false,
                                    topLeft = Offset(inset, inset),
                                    size = arcSize,
                                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                )
                            }
                        }

                        // ROTATING 3D LANDLINE FINGER WHEEL (Drag Clockwise OR Tap Golden Hole!)
                        // Pointer tracking lives on the NON-ROTATING outer Box so drag angles stay rock-solid 1:1!
                        var lastTouchAngle by remember { mutableFloatStateOf(0f) }
                        var lastClickBucket by remember { mutableIntStateOf(0) }

                        Box(
                            modifier = Modifier
                                .size(214.dp)
                                .pointerInput(effectiveUnlocked, isSpinningAuto) {
                                    if (effectiveUnlocked || isSpinningAuto) return@pointerInput
                                    detectTapGestures(
                                        onTap = {
                                            // Kid-friendly 1-tap spin assist for 6-year-olds!
                                            triggerGuidedRotarySpin()
                                        }
                                    )
                                }
                                .pointerInput(effectiveUnlocked, isSpinningAuto) {
                                    if (effectiveUnlocked || isSpinningAuto) return@pointerInput
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val cx = size.width / 2f
                                            val cy = size.height / 2f
                                            lastTouchAngle = (atan2(offset.y - cy, offset.x - cx) * 180f / PI.toFloat())
                                            lastClickBucket = floor(rotaryAngle.value / 24f).toInt()
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            val cx = size.width / 2f
                                            val cy = size.height / 2f
                                            val curAngle = (atan2(change.position.y - cy, change.position.x - cx) * 180f / PI.toFloat())
                                            var delta = curAngle - lastTouchAngle
                                            while (delta > 180f) delta -= 360f
                                            while (delta < -180f) delta += 360f
                                            lastTouchAngle = curAngle

                                            val next = (rotaryAngle.value + delta).coerceIn(0f, 140f)
                                            scope.launch { rotaryAngle.snapTo(next) }

                                            val bucket = floor(next / 24f).toInt()
                                            if (bucket != lastClickBucket) {
                                                lastClickBucket = bucket
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        },
                                        onDragEnd = {
                                            scope.launch {
                                                val reached = rotaryAngle.value >= 72f
                                                if (reached) {
                                                    isDialUnlocked = true
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                                // Authentic spring-loaded landline rotary dial recoil!
                                                rotaryAngle.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = 0.56f,
                                                        stiffness = 135f
                                                    )
                                                )
                                            }
                                        },
                                        onDragCancel = {
                                            scope.launch {
                                                rotaryAngle.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 160f)
                                                )
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        rotationZ = rotaryAngle.value
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                            // Sculpted 3D Matte Clay Rotary Finger Wheel Canvas
                            val holeLabels = listOf("★", "2", "3", "4", "5", "6")
                            val holeColors = listOf(ClayHoney, ClayAqua, ClayCoral, ClaySage, ClayAqua, ClayCoral)

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val r = size.minDimension / 2f
                                val center = Offset(size.width / 2f, size.height / 2f)

                                // 3D bottom extruded clay lip of the rotary wheel
                                drawCircle(
                                    color = ClayVanillaDeep,
                                    radius = r - 1.dp.toPx(),
                                    center = Offset(center.x, center.y + 4.dp.toPx())
                                )
                                // Main Vanilla-Cream sculpted clay rotary disc
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(ClayVanillaLight, ClayVanilla, Color(0xFFFFEFE2)),
                                        center = Offset(center.x - r * 0.25f, center.y - r * 0.25f),
                                        radius = r * 1.45f
                                    ),
                                    radius = r - 2.dp.toPx(),
                                    center = center
                                )
                                // Crisp sunlit upper-left bevel ring
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.85f),
                                    radius = r - 3.dp.toPx(),
                                    center = center,
                                    style = Stroke(width = 2.dp.toPx())
                                )

                                // 6 Authentic Recessed Landline Finger Holes arranged around the wheel!
                                // Angles from -130° clockwise to +10° (leaving room on right for the Finger Stop)
                                val startDeg = -135f
                                val stepDeg = 38f
                                val ringRadius = r * 0.74f
                                val holeRadius = 17.5.dp.toPx()

                                for (i in 0 until 6) {
                                    val deg = startDeg + i * stepDeg
                                    val rad = deg * (PI / 180.0)
                                    val hx = center.x + (cos(rad) * ringRadius).toFloat()
                                    val hy = center.y + (sin(rad) * ringRadius).toFloat()
                                    val isTargetStar = i == 0 && !effectiveUnlocked

                                    // Pulsing Honey-Gold target halo on hole #1 so a 6-year-old spots it instantly!
                                    if (isTargetStar) {
                                        drawCircle(
                                            color = ClayHoney.copy(alpha = 0.35f + hintPulse * 0.5f),
                                            radius = holeRadius + (3.dp.toPx() + hintPulse * 3.dp.toPx()),
                                            center = Offset(hx, hy)
                                        )
                                    }

                                    // Deep recessed inner finger-hole shadow
                                    drawCircle(
                                        color = ClayDeepTealInk.copy(alpha = 0.28f),
                                        radius = holeRadius,
                                        center = Offset(hx, hy)
                                    )
                                    // Colored glazed ceramic-clay bottom inside each finger hole
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = if (isTargetStar) {
                                                listOf(ClayHoneyLight, ClayHoney)
                                            } else {
                                                listOf(holeColors[i].copy(alpha = 0.85f), holeColors[i])
                                            },
                                            center = Offset(hx + 2.dp.toPx(), hy + 3.dp.toPx()),
                                            radius = holeRadius * 1.1f
                                        ),
                                        radius = holeRadius - 2.2.dp.toPx(),
                                        center = Offset(hx, hy + 1.dp.toPx())
                                    )
                                    // Recessed rim lip highlight
                                    drawCircle(
                                        color = Color.White.copy(alpha = if (isTargetStar) 0.95f else 0.72f),
                                        radius = holeRadius,
                                        center = Offset(hx, hy),
                                        style = Stroke(width = if (isTargetStar) 2.2.dp.toPx() else 1.4.dp.toPx())
                                    )
                                }

                                // Curved clockwise hint arrow on the wheel when locked
                                if (!effectiveUnlocked && spinFraction < 0.1f) {
                                    drawArc(
                                        color = ClayHoneyDeep.copy(alpha = 0.45f + hintPulse * 0.4f),
                                        startAngle = -105f,
                                        sweepAngle = 54f,
                                        useCenter = false,
                                        topLeft = Offset(center.x - r * 0.92f, center.y - r * 0.92f),
                                        size = Size(r * 1.84f, r * 1.84f),
                                        style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                            }

                            // Crisp 3D Number/Star labels inside the 6 finger holes
                            val startDeg = -135.0
                            val stepDeg = 38.0
                            val ringOffsetDp = 79.dp
                            holeLabels.forEachIndexed { idx, lbl ->
                                val rad = (startDeg + idx * stepDeg) * (PI / 180.0)
                                val ox = (cos(rad) * ringOffsetDp.value).dp
                                val oy = (sin(rad) * ringOffsetDp.value).dp
                                Text3D(
                                    text = lbl,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = if (idx == 0) 16.sp else 13.5.sp
                                    ),
                                    depthDp = 1.dp,
                                    modifier = Modifier.offset(x = ox, y = oy)
                                )
                            }
                            }
                        }

                        // FIXED 3D HONEY-GOLD FINGER-STOP BRACKET (Authentic vintage landline stop at 4 o'clock!)
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .offset(x = (-2).dp, y = 46.dp)
                                .graphicsLayer { rotationZ = 24f }
                                .width(34.dp)
                                .height(15.dp)
                                .shadow(6.dp, RoundedCornerShape(8.dp), ambientColor = ClayTealShadow, spotColor = ClayTealShadow)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.verticalGradient(listOf(ClayHoneyLight, ClayHoney, ClayHoneyDeep)))
                                .border(1.4.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                        )

                        // =========================================================================
                        // REAL 3D PHYSICAL POP-UP / PUSH-DOWN HOLD-TO-TALK PLUNGER BUTTON!
                        // =========================================================================
                        Box(
                            modifier = Modifier
                                .size(116.dp)
                                .graphicsLayer {
                                    rotationZ = lockedShake.value
                                    scaleX = plungerScale
                                    scaleY = plungerScale
                                }
                                .testTag("walkie_talkie_hold_button")
                                .pointerInput(effectiveUnlocked) {
                                    detectTapGestures(
                                        onPress = {
                                            if (!effectiveUnlocked) {
                                                // Playful 3D wiggle + gently invite child to spin the golden hole!
                                                triggerLockedWiggle()
                                                return@detectTapGestures
                                            }
                                            val started = onPressStart()
                                            if (!started) return@detectTapGestures
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            try {
                                                tryAwaitRelease()
                                            } finally {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onPressRelease()
                                                if (puzzleModeEnabled) {
                                                    justSentBurst = true
                                                }
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Deep recessed center plunger socket
                            Box(
                                modifier = Modifier
                                    .size(114.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (effectiveUnlocked) ClaySageDeep.copy(alpha = 0.25f)
                                        else ClayDeepTealInk.copy(alpha = 0.2f)
                                    )
                                    .border(
                                        width = if (effectiveUnlocked) 3.dp else 2.dp,
                                        color = if (isRecording) ClayHoney else if (effectiveUnlocked) ClaySage else ClayPeachDeep,
                                        shape = CircleShape
                                    )
                            )

                            // REAL 3D EXTRUDED CYLINDER BARREL + MOVING DOME CAP!
                            // As plungerLiftDp changes (3dp locked -> 13dp popped UP -> 2dp pressed DOWN),
                            // the cylinder side wall visibly extends and collapses in real 3D!
                            val capTopColor = when {
                                isRecording -> ClayHoneyLight
                                effectiveUnlocked -> ClayCoralLight
                                else -> ClayAquaLight
                            }
                            val capMidColor = when {
                                isRecording -> ClayHoney
                                effectiveUnlocked -> ClayCoral
                                else -> ClayAqua
                            }
                            val barrelDeepColor = when {
                                isRecording -> ClayHoneyDeep
                                effectiveUnlocked -> ClayCoralDeep
                                else -> ClayAquaDeep
                            }

                            // 3D Cylindrical Side Wall + Dynamic Cast Shadow
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val capR = 48.dp.toPx()
                                val liftPx = plungerLiftDp.toPx()
                                val baseCenter = Offset(size.width / 2f, size.height / 2f + 4.dp.toPx())
                                val capCenter = Offset(size.width / 2f, baseCenter.y - liftPx)

                                // Soft cast shadow projected onto the radio socket beneath the popped-up plunger
                                drawCircle(
                                    color = ClayTealShadow.copy(alpha = 0.34f),
                                    radius = capR + 2.dp.toPx(),
                                    center = Offset(baseCenter.x, baseCenter.y + 3.dp.toPx())
                                )

                                // Sculpted 3D Vertical Cylinder Barrel (stacking discs from base up to cap!)
                                val steps = 10
                                for (s in 0..steps) {
                                    val t = s / steps.toFloat()
                                    val y = baseCenter.y - t * liftPx
                                    drawCircle(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                barrelDeepColor,
                                                capMidColor,
                                                barrelDeepColor
                                            ),
                                            startX = baseCenter.x - capR,
                                            endX = baseCenter.x + capR
                                        ),
                                        radius = capR,
                                        center = Offset(baseCenter.x, y)
                                    )
                                }

                                // Top Sculpted Matte Clay Dome Cap
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(capTopColor, capMidColor, barrelDeepColor.copy(alpha = 0.92f)),
                                        center = Offset(capCenter.x - capR * 0.32f, capCenter.y - capR * 0.34f),
                                        radius = capR * 1.55f
                                    ),
                                    radius = capR,
                                    center = capCenter
                                )
                                // Crisp sunlit upper clay rim on the button cap
                                drawCircle(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.88f),
                                            Color.Transparent,
                                            barrelDeepColor.copy(alpha = 0.55f)
                                        ),
                                        start = Offset(capCenter.x - capR, capCenter.y - capR),
                                        end = Offset(capCenter.x + capR, capCenter.y + capR)
                                    ),
                                    radius = capR - 1.5.dp.toPx(),
                                    center = capCenter,
                                    style = Stroke(width = 2.4.dp.toPx())
                                )
                            }

                            // Center Plunger Icon + 3D Text (rides up & down WITH the physical 3D plunger cap!)
                            Column(
                                modifier = Modifier.offset(y = 4.dp - plungerLiftDp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Icon(
                                    imageVector = when {
                                        isRecording -> Icons.Rounded.GraphicEq
                                        effectiveUnlocked -> Icons.Rounded.Mic
                                        else -> Icons.Rounded.Lock
                                    },
                                    contentDescription = if (effectiveUnlocked) "Hold to talk" else "Spin rotary dial to unlock",
                                    tint = ClayTextCream,
                                    modifier = Modifier.size(if (effectiveUnlocked) 30.dp else 26.dp)
                                )
                                Text3D(
                                    text = when {
                                        isRecording -> "TALKING!"
                                        effectiveUnlocked -> "HOLD"
                                        else -> "SPIN"
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    ),
                                    extrudeColor = Color(0x664A1D18),
                                    depthDp = 1.6.dp
                                )
                                Text3D(
                                    text = when {
                                        isRecording -> "Release"
                                        effectiveUnlocked -> "TO TALK"
                                        else -> "DIAL ★"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 9.5.sp
                                    ),
                                    extrudeColor = Color(0x554A1D18),
                                    depthDp = 1.dp
                                )
                            }
                        }
                    }
                }

                // -----------------------------------------------------------------
                // D. LOWER AUTHENTIC 3D RADIO HARDWARE DECK:
                //    Left 3D Push-Switch (PUZZLE / FREE) + Guide + Right 3D Rotary Knob (CH)
                // -----------------------------------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left 3D Mechanical Square Radio Switch: Toggle Rotary Puzzle / Free Talk
                    Radio3DMechanicalSwitch(
                        label = if (puzzleModeEnabled) "PUZZLE" else "FREE",
                        isPressedIn = !puzzleModeEnabled,
                        activeColor = if (puzzleModeEnabled) ClayHoney else ClaySage,
                        testTag = "radio_puzzle_mode_switch",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            puzzleModeEnabled = !puzzleModeEnabled
                            if (!puzzleModeEnabled) {
                                isDialUnlocked = true
                            } else {
                                isDialUnlocked = false
                            }
                        }
                    )

                    // Center Kid-Friendly Prompt Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .claymorphic(
                                cornerRadius = 16.dp,
                                surfaceColor = ClayVanilla,
                                elevation = 5.dp,
                                ledgeDepth = 3.dp
                            )
                            .clickable(enabled = !effectiveUnlocked) {
                                triggerGuidedRotarySpin()
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ClayTextOnLight(
                            text = when {
                                isRecording -> "Keep holding to talk!"
                                justSentBurst -> "Voice sent! ✨"
                                effectiveUnlocked -> "Hold red button & speak!"
                                else -> "Spin ★ around to unlock!"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.5.sp
                            ),
                            textColor = if (effectiveUnlocked) ClayDeepTealInk else ClayCoralDeep,
                            textAlign = TextAlign.Center,
                            depthDp = 0.7.dp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Right 3D Knurled Radio Channel Tuner Knob (Tap to Twist & Click!)
                    Radio3DKnurledKnob(
                        label = "CH 0${channelIndex + 1}",
                        rotationDeg = knobAngle,
                        testTag = "radio_channel_knob",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            knobAngleTarget += 45f
                            channelIndex = (channelIndex + 1) % channelPresets.size
                        }
                    )
                }
            }
        }

        // =========================================================================
        // 3. CUTE MOLDED CLAY RADIO FEET (anchors the retro radio silhouette!)
        // =========================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 42.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                        .background(Brush.verticalGradient(listOf(ClayAqua, ClayAquaDeep)))
                )
            }
        }
    }
}

@Composable
private fun RadioJewelLed(
    label: String,
    isLit: Boolean,
    litColor: Color,
    unlitColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(11.dp)
                .shadow(if (isLit) 6.dp else 1.dp, CircleShape, ambientColor = litColor, spotColor = litColor)
                .clip(CircleShape)
                .background(if (isLit) litColor else unlitColor)
                .border(1.4.dp, Color.White.copy(alpha = 0.85f), CircleShape)
        )
        ClayTextOnLight(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp
            ),
            textColor = if (isLit) ClayDeepTealInk else ClayMutedInk,
            depthDp = 0.6.dp
        )
    }
}

/**
 * Authentic 3D Mechanical Square Radio Push-Key with real physical Z-depression!
 */
@Composable
private fun Radio3DMechanicalSwitch(
    label: String,
    isPressedIn: Boolean,
    activeColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val liftDp by animateDpAsState(
        targetValue = if (isPressedIn) 1.5.dp else 6.dp,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 460f),
        label = "switch_lift"
    )
    Box(
        modifier = Modifier
            .width(68.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ClayPeachDeep.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(3.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .offset(y = -liftDp + 3.dp)
                .claymorphic(
                    cornerRadius = 11.dp,
                    surfaceColor = activeColor,
                    elevation = if (isPressedIn) 2.dp else 8.dp,
                    shadowTint = ClayTealShadow,
                    ledgeDepth = liftDp
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = ClayTextCream,
                    modifier = Modifier.size(12.dp)
                )
                Text3D(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp
                    ),
                    depthDp = 0.9.dp
                )
            }
        }
    }
}

/**
 * Authentic 3D Knurled Matte Clay Radio Tuning Knob that physically twists on tap!
 */
@Composable
private fun Radio3DKnurledKnob(
    label: String,
    rotationDeg: Float,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .graphicsLayer { rotationZ = rotationDeg },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                val c = Offset(size.width / 2f, size.height / 2f)
                // 3D base rim
                drawCircle(color = ClayAquaDeep, radius = r, center = Offset(c.x, c.y + 3.dp.toPx()))
                // Knurled notches around perimeter
                for (i in 0 until 12) {
                    val a = i * 30.0 * (PI / 180.0)
                    val nx = c.x + (cos(a) * (r - 2.dp.toPx())).toFloat()
                    val ny = c.y + (sin(a) * (r - 2.dp.toPx())).toFloat()
                    drawCircle(color = ClayAquaLight, radius = 2.2.dp.toPx(), center = Offset(nx, ny))
                }
                // Knob top face
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(ClayAquaLight, ClayAqua, ClayAquaDeep),
                        center = Offset(c.x - 4.dp.toPx(), c.y - 4.dp.toPx()),
                        radius = r * 1.4f
                    ),
                    radius = r - 3.dp.toPx(),
                    center = c
                )
                // Crisp Cream-White pointer notch (visibly rotates when tapped!)
                drawLine(
                    color = ClayTextCream,
                    start = Offset(c.x, c.y - 4.dp.toPx()),
                    end = Offset(c.x, c.y - r + 5.dp.toPx()),
                    strokeWidth = 3.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        ClayTextOnLight(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 9.5.sp
            ),
            textColor = ClayDeepTealInk,
            depthDp = 0.5.dp
        )
    }
}
