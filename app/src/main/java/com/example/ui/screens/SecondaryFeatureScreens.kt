package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.local.FamilyActivityEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.BadgeElement
import com.example.data.model.FamilyMemberId
import com.example.data.model.LiveStroke
import com.example.data.model.UserSessionState
import com.example.data.model.VoiceLanguage
import com.example.security.ParentalAuthManager
import com.example.ui.components.ClayActionButton
import com.example.ui.components.ClayCircleButton
import com.example.ui.components.ClayTextOnLight
import com.example.ui.components.FamilyMemberAvatar
import com.example.ui.components.Text3D
import com.example.ui.theme.ClayAqua
import com.example.ui.theme.ClayAquaDeep
import com.example.ui.theme.ClayCocoaInk
import com.example.ui.theme.ClayCoral
import com.example.ui.theme.ClayCoralDeep
import com.example.ui.theme.ClayDeepTealInk
import com.example.ui.theme.ClayHoney
import com.example.ui.theme.ClayHoneyDeep
import com.example.ui.theme.ClayMutedInk
import com.example.ui.theme.ClayPeach
import com.example.ui.theme.ClayPeachDeep
import com.example.ui.theme.ClaySage
import com.example.ui.theme.ClayTealShadow
import com.example.ui.theme.ClayTextCream
import com.example.ui.theme.ClayVanilla
import com.example.ui.theme.ClayVanillaDeep
import com.example.ui.theme.ClayVanillaLight
import com.example.ui.theme.clayDisc3D
import com.example.ui.theme.claymorphic

@Composable
private fun SubScreenTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ClayCircleButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            clayColor = ClayPeach,
            iconTint = ClayDeepTealInk,
            size = 48.dp,
            testTag = "subscreen_back_button",
            onClick = onBack
        )
    }
}

@Composable
private fun clayLightFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = ClayDeepTealInk,
    unfocusedTextColor = ClayDeepTealInk,
    focusedBorderColor = ClayAqua,
    unfocusedBorderColor = ClayPeachDeep,
    focusedLabelColor = ClayAquaDeep,
    unfocusedLabelColor = ClayMutedInk,
    cursorColor = ClayCoral,
    focusedContainerColor = ClayVanillaLight,
    unfocusedContainerColor = ClayVanillaLight.copy(alpha = 0.9f)
)

/**
 * Synchronized Multiplayer Vector Sketching Canvas.
 */
