package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.UserProfileEntity
import com.example.data.model.BadgeElement
import com.example.data.model.FamilyMemberId
import com.example.ui.components.ClayActionButton
import com.example.ui.components.ClayTextOnLight
import com.example.ui.components.FamilyMemberAvatar
import com.example.ui.components.KinNestAmbientCanvas
import com.example.ui.components.Text3D
import com.example.ui.theme.ClayAqua
import com.example.ui.theme.ClayCoral
import com.example.ui.theme.ClayDeepTealInk
import com.example.ui.theme.ClayHoney
import com.example.ui.theme.ClayMutedInk
import com.example.ui.theme.ClayPeach
import com.example.ui.theme.ClaySage
import com.example.ui.theme.ClayTealShadow
import com.example.ui.theme.ClayTextCream
import com.example.ui.theme.LocalKinNestAccessibility
import com.example.ui.theme.clayDisc3D
import com.example.ui.theme.claymorphic

@Composable
fun IncomingCallOverlayScreen(
    caller: FamilyMemberId,
    isThreeWay: Boolean,
    videoEnabled: Boolean,
    profiles: List<UserProfileEntity>,
    onAccept: () -> Unit,
    onAcceptVoiceOnly: () -> Unit,
    onDecline: () -> Unit
) {
    val a11y = LocalKinNestAccessibility.current
    val profile = profiles.firstOrNull { it.memberId == caller.name }
    val badge = runCatching {
        BadgeElement.valueOf(profile?.badgeElement ?: BadgeElement.STARLIGHT.name)
    }.getOrDefault(BadgeElement.STARLIGHT)

    val infiniteTransition = rememberInfiniteTransition(label = "ring_pulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = if (a11y.reducedMotion) 1f else 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_pulse_scale"
    )

    val callTypePillText = when {
        isThreeWay && videoEnabled -> "3-Way Video Call"
        isThreeWay && !videoEnabled -> "3-Way Voice Call"
        videoEnabled -> "Incoming Video Call"
        else -> "Incoming Voice Call"
    }

    KinNestAmbientCanvas(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .claymorphic(
                            cornerRadius = 22.dp,
                            surfaceColor = if (videoEnabled) ClayHoney else ClayAqua,
                            elevation = 10.dp,
                            shadowTint = ClayTealShadow,
                            ledgeDepth = 4.dp
                        )
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (videoEnabled) Icons.Rounded.Videocam else Icons.Rounded.Call,
                        contentDescription = null,
                        tint = ClayTextCream,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text3D(
                        text = callTypePillText,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        depthDp = 1.5.dp
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(182.dp)
                            .graphicsLayer {
                                scaleX = ringScale
                                scaleY = ringScale
                            }
                            .clayDisc3D(
                                surfaceColor = ClayPeach,
                                elevation = 18.dp,
                                shadowTint = ClayTealShadow,
                                ledgeDepth = 7.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        FamilyMemberAvatar(
                            member = caller,
                            customPhotoUri = profile?.customPhotoUri,
                            badge = badge,
                            size = 142.dp
                        )
                    }

                    ClayTextOnLight(
                        text = "${caller.displayName} (${caller.title})",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        textColor = ClayDeepTealInk,
                        depthDp = 0.8.dp,
                        textAlign = TextAlign.Center
                    )
                    ClayTextOnLight(
                        text = if (videoEnabled) {
                            "Video calling on Holy Moly ! • Tap Accept to join"
                        } else {
                            "Voice calling on Holy Moly ! • Tap Accept to talk"
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        textColor = ClayMutedInk,
                        depthDp = 0.dp,
                        textAlign = TextAlign.Center
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (videoEnabled) {
                        ClayActionButton(
                            text = "Answer Voice Only",
                            containerColor = ClayAqua,
                            icon = Icons.Rounded.Call,
                            testTag = "incoming_accept_voice_only_button",
                            modifier = Modifier.fillMaxWidth(0.75f),
                            minHeight = 50.dp,
                            onClick = onAcceptVoiceOnly
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ClayActionButton(
                            text = "Decline",
                            containerColor = ClayCoral,
                            icon = Icons.Rounded.CallEnd,
                            testTag = "incoming_decline_button",
                            modifier = Modifier.weight(1f),
                            minHeight = 62.dp,
                            onClick = onDecline
                        )
                        ClayActionButton(
                            text = if (videoEnabled) "Accept Video" else "Accept Voice",
                            containerColor = ClaySage,
                            icon = if (videoEnabled) Icons.Rounded.Videocam else Icons.Rounded.Call,
                            testTag = "incoming_accept_button",
                            modifier = Modifier.weight(1.25f),
                            minHeight = 62.dp,
                            onClick = onAccept
                        )
                    }
                }
            }
        }
    }
}
