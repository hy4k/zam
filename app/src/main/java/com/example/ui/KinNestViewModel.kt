package com.example.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ComicPhotoStyle
import com.example.ai.DailyComicGenerationState
import com.example.ai.DailyComicStylizerRepository
import com.example.ai.DailyTurnSchedule
import com.example.data.local.DailyComicPhotoEntity
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
import com.example.data.model.MemberPresenceStatus
import com.example.data.model.MessageType
import com.example.data.model.MomentCategory
import com.example.data.model.OfflineSyncState
import com.example.data.model.UserSessionState
import com.example.data.model.VoiceLanguage
import com.example.data.model.XoxoGameState
import com.example.data.model.XoxoMark
import com.example.data.model.XoxoPlayMode
import com.example.media.LiveKitCallEngine
import com.example.media.LiveKitDataPacket
import com.example.media.StopRecordingResult
import com.example.media.VoiceAndMediaCapture
import com.example.security.DeviceKeystoreManager
import com.example.security.ParentalAuthManager
import com.example.sync.FamilyRealtimeSignal
import com.example.sync.KinNestSyncRepository
import com.example.sync.OfflineSyncWorker
import com.example.sync.SupabaseCallLogDto
import com.example.sync.SupabaseCapsuleDto
import com.example.sync.SupabaseFamilyMessageDto
import com.example.sync.SupabaseMessageDto
import com.example.sync.SupabaseParentLogDto
import com.example.sync.SupabaseProfileDto
import com.example.sync.SupabaseRealtimeSignaling
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    val realtimeSignaling = SupabaseRealtimeSignaling()
    val liveKitEngine = LiveKitCallEngine(appContext)
    val telecomManager = FamilyTelecomManager(appContext)
    val voiceCapture = VoiceAndMediaCapture(appContext)
    private val dailyComicRepo = DailyComicStylizerRepository(appContext)
    private val json = Json { ignoreUnknownKeys = true }

    val liveKitRoom = liveKitEngine.activeRoom
    val isLiveStreamActive = liveKitEngine.isLiveStreamActive
    val videoTracksByMember = liveKitEngine.videoTracksByMember
    val connectedLiveMembers = liveKitEngine.connectedLiveMembers

    private val devicePrefs = appContext.getSharedPreferences(PREFS_DEVICE_BINDING, Context.MODE_PRIVATE)
    private val initialDeviceOwner: FamilyMemberId? = devicePrefs
        .getString(KEY_ASSIGNED_MEMBER, null)
        ?.let { stored -> runCatching { FamilyMemberId.valueOf(stored) }.getOrNull() }

    private val _session = MutableStateFlow(
        UserSessionState(
            deviceOwner = initialDeviceOwner,
            activeMember = initialDeviceOwner,
            isAuthenticated = initialDeviceOwner != null,
            isDeviceTrusted = true
        )
    )
    val session: StateFlow<UserSessionState> = _session.asStateFlow()

    private val _callState = MutableStateFlow(
        CallState(
            roomName = LiveKitCallEngine.DEFAULT_FAMILY_ROOM,
            recentCallLogs = listOf(
                CallLogEntry("c1", "Mithun, Mafitha & Zayn", "3-Way Video", false, "Today • 8:05 AM", "14m 22s"),
                CallLogEntry("c2", "Mafitha (Ummi)", "1-on-1 Video", false, "Yesterday • 6:40 PM", "06m 10s"),
                CallLogEntry("c3", "Mithun (Vapi)", "Missed Call", true, "Yesterday • 1:15 PM", "Tap to call back")
            )
        )
    )
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _isOnline = MutableStateFlow(checkCurrentConnectivity())
    private val _isSyncingNow = MutableStateFlow(false)
    private val _lastSyncedLabel = MutableStateFlow("Up to date")

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

    val dailyComicPhotos: StateFlow<List<DailyComicPhotoEntity>> = dao.observeDailyComicPhotos()
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

    private val _dailyComicGenState = MutableStateFlow<DailyComicGenerationState>(DailyComicGenerationState.Idle)
    val dailyComicGenState: StateFlow<DailyComicGenerationState> = _dailyComicGenState.asStateFlow()

    private val _memberPresence = MutableStateFlow(
        FamilyMemberId.entries.associateWith { member ->
            MemberPresenceStatus(
                memberId = member,
                isOnline = member == initialDeviceOwner && _isOnline.value,
                lastSeenMs = if (member == initialDeviceOwner) System.currentTimeMillis() else null
            )
        }
    )
    val memberPresence: StateFlow<Map<FamilyMemberId, MemberPresenceStatus>> = _memberPresence.asStateFlow()

    private val _xoxoState = MutableStateFlow(defaultXoxoStateFor(initialDeviceOwner))
    val xoxoState: StateFlow<XoxoGameState> = _xoxoState.asStateFlow()

    private val _uiFeedback = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val uiFeedback = _uiFeedback.asSharedFlow()

    private var isAppInForeground = true
    private var callTimerJob: Job? = null
    private var presenceLoopJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // Ensure initial family profiles & starter activities exist on first launch
            if (dao.countProfiles() == 0) {
                KinNestDatabase.ensureSeeded(appContext, dao)
            }
            if (checkCurrentConnectivity()) {
                flushPendingOfflineMessages()
                hydrateFromSupabase()
            }
        }
        registerConnectivityMonitor()
        observeLiveKitDataChannel()
        observeLiveKitRoomState()
        observeRealtimeSignaling()
        if (_isOnline.value) {
            realtimeSignaling.connectIfConfigured()
        }
        startPresenceLoop()
    }

    private suspend fun hydrateFromSupabase() {
        if (!syncRepo.isSupabaseConfigured() || !checkCurrentConnectivity()) return

        // 1. Hydrate Family Profiles (Profile Pics, Daily Badges, Halo Colors)
        val remoteProfiles = syncRepo.fetchAllProfiles()
        if (remoteProfiles.isNotEmpty()) {
            val localById = dao.getAllProfiles().associateBy { it.memberId }
            remoteProfiles.forEach { dto ->
                val member = runCatching { FamilyMemberId.valueOf(dto.member_id) }.getOrNull() ?: return@forEach
                val existing = localById[dto.member_id]
                val merged = if (existing != null) {
                    existing.copy(
                        displayName = dto.display_name.ifBlank { existing.displayName },
                        roleLabel = dto.role_label.ifBlank { existing.roleLabel },
                        customPhotoUri = dto.avatar_url?.takeIf { it.isNotBlank() } ?: existing.customPhotoUri,
                        badgeElement = dto.badge_element.ifBlank { existing.badgeElement },
                        haloColorHex = dto.halo_color_hex.takeIf { it != 0L } ?: existing.haloColorHex
                    )
                } else {
                    UserProfileEntity(
                        memberId = member.name,
                        displayName = dto.display_name.ifBlank { member.displayName },
                        roleLabel = dto.role_label.ifBlank { member.title },
                        customPhotoUri = dto.avatar_url?.takeIf { it.isNotBlank() },
                        badgeElement = dto.badge_element.ifBlank { BadgeElement.STARLIGHT.name },
                        haloColorHex = dto.halo_color_hex.takeIf { it != 0L } ?: 0xFF38BDF8,
                        deviceKeyAlias = "kinnest_device_${member.name.lowercase()}",
                        devicePublicFingerprint = "TRUSTED-DEVICE",
                        isDeviceTrusted = true,
                        biometricEnabled = member != FamilyMemberId.ZAYN
                    )
                }
                dao.upsertProfile(merged)
            }
        }

        // 2. Hydrate Family Chat Messages, Voice Clips, Shared Photos/Videos & Reactions
        val remoteMessages = syncRepo.fetchRecentMessages(limit = 150L)
        remoteMessages.forEach { dto ->
            val existing = dao.getMessageById(dto.id)
            val encrypted = if (existing != null) {
                existing.ciphertextPayload to existing.ivBase64
            } else {
                keystore.encryptMessageForLocalVault(dto.body)
            }
            dao.upsertMessage(
                EncryptedMessageEntity(
                    id = dto.id,
                    threadId = dto.thread_id.ifBlank { "family_group" },
                    senderId = dto.sender_id,
                    recipientIdsCsv = dto.recipient_ids_csv.ifBlank { "MITHUN,MAFITHA,ZAYN" },
                    ciphertextPayload = encrypted.first,
                    ivBase64 = encrypted.second,
                    decryptedPreview = dto.body,
                    messageType = dto.message_type.ifBlank { MessageType.TEXT.name },
                    mediaLocalPath = existing?.mediaLocalPath,
                    mediaRemoteUrl = dto.media_url?.takeIf { it.isNotBlank() } ?: existing?.mediaRemoteUrl,
                    reactionEmoji = dto.reaction_emoji ?: existing?.reactionEmoji,
                    deliveryStatus = DeliveryStatus.DELIVERED.name,
                    readByCsv = existing?.readByCsv ?: dto.sender_id,
                    timestampMs = dto.timestamp_ms
                )
            )
        }

        // 3. Hydrate Live 2-of-3 XOXO Game Board & Scoreboard
        val remoteXoxo = syncRepo.fetchXoxoGame()
        if (remoteXoxo != null && remoteXoxo.lastUpdatedMs > _xoxoState.value.lastUpdatedMs) {
            _xoxoState.value = remoteXoxo
        }

        // 4. Hydrate Family Online / Last-Seen Presence
        val remotePresence = syncRepo.fetchAllPresence()
        if (remotePresence.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val owner = _session.value.deviceOwner
            _memberPresence.update { current ->
                val mutable = current.toMutableMap()
                remotePresence.forEach { dto ->
                    val member = runCatching { FamilyMemberId.valueOf(dto.member_id) }.getOrNull() ?: return@forEach
                    if (member != owner) {
                        val freshOnline = dto.is_online && dto.last_seen_ms > 0L && (now - dto.last_seen_ms) <= PRESENCE_TIMEOUT_MS
                        val existing = mutable[member]
                        val bestLastSeen = maxOf(dto.last_seen_ms, existing?.lastSeenMs ?: 0L).takeIf { it > 0L }
                        mutable[member] = MemberPresenceStatus(
                            memberId = member,
                            isOnline = freshOnline || (existing?.isOnline == true && existing.lastSeenMs != null && (now - existing.lastSeenMs) <= PRESENCE_TIMEOUT_MS),
                            lastSeenMs = bestLastSeen
                        )
                    }
                }
                mutable
            }
        }

        // 5. Hydrate Parent Settings Grown-Up Logs
        val remoteParentLogs = syncRepo.fetchParentLogs()
        remoteParentLogs.forEach { dto ->
            dao.upsertActivity(
                FamilyActivityEntity(
                    id = dto.id,
                    kind = "PARENT_LOG",
                    title = dto.title,
                    subtitle = dto.subtitle,
                    categoryOrTag = "PARENT",
                    authorId = dto.author_id,
                    votesCount = 1,
                    costEstimate = dto.cost_estimate,
                    targetDateLabel = dto.target_date_label ?: "Adults Only",
                    isCompleted = false,
                    timestampMs = dto.created_at_ms
                )
            )
        }

        // 6. Hydrate Recent Video Call Logs
        val remoteCalls = syncRepo.fetchRecentCallLogs(limit = 20L)
        if (remoteCalls.isNotEmpty()) {
            val fmt = SimpleDateFormat("MMM d • h:mm a", Locale.getDefault())
            val mapped = remoteCalls.map { dto ->
                val names = dto.participants_csv.split(",")
                    .mapNotNull { runCatching { FamilyMemberId.valueOf(it.trim()).displayName }.getOrNull() }
                    .joinToString(", ")
                    .ifBlank { dto.participants_csv }
                val mins = dto.duration_seconds / 60L
                val secs = dto.duration_seconds % 60L
                CallLogEntry(
                    id = dto.id,
                    participantsLabel = names,
                    callTypeLabel = if (dto.is_three_way) "3-Way Video" else "1-on-1 Video",
                    isMissed = false,
                    timestampLabel = fmt.format(Date(dto.started_at_ms)),
                    durationLabel = "%02d:%02d".format(mins, secs)
                )
            }
            _callState.update { it.copy(recentCallLogs = mapped) }
        }
    }

    private fun checkCurrentConnectivity(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            (
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                )
    }

    private fun registerConnectivityMonitor() {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                updateSelfPresence(online = isAppInForeground)
                if (isAppInForeground) {
                    realtimeSignaling.connectIfConfigured()
                    _session.value.deviceOwner?.let { owner ->
                        realtimeSignaling.broadcastPresencePing(owner, isHello = true)
                    }
                }
                triggerSyncNow(expedited = false)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val currentlyOnline = hasInternet || checkCurrentConnectivity()
                if (_isOnline.value != currentlyOnline) {
                    _isOnline.value = currentlyOnline
                    updateSelfPresence(online = currentlyOnline && isAppInForeground)
                    if (currentlyOnline && isAppInForeground) {
                        realtimeSignaling.connectIfConfigured()
                        triggerSyncNow(expedited = false)
                    }
                }
            }

            override fun onLost(network: Network) {
                // Re-verify active default network so switching Wi-Fi <-> Cellular never shows a false offline banner
                val stillConnected = checkCurrentConnectivity()
                _isOnline.value = stillConnected
                if (!stillConnected) {
                    updateSelfPresence(online = false)
                    realtimeSignaling.disconnect()
                }
            }
        }
        networkCallback = cb
        runCatching {
            cm.registerDefaultNetworkCallback(cb)
        }.onFailure {
            runCatching {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                cm.registerNetworkCallback(request, cb)
            }
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

    private fun observeLiveKitRoomState() {
        viewModelScope.launch {
            liveKitEngine.connectionStatusLabel.collect { statusLabel ->
                _callState.update { it.copy(networkQualityLabel = statusLabel) }
            }
        }
        viewModelScope.launch {
            combine(
                liveKitEngine.connectedLiveMembers,
                liveKitEngine.speakingMembers,
                liveKitEngine.videoTracksByMember,
                liveKitEngine.isLiveStreamActive
            ) { connected, speaking, tracks, liveActive ->
                Triple(connected, speaking, Pair(tracks, liveActive))
            }.collect { (connected, speaking, pair) ->
                val (tracks, liveActive) = pair
                if (!liveActive) return@collect
                _callState.update { state ->
                    if (state.phase !is CallPhase.Active && state.phase !is CallPhase.DialingOutgoing) {
                        return@update state
                    }
                    val existingMap = state.participants.associateBy { it.memberId }.toMutableMap()
                    connected.forEach { memberId ->
                        val prev = existingMap[memberId]
                        val isSpk = memberId in speaking
                        val hasCam = tracks.containsKey(memberId)
                        existingMap[memberId] = (prev ?: CallParticipantState(
                            memberId = memberId,
                            isConnected = true,
                            isSpeaking = isSpk
                        )).copy(
                            isConnected = true,
                            isSpeaking = isSpk,
                            audioLevel = if (isSpk) 0.85f else 0.14f,
                            isCameraEnabled = if (memberId == _session.value.activeMember) {
                                state.isLocalCameraOn
                            } else {
                                hasCam
                            }
                        )
                    }
                    // Keep invited members visible while waiting for them to join
                    val ordered = existingMap.values.sortedBy { p ->
                        if (p.memberId == _session.value.activeMember) 0
                        else if (p.memberId in connected) 1
                        else 2
                    }
                    state.copy(participants = ordered)
                }
            }
        }
    }

    private fun observeRealtimeSignaling() {
        viewModelScope.launch {
            realtimeSignaling.incomingSignals.collect { signal ->
                val self = _session.value.activeMember
                val sender = runCatching { FamilyMemberId.valueOf(signal.senderId) }.getOrNull()
                    ?: return@collect
                if (self != null && sender == self) return@collect

                val nowMs = System.currentTimeMillis()
                // Any live signal from another family member proves they are online right now
                if (signal.kind != SupabaseRealtimeSignaling.KIND_PRESENCE_OFFLINE) {
                    markRemoteMemberOnline(sender, nowMs)
                }

                when (signal.kind) {
                    SupabaseRealtimeSignaling.KIND_PRESENCE_PING -> {
                        if (signal.text == "HELLO" && self != null && isAppInForeground && _isOnline.value) {
                            realtimeSignaling.broadcastPresencePing(self, isHello = false)
                        }
                    }
                    SupabaseRealtimeSignaling.KIND_PRESENCE_OFFLINE -> {
                        markRemoteMemberOffline(sender, signal.timestampMs.takeIf { it > 0L } ?: nowMs)
                    }
                    SupabaseRealtimeSignaling.KIND_CALL_INVITE -> {
                        val targets = signal.targetIdsCsv.split(",").map { it.trim() }
                        val isForMe = self == null || targets.contains(self.name)
                        if (isForMe && _callState.value.phase is CallPhase.Idle) {
                            telecomManager.postLockScreenIncomingCallNotification(
                                caller = sender,
                                isThreeWay = signal.isThreeWay,
                                quietHoursActive = _session.value.quietHoursEnabled
                            )
                            _callState.update {
                                it.copy(
                                    roomName = signal.roomName.ifBlank { LiveKitCallEngine.DEFAULT_FAMILY_ROOM },
                                    phase = CallPhase.RingingIncoming(
                                        caller = sender,
                                        isThreeWay = signal.isThreeWay,
                                        roomName = signal.roomName.ifBlank { LiveKitCallEngine.DEFAULT_FAMILY_ROOM }
                                    )
                                )
                            }
                        }
                    }
                    SupabaseRealtimeSignaling.KIND_HEARTBEAT -> {
                        _heartbeatBanner.value = HeartbeatBannerAlert(
                            sender = sender,
                            message = signal.text.ifBlank { "Thinking of you!" },
                            isSafetyAlert = signal.isSafetyAlert,
                            timestampMs = signal.timestampMs.takeIf { it > 0L } ?: System.currentTimeMillis()
                        )
                        speakHelper(
                            english = "${sender.displayName} sent a heartbeat: ${signal.text}",
                            malayalam = "${sender.displayName} സന്ദേശം അയച്ചു: ${signal.text}"
                        )
                    }
                    SupabaseRealtimeSignaling.KIND_CHAT_MESSAGE -> {
                        if (signal.messageId.isBlank() || (signal.text.isBlank() && signal.mediaUrl.isBlank())) return@collect
                        viewModelScope.launch(Dispatchers.IO) {
                            val existing = dao.getMessageById(signal.messageId)
                            val encrypted = keystore.encryptMessageForLocalVault(signal.text)
                            dao.upsertMessage(
                                EncryptedMessageEntity(
                                    id = signal.messageId,
                                    threadId = signal.threadId.ifBlank { "family_group" },
                                    senderId = sender.name,
                                    recipientIdsCsv = signal.targetIdsCsv.ifBlank { "MITHUN,MAFITHA,ZAYN" },
                                    ciphertextPayload = encrypted.first,
                                    ivBase64 = encrypted.second,
                                    decryptedPreview = signal.text,
                                    messageType = signal.messageType.ifBlank { MessageType.TEXT.name },
                                    mediaLocalPath = existing?.mediaLocalPath,
                                    mediaRemoteUrl = signal.mediaUrl.ifBlank { existing?.mediaRemoteUrl },
                                    reactionEmoji = existing?.reactionEmoji,
                                    deliveryStatus = DeliveryStatus.DELIVERED.name,
                                    readByCsv = sender.name,
                                    timestampMs = signal.timestampMs.takeIf { it > 0L } ?: System.currentTimeMillis()
                                )
                            )
                        }
                    }
                    SupabaseRealtimeSignaling.KIND_PROFILE_UPDATE -> {
                        viewModelScope.launch(Dispatchers.IO) {
                            val existing = dao.getAllProfiles().firstOrNull { it.memberId == sender.name }
                                ?: return@launch
                            dao.upsertProfile(
                                existing.copy(
                                    badgeElement = signal.comicStyle.ifBlank { existing.badgeElement },
                                    customPhotoUri = signal.mediaUrl.ifBlank { existing.customPhotoUri }
                                )
                            )
                        }
                    }
                    SupabaseRealtimeSignaling.KIND_MESSAGE_REACTION -> {
                        if (signal.messageId.isBlank()) return@collect
                        viewModelScope.launch(Dispatchers.IO) {
                            dao.updateMessageReaction(signal.messageId, signal.text.ifBlank { null })
                        }
                    }
                    SupabaseRealtimeSignaling.KIND_PARENT_LOG -> {
                        if (signal.messageId.isBlank() || signal.text.isBlank()) return@collect
                        viewModelScope.launch(Dispatchers.IO) {
                            dao.upsertActivity(
                                FamilyActivityEntity(
                                    id = signal.messageId,
                                    kind = "PARENT_LOG",
                                    title = signal.text,
                                    subtitle = signal.comicStyle,
                                    categoryOrTag = "PARENT",
                                    authorId = sender.name,
                                    votesCount = 1,
                                    costEstimate = signal.dateKey.ifBlank { null },
                                    targetDateLabel = "Adults Only",
                                    isCompleted = false,
                                    timestampMs = signal.timestampMs.takeIf { it > 0L } ?: System.currentTimeMillis()
                                )
                            )
                        }
                    }
                    SupabaseRealtimeSignaling.KIND_DAILY_COMIC -> {
                        if (signal.dateKey.isBlank() || signal.imageBase64.isBlank()) return@collect
                        viewModelScope.launch(Dispatchers.IO) {
                            val savedPath = dailyComicRepo.saveReceivedBase64Comic(
                                dateKey = signal.dateKey,
                                base64Data = signal.imageBase64
                            ) ?: return@launch
                            dao.upsertDailyComicPhoto(
                                DailyComicPhotoEntity(
                                    dateKey = signal.dateKey,
                                    id = signal.messageId.ifBlank { "comic_${UUID.randomUUID()}" },
                                    authorId = sender.name,
                                    caption = signal.text,
                                    comicStyle = signal.comicStyle.ifBlank { ComicPhotoStyle.SUNNY_COMIC.id },
                                    localImagePath = savedPath,
                                    reactionEmoji = null,
                                    createdAtMs = signal.timestampMs.takeIf { it > 0L } ?: System.currentTimeMillis()
                                )
                            )
                            _uiFeedback.tryEmit("${sender.displayName} posted today's family comic photo!")
                        }
                    }
                    SupabaseRealtimeSignaling.KIND_XOXO_STATE -> {
                        if (signal.text.isBlank()) return@collect
                        val incoming = runCatching {
                            json.decodeFromString<XoxoGameState>(signal.text)
                        }.getOrNull() ?: return@collect
                        val current = _xoxoState.value
                        if (incoming.lastUpdatedMs >= current.lastUpdatedMs) {
                            _xoxoState.value = incoming.copy(playMode = XoxoPlayMode.LIVE_TWO_PHONES)
                        }
                    }
                }
            }
        }
    }

    private fun startPresenceLoop() {
        presenceLoopJob?.cancel()
        presenceLoopJob = viewModelScope.launch {
            var firstPing = true
            while (true) {
                val onlineNow = checkCurrentConnectivity()
                if (_isOnline.value != onlineNow) {
                    _isOnline.value = onlineNow
                }
                val owner = _session.value.deviceOwner
                val now = System.currentTimeMillis()
                if (owner != null && isAppInForeground && onlineNow) {
                    updateSelfPresence(online = true)
                    realtimeSignaling.broadcastPresencePing(owner, isHello = firstPing)
                    viewModelScope.launch(Dispatchers.IO) {
                        syncRepo.upsertPresence(owner, isOnline = true, nowMs = now)
                    }
                    firstPing = false
                }
                // Expire stale remote online flags after 30 seconds without a ping
                _memberPresence.update { current ->
                    current.mapValues { (member, status) ->
                        if (member == owner) {
                            status.copy(
                                isOnline = isAppInForeground && onlineNow,
                                lastSeenMs = if (isAppInForeground && onlineNow) now else status.lastSeenMs
                            )
                        } else if (status.isOnline && status.lastSeenMs != null && (now - status.lastSeenMs) > PRESENCE_TIMEOUT_MS) {
                            status.copy(isOnline = false)
                        } else {
                            status
                        }
                    }
                }
                delay(12_000L)
            }
        }
    }

    private fun updateSelfPresence(online: Boolean) {
        val owner = _session.value.deviceOwner ?: return
        val now = System.currentTimeMillis()
        _memberPresence.update { current ->
            val prev = current[owner] ?: MemberPresenceStatus(owner)
            current + (owner to prev.copy(
                isOnline = online,
                lastSeenMs = if (online) now else (prev.lastSeenMs ?: now)
            ))
        }
    }

    private fun markRemoteMemberOnline(member: FamilyMemberId, timestampMs: Long) {
        _memberPresence.update { current ->
            current + (member to MemberPresenceStatus(
                memberId = member,
                isOnline = true,
                lastSeenMs = timestampMs
            ))
        }
    }

    private fun markRemoteMemberOffline(member: FamilyMemberId, timestampMs: Long) {
        _memberPresence.update { current ->
            current + (member to MemberPresenceStatus(
                memberId = member,
                isOnline = false,
                lastSeenMs = timestampMs
            ))
        }
    }

    fun onAppForegrounded() {
        isAppInForeground = true
        val onlineNow = checkCurrentConnectivity()
        _isOnline.value = onlineNow
        val owner = _session.value.deviceOwner
        if (owner != null) {
            updateSelfPresence(online = onlineNow)
        }
        if (onlineNow) {
            realtimeSignaling.connectIfConfigured()
            if (owner != null) {
                realtimeSignaling.broadcastPresencePing(owner, isHello = true)
            }
            viewModelScope.launch(Dispatchers.IO) {
                if (owner != null) {
                    syncRepo.upsertPresence(owner, isOnline = true, nowMs = System.currentTimeMillis())
                }
                flushPendingOfflineMessages()
                hydrateFromSupabase()
            }
        }
    }

    fun onAppBackgrounded() {
        isAppInForeground = false
        val owner = _session.value.deviceOwner
        if (owner != null) {
            updateSelfPresence(online = false)
            viewModelScope.launch(Dispatchers.IO) {
                syncRepo.upsertPresence(owner, isOnline = false, nowMs = System.currentTimeMillis())
            }
        }
        // Cleanly close Realtime WebSocket while backgrounded so Android does not abort an idle socket
        realtimeSignaling.disconnectGracefully(offlineMember = owner)
    }

    // --- Equipment-Assigned Phone Ownership (No passwords, no accidental switching) ---

    fun assignThisDeviceToMember(member: FamilyMemberId) {
        val previousOwner = _session.value.deviceOwner
        if (previousOwner != null && previousOwner != member) {
            realtimeSignaling.broadcastPresenceOffline(previousOwner)
        }
        devicePrefs.edit().putString(KEY_ASSIGNED_MEMBER, member.name).apply()
        val alias = "kinnest_device_${member.name.lowercase()}"
        val fp = keystore.ensureDeviceIdentityKey(alias)
        _session.update {
            it.copy(
                deviceOwner = member,
                activeMember = member,
                isAuthenticated = true,
                isDeviceTrusted = true,
                hardwareKeyAlias = alias,
                hardwareKeyFingerprint = fp,
                isParentalUnlocked = false
            )
        }
        val now = System.currentTimeMillis()
        _memberPresence.update { current ->
            current.mapValues { (id, status) ->
                when (id) {
                    member -> MemberPresenceStatus(id, isOnline = _isOnline.value, lastSeenMs = now)
                    previousOwner -> status.copy(isOnline = false)
                    else -> status
                }
            }
        }
        if (_isOnline.value) {
            realtimeSignaling.connectIfConfigured()
            realtimeSignaling.broadcastPresencePing(member, isHello = true)
            viewModelScope.launch(Dispatchers.IO) {
                if (previousOwner != null && previousOwner != member) {
                    syncRepo.upsertPresence(previousOwner, isOnline = false, nowMs = now)
                }
                syncRepo.upsertPresence(member, isOnline = true, nowMs = now)
                hydrateFromSupabase()
            }
        }
        _uiFeedback.tryEmit("This phone is now set to ${member.displayName} (${member.title})!")
        if (member == FamilyMemberId.ZAYN) {
            speakHelper(
                english = "Hi Zayn Adam! Welcome to A 12 E.",
                malayalam = "ഹായ് സെയ്ൻ ആദം! സ്വാഗതം."
            )
        }
    }

    fun reassignDeviceOwner(newOwner: FamilyMemberId) {
        assignThisDeviceToMember(newOwner)
    }

    fun signInWithDeviceBoundAvatar(member: FamilyMemberId): Boolean {
        // Always bind the device only if unassigned; otherwise preserve the phone's assigned owner
        val currentOwner = _session.value.deviceOwner
        if (currentOwner == null) {
            assignThisDeviceToMember(member)
            return true
        }
        if (currentOwner != member) {
            _uiFeedback.tryEmit("This phone belongs to ${currentOwner.displayName} (${currentOwner.title}).")
            return false
        }
        return true
    }

    fun toggleDeviceTrustSimulation(memberId: String, trusted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = profiles.value.firstOrNull { it.memberId == memberId } ?: return@launch
            dao.upsertProfile(profile.copy(isDeviceTrusted = trusted))
        }
    }

    fun updateMemberPersonalization(
        member: FamilyMemberId,
        badge: BadgeElement,
        photoUri: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = dao.getAllProfiles().firstOrNull { it.memberId == member.name } ?: return@launch
            val localPhotoUri = photoUri ?: existing.customPhotoUri
            // 1. Save immediately to Room so UI updates without delay
            dao.upsertProfile(
                existing.copy(
                    badgeElement = badge.name,
                    customPhotoUri = localPhotoUri
                )
            )

            // 2. Upload avatar JPEG to Supabase Storage & upsert family_profiles so all 3 phones sync
            var finalAvatarUrl = localPhotoUri
            if (checkCurrentConnectivity() && syncRepo.isSupabaseConfigured()) {
                if (!localPhotoUri.isNullOrBlank() && !localPhotoUri.startsWith("https://")) {
                    val uploadedUrl = syncRepo.uploadAvatarPhoto(appContext, member, localPhotoUri)
                    if (!uploadedUrl.isNullOrBlank()) {
                        finalAvatarUrl = uploadedUrl
                        dao.upsertProfile(
                            existing.copy(
                                badgeElement = badge.name,
                                customPhotoUri = uploadedUrl
                            )
                        )
                    }
                }
                val remoteAvatar = finalAvatarUrl?.takeIf { it.startsWith("https://") }
                syncRepo.upsertProfile(
                    SupabaseProfileDto(
                        member_id = member.name,
                        display_name = existing.displayName,
                        role_label = existing.roleLabel,
                        avatar_url = remoteAvatar,
                        badge_element = badge.name,
                        halo_color_hex = existing.haloColorHex,
                        updated_at_ms = System.currentTimeMillis()
                    )
                )
                realtimeSignaling.broadcastProfileUpdate(
                    member = member,
                    badgeElement = badge.name,
                    avatarUrl = remoteAvatar
                )
            }
            _uiFeedback.emit("Updated ${member.displayName}'s photo & badge!")
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

    // --- Real-Time 3-Way Video & Voice Engine (LiveKit + Core-Telecom + Supabase Signaling) ---

    fun startFamilyCall(targets: List<FamilyMemberId>) {
        val self = _session.value.activeMember ?: FamilyMemberId.MITHUN
        val roomName = LiveKitCallEngine.DEFAULT_FAMILY_ROOM
        val allInRoom = (listOf(self) + targets).distinct()
        val participantStates = allInRoom.map { member ->
            CallParticipantState(
                memberId = member,
                isConnected = member == self,
                isSpeaking = false,
                audioLevel = if (member == self) 0.25f else 0f,
                isMuted = false,
                isCameraEnabled = member == self,
                activeFilter = if (member == self) _callState.value.activeFilter else ArFilterType.NONE
            )
        }

        _callState.update {
            it.copy(
                roomName = roomName,
                phase = CallPhase.DialingOutgoing(targets),
                participants = participantStates,
                durationSeconds = 0L,
                networkQualityLabel = "Connecting your call…"
            )
        }

        // Ring other family devices immediately over Supabase Realtime Broadcast
        realtimeSignaling.broadcastCallInvite(
            caller = self,
            targets = targets.filter { it != self },
            roomName = roomName,
            isThreeWay = allInRoom.size >= 3
        )

        val primaryTarget = targets.firstOrNull() ?: FamilyMemberId.MAFITHA
        telecomManager.startManagedFamilyCall(
            callerOrTarget = primaryTarget,
            isIncoming = false,
            isVideo = true,
            onAnswered = { connectCallMedia(allInRoom, roomName) },
            onDisconnected = { endCallInternal("Call ended") }
        )

        viewModelScope.launch {
            delay(450)
            if (_callState.value.phase is CallPhase.DialingOutgoing) {
                connectCallMedia(allInRoom, roomName)
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
                roomName = LiveKitCallEngine.DEFAULT_FAMILY_ROOM,
                phase = CallPhase.RingingIncoming(
                    caller = caller,
                    isThreeWay = isThreeWay,
                    roomName = LiveKitCallEngine.DEFAULT_FAMILY_ROOM
                )
            )
        }
    }

    fun acceptIncomingCall() {
        val incoming = _callState.value.phase as? CallPhase.RingingIncoming ?: return
        val self = _session.value.activeMember ?: FamilyMemberId.MAFITHA
        val members = if (incoming.isThreeWay) {
            listOf(self, FamilyMemberId.MITHUN, FamilyMemberId.MAFITHA, FamilyMemberId.ZAYN).distinct()
        } else {
            listOf(self, incoming.caller).distinct()
        }
        telecomManager.cancelIncomingCallNotification()
        connectCallMedia(members, incoming.roomName.ifBlank { LiveKitCallEngine.DEFAULT_FAMILY_ROOM })
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
        val self = _session.value.activeMember ?: FamilyMemberId.MITHUN
        realtimeSignaling.broadcastCallInvite(
            caller = self,
            targets = listOf(member),
            roomName = _callState.value.roomName.ifBlank { LiveKitCallEngine.DEFAULT_FAMILY_ROOM },
            isThreeWay = true
        )
        _callState.update { state ->
            if (state.participants.any { it.memberId == member }) return@update state
            state.copy(
                participants = state.participants + CallParticipantState(
                    memberId = member,
                    isConnected = false,
                    isSpeaking = false,
                    audioLevel = 0f,
                    isMuted = false,
                    isCameraEnabled = false
                )
            )
        }
        _uiFeedback.tryEmit("Ringing ${member.displayName} to join the family call!")
    }

    private fun connectCallMedia(members: List<FamilyMemberId>, roomName: String) {
        if (_callState.value.phase is CallPhase.Active) return
        val self = _session.value.activeMember ?: FamilyMemberId.MITHUN
        _callState.update { state ->
            state.copy(
                roomName = roomName,
                phase = CallPhase.Active,
                participants = members.map { member ->
                    CallParticipantState(
                        memberId = member,
                        isConnected = member == self,
                        isSpeaking = false,
                        audioLevel = if (member == self) 0.25f else 0f,
                        isMuted = false,
                        isCameraEnabled = member == self
                    )
                }
            )
        }
        viewModelScope.launch {
            CallForegroundService.start(
                appContext,
                members.joinToString(", ") { it.displayName }
            )
            startCallTimerAndWaveformLoop()

            val connectResult = liveKitEngine.connectToFamilyRoom(
                member = self,
                roomName = roomName,
                enableMic = !_callState.value.isLocalMicMuted,
                enableCamera = _callState.value.isLocalCameraOn
            )
            connectResult.onFailure {
                _uiFeedback.tryEmit("Couldn't connect the call right now — please check internet.")
            }
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
                val liveActive = liveKitEngine.isLiveStreamActive.value
                _callState.update { state ->
                    val nextSec = state.durationSeconds + 1
                    if (liveActive) {
                        state.copy(durationSeconds = nextSec)
                    } else {
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
        viewModelScope.launch { liveKitEngine.switchCamera() }
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
                    phase = CallPhase.Reconnecting("Reconnecting call…"),
                    networkQualityLabel = "Reconnecting…"
                )
            }
            delay(1800)
            _callState.update {
                it.copy(
                    phase = CallPhase.Active,
                    networkQualityLabel = "Connected"
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
        val callId = "call_${UUID.randomUUID()}"
        val participantsCsv = snapshot.participants.joinToString(",") { it.memberId.name }
            .ifBlank { (_session.value.activeMember ?: FamilyMemberId.MITHUN).name }
        val log = CallLogEntry(
            id = callId,
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
        if (snapshot.durationSeconds > 0L) {
            val caller = _session.value.deviceOwner ?: _session.value.activeMember ?: FamilyMemberId.MITHUN
            val startedAtMs = (System.currentTimeMillis() - snapshot.durationSeconds * 1000L).coerceAtLeast(1L)
            viewModelScope.launch(Dispatchers.IO) {
                syncRepo.insertCallLog(
                    SupabaseCallLogDto(
                        id = callId,
                        caller_id = caller.name,
                        participants_csv = participantsCsv,
                        room_name = snapshot.roomName.ifBlank { LiveKitCallEngine.DEFAULT_FAMILY_ROOM },
                        is_three_way = snapshot.participants.size >= 3,
                        duration_seconds = snapshot.durationSeconds,
                        started_at_ms = startedAtMs
                    )
                )
            }
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
        realtimeSignaling.broadcastHeartbeat(sender, label, isSafetyAlert)
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
                    plaintext = "🎙️ Voice Note (${durationSec}s)",
                    type = MessageType.VOICE_AAC,
                    mediaLocalPath = result.file.absolutePath,
                    expedited = true
                )
                _uiFeedback.tryEmit("Sent ${durationSec}s voice note!")
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
            val onlineNow = checkCurrentConnectivity()
            _isOnline.value = onlineNow
            val now = System.currentTimeMillis()
            val msgId = "msg_${UUID.randomUUID()}"
            val encrypted = keystore.encryptMessageForLocalVault(plaintext)
            val initialStatus = if (onlineNow) {
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

            if (onlineNow) {
                val remoteMediaUrl = uploadMessageMediaIfNeeded(msgId, type, mediaLocalPath)
                if (!remoteMediaUrl.isNullOrBlank()) {
                    dao.upsertMessage(entity.copy(mediaRemoteUrl = remoteMediaUrl))
                }
                syncRepo.upsertFamilyMessage(
                    SupabaseFamilyMessageDto(
                        id = msgId,
                        thread_id = threadId,
                        sender_id = sender.name,
                        recipient_ids_csv = recipients,
                        body = plaintext,
                        message_type = type.name,
                        media_url = remoteMediaUrl,
                        reaction_emoji = null,
                        delivery_status = DeliveryStatus.DELIVERED.name,
                        timestamp_ms = now
                    )
                )
                realtimeSignaling.broadcastChatMessage(
                    messageId = msgId,
                    sender = sender,
                    threadId = threadId,
                    recipientsCsv = recipients,
                    text = plaintext,
                    messageType = type.name,
                    timestampMs = now,
                    mediaUrl = remoteMediaUrl.orEmpty()
                )
            } else {
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
                        status = "PENDING",
                        lastError = "Waiting for internet",
                        createdAtMs = now
                    )
                )
                OfflineSyncWorker.enqueueSync(appContext, expedited = expedited)
            }
        }
    }

    private suspend fun uploadMessageMediaIfNeeded(
        messageId: String,
        type: MessageType,
        mediaLocalPath: String?
    ): String? {
        if (mediaLocalPath.isNullOrBlank() || !syncRepo.isSupabaseConfigured()) return null
        return when (type) {
            MessageType.VOICE_AAC -> syncRepo.uploadVoiceClip(messageId, mediaLocalPath)
            MessageType.PHOTO -> syncRepo.uploadChatPhoto(appContext, messageId, mediaLocalPath)
            MessageType.VIDEO_MP4 -> syncRepo.uploadChatVideo(appContext, messageId, mediaLocalPath)
            else -> null
        }
    }

    fun reactToMessage(messageId: String, emoji: String) {
        val self = _session.value.deviceOwner ?: _session.value.activeMember ?: FamilyMemberId.MITHUN
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateMessageReaction(messageId, emoji)
            if (checkCurrentConnectivity()) {
                syncRepo.updateRemoteMessageReaction(messageId, emoji)
                realtimeSignaling.broadcastMessageReaction(self, messageId, emoji)
            }
        }
    }

    private suspend fun flushPendingOfflineMessages() {
        val pending = dao.getPendingSyncItems()
        if (pending.isEmpty()) {
            dao.markPendingMessagesDelivered()
            return
        }
        pending.forEach { item ->
            if (item.entityType == "MESSAGE") {
                val localMsg = dao.getMessageById(item.entityId)
                if (localMsg != null && localMsg.deliveryStatus == DeliveryStatus.QUEUED_OFFLINE.name) {
                    val msgSender = runCatching { FamilyMemberId.valueOf(localMsg.senderId) }
                        .getOrDefault(_session.value.deviceOwner ?: FamilyMemberId.MITHUN)
                    val msgType = runCatching { MessageType.valueOf(localMsg.messageType) }
                        .getOrDefault(MessageType.TEXT)
                    val remoteUrl = localMsg.mediaRemoteUrl
                        ?: uploadMessageMediaIfNeeded(localMsg.id, msgType, localMsg.mediaLocalPath)
                    if (!remoteUrl.isNullOrBlank() && remoteUrl != localMsg.mediaRemoteUrl) {
                        dao.upsertMessage(localMsg.copy(mediaRemoteUrl = remoteUrl))
                    }
                    syncRepo.upsertFamilyMessage(
                        SupabaseFamilyMessageDto(
                            id = localMsg.id,
                            thread_id = localMsg.threadId,
                            sender_id = localMsg.senderId,
                            recipient_ids_csv = localMsg.recipientIdsCsv,
                            body = localMsg.decryptedPreview,
                            message_type = localMsg.messageType,
                            media_url = remoteUrl,
                            reaction_emoji = localMsg.reactionEmoji,
                            delivery_status = DeliveryStatus.DELIVERED.name,
                            timestamp_ms = localMsg.timestampMs
                        )
                    )
                    realtimeSignaling.broadcastChatMessage(
                        messageId = localMsg.id,
                        sender = msgSender,
                        threadId = localMsg.threadId,
                        recipientsCsv = localMsg.recipientIdsCsv,
                        text = localMsg.decryptedPreview,
                        messageType = localMsg.messageType,
                        timestampMs = localMsg.timestampMs,
                        mediaUrl = remoteUrl.orEmpty()
                    )
                }
                dao.updateMessageStatus(item.entityId, DeliveryStatus.DELIVERED.name)
            }
            dao.updateSyncItemStatus(item.id, "SYNCED", item.retryCount, null)
        }
        dao.markPendingMessagesDelivered()
        dao.clearAllSyncItems()
        _lastSyncedLabel.value = "Up to date"
    }

    fun triggerSyncNow(expedited: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            val onlineNow = checkCurrentConnectivity()
            _isOnline.value = onlineNow
            if (!onlineNow) return@launch

            _isSyncingNow.value = true
            realtimeSignaling.connectIfConfigured()
            flushPendingOfflineMessages()
            hydrateFromSupabase()
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
            val id = "${kind.lowercase()}_${UUID.randomUUID()}"
            val now = System.currentTimeMillis()
            dao.upsertActivity(
                FamilyActivityEntity(
                    id = id,
                    kind = kind,
                    title = title,
                    subtitle = subtitle,
                    categoryOrTag = tag,
                    authorId = author.name,
                    votesCount = 1,
                    costEstimate = costOrDuration,
                    targetDateLabel = dateLabel,
                    isCompleted = false,
                    timestampMs = now
                )
            )
            if (kind == "PARENT_LOG" && checkCurrentConnectivity()) {
                syncRepo.upsertParentLog(
                    SupabaseParentLogDto(
                        id = id,
                        author_id = author.name,
                        title = title,
                        subtitle = subtitle,
                        cost_estimate = costOrDuration,
                        target_date_label = dateLabel ?: "Adults Only",
                        created_at_ms = now
                    )
                )
                realtimeSignaling.broadcastParentLog(
                    author = author,
                    logId = id,
                    title = title,
                    subtitle = subtitle,
                    costEstimate = costOrDuration,
                    timestampMs = now
                )
            }
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

    // --- Shared Daily Family Comic Photo Timeline ---

    fun getTodayDateKey(): String = DailyTurnSchedule.todayDateKey()

    fun getTodayTurnMember(): FamilyMemberId = DailyTurnSchedule.turnMemberForDay()

    fun createAndPostDailyComic(
        sourceUri: Uri,
        caption: String,
        style: ComicPhotoStyle,
        author: FamilyMemberId
    ) {
        val dateKey = DailyTurnSchedule.todayDateKey()
        if (dailyComicPhotos.value.any { it.dateKey == dateKey }) {
            _uiFeedback.tryEmit("Today's family comic photo is already posted!")
            return
        }
        viewModelScope.launch {
            _dailyComicGenState.value = DailyComicGenerationState.Loading(style.label)
            val result = dailyComicRepo.stylizePhotoToComic(
                sourceUri = sourceUri,
                style = style,
                dateKey = dateKey,
                author = author
            )
            result.onSuccess { comic ->
                val photoId = "comic_${UUID.randomUUID()}"
                val now = System.currentTimeMillis()
                dao.upsertDailyComicPhoto(
                    DailyComicPhotoEntity(
                        dateKey = dateKey,
                        id = photoId,
                        authorId = author.name,
                        caption = caption,
                        comicStyle = style.id,
                        localImagePath = comic.localImagePath,
                        reactionEmoji = null,
                        createdAtMs = now
                    )
                )
                realtimeSignaling.broadcastDailyComicPhoto(
                    photoId = photoId,
                    dateKey = dateKey,
                    author = author,
                    caption = caption,
                    comicStyle = style.id,
                    imageBase64 = comic.shareBase64,
                    timestampMs = now
                )
                _dailyComicGenState.value = DailyComicGenerationState.Idle
                _uiFeedback.tryEmit("Today's comic photo is live on our family timeline!")
            }.onFailure { err ->
                _dailyComicGenState.value = DailyComicGenerationState.Error(
                    err.localizedMessage ?: "Couldn't create your comic photo. Please try again."
                )
            }
        }
    }

    fun reactToDailyComic(dateKey: String, emoji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateDailyComicReaction(dateKey, emoji)
        }
    }

    fun clearDailyComicError() {
        if (_dailyComicGenState.value is DailyComicGenerationState.Error) {
            _dailyComicGenState.value = DailyComicGenerationState.Idle
        }
    }

    // --- Premium 2-of-3 Family XOXO Game Engine (Live 2-Phone + Pass & Play) ---

    private fun defaultXoxoStateFor(owner: FamilyMemberId?): XoxoGameState {
        val px = owner ?: FamilyMemberId.MITHUN
        val po = when (px) {
            FamilyMemberId.MITHUN -> FamilyMemberId.ZAYN
            FamilyMemberId.MAFITHA -> FamilyMemberId.ZAYN
            FamilyMemberId.ZAYN -> FamilyMemberId.MITHUN
        }
        return XoxoGameState(playerX = px, playerO = po)
    }

    fun selectXoxoPlayers(playerX: FamilyMemberId, playerO: FamilyMemberId) {
        if (playerX == playerO) return
        val prev = _xoxoState.value
        val samePair = setOf(prev.playerX, prev.playerO) == setOf(playerX, playerO)
        val updated = prev.copy(
            board = List(9) { "" },
            playerX = playerX,
            playerO = playerO,
            currentTurn = XoxoMark.X,
            startingMark = XoxoMark.X,
            winnerMark = null,
            winningLine = emptyList(),
            isDraw = false,
            xScore = if (samePair) prev.xScore else 0,
            oScore = if (samePair) prev.oScore else 0,
            drawScore = if (samePair) prev.drawScore else 0,
            roundNumber = if (samePair) prev.roundNumber else 1,
            lastUpdatedMs = System.currentTimeMillis()
        )
        _xoxoState.value = updated
        broadcastXoxoIfLive(updated)
    }

    fun swapXoxoSides() {
        val prev = _xoxoState.value
        val updated = prev.copy(
            board = List(9) { "" },
            playerX = prev.playerO,
            playerO = prev.playerX,
            currentTurn = XoxoMark.X,
            startingMark = XoxoMark.X,
            winnerMark = null,
            winningLine = emptyList(),
            isDraw = false,
            xScore = prev.oScore,
            oScore = prev.xScore,
            lastUpdatedMs = System.currentTimeMillis()
        )
        _xoxoState.value = updated
        broadcastXoxoIfLive(updated)
    }

    fun setXoxoPlayMode(mode: XoxoPlayMode) {
        val updated = _xoxoState.value.copy(
            playMode = mode,
            lastUpdatedMs = System.currentTimeMillis()
        )
        _xoxoState.value = updated
        if (mode == XoxoPlayMode.LIVE_TWO_PHONES) {
            broadcastXoxoIfLive(updated)
        }
    }

    fun makeXoxoMove(cellIndex: Int) {
        if (cellIndex !in 0..8) return
        val state = _xoxoState.value
        if (state.isGameOver || state.board[cellIndex].isNotEmpty()) return

        val self = _session.value.deviceOwner ?: _session.value.activeMember ?: FamilyMemberId.MITHUN
        if (state.playMode == XoxoPlayMode.LIVE_TWO_PHONES && state.currentTurnMember != self) {
            val isParticipant = self == state.playerX || self == state.playerO
            if (isParticipant) {
                _uiFeedback.tryEmit("Waiting for ${state.currentTurnMember.displayName}'s turn!")
            } else {
                _uiFeedback.tryEmit("You're watching live — tap your name above to jump in!")
            }
            return
        }

        val nextBoard = state.board.toMutableList()
        nextBoard[cellIndex] = state.currentTurn.name

        val winLine = findWinningLine(nextBoard, state.currentTurn.name)
        val winner = if (winLine.isNotEmpty()) state.currentTurn else null
        val isDraw = winner == null && nextBoard.none { it.isEmpty() }

        val updated = state.copy(
            board = nextBoard,
            currentTurn = if (winner == null && !isDraw) state.currentTurn.opposite() else state.currentTurn,
            winnerMark = winner,
            winningLine = winLine,
            isDraw = isDraw,
            xScore = state.xScore + if (winner == XoxoMark.X) 1 else 0,
            oScore = state.oScore + if (winner == XoxoMark.O) 1 else 0,
            drawScore = state.drawScore + if (isDraw) 1 else 0,
            lastUpdatedMs = System.currentTimeMillis()
        )
        _xoxoState.value = updated
        broadcastXoxoIfLive(updated)
    }

    fun startNextXoxoRound() {
        val state = _xoxoState.value
        val nextStarter = state.startingMark.opposite()
        val updated = state.copy(
            board = List(9) { "" },
            currentTurn = nextStarter,
            startingMark = nextStarter,
            winnerMark = null,
            winningLine = emptyList(),
            isDraw = false,
            roundNumber = state.roundNumber + 1,
            lastUpdatedMs = System.currentTimeMillis()
        )
        _xoxoState.value = updated
        broadcastXoxoIfLive(updated)
    }

    fun resetXoxoMatchScores() {
        val state = _xoxoState.value
        val updated = state.copy(
            board = List(9) { "" },
            currentTurn = XoxoMark.X,
            startingMark = XoxoMark.X,
            winnerMark = null,
            winningLine = emptyList(),
            isDraw = false,
            xScore = 0,
            oScore = 0,
            drawScore = 0,
            roundNumber = 1,
            lastUpdatedMs = System.currentTimeMillis()
        )
        _xoxoState.value = updated
        broadcastXoxoIfLive(updated)
    }

    private fun findWinningLine(board: List<String>, mark: String): List<Int> {
        val lines = listOf(
            listOf(0, 1, 2),
            listOf(3, 4, 5),
            listOf(6, 7, 8),
            listOf(0, 3, 6),
            listOf(1, 4, 7),
            listOf(2, 5, 8),
            listOf(0, 4, 8),
            listOf(2, 4, 6)
        )
        return lines.firstOrNull { line -> line.all { board[it] == mark } } ?: emptyList()
    }

    private fun broadcastXoxoIfLive(state: XoxoGameState) {
        viewModelScope.launch(Dispatchers.IO) {
            if (checkCurrentConnectivity()) {
                syncRepo.upsertXoxoGame(state)
            }
        }
        if (state.playMode != XoxoPlayMode.LIVE_TWO_PHONES) return
        val sender = _session.value.deviceOwner ?: _session.value.activeMember ?: FamilyMemberId.MITHUN
        val encoded = runCatching { json.encodeToString(state) }.getOrNull() ?: return
        realtimeSignaling.broadcastXoxoState(sender = sender, stateJson = encoded)
    }

    override fun onCleared() {
        super.onCleared()
        presenceLoopJob?.cancel()
        realtimeSignaling.disconnectGracefully(offlineMember = _session.value.deviceOwner)
        liveKitEngine.disconnect()
        voiceCapture.release()
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback?.let { runCatching { cm.unregisterNetworkCallback(it) } }
    }

    companion object {
        private const val PREFS_DEVICE_BINDING = "holymoly_device_binding"
        private const val KEY_ASSIGNED_MEMBER = "assigned_member_id"
        private const val PRESENCE_TIMEOUT_MS = 30_000L
    }
}
