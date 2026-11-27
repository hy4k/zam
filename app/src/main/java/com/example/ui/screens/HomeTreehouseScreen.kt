package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.UserProfileEntity
import com.example.data.model.BadgeElement
import com.example.data.model.FamilyMemberId
import com.example.data.model.MemberPresenceStatus
import com.example.data.model.UserSessionState
import com.example.data.model.haloColor
import com.example.ui.components.ClayActionButton
import com.example.ui.components.FamilyMemberAvatar
import com.example.ui.theme.CoralSalmon
import com.example.ui.theme.CoralSalmonDeep
import com.example.ui.theme.EmeraldAccept
import com.example.ui.theme.LocalKinNestAccessibility
import com.example.ui.theme.OchreGoldBright
import com.example.ui.theme.StudioTeal
import com.example.ui.theme.StudioTealDeep
import com.example.ui.theme.WalkieAmberDeep
import com.example.ui.theme.liquidGlass
import kotlinx.coroutines.delay
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeTreehouseScreen(
    session: UserSessionState,
    profiles: List<UserProfileEntity>,
    memberPresence: Map<FamilyMemberId, MemberPresenceStatus>,
    isWalkieRecording: Boolean,
    onCallSpecificMember: (FamilyMemberId) -> Unit,
    onCustomizeMyProfile: (BadgeElement, String?) -> Unit,
    onWalkiePressStart: () -> Unit,
    onWalkiePressRelease: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val a11y = LocalKinNestAccessibility.current
    val selfMember = session.deviceOwner ?: session.activeMember ?: FamilyMemberId.MITHUN
    val selfProfile = profiles.firstOrNull { it.memberId == selfMember.name }
    val selfBadge = runCatching {
        BadgeElement.valueOf(selfProfile?.badgeElement ?: BadgeElement.ROCKET.name)
    }.getOrDefault(BadgeElement.ROCKET)
    val selfPresence = memberPresence[selfMember]
    val selfIsOnline = selfPresence?.isOnline == true

    var showCustomizeSelfSheet by remember { mutableStateOf(false) }
    var inspectedMember by remember { mutableStateOf<FamilyMemberId?>(null) }

    val audioPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. "This Phone" Identity & Live Family Online Presence Card
        item {
            Box(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(28.dp),
                        surfaceAlpha = 0.84f,
                        tintColor = MaterialTheme.colorScheme.surface,
                        reducedMotion = a11y.reducedMotion,
                        cornerRadius = 28.dp
                    )
                    .padding(16.dp)
                    .testTag("home_identity_presence_card")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clickable { showCustomizeSelfSheet = true }
                                .testTag("home_self_avatar_button")
                        ) {
                            FamilyMemberAvatar(
                                member = selfMember,
                                customPhotoUri = selfProfile?.customPhotoUri,
                                badge = selfBadge,
                                size = 58.dp,
                                isOnline = selfIsOnline
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selfIsOnline) EmeraldAccept.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (selfIsOnline) EmeraldAccept else Color(0xFF94A3B8))
                                    )
                                    Text(
                                        text = "THIS PHONE • ${selfMember.title.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selfIsOnline) EmeraldAccept else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Hi, ${selfMember.displayName}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { showCustomizeSelfSheet = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("edit_my_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit my photo and badge",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(StudioTeal.copy(alpha = 0.16f))
                                .testTag("home_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Device and family settings",
                                tint = StudioTealDeep,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
                    )

                    // Live Family Online Presence Strip (Tapping another avatar NEVER switches login)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FamilyMemberId.entries.forEach { member ->
                            val isSelf = member == selfMember
                            val prof = profiles.firstOrNull { it.memberId == member.name }
                            val badge = runCatching {
                                BadgeElement.valueOf(prof?.badgeElement ?: BadgeElement.STARLIGHT.name)
                            }.getOrDefault(BadgeElement.STARLIGHT)
                            val presence = memberPresence[member]
                            val isMemberOnline = presence?.isOnline == true

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelf) {
                                    member.haloColor().copy(alpha = 0.22f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = if (isSelf) 1.5.dp else 1.dp,
                                        color = if (isSelf) member.haloColor() else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        if (isSelf) {
                                            showCustomizeSelfSheet = true
                                        } else {
                                            inspectedMember = member
                                        }
                                    }
                                    .testTag("presence_chip_${member.name.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    FamilyMemberAvatar(
                                        member = member,
                                        customPhotoUri = prof?.customPhotoUri,
                                        badge = badge,
                                        size = 44.dp,
                                        showBadge = false,
                                        isOnline = isMemberOnline
                                    )
                                    Text(
                                        text = if (isSelf) "${member.title} (You)" else member.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelf) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(if (isMemberOnline) EmeraldAccept else Color(0xFF94A3B8))
                                        )
                                        Text(
                                            text = if (isMemberOnline) "Online" else "Offline",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isMemberOnline) EmeraldAccept else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Hero Retro-Modern Walkie-Talkie Handset ("HOLD TO TALK")
        item {
            WalkieTalkieHandset(
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
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    // Quick Video Call Dialog when tapping another family member's avatar (NEVER switches login)
    inspectedMember?.let { member ->
        val prof = profiles.firstOrNull { it.memberId == member.name }
        val badge = runCatching {
            BadgeElement.valueOf(prof?.badgeElement ?: BadgeElement.STARLIGHT.name)
        }.getOrDefault(BadgeElement.STARLIGHT)
        val presence = memberPresence[member]
        val isOnline = presence?.isOnline == true

        AlertDialog(
            onDismissRequest = { inspectedMember = null },
            icon = {
                FamilyMemberAvatar(
                    member = member,
                    customPhotoUri = prof?.customPhotoUri,
                    badge = badge,
                    size = 72.dp,
                    isOnline = isOnline
                )
            },
            title = {
                Text("${member.displayName} (${member.title})")
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isOnline) EmeraldAccept.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) EmeraldAccept else Color(0xFF94A3B8))
                            )
                            Text(
                                text = presence?.statusLabel(isSelf = false) ?: "Offline",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isOnline) EmeraldAccept else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    ClayActionButton(
                        text = "Video Call ${member.displayName}",
                        containerColor = EmeraldAccept,
                        icon = Icons.Rounded.VideoCall,
                        testTag = "quick_call_${member.name.lowercase()}",
                        modifier = Modifier.fillMaxWidth(),
                        minHeight = 54.dp,
                        onClick = {
                            inspectedMember = null
                            onCallSpecificMember(member)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectedMember = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Personalize ONLY your own profile photo & daily badge
    if (showCustomizeSelfSheet) {
        var selectedBadge by remember(selfMember) { mutableStateOf(selfBadge) }
        var pickedPhotoUri by remember(selfMember) { mutableStateOf(selfProfile?.customPhotoUri) }

        val photoPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            uri?.let { pickedPhotoUri = it.toString() }
        }

        ModalBottomSheet(onDismissRequest = { showCustomizeSelfSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Style Your Look (${selfMember.displayName})",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                FamilyMemberAvatar(
                    member = selfMember,
                    customPhotoUri = pickedPhotoUri,
                    badge = selectedBadge,
                    size = 92.dp,
                    isOnline = selfIsOnline
                )
                Spacer(modifier = Modifier.height(14.dp))

                ClayActionButton(
                    text = "Pick My Photo",
                    containerColor = selfMember.haloColor(),
                    contentColor = Color(0xFF0D1B2A),
                    icon = Icons.Rounded.PhotoLibrary,
                    testTag = "pick_avatar_photo_button",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "CHOOSE DAILY ELEMENT BADGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BadgeElement.entries.forEach { badge ->
                        FilterChip(
                            selected = selectedBadge == badge,
                            onClick = { selectedBadge = badge },
                            label = { Text("${badge.emoji} ${badge.label}") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                ClayActionButton(
                    text = "Save My Look",
                    containerColor = StudioTealDeep,
                    testTag = "save_avatar_style_button",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onCustomizeMyProfile(selectedBadge, pickedPhotoUri)
                        showCustomizeSelfSheet = false
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Tactile Retro-Modern Family Walkie-Talkie Handset:
 * Features a sculpted stubby antenna, channel knob, perforated acoustic speaker grille,
 * backlit LCD channel readout with live transmit timer & audio bars, and a big recessed
 * rubber Push-To-Talk (PTT) button with glowing radio pulse rings.
 */
@Composable
private fun WalkieTalkieHandset(
    selfMember: FamilyMemberId,
    isOnline: Boolean,
    isRecording: Boolean,
    reducedMotion: Boolean,
    onPressStart: () -> Boolean,
    onPressRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var elapsedSec by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsedSec = 0
            while (true) {
                delay(1000L)
                elapsedSec += 1
            }
        } else {
            elapsedSec = 0
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "walkie_tx_anim")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "walkie_wave_phase"
    )
    val pulseRing by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "walkie_pulse_ring"
    )
    val pttScale by animateFloatAsState(
        targetValue = if (isRecording && !reducedMotion) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "ptt_press_scale"
    )

    Column(
        modifier = modifier.padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Hardware Row: Stubby Rubber Antenna (left) + Rotary Channel Knob (right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 34.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Stubby Textured Radio Antenna
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0A1620), Color(0xFF284354), Color(0xFF0C1A24))
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
                        )
                ) {
                    // Signal ring band on antenna tip
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                            .height(5.dp)
                            .background(if (isRecording) CoralSalmonDeep else OchreGoldBright)
                    )
                }
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(Color(0xFF1A3140))
                )
            }

            // Rotary Channel / Volume Knob + Mini Status Cap
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF1A3140), Color(0xFF3E6478), Color(0xFF162834))
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(CoralSalmonDeep, Color(0xFFF98A54), WalkieAmberDeep)
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                        )
                )
            }
        }

        // Main Rugged Walkie-Talkie Handset Body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(38.dp),
                    ambientColor = if (isRecording) CoralSalmonDeep.copy(alpha = 0.55f) else Color(0xFF0A1D29),
                    spotColor = if (isRecording) CoralSalmonDeep.copy(alpha = 0.55f) else Color(0xFF0A1D29)
                )
                .clip(RoundedCornerShape(38.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF18364A),
                            Color(0xFF0F2534),
                            Color(0xFF0B1A26)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.34f),
                            StudioTeal.copy(alpha = 0.22f),
                            Color.Black.copy(alpha = 0.45f)
                        )
                    ),
                    shape = RoundedCornerShape(38.dp)
                )
        ) {
            // Left & Right Tactile Coral Rubber Bumper Grips
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(vertical = 54.dp)
                    .width(8.dp)
                    .height(140.dp)
                    .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    .background(CoralSalmonDeep.copy(alpha = 0.9f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(vertical = 54.dp)
                    .width(8.dp)
                    .height(140.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    .background(CoralSalmonDeep.copy(alpha = 0.9f))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Brand Plate + Live TX / RX LED Indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isRecording) Color(0xFFFF4D4D) else Color(0xFF334E5E))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        )
                        Text(
                            text = "TX",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isRecording) Color(0xFFFF8080) else Color(0xFF7D9CAE)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) EmeraldAccept else Color(0xFF475569))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        )
                        Text(
                            text = "LINK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isOnline) EmeraldAccept else Color(0xFF7D9CAE)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = "A12-E • CH 03",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFD6E6EE),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Perforated Acoustic Speaker Grille
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    val rows = 4
                    val rowGap = size.height / (rows + 1)
                    val slotHeight = 4.5.dp.toPx()
                    for (r in 1..rows) {
                        val y = r * rowGap
                        val inset = if (r == 1 || r == rows) 18.dp.toPx() else 4.dp.toPx()
                        val slotWidth = size.width - inset * 2
                        // Dark acoustic slit
                        drawRoundRect(
                            color = Color(0xFF061017),
                            topLeft = Offset(inset, y - slotHeight / 2f),
                            size = Size(slotWidth, slotHeight),
                            cornerRadius = CornerRadius(slotHeight / 2f, slotHeight / 2f)
                        )
                        // Subtle bottom bevel reflection
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.12f),
                            topLeft = Offset(inset + 2f, y + slotHeight / 2f),
                            size = Size((slotWidth - 4f).coerceAtLeast(0f), 1.dp.toPx()),
                            cornerRadius = CornerRadius(1f, 1f)
                        )
                    }
                }

                // Backlit Retro LCD Display Screen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isRecording) Color(0xFF2B130B) else Color(0xFF0A2429)
                        )
                        .border(
                            width = 2.dp,
                            color = if (isRecording) CoralSalmonDeep.copy(alpha = 0.8f) else StudioTeal.copy(alpha = 0.48f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isRecording) "● ON AIR • BROADCASTING" else "FAMILY RADIO • READY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = if (isRecording) OchreGoldBright else Color(0xFF5EEAD4)
                            )
                            Text(
                                text = if (isRecording) {
                                    "%02d:%02d".format(elapsedSec / 60, elapsedSec % 60)
                                } else {
                                    selfMember.title.uppercase()
                                },
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = if (isRecording) Color.White else Color(0xFF99F6E4)
                            )
                        }

                        // Live Radio Frequency Waveform Visualizer inside LCD
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                        ) {
                            val barCount = 24
                            val spacing = size.width / barCount
                            val barWidth = (spacing * 0.56f).coerceAtLeast(3f)
                            val activeColor = if (isRecording) Color(0xFFFF9F43) else Color(0xFF2DD4BF).copy(alpha = 0.45f)
                            for (i in 0 until barCount) {
                                val amp = if (isRecording) {
                                    val wave = (sin(wavePhase + i * 0.65f) + 1f) * 0.5f
                                    0.22f + wave * 0.78f
                                } else {
                                    0.18f
                                }
                                val barH = size.height * amp
                                val x = i * spacing + (spacing - barWidth) / 2f
                                drawRoundRect(
                                    color = activeColor,
                                    topLeft = Offset(x, (size.height - barH) / 2f),
                                    size = Size(barWidth, barH),
                                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                                )
                            }
                        }
                    }
                }

                // Giant Tactile Push-To-Talk (PTT) Circular Radio Button
                Box(
                    modifier = Modifier
                        .size(188.dp)
                        .testTag("walkie_talkie_hold_button")
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    val started = onPressStart()
                                    if (!started) return@detectTapGestures
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    try {
                                        tryAwaitRelease()
                                    } finally {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onPressRelease()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing Radio Transmission Waves while holding PTT
                    if (isRecording && !reducedMotion) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val maxRadius = size.minDimension / 2f
                            listOf(pulseRing, (pulseRing + 0.5f) % 1f).forEach { p ->
                                val r = maxRadius * (0.68f + p * 0.32f)
                                drawCircle(
                                    color = CoralSalmonDeep.copy(alpha = (1f - p) * 0.55f),
                                    radius = r,
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }
                        }
                    }

                    // Recessed Metallic Bezel Ring
                    Box(
                        modifier = Modifier
                            .size(156.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF07131C))
                            .border(
                                width = 2.5.dp,
                                color = if (isRecording) OchreGoldBright.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.16f),
                                shape = CircleShape
                            )
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Tactile Push-To-Talk Dome Button
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = pttScale
                                    scaleY = pttScale
                                }
                                .shadow(
                                    elevation = if (isRecording) 4.dp else 14.dp,
                                    shape = CircleShape,
                                    ambientColor = CoralSalmonDeep,
                                    spotColor = CoralSalmonDeep
                                )
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = if (isRecording) {
                                            listOf(Color(0xFFFF6B35), Color(0xFFD9381E), Color(0xFF8C1D0E))
                                        } else {
                                            listOf(Color(0xFFFF834F), CoralSalmonDeep, WalkieAmberDeep)
                                        }
                                    )
                                )
                                .border(
                                    width = 2.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.65f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.28f)
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                                    contentDescription = "Hold to talk walkie talkie",
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isRecording) "TALKING…" else "HOLD TO",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = if (isRecording) "RELEASE" else "TALK",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.4.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.96f)
                                )
                            }
                        }
                    }
                }

                // Bottom Walkie-Talkie Instruction Caption
                Text(
                    text = if (isRecording) {
                        "Let go to send your voice message to the family"
                    } else {
                        "Press & hold the button like a walkie-talkie to send a voice clip"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9EBAC9)
                )
            }
        }
    }
}
