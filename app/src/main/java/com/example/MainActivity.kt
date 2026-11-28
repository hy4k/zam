package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil.Coil
import coil.ImageLoader
import com.example.data.model.CallPhase
import com.example.data.model.FamilyMemberId
import com.example.data.model.MessageType
import com.example.data.model.VoiceLanguage
import com.example.security.configureAppCheck
import com.example.telecom.FamilyTelecomManager
import com.example.ui.KinNestRoute
import com.example.ui.KinNestViewModel
import com.example.ui.WorkspaceTab
import com.example.ui.components.KinNestAmbientCanvas
import com.example.ui.components.KinNestNavigationRail
import com.example.ui.components.KinNestToyBottomDock
import com.example.ui.components.OfflineAndQuietStatusBanners
import com.example.ui.screens.ActiveVideoCallScreen
import com.example.ui.screens.AvatarLoginScreen
import com.example.ui.screens.CallLauncherScreen
import com.example.ui.screens.ChatBubbleScreen
import com.example.ui.screens.HomeTreehouseScreen
import com.example.ui.screens.IncomingCallOverlayScreen
import com.example.ui.screens.ParentalSettingsScreen
import com.example.ui.screens.PlayChestScreen
import com.example.ui.theme.KinNestAccessibilityConfig
import com.example.ui.theme.KinNestTheme

class MainActivity : FragmentActivity() {

    private var pendingWidgetAction: String? = null
    private var pendingIncomingCaller: String? = null
    private var pendingIncomingThreeWay: Boolean = false
    private var pendingIncomingVideoEnabled: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // STEP 1: Configure Firebase App Check with launch intent before UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)
        // Use software-compatible bitmaps in Coil to prevent AHardwareBuffer allocation failures on virtual GPUs
        Coil.setImageLoader(
            ImageLoader.Builder(applicationContext)
                .allowHardware(false)
                .build()
        )
        enableEdgeToEdge()
        captureIntentExtras(intent)

