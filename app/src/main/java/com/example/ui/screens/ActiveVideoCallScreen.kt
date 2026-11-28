package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.SpeakerPhone
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.local.UserProfileEntity
import com.example.data.model.ArFilterType
import com.example.data.model.AudioRoute
import com.example.data.model.BadgeElement
import com.example.data.model.CallParticipantState
import com.example.data.model.CallPhase
import com.example.data.model.CallState
import com.example.data.model.FamilyMemberId
import com.example.data.model.LiveStroke
import com.example.data.model.haloColor
import com.example.media.LiveKitTokenProvider
import com.example.ui.components.EmojiPhysicsBurstOverlay
import com.example.ui.components.FamilyMemberAvatar
import com.example.ui.theme.CrimsonDecline
import com.example.ui.theme.ElectricRocket
import com.example.ui.theme.EmeraldAccept
import com.example.ui.theme.LocalKinNestAccessibility
import com.example.ui.theme.MintTreehouse
import com.example.ui.theme.PurpleChest
import com.example.ui.theme.WalkieAmber
import com.example.ui.theme.liquidGlass
import io.livekit.android.renderer.SurfaceViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack
import livekit.org.webrtc.RendererCommon

@Composable
fun ActiveVideoCallScreen(
    selfMember: FamilyMemberId,
    callState: CallState,
    profiles: List<UserProfileEntity>,
    liveKitRoom: Room?,
    isLiveStreamActive: Boolean,
    videoTracksByMember: Map<FamilyMemberId, VideoTrack>,
    connectedLiveMembers: Set<FamilyMemberId>,
    liveStrokes: List<LiveStroke>,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onFlipCamera: () -> Unit,
    onCycleAudioRoute: () -> Unit,
    onInviteMember: (FamilyMemberId) -> Unit,
    onSelectArFilter: (ArFilterType) -> Unit,
    onTriggerEmojiBurst: (String) -> Unit,
    onTriggerScreenTap: () -> Unit,
    onToggleDrawingOverlay: () -> Unit,
    onCommitDrawingStroke: (Color, Float, List<Pair<Float, Float>>) -> Unit,
    onMediaPermissionsChanged: (cameraGranted: Boolean, audioGranted: Boolean) -> Unit,
    onEndCall: () -> Unit
) {
    BackHandler { onEndCall() }

    val context = LocalContext.current
    val a11y = LocalKinNestAccessibility.current

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var pendingEnableCameraAfterPerm by remember { mutableStateOf(false) }
    var pendingUnmuteAfterPerm by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val camGranted = result[Manifest.permission.CAMERA] == true || hasCameraPerm
        val audGranted = result[Manifest.permission.RECORD_AUDIO] == true || hasAudioPerm
        hasCameraPerm = camGranted
        hasAudioPerm = audGranted
        onMediaPermissionsChanged(camGranted, audGranted)
        if (pendingEnableCameraAfterPerm) {
            pendingEnableCameraAfterPerm = false
            if (camGranted && !callState.isLocalCameraOn) {
                onToggleCamera()
            }
        }
        if (pendingUnmuteAfterPerm) {
            pendingUnmuteAfterPerm = false
            if (audGranted && callState.isLocalMicMuted) {
                onToggleMic()
            }
        }
    }

    // Request only the permissions required by the call mode (Voice Call = Mic only; Video Call = Camera + Mic)
    LaunchedEffect(callState.isLocalCameraOn) {
        val needed = buildList {
            if (!hasAudioPerm) add(Manifest.permission.RECORD_AUDIO)
            if (callState.isLocalCameraOn && !hasCameraPerm) add(Manifest.permission.CAMERA)
        }
        if (needed.isNotEmpty()) {
            permLauncher.launch(needed.toTypedArray())
        }
    }

    val participants = callState.participants.ifEmpty {
        listOf(
            CallParticipantState(
                memberId = selfMember,
                isConnected = true,
                isSpeaking = false,
                audioLevel = 0.2f,
                isMuted = callState.isLocalMicMuted,
                isCameraEnabled = callState.isLocalCameraOn
            )
        )
    }
    val missingThirdMember = FamilyMemberId.entries.firstOrNull { m ->
        participants.none { it.memberId == m }
    }

    val mins = callState.durationSeconds / 60
    val secs = callState.durationSeconds % 60
    val formattedDuration = "%02d:%02d".format(mins, secs)

    val anyVideoActive = callState.isLocalCameraOn || videoTracksByMember.isNotEmpty()
    val isVoiceStage = !anyVideoActive

    val (audioRouteIcon, audioRouteDesc) = when (callState.audioRoute) {
        AudioRoute.SPEAKER -> Icons.Rounded.SpeakerPhone to "Speaker"
        AudioRoute.BLUETOOTH -> Icons.Rounded.BluetoothAudio to "Bluetooth"
        AudioRoute.EARPIECE -> Icons.Rounded.Hearing to "Earpiece"
    }

    val stageBgBrush = if (isVoiceStage) {
        Brush.verticalGradient(
            listOf(Color(0xFF0E2936), Color(0xFF13394A), Color(0xFF0A1D28))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xFF070B16), Color(0xFF070B16))
        )
    }

    // Media Isolation: Deep dark unblurred stage for video feeds so hardware rendering stays cool
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(stageBgBrush)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Status Glass Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(24.dp),
                        surfaceAlpha = 0.25f,
                        tintColor = Color.White,
                        reducedMotion = a11y.reducedMotion
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (callState.phase is CallPhase.Reconnecting) WalkieAmber else EmeraldAccept
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when {
                                callState.phase is CallPhase.Reconnecting -> callState.phase.reason
                                isVoiceStage -> "Voice Call • $formattedDuration"
                                else -> "Video Call • $formattedDuration"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isLiveStreamActive) EmeraldAccept else WalkieAmber
                        ) {
                            Text(
                                text = if (isLiveStreamActive) "LIVE" else "RINGING",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "${callState.networkQualityLabel} • $audioRouteDesc",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.80f),
                        maxLines = 2
                    )
                }
                if (missingThirdMember != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ElectricRocket,
                        modifier = Modifier
                            .clickable { onInviteMember(missingThirdMember) }
                            .testTag("invite_third_member_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PersonAdd,
                                contentDescription = "Invite ${missingThirdMember.displayName}",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ ${missingThirdMember.displayName.substringBefore(" ")}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Main Stage: Seamless Live Voice Stage OR Unblurred 3-Way Split Video Grid
            if (isVoiceStage) {
                LiveVoiceCallStage(
                    selfMember = selfMember,
                    participants = participants,
                    profiles = profiles,
                    isLiveStreamActive = isLiveStreamActive,
                    connectedLiveMembers = connectedLiveMembers,
                    tapPulseMember = callState.screenTapPulseMember,
                    reducedMotion = a11y.reducedMotion,
                    onUpgradeToVideo = {
                        if (!hasCameraPerm) {
                            pendingEnableCameraAfterPerm = true
                            permLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                        } else {
                            onToggleCamera()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val allowOfflineCameraPreview = remember {
                        !LiveKitTokenProvider.isLiveKitUrlConfigured()
                    }
                    val primary = participants.first()
                    val primaryProfile = profiles.firstOrNull { it.memberId == primary.memberId.name }
                    key(primary.memberId) {
                        VideoParticipantTile(
                            participant = primary,
                            profile = primaryProfile,
                            isLocalSelf = primary.memberId == selfMember,
                            liveKitRoom = liveKitRoom,
                            liveVideoTrack = videoTracksByMember[primary.memberId],
                            isLiveStreamActive = isLiveStreamActive,
                            allowOfflineCameraPreview = allowOfflineCameraPreview && callState.phase is CallPhase.Active,
                            isMemberJoinedLive = primary.memberId in connectedLiveMembers,
                            hasCameraPerm = hasCameraPerm,
                            isCameraOn = if (primary.memberId == selfMember) callState.isLocalCameraOn else primary.isCameraEnabled,
                            isFrontCamera = callState.isFrontCamera,
                            tapPulseActive = callState.screenTapPulseMember == primary.memberId,
                            modifier = Modifier
                                .weight(1.25f)
                                .fillMaxWidth()
                        )
                    }

                    if (participants.size > 1) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            participants.drop(1).forEach { p ->
                                val prof = profiles.firstOrNull { it.memberId == p.memberId.name }
                                key(p.memberId) {
                                    VideoParticipantTile(
                                        participant = p,
                                        profile = prof,
                                        isLocalSelf = p.memberId == selfMember,
                                        liveKitRoom = liveKitRoom,
                                        liveVideoTrack = videoTracksByMember[p.memberId],
                                        isLiveStreamActive = isLiveStreamActive,
                                        allowOfflineCameraPreview = allowOfflineCameraPreview && callState.phase is CallPhase.Active,
                                        isMemberJoinedLive = p.memberId in connectedLiveMembers,
                                        hasCameraPerm = hasCameraPerm,
                                        isCameraOn = if (p.memberId == selfMember) callState.isLocalCameraOn else p.isCameraEnabled,
                                        isFrontCamera = callState.isFrontCamera,
                                        tapPulseActive = callState.screenTapPulseMember == p.memberId,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // In-Call Interactive Fun Strip: AR Filters (in video mode) + Emoji Physics Triggers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isVoiceStage) {
                    ArFilterType.entries.forEach { filter ->
                        val selected = callState.activeFilter == filter
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (selected) PurpleChest else Color.White.copy(alpha = 0.16f),
                            modifier = Modifier
                                .height(44.dp)
                                .clickable { onSelectArFilter(filter) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = filter.emoji, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                listOf("🚀", "💖", "😂", "🦕", "👋", "🎉").forEach { emoji ->
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { onTriggerEmojiBurst(emoji) }
                            .testTag("emoji_burst_$emoji")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }
            }

            // Lower-Third Clay Controls (48dp+ accessible targets for Zayn & Parents)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .liquidGlass(
                        shape = RoundedCornerShape(30.dp),
                        surfaceAlpha = 0.25f,
                        tintColor = Color.White,
                        reducedMotion = a11y.reducedMotion
                    )
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallClayControlButton(
                    icon = if (callState.isLocalMicMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                    contentDescription = if (callState.isLocalMicMuted) "Unmute microphone" else "Mute microphone",
                    color = if (callState.isLocalMicMuted) WalkieAmber else Color.White.copy(alpha = 0.22f),
                    testTag = "call_mute_button",
                    onClick = {
                        if (callState.isLocalMicMuted && !hasAudioPerm) {
                            pendingUnmuteAfterPerm = true
                            permLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                        } else {
                            onToggleMic()
                        }
                    }
                )
                CallClayControlButton(
                    icon = if (callState.isLocalCameraOn) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff,
                    contentDescription = if (callState.isLocalCameraOn) "Turn camera off" else "Turn camera on",
                    color = if (!callState.isLocalCameraOn) WalkieAmber else Color.White.copy(alpha = 0.22f),
                    testTag = "call_camera_button",
                    onClick = {
                        if (!callState.isLocalCameraOn && !hasCameraPerm) {
                            pendingEnableCameraAfterPerm = true
                            permLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                        } else {
                            onToggleCamera()
                        }
                    }
                )
                if (callState.isLocalCameraOn) {
                    CallClayControlButton(
                        icon = Icons.Rounded.Cameraswitch,
                        contentDescription = "Flip camera",
                        color = Color.White.copy(alpha = 0.22f),
                        testTag = "call_flip_camera_button",
                        onClick = onFlipCamera
                    )
                }
                CallClayControlButton(
                    icon = audioRouteIcon,
                    contentDescription = "Switch audio route ($audioRouteDesc)",
                    color = MintTreehouse.copy(alpha = 0.78f),
                    testTag = "call_audio_route_button",
                    onClick = onCycleAudioRoute
                )
                CallClayControlButton(
                    icon = Icons.Rounded.TouchApp,
                    contentDescription = "Tap pulse all screens",
                    color = ElectricRocket,
                    testTag = "call_tap_pulse_button",
                    onClick = onTriggerScreenTap
                )
                if (!isVoiceStage) {
                    CallClayControlButton(
                        icon = Icons.Rounded.Brush,
                        contentDescription = "In-call drawing overlay",
                        color = if (callState.showLiveDrawingOverlay) PurpleChest else Color.White.copy(alpha = 0.22f),
                        testTag = "call_drawing_overlay_button",
                        onClick = onToggleDrawingOverlay
                    )
                }
                CallClayControlButton(
                    icon = Icons.Rounded.CallEnd,
                    contentDescription = "End family call",
                    color = CrimsonDecline,
                    testTag = "call_end_button",
                    onClick = onEndCall
                )
            }
        }

        // Optional Synchronized Live Drawing Overlay right over the video call
        AnimatedVisibility(
            visible = callState.showLiveDrawingOverlay,
            modifier = Modifier.fillMaxSize()
        ) {
            val activePoints = remember { mutableStateListOf<Pair<Float, Float>>() }
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                activePoints.clear()
                                activePoints.add(offset.x / size.width to offset.y / size.height)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                activePoints.add(
                                    (change.position.x / size.width).coerceIn(0f, 1f) to
                                        (change.position.y / size.height).coerceIn(0f, 1f)
                                )
                            },
                            onDragEnd = {
                                onCommitDrawingStroke(Color(0xFFF43F5E), 8f, activePoints.toList())
                                activePoints.clear()
                            }
                        )
                    }
            ) {
                fun drawNormalizedPath(points: List<Pair<Float, Float>>, color: Color, widthDp: Float) {
                    if (points.size < 2) return
                    val path = Path().apply {
                        moveTo(points.first().first * size.width, points.first().second * size.height)
                        points.drop(1).forEach { (x, y) ->
                            lineTo(x * size.width, y * size.height)
                        }
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = widthDp.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                liveStrokes.forEach { stroke ->
                    drawNormalizedPath(stroke.points, Color(stroke.colorArgb), stroke.strokeWidthDp)
                }
                drawNormalizedPath(activePoints, Color(0xFFF43F5E), 8f)
            }
        }

        // Flying Emoji Physics Bursts
        EmojiPhysicsBurstOverlay(
            bursts = callState.activeEmojiBursts,
            reducedMotion = a11y.reducedMotion
        )
    }
}

@Composable
private fun LiveVoiceCallStage(
    selfMember: FamilyMemberId,
    participants: List<CallParticipantState>,
    profiles: List<UserProfileEntity>,
    isLiveStreamActive: Boolean,
    connectedLiveMembers: Set<FamilyMemberId>,
    tapPulseMember: FamilyMemberId?,
    reducedMotion: Boolean,
    onUpgradeToVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "voice_pulse")
    val pulseScale by infinite.animateFloat(
        initialValue = 1.0f,
        targetValue = if (reducedMotion) 1.0f else 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voice_ring_scale"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(28.dp))
            .padding(16.dp)
            .testTag("voice_call_stage"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.PhoneInTalk,
                contentDescription = null,
                tint = EmeraldAccept,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Full-Duplex Family Voice Room",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            participants.forEach { p ->
                val prof = profiles.firstOrNull { it.memberId == p.memberId.name }
                val badge = runCatching {
                    BadgeElement.valueOf(prof?.badgeElement ?: BadgeElement.STARLIGHT.name)
                }.getOrDefault(BadgeElement.STARLIGHT)
                val halo = p.memberId.haloColor()
                val isSelf = p.memberId == selfMember
                val isJoined = isSelf || (p.memberId in connectedLiveMembers) || p.isConnected
                val isPulsing = p.isSpeaking || tapPulseMember == p.memberId

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isPulsing) {
                            Box(
                                modifier = Modifier
                                    .size(106.dp)
                                    .graphicsLayer {
                                        scaleX = pulseScale
                                        scaleY = pulseScale
                                    }
                                    .clip(CircleShape)
                                    .background(halo.copy(alpha = 0.24f))
                                    .border(2.5.dp, halo.copy(alpha = 0.78f), CircleShape)
                            )
                        }
                        FamilyMemberAvatar(
                            member = p.memberId,
                            customPhotoUri = prof?.customPhotoUri,
                            badge = badge,
                            size = 86.dp,
                            showBadge = true
                        )
                        if (p.isMuted) {
                            Surface(
                                shape = CircleShape,
                                color = CrimsonDecline,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(26.dp)
                                    .border(2.dp, Color(0xFF0E2936), CircleShape)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.MicOff,
                                        contentDescription = "Muted",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = if (isSelf) "${p.memberId.displayName.substringBefore(" ")} (You)" else p.memberId.displayName.substringBefore(" "),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AudioWaveformBars(
                            level = if (p.isMuted || !isJoined) 0f else p.audioLevel,
                            barColor = halo
                        )
                        Text(
                            text = when {
                                p.isMuted -> "Muted"
                                !isSelf && isLiveStreamActive && !isJoined -> "Ringing…"
                                p.isSpeaking -> "Speaking"
                                else -> "Connected"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                }
            }
        }

        // Seamless 1-tap upgrade from Voice Call to Live Video Call
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.14f),
            modifier = Modifier
                .clickable(onClick = onUpgradeToVideo)
                .testTag("upgrade_voice_to_video_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Videocam,
                    contentDescription = "Switch to video",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Tap to turn on Camera",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun VideoParticipantTile(
    participant: CallParticipantState,
    profile: UserProfileEntity?,
    isLocalSelf: Boolean,
    liveKitRoom: Room?,
    liveVideoTrack: VideoTrack?,
    isLiveStreamActive: Boolean,
    allowOfflineCameraPreview: Boolean,
    isMemberJoinedLive: Boolean,
    hasCameraPerm: Boolean,
    isCameraOn: Boolean,
    isFrontCamera: Boolean,
    tapPulseActive: Boolean,
    modifier: Modifier = Modifier
) {
    val halo = participant.memberId.haloColor()
    val badge = runCatching {
        BadgeElement.valueOf(profile?.badgeElement ?: BadgeElement.STARLIGHT.name)
    }.getOrDefault(BadgeElement.STARLIGHT)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF111827))
            .border(
                width = if (tapPulseActive) 5.dp else if (participant.isSpeaking) 3.5.dp else 1.5.dp,
                color = if (tapPulseActive) WalkieAmber else if (participant.isSpeaking) halo else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(26.dp)
            )
    ) {
        // Real LiveKit WebRTC video surface for both local & remote family members
        if (liveKitRoom != null && liveVideoTrack != null && (if (isLocalSelf) isCameraOn else true)) {
            LiveKitVideoRendererView(
                room = liveKitRoom,
                videoTrack = liveVideoTrack,
                mirror = isLocalSelf && isFrontCamera,
                modifier = Modifier.fillMaxSize()
            )
        } else if (isLocalSelf && allowOfflineCameraPreview && liveKitRoom == null && !isLiveStreamActive && hasCameraPerm && isCameraOn) {
            // Only fall back to CameraX in offline preview mode (never contend with LiveKit's camera/EGL)
            CameraXLivePreview(
                useFrontCamera = isFrontCamera,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(halo.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FamilyMemberAvatar(
                        member = participant.memberId,
                        customPhotoUri = profile?.customPhotoUri,
                        badge = badge,
                        size = 86.dp
                    )
                    if (isLiveStreamActive && !isLocalSelf && !isMemberJoinedLive) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.55f)
                        ) {
                            Text(
                                text = "Ringing • Waiting to join…",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    } else if (!isCameraOn) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.45f)
                        ) {
                            Text(
                                text = "Camera off • Voice active",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.88f),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // AR Filter Overlay Badge
        if (participant.activeFilter != ArFilterType.NONE) {
            Text(
                text = participant.activeFilter.emoji,
                fontSize = 44.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp)
            )
        }

        // Muted indicator pill in top-right corner
        if (participant.isMuted) {
            Surface(
                shape = CircleShape,
                color = CrimsonDecline,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.MicOff,
                        contentDescription = "${participant.memberId.displayName} is muted",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Audio Waveform + Name Pill
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.56f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = participant.memberId.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            AudioWaveformBars(
                level = if (participant.isMuted) 0f else participant.audioLevel,
                barColor = halo
            )
        }
    }
}

@Composable
private fun AudioWaveformBars(level: Float, barColor: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(0.45f, 0.9f, 0.65f, 0.8f).forEach { factor ->
            val h = (6 + (level * factor * 14)).dp
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(h)
                    .clip(CircleShape)
                    .background(if (level > 0.05f) barColor else Color.Gray)
            )
        }
    }
}

@Composable
private fun CallClayControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    color: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val resolvedPlush = if (color.alpha < 0.45f) {
        com.example.ui.theme.PlushCobalt
    } else {
        color
    }
    com.example.ui.components.PlushCircleButton(
        icon = icon,
        contentDescription = contentDescription,
        plushColor = resolvedPlush,
        iconTint = Color.White,
        size = 48.dp,
        furLength = 4.dp,
        testTag = testTag,
        onClick = onClick
    )
}

