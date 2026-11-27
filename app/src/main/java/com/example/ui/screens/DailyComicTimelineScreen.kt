package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.ai.ComicPhotoStyle
import com.example.ai.DailyComicGenerationState
import com.example.ai.DailyTurnSchedule
import com.example.data.local.DailyComicPhotoEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.BadgeElement
import com.example.data.model.FamilyMemberId
import com.example.data.model.haloColor
import com.example.ui.components.ClayActionButton
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.components.FamilyMemberAvatar
import com.example.ui.theme.CitrusLime
import com.example.ui.theme.CoralSalmon
import com.example.ui.theme.CoralSalmonDeep
import com.example.ui.theme.EmeraldAccept
import com.example.ui.theme.LocalKinNestAccessibility
import com.example.ui.theme.OchreGold
import com.example.ui.theme.OchreGoldBright
import com.example.ui.theme.OchreGoldSoft
import com.example.ui.theme.PeriwinklePop
import com.example.ui.theme.StudioTealDeep
import com.example.ui.theme.StudioTealSoft
import com.example.ui.theme.clayPressScale
import com.example.ui.theme.claymorphic
import java.io.File

private fun createTempCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera_captures").apply { mkdirs() }
    val photoFile = File(dir, "daily_snap_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )
}

/**
 * Reusable Hero Card shown on the Front Page & Home Screen (replaces Family Storyverse & 3D Worlds):
 * - Displays today's comic photo if posted, OR today's turn with direct Camera & Upload options.
 */
