package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.UserProfileEntity
import com.example.data.model.BadgeElement
import com.example.data.model.FamilyMemberId
import com.example.data.model.haloColor
import com.example.ui.components.ClayActionButton
import com.example.ui.components.FamilyMemberAvatar
import com.example.ui.theme.CrimsonDecline
import com.example.ui.theme.EmeraldAccept
import com.example.ui.theme.LocalKinNestAccessibility
import com.example.ui.theme.liquidGlass

@Composable
fun IncomingCallOverlayScreen(
    caller: FamilyMemberId,
    isThreeWay: Boolean,
    profiles: List<UserProfileEntity>,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val a11y = LocalKinNestAccessibility.current
    val profile = profiles.firstOrNull { it.memberId == caller.name }
    val badge = runCatching {
        BadgeElement.valueOf(profile?.badgeElement ?: BadgeElement.STARLIGHT.name)
    }.getOrDefault(BadgeElement.STARLIGHT)
    val halo = caller.haloColor()

    val infiniteTransition = rememberInfiniteTransition(label = "ring_pulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = if (a11y.reducedMotion) 1f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        halo.copy(alpha = 0.45f),
                        Color(0xFF0F172A),
                        Color(0xFF090D16)
                    )
                )
            )
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
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.16f)
            ) {
                Text(
                    text = if (isThreeWay) "3-Way Family Video Call" else "KinNest Private Video Call",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(172.dp)
                        .graphicsLayer {
                            scaleX = ringScale
                            scaleY = ringScale
                        }
                        .clip(CircleShape)
                        .background(halo.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    FamilyMemberAvatar(
                        member = caller,
                        customPhotoUri = profile?.customPhotoUri,
                        badge = badge,
                        size = 140.dp
                    )
                }

                Text(
                    text = "${caller.displayName} (${caller.title})",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Core-Telecom Lock-Screen Wake • LiveKit SFU",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }

            // Glossy Emerald-Green Accept Pill + Pillowy Crimson Decline Button (Lower Third)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(34.dp),
                        surfaceAlpha = 0.25f,
                        tintColor = Color.White,
                        reducedMotion = a11y.reducedMotion
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ClayActionButton(
                        text = "Decline",
                        containerColor = CrimsonDecline,
                        icon = Icons.Rounded.CallEnd,
                        testTag = "incoming_decline_button",
                        modifier = Modifier.weight(1f),
                        minHeight = 66.dp,
                        onClick = onDecline
                    )
                    ClayActionButton(
                        text = "Accept",
                        containerColor = EmeraldAccept,
                        icon = Icons.Rounded.Call,
                        testTag = "incoming_accept_button",
                        modifier = Modifier.weight(1.3f),
                        minHeight = 66.dp,
                        onClick = onAccept
                    )
                }
            }
        }
    }
}
