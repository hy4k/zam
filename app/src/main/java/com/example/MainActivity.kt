package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.model.CallPhase
import com.example.data.model.FamilyMemberId
import com.example.data.model.MessageType
import com.example.data.model.VoiceLanguage
import com.example.telecom.FamilyTelecomManager
import com.example.ui.KinNestRoute
import com.example.ui.KinNestViewModel
import com.example.ui.WorkspaceTab
import com.example.ui.components.KinNestAmbientCanvas
import com.example.ui.components.KinNestNavigationRail
import com.example.ui.components.KinNestToyBottomDock
import com.example.ui.components.OfflineAndQuietStatusBanners
import com.example.ui.screens.ActiveVideoCallScreen
import com.example.ui.screens.AdventureMissionScreen
import com.example.ui.screens.AvatarLoginScreen
import com.example.ui.screens.CallLauncherScreen
import com.example.ui.screens.ChatBubbleScreen
import com.example.ui.screens.HomeTreehouseScreen
import com.example.ui.screens.IncomingCallOverlayScreen
import com.example.ui.screens.LiveDrawingBoardScreen
import com.example.ui.screens.ParentalSettingsScreen
import com.example.ui.screens.PermissionsRecoveryScreen
import com.example.ui.screens.PlayChestScreen
import com.example.ui.screens.StoryTogetherScreen
import com.example.ui.screens.UsConstellationScreen
import com.example.ui.theme.KinNestAccessibilityConfig
import com.example.ui.theme.KinNestTheme

class MainActivity : FragmentActivity() {

    private var pendingWidgetAction: String? = null
    private var pendingIncomingCaller: String? = null
    private var pendingIncomingThreeWay: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        captureIntentExtras(intent)

