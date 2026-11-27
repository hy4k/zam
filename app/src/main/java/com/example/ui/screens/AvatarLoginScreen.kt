package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.UserProfileEntity
import com.example.data.model.BadgeElement
import com.example.data.model.FamilyMemberId
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.components.FamilyMemberAvatar
import com.example.ui.theme.CoralSalmon
import com.example.ui.theme.CoralSalmonDeep
import com.example.ui.theme.CoralSalmonSoft
import com.example.ui.theme.LocalKinNestAccessibility
import com.example.ui.theme.OchreGold
import com.example.ui.theme.OchreGoldSoft
import com.example.ui.theme.StudioTealDeep
import com.example.ui.theme.StudioTealSoft
import com.example.ui.theme.clayPressScale
import com.example.ui.theme.claymorphic
import com.example.ui.theme.liquidGlass

/**
 * One-Time Phone Owner Setup Screen:
 * Shown ONLY on first launch when this phone has not yet been assigned to Mithun (Vapi),
 * Mafitha (Ummi), or Zayn Adam. Once confirmed, this phone always opens straight into A12 E
 * as that family member — zero passwords, zero accidental profile switching.
 */
@Composable
fun AvatarLoginScreen(
    profiles: List<UserProfileEntity>,
    onConfirmDeviceOwner: (FamilyMemberId) -> Unit
) {
    val a11y = LocalKinNestAccessibility.current
    var pendingMember by remember { mutableStateOf<FamilyMemberId?>(null) }

    val mithunProfile = profiles.firstOrNull { it.memberId == FamilyMemberId.MITHUN.name }
    val mafithaProfile = profiles.firstOrNull { it.memberId == FamilyMemberId.MAFITHA.name }
    val zaynProfile = profiles.firstOrNull { it.memberId == FamilyMemberId.ZAYN.name }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_holymoly_launcher_1795535055625),
                    contentDescription = "Holy Moly ! app logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, Color.White, RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ONE-TIME PHONE SETUP",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioTealDeep
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Holy Moly !",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(CoralSalmon)
                        )
                    }
                }
            }

            // 2. Friendly Equipment-Assigned Explanation Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(26.dp),
                        surfaceAlpha = 0.82f,
                        tintColor = MaterialTheme.colorScheme.surface,
                        reducedMotion = a11y.reducedMotion,
                        cornerRadius = 26.dp
                    )
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(StudioTealSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhoneAndroid,
                            contentDescription = "This phone",
                            tint = StudioTealDeep,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Set this phone once",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pick whose phone this is. It will always stay signed in as you automatically — no logins or accidental switching.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3. Family Member Selection
            EditorialSectionHeader(
                eyebrow = "Assign This Phone",
                title = "Whose Phone Is This?",
                accentColor = CoralSalmonDeep
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ParentSetupCard(
                    member = FamilyMemberId.MITHUN,
                    profile = mithunProfile,
                    cardColor = Color(0xFFDCEFFF),
                    accentPillColor = StudioTealDeep,
                    reducedMotion = a11y.reducedMotion,
                    modifier = Modifier.weight(1f),
                    onTap = { pendingMember = FamilyMemberId.MITHUN }
                )
                ParentSetupCard(
                    member = FamilyMemberId.MAFITHA,
                    profile = mafithaProfile,
                    cardColor = CoralSalmonSoft,
                    accentPillColor = CoralSalmonDeep,
                    reducedMotion = a11y.reducedMotion,
                    modifier = Modifier.weight(1f),
                    onTap = { pendingMember = FamilyMemberId.MAFITHA }
                )
            }

            ZaynSetupCard(
                profile = zaynProfile,
                reducedMotion = a11y.reducedMotion,
                onTap = { pendingMember = FamilyMemberId.ZAYN }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    pendingMember?.let { selected ->
        AlertDialog(
            onDismissRequest = { pendingMember = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.VerifiedUser,
                    contentDescription = null,
                    tint = StudioTealDeep
                )
            },
            title = {
                Text("Assign phone to ${selected.displayName}?")
            },
            text = {
                Text(
                    "This phone will always open directly as ${selected.displayName} (${selected.title}). You can change this anytime from Settings."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingMember = null
                        onConfirmDeviceOwner(selected)
                    },
                    modifier = Modifier.testTag("confirm_device_owner_button")
                ) {
                    Text(
                        text = "Yes, this is my phone",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingMember = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ParentSetupCard(
    member: FamilyMemberId,
    profile: UserProfileEntity?,
    cardColor: Color,
    accentPillColor: Color,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    val badge = runCatching {
        BadgeElement.valueOf(profile?.badgeElement ?: BadgeElement.STARLIGHT.name)
    }.getOrDefault(BadgeElement.STARLIGHT)

    Box(
        modifier = modifier
            .clayPressScale(reducedMotion = reducedMotion)
            .claymorphic(
                cornerRadius = 28.dp,
                surfaceColor = cardColor,
                elevation = 12.dp
            )
            .clickable(onClick = onTap)
            .padding(16.dp)
            .testTag("avatar_login_${member.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentPillColor
            ) {
                Text(
                    text = member.title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            FamilyMemberAvatar(
                member = member,
                customPhotoUri = profile?.customPhotoUri,
                badge = badge,
                size = 76.dp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = member.displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF0D1B2A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Tap to assign this phone",
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioTealDeep,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ZaynSetupCard(
    profile: UserProfileEntity?,
    reducedMotion: Boolean,
    onTap: () -> Unit
) {
    val member = FamilyMemberId.ZAYN
    val badge = runCatching {
        BadgeElement.valueOf(profile?.badgeElement ?: BadgeElement.ROCKET.name)
    }.getOrDefault(BadgeElement.ROCKET)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clayPressScale(reducedMotion = reducedMotion)
            .claymorphic(
                cornerRadius = 30.dp,
                surfaceColor = OchreGoldSoft,
                elevation = 14.dp
            )
            .clickable(onClick = onTap)
            .padding(16.dp)
            .testTag("avatar_login_zayn")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FamilyMemberAvatar(
                member = member,
                customPhotoUri = profile?.customPhotoUri,
                badge = badge,
                size = 84.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = OchreGold
                    ) {
                        Text(
                            text = "6YO EXPLORER",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF0D1B2A),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StudioTealSoft
                    ) {
                        Text(
                            text = "EN • മലയാളം",
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioTealDeep,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = member.displayName,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color(0xFF0D1B2A)
                )
                Text(
                    text = "Tap if this phone or tablet belongs to Zayn!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5C4410),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .claymorphic(
                        cornerRadius = 24.dp,
                        surfaceColor = StudioTealDeep,
                        elevation = 8.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Assign to Zayn Adam",
                    tint = Color.White
                )
            }
        }
    }
}
