package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.local.FamilyActivityEntity
import com.example.data.local.OfflineSyncQueueEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.LiveStroke
import com.example.data.model.OfflineSyncState
import com.example.data.model.UserSessionState
import com.example.data.model.VoiceLanguage
import com.example.security.ParentalAuthManager
import com.example.ui.components.ClayActionButton
import com.example.ui.theme.BubblePink
import com.example.ui.theme.ConstellationGold
import com.example.ui.theme.CrimsonDecline
import com.example.ui.theme.ElectricRocket
import com.example.ui.theme.EmeraldAccept
import com.example.ui.theme.MintTreehouseDeep
import com.example.ui.theme.PurpleChest
import com.example.ui.theme.WalkieAmber
import com.example.ui.theme.claymorphic

@Composable
private fun SubScreenTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .testTag("subscreen_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back"
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

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
        BubblePink,
        ElectricRocket,
        MintTreehouseDeep,
        ConstellationGold,
        PurpleChest,
        Color(0xFF0F172A)
    )
    var selectedColor by remember { mutableStateOf(palette.first()) }
    val activePoints = remember { mutableStateListOf<Pair<Float, Float>>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubScreenTopBar(title = "Live Drawing Board", onBack = onBack)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .claymorphic(cornerRadius = 30.dp, surfaceColor = Color.White, elevation = 12.dp)
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
                Text(
                    text = "Draw together! Every stroke syncs across all 3 family screens.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            palette.forEach { swatch ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(swatch)
                        .border(
                            width = if (selectedColor == swatch) 4.dp else 0.dp,
                            color = Color.White,
                            shape = CircleShape
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
                containerColor = CrimsonDecline,
                icon = Icons.Rounded.Clear,
                testTag = "drawing_clear_button",
                modifier = Modifier.weight(1f),
                onClick = onClearBoard
            )
            ClayActionButton(
                text = "Save Capsule",
                containerColor = EmeraldAccept,
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
                english = "Zayn Adam, Papa Mithun, and Mama Mafitha hopped inside a glowing mint rocket bound for Starlight Island.",
                malayalam = "സെയ്ൻ ആദവും പപ്പ മിഥുനും മമ്മ മഫിതയും തിളങ്ങുന്ന റോക്കറ്റിൽ നക്ഷത്രദ്വീപിലേക്ക് പറന്നു."
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
                english = "Three golden stars tucked them safely back into the KinNest treehouse. Goodnight!",
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
        SubScreenTopBar(title = "Story Together", onBack = onBack)

        Box(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp)
                .claymorphic(
                    cornerRadius = 32.dp,
                    surfaceColor = MaterialTheme.colorScheme.surface,
                    elevation = 12.dp
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = current.emoji, fontSize = 72.sp)
                Text(
                    text = current.title,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (voiceLanguage == VoiceLanguage.MALAYALAM) current.malayalam else current.english,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MintTreehouseDeep.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "Page ${pageIndex + 1} of ${pages.size} • Synced for all 3",
                        style = MaterialTheme.typography.labelLarge,
                        color = MintTreehouseDeep,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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
                containerColor = WalkieAmber,
                icon = Icons.Rounded.VolumeUp,
                testTag = "story_narrate_button",
                modifier = Modifier.fillMaxWidth(),
                onClick = { onNarratePage(current.english, current.malayalam) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ClayActionButton(
                    text = "Prev",
                    containerColor = PurpleChest,
                    icon = Icons.AutoMirrored.Rounded.NavigateBefore,
                    testTag = "story_prev_button",
                    modifier = Modifier.weight(1f),
                    onClick = { onChangePage(pageIndex - 1) }
                )
                ClayActionButton(
                    text = "Next",
                    containerColor = ElectricRocket,
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
        "Clue 3: Take a goofy victory selfie with Mama or Papa!"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubScreenTopBar(title = "AR Adventure Mission", onBack = onBack)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(Color(0xFF0F172A))
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "Enable Camera to start Zayn's Indoor AR Clue Scanner!",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    ClayActionButton(
                        text = "Allow Camera",
                        containerColor = PurpleChest,
                        testTag = "ar_allow_camera_button",
                        onClick = { launcher.launch(Manifest.permission.CAMERA) }
                    )
                }
            }

            // AR Scanner HUD Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .claymorphic(cornerRadius = 24.dp, surfaceColor = Color.White.copy(alpha = 0.92f))
                    .padding(16.dp)
            ) {
                Text(
                    text = clues[clueIndex],
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF1E1B4B)
                )
            }
        }

        ClayActionButton(
            text = if (clueIndex < clues.lastIndex) "Clue Found! Next Clue 🎉" else "Mission Complete! +25 Stars 🌟",
            containerColor = EmeraldAccept,
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
 * Operational / Recovery Screen: Runtime Permissions Setup + Offline Sync Queue & Retry Manager.
 */
@Composable
fun PermissionsRecoveryScreen(
    syncState: OfflineSyncState,
    syncQueue: List<OfflineSyncQueueEntity>,
    onRetrySync: () -> Unit,
    onToggleOfflineSim: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var refreshTick by remember { mutableStateOf(0) }

    val multiPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshTick++ }

    val requiredPerms = buildList {
        add(Manifest.permission.CAMERA to "Camera (3-Way Video & AR Missions)")
        add(Manifest.permission.RECORD_AUDIO to "Microphone (LiveKit & Walkie-Talkie)")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS to "Notifications (Lock-Screen Ring & Heartbeat)")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT to "Bluetooth Audio Routing (Headsets)")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SubScreenTopBar(title = "Permissions & Sync Recovery", onBack = onBack) }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .claymorphic(
                        cornerRadius = 26.dp,
                        surfaceColor = MaterialTheme.colorScheme.surface,
                        elevation = 8.dp
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Hardware & Call Permissions", style = MaterialTheme.typography.titleLarge)
                    requiredPerms.forEach { (perm, label) ->
                        val granted = remember(refreshTick) {
                            ContextCompat.checkSelfPermission(context, perm) ==
                                PackageManager.PERMISSION_GRANTED
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.Security,
                                contentDescription = null,
                                tint = if (granted) EmeraldAccept else WalkieAmber
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (granted) "Ready" else "Needed",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (granted) EmeraldAccept else WalkieAmber
                            )
                        }
                    }
                    ClayActionButton(
                        text = "Grant Missing Permissions",
                        containerColor = ElectricRocket,
                        testTag = "grant_permissions_button",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            multiPermLauncher.launch(requiredPerms.map { it.first }.toTypedArray())
                        }
                    )
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .claymorphic(
                        cornerRadius = 26.dp,
                        surfaceColor = MaterialTheme.colorScheme.surface,
                        elevation = 8.dp
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("WorkManager Sync & Retry Manager", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Supabase: ${if (syncState.supabaseConfigured) "Connected" else "Local Room Vault (Add keys in Secrets)"}\n" +
                            "LiveKit SFU: ${if (syncState.liveKitConfigured) "Connected" else "Local Preview Mode (Add URL in Secrets)"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Simulate Offline Mode",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = !syncState.isOnline,
                            onCheckedChange = onToggleOfflineSim
                        )
                    }
                    ClayActionButton(
                        text = "Retry All Queued Uploads (${syncQueue.size})",
                        containerColor = MintTreehouseDeep,
                        icon = Icons.Rounded.Refresh,
                        testTag = "retry_all_sync_button",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRetrySync
                    )
                }
            }
        }

        items(syncQueue, key = { it.id }) { q ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .claymorphic(cornerRadius = 20.dp, surfaceColor = MaterialTheme.colorScheme.surface)
                    .padding(14.dp)
            ) {
                Column {
                    Text("${q.entityType} • ${q.status} (Retries: ${q.retryCount})", style = MaterialTheme.typography.titleMedium)
                    q.lastError?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = CrimsonDecline)
                    }
                }
            }
        }
    }
}

