package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.SpeakerPhone
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.UserProfileEntity
import com.example.data.model.AudioRoute
import com.example.data.model.BadgeElement
import com.example.data.model.CallState
import com.example.data.model.FamilyMemberId
import com.example.data.model.MemberPresenceStatus
import com.example.data.model.UserSessionState
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.components.FamilyMemberAvatar
import com.example.ui.theme.CoralSalmon
import com.example.ui.theme.CoralSalmonDeep
import com.example.ui.theme.EmeraldAccept
import com.example.ui.theme.LocalKinNestAccessibility
import com.example.ui.theme.OchreGold
import com.example.ui.theme.OchreGoldBright
import com.example.ui.theme.PowderSkyDeep
import com.example.ui.theme.StudioTeal
import com.example.ui.theme.StudioTealDeep
import com.example.ui.theme.clayPressScale

@Composable
fun CallLauncherScreen(
    session: UserSessionState,
    callState: CallState,
    profiles: List<UserProfileEntity>,
    memberPresence: Map<FamilyMemberId, MemberPresenceStatus>,
    onStartThreeWayCall: () -> Unit,
    onStartOneOnOneCall: (FamilyMemberId) -> Unit,
    onCycleAudioRoute: () -> Unit
) {
    val a11y = LocalKinNestAccessibility.current
    val self = session.deviceOwner ?: session.activeMember ?: FamilyMemberId.MITHUN
    val otherMembers = FamilyMemberId.entries.filterNot { it == self }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Group Video Call Section
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EditorialSectionHeader(
                    eyebrow = "Everyone Together",
                    title = "Group Call",
                    accentColor = StudioTealDeep
                )

                // Big Vibrant Group Call Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clayPressScale(reducedMotion = a11y.reducedMotion)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(32.dp),
                            ambientColor = StudioTealDeep.copy(alpha = 0.45f),
                            spotColor = EmeraldAccept.copy(alpha = 0.45f)
                        )
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(StudioTealDeep, StudioTeal, EmeraldAccept)
                            )
                        )
                        .border(
                            width = 1.8.dp,
                            color = Color.White.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .clickable(onClick = onStartThreeWayCall)
                        .padding(horizontal = 22.dp, vertical = 22.dp)
                        .testTag("launch_3way_call_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Mini overlapping family trio avatars
                            Box(modifier = Modifier.height(42.dp)) {
                                FamilyMemberId.entries.forEachIndexed { idx, member ->
                                    val prof = profiles.firstOrNull { it.memberId == member.name }
                                    FamilyMemberAvatar(
                                        member = member,
                                        customPhotoUri = prof?.customPhotoUri,
                                        size = 40.dp,
                                        showBadge = false,
                                        isOnline = memberPresence[member]?.isOnline == true,
                                        modifier = Modifier.offset(x = (idx * 28).dp)
                                    )
                                }
                            }
                            Text(
                                text = "Group Video Call",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "Vapi • Ummi • Zayn live together",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.24f))
                                .border(2.dp, Color.White.copy(alpha = 0.65f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Groups,
                                contentDescription = "Start group family video call",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        }

        // Simple Colorful Individual Call Buttons
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EditorialSectionHeader(
                    eyebrow = "1-on-1 Live Video",
                    title = "Individual Call",
                    accentColor = CoralSalmonDeep
                )

                otherMembers.forEach { target ->
                    val prof = profiles.firstOrNull { it.memberId == target.name }
                    val badge = runCatching {
                        BadgeElement.valueOf(prof?.badgeElement ?: BadgeElement.STARLIGHT.name)
                    }.getOrDefault(BadgeElement.STARLIGHT)
                    val presence = memberPresence[target]
                    val isOnline = presence?.isOnline == true

                    val (gradientColors, contentColor) = when (target) {
                        FamilyMemberId.MITHUN -> listOf(Color(0xFF1D6FA5), PowderSkyDeep) to Color.White
                        FamilyMemberId.MAFITHA -> listOf(CoralSalmonDeep, CoralSalmon) to Color.White
                        FamilyMemberId.ZAYN -> listOf(OchreGold, OchreGoldBright) to Color(0xFF0D1B2A)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clayPressScale(reducedMotion = a11y.reducedMotion)
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(28.dp),
                                ambientColor = gradientColors.first().copy(alpha = 0.42f),
                                spotColor = gradientColors.first().copy(alpha = 0.42f)
                            )
                            .clip(RoundedCornerShape(28.dp))
                            .background(Brush.horizontalGradient(gradientColors))
                            .border(
                                width = 1.5.dp,
                                color = Color.White.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .clickable { onStartOneOnOneCall(target) }
                            .padding(horizontal = 18.dp, vertical = 18.dp)
                            .testTag("call_1on1_${target.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FamilyMemberAvatar(
                                member = target,
                                customPhotoUri = prof?.customPhotoUri,
                                badge = badge,
                                size = 64.dp,
                                isOnline = isOnline
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Call ${target.displayName}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = contentColor
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (contentColor == Color.White) {
                                        Color.Black.copy(alpha = 0.18f)
                                    } else {
                                        Color.White.copy(alpha = 0.52f)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (isOnline) EmeraldAccept else Color(0xFFCBD5E1))
                                        )
                                        Text(
                                            text = if (isOnline) "${target.title} • Online" else "${target.title} • Offline",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = contentColor
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (contentColor == Color.White) {
                                            Color.White.copy(alpha = 0.24f)
                                        } else {
                                            Color(0xFF0D1B2A).copy(alpha = 0.14f)
                                        }
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = contentColor.copy(alpha = 0.45f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VideoCall,
                                    contentDescription = "Video call ${target.displayName}",
                                    tint = contentColor,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Simple Audio Output Toggle Pill
        item {
            val (audioLabel, audioIcon, audioBg) = when (callState.audioRoute) {
                AudioRoute.SPEAKER -> Triple("Sound: Speaker", Icons.Rounded.SpeakerPhone, StudioTealDeep)
                AudioRoute.BLUETOOTH -> Triple("Sound: Bluetooth", Icons.Rounded.BluetoothAudio, PowderSkyDeep)
                AudioRoute.EARPIECE -> Triple("Sound: Phone Ear", Icons.Rounded.Hearing, Color(0xFF334155))
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = audioBg,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(onClick = onCycleAudioRoute)
                    .testTag("cycle_audio_route_button")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = audioIcon,
                        contentDescription = "Switch call audio output",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = audioLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
