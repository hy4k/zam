package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.UserProfileEntity
import com.example.data.model.BadgeElement
import com.example.data.model.FamilyMemberId
import com.example.data.model.haloColor
import com.example.ui.WorkspaceTab
import com.example.ui.components.ClayActionButton
import com.example.ui.components.ComicStoryboardBanner
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.components.FamilyMemberAvatar
import com.example.ui.components.LandingBottomMenuBar
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AvatarLoginScreen(
    profiles: List<UserProfileEntity>,
    onAvatarTap: (FamilyMemberId) -> Unit,
    onQuickEnterTab: (WorkspaceTab) -> Unit,
    onCustomizeProfile: (FamilyMemberId, BadgeElement, String?) -> Unit,
    onOpenPermissions: () -> Unit
) {
    val a11y = LocalKinNestAccessibility.current
    var customizingMember by remember { mutableStateOf<FamilyMemberId?>(null) }

    val mithunProfile = profiles.firstOrNull { it.memberId == FamilyMemberId.MITHUN.name }
    val mafithaProfile = profiles.firstOrNull { it.memberId == FamilyMemberId.MAFITHA.name }
    val zaynProfile = profiles.firstOrNull { it.memberId == FamilyMemberId.ZAYN.name }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            LandingBottomMenuBar(onSelectWorkspaceTab = onQuickEnterTab)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Editorial Studio Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PRIVATE FAMILY COMICVERSE • VOL. 03",
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioTealDeep
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "KinNest",
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(CoralSalmon)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .clayPressScale(reducedMotion = a11y.reducedMotion)
                            .liquidGlass(
                                shape = RoundedCornerShape(24.dp),
                                surfaceAlpha = 0.72f,
                                tintColor = MaterialTheme.colorScheme.surface,
                                reducedMotion = a11y.reducedMotion,
                                cornerRadius = 24.dp
                            )
                            .clickable(onClick = onOpenPermissions)
                            .padding(horizontal = 14.dp)
                            .testTag("login_permissions_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Permissions and hardware check",
                                tint = StudioTealDeep,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "KEYS & PERMS",
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioTealDeep
                            )
                        }
                    }
                }

                // 2. Elegant Comic-World Storyboard Showcase Panel
                ComicStoryboardBanner(
                    issueBadge = "ISSUE № 03 • STORYVERSE",
                    secondaryBadge = "E2EE VAULT",
                    headline = "The Mithun, Mafitha & Zayn Saga",
                    caption = "Ligne-claire family universe • LiveKit 3-way ready",
                    leadingIcon = Icons.Rounded.RocketLaunch,
                    height = 212.dp
                )

            // 3. Structured Bento Profile Portal
            EditorialSectionHeader(
                eyebrow = "Device-Bound Access",
                title = "Choose Your Profile",
                accentColor = CoralSalmon
            )

            // 2-Column Asymmetric Parent Studio Tiles (Mithun + Mafitha)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ParentBentoAvatarCard(
                    member = FamilyMemberId.MITHUN,
                    profile = mithunProfile,
                    cardColor = Color(0xFFDCEFFF),
                    accentPillColor = StudioTealDeep,
                    reducedMotion = a11y.reducedMotion,
                    modifier = Modifier.weight(1f),
                    onTap = { onAvatarTap(FamilyMemberId.MITHUN) },
                    onCustomize = { customizingMember = FamilyMemberId.MITHUN }
                )
                ParentBentoAvatarCard(
                    member = FamilyMemberId.MAFITHA,
                    profile = mafithaProfile,
                    cardColor = CoralSalmonSoft,
                    accentPillColor = CoralSalmonDeep,
                    reducedMotion = a11y.reducedMotion,
                    modifier = Modifier.weight(1f),
                    onTap = { onAvatarTap(FamilyMemberId.MAFITHA) },
                    onCustomize = { customizingMember = FamilyMemberId.MAFITHA }
                )
            }

            // Full-Width Lower-Third Kid Explorer Hero Card (Zayn Adam)
            ZaynHeroAvatarCard(
                profile = zaynProfile,
                reducedMotion = a11y.reducedMotion,
                onTap = { onAvatarTap(FamilyMemberId.ZAYN) },
                onCustomize = { customizingMember = FamilyMemberId.ZAYN }
            )

            // 4. Structured Glassmorphic Architecture Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(24.dp),
                        surfaceAlpha = 0.65f,
                        tintColor = MaterialTheme.colorScheme.surface,
                        reducedMotion = a11y.reducedMotion,
                        cornerRadius = 24.dp
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LandingTelemetryColumn("VIDEO", "LiveKit SFU")
                LandingTelemetryColumn("SECURITY", "Keystore ECDSA")
                LandingTelemetryColumn("STORAGE", "Offline Room")
            }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // Personalization Bottom Sheet
    customizingMember?.let { targetMember ->
        val existing = profiles.firstOrNull { it.memberId == targetMember.name }
        var selectedBadge by remember(targetMember) {
            mutableStateOf(
                runCatching {
                    BadgeElement.valueOf(existing?.badgeElement ?: BadgeElement.STARLIGHT.name)
                }.getOrDefault(BadgeElement.STARLIGHT)
            )
        }
        var pickedPhotoUri by remember(targetMember) { mutableStateOf(existing?.customPhotoUri) }

        val photoPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            uri?.let { pickedPhotoUri = it.toString() }
        }

        ModalBottomSheet(onDismissRequest = { customizingMember = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Style ${targetMember.displayName}'s Halo",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                FamilyMemberAvatar(
                    member = targetMember,
                    customPhotoUri = pickedPhotoUri,
                    badge = selectedBadge,
                    size = 92.dp
                )
                Spacer(modifier = Modifier.height(14.dp))

                ClayActionButton(
                    text = "Pick Custom Photo",
                    containerColor = targetMember.haloColor(),
                    contentColor = Color(0xFF15181C),
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
                    text = "Save Look",
                    containerColor = StudioTealDeep,
                    testTag = "save_avatar_style_button",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onCustomizeProfile(targetMember, selectedBadge, pickedPhotoUri)
                        customizingMember = null
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ParentBentoAvatarCard(
    member: FamilyMemberId,
    profile: UserProfileEntity?,
    cardColor: Color,
    accentPillColor: Color,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onCustomize: () -> Unit
) {
    val badge = runCatching {
        BadgeElement.valueOf(profile?.badgeElement ?: BadgeElement.STARLIGHT.name)
    }.getOrDefault(BadgeElement.STARLIGHT)
    val isTrusted = profile?.isDeviceTrusted ?: true
    val fp = profile?.devicePublicFingerprint?.take(11) ?: "HW-BOUND"

    Box(
        modifier = modifier
            .clayPressScale(reducedMotion = reducedMotion)
            .claymorphic(
                cornerRadius = 28.dp,
                surfaceColor = cardColor,
                elevation = 12.dp
            )
            .clickable(onClick = onTap)
            .padding(14.dp)
            .testTag("avatar_login_${member.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
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

                IconButton(
                    onClick = onCustomize,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("customize_${member.name.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.78f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Customize ${member.displayName}",
                            tint = Color(0xFF15181C),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            FamilyMemberAvatar(
                member = member,
                customPhotoUri = profile?.customPhotoUri,
                badge = badge,
                size = 74.dp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = member.displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF15181C),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isTrusted) Icons.Rounded.VerifiedUser else Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = if (isTrusted) StudioTealDeep else CoralSalmonDeep,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isTrusted) fp else "Untrusted",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isTrusted) StudioTealDeep else CoralSalmonDeep,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ZaynHeroAvatarCard(
    profile: UserProfileEntity?,
    reducedMotion: Boolean,
    onTap: () -> Unit,
    onCustomize: () -> Unit
) {
    val member = FamilyMemberId.ZAYN
    val badge = runCatching {
        BadgeElement.valueOf(profile?.badgeElement ?: BadgeElement.ROCKET.name)
    }.getOrDefault(BadgeElement.ROCKET)
    val isTrusted = profile?.isDeviceTrusted ?: true

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
                            color = Color.White,
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
                    color = Color(0xFF15181C)
                )
                Text(
                    text = if (isTrusted) "Tap anywhere to enter Storyverse" else "Untrusted device — blocked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isTrusted) Color(0xFF5C4410) else CoralSalmonDeep,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onCustomize,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("customize_zayn")
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.82f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Customize Zayn Adam",
                            tint = Color(0xFF15181C),
                            modifier = Modifier.size(16.dp)
                        )
                    }
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
                        contentDescription = "Enter as Zayn Adam",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun LandingTelemetryColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = StudioTealDeep
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