/**
 * PIN / Biometric-Gated Parental Settings & Mithun + Mafitha's Satirical Household Ledger.
 */
@Composable
fun ParentalSettingsScreen(
    session: UserSessionState,
    profiles: List<UserProfileEntity>,
    parentLogs: List<FamilyActivityEntity>,
    onUnlockWithPin: (String) -> Boolean,
    onBiometricUnlocked: () -> Unit,
    onUpdatePin: (String) -> Unit,
    onToggleQuietHours: (Boolean) -> Unit,
    onToggleReducedMotion: (Boolean) -> Unit,
    onToggleDarkMode: (Boolean?) -> Unit,
    onToggleDeviceTrust: (String, Boolean) -> Unit,
    onAddParentLog: (title: String, note: String, cost: String) -> Unit,
    onLockAndBack: () -> Unit
) {
    BackHandler { onLockAndBack() }

    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }

    if (!session.isParentalUnlocked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .claymorphic(
                        cornerRadius = 30.dp,
                        surfaceColor = MaterialTheme.colorScheme.surface,
                        elevation = 14.dp
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Parental Lock",
                    tint = PurpleChest,
                    modifier = Modifier.size(54.dp)
                )
                Text("Mithun & Mafitha Only", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Unlock with Biometrics or Family PIN (default: 2468)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { pinInput = it.filter(Char::isDigit).take(6) },
                    label = { Text("Enter Family PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("parent_pin_input")
                )
                ClayActionButton(
                    text = "Unlock with PIN",
                    containerColor = PurpleChest,
                    testTag = "parent_unlock_pin_button",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onUnlockWithPin(pinInput) }
                )
                if (ParentalAuthManager.canUseBiometrics(context) && context is FragmentActivity) {
                    ClayActionButton(
                        text = "Use Fingerprint / Face",
                        containerColor = MintTreehouseDeep,
                        icon = Icons.Rounded.Fingerprint,
                        testTag = "parent_biometric_button",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            ParentalAuthManager.showBiometricPrompt(
                                activity = context,
                                onSuccess = onBiometricUnlocked,
                                onFallbackToPin = {}
                            )
                        }
                    )
                }
                ClayActionButton(
                    text = "Back",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    testTag = "parent_gate_back_button",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onLockAndBack
                )
            }
        }
        return
    }

    var newLogTitle by remember { mutableStateOf("") }
    var newLogCost by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SubScreenTopBar(title = "Parent Controls & Satirical Ledger", onBack = onLockAndBack) }

        // Comfort & Accessibility Controls
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .claymorphic(cornerRadius = 26.dp, surfaceColor = MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Family Comfort & Accessibility", style = MaterialTheme.typography.titleLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Quiet Hours (Gentle Chimes)", modifier = Modifier.weight(1f))
                        Switch(checked = session.quietHoursEnabled, onCheckedChange = onToggleQuietHours)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Reduced Motion", modifier = Modifier.weight(1f))
                        Switch(checked = session.reducedMotionEnabled, onCheckedChange = onToggleReducedMotion)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Force Dark Cosmos Theme", modifier = Modifier.weight(1f))
                        Switch(
                            checked = session.darkModeOverride == true,
                            onCheckedChange = { onToggleDarkMode(if (it) true else null) }
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
                            modifier = Modifier.weight(1f)
                        )
                        ClayActionButton(
                            text = "Set",
                            containerColor = MintTreehouseDeep,
                            testTag = "save_new_pin_button",
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

        // Hardware Keystore Device Authorization
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .claymorphic(cornerRadius = 26.dp, surfaceColor = MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hardware Keystore Device Trust", style = MaterialTheme.typography.titleLarge)
                    profiles.forEach { prof ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prof.displayName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Key: ${prof.devicePublicFingerprint}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = prof.isDeviceTrusted,
                                onCheckedChange = { onToggleDeviceTrust(prof.memberId, it) }
                            )
                        }
                    }
                }
            }
        }

        // Playful Satirical Parent-Only Household Log & Expense Splitter
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .claymorphic(cornerRadius = 26.dp, surfaceColor = Color(0xFFFFF7ED))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Mithun & Mafitha's Classified Grown-Up Log",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF9A3412)
                    )
                    Text(
                        text = "100% hidden from Zayn Adam. Sarcasm & sneaky snack splits welcome.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFC2410C)
                    )
                    OutlinedTextField(
                        value = newLogTitle,
                        onValueChange = { newLogTitle = it },
                        label = { Text("Task / Sarcastic Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newLogCost,
                        onValueChange = { newLogCost = it },
                        label = { Text("Expense Split / Dramatic Verdict") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    ClayActionButton(
                        text = "Add to Classified Ledger",
                        containerColor = WalkieAmber,
                        testTag = "add_parent_ledger_button",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (newLogTitle.isNotBlank()) {
                                onAddParentLog(
                                    newLogTitle.trim(),
                                    "Filed under dubious domestic negotiations.",
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
                    .fillMaxWidth()
                    .claymorphic(cornerRadius = 20.dp, surfaceColor = MaterialTheme.colorScheme.surface)
                    .padding(14.dp)
            ) {
                Column {
                    Text(log.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        log.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    log.costEstimate?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelLarge,
                            color = WalkieAmber
                        )
                    }
                }
            }
        }
    }
}
