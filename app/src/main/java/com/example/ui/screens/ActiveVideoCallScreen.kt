package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.PersonAdd
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.ArFilterType
import com.example.data.model.AudioRoute
import com.example.data.model.CallParticipantState
import com.example.data.model.CallPhase
import com.example.data.model.CallState
import com.example.data.model.FamilyMemberId
import com.example.data.model.LiveStroke
import com.example.data.model.haloColor
import com.example.ui.components.EmojiPhysicsBurstOverlay
import com.example.ui.components.FamilyMemberAvatar
import com.example.ui.theme.CrimsonDecline
import com.example.ui.theme.ElectricRocket
import com.example.ui.theme.EmeraldAccept
import com.example.ui.theme.LocalKinNestAccessibility
import com.example.ui.theme.MintTreehouse
import com.example.ui.theme.PurpleChest
import com.example.ui.theme.WalkieAmber
import com.example.ui.theme.claymorphic
import com.example.ui.theme.liquidGlass
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack

@Composable
fun ActiveVideoCallScreen(
    selfMember: FamilyMemberId,
    callState: CallState,
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

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasCameraPerm = result[Manifest.permission.CAMERA] == true || hasCameraPerm
        hasAudioPerm = result[Manifest.permission.RECORD_AUDIO] == true || hasAudioPerm
        onMediaPermissionsChanged(hasCameraPerm, hasAudioPerm)
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPerm || !hasAudioPerm) {
            permLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    val participants = callState.participants.ifEmpty {
        listOf(
            CallParticipantState(selfMember, isConnected = true, isSpeaking = true, audioLevel = 0.6f)
        )
    }
    val missingThirdMember = FamilyMemberId.entries.firstOrNull { m ->
        participants.none { it.memberId == m }
    }

    val mins = callState.durationSeconds / 60
    val secs = callState.durationSeconds % 60
    val formattedDuration = "%02d:%02d".format(mins, secs)

    // Media Isolation: Deep dark unblurred stage for video feeds so hardware rendering stays cool
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B16))
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
                            text = if (callState.phase is CallPhase.Reconnecting) {
                                callState.phase.reason
                            } else {
                                "Family Live • $formattedDuration"
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
                                text = if (isLiveStreamActive) "LIVE" else "READY",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = callState.networkQualityLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f),
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
                                text = "+ ${missingThirdMember.displayName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Unblurred 3-Way Split Video Grid (Active Speaker + Concurrent Full-Duplex)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val primary = participants.first()
                VideoParticipantTile(
                    participant = primary,
                    isLocalSelf = primary.memberId == selfMember,
                    liveKitRoom = liveKitRoom,
                    liveVideoTrack = videoTracksByMember[primary.memberId],
                    isLiveStreamActive = isLiveStreamActive,
                    isMemberJoinedLive = primary.memberId in connectedLiveMembers,
                    hasCameraPerm = hasCameraPerm,
                    isCameraOn = callState.isLocalCameraOn,
                    isFrontCamera = callState.isFrontCamera,
                    tapPulseActive = callState.screenTapPulseMember == primary.memberId,
                    modifier = Modifier
                        .weight(1.25f)
                        .fillMaxWidth()
                )

                if (participants.size > 1) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        participants.drop(1).forEach { p ->
                            VideoParticipantTile(
                                participant = p,
                                isLocalSelf = p.memberId == selfMember,
                                liveKitRoom = liveKitRoom,
                                liveVideoTrack = videoTracksByMember[p.memberId],
                                isLiveStreamActive = isLiveStreamActive,
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

            // In-Call Interactive Fun Strip: AR Filters, Emoji Physics Triggers, Tap Ripple, Drawing Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                listOf("🚀", "💖", "😂", "🦕").forEach { emoji ->
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

            // Lower-Third Clay Controls (56dp+ targets for Zayn & Parents)
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
                    contentDescription = "Mute microphone",
                    color = if (callState.isLocalMicMuted) WalkieAmber else Color.White.copy(alpha = 0.22f),
                    testTag = "call_mute_button",
                    onClick = onToggleMic
                )
                CallClayControlButton(
                    icon = if (callState.isLocalCameraOn) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff,
                    contentDescription = "Toggle camera",
                    color = if (!callState.isLocalCameraOn) WalkieAmber else Color.White.copy(alpha = 0.22f),
                    testTag = "call_camera_button",
                    onClick = onToggleCamera
                )
                CallClayControlButton(
                    icon = Icons.Rounded.Cameraswitch,
                    contentDescription = "Flip camera",
                    color = Color.White.copy(alpha = 0.22f),
                    testTag = "call_flip_camera_button",
                    onClick = onFlipCamera
                )
                CallClayControlButton(
                    icon = when (callState.audioRoute) {
                        AudioRoute.BLUETOOTH -> Icons.Rounded.BluetoothAudio
                        else -> Icons.Rounded.SpeakerPhone
                    },
                    contentDescription = "Audio route",
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
                CallClayControlButton(
                    icon = Icons.Rounded.Brush,
                    contentDescription = "In-call drawing overlay",
                    color = if (callState.showLiveDrawingOverlay) PurpleChest else Color.White.copy(alpha = 0.22f),
                    testTag = "call_drawing_overlay_button",
                    onClick = onToggleDrawingOverlay
                )
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
private fun VideoParticipantTile(
    participant: CallParticipantState,
    isLocalSelf: Boolean,
    liveKitRoom: Room?,
    liveVideoTrack: VideoTrack?,
    isLiveStreamActive: Boolean,
    isMemberJoinedLive: Boolean,
    hasCameraPerm: Boolean,
    isCameraOn: Boolean,
    isFrontCamera: Boolean,
    tapPulseActive: Boolean,
    modifier: Modifier = Modifier
) {
    val halo = participant.memberId.haloColor()
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
        } else if (isLocalSelf && !isLiveStreamActive && hasCameraPerm && isCameraOn) {
            // Only fall back to CameraX in offline preview mode (never contend with LiveKit's camera)
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
            AudioWaveformBars(level = participant.audioLevel, barColor = halo)
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
    Box(
        modifier = Modifier
            .size(56.dp)
            .claymorphic(cornerRadius = 28.dp, surfaceColor = color, elevation = 8.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
fun CameraXLivePreview(
    useFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(useFrontCamera) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            runCatching {
                val cameraProvider = cameraProviderFuture.get()
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
        TextureViewRenderer(context).also { view ->
            runCatching { room.initVideoRenderer(view) }
        }
    }

    LaunchedEffect(renderer, mirror) {
        runCatching { renderer.setMirror(mirror) }
    }

    DisposableEffect(videoTrack, renderer) {
        runCatching { videoTrack.addRenderer(renderer) }
        onDispose {
            runCatching { videoTrack.removeRenderer(renderer) }
        }
    }

    DisposableEffect(renderer) {
        onDispose {
            runCatching { renderer.release() }
        }
    }

    AndroidView(
        factory = { renderer },
        modifier = modifier
    )
}
