package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.SpeakerPhone
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserProfileEntity
import com.example.data.model.AudioRoute
import com.example.data.model.BadgeElement
import com.example.data.model.CallState
import com.example.data.model.FamilyMemberId
import com.example.data.model.MemberPresenceStatus
import com.example.data.model.PresenceAvailability
import com.example.data.model.UserSessionState
import com.example.data.model.statusColor
import com.example.ui.components.ClayTextOnLight
import com.example.ui.components.FamilyMemberAvatar
import com.example.ui.theme.ClayAqua
import com.example.ui.theme.ClayAquaDeep
import com.example.ui.theme.ClayCoral
import com.example.ui.theme.ClayCoralDeep
import com.example.ui.theme.ClayDeepTealInk
import com.example.ui.theme.ClayHoney
import com.example.ui.theme.ClayHoneyDeep
import com.example.ui.theme.ClayMutedInk
import com.example.ui.theme.ClayPeach
import com.example.ui.theme.ClaySage
import com.example.ui.theme.ClaySageDeep
import com.example.ui.theme.ClayTealShadow
import com.example.ui.theme.ClayTextCream
import com.example.ui.theme.ClayVanilla
import com.example.ui.theme.ClayVanillaLight
import com.example.ui.theme.LocalKinNestAccessibility
import com.example.ui.theme.VibrantViolet
import com.example.ui.theme.clayDisc3D
import com.example.ui.theme.clayPressScale
import com.example.ui.theme.claymorphic

/**
 * Page 2 — 50/50 Video Call & Voice Call Launcher (NO top page title!).
 * Styled strictly in the Pixar 3D Matte Clay theme with live Supabase Realtime
 * 'Online' / 'Away' / 'Busy' presence status for Mithun, Mafitha, and Zayn!
 */