@Composable
fun LiveDrawingBoardScreen(
    strokes: List<LiveStroke>,
    onAddStroke: (Color, Float, List<Pair<Float, Float>>) -> Unit,
    onClearBoard: () -> Unit,
    onSaveToCapsules: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val palette = listOf(
        ClayCoral,
        ClayAqua,
        ClaySage,
        ClayHoney,
        ClayDeepTealInk,
        ClayCocoaInk
    )
    var selectedColor by remember { mutableStateOf(palette.first()) }
    val activePoints = remember { mutableStateListOf<Pair<Float, Float>>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubScreenTopBar(onBack = onBack)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .claymorphic(
                    cornerRadius = 30.dp,
                    surfaceColor = ClayVanillaLight,
                    elevation = 14.dp,
                    shadowTint = ClayTealShadow,
                    ledgeDepth = 6.dp
                )
                .padding(6.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedColor) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                activePoints.clear()
                                activePoints.add(offset.x / size.width to offset.y / size.height)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                activePoints.add(
                                    (change.position.x / size.width).coerceIn(0f, 1f) to
                                        (change.position.y / size.height).coerceIn(0f, 1f)
                                )
                            },
                            onDragEnd = {
                                onAddStroke(selectedColor, 10f, activePoints.toList())
                                activePoints.clear()
                            }
                        )
                    }
            ) {
                fun drawPoints(points: List<Pair<Float, Float>>, color: Color, widthDp: Float) {
                    if (points.size < 2) return
                    val path = Path().apply {
                        moveTo(points.first().first * size.width, points.first().second * size.height)
                        points.drop(1).forEach { (x, y) ->
                            lineTo(x * size.width, y * size.height)
                        }
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = widthDp.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                strokes.forEach { stroke ->
                    drawPoints(stroke.points, Color(stroke.colorArgb), stroke.strokeWidthDp)
                }
                drawPoints(activePoints, selectedColor, 10f)
            }

            if (strokes.isEmpty() && activePoints.isEmpty()) {
                ClayTextOnLight(
                    text = "Draw together! Every stroke syncs across all 3 screens.",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    textColor = ClayMutedInk,
                    textAlign = TextAlign.Center,
                    depthDp = 1.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .claymorphic(
                    cornerRadius = 24.dp,
                    surfaceColor = ClayPeach,
                    elevation = 10.dp,
                    ledgeDepth = 4.dp
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            palette.forEach { swatch ->
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clayDisc3D(
                            surfaceColor = swatch,
                            elevation = if (selectedColor == swatch) 10.dp else 5.dp,
                            ledgeDepth = 3.dp
                        )
                        .then(
                            if (selectedColor == swatch) Modifier.border(3.dp, ClayVanillaLight, CircleShape)
                            else Modifier
                        )
                        .clickable { selectedColor = swatch }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ClayActionButton(
                text = "Clear",
                containerColor = ClayCoral,
                icon = Icons.Rounded.Clear,
                testTag = "drawing_clear_button",
                modifier = Modifier.weight(1f),
                onClick = onClearBoard
            )
            ClayActionButton(
                text = "Save",
                containerColor = ClaySage,
                icon = Icons.Rounded.Save,
                testTag = "drawing_save_button",
                modifier = Modifier.weight(1f),
                onClick = onSaveToCapsules
            )
        }
    }
}

/**
 * Story Together: Synchronized Digital Storybook with Live Voice Narration (EN & Malayalam).
 */
@Composable
fun StoryTogetherScreen(
    pageIndex: Int,
    voiceLanguage: VoiceLanguage,
    onChangePage: (Int) -> Unit,
    onNarratePage: (String, String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    data class StoryPage(val emoji: String, val title: String, val english: String, val malayalam: String)

    val pages = remember {
        listOf(
            StoryPage(
                emoji = "🚀",
                title = "Chapter 1: The Mint Rocket",
                english = "Zayn Adam, Vapi Mithun, and Ummi Mafitha hopped inside a glowing mint rocket bound for Starlight Island.",
                malayalam = "സെയ്ൻ ആദവും വാപ്പി മിഥുനും ഉമ്മി മഫിതയും തിളങ്ങുന്ന റോക്കറ്റിൽ നക്ഷത്രദ്വീപിലേക്ക് പറന്നു."
            ),
            StoryPage(
                emoji = "🦕",
                title = "Chapter 2: The Singing Dino",
                english = "On the cloud beach, a baby green dinosaur was humming a happy Malayalam lullaby.",
                malayalam = "മേഘക്കടൽത്തീരത്ത് ഒരു കുഞ്ഞു ദിനോസർ സന്തോഷത്തോടെ പാട്ടു പാടുന്നുണ്ടായിരുന്നു."
            ),
            StoryPage(
                emoji = "🌈",
                title = "Chapter 3: The Rainbow Slide",
                english = "They slid down a giant rainbow right into a bowl of mango ice cream!",
                malayalam = "അവർ വലിയൊരു മഴവില്ലിലൂടെ തെന്നി മാമ്പഴ ഐസ്ക്രീം കുളത്തിലേക്ക് വീണു!"
            ),
            StoryPage(
                emoji = "🏡",
                title = "Chapter 4: Home to Our Treehouse",
                english = "Three golden stars tucked them safely back into the Holy Moly ! treehouse. Goodnight!",
                malayalam = "മൂന്ന് സ്വർണ്ണ നക്ഷത്രങ്ങൾ അവരെ തിരികെ ട്രീഹൗസിൽ എത്തിച്ചു. ശുഭരാത്രി!"
            )
        )
    }

    val current = pages[pageIndex.coerceIn(0, pages.lastIndex)]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SubScreenTopBar(onBack = onBack)

        Box(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 14.dp)
                .claymorphic(
                    cornerRadius = 32.dp,
                    surfaceColor = ClayPeach,
                    elevation = 16.dp,
                    shadowTint = ClayTealShadow,
                    ledgeDepth = 8.dp
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clayDisc3D(
                            surfaceColor = ClayVanillaLight,
                            elevation = 10.dp,
                            ledgeDepth = 4.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = current.emoji, fontSize = 58.sp)
                }

                ClayTextOnLight(
                    text = current.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    textColor = ClayDeepTealInk,
                    textAlign = TextAlign.Center,
                    depthDp = 1.6.dp
                )

                ClayTextOnLight(
                    text = if (voiceLanguage == VoiceLanguage.MALAYALAM) current.malayalam else current.english,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    textColor = ClayCocoaInk,
                    textAlign = TextAlign.Center,
                    depthDp = 1.dp
                )

                Box(
                    modifier = Modifier
                        .claymorphic(
                            cornerRadius = 18.dp,
                            surfaceColor = ClayAqua,
                            elevation = 6.dp,
                            ledgeDepth = 3.dp
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text3D(
                        text = "Page ${pageIndex + 1} of ${pages.size} • Synced",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        depthDp = 1.2.dp
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ClayActionButton(
                text = "Read Aloud (${voiceLanguage.label})",
                containerColor = ClayHoney,
                icon = Icons.Rounded.VolumeUp,
                testTag = "story_narrate_button",
                modifier = Modifier.fillMaxWidth(),
                onClick = { onNarratePage(current.english, current.malayalam) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ClayActionButton(
                    text = "Prev",
                    containerColor = ClayAqua,
                    icon = Icons.AutoMirrored.Rounded.NavigateBefore,
                    testTag = "story_prev_button",
                    modifier = Modifier.weight(1f),
                    onClick = { onChangePage(pageIndex - 1) }
                )
                ClayActionButton(
                    text = "Next",
                    containerColor = ClaySage,
                    icon = Icons.AutoMirrored.Rounded.NavigateNext,
                    testTag = "story_next_button",
                    modifier = Modifier.weight(1f),
                    onClick = { onChangePage(pageIndex + 1) }
                )
            }
        }
    }
}

/**
 * Adventure Missions: Indoor AR Camera Clue Hunt for Zayn Adam.
 */
@Composable
fun AdventureMissionScreen(
    onMissionFound: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPerm = granted }

    var clueIndex by remember { mutableStateOf(0) }
    val clues = listOf(
        "Clue 1: Point your Space Scanner at something BLUE & SOFT!",
        "Clue 2: Find a shiny spoon or cup in the kitchen!",
        "Clue 3: Take a goofy victory selfie with Ummi or Vapi!"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubScreenTopBar(onBack = onBack)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .claymorphic(
                    cornerRadius = 30.dp,
                    surfaceColor = ClayPeach,
                    elevation = 14.dp,
                    ledgeDepth = 6.dp
                )
                .padding(6.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF241810))
        ) {
            if (hasCameraPerm) {
                CameraXLivePreview(
                    useFrontCamera = false,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clayDisc3D(surfaceColor = ClayPeach, elevation = 10.dp, ledgeDepth = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = null,
                            tint = ClayAquaDeep,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Text3D(
                        text = "Turn on the camera to start Zayn's Treasure Hunt!",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        depthDp = 2.dp
                    )
                    ClayActionButton(
                        text = "Allow Camera",
                        containerColor = ClayAqua,
                        testTag = "ar_allow_camera_button",
                        onClick = { launcher.launch(Manifest.permission.CAMERA) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(14.dp)
                    .fillMaxWidth()
                    .claymorphic(
                        cornerRadius = 22.dp,
                        surfaceColor = ClayVanilla,
                        elevation = 10.dp,
                        ledgeDepth = 4.dp
                    )
                    .padding(14.dp)
            ) {
                ClayTextOnLight(
                    text = clues[clueIndex],
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    textColor = ClayDeepTealInk,
                    depthDp = 1.2.dp
                )
            }
        }

        ClayActionButton(
            text = if (clueIndex < clues.lastIndex) "Clue Found! Next Clue 🎉" else "Mission Complete! +25 Stars 🌟",
            containerColor = ClaySage,
            icon = Icons.Rounded.CheckCircle,
            testTag = "ar_clue_found_button",
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (clueIndex < clues.lastIndex) {
                    clueIndex++
                } else {
                    onMissionFound()
                    onBack()
                }
            }
        )
    }
}

/**
 * Gear Hub (Settings & Profile Edit):
 * Smooth Pixar-style 3D Matte Clay cards (Warm Peach-Cream & Vanilla-Cream)
 * with guaranteed-legible Deep-Teal / Cocoa 3D typography and sculpted Aqua / Coral / Honey / Sage buttons.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParentalSettingsScreen(
    session: UserSessionState,
    profiles: List<UserProfileEntity>,
    parentLogs: List<FamilyActivityEntity>,
    onCustomizeMyProfile: (BadgeElement, String?) -> Unit,
    onUnlockWithPin: (String) -> Boolean,
    onBiometricUnlocked: () -> Unit,
    onUpdatePin: (String) -> Unit,
    onToggleQuietHours: (Boolean) -> Unit,
    onToggleReducedMotion: (Boolean) -> Unit,
    onToggleDarkMode: (Boolean?) -> Unit,
    onReassignDeviceOwner: (FamilyMemberId) -> Unit,
    onAddParentLog: (title: String, note: String, cost: String) -> Unit,
    onLockAndBack: () -> Unit,
    onSignOut: () -> Unit
) {
    BackHandler { onLockAndBack() }

    val context = LocalContext.current
    val selfMember = session.deviceOwner ?: session.activeMember ?: FamilyMemberId.MITHUN
    val selfProfile = profiles.firstOrNull { it.memberId == selfMember.name }
    val currentBadge = runCatching {
        BadgeElement.valueOf(selfProfile?.badgeElement ?: BadgeElement.ROCKET.name)
    }.getOrDefault(BadgeElement.ROCKET)

    var selectedBadge by remember(selfMember, currentBadge) { mutableStateOf(currentBadge) }
    var pickedPhotoUri by remember(selfMember, selfProfile?.customPhotoUri) {
        mutableStateOf(selfProfile?.customPhotoUri)
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { pickedPhotoUri = it.toString() }
    }

    var pinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var newLogTitle by remember { mutableStateOf("") }
    var newLogCost by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box(modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                SubScreenTopBar(onBack = onLockAndBack)
            }
        }

        // 1. Style Your Look — Warm Peach-Cream 3D Matte Clay Card
        item {
            Box(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .claymorphic(
                        cornerRadius = 30.dp,
                        surfaceColor = ClayPeach,
                        elevation = 16.dp,
                        shadowTint = ClayTealShadow,
                        ledgeDepth = 8.dp
                    )
                    .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 22.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ClayTextOnLight(
                        text = "Style Your Look (${selfMember.displayName})",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        textColor = ClayDeepTealInk,
                        depthDp = 1.5.dp,
                        textAlign = TextAlign.Center
                    )

                    FamilyMemberAvatar(
                        member = selfMember,
                        customPhotoUri = pickedPhotoUri,
                        badge = selectedBadge,
                        size = 88.dp,
                        isOnline = true
                    )

                    ClayActionButton(
                        text = "Pick My Profile Photo",
                        containerColor = ClayHoney,
                        icon = Icons.Rounded.PhotoLibrary,
                        testTag = "pick_avatar_photo_button",
                        modifier = Modifier.fillMaxWidth(),
                        minHeight = 50.dp,
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )

                    ClayTextOnLight(
                        text = "Choose Your Fun Badge",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        textColor = ClayCocoaInk,
                        depthDp = 1.dp,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BadgeElement.entries.forEach { badge ->
                            val isSelected = selectedBadge == badge
                            Box(
                                modifier = Modifier
                                    .claymorphic(
                                        cornerRadius = 18.dp,
                                        surfaceColor = if (isSelected) ClayCoral else ClayVanillaLight,
                                        elevation = if (isSelected) 8.dp else 4.dp,
                                        ledgeDepth = if (isSelected) 4.dp else 2.5.dp
                                    )
                                    .clickable { selectedBadge = badge }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                if (isSelected) {
                                    Text3D(
                                        text = "${badge.emoji} ${badge.label}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        depthDp = 1.1.dp
                                    )
                                } else {
                                    ClayTextOnLight(
                                        text = "${badge.emoji} ${badge.label}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        textColor = ClayDeepTealInk,
                                        depthDp = 0.8.dp
                                    )
                                }
                            }
                        }
                    }

                    ClayActionButton(
                        text = "Save My Look",
                        containerColor = ClaySage,
                        icon = Icons.Rounded.Save,
                        testTag = "save_avatar_style_button",
                        modifier = Modifier.fillMaxWidth(),
                        minHeight = 52.dp,
                        onClick = { onCustomizeMyProfile(selectedBadge, pickedPhotoUri) }
                    )
                }
            }
        }

        // 2. Account / Sign Out — Warm Vanilla-Cream 3D Matte Clay Card
        item {
            Box(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .claymorphic(
                        cornerRadius = 28.dp,
                        surfaceColor = ClayVanilla,
                        elevation = 14.dp,
                        shadowTint = ClayTealShadow,
                        ledgeDepth = 7.dp
                    )
                    .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ClayTextOnLight(
                        text = "Account & Switch User",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        textColor = ClayDeepTealInk,
                        depthDp = 1.4.dp
                    )
                    ClayTextOnLight(
                        text = "Signed in as ${selfMember.displayName} (${selfMember.title}). Sign out to log in with another username & password.",
                        style = MaterialTheme.typography.bodyMedium,
                        textColor = ClayCocoaInk,
                        depthDp = 0.8.dp
                    )
                    ClayActionButton(
                        text = "Sign Out / Switch User",
                        containerColor = ClayCoral,
                        icon = Icons.AutoMirrored.Rounded.Logout,
                        testTag = "settings_sign_out_button",
                        modifier = Modifier.fillMaxWidth(),
                        minHeight = 50.dp,
                        onClick = onSignOut
                    )
                }
            }
        }

        // 3. PIN / Biometric-Gated Vapi & Ummi's Grown-Up Corner
        if (!session.isParentalUnlocked) {
            item {
                Column(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .claymorphic(
                            cornerRadius = 30.dp,
                            surfaceColor = ClayPeach,
                            elevation = 16.dp,
                            shadowTint = ClayTealShadow,
                            ledgeDepth = 8.dp
                        )
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clayDisc3D(surfaceColor = ClayHoney, elevation = 8.dp, ledgeDepth = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Parental Lock",
                            tint = ClayTextCream,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    ClayTextOnLight(
                        text = "Vapi & Ummi's Grown-Up Corner",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        textColor = ClayDeepTealInk,
                        depthDp = 1.5.dp,
                        textAlign = TextAlign.Center
                    )
                    ClayTextOnLight(
                        text = "Unlock with Biometrics or Family PIN (default: 2468)",
                        style = MaterialTheme.typography.bodyMedium,
                        textColor = ClayCocoaInk,
                        depthDp = 0.8.dp,
                        textAlign = TextAlign.Center
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it.filter(Char::isDigit).take(6) },
                        label = { Text("Enter Family PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = clayLightFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parent_pin_input")
                    )
                    ClayActionButton(
                        text = "Unlock Parent Controls",
                        containerColor = ClayAqua,
                        testTag = "parent_unlock_pin_button",
                        modifier = Modifier.fillMaxWidth(),
                        minHeight = 50.dp,
                        onClick = { onUnlockWithPin(pinInput) }
                    )
                    if (ParentalAuthManager.canUseBiometrics(context) && context is FragmentActivity) {
                        ClayActionButton(
                            text = "Use Fingerprint / Face",
                            containerColor = ClaySage,
                            icon = Icons.Rounded.Fingerprint,
                            testTag = "parent_biometric_button",
                            modifier = Modifier.fillMaxWidth(),
                            minHeight = 50.dp,
                            onClick = {
                                ParentalAuthManager.showBiometricPrompt(
                                    activity = context,
                                    onSuccess = onBiometricUnlocked,
                                    onFallbackToPin = {}
                                )
                            }
                        )
                    }
                }
            }
        } else {
            // Comfort & Accessibility Controls
            item {
                Box(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .claymorphic(
                            cornerRadius = 28.dp,
                            surfaceColor = ClayPeach,
                            elevation = 16.dp,
                            shadowTint = ClayTealShadow,
                            ledgeDepth = 8.dp
                        )
                        .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ClayTextOnLight(
                            text = "Family Comfort & Controls",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            textColor = ClayDeepTealInk,
                            depthDp = 1.5.dp
                        )
                        val switchColors = SwitchDefaults.colors(
                            checkedThumbColor = ClayVanillaLight,
                            checkedTrackColor = ClaySage,
                            uncheckedThumbColor = ClayVanillaLight,
                            uncheckedTrackColor = ClayPeachDeep
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ClayTextOnLight(
                                text = "Quiet Hours (Gentle Chimes)",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                textColor = ClayCocoaInk,
                                modifier = Modifier.weight(1f),
                                depthDp = 0.9.dp
                            )
                            Switch(checked = session.quietHoursEnabled, onCheckedChange = onToggleQuietHours, colors = switchColors)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ClayTextOnLight(
                                text = "Reduced Motion",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                textColor = ClayCocoaInk,
                                modifier = Modifier.weight(1f),
                                depthDp = 0.9.dp
                            )
                            Switch(checked = session.reducedMotionEnabled, onCheckedChange = onToggleReducedMotion, colors = switchColors)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ClayTextOnLight(
                                text = "Twilight Clay Dark Mode",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                textColor = ClayCocoaInk,
                                modifier = Modifier.weight(1f),
                                depthDp = 0.9.dp
                            )
                            Switch(
                                checked = session.darkModeOverride == true,
                                onCheckedChange = { onToggleDarkMode(if (it) true else null) },
                                colors = switchColors
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newPinInput,
                                onValueChange = { newPinInput = it.filter(Char::isDigit).take(6) },
                                label = { Text("New PIN (4+ digits)") },
                                singleLine = true,
                                shape = RoundedCornerShape(20.dp),
                                colors = clayLightFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                            ClayActionButton(
                                text = "Set",
                                containerColor = ClaySage,
                                testTag = "save_new_pin_button",
                                minHeight = 52.dp,
                                onClick = {
                                    if (newPinInput.length >= 4) {
                                        onUpdatePin(newPinInput)
                                        newPinInput = ""
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Quick Phone Owner Switch (Parent-only)
            item {
                val currentOwner = session.deviceOwner ?: session.activeMember ?: FamilyMemberId.MITHUN
                Box(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .claymorphic(
                            cornerRadius = 28.dp,
                            surfaceColor = ClayVanilla,
                            elevation = 14.dp,
                            shadowTint = ClayTealShadow,
                            ledgeDepth = 7.dp
                        )
                        .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ClayTextOnLight(
                            text = "Quick Member Override",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            textColor = ClayDeepTealInk,
                            depthDp = 1.4.dp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FamilyMemberId.entries.forEach { member ->
                                val isAssigned = member == currentOwner
                                ClayActionButton(
                                    text = member.title,
                                    containerColor = if (isAssigned) ClaySage else ClayAqua,
                                    testTag = "reassign_device_${member.name.lowercase()}",
                                    modifier = Modifier.weight(1f),
                                    minHeight = 48.dp,
                                    onClick = { onReassignDeviceOwner(member) }
                                )
                            }
                        }
                    }
                }
            }

            // Parent-Only Classified Grown-Up Log
            item {
                Box(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .claymorphic(
                            cornerRadius = 28.dp,
                            surfaceColor = ClayPeach,
                            elevation = 14.dp,
                            shadowTint = ClayTealShadow,
                            ledgeDepth = 7.dp
                        )
                        .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ClayTextOnLight(
                            text = "Vapi & Ummi's Classified Log",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            textColor = ClayDeepTealInk,
                            depthDp = 1.5.dp
                        )
                        OutlinedTextField(
                            value = newLogTitle,
                            onValueChange = { newLogTitle = it },
                            label = { Text("Task / Note") },
                            shape = RoundedCornerShape(20.dp),
                            colors = clayLightFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newLogCost,
                            onValueChange = { newLogCost = it },
                            label = { Text("Expense Split / Verdict") },
                            shape = RoundedCornerShape(20.dp),
                            colors = clayLightFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        ClayActionButton(
                            text = "Add to Grown-Up Log",
                            containerColor = ClayAqua,
                            testTag = "add_parent_ledger_button",
                            modifier = Modifier.fillMaxWidth(),
                            minHeight = 50.dp,
                            onClick = {
                                if (newLogTitle.isNotBlank()) {
                                    onAddParentLog(
                                        newLogTitle.trim(),
                                        "Filed under Vapi & Ummi notes.",
                                        newLogCost.ifBlank { "50/50 split" }
                                    )
                                    newLogTitle = ""
                                    newLogCost = ""
                                }
                            }
                        )
                    }
                }
            }

            items(parentLogs, key = { it.id }) { log ->
                Box(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .claymorphic(
                            cornerRadius = 22.dp,
                            surfaceColor = ClayVanilla,
                            elevation = 8.dp,
                            shadowTint = ClayTealShadow,
                            ledgeDepth = 5.dp
                        )
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        ClayTextOnLight(
                            text = log.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            textColor = ClayDeepTealInk,
                            depthDp = 1.2.dp
                        )
                        ClayTextOnLight(
                            text = log.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            textColor = ClayCocoaInk,
                            depthDp = 0.8.dp
                        )
                        log.costEstimate?.let {
                            ClayTextOnLight(
                                text = it,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                textColor = ClayCoralDeep,
                                depthDp = 0.9.dp
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(18.dp)) }
    }
}
