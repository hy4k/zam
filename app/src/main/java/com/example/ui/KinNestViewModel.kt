package com.example.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.EncryptedMessageEntity
import com.example.data.local.FamilyActivityEntity
import com.example.data.local.KinNestDatabase
import com.example.data.local.MemoryCapsuleEntity
import com.example.data.local.OfflineSyncQueueEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.ArFilterType
import com.example.data.model.AudioRoute
import com.example.data.model.BadgeElement
import com.example.data.model.CallLogEntry
import com.example.data.model.CallParticipantState
import com.example.data.model.CallPhase
import com.example.data.model.CallState
import com.example.data.model.DeliveryStatus
import com.example.data.model.EmojiBurstEvent
import com.example.data.model.FamilyMemberId
import com.example.data.model.LiveStroke
import com.example.data.model.MessageType
import com.example.data.model.MomentCategory
import com.example.data.model.OfflineSyncState
import com.example.data.model.UserSessionState
import com.example.data.model.VoiceLanguage
import com.example.media.LiveKitCallEngine
import com.example.media.LiveKitDataPacket
import com.example.media.StopRecordingResult
import com.example.media.VoiceAndMediaCapture
import com.example.security.DeviceKeystoreManager
import com.example.security.ParentalAuthManager
import com.example.sync.KinNestSyncRepository
import com.example.sync.OfflineSyncWorker
import com.example.sync.SupabaseCapsuleDto
import com.example.sync.SupabaseMessageDto
import com.example.telecom.CallForegroundService
import com.example.telecom.FamilyTelecomManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

data class HeartbeatBannerAlert(
    val sender: FamilyMemberId,
    val message: String,
    val isSafetyAlert: Boolean,
    val timestampMs: Long
)

class KinNestViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val db = KinNestDatabase.getInstance(appContext)
    private val dao = db.kinNestDao()
    private val keystore = DeviceKeystoreManager(appContext)
    private val syncRepo = KinNestSyncRepository(dao)
    val liveKitEngine = LiveKitCallEngine(appContext)
    val telecomManager = FamilyTelecomManager(appContext)
    val voiceCapture = VoiceAndMediaCapture(appContext)
    private val json = Json { ignoreUnknownKeys = true }

    private val _session = MutableStateFlow(UserSessionState())
    val session: StateFlow<UserSessionState> = _session.asStateFlow()

    private val _callState = MutableStateFlow(
        CallState(
            recentCallLogs = listOf(
                CallLogEntry("c1", "Mithun, Mafitha & Zayn", "3-Way Video", false, "Today • 8:05 AM", "14m 22s"),
                CallLogEntry("c2", "Mafitha (Mama)", "1-on-1 Video", false, "Yesterday • 6:40 PM", "06m 10s"),
                CallLogEntry("c3", "Mithun (Papa)", "Missed Call", true, "Yesterday • 1:15 PM", "Tap to call back")
            )
        )
    )
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _isOnline = MutableStateFlow(checkCurrentConnectivity())
    private val _isSyncingNow = MutableStateFlow(false)
    private val _lastSyncedLabel = MutableStateFlow("Local Vault Ready")

    val profiles: StateFlow<List<UserProfileEntity>> = dao.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMessages: StateFlow<List<EncryptedMessageEntity>> = dao.observeAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memoryCapsules: StateFlow<List<MemoryCapsuleEntity>> = dao.observeMemoryCapsules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncQueue: StateFlow<List<OfflineSyncQueueEntity>> = dao.observeSyncQueue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActivities: StateFlow<List<FamilyActivityEntity>> = dao.observeAllActivities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offlineSyncState: StateFlow<OfflineSyncState> = combine(
        _isOnline,
        syncQueue,
        _isSyncingNow,
        _lastSyncedLabel
    ) { online, queue, syncing, lastLabel ->
        OfflineSyncState(
            isOnline = online,
            pendingQueueCount = queue.count { it.status == "PENDING" || it.status == "RETRYING" },
            failedQueueCount = queue.count { it.status == "FAILED" },
            isSyncingNow = syncing,
            lastSyncedLabel = lastLabel,
            supabaseConfigured = syncRepo.isSupabaseConfigured(),
            liveKitConfigured = liveKitEngine.isLiveKitEndpointConfigured()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OfflineSyncState())

    private val _drawingStrokes = MutableStateFlow<List<LiveStroke>>(emptyList())
    val drawingStrokes: StateFlow<List<LiveStroke>> = _drawingStrokes.asStateFlow()

    private val _heartbeatBanner = MutableStateFlow<HeartbeatBannerAlert?>(null)
    val heartbeatBanner: StateFlow<HeartbeatBannerAlert?> = _heartbeatBanner.asStateFlow()

    private val _isWalkieRecording = MutableStateFlow(false)
    val isWalkieRecording: StateFlow<Boolean> = _isWalkieRecording.asStateFlow()

    private val _storyPageIndex = MutableStateFlow(0)
    val storyPageIndex: StateFlow<Int> = _storyPageIndex.asStateFlow()

    private val _uiFeedback = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val uiFeedback = _uiFeedback.asSharedFlow()

    private var callTimerJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // Ensure initial family profiles & starter activities exist on first launch
            if (profiles.value.isEmpty()) {
                KinNestDatabase.ensureSeeded(appContext, dao)
            }
        }
        registerConnectivityMonitor()
        observeLiveKitDataChannel()
    }

    private fun checkCurrentConnectivity(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun registerConnectivityMonitor() {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                triggerSyncNow(expedited = false)
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
            }
        }
        networkCallback = cb
        runCatching {
            cm.registerNetworkCallback(NetworkRequest.Builder().build(), cb)
        }
    }

    private fun observeLiveKitDataChannel() {
        viewModelScope.launch {
            liveKitEngine.incomingDataPackets.collect { packet ->
                when (packet) {
                    is LiveKitDataPacket.EmojiBurstPacket -> {
                        val sender = runCatching { FamilyMemberId.valueOf(packet.senderId) }
                            .getOrDefault(FamilyMemberId.ZAYN)
                        spawnEmojiBurst(packet.emoji, sender, broadcast = false)
                    }
                    is LiveKitDataPacket.ArFilterPacket -> {
                        val sender = runCatching { FamilyMemberId.valueOf(packet.senderId) }
                            .getOrNull() ?: return@collect
                        val filter = runCatching { ArFilterType.valueOf(packet.filterName) }
                            .getOrDefault(ArFilterType.NONE)
                        _callState.update { state ->
                            state.copy(
                                participants = state.participants.map {
                                    if (it.memberId == sender) it.copy(activeFilter = filter) else it
                                }
                            )
                        }
                    }
                    is LiveKitDataPacket.ScreenTapPulsePacket -> {
                        val sender = runCatching { FamilyMemberId.valueOf(packet.senderId) }
                            .getOrNull() ?: return@collect
                        _callState.update { it.copy(screenTapPulseMember = sender) }
                        delay(1100)
                        _callState.update { it.copy(screenTapPulseMember = null) }
                    }
                    is LiveKitDataPacket.DrawingStrokePacket -> {
                        val sender = runCatching { FamilyMemberId.valueOf(packet.senderId) }
                            .getOrDefault(FamilyMemberId.ZAYN)
                        _drawingStrokes.update { existing ->
                            existing + LiveStroke(
                                id = packet.strokeId,
                                author = sender,
                                colorArgb = packet.colorArgb,
                                strokeWidthDp = packet.strokeWidthDp,
                                points = packet.points
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Authentication & Device-Bound Hardware Keystore ---

    fun signInWithDeviceBoundAvatar(member: FamilyMemberId): Boolean {
        val profile = profiles.value.firstOrNull { it.memberId == member.name }
        val alias = profile?.deviceKeyAlias ?: "kinnest_device_${member.name.lowercase()}"
        val expectedFp = profile?.devicePublicFingerprint ?: keystore.ensureDeviceIdentityKey(alias)
        val trusted = profile?.isDeviceTrusted ?: true
        val verified = trusted && keystore.verifyDeviceBinding(alias, expectedFp)

        if (!verified) {
            _uiFeedback.tryEmit("Untrusted device: Hardware Keystore verification blocked sign-in.")
            return false
        }

        _session.update {
            it.copy(
                activeMember = member,
                isAuthenticated = true,
                isDeviceTrusted = true,
                hardwareKeyAlias = alias,
                hardwareKeyFingerprint = expectedFp,
                isParentalUnlocked = false
            )
        }

        if (member == FamilyMemberId.ZAYN) {
            speakHelper(
                english = "Hi Zayn Adam! Welcome to your family treehouse.",
                malayalam = "ഹായ് സെയ്ൻ ആദം! നമ്മുടെ ഫാമിലി ട്രീഹൗസിലേക്ക് സ്വാഗതം."
            )
        }
        return true
    }

    fun signOut() {
        _session.update {
            it.copy(
                activeMember = null,
                isAuthenticated = false,
                isParentalUnlocked = false
            )
        }
    }

    fun toggleDeviceTrustSimulation(memberId: String, trusted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = profiles.value.firstOrNull { it.memberId == memberId } ?: return@launch
            dao.upsertProfile(profile.copy(isDeviceTrusted = trusted))
            _uiFeedback.emit(
                if (trusted) "${profile.displayName}'s device key authorized"
                else "${profile.displayName}'s device key revoked"
            )
        }
    }

    fun updateMemberPersonalization(
        member: FamilyMemberId,
        badge: BadgeElement,
        photoUri: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = profiles.value.firstOrNull { it.memberId == member.name } ?: return@launch
            dao.upsertProfile(
                existing.copy(
                    badgeElement = badge.name,
                    customPhotoUri = photoUri ?: existing.customPhotoUri
                )
            )
            _uiFeedback.emit("Updated ${member.displayName}'s halo & badge!")
        }
    }

    // --- Accessibility, Quiet Hours, Voice Over ---

    fun setVoiceLanguage(language: VoiceLanguage) {
        _session.update { it.copy(voiceLanguage = language) }
        speakHelper(
            english = "English voice helper ready!",
            malayalam = "മലയാളം വോയ്സ് സഹായി തയ്യാറാണ്!"
        )
    }

    fun toggleQuietHours(enabled: Boolean) {
        _session.update { it.copy(quietHoursEnabled = enabled) }
    }

    fun toggleReducedMotion(enabled: Boolean) {
        _session.update { it.copy(reducedMotionEnabled = enabled) }
    }

    fun setDarkModeOverride(override: Boolean?) {
        _session.update { it.copy(darkModeOverride = override) }
    }

    fun unlockParentControlsWithPin(pin: String): Boolean {
        val valid = ParentalAuthManager.verifyPin(appContext, pin)
        if (valid) {
            _session.update { it.copy(isParentalUnlocked = true) }
        } else {
            _uiFeedback.tryEmit("Incorrect Family PIN (default: 2468)")
        }
        return valid
    }

    fun markParentControlsBiometricUnlocked() {
        _session.update { it.copy(isParentalUnlocked = true) }
    }

    fun lockParentControls() {
        _session.update { it.copy(isParentalUnlocked = false) }
    }

    fun updateParentPin(newPin: String) {
        ParentalAuthManager.updatePin(appContext, newPin)
        _uiFeedback.tryEmit("Updated Family Parent PIN")
    }

    fun speakHelper(english: String, malayalam: String) {
        val s = _session.value
        voiceCapture.speakGuidance(
            englishText = english,
            malayalamText = malayalam,
            language = s.voiceLanguage,
            quietHoursActive = s.quietHoursEnabled
        )
    }

    // --- Real-Time 3-Way Video & Voice Engine (LiveKit + Core-Telecom) ---

    fun startFamilyCall(targets: List<FamilyMemberId>) {
        val self = _session.value.activeMember ?: FamilyMemberId.MITHUN
        val allInRoom = (listOf(self) + targets).distinct()
        val participantStates = allInRoom.map { member ->
            CallParticipantState(
                memberId = member,
                isConnected = member == self,
                isSpeaking = false,
                audioLevel = if (member == self) 0.3f else 0f,
                isMuted = false,
                isCameraEnabled = true,
                activeFilter = if (member == self) _callState.value.activeFilter else ArFilterType.NONE
            )
        }

        _callState.update {
            it.copy(
                phase = CallPhase.DialingOutgoing(targets),
                participants = participantStates,
                durationSeconds = 0L,
                networkQualityLabel = "LiveKit SFU • H.264 • Opus AEC/NS/AGC"
            )
        }

        val primaryTarget = targets.firstOrNull() ?: FamilyMemberId.MAFITHA
        telecomManager.startManagedFamilyCall(
            callerOrTarget = primaryTarget,
            isIncoming = false,
            isVideo = true,
            onAnswered = { connectCallMedia(allInRoom) },
            onDisconnected = { endCallInternal("Call ended") }
        )

        viewModelScope.launch {
            delay(900)
            if (_callState.value.phase is CallPhase.DialingOutgoing) {
                connectCallMedia(allInRoom)
            }
        }
    }

    fun simulateIncomingCall(caller: FamilyMemberId, isThreeWay: Boolean) {
        telecomManager.postLockScreenIncomingCallNotification(
            caller = caller,
            isThreeWay = isThreeWay,
            quietHoursActive = _session.value.quietHoursEnabled
        )
        _callState.update {
            it.copy(
                phase = CallPhase.RingingIncoming(
                    caller = caller,
                    isThreeWay = isThreeWay,
                    roomName = "kinnest-family-room"
                )
            )
        }
    }

    fun acceptIncomingCall() {
        val incoming = _callState.value.phase as? CallPhase.RingingIncoming ?: return
        val self = _session.value.activeMember ?: FamilyMemberId.ZAYN
        val members = if (incoming.isThreeWay) {
            listOf(self, FamilyMemberId.MITHUN, FamilyMemberId.MAFITHA, FamilyMemberId.ZAYN).distinct()
        } else {
            listOf(self, incoming.caller).distinct()
        }
        telecomManager.cancelIncomingCallNotification()
        connectCallMedia(members)
    }

    fun declineIncomingCall() {
        val incoming = _callState.value.phase as? CallPhase.RingingIncoming
        telecomManager.cancelIncomingCallNotification()
        if (incoming != null) {
            val missedLog = CallLogEntry(
                id = UUID.randomUUID().toString(),
                participantsLabel = incoming.caller.displayName,
                callTypeLabel = if (incoming.isThreeWay) "3-Way Video (Declined)" else "1-on-1 Video (Declined)",
                isMissed = true,
                timestampLabel = "Just now",
                durationLabel = "Tap to call back"
            )
            _callState.update {
                it.copy(
                    phase = CallPhase.Idle,
                    recentCallLogs = listOf(missedLog) + it.recentCallLogs
                )
            }
        } else {
            _callState.update { it.copy(phase = CallPhase.Idle) }
        }
    }

    fun inviteThirdMemberMidCall(member: FamilyMemberId) {
        _callState.update { state ->
            if (state.participants.any { it.memberId == member }) return@update state
            state.copy(
                participants = state.participants + CallParticipantState(
                    memberId = member,
                    isConnected = true,
                    isSpeaking = true,
                    audioLevel = 0.65f,
                    isMuted = false,
                    isCameraEnabled = true
                )
            )
        }
        _uiFeedback.tryEmit("${member.displayName} joined the 3-way family stream!")
    }

    private fun connectCallMedia(members: List<FamilyMemberId>) {
        viewModelScope.launch {
            liveKitEngine.connectToFamilyRoom(token = "", enableMic = true, enableCamera = true)
            CallForegroundService.start(
                appContext,
                members.joinToString(", ") { it.displayName }
            )
            _callState.update { state ->
                state.copy(
                    phase = CallPhase.Active,
                    participants = members.map { member ->
                        CallParticipantState(
                            memberId = member,
                            isConnected = true,
                            isSpeaking = member != _session.value.activeMember,
                            audioLevel = 0.55f,
                            isMuted = false,
                            isCameraEnabled = true
                        )
                    }
                )
            }
            startCallTimerAndWaveformLoop()
        }
    }

    fun onCallMediaPermissionsUpdated(cameraGranted: Boolean, audioGranted: Boolean) {
        val state = _callState.value
        if (state.phase !is CallPhase.Active && state.phase !is CallPhase.DialingOutgoing) return
        if (cameraGranted || audioGranted) {
            val label = state.participants.joinToString(", ") { it.memberId.displayName }
                .ifBlank { "Mithun, Mafitha & Zayn Adam" }
            CallForegroundService.start(appContext, label)
            viewModelScope.launch {
                if (audioGranted && !state.isLocalMicMuted) {
                    liveKitEngine.setMicrophoneEnabled(true)
                }
                if (cameraGranted && state.isLocalCameraOn) {
                    liveKitEngine.setCameraEnabled(true)
                }
            }
        }
    }

    private fun startCallTimerAndWaveformLoop() {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (_callState.value.phase is CallPhase.Active || _callState.value.phase is CallPhase.Reconnecting) {
                delay(1000)
                _callState.update { state ->
                    val nextSec = state.durationSeconds + 1
                    val updatedParticipants = state.participants.mapIndexed { idx, p ->
                        val pulse = ((nextSec + idx) % 3L == 0L) && !p.isMuted
                        p.copy(
                            isSpeaking = pulse,
                            audioLevel = if (p.isMuted) 0f else if (pulse) 0.82f else 0.24f
                        )
                    }
                    state.copy(durationSeconds = nextSec, participants = updatedParticipants)
                }
            }
        }
    }

    fun toggleMicMute() {
        val nextMuted = !_callState.value.isLocalMicMuted
        val self = _session.value.activeMember ?: FamilyMemberId.MITHUN
        viewModelScope.launch {
            liveKitEngine.setMicrophoneEnabled(!nextMuted)
        }
        _callState.update { state ->
            state.copy(
                isLocalMicMuted = nextMuted,
                participants = state.participants.map {
                    if (it.memberId == self) it.copy(isMuted = nextMuted, audioLevel = 0f) else it
                }
            )
        }
    }

    fun toggleCameraEnabled() {
        val nextOn = !_callState.value.isLocalCameraOn
        val self = _session.value.activeMember ?: FamilyMemberId.MITHUN
        viewModelScope.launch {
            liveKitEngine.setCameraEnabled(nextOn)
        }
        _callState.update { state ->
            state.copy(
                isLocalCameraOn = nextOn,
                participants = state.participants.map {
                    if (it.memberId == self) it.copy(isCameraEnabled = nextOn) else it
                }
            )
        }
    }

    fun flipCamera() {
        val nextFront = !_callState.value.isFrontCamera
        viewModelScope.launch { liveKitEngine.switchCamera(nextFront) }
        _callState.update { it.copy(isFrontCamera = nextFront) }
    }

    fun cycleAudioRoute() {
        val next = when (_callState.value.audioRoute) {
            AudioRoute.SPEAKER -> AudioRoute.BLUETOOTH
            AudioRoute.BLUETOOTH -> AudioRoute.EARPIECE
            AudioRoute.EARPIECE -> AudioRoute.SPEAKER
        }
        liveKitEngine.configureCommunicationAudio(next)
        _callState.update { it.copy(audioRoute = next) }
    }

    fun simulateNetworkHandoff() {
        viewModelScope.launch {
            _callState.update {
                it.copy(
                    phase = CallPhase.Reconnecting("Switching Wi-Fi → 5G seamlessly…"),
                    networkQualityLabel = "Reconnecting ICE/SFU…"
                )
            }
            delay(1800)
            _callState.update {
                it.copy(
                    phase = CallPhase.Active,
                    networkQualityLabel = "5G SFU Handover • H.264 + Opus"
                )
            }
        }
    }

    fun setArFilter(filter: ArFilterType) {
        val self = _session.value.activeMember ?: FamilyMemberId.ZAYN
        _callState.update { state ->
            state.copy(
                activeFilter = filter,
                participants = state.participants.map {
                    if (it.memberId == self) it.copy(activeFilter = filter) else it
                }
            )
        }
        viewModelScope.launch { liveKitEngine.broadcastArFilter(self, filter) }
    }

    fun toggleInCallDrawingOverlay() {
        _callState.update { it.copy(showLiveDrawingOverlay = !it.showLiveDrawingOverlay) }
    }

    fun spawnEmojiBurst(emoji: String, senderOverride: FamilyMemberId? = null, broadcast: Boolean = true) {
        val sender = senderOverride ?: _session.value.activeMember ?: FamilyMemberId.ZAYN
        val xFrac = (15..85).random() / 100f
        val burst = EmojiBurstEvent(
            id = System.nanoTime(),
            emoji = emoji,
            sender = sender,
            startXFraction = xFrac
        )
        _callState.update { it.copy(activeEmojiBursts = (it.activeEmojiBursts + burst).takeLast(16)) }
        if (broadcast) {
            viewModelScope.launch { liveKitEngine.broadcastEmojiBurst(sender, emoji, xFrac) }
        }
        viewModelScope.launch {
            delay(1900)
            _callState.update { state ->
                state.copy(activeEmojiBursts = state.activeEmojiBursts.filterNot { it.id == burst.id })
            }
        }
    }

    fun triggerScreenTapRipple() {
        val self = _session.value.activeMember ?: FamilyMemberId.ZAYN
        viewModelScope.launch {
            liveKitEngine.broadcastTapPulse(self)
            _callState.update { it.copy(screenTapPulseMember = self) }
            delay(950)
            _callState.update { it.copy(screenTapPulseMember = null) }
        }
    }

    fun endActiveCall() {
        endCallInternal("Call finished")
    }

    private fun endCallInternal(reason: String) {
        callTimerJob?.cancel()
        liveKitEngine.disconnect()
        CallForegroundService.stop(appContext)
        val snapshot = _callState.value
        val mins = snapshot.durationSeconds / 60
        val secs = snapshot.durationSeconds % 60
        val log = CallLogEntry(
            id = UUID.randomUUID().toString(),
            participantsLabel = snapshot.participants.joinToString(", ") { it.memberId.displayName }
                .ifBlank { "Family Call" },
            callTypeLabel = if (snapshot.participants.size >= 3) "3-Way Video" else "1-on-1 Video",
            isMissed = false,
            timestampLabel = "Just now",
            durationLabel = "%02d:%02d".format(mins, secs)
        )
        _callState.update {
            it.copy(
                phase = CallPhase.Idle,
                durationSeconds = 0L,
                showLiveDrawingOverlay = false,
                recentCallLogs = listOf(log) + it.recentCallLogs
            )
        }
        _uiFeedback.tryEmit(reason)
    }

    // --- Heartbeat, Walkie-Talkie & E2EE Messaging + Offline Sync ---

    fun sendHeartbeatTrigger(label: String, isSafetyAlert: Boolean) {
        val sender = _session.value.activeMember ?: FamilyMemberId.ZAYN
        _heartbeatBanner.value = HeartbeatBannerAlert(
            sender = sender,
            message = label,
            isSafetyAlert = isSafetyAlert,
            timestampMs = System.currentTimeMillis()
        )
        sendMessage(
            threadId = "family_group",
            plaintext = if (isSafetyAlert) "🚨 SAFETY ALERT: $label" else "💖 Heartbeat: $label",
            type = MessageType.HEARTBEAT_ALERT,
            expedited = true
        )
        speakHelper(
            english = "Sent $label to the whole family!",
            malayalam = "$label കുടുംബത്തിലേക്ക് അയച്ചു!"
        )
    }

    fun dismissHeartbeatBanner() {
        _heartbeatBanner.value = null
    }

    fun startWalkieTalkieHold() {
        val started = voiceCapture.startAacRecording("walkie")
        _isWalkieRecording.value = started != null
    }

    fun releaseWalkieTalkieHold() {
        if (!_isWalkieRecording.value) return
        _isWalkieRecording.value = false
        when (val result = voiceCapture.stopRecording()) {
            is StopRecordingResult.Saved -> {
                val durationSec = (result.durationMs / 1000L).coerceIn(1L, 60L)
                sendMessage(
                    threadId = _session.value.selectedThreadId,
                    plaintext = "🎙️ Walkie-Talkie Voice Clip (${durationSec}s AAC)",
                    type = MessageType.VOICE_AAC,
                    mediaLocalPath = result.file.absolutePath,
                    expedited = true
                )
                _uiFeedback.tryEmit("Sent ${durationSec}s Walkie-Talkie voice clip!")
            }
            is StopRecordingResult.TooShort -> {
                _uiFeedback.tryEmit("Hold the orange button while talking to send a voice clip!")
            }
            is StopRecordingResult.Failed -> {
                _uiFeedback.tryEmit("Microphone could not capture audio — check mic & try holding longer.")
            }
            is StopRecordingResult.Idle -> Unit
        }
    }

    fun playVoiceMessage(path: String?) {
        voiceCapture.playVoiceFile(path)
    }

    fun selectChatThread(threadId: String) {
        _session.update { it.copy(selectedThreadId = threadId) }
    }

    fun sendMessage(
        threadId: String,
        plaintext: String,
        type: MessageType = MessageType.TEXT,
        mediaLocalPath: String? = null,
        expedited: Boolean = false
    ) {
        if (plaintext.isBlank() && mediaLocalPath == null) return
        val sender = _session.value.activeMember ?: FamilyMemberId.MITHUN
        val recipients = when (threadId) {
            "mithun_mafitha" -> "MITHUN,MAFITHA"
            "mithun_zayn" -> "MITHUN,ZAYN"
            "mafitha_zayn" -> "MAFITHA,ZAYN"
            else -> "MITHUN,MAFITHA,ZAYN"
        }

        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val msgId = "msg_${UUID.randomUUID()}"
            val encrypted = keystore.encryptMessageForLocalVault(plaintext)
            val initialStatus = if (_isOnline.value && syncRepo.isSupabaseConfigured()) {
                DeliveryStatus.SENDING
            } else if (_isOnline.value) {
                DeliveryStatus.DELIVERED
            } else {
                DeliveryStatus.QUEUED_OFFLINE
            }

            val entity = EncryptedMessageEntity(
                id = msgId,
                threadId = threadId,
                senderId = sender.name,
                recipientIdsCsv = recipients,
                ciphertextPayload = encrypted.first,
                ivBase64 = encrypted.second,
                decryptedPreview = plaintext,
                messageType = type.name,
                mediaLocalPath = mediaLocalPath,
                mediaRemoteUrl = null,
                reactionEmoji = null,
                deliveryStatus = initialStatus.name,
                readByCsv = sender.name,
                timestampMs = now
            )
            dao.upsertMessage(entity)

            if (!_isOnline.value || syncRepo.isSupabaseConfigured()) {
                val dto = SupabaseMessageDto(
                    id = msgId,
                    thread_id = threadId,
                    sender_id = sender.name,
                    recipient_ids_csv = recipients,
                    ciphertext_payload = encrypted.first,
                    iv_base64 = encrypted.second,
                    message_type = type.name,
                    timestamp_ms = now
                )
                dao.enqueueSyncItem(
                    OfflineSyncQueueEntity(
                        id = "sync_${UUID.randomUUID()}",
                        entityType = "MESSAGE",
                        entityId = msgId,
                        payloadJson = json.encodeToString(dto),
                        priority = if (expedited) "EXPEDITED" else "NORMAL",
                        retryCount = 0,
                        status = if (_isOnline.value) "PENDING" else "FAILED",
                        lastError = if (_isOnline.value) null else "Waiting for connection",
                        createdAtMs = now
                    )
                )
                OfflineSyncWorker.enqueueSync(appContext, expedited = expedited)
            }
        }
    }

    fun reactToMessage(messageId: String, emoji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateMessageReaction(messageId, emoji)
        }
    }

    fun triggerSyncNow(expedited: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncingNow.value = true
            OfflineSyncWorker.enqueueSync(appContext, expedited = expedited)
            delay(900)
            val pending = dao.getPendingSyncItems()
            if (_isOnline.value && !syncRepo.isSupabaseConfigured()) {
                // In local demo mode without Supabase project credentials, mark local ledger synced cleanly
                pending.forEach { item ->
                    dao.updateSyncItemStatus(item.id, "SYNCED", item.retryCount, null)
                    if (item.entityType == "MESSAGE") {
                        dao.updateMessageStatus(item.entityId, DeliveryStatus.DELIVERED.name)
                    }
                }
                dao.pruneSyncedItems()
            }
            _isSyncingNow.value = false
            _lastSyncedLabel.value = "Synced just now"
        }
    }

    fun toggleSimulatedOfflineMode(forceOffline: Boolean) {
        _isOnline.value = !forceOffline
        if (!forceOffline) {
            triggerSyncNow(expedited = true)
        }
    }

    // --- Memory Capsules & Collaborative Spaces ---

    fun addMemoryCapsule(title: String, caption: String, unlockDaysFromNow: Int?) {
        val author = _session.value.activeMember ?: FamilyMemberId.MAFITHA
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val unlockAt = unlockDaysFromNow?.let { now + it * 86_400_000L }
            val capsuleId = "cap_${UUID.randomUUID()}"
            dao.upsertCapsule(
                MemoryCapsuleEntity(
                    id = capsuleId,
                    authorId = author.name,
                    title = title,
                    caption = caption,
                    mediaType = if (unlockAt != null) "TIME_CAPSULE" else "NOTE",
                    mediaUri = null,
                    unlockAtMs = unlockAt,
                    isVaultOfflinePinned = true,
                    createdAtMs = now
                )
            )
            val dto = SupabaseCapsuleDto(
                id = capsuleId,
                author_id = author.name,
                title = title,
                caption = caption,
                media_type = if (unlockAt != null) "TIME_CAPSULE" else "NOTE",
                unlock_at_ms = unlockAt,
                created_at_ms = now
            )
            dao.enqueueSyncItem(
                OfflineSyncQueueEntity(
                    id = "sync_${UUID.randomUUID()}",
                    entityType = "CAPSULE",
                    entityId = capsuleId,
                    payloadJson = json.encodeToString(dto),
                    priority = "NORMAL",
                    retryCount = 0,
                    status = "PENDING",
                    lastError = null,
                    createdAtMs = now
                )
            )
            OfflineSyncWorker.enqueueSync(appContext)
        }
    }

    fun toggleCapsuleOfflinePin(capsuleId: String, pinned: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateCapsuleOfflinePin(capsuleId, pinned)
        }
    }

    fun rotateDailyMoment(category: MomentCategory) {
        val prompts = when (category) {
            MomentCategory.CONNECT -> listOf(
                "Tell each other one thing that made you smile today!",
                "Send a 3-second hug wave on video right now."
            )
            MomentCategory.PLAY -> listOf(
                "Everyone pull your funniest dinosaur face in 3, 2, 1!",
                "Make a goofy 5-second alien sound for Zayn!"
            )
            MomentCategory.REMEMBER -> listOf(
                "Record a funny quote Zayn Adam said this week into a Capsule!",
                "Pick your favorite photo from our last trip for the Vault."
            )
        }
        val author = _session.value.activeMember ?: FamilyMemberId.MAFITHA
        viewModelScope.launch(Dispatchers.IO) {
            dao.upsertActivity(
                FamilyActivityEntity(
                    id = "moment_today_1",
                    kind = "MOMENT",
                    title = prompts.random(),
                    subtitle = "Category: ${category.title} • ${category.subtitle}",
                    categoryOrTag = category.name,
                    authorId = author.name,
                    votesCount = 3,
                    costEstimate = null,
                    targetDateLabel = "Today",
                    isCompleted = false,
                    timestampMs = System.currentTimeMillis()
                )
            )
        }
    }

    fun addFamilyActivity(
        kind: String,
        title: String,
        subtitle: String,
        tag: String,
        costOrDuration: String? = null,
        dateLabel: String? = null
    ) {
        val author = _session.value.activeMember ?: FamilyMemberId.MITHUN
        viewModelScope.launch(Dispatchers.IO) {
            dao.upsertActivity(
                FamilyActivityEntity(
                    id = "${kind.lowercase()}_${UUID.randomUUID()}",
                    kind = kind,
                    title = title,
                    subtitle = subtitle,
                    categoryOrTag = tag,
                    authorId = author.name,
                    votesCount = 1,
                    costEstimate = costOrDuration,
                    targetDateLabel = dateLabel,
                    isCompleted = false,
                    timestampMs = System.currentTimeMillis()
                )
            )
        }
    }

    fun voteActivity(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.incrementActivityVote(id)
        }
    }

    fun toggleActivityCompleted(id: String, completed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.setActivityCompleted(id, completed)
        }
    }

    // --- Live Drawing Board & Story Together ---

    fun addDrawingStroke(color: Color, strokeWidthDp: Float, points: List<Pair<Float, Float>>) {
        if (points.size < 2) return
        val author = _session.value.activeMember ?: FamilyMemberId.ZAYN
        val strokeId = System.nanoTime()
        val colorArgb = (color.alpha * 255).toLong() shl 24 or
            ((color.red * 255).toLong() shl 16) or
            ((color.green * 255).toLong() shl 8) or
            (color.blue * 255).toLong()
        val stroke = LiveStroke(
            id = strokeId,
            author = author,
            colorArgb = colorArgb,
            strokeWidthDp = strokeWidthDp,
            points = points
        )
        _drawingStrokes.update { it + stroke }
        viewModelScope.launch {
            liveKitEngine.broadcastDrawingStroke(strokeId, author, colorArgb, strokeWidthDp, points)
        }
    }

    fun clearDrawingBoard() {
        _drawingStrokes.value = emptyList()
    }

    fun setStoryPage(index: Int) {
        _storyPageIndex.value = index.coerceIn(0, 3)
    }

    override fun onCleared() {
        super.onCleared()
        voiceCapture.release()
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback?.let { runCatching { cm.unregisterNetworkCallback(it) } }
    }
}