@Composable
fun CallLauncherScreen(
    session: UserSessionState,
    callState: CallState,
    profiles: List<UserProfileEntity>,
    memberPresence: Map<FamilyMemberId, MemberPresenceStatus>,
    onCycleMyPresenceStatus: () -> Unit,
    onStartGroupVideoCall: () -> Unit,
    onStartIndividualVideoCall: (FamilyMemberId) -> Unit,
    onStartGroupVoiceCall: () -> Unit,
    onStartIndividualVoiceCall: (FamilyMemberId) -> Unit,
    onCycleAudioRoute: () -> Unit
) {
    val self = session.deviceOwner ?: session.activeMember ?: FamilyMemberId.MITHUN
    val otherMembers = FamilyMemberId.entries.filterNot { it == self }
    val a11y = LocalKinNestAccessibility.current

    val (audioLabel, audioIcon) = when (callState.audioRoute) {
        AudioRoute.SPEAKER -> "Speaker" to Icons.Rounded.SpeakerPhone
        AudioRoute.BLUETOOTH -> "Bluetooth" to Icons.Rounded.BluetoothAudio
        AudioRoute.EARPIECE -> "Ear" to Icons.Rounded.Hearing
    }

    val onlineCount = memberPresence.values.count { it.availability == PresenceAvailability.ONLINE }
    val groupAvailability = when {
        memberPresence.values.any { it.availability == PresenceAvailability.ONLINE } -> PresenceAvailability.ONLINE
        memberPresence.values.any { it.availability == PresenceAvailability.BUSY } -> PresenceAvailability.BUSY
        memberPresence.values.any { it.availability == PresenceAvailability.AWAY } -> PresenceAvailability.AWAY
        else -> PresenceAvailability.OFFLINE
    }
    val groupStatusLabel = when {
        onlineCount >= 2 -> "$onlineCount Online"
        onlineCount == 1 -> "1 Online"
        else -> groupAvailability.label
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Minimal Top Row (NO page name!) — Compact Live Presence Strip + 3D Clay Audio Output Toggle Pill
        Row(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val myPresence = memberPresence[self] ?: MemberPresenceStatus(self)
            Row(
                modifier = Modifier
                    .height(42.dp)
                    .clayPressScale(reducedMotion = a11y.reducedMotion)
                    .claymorphic(
                        cornerRadius = 21.dp,
                        surfaceColor = ClayPeach,
                        elevation = 8.dp,
                        ledgeDepth = 3.5.dp
                    )
                    .clickable(onClick = onCycleMyPresenceStatus)
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 2.dp)
                    .testTag("call_self_presence_toggle"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(myPresence.availability.statusColor())
                        .border(1.4.dp, Color.White, CircleShape)
                )
                ClayTextOnLight(
                    text = "${self.displayName.substringBefore(" ")} • ${myPresence.shortBadgeLabel()}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    textColor = ClayDeepTealInk,
                    depthDp = 0.7.dp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                modifier = Modifier
                    .height(42.dp)
                    .clayPressScale(reducedMotion = a11y.reducedMotion)
                    .claymorphic(
                        cornerRadius = 21.dp,
                        surfaceColor = ClayVanillaLight,
                        elevation = 8.dp,
                        ledgeDepth = 3.5.dp
                    )
                    .clickable(onClick = onCycleAudioRoute)
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 2.dp)
                    .testTag("cycle_audio_route_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = audioIcon,
                    contentDescription = "Switch call audio output",
                    tint = ClayAquaDeep,
                    modifier = Modifier.size(18.dp)
                )
                ClayTextOnLight(
                    text = audioLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    textColor = ClayDeepTealInk,
                    depthDp = 0.8.dp
                )
            }
        }

        // =====================================================================
        // TOP HALF (50%) — VIDEO CALL (Warm Peach-Cream 3D Matte Clay Slab)
        // =====================================================================
        Box(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .weight(1f)
                .claymorphic(
                    cornerRadius = 34.dp,
                    surfaceColor = ClayPeach,
                    elevation = 16.dp,
                    shadowTint = ClayTealShadow,
                    ledgeDepth = 7.dp
                )
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 18.dp)
                .testTag("video_call_half_section")
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clayDisc3D(
                                surfaceColor = ClayCoral,
                                elevation = 8.dp,
                                ledgeDepth = 3.5.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Videocam,
                            contentDescription = "Video Call",
                            tint = ClayTextCream,
                            modifier = Modifier
                                .offset(y = (-1).dp)
                                .size(23.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        ClayTextOnLight(
                            text = "Video Call",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 23.sp
                            ),
                            textColor = ClayCoralDeep,
                            depthDp = 1.6.dp
                        )
                        ClayTextOnLight(
                            text = "See smiles live on camera",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            textColor = ClayDeepTealInk,
                            depthDp = 0.7.dp
                        )
                    }
                }

                // 3 Compact, Sculpted 3D Clay Squircle Video Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactStudioClayCallTile(
                        title = "All 3",
                        statusSubtitle = groupStatusLabel,
                        clayColor = ClayAqua,
                        badgeIcon = Icons.Rounded.Groups,
                        member = null,
                        profile = null,
                        availability = groupAvailability,
                        callTypeIcon = Icons.Rounded.Videocam,
                        testTag = "launch_3way_call_button",
                        modifier = Modifier.weight(1f),
                        onClick = onStartGroupVideoCall
                    )

                    otherMembers.forEachIndexed { idx, target ->
                        val prof = profiles.firstOrNull { it.memberId == target.name }
                        val presence = memberPresence[target] ?: MemberPresenceStatus(target)
                        val clayTint = if (idx == 0) ClayCoral else ClayHoney
                        CompactStudioClayCallTile(
                            title = target.displayName.substringBefore(" "),
                            statusSubtitle = presence.shortBadgeLabel(),
                            clayColor = clayTint,
                            badgeIcon = null,
                            member = target,
                            profile = prof,
                            availability = presence.availability,
                            callTypeIcon = Icons.Rounded.Videocam,
                            testTag = "call_1on1_${target.name.lowercase()}",
                            modifier = Modifier.weight(1f),
                            onClick = { onStartIndividualVideoCall(target) }
                        )
                    }
                }
            }
        }

        // =====================================================================
        // BOTTOM HALF (50%) — VOICE CALL (Warm Vanilla-Cream 3D Matte Clay Slab)
        // =====================================================================
        Box(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .weight(1f)
                .claymorphic(
                    cornerRadius = 34.dp,
                    surfaceColor = ClayVanilla,
                    elevation = 16.dp,
                    shadowTint = ClayTealShadow,
                    ledgeDepth = 7.dp
                )
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 18.dp)
                .testTag("voice_call_half_section")
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clayDisc3D(
                                surfaceColor = ClayAqua,
                                elevation = 8.dp,
                                ledgeDepth = 3.5.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhoneInTalk,
                            contentDescription = "Voice Call",
                            tint = ClayTextCream,
                            modifier = Modifier
                                .offset(y = (-1).dp)
                                .size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        ClayTextOnLight(
                            text = "Voice Call",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 23.sp
                            ),
                            textColor = ClayDeepTealInk,
                            depthDp = 1.6.dp
                        )
                        ClayTextOnLight(
                            text = "Quick audio chat without video",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            textColor = ClayMutedInk,
                            depthDp = 0.7.dp
                        )
                    }
                }

                // 3 Compact, Sculpted 3D Clay Squircle Voice Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactStudioClayCallTile(
                        title = "All 3",
                        statusSubtitle = groupStatusLabel,
                        clayColor = ClayHoney,
                        badgeIcon = Icons.Rounded.Groups,
                        member = null,
                        profile = null,
                        availability = groupAvailability,
                        callTypeIcon = Icons.Rounded.Call,
                        testTag = "launch_3way_voice_call_button",
                        modifier = Modifier.weight(1f),
                        onClick = onStartGroupVoiceCall
                    )

                    otherMembers.forEachIndexed { idx, target ->
                        val prof = profiles.firstOrNull { it.memberId == target.name }
                        val presence = memberPresence[target] ?: MemberPresenceStatus(target)
                        val clayTint = if (idx == 0) ClaySage else VibrantViolet
                        CompactStudioClayCallTile(
                            title = target.displayName.substringBefore(" "),
                            statusSubtitle = presence.shortBadgeLabel(),
                            clayColor = clayTint,
                            badgeIcon = null,
                            member = target,
                            profile = prof,
                            availability = presence.availability,
                            callTypeIcon = Icons.Rounded.Call,
                            testTag = "voice_call_1on1_${target.name.lowercase()}",
                            modifier = Modifier.weight(1f),
                            onClick = { onStartIndividualVoiceCall(target) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
    }
}

/**
 * Compact, Super-Attractive 3D Matte Clay Call Button Tile with live Supabase Realtime
 * 'Online' / 'Away' / 'Busy' / 'Offline' presence badge underneath.
 */
@Composable
private fun CompactStudioClayCallTile(
    title: String,
    statusSubtitle: String,
    clayColor: Color,
    badgeIcon: ImageVector?,
    member: FamilyMemberId?,
    profile: UserProfileEntity?,
    availability: PresenceAvailability,
    callTypeIcon: ImageVector,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val a11y = LocalKinNestAccessibility.current
    val badge = runCatching {
        BadgeElement.valueOf(profile?.badgeElement ?: BadgeElement.STARLIGHT.name)
    }.getOrDefault(BadgeElement.STARLIGHT)
    val dotColor = availability.statusColor()
    val statusInk = when (availability) {
        PresenceAvailability.ONLINE -> ClaySageDeep
        PresenceAvailability.AWAY -> ClayHoneyDeep
        PresenceAvailability.BUSY -> ClayCoralDeep
        PresenceAvailability.OFFLINE -> ClayMutedInk
    }

    Column(
        modifier = modifier
            .clayPressScale(reducedMotion = a11y.reducedMotion)
            .clickable(onClick = onClick)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .claymorphic(
                    cornerRadius = 26.dp,
                    surfaceColor = clayColor,
                    elevation = 12.dp,
                    shadowTint = ClayTealShadow,
                    ledgeDepth = 5.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            if (member != null) {
                FamilyMemberAvatar(
                    member = member,
                    customPhotoUri = profile?.customPhotoUri,
                    badge = badge,
                    size = 54.dp,
                    showBadge = false,
                    availability = availability,
                    modifier = Modifier.offset(y = (-2).dp)
                )
            } else if (badgeIcon != null) {
                Box(
                    modifier = Modifier
                        .offset(y = (-2).dp)
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                        .border(1.8.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = Color(0x3616484C),
                        modifier = Modifier
                            .offset(y = 1.8.dp)
                            .size(31.dp)
                    )
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = title,
                        tint = ClayTextCream,
                        modifier = Modifier.size(31.dp)
                    )
                }
            }

            // Crisp Cream Corner Call-Type Emblem
            Surface(
                shape = CircleShape,
                color = ClayVanillaLight,
                shadowElevation = 5.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 7.dp, bottom = 9.dp)
                    .size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = callTypeIcon,
                        contentDescription = null,
                        tint = clayColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Crisp 100% Legible Deep-Teal Name + Live Online/Away/Busy Status Underneath
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.5.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .border(1.dp, Color.White, CircleShape)
                )
                ClayTextOnLight(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp
                    ),
                    textColor = ClayDeepTealInk,
                    depthDp = 1.dp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            ClayTextOnLight(
                text = statusSubtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.5.sp
                ),
                textColor = statusInk,
                depthDp = 0.5.dp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
