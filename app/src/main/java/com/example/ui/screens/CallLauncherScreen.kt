package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.CallMissed
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RocketLaunch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.UserProfileEntity
import com.example.data.model.AudioRoute
import com.example.data.model.BadgeElement
import com.example.data.model.CallState
import com.example.data.model.FamilyMemberId
import com.example.data.model.UserSessionState
import com.example.data.model.haloColor
import com.example.ui.components.ClayActionButton
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.components.FamilyMemberAvatar
import com.example.ui.theme.CoralSalmon
import com.example.ui.theme.CrimsonDecline
import com.example.ui.theme.ElectricRocket
import com.example.ui.theme.EmeraldAccept
import com.example.ui.theme.LocalKinNestAccessibility
import com.example.ui.theme.MintTreehouseDeep
import com.example.ui.theme.PurpleChest
import com.example.ui.theme.StudioTeal
import com.example.ui.theme.clayPressScale
import com.example.ui.theme.claymorphic

@Composable
fun CallLauncherScreen(
    session: UserSessionState,
    callState: CallState,
    profiles: List<UserProfileEntity>,
    onStartThreeWayCall: () -> Unit,
    onStartOneOnOneCall: (FamilyMemberId) -> Unit,
    onCycleAudioRoute: () -> Unit,
    onTestIncomingLockCall: () -> Unit
) {
    val a11y = LocalKinNestAccessibility.current
    val self = session.activeMember ?: FamilyMemberId.MITHUN
    val otherMembers = FamilyMemberId.entries.filterNot { it == self }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Primary 3-Way Group Video Rocket Launcher
        item {
            Box(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .claymorphic(
                        cornerRadius = 32.dp,
                        surfaceColor = ElectricRocket,
                        elevation = 14.dp
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RocketLaunch,
                                contentDescription = "Rocket Call Launcher",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "3-Way Family Call",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = "LiveKit SFU • H.264/VP9 • Opus AEC/NS/AGC",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                        }
                    }

                    ClayActionButton(
                        text = "Launch 3-Way Video Now",
                        containerColor = EmeraldAccept,
                        icon = Icons.Rounded.Groups,
                        testTag = "launch_3way_call_button",
                        modifier = Modifier.fillMaxWidth(),
                        minHeight = 62.dp,
                        onClick = onStartThreeWayCall
                    )
                }
            }
        }

        // Quick 1-on-1 Call Cards
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EditorialSectionHeader(
                    eyebrow = "Direct Family Channels",
                    title = "1-on-1 Quick Calls",
                    accentColor = CoralSalmon
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    otherMembers.forEach { target ->
                        val prof = profiles.firstOrNull { it.memberId == target.name }
                        val badge = runCatching {
                            BadgeElement.valueOf(prof?.badgeElement ?: BadgeElement.STARLIGHT.name)
                        }.getOrDefault(BadgeElement.STARLIGHT)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clayPressScale(reducedMotion = a11y.reducedMotion)
                                .claymorphic(
                                    cornerRadius = 26.dp,
                                    surfaceColor = MaterialTheme.colorScheme.surface,
                                    elevation = 10.dp
                                )
                                .clickable { onStartOneOnOneCall(target) }
                                .padding(14.dp)
                                .testTag("call_1on1_${target.name.lowercase()}")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FamilyMemberAvatar(
                                    member = target,
                                    customPhotoUri = prof?.customPhotoUri,
                                    badge = badge,
                                    size = 66.dp
                                )
                                Text(
                                    text = target.displayName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = target.haloColor().copy(alpha = 0.25f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.VideoCall,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Call",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Core-Telecom Audio Routing & Incoming Lock-Screen Call Preview
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ClayActionButton(
                    text = when (callState.audioRoute) {
                        AudioRoute.SPEAKER -> "Audio: Speaker"
                        AudioRoute.BLUETOOTH -> "Audio: Bluetooth"
                        AudioRoute.EARPIECE -> "Audio: Earpiece"
                    },
                    containerColor = MintTreehouseDeep,
                    icon = if (callState.audioRoute == AudioRoute.BLUETOOTH) Icons.Rounded.BluetoothAudio else Icons.Rounded.SpeakerPhone,
                    testTag = "cycle_audio_route_button",
                    modifier = Modifier.weight(1f),
                    onClick = onCycleAudioRoute
                )
                ClayActionButton(
                    text = "Test Lock Ring",
                    containerColor = PurpleChest,
                    icon = Icons.Rounded.NotificationsActive,
                    testTag = "test_incoming_call_button",
                    modifier = Modifier.weight(1f),
                    onClick = onTestIncomingLockCall
                )
            }
        }

        // Recent & Missed Call Logs
        item {
            EditorialSectionHeader(
                eyebrow = "Core-Telecom History",
                title = "Recent Family Calls",
                accentColor = StudioTeal,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            )
        }

        items(callState.recentCallLogs, key = { it.id }) { log ->
            Box(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .claymorphic(
                        cornerRadius = 22.dp,
                        surfaceColor = MaterialTheme.colorScheme.surface,
                        elevation = 6.dp
                    )
                    .clickable { onStartThreeWayCall() }
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (log.isMissed) CrimsonDecline.copy(alpha = 0.16f)
                                else EmeraldAccept.copy(alpha = 0.16f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (log.isMissed) Icons.Rounded.CallMissed else Icons.Rounded.VideoCall,
                            contentDescription = null,
                            tint = if (log.isMissed) CrimsonDecline else EmeraldAccept
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = log.participantsLabel,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${log.callTypeLabel} • ${log.timestampLabel}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = log.durationLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (log.isMissed) CrimsonDecline else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