@Composable
fun CameraXLivePreview(
    useFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            id = R.id.camerax_live_preview
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var boundCameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(lifecycleOwner, useFrontCamera) {
        var disposed = false
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            if (disposed) return@addListener
            runCatching {
                val cameraProvider = cameraProviderFuture.get()
                boundCameraProvider = cameraProvider
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val selector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed = true
            runCatching { boundCameraProvider?.unbindAll() }
            boundCameraProvider = null
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

@Composable
private fun LiveKitVideoRendererView(
    room: Room,
    videoTrack: VideoTrack,
    mirror: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val renderer = remember(room) {
        val view = runCatching {
            LayoutInflater.from(context)
                .inflate(R.layout.view_livekit_renderer, null, false) as SurfaceViewRenderer
        }.getOrElse {
            SurfaceViewRenderer(context).apply { id = R.id.livekit_video_renderer }
        }
        runCatching {
            room.initVideoRenderer(view)
            view.setEnableHardwareScaler(true)
            view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        }
        view
    }
    var attachedTrack by remember(renderer) { mutableStateOf<VideoTrack?>(null) }

    LaunchedEffect(renderer, mirror) {
        runCatching { renderer.setMirror(mirror) }
    }

    // Registered SECOND after AndroidView/onRelease so videoTrack always detaches before EGL release
    DisposableEffect(videoTrack, renderer) {
        runCatching {
            videoTrack.addRenderer(renderer)
            attachedTrack = videoTrack
        }
        onDispose {
            runCatching { videoTrack.removeRenderer(renderer) }
            if (attachedTrack == videoTrack) {
                attachedTrack = null
            }
        }
    }

    AndroidView(
        factory = { renderer },
        onRelease = { view ->
            runCatching { attachedTrack?.removeRenderer(view) }
            attachedTrack = null
            runCatching { view.release() }
        },
        modifier = modifier
    )
}