        setContent {
            val viewModel: KinNestViewModel = viewModel()
            KinNestAppRoot(
                viewModel = viewModel,
                initialAction = pendingWidgetAction,
                initialCaller = pendingIncomingCaller,
                initialThreeWay = pendingIncomingThreeWay,
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
    }
}

@Composable
private fun KinNestAppRoot(
    viewModel: KinNestViewModel,
    initialAction: String?,
    initialCaller: String?,
    initialThreeWay: Boolean,
    onConsumeLaunchIntent: () -> Unit
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val callState by viewModel.callState.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val allMessages by viewModel.allMessages.collectAsStateWithLifecycle()
    val capsules by viewModel.memoryCapsules.collectAsStateWithLifecycle()
    val syncQueue by viewModel.syncQueue.collectAsStateWithLifecycle()
    val allActivities by viewModel.allActivities.collectAsStateWithLifecycle()
    val syncState by viewModel.offlineSyncState.collectAsStateWithLifecycle()
    val strokes by viewModel.drawingStrokes.collectAsStateWithLifecycle()
    val heartbeatBanner by viewModel.heartbeatBanner.collectAsStateWithLifecycle()
    val isWalkieRecording by viewModel.isWalkieRecording.collectAsStateWithLifecycle()
    val storyPageIndex by viewModel.storyPageIndex.collectAsStateWithLifecycle()

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
                viewModel.simulateIncomingCall(caller, initialThreeWay)
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
                NavHost(
                    navController = navController,
                    startDestination = if (session.isAuthenticated) {
                        KinNestRoute.Workspace(0)
                    } else {
                        KinNestRoute.LoginPortal
                    }
                ) {
                    composable<KinNestRoute.LoginPortal> {
                        AvatarLoginScreen(
                            profiles = profiles,
                            onAvatarTap = { member ->
                                if (viewModel.signInWithDeviceBoundAvatar(member)) {
                                    navController.navigate(KinNestRoute.Workspace(0)) {
                                        popUpTo<KinNestRoute.LoginPortal> { inclusive = true }
                                    }
                                }
                            },
                            onQuickEnterTab = { tab ->
                                val targetMember = session.activeMember ?: FamilyMemberId.ZAYN
                                if (viewModel.signInWithDeviceBoundAvatar(targetMember)) {
                                    navController.navigate(KinNestRoute.Workspace(tab.index)) {
                                        popUpTo<KinNestRoute.LoginPortal> { inclusive = true }
                                    }
                                }
                            },
                            onCustomizeProfile = { member, badge, uri ->
                                viewModel.updateMemberPersonalization(member, badge, uri)
                            },
                            onOpenPermissions = {
                                navController.navigate(KinNestRoute.PermissionsRecovery)
                            }
                        )
                    }

                    composable<KinNestRoute.Workspace> { backStackEntry ->
                        val workspaceArgs = backStackEntry.toRoute<KinNestRoute.Workspace>()
                        var selectedTabOrdinal by rememberSaveable(workspaceArgs.initialTab) {
                            mutableStateOf(workspaceArgs.initialTab)
                        }
                        val selectedTab = WorkspaceTab.entries.getOrElse(selectedTabOrdinal) {
                            WorkspaceTab.HOME
                        }

                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val isWideScreen = maxWidth >= 840.dp

                            Row(modifier = Modifier.fillMaxSize()) {
                                if (isWideScreen) {
                                    KinNestNavigationRail(
                                        selectedTab = selectedTab,
                                        onSelectTab = { selectedTabOrdinal = it.index }
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
                                                onSelectTab = { selectedTabOrdinal = it.index }
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
                                            onRetrySync = { viewModel.triggerSyncNow(expedited = true) },
                                            onOpenRecovery = {
                                                navController.navigate(KinNestRoute.PermissionsRecovery)
                                            }
                                        )

                                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                            when (selectedTab) {
                                                WorkspaceTab.HOME -> {
                                                    val moment = allActivities.firstOrNull { it.kind == "MOMENT" }
                                                    HomeTreehouseScreen(
                                                        session = session,
                                                        profiles = profiles,
                                                        dailyMoment = moment,
                                                        heartbeatBanner = heartbeatBanner,
                                                        isWalkieRecording = isWalkieRecording,
                                                        onDismissHeartbeatBanner = viewModel::dismissHeartbeatBanner,
                                                        onSendHeartbeat = viewModel::sendHeartbeatTrigger,
                                                        onStartThreeWayCall = {
                                                            viewModel.startFamilyCall(
                                                                listOf(
                                                                    FamilyMemberId.MITHUN,
                                                                    FamilyMemberId.MAFITHA,
                                                                    FamilyMemberId.ZAYN
                                                                )
                                                            )
                                                        },
                                                        onWalkiePressStart = viewModel::startWalkieTalkieHold,
                                                        onWalkiePressRelease = viewModel::releaseWalkieTalkieHold,
                                                        onRotateMoment = viewModel::rotateDailyMoment,
                                                        onToggleLanguage = {
                                                            val next = if (session.voiceLanguage == VoiceLanguage.ENGLISH) {
                                                                VoiceLanguage.MALAYALAM
                                                            } else {
                                                                VoiceLanguage.ENGLISH
                                                            }
                                                            viewModel.setVoiceLanguage(next)
                                                        },
                                                        onSpeakTreehouseTip = {
                                                            viewModel.speakHelper(
                                                                english = "Tap the blue rocket for a video call, or hold the big orange button to talk!",
                                                                malayalam = "വീഡിയോ കോളിനായി നീല റോക്കറ്റ് അമർത്തൂ, സംസാരിക്കാൻ വലിയ ഓറഞ്ച് ബട്ടൺ അമർത്തിപ്പിടിക്കൂ!"
                                                            )
                                                        },
                                                        onReplyHeartbeatWithVideo = {
                                                            viewModel.startFamilyCall(
                                                                listOf(
                                                                    FamilyMemberId.MITHUN,
                                                                    FamilyMemberId.MAFITHA,
                                                                    FamilyMemberId.ZAYN
                                                                )
                                                            )
                                                        }
                                                    )
                                                }

                                                WorkspaceTab.CALL -> {
                                                    CallLauncherScreen(
                                                        session = session,
                                                        callState = callState,
                                                        profiles = profiles,
                                                        onStartThreeWayCall = {
                                                            viewModel.startFamilyCall(
                                                                listOf(
                                                                    FamilyMemberId.MITHUN,
                                                                    FamilyMemberId.MAFITHA,
                                                                    FamilyMemberId.ZAYN
                                                                )
                                                            )
                                                        },
                                                        onStartOneOnOneCall = { target ->
                                                            viewModel.startFamilyCall(listOf(target))
                                                        },
                                                        onCycleAudioRoute = viewModel::cycleAudioRoute,
                                                        onTestIncomingLockCall = {
                                                            val caller = if (session.activeMember == FamilyMemberId.MAFITHA) {
                                                                FamilyMemberId.MITHUN
                                                            } else {
                                                                FamilyMemberId.MAFITHA
                                                            }
                                                            viewModel.simulateIncomingCall(caller, isThreeWay = true)
                                                        }
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
                                                        activities = allActivities,
                                                        onOpenLiveDrawing = {
                                                            navController.navigate(KinNestRoute.LiveDrawingBoard)
                                                        },
                                                        onOpenStoryTogether = {
                                                            navController.navigate(KinNestRoute.StoryTogether)
                                                        },
                                                        onOpenAdventureMission = {
                                                            navController.navigate(KinNestRoute.AdventureMission)
                                                        },
                                                        onToggleActivityDone = viewModel::toggleActivityCompleted,
                                                        onStartPartyGameCall = {
                                                            viewModel.startFamilyCall(
                                                                listOf(
                                                                    FamilyMemberId.MITHUN,
                                                                    FamilyMemberId.MAFITHA,
                                                                    FamilyMemberId.ZAYN
                                                                )
                                                            )
                                                        }
                                                    )
                                                }

                                                WorkspaceTab.US -> {
                                                    UsConstellationScreen(
                                                        capsules = capsules,
                                                        activities = allActivities,
                                                        onAddCapsule = viewModel::addMemoryCapsule,
                                                        onToggleVaultPin = viewModel::toggleCapsuleOfflinePin,
                                                        onVoteTravelPin = viewModel::voteActivity,
                                                        onAddTravelPin = { title, sub, cost, whenLabel ->
                                                            viewModel.addFamilyActivity(
                                                                kind = "TRAVEL_PIN",
                                                                title = title,
                                                                subtitle = sub,
                                                                tag = "TRIP",
                                                                costOrDuration = cost,
                                                                dateLabel = whenLabel
                                                            )
                                                        },
                                                        onOpenParentalSettings = {
                                                            navController.navigate(KinNestRoute.ParentalSettings)
                                                        },
                                                        onOpenPermissionsRecovery = {
                                                            navController.navigate(KinNestRoute.PermissionsRecovery)
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
                        }
                    }

                    composable<KinNestRoute.ActiveCall> {
                        ActiveVideoCallScreen(
                            selfMember = session.activeMember ?: FamilyMemberId.MITHUN,
                            callState = callState,
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
                            onSimulateHandoff = viewModel::simulateNetworkHandoff,
                            onMediaPermissionsChanged = viewModel::onCallMediaPermissionsUpdated,
                            onEndCall = viewModel::endActiveCall
                        )
                    }

                    composable<KinNestRoute.IncomingCall> {
                        val ringing = callState.phase as? CallPhase.RingingIncoming
                        IncomingCallOverlayScreen(
                            caller = ringing?.caller ?: FamilyMemberId.MAFITHA,
                            isThreeWay = ringing?.isThreeWay ?: true,
                            profiles = profiles,
                            onAccept = viewModel::acceptIncomingCall,
                            onDecline = viewModel::declineIncomingCall
                        )
                    }

                    composable<KinNestRoute.LiveDrawingBoard> {
                        LiveDrawingBoardScreen(
                            strokes = strokes,
                            onAddStroke = viewModel::addDrawingStroke,
                            onClearBoard = viewModel::clearDrawingBoard,
                            onSaveToCapsules = {
                                viewModel.addMemoryCapsule(
                                    title = "Family Drawing Masterpiece 🎨",
                                    caption = "Saved ${strokes.size} live collaborative strokes!",
                                    unlockDaysFromNow = null
                                )
                                navController.popBackStack()
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<KinNestRoute.StoryTogether> {
                        StoryTogetherScreen(
                            pageIndex = storyPageIndex,
                            voiceLanguage = session.voiceLanguage,
                            onChangePage = viewModel::setStoryPage,
                            onNarratePage = { en, ml -> viewModel.speakHelper(en, ml) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<KinNestRoute.AdventureMission> {
                        AdventureMissionScreen(
                            onMissionFound = {
                                viewModel.addMemoryCapsule(
                                    title = "AR Mission Victory! 🧭",
                                    caption = "Zayn Adam solved all indoor AR clues!",
                                    unlockDaysFromNow = null
                                )
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<KinNestRoute.PermissionsRecovery> {
                        PermissionsRecoveryScreen(
                            syncState = syncState,
                            syncQueue = syncQueue,
                            onRetrySync = { viewModel.triggerSyncNow(expedited = true) },
                            onToggleOfflineSim = viewModel::toggleSimulatedOfflineMode,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<KinNestRoute.ParentalSettings> {
                        ParentalSettingsScreen(
                            session = session,
                            profiles = profiles,
                            parentLogs = allActivities.filter { it.kind == "PARENT_LOG" },
                            onUnlockWithPin = viewModel::unlockParentControlsWithPin,
                            onBiometricUnlocked = viewModel::markParentControlsBiometricUnlocked,
                            onUpdatePin = viewModel::updateParentPin,
                            onToggleQuietHours = viewModel::toggleQuietHours,
                            onToggleReducedMotion = viewModel::toggleReducedMotion,
                            onToggleDarkMode = viewModel::setDarkModeOverride,
                            onToggleDeviceTrust = viewModel::toggleDeviceTrustSimulation,
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
                            }
                        )
                    }
                }
            }
        }
    }
}