        setContent {
            val viewModel: KinNestViewModel = viewModel()
            KinNestAppRoot(
                viewModel = viewModel,
                initialAction = pendingWidgetAction,
                initialCaller = pendingIncomingCaller,
                initialThreeWay = pendingIncomingThreeWay,
                initialVideoEnabled = pendingIncomingVideoEnabled,
                onConsumeLaunchIntent = {
                    pendingWidgetAction = null
                    pendingIncomingCaller = null
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureIntentExtras(intent)
    }

    private fun captureIntentExtras(intent: Intent?) {
        pendingWidgetAction = intent?.getStringExtra(FamilyTelecomManager.EXTRA_ACTION)
        pendingIncomingCaller = intent?.getStringExtra(FamilyTelecomManager.EXTRA_CALLER_ID)
        pendingIncomingThreeWay = intent?.getBooleanExtra(FamilyTelecomManager.EXTRA_THREE_WAY, false) ?: false
        pendingIncomingVideoEnabled = intent?.getBooleanExtra(FamilyTelecomManager.EXTRA_VIDEO_ENABLED, true) ?: true
    }
}

@Composable
private fun KinNestAppRoot(
    viewModel: KinNestViewModel,
    initialAction: String?,
    initialCaller: String?,
    initialThreeWay: Boolean,
    initialVideoEnabled: Boolean,
    onConsumeLaunchIntent: () -> Unit
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val loginUiState by viewModel.loginUiState.collectAsStateWithLifecycle()
    val callState by viewModel.callState.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val allMessages by viewModel.allMessages.collectAsStateWithLifecycle()
    val allActivities by viewModel.allActivities.collectAsStateWithLifecycle()
    val syncState by viewModel.offlineSyncState.collectAsStateWithLifecycle()
    val strokes by viewModel.drawingStrokes.collectAsStateWithLifecycle()
    val isWalkieRecording by viewModel.isWalkieRecording.collectAsStateWithLifecycle()
    val memberPresence by viewModel.memberPresence.collectAsStateWithLifecycle()
    val xoxoState by viewModel.xoxoState.collectAsStateWithLifecycle()
    val liveKitRoom by viewModel.liveKitRoom.collectAsStateWithLifecycle()
    val isLiveStreamActive by viewModel.isLiveStreamActive.collectAsStateWithLifecycle()
    val videoTracksByMember by viewModel.videoTracksByMember.collectAsStateWithLifecycle()
    val connectedLiveMembers by viewModel.connectedLiveMembers.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        viewModel.onAppForegrounded()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.onAppBackgrounded()
    }

    val darkTheme = session.darkModeOverride ?: isSystemInDarkTheme()
    val a11yConfig = KinNestAccessibilityConfig(
        reducedMotion = session.reducedMotionEnabled,
        quietHoursActive = session.quietHoursEnabled,
        malayalamVoiceEnabled = session.voiceLanguage == VoiceLanguage.MALAYALAM
    )

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiFeedback.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Handle Glance Widget or Full-Screen Incoming Call Intent launches
    LaunchedEffect(initialAction) {
        when (initialAction) {
            FamilyTelecomManager.ACTION_INCOMING_CALL -> {
                val caller = runCatching {
                    FamilyMemberId.valueOf(initialCaller ?: FamilyMemberId.MAFITHA.name)
                }.getOrDefault(FamilyMemberId.MAFITHA)
                viewModel.simulateIncomingCall(caller, initialThreeWay, initialVideoEnabled)
                onConsumeLaunchIntent()
            }
            FamilyTelecomManager.ACTION_QUICK_CALL -> {
                if (!session.isAuthenticated) viewModel.signInWithDeviceBoundAvatar(FamilyMemberId.ZAYN)
                viewModel.startFamilyCall(listOf(FamilyMemberId.MITHUN, FamilyMemberId.MAFITHA))
                onConsumeLaunchIntent()
            }
            FamilyTelecomManager.ACTION_QUICK_HEARTBEAT -> {
                if (!session.isAuthenticated) viewModel.signInWithDeviceBoundAvatar(FamilyMemberId.ZAYN)
                viewModel.sendHeartbeatTrigger("I miss you!", false)
                onConsumeLaunchIntent()
            }
        }
    }

    // Keep Active Call & Incoming Call overlay synchronized with CallPhase priority
    LaunchedEffect(callState.phase) {
        val currentRoute = navController.currentDestination?.route
        when (callState.phase) {
            is CallPhase.RingingIncoming -> {
                navController.navigate(KinNestRoute.IncomingCall) { launchSingleTop = true }
            }
            is CallPhase.DialingOutgoing,
            is CallPhase.Active,
            is CallPhase.Reconnecting -> {
                if (currentRoute?.contains("ActiveCall") != true) {
                    navController.navigate(KinNestRoute.ActiveCall) { launchSingleTop = true }
                }
            }
            is CallPhase.Idle,
            is CallPhase.Ended -> {
                if (currentRoute?.contains("ActiveCall") == true ||
                    currentRoute?.contains("IncomingCall") == true
                ) {
                    navController.popBackStack()
                }
            }
        }
    }

    KinNestTheme(
        darkTheme = darkTheme,
        accessibilityConfig = a11yConfig
    ) {
        KinNestAmbientCanvas(darkTheme = darkTheme) {
            Scaffold(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) { _ ->
                val initialStartDestination: KinNestRoute = remember {
                    if (session.deviceOwner != null) {
                        KinNestRoute.Workspace(0)
                    } else {
                        KinNestRoute.LoginPortal
                    }
                }
                NavHost(
                    navController = navController,
                    startDestination = initialStartDestination
                ) {
                    composable<KinNestRoute.LoginPortal> {
                        AvatarLoginScreen(
                            profiles = profiles,
                            loginUiState = loginUiState,
                            onSubmitLogin = { username, password ->
                                viewModel.loginWithCredentials(
                                    username = username,
                                    password = password,
                                    onLoginSuccess = {
                                        navController.navigate(KinNestRoute.Workspace(0)) {
                                            popUpTo<KinNestRoute.LoginPortal> { inclusive = true }
                                        }
                                    }
                                )
                            },
                            onClearError = viewModel::clearLoginError
                        )
                    }

                    composable<KinNestRoute.Workspace> { backStackEntry ->
                        val workspaceArgs = backStackEntry.toRoute<KinNestRoute.Workspace>()
                        var selectedTabOrdinal by rememberSaveable(workspaceArgs.initialTab) {
                            mutableStateOf(workspaceArgs.initialTab.coerceIn(0, WorkspaceTab.entries.lastIndex))
                        }
                        val selectedTab = WorkspaceTab.entries.getOrElse(selectedTabOrdinal) {
                            WorkspaceTab.HOME
                        }

                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val isWideScreen = maxWidth >= 840.dp

                            if (selectedTab == WorkspaceTab.CHAT) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                                colorStops = arrayOf(
                                                    0.0f to androidx.compose.ui.graphics.Color(0xFF5D96F5),
                                                    0.45f to androidx.compose.ui.graphics.Color(0xFF3E78E8),
                                                    1.0f to androidx.compose.ui.graphics.Color(0xFF2356C8)
                                                )
                                            )
                                        )
                                )
                            }

                            Row(modifier = Modifier.fillMaxSize()) {
                                if (isWideScreen) {
                                    KinNestNavigationRail(
                                        selectedTab = selectedTab,
                                        onSelectTab = {
                                            viewModel.recordUserInteraction()
                                            selectedTabOrdinal = it.index
                                        }
                                    )
                                }

                                Scaffold(
                                    modifier = Modifier.weight(1f).fillMaxSize(),
                                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    bottomBar = {
                                        if (!isWideScreen) {
                                            KinNestToyBottomDock(
                                                selectedTab = selectedTab,
                                                unreadCount = allMessages.count { it.deliveryStatus == "QUEUED_OFFLINE" },
                                                onSelectTab = {
                                                    viewModel.recordUserInteraction()
                                                    selectedTabOrdinal = it.index
                                                }
                                            )
                                        }
                                    }
                                ) { workspacePadding ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .statusBarsPadding()
                                            .padding(bottom = workspacePadding.calculateBottomPadding())
                                    ) {
                                        OfflineAndQuietStatusBanners(
                                            syncState = syncState,
                                            quietHoursActive = session.quietHoursEnabled,
                                            onRetrySync = { viewModel.triggerSyncNow(expedited = true) }
                                        )

                                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                            when (selectedTab) {
                                                WorkspaceTab.HOME -> {
                                                    HomeTreehouseScreen(
                                                        session = session,
                                                        profiles = profiles,
                                                        memberPresence = memberPresence,
                                                        isWalkieRecording = isWalkieRecording,
                                                        onSelectPresenceStatus = viewModel::setMyPresenceAvailability,
                                                        onWalkiePressStart = viewModel::startWalkieTalkieHold,
                                                        onWalkiePressRelease = viewModel::releaseWalkieTalkieHold,
                                                        onOpenSettings = {
                                                            viewModel.recordUserInteraction()
                                                            navController.navigate(KinNestRoute.ParentalSettings)
                                                        }
                                                    )
                                                }

                                                WorkspaceTab.CALL -> {
                                                    CallLauncherScreen(
                                                        session = session,
                                                        callState = callState,
                                                        profiles = profiles,
                                                        memberPresence = memberPresence,
                                                        onCycleMyPresenceStatus = viewModel::cycleMyPresenceAvailability,
                                                        onStartGroupVideoCall = {
                                                            viewModel.startFamilyCall(
                                                                targets = listOf(
                                                                    FamilyMemberId.MITHUN,
                                                                    FamilyMemberId.MAFITHA,
                                                                    FamilyMemberId.ZAYN
                                                                ),
                                                                videoEnabled = true
                                                            )
                                                        },
                                                        onStartIndividualVideoCall = { target ->
                                                            viewModel.startFamilyCall(
                                                                targets = listOf(target),
                                                                videoEnabled = true
                                                            )
                                                        },
                                                        onStartGroupVoiceCall = {
                                                            viewModel.startFamilyCall(
                                                                targets = listOf(
                                                                    FamilyMemberId.MITHUN,
                                                                    FamilyMemberId.MAFITHA,
                                                                    FamilyMemberId.ZAYN
                                                                ),
                                                                videoEnabled = false
                                                            )
                                                        },
                                                        onStartIndividualVoiceCall = { target ->
                                                            viewModel.startFamilyCall(
                                                                targets = listOf(target),
                                                                videoEnabled = false
                                                            )
                                                        },
                                                        onCycleAudioRoute = viewModel::cycleAudioRoute
                                                    )
                                                }

                                                WorkspaceTab.CHAT -> {
                                                    ChatBubbleScreen(
                                                        session = session,
                                                        profiles = profiles,
                                                        allMessages = allMessages,
                                                        onSelectThread = viewModel::selectChatThread,
                                                        onSendText = { text ->
                                                            viewModel.sendMessage(
                                                                threadId = session.selectedThreadId,
                                                                plaintext = text,
                                                                type = MessageType.TEXT
                                                            )
                                                        },
                                                        onSendMediaUri = { uri, type ->
                                                            viewModel.sendMessage(
                                                                threadId = session.selectedThreadId,
                                                                plaintext = if (type == MessageType.PHOTO) "Shared a family photo 📸" else "Shared a video clip 🎬",
                                                                type = type,
                                                                mediaLocalPath = uri
                                                            )
                                                        },
                                                        onPlayVoiceClip = viewModel::playVoiceMessage,
                                                        onReactMessage = viewModel::reactToMessage,
                                                        onRetrySync = { viewModel.triggerSyncNow(expedited = true) }
                                                    )
                                                }

                                                WorkspaceTab.PLAY -> {
                                                    PlayChestScreen(
                                                        xoxoState = xoxoState,
                                                        selfMember = session.deviceOwner ?: session.activeMember ?: FamilyMemberId.MITHUN,
                                                        profiles = profiles,
                                                        memberPresence = memberPresence,
                                                        onSelectPlayers = viewModel::selectXoxoPlayers,
                                                        onSwapSides = viewModel::swapXoxoSides,
                                                        onChangePlayMode = viewModel::setXoxoPlayMode,
                                                        onMakeMove = viewModel::makeXoxoMove,
                                                        onNextRound = viewModel::startNextXoxoRound,
                                                        onResetScores = viewModel::resetXoxoMatchScores
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    composable<KinNestRoute.ActiveCall> {
                        ActiveVideoCallScreen(
                            selfMember = session.activeMember ?: FamilyMemberId.MITHUN,
                            callState = callState,
                            profiles = profiles,
                            liveKitRoom = liveKitRoom,
                            isLiveStreamActive = isLiveStreamActive,
                            videoTracksByMember = videoTracksByMember,
                            connectedLiveMembers = connectedLiveMembers,
                            liveStrokes = strokes,
                            onToggleMic = viewModel::toggleMicMute,
                            onToggleCamera = viewModel::toggleCameraEnabled,
                            onFlipCamera = viewModel::flipCamera,
                            onCycleAudioRoute = viewModel::cycleAudioRoute,
                            onInviteMember = viewModel::inviteThirdMemberMidCall,
                            onSelectArFilter = viewModel::setArFilter,
                            onTriggerEmojiBurst = { viewModel.spawnEmojiBurst(it) },
                            onTriggerScreenTap = viewModel::triggerScreenTapRipple,
                            onToggleDrawingOverlay = viewModel::toggleInCallDrawingOverlay,
                            onCommitDrawingStroke = viewModel::addDrawingStroke,
                            onMediaPermissionsChanged = viewModel::onCallMediaPermissionsUpdated,
                            onEndCall = viewModel::endActiveCall
                        )
                    }

                    composable<KinNestRoute.IncomingCall> {
                        val ringing = callState.phase as? CallPhase.RingingIncoming
                        IncomingCallOverlayScreen(
                            caller = ringing?.caller ?: FamilyMemberId.MAFITHA,
                            isThreeWay = ringing?.isThreeWay ?: true,
                            videoEnabled = ringing?.videoEnabled ?: !callState.isVoiceCallMode,
                            profiles = profiles,
                            onAccept = { viewModel.acceptIncomingCall() },
                            onAcceptVoiceOnly = { viewModel.acceptIncomingCall(joinWithVideo = false) },
                            onDecline = viewModel::declineIncomingCall
                        )
                    }

                    composable<KinNestRoute.ParentalSettings> {
                        ParentalSettingsScreen(
                            session = session,
                            profiles = profiles,
                            parentLogs = allActivities.filter { it.kind == "PARENT_LOG" },
                            onCustomizeMyProfile = { badge, uri ->
                                val self = session.deviceOwner ?: session.activeMember ?: FamilyMemberId.MITHUN
                                viewModel.updateMemberPersonalization(self, badge, uri)
                            },
                            onUnlockWithPin = viewModel::unlockParentControlsWithPin,
                            onBiometricUnlocked = viewModel::markParentControlsBiometricUnlocked,
                            onUpdatePin = viewModel::updateParentPin,
                            onToggleQuietHours = viewModel::toggleQuietHours,
                            onToggleReducedMotion = viewModel::toggleReducedMotion,
                            onToggleDarkMode = viewModel::setDarkModeOverride,
                            onReassignDeviceOwner = viewModel::reassignDeviceOwner,
                            onAddParentLog = { title, note, cost ->
                                viewModel.addFamilyActivity(
                                    kind = "PARENT_LOG",
                                    title = title,
                                    subtitle = note,
                                    tag = "HOUSEHOLD_SATIRE",
                                    costOrDuration = cost,
                                    dateLabel = "Adults Only"
                                )
                            },
                            onLockAndBack = {
                                viewModel.lockParentControls()
                                navController.popBackStack()
                            },
                            onSignOut = {
                                viewModel.signOut()
                                navController.navigate(KinNestRoute.LoginPortal) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