@Composable
fun DailyComicFeatureCard(
    todaysPhoto: DailyComicPhotoEntity?,
    todaysTurnMember: FamilyMemberId,
    activeMember: FamilyMemberId?,
    profiles: List<UserProfileEntity>,
    totalPostsCount: Int,
    onTakePhotoClick: () -> Unit,
    onPickPhotoClick: () -> Unit,
    onOpenTimeline: () -> Unit,
    onNudgeTurnMember: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val a11y = LocalKinNestAccessibility.current
    val turnProfile = profiles.firstOrNull { it.memberId == todaysTurnMember.name }
    val turnBadge = runCatching {
        BadgeElement.valueOf(turnProfile?.badgeElement ?: BadgeElement.ROCKET.name)
    }.getOrDefault(BadgeElement.ROCKET)

    // Allow direct posting when it's your own turn OR when a parent helps 6yo Zayn on their phone (without switching logins!)
    val isHelpingZayn = todaysTurnMember == FamilyMemberId.ZAYN && activeMember != null && !activeMember.isChild
    val canPostDirectly = activeMember == todaysTurnMember || isHelpingZayn

    Box(
        modifier = modifier
            .fillMaxWidth()
            .claymorphic(
                cornerRadius = 30.dp,
                surfaceColor = Color(0xFFFFFBF2),
                elevation = 14.dp
            )
            .border(2.dp, Color(0xFF15181C), RoundedCornerShape(30.dp))
            .padding(14.dp)
            .testTag("daily_comic_feature_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Top Comic Strip Masthead Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CitrusLime,
                    modifier = Modifier.border(1.4.dp, Color(0xFF15181C), RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "DAILY FAMILY COMIC",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF15181C),
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = OchreGoldBright,
                    modifier = Modifier
                        .border(1.4.dp, Color(0xFF15181C), RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenTimeline)
                        .testTag("daily_comic_open_timeline_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Collections,
                            contentDescription = "Open family timeline",
                            tint = Color(0xFF15181C),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (totalPostsCount > 0) "TIMELINE ($totalPostsCount)" else "TIMELINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF15181C),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (todaysPhoto != null) {
                // Today's Comic Photo is live! Show it inside a classic comic panel frame
                val author = runCatching { FamilyMemberId.valueOf(todaysPhoto.authorId) }
                    .getOrDefault(todaysTurnMember)
                val nextUp = DailyTurnSchedule.nextTurnMember(todaysTurnMember)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(216.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .border(2.dp, Color(0xFF15181C), RoundedCornerShape(22.dp))
                        .clickable(onClick = onOpenTimeline)
                        .testTag("daily_comic_today_panel")
                ) {
                    AsyncImage(
                        model = File(todaysPhoto.localImagePath),
                        contentDescription = "Today's family comic photo by ${author.displayName}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Transparent, Color(0xCC111827))
                                )
                            )
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldAccept,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .border(1.2.dp, Color(0xFF15181C), RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = "TODAY'S SNAP • ${author.displayName.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        if (todaysPhoto.caption.isNotBlank()) {
                            Text(
                                text = "“${todaysPhoto.caption}”",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "Tomorrow's turn: ${nextUp.displayName} (${nextUp.title}) • Tap to see all days",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                // Today's Turn Waiting Panel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    todaysTurnMember.haloColor().copy(alpha = 0.22f),
                                    OchreGoldSoft
                                )
                            )
                        )
                        .border(1.5.dp, Color(0xFF15181C), RoundedCornerShape(22.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FamilyMemberAvatar(
                        member = todaysTurnMember,
                        customPhotoUri = turnProfile?.customPhotoUri,
                        badge = turnBadge,
                        size = 66.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "TODAY'S TURN • 1 COMIC PHOTO A DAY",
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioTealDeep,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${todaysTurnMember.displayName} (${todaysTurnMember.title})",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF15181C)
                        )
                        Text(
                            text = when {
                                activeMember == todaysTurnMember -> "Your turn! Take a photo or upload one — it becomes a comic page!"
                                isHelpingZayn -> "It's Zayn's turn! Snap or upload his photo right here."
                                else -> "Waiting for ${todaysTurnMember.displayName} to post today's comic photo!"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF334155)
                        )
                    }
                }

                // Direct Action Buttons right inside the card!
                if (canPostDirectly) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ClayActionButton(
                            text = "Take Photo",
                            containerColor = StudioTealDeep,
                            icon = Icons.Rounded.CameraAlt,
                            testTag = "daily_comic_camera_button",
                            modifier = Modifier.weight(1f),
                            minHeight = 52.dp,
                            onClick = onTakePhotoClick
                        )
                        ClayActionButton(
                            text = "Upload Photo",
                            containerColor = CoralSalmon,
                            icon = Icons.Rounded.PhotoLibrary,
                            testTag = "daily_comic_upload_button",
                            modifier = Modifier.weight(1f),
                            minHeight = 52.dp,
                            onClick = onPickPhotoClick
                        )
                    }
                } else if (onNudgeTurnMember != null) {
                    ClayActionButton(
                        text = "Nudge ${todaysTurnMember.displayName} (${todaysTurnMember.title})",
                        containerColor = CoralSalmon,
                        icon = Icons.Rounded.NotificationsActive,
                        testTag = "daily_comic_nudge_button",
                        modifier = Modifier.fillMaxWidth(),
                        minHeight = 50.dp,
                        onClick = onNudgeTurnMember
                    )
                }
            }

            // Compact 3-Person Turn Indicator Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DailyTurnSchedule.orderedTurnLineup().forEach { member ->
                    val isToday = member == todaysTurnMember
                    val postedToday = isToday && todaysPhoto != null
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            postedToday -> EmeraldAccept.copy(alpha = 0.16f)
                            isToday -> member.haloColor().copy(alpha = 0.28f)
                            else -> Color(0xFFF1F5F9)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clayPressScale(reducedMotion = a11y.reducedMotion)
                            .border(
                                width = if (isToday) 1.5.dp else 1.dp,
                                color = if (isToday) Color(0xFF15181C) else Color(0xFFCBD5E1),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable(onClick = onOpenTimeline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (postedToday) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Posted today",
                                    tint = EmeraldAccept,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = if (isToday) "★ ${member.title}" else member.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF15181C),
                                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full Communal Family Daily Comic Photo Timeline & Studio:
 * - Enforces 1 shared comic-styled photo per day on a rotating Vapi / Ummi / Zayn turn schedule.
 * - Supports live camera capture OR uploading a previous photo from the phone gallery.
 * - Transforms the photo into a comic-book illustration before posting to the family timeline.
 */
@Composable
fun DailyComicTimelineScreen(
    initialLaunchMode: String,
    activeMember: FamilyMemberId,
    todaysTurnMember: FamilyMemberId,
    todaysDateKey: String,
    profiles: List<UserProfileEntity>,
    photos: List<DailyComicPhotoEntity>,
    generationState: DailyComicGenerationState,
    onPostComicPhoto: (sourceUri: Uri, caption: String, style: ComicPhotoStyle, author: FamilyMemberId) -> Unit,
    onReactPhoto: (dateKey: String, emoji: String) -> Unit,
    onNudgeTurnMember: (FamilyMemberId) -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val todaysPhoto = remember(photos, todaysDateKey) {
        photos.firstOrNull { it.dateKey == todaysDateKey }
    }
    val isHelpingZayn = todaysTurnMember == FamilyMemberId.ZAYN && !activeMember.isChild
    val canPostToday = activeMember == todaysTurnMember || isHelpingZayn
    val postAuthor = if (isHelpingZayn) FamilyMemberId.ZAYN else activeMember

    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraOutputUri by remember { mutableStateOf<Uri?>(null) }
    var captionDraft by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf(ComicPhotoStyle.SUNNY_COMIC) }
    var fullscreenPhoto by remember { mutableStateOf<DailyComicPhotoEntity?>(null) }
    var autoLaunched by remember(initialLaunchMode) { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraOutputUri != null) {
            pendingPhotoUri = tempCameraOutputUri
            onClearError()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val outUri = createTempCameraUri(context)
            tempCameraOutputUri = outUri
            cameraLauncher.launch(outUri)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pendingPhotoUri = uri
            onClearError()
        }
    }

    fun launchCameraNow() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            val outUri = createTempCameraUri(context)
            tempCameraOutputUri = outUri
            cameraLauncher.launch(outUri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun launchGalleryNow() {
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // Clear draft once today's photo succeeds and lands in timeline
    LaunchedEffect(todaysPhoto?.id) {
        if (todaysPhoto != null) {
            pendingPhotoUri = null
            captionDraft = ""
        }
    }

    // Auto-open camera or gallery if user tapped Take Photo / Upload Photo from Home
    LaunchedEffect(initialLaunchMode, canPostToday, todaysPhoto) {
        if (!autoLaunched && todaysPhoto == null && canPostToday) {
            autoLaunched = true
            when (initialLaunchMode) {
                "camera" -> launchCameraNow()
                "gallery" -> launchGalleryNow()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Bar
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .testTag("daily_comic_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SHARED FAMILY TIMELINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioTealDeep
                    )
                    Text(
                        text = "Daily Family Comic",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Daily Turn Wheel Card
        item {
            Box(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .claymorphic(
                        cornerRadius = 28.dp,
                        surfaceColor = Color(0xFFFFFBF2),
                        elevation = 10.dp
                    )
                    .border(2.dp, Color(0xFF15181C), RoundedCornerShape(28.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = DailyTurnSchedule.formatFriendlyDate(todaysDateKey),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF15181C)
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (todaysPhoto != null) EmeraldAccept else OchreGoldBright,
                            modifier = Modifier.border(1.2.dp, Color(0xFF15181C), RoundedCornerShape(10.dp))
                        ) {
                            Text(
                                text = if (todaysPhoto != null) "TODAY DONE!" else "${todaysTurnMember.title.uppercase()}'S TURN",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (todaysPhoto != null) Color.White else Color(0xFF15181C),
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DailyTurnSchedule.orderedTurnLineup().forEach { member ->
                            val isToday = member == todaysTurnMember
                            val prof = profiles.firstOrNull { it.memberId == member.name }
                            val badge = runCatching {
                                BadgeElement.valueOf(prof?.badgeElement ?: BadgeElement.STARLIGHT.name)
                            }.getOrDefault(BadgeElement.STARLIGHT)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isToday) 3.dp else 0.dp,
                                            color = if (isToday) Color(0xFF15181C) else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .padding(2.dp)
                                ) {
                                    FamilyMemberAvatar(
                                        member = member,
                                        customPhotoUri = prof?.customPhotoUri,
                                        badge = badge,
                                        size = if (isToday) 64.dp else 52.dp
                                    )
                                }
                                Text(
                                    text = member.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF15181C),
                                    fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium
                                )
                                Text(
                                    text = when {
                                        isToday && todaysPhoto != null -> "Posted!"
                                        isToday -> "Today's Turn"
                                        member == DailyTurnSchedule.nextTurnMember(todaysTurnMember) -> "Tomorrow"
                                        else -> "Next Up"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isToday) StudioTealDeep else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Today's Photo Capture & Comic Stylizer Studio (if today's photo hasn't been posted yet)
        if (todaysPhoto == null) {
            item {
                Box(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .claymorphic(
                            cornerRadius = 28.dp,
                            surfaceColor = MaterialTheme.colorScheme.surface,
                            elevation = 12.dp
                        )
                        .border(2.dp, Color(0xFF15181C), RoundedCornerShape(28.dp))
                        .padding(16.dp)
                        .testTag("daily_comic_studio_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (!canPostToday) {
                            Text(
                                text = "It's ${todaysTurnMember.displayName}'s turn today!",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "One family member posts today's comic photo so everyone gets a turn. Send ${todaysTurnMember.displayName} a quick ping on their phone!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ClayActionButton(
                                text = "Nudge ${todaysTurnMember.displayName} (${todaysTurnMember.title})",
                                containerColor = CoralSalmon,
                                icon = Icons.Rounded.NotificationsActive,
                                testTag = "timeline_nudge_button",
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onNudgeTurnMember(todaysTurnMember) }
                            )
                        } else if (generationState is DailyComicGenerationState.Loading) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = StudioTealDeep,
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Drawing your ${generationState.styleLabel} page…",
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Adding bold ink lines & comic colors for Mithun, Mafitha & Zayn! This usually takes 20–40 seconds.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else if (pendingPhotoUri == null) {
                            Text(
                                text = if (isHelpingZayn) {
                                    "Helping Zayn Post Today! 📸"
                                } else {
                                    "Your Turn, ${activeMember.displayName}! 📸"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Snap a picture right here or pick one from your gallery. We'll turn it into a colorful comic book panel for our family timeline!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ClayActionButton(
                                    text = "Take Photo",
                                    containerColor = StudioTealDeep,
                                    icon = Icons.Rounded.CameraAlt,
                                    testTag = "studio_take_photo_button",
                                    modifier = Modifier.weight(1f),
                                    onClick = { launchCameraNow() }
                                )
                                ClayActionButton(
                                    text = "Upload Photo",
                                    containerColor = CoralSalmon,
                                    icon = Icons.Rounded.PhotoLibrary,
                                    testTag = "studio_upload_photo_button",
                                    modifier = Modifier.weight(1f),
                                    onClick = { launchGalleryNow() }
                                )
                            }
                        } else {
                            // Photo selected! Preview + Comic Style + Caption + Transform & Post
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Ready to Comic-ify!",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = StudioTealSoft,
                                    modifier = Modifier.clickable {
                                        pendingPhotoUri = null
                                        onClearError()
                                    }
                                ) {
                                    Text(
                                        text = "Change Photo",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = StudioTealDeep,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(22.dp))
                                    .border(2.dp, Color(0xFF15181C), RoundedCornerShape(22.dp))
                            ) {
                                AsyncImage(
                                    model = pendingPhotoUri,
                                    contentDescription = "Selected photo preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Text(
                                text = "PICK YOUR COMIC LOOK",
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioTealDeep
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ComicPhotoStyle.entries.forEach { style ->
                                    FilterChip(
                                        selected = selectedStyle == style,
                                        onClick = { selectedStyle = style },
                                        label = { Text("${style.emoji} ${style.label}") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = captionDraft,
                                onValueChange = { captionDraft = it.take(120) },
                                label = { Text("Fun comic caption (optional)") },
                                placeholder = { Text("e.g. Zayn's giant ice cream grin!") },
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("daily_comic_caption_input")
                            )

                            AnimatedVisibility(visible = generationState is DailyComicGenerationState.Error) {
                                val errMsg = (generationState as? DailyComicGenerationState.Error)?.message.orEmpty()
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = CoralSalmonDeep.copy(alpha = 0.14f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = errMsg,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = CoralSalmonDeep,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            ClayActionButton(
                                text = if (generationState is DailyComicGenerationState.Error) {
                                    "Try Again • Make Comic"
                                } else {
                                    "Turn Into Comic & Post!"
                                },
                                containerColor = EmeraldAccept,
                                icon = if (generationState is DailyComicGenerationState.Error) {
                                    Icons.Rounded.Refresh
                                } else {
                                    Icons.Rounded.AutoAwesome
                                },
                                testTag = "daily_comic_submit_button",
                                modifier = Modifier.fillMaxWidth(),
                                minHeight = 58.dp,
                                onClick = {
                                    pendingPhotoUri?.let { uri ->
                                        onPostComicPhoto(
                                            uri,
                                            captionDraft.trim(),
                                            selectedStyle,
                                            postAuthor
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Communal Family Timeline Header
        item {
            EditorialSectionHeader(
                eyebrow = "Our Family Comic Book",
                title = "Shared Daily Timeline",
                accentColor = CoralSalmon,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            )
        }

        if (photos.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .claymorphic(
                            cornerRadius = 26.dp,
                            surfaceColor = Color(0xFFFFFBF2),
                            elevation = 8.dp
                        )
                        .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(26.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "💥📸", fontSize = 42.sp)
                        Text(
                            text = "Issue #1 is waiting!",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF15181C)
                        )
                        Text(
                            text = "Every day, Vapi, Ummi, and Zayn take turns adding one comic photo to our family storybook.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            itemsIndexed(photos, key = { _, item -> item.dateKey }) { idx, photo ->
                val issueNumber = photos.size - idx
                ComicTimelineEntryCard(
                    photo = photo,
                    issueNumber = issueNumber,
                    profiles = profiles,
                    onReact = { emoji -> onReactPhoto(photo.dateKey, emoji) },
                    onOpenFullScreen = { fullscreenPhoto = photo },
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                )
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    // Full-Screen Comic Viewer Dialog
    fullscreenPhoto?.let { photo ->
        val author = runCatching { FamilyMemberId.valueOf(photo.authorId) }
            .getOrDefault(FamilyMemberId.ZAYN)
        Dialog(
            onDismissRequest = { fullscreenPhoto = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF00B1220))
                    .clickable { fullscreenPhoto = null }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.widthIn(max = 560.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${DailyTurnSchedule.formatFriendlyDate(photo.dateKey)} • ${author.displayName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        IconButton(
                            onClick = { fullscreenPhoto = null },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close viewer",
                                tint = Color.White
                            )
                        }
                    }
                    AsyncImage(
                        model = File(photo.localImagePath),
                        contentDescription = "Full comic photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .border(2.5.dp, Color.White, RoundedCornerShape(24.dp))
                    )
                    if (photo.caption.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFFFFFBF2)
                        ) {
                            Text(
                                text = "“${photo.caption}”",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF15181C),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComicTimelineEntryCard(
    photo: DailyComicPhotoEntity,
    issueNumber: Int,
    profiles: List<UserProfileEntity>,
    onReact: (String) -> Unit,
    onOpenFullScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val author = runCatching { FamilyMemberId.valueOf(photo.authorId) }
        .getOrDefault(FamilyMemberId.ZAYN)
    val prof = profiles.firstOrNull { it.memberId == author.name }
    val badge = runCatching {
        BadgeElement.valueOf(prof?.badgeElement ?: BadgeElement.STARLIGHT.name)
    }.getOrDefault(BadgeElement.STARLIGHT)
    val style = ComicPhotoStyle.entries.firstOrNull { it.id == photo.comicStyle }
        ?: ComicPhotoStyle.SUNNY_COMIC

    Box(
        modifier = modifier
            .claymorphic(
                cornerRadius = 28.dp,
                surfaceColor = Color(0xFFFFFBF2),
                elevation = 12.dp
            )
            .border(2.dp, Color(0xFF15181C), RoundedCornerShape(28.dp))
            .padding(14.dp)
            .testTag("comic_timeline_entry_${photo.dateKey}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Author & Date Masthead
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FamilyMemberAvatar(
                    member = author,
                    customPhotoUri = prof?.customPhotoUri,
                    badge = badge,
                    size = 46.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${author.displayName}'s Turn",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF15181C)
                    )
                    Text(
                        text = "${DailyTurnSchedule.formatFriendlyDate(photo.dateKey)} • ${style.emoji} ${style.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CitrusLime,
                    modifier = Modifier.border(1.2.dp, Color(0xFF15181C), RoundedCornerShape(10.dp))
                ) {
                    Text(
                        text = "ISSUE #$issueNumber",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF15181C),
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Comic Artwork Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(2.2.dp, Color(0xFF15181C), RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenFullScreen)
            ) {
                AsyncImage(
                    model = File(photo.localImagePath),
                    contentDescription = "Daily family comic photo by ${author.displayName}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Speech Bubble Caption
            if (photo.caption.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Color(0xFF15181C), RoundedCornerShape(18.dp))
                ) {
                    Text(
                        text = "💬 “${photo.caption}”",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF15181C),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            // Family Reaction Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("💖", "😂", "🚀", "🌟").forEach { emoji ->
                    val selected = photo.reactionEmoji == emoji
                    Surface(
                        shape = CircleShape,
                        color = if (selected) OchreGoldBright else Color(0xFFF1F5F9),
                        modifier = Modifier
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) Color(0xFF15181C) else Color(0xFFCBD5E1),
                                shape = CircleShape
                            )
                            .clickable { onReact(emoji) }
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
