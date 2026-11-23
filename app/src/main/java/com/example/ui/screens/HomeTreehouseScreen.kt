package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.FamilyActivityEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.BadgeElement
import com.example.data.model.FamilyMemberId
import com.example.data.model.MomentCategory
import com.example.data.model.UserSessionState
import com.example.data.model.VoiceLanguage
import com.example.ui.HeartbeatBannerAlert
import com.example.ui.components.ClayActionButton
import com.example.ui.components.ComicStoryboardBanner
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.components.FamilyMemberAvatar
import com.example.ui.theme.CoralSalmon
import com.example.ui.theme.CoralSalmonDeep
import com.example.ui.theme.EmeraldAccept
import com.example.ui.theme.LocalKinNestAccessibility
import com.example.ui.theme.OchreGold
import com.example.ui.theme.PeriwinklePop
import com.example.ui.theme.PowderSky
import com.example.ui.theme.StudioTealDeep
import com.example.ui.theme.StudioTealSoft
import com.example.ui.theme.WalkieAmberDeep
import com.example.ui.theme.claymorphic
import com.example.ui.theme.liquidGlass

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeTreehouseScreen(
    session: UserSessionState,
    profiles: List<UserProfileEntity>,
    dailyMoment: FamilyActivityEntity?,
    heartbeatBanner: HeartbeatBannerAlert?,
    isWalkieRecording: Boolean,
    onDismissHeartbeatBanner: () -> Unit,
    onSendHeartbeat: (String, Boolean) -> Unit,
    onStartThreeWayCall: () -> Unit,
    onWalkiePressStart: () -> Unit,
    onWalkiePressRelease: () -> Unit,
    onRotateMoment: (MomentCategory) -> Unit,
    onToggleLanguage: () -> Unit,
    onSpeakTreehouseTip: () -> Unit,
    onReplyHeartbeatWithVideo: () -> Unit
) {
    val context = LocalContext.current
    val a11y = LocalKinNestAccessibility.current
    val selfMember = session.activeMember ?: FamilyMemberId.ZAYN
    val selfProfile = profiles.firstOrNull { it.memberId == selfMember.name }
    val selfBadge = runCatching {
        BadgeElement.valueOf(selfProfile?.badgeElement ?: BadgeElement.ROCKET.name)
    }.getOrDefault(BadgeElement.ROCKET)

    val audioPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val walkieScale by animateFloatAsState(
        targetValue = if (isWalkieRecording && !a11y.reducedMotion) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 450f),
        label = "walkie_scale"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (heartbeatBanner != null) {
            item {
                Box(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .claymorphic(
                            cornerRadius = 28.dp,
                            surfaceColor = if (heartbeatBanner.isSafetyAlert) CoralSalmonDeep else CoralSalmon,
                            elevation = 14.dp
                        )
                        .padding(18.dp)
                        .testTag("active_heartbeat_alert_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (heartbeatBanner.isSafetyAlert) Icons.Rounded.WarningAmber else Icons.Rounded.NotificationsActive,
                                contentDescription = "Alert",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${heartbeatBanner.sender.displayName} sent a ${if (heartbeatBanner.isSafetyAlert) "Safety Alert" else "Heartbeat"}!",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = heartbeatBanner.message,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.95f)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ClayActionButton(
                                text = "Reply Video",
                                containerColor = EmeraldAccept,
                                icon = Icons.Rounded.VideoCall,
                                testTag = "heartbeat_reply_video",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onDismissHeartbeatBanner()
                                    onReplyHeartbeatWithVideo()
                                }
                            )
                            ClayActionButton(
                                text = "Got It 💖",
                                containerColor = Color.White,
                                contentColor = Color(0xFF6B1D17),
                                testTag = "heartbeat_dismiss",
                                modifier = Modifier.weight(1f),
                                onClick = onDismissHeartbeatBanner
                            )
                        }
                    }
                }
            }
        }

        // Studio Frosted Profile & Bilingual TTS Header
        item {
            Box(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(28.dp),
                        surfaceAlpha = 0.78f,
                        tintColor = MaterialTheme.colorScheme.surface,
                        reducedMotion = a11y.reducedMotion,
                        cornerRadius = 28.dp
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FamilyMemberAvatar(
                        member = selfMember,
                        customPhotoUri = selfProfile?.customPhotoUri,
                        badge = selfBadge,
                        size = 62.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (session.voiceLanguage == VoiceLanguage.MALAYALAM) {
                                "കുടുംബ സ്റ്റുഡിയോ • മലയാളം"
                            } else {
                                "FAMILY STUDIO • ${selfMember.title.uppercase()}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioTealDeep
                        )
                        Text(
                            text = "Hi, ${selfMember.displayName}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onSpeakTreehouseTip,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(StudioTealSoft)
                            .testTag("tts_guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VolumeUp,
                            contentDescription = "Read aloud in English or Malayalam",
                            tint = StudioTealDeep
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onToggleLanguage,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(PowderSky)
                            .testTag("toggle_malayalam_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Translate,
                            contentDescription = "Switch Malayalam / English voice-over",
                            tint = Color(0xFF152754)
                        )
                    }
                }
            }
        }

        // Comic-World Storyboard 3-Way Call Hero Panel
        item {
            ComicStoryboardBanner(
                issueBadge = "LIVEKIT 3-WAY SFU",
                secondaryBadge = "ON AIR",
                headline = "Jump Into the Storyverse Call",
                caption = "Tap to video-call Mithun, Mafitha & Zayn together",
                leadingIcon = Icons.Rounded.VideoCall,
                height = 192.dp,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .testTag("home_3way_call_hero"),
                onClick = onStartThreeWayCall
            )
        }

        // Daily "Moments" Glassmorphic Card
        item {
            Box(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(28.dp),
                        surfaceAlpha = 0.78f,
                        tintColor = MaterialTheme.colorScheme.surface,
                        reducedMotion = a11y.reducedMotion,
                        cornerRadius = 28.dp
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = PeriwinklePop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DAILY FAMILY MOMENT",
                                style = MaterialTheme.typography.labelSmall,
                                color = PeriwinklePop
                            )
                            Text(
                                text = "Today's Spark",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        IconButton(
                            onClick = { onRotateMoment(MomentCategory.entries.random()) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("rotate_moment_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "New Daily Moment"
                            )
                        }
                    }
                    Text(
                        text = dailyMoment?.title ?: "Make a goofy 5-second alien sound for Zayn!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MomentCategory.entries.forEach { cat ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = StudioTealSoft,
                                modifier = Modifier
                                    .height(38.dp)
                                    .clickable { onRotateMoment(cat) }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Text(
                                        text = cat.title.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioTealDeep
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Ochre Gold Massive Press-and-Hold Walkie-Talkie Clay Control
        item {
            Box(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = walkieScale
                        scaleY = walkieScale
                    }
                    .claymorphic(
                        cornerRadius = 34.dp,
                        surfaceColor = if (isWalkieRecording) WalkieAmberDeep else OchreGold,
                        elevation = 16.dp
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!hasPerm) {
                                    audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    return@detectTapGestures
                                }
                                onWalkiePressStart()
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    onWalkiePressRelease()
                                }
                            }
                        )
                    }
                    .padding(vertical = 24.dp, horizontal = 20.dp)
                    .testTag("walkie_talkie_hold_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.26f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isWalkieRecording) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                            contentDescription = "Walkie Talkie Microphone",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (isWalkieRecording) "LISTENING… RELEASE!" else "HOLD TO TALK",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Instant AAC voice clip • Hold 1s+ and release",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }
                }
            }
        }

        // One-Tap Family Heartbeat & Safety Check-In Grid
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EditorialSectionHeader(
                    eyebrow = "Instant Family Pulse",
                    title = "Heartbeat & Safety Check-In",
                    accentColor = CoralSalmon
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ClayActionButton(
                        text = "I miss you 💖",
                        containerColor = CoralSalmon,
                        icon = Icons.Rounded.Favorite,
                        testTag = "heartbeat_miss_you",
                        onClick = { onSendHeartbeat("I miss you!", false) }
                    )
                    ClayActionButton(
                        text = "Call Papa 🚀",
                        containerColor = StudioTealDeep,
                        icon = Icons.Rounded.Call,
                        testTag = "heartbeat_call_papa",
                        onClick = { onSendHeartbeat("Call Papa!", false) }
                    )
                    ClayActionButton(
                        text = "Come play! 🦕",
                        containerColor = PeriwinklePop,
                        testTag = "heartbeat_come_play",
                        onClick = { onSendHeartbeat("Come play with me!", false) }
                    )
                    ClayActionButton(
                        text = "Call me now",
                        containerColor = WalkieAmberDeep,
                        icon = Icons.Rounded.NotificationsActive,
                        testTag = "safety_call_me",
                        onClick = { onSendHeartbeat("Call me right now", true) }
                    )
                    ClayActionButton(
                        text = "Come get me",
                        containerColor = CoralSalmonDeep,
                        icon = Icons.Rounded.WarningAmber,
                        testTag = "safety_come_get_me",
                        onClick = { onSendHeartbeat("Come get me please", true) }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
