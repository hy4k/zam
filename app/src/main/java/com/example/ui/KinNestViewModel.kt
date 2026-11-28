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
import com.example.data.model.PresenceAvailability
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
import com.example.sync.FamilyLoginResult
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

data class HeartbeatBannerAlert(
    val sender: FamilyMemberId,
    val message: String,
    val isSafetyAlert: Boolean,
    val timestampMs: Long
)

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
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

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

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

    private val _manualPresenceChoice = MutableStateFlow(PresenceAvailability.ONLINE)
    val manualPresenceChoice: StateFlow<PresenceAvailability> = _manualPresenceChoice.asStateFlow()

    private val _memberPresence = MutableStateFlow(
        FamilyMemberId.entries.associateWith { member ->
            MemberPresenceStatus(
                memberId = member,
                availability = if (member == initialDeviceOwner && _isOnline.value) {
                    PresenceAvailability.ONLINE
                } else {
                    PresenceAvailability.OFFLINE
                },
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
    private val lastUserInteractionMs = AtomicLong(System.currentTimeMillis())
    private var callTimerJob: Job? = null
    private var presenceLoopJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                // Ensure initial family profiles & starter activities exist on first launch
                if (dao.countProfiles() == 0) {
                    KinNestDatabase.ensureSeeded(appContext, dao)
                }
                if (checkCurrentConnectivity()) {
                    flushPendingOfflineMessages()
                    hydrateFromSupabase()
                }
            }
        }
        registerConnectivityMonitor()
        observeLiveKitDataChannel()
        observeLiveKitRoomState()
        observeRealtimePresence()
        observeRealtimeSignaling()
        if (_isOnline.value) {
            val (avail, note) = computeEffectiveSelfPresence()
            realtimeSignaling.connectIfConfigured(
                member = initialDeviceOwner,
                availability = avail,
                statusNote = note
            )
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

        // 4. Hydrate Family Online / Last-Seen Presence (do not overwrite fresher Realtime Presence Away/Busy!)
        val remotePresence = syncRepo.fetchAllPresence()
        if (remotePresence.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val owner = _session.value.deviceOwner
            val realtimeLive = realtimeSignaling.realtimePresenceMap.value
            _memberPresence.update { current ->
                val mutable = current.toMutableMap()
                remotePresence.forEach { dto ->
                    val member = runCatching { FamilyMemberId.valueOf(dto.member_id) }.getOrNull() ?: return@forEach
                    if (member != owner) {
                        val rtEntry = realtimeLive[member]
                        if (rtEntry != null) {
                            mutable[member] = MemberPresenceStatus(
                                memberId = member,
                                availability = rtEntry.availability,
                                statusNote = rtEntry.statusNote,
                                lastSeenMs = maxOf(rtEntry.updatedAtMs, dto.last_seen_ms)
                            )
                        } else {
                            val freshOnline = dto.is_online && dto.last_seen_ms > 0L && (now - dto.last_seen_ms) <= PRESENCE_TIMEOUT_MS
                            val existing = mutable[member]
                            val bestLastSeen = maxOf(dto.last_seen_ms, existing?.lastSeenMs ?: 0L).takeIf { it > 0L }
                            val keepExistingAvailability = existing != null && existing.isOnline &&
                                existing.lastSeenMs != null && (now - existing.lastSeenMs) <= PRESENCE_TIMEOUT_MS
                            val resolvedAvailability = when {
                                keepExistingAvailability -> existing!!.availability
                                freshOnline -> PresenceAvailability.ONLINE
                                else -> PresenceAvailability.OFFLINE
                            }
                            mutable[member] = MemberPresenceStatus(
                                memberId = member,
                                availability = resolvedAvailability,
                                statusNote = if (keepExistingAvailability) existing?.statusNote else null,
                                lastSeenMs = bestLastSeen
                            )
                        }
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

    private fun checkCurrentConnectivity(): Boolean = runCatching {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return@runCatching false
        val network = cm.activeNetwork ?: return@runCatching false
        val caps = cm.getNetworkCapabilities(network) ?: return@runCatching false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            (
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                )
    }.getOrDefault(false)

    private fun registerConnectivityMonitor() {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                publishSelfPresenceNow(isHello = true)
                if (isAppInForeground || isCallInProgress()) {
                    val (avail, note) = computeEffectiveSelfPresence()
                    realtimeSignaling.connectIfConfigured(
                        member = _session.value.deviceOwner,
                        availability = avail,
                        statusNote = note
                    )
                }
                triggerSyncNow(expedited = false)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val currentlyOnline = hasInternet || checkCurrentConnectivity()
                if (_isOnline.value != currentlyOnline) {
                    _isOnline.value = currentlyOnline
                    publishSelfPresenceNow(isHello = currentlyOnline)
                    if (currentlyOnline && (isAppInForeground || isCallInProgress())) {
                        val (avail, note) = computeEffectiveSelfPresence()
                        realtimeSignaling.connectIfConfigured(
                            member = _session.value.deviceOwner,
                            availability = avail,
                            statusNote = note
                        )
                        triggerSyncNow(expedited = false)
                    }
                }
            }

            override fun onLost(network: Network) {
                // Re-verify active default network so switching Wi-Fi <-> Cellular never shows a false offline banner
                val stillConnected = checkCurrentConnectivity()
                _isOnline.value = stillConnected
                if (!stillConnected) {
                    updateSelfPresence(PresenceAvailability.OFFLINE, null)
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
            liveKitEngine.isReconnecting.collect { reconnecting ->
                _callState.update { state ->
                    when {
                        reconnecting && state.phase is CallPhase.Active ->
                            state.copy(phase = CallPhase.Reconnecting("Reconnecting call…"))
                        !reconnecting && state.phase is CallPhase.Reconnecting ->
                            state.copy(phase = CallPhase.Active)
                        else -> state
                    }
                }
            }
        }
        viewModelScope.launch {
            combine(
                liveKitEngine.connectedLiveMembers,
                liveKitEngine.speakingMembers,
                liveKitEngine.videoTracksByMember,
                liveKitEngine.mutedMembers,
                liveKitEngine.isLiveStreamActive
            ) { connected, speaking, tracks, muted, liveActive ->
                LiveKitRoomSnapshot(connected, speaking, tracks, muted, liveActive)
            }.collect { snap ->
                if (!snap.liveActive) return@collect
                val self = _session.value.activeMember ?: FamilyMemberId.MITHUN
                _callState.update { state ->
                    if (state.phase !is CallPhase.Active &&
                        state.phase !is CallPhase.DialingOutgoing &&
                        state.phase !is CallPhase.Reconnecting
                    ) {
                        return@update state
                    }
                    val existingMap = state.participants.associateBy { it.memberId }.toMutableMap()
                    // Update any already-listed participants (including disconnects!)
                    existingMap.keys.toList().forEach { memberId ->
                        val prev = existingMap[memberId] ?: return@forEach
                        val isConn = memberId in snap.connected || memberId == self
                        val isSpk = memberId in snap.speaking
                        val isMutedNow = if (memberId == self) state.isLocalMicMuted else (memberId in snap.muted)
                        val hasCam = if (memberId == self) state.isLocalCameraOn else snap.tracks.containsKey(memberId)
                        existingMap[memberId] = prev.copy(
                            isConnected = isConn,
                            isSpeaking = isSpk && !isMutedNow,
                            audioLevel = if (isMutedNow || !isConn) 0f else if (isSpk) 0.85f else 0.14f,
                            isMuted = isMutedNow,
                            isCameraEnabled = hasCam
                        )
                    }
                    // Add any newly joined remote family members
                    snap.connected.forEach { memberId ->
                        if (!existingMap.containsKey(memberId)) {
                            val isSpk = memberId in snap.speaking
                            val isMutedNow = memberId in snap.muted
                            existingMap[memberId] = CallParticipantState(
                                memberId = memberId,
                                isConnected = true,
                                isSpeaking = isSpk && !isMutedNow,
                                audioLevel = if (isMutedNow) 0f else if (isSpk) 0.85f else 0.14f,
                                isMuted = isMutedNow,
                                isCameraEnabled = snap.tracks.containsKey(memberId)
                            )
                        }
                    }
                    val ordered = existingMap.values.sortedBy { p ->
                        when {
                            p.memberId == self -> 0
                            p.isConnected -> 1
                            else -> 2
                        }
                    }
                    state.copy(participants = ordered)
                }
            }
        }
    }

    private fun observeRealtimePresence() {
        viewModelScope.launch {
            realtimeSignaling.realtimePresenceMap.collect { presenceMap ->
                val owner = _session.value.deviceOwner
                val now = System.currentTimeMillis()
                _memberPresence.update { current ->
                    FamilyMemberId.entries.associateWith { member ->
                        if (member == owner) {
                            current[member] ?: MemberPresenceStatus(member)
                        } else {
                            val rt = presenceMap[member]
                            if (rt != null) {
                                MemberPresenceStatus(
                                    memberId = member,
                                    availability = rt.availability,
                                    statusNote = rt.statusNote,
                                    lastSeenMs = maxOf(rt.updatedAtMs, now)
                                )
                            } else {
                                // Only mark offline if no fresh broadcast ping was heard very recently
                                val prev = current[member] ?: MemberPresenceStatus(member)
                                val heardRecently = prev.lastSeenMs != null && (now - prev.lastSeenMs) < PRESENCE_TIMEOUT_MS
                                if (heardRecently && prev.isOnline) {
                                    prev
                                } else {
                                    prev.copy(
                                        availability = PresenceAvailability.OFFLINE,
                                        statusNote = null
                                    )
                                }
                            }
                        }
                    }
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
                // Any live non-offline signal from another family member updates their live presence
                if (signal.kind != SupabaseRealtimeSignaling.KIND_PRESENCE_OFFLINE) {
                    val parsedAvail = signal.presenceStatus.takeIf { it.isNotBlank() }?.let {
                        runCatching { PresenceAvailability.valueOf(it) }.getOrNull()
                    }
                    val currentExisting = _memberPresence.value[sender]
                    val resolvedAvail = parsedAvail
                        ?: currentExisting?.availability?.takeIf { it != PresenceAvailability.OFFLINE }
                        ?: PresenceAvailability.ONLINE
                    val resolvedNote = signal.statusNote.takeIf { it.isNotBlank() }
                        ?: if (parsedAvail == null) currentExisting?.statusNote else null
                    updateRemoteMemberPresence(sender, resolvedAvail, resolvedNote, nowMs)
                }

                when (signal.kind) {
                    SupabaseRealtimeSignaling.KIND_PRESENCE_PING -> {
                        if (signal.text == "HELLO" && self != null && (isAppInForeground || isCallInProgress()) && _isOnline.value) {
                            val (avail, note) = computeEffectiveSelfPresence()
                            realtimeSignaling.trackPresence(
                                member = self,
                                availability = avail,
                                statusNote = note,
                                isHello = false
                            )
                        }
                    }
                    SupabaseRealtimeSignaling.KIND_PRESENCE_OFFLINE -> {
                        updateRemoteMemberPresence(
                            member = sender,
                            availability = PresenceAvailability.OFFLINE,
                            statusNote = null,
                            timestampMs = signal.timestampMs.takeIf { it > 0L } ?: nowMs
                        )
                    }
                    SupabaseRealtimeSignaling.KIND_CALL_INVITE -> {
                        val targets = signal.targetIdsCsv.split(",").map { it.trim() }
                        val isForMe = self == null || targets.contains(self.name)
                        if (isForMe && _callState.value.phase is CallPhase.Idle) {
                            telecomManager.postLockScreenIncomingCallNotification(
                                caller = sender,
                                isThreeWay = signal.isThreeWay,
                                videoEnabled = signal.videoEnabled,
                                quietHoursActive = _session.value.quietHoursEnabled
                            )
                            _callState.update {
                                it.copy(
                                    roomName = signal.roomName.ifBlank { LiveKitCallEngine.DEFAULT_FAMILY_ROOM },
                                    isVoiceCallMode = !signal.videoEnabled,
                                    isLocalCameraOn = signal.videoEnabled,
                                    phase = CallPhase.RingingIncoming(
                                        caller = sender,
                                        isThreeWay = signal.isThreeWay,
                                        roomName = signal.roomName.ifBlank { LiveKitCallEngine.DEFAULT_FAMILY_ROOM },
                                        videoEnabled = signal.videoEnabled
                                    )
                                )
                            }
                        }
                    }
                    SupabaseRealtimeSignaling.KIND_CALL_DECLINE -> {
                        val currentPhase = _callState.value.phase
                        val sameRoom = signal.roomName.isBlank() || signal.roomName == _callState.value.roomName
                        if (sameRoom && (currentPhase is CallPhase.DialingOutgoing || currentPhase is CallPhase.Active)) {
                            _uiFeedback.tryEmit("${sender.displayName} couldn't answer right now")
                            val otherRemoteConnected = liveKitEngine.connectedLiveMembers.value
                                .any { it != self && it != sender }
                            val isSoloCallToDecliner = _callState.value.participants.size <= 2
                            if (!otherRemoteConnected && isSoloCallToDecliner) {
                                endCallInternal("${sender.displayName} declined the call", notifyRemote = false)
                            }
                        }
                    }
                    SupabaseRealtimeSignaling.KIND_CALL_END -> {
                        val ringing = _callState.value.phase as? CallPhase.RingingIncoming
                        if (ringing != null && ringing.caller == sender) {
                            telecomManager.cancelIncomingCallNotification()
                            _callState.update { it.copy(phase = CallPhase.Idle) }
                            _uiFeedback.tryEmit("${sender.displayName} cancelled the call")
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

    private fun isCallInProgress(): Boolean {
        val p = _callState.value.phase
        return p !is CallPhase.Idle && p !is CallPhase.Ended
    }

    private fun computeEffectiveSelfPresence(): Pair<PresenceAvailability, String?> {
        if (!_isOnline.value) return PresenceAvailability.OFFLINE to null
        if (isCallInProgress()) {
            val label = if (_callState.value.isVoiceCallMode) "Voice call" else "Video call"
            return PresenceAvailability.BUSY to label
        }
        if (_isWalkieRecording.value) {
            return PresenceAvailability.BUSY to "On radio"
        }
        if (!isAppInForeground) {
            return PresenceAvailability.AWAY to "Away"
        }
        val manual = _manualPresenceChoice.value
        if (manual == PresenceAvailability.BUSY) return PresenceAvailability.BUSY to "Busy"
        if (manual == PresenceAvailability.AWAY) return PresenceAvailability.AWAY to "Away"
        val idleMs = System.currentTimeMillis() - lastUserInteractionMs.get()
        if (idleMs >= IDLE_AWAY_TIMEOUT_MS) {
            return PresenceAvailability.AWAY to "Away"
        }
        return PresenceAvailability.ONLINE to null
    }

    fun setMyPresenceAvailability(availability: PresenceAvailability) {
        if (availability == PresenceAvailability.OFFLINE) return
        lastUserInteractionMs.set(System.currentTimeMillis())
        _manualPresenceChoice.value = availability
        publishSelfPresenceNow(isHello = false)
        _uiFeedback.tryEmit("Status set to ${availability.label}")
    }

    fun cycleMyPresenceAvailability() {
        val next = when (_manualPresenceChoice.value) {
            PresenceAvailability.ONLINE -> PresenceAvailability.AWAY
            PresenceAvailability.AWAY -> PresenceAvailability.BUSY
            else -> PresenceAvailability.ONLINE
        }
        setMyPresenceAvailability(next)
    }

    fun recordUserInteraction() {
        val wasAutoAway = _manualPresenceChoice.value == PresenceAvailability.ONLINE &&
            (System.currentTimeMillis() - lastUserInteractionMs.get()) >= IDLE_AWAY_TIMEOUT_MS
        lastUserInteractionMs.set(System.currentTimeMillis())
        if (wasAutoAway) {
            publishSelfPresenceNow(isHello = false)
        }
    }

    private fun publishSelfPresenceNow(isHello: Boolean = false) {
        val owner = _session.value.deviceOwner ?: return
        val (avail, note) = computeEffectiveSelfPresence()
        updateSelfPresence(avail, note)
        if (_isOnline.value && (isAppInForeground || isCallInProgress())) {
            realtimeSignaling.trackPresence(
                member = owner,
                availability = avail,
                statusNote = note,
                isHello = isHello
            )
            val now = System.currentTimeMillis()
            viewModelScope.launch(Dispatchers.IO) {
                syncRepo.upsertPresence(owner, isOnline = avail != PresenceAvailability.OFFLINE, nowMs = now)
            }
        }
    }

    private fun startPresenceLoop() {
        presenceLoopJob?.cancel()
        presenceLoopJob = viewModelScope.launch {
            var firstPing = true
            while (true) {
                runCatching {
                    val onlineNow = checkCurrentConnectivity()
                    if (_isOnline.value != onlineNow) {
                        _isOnline.value = onlineNow
                    }
                    val owner = _session.value.deviceOwner
                    val now = System.currentTimeMillis()
                    val (selfAvail, selfNote) = computeEffectiveSelfPresence()
                    if (owner != null && (isAppInForeground || isCallInProgress()) && onlineNow) {
                        updateSelfPresence(selfAvail, selfNote)
                        realtimeSignaling.trackPresence(
                            member = owner,
                            availability = selfAvail,
                            statusNote = selfNote,
                            isHello = firstPing
                        )
                        viewModelScope.launch(Dispatchers.IO) {
                            runCatching {
                                syncRepo.upsertPresence(owner, isOnline = true, nowMs = now)
                            }
                        }
                        firstPing = false
                    }
                    // Keep Realtime Presence authoritative; expire stale broadcast-only pings after 35s
                    val rtLive = realtimeSignaling.realtimePresenceMap.value
                    _memberPresence.update { current ->
                        current.mapValues { (member, status) ->
                            if (member == owner) {
                                status.copy(
                                    availability = if ((isAppInForeground || isCallInProgress()) && onlineNow) {
                                        selfAvail
                                    } else if (onlineNow) {
                                        PresenceAvailability.AWAY
                                    } else {
                                        PresenceAvailability.OFFLINE
                                    },
                                    statusNote = selfNote,
                                    lastSeenMs = if (onlineNow) now else status.lastSeenMs
                                )
                            } else if (rtLive.containsKey(member)) {
                                val entry = rtLive[member]!!
                                status.copy(
                                    availability = entry.availability,
                                    statusNote = entry.statusNote,
                                    lastSeenMs = now
                                )
                            } else if (status.isOnline && status.lastSeenMs != null && (now - status.lastSeenMs) > PRESENCE_TIMEOUT_MS) {
                                status.copy(availability = PresenceAvailability.OFFLINE, statusNote = null)
                            } else {
                                status
                            }
                        }
                    }
                }
                delay(12_000L)
            }
        }
    }

    private fun updateSelfPresence(availability: PresenceAvailability, note: String?) {
        val owner = _session.value.deviceOwner ?: return
        val now = System.currentTimeMillis()
        _memberPresence.update { current ->
            val prev = current[owner] ?: MemberPresenceStatus(owner)
            current + (owner to prev.copy(
                availability = availability,
                statusNote = note,
                lastSeenMs = if (availability != PresenceAvailability.OFFLINE) now else (prev.lastSeenMs ?: now)
            ))
        }
    }

    private fun updateRemoteMemberPresence(
        member: FamilyMemberId,
        availability: PresenceAvailability,
        statusNote: String?,
        timestampMs: Long
    ) {
        _memberPresence.update { current ->
            current + (member to MemberPresenceStatus(
                memberId = member,
                availability = availability,
                statusNote = statusNote,
                lastSeenMs = timestampMs
            ))
        }
    }

    fun onAppForegrounded() {
        isAppInForeground = true
        lastUserInteractionMs.set(System.currentTimeMillis())
        val onlineNow = checkCurrentConnectivity()
        _isOnline.value = onlineNow
        val owner = _session.value.deviceOwner
        val (avail, note) = computeEffectiveSelfPresence()
        if (owner != null) {
            updateSelfPresence(avail, note)
        }
        if (onlineNow) {
            realtimeSignaling.connectIfConfigured(
                member = owner,
                availability = avail,
                statusNote = note
            )
            if (owner != null) {
                realtimeSignaling.trackPresence(owner, avail, note, isHello = true)
            }
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    if (owner != null) {
                        syncRepo.upsertPresence(owner, isOnline = true, nowMs = System.currentTimeMillis())
                    }
                    flushPendingOfflineMessages()
                    hydrateFromSupabase()
                }
            }
        }
    }

    fun onAppBackgrounded() {
        isAppInForeground = false
        val owner = _session.value.deviceOwner
        // Keep Realtime & Busy presence active if the family is currently in a live video/voice call
        if (isCallInProgress()) {
            publishSelfPresenceNow(isHello = false)
            return
        }
        if (owner != null) {
            updateSelfPresence(PresenceAvailability.AWAY, "Away")
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    syncRepo.upsertPresence(owner, isOnline = false, nowMs = System.currentTimeMillis())
                }
            }
        }
        // Cleanly broadcast Away and close Realtime WebSocket before Android suspends idle sockets
        realtimeSignaling.disconnectGracefully(
            offlineMember = owner,
            finalAvailability = PresenceAvailability.AWAY
        )
    }

    // --- Supabase Username & Password Authentication ---

    fun clearLoginError() {
        if (_loginUiState.value.errorMessage != null) {
            _loginUiState.update { it.copy(errorMessage = null) }
        }
    }

    fun loginWithCredentials(
        username: String,
        password: String,
        onLoginSuccess: () -> Unit
    ) {
        val cleanUser = username.trim()
        if (cleanUser.isEmpty()) {
            _loginUiState.value = LoginUiState(isLoading = false, errorMessage = "Please enter your username.")
            return
        }
        if (password.isEmpty()) {
            _loginUiState.value = LoginUiState(isLoading = false, errorMessage = "Please enter your password.")
            return
        }
        if (!checkCurrentConnectivity()) {
            _loginUiState.value = LoginUiState(
                isLoading = false,
                errorMessage = "No internet connection. Connect to sign in."
            )
            return
        }
        if (_loginUiState.value.isLoading) return

        _loginUiState.value = LoginUiState(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = syncRepo.verifyFamilyLogin(cleanUser, password)) {
                is FamilyLoginResult.Success -> {
                    assignThisDeviceToMember(result.member)
                    _loginUiState.value = LoginUiState(isLoading = false, errorMessage = null)
                    withContext(Dispatchers.Main) {
                        onLoginSuccess()
                    }
                }
                is FamilyLoginResult.Failure -> {
                    _loginUiState.value = LoginUiState(
                        isLoading = false,
                        errorMessage = result.reason
                    )
                }
            }
        }
    }

    fun signOut() {
        val previousOwner = _session.value.deviceOwner
        val now = System.currentTimeMillis()
        if (previousOwner != null) {
            realtimeSignaling.broadcastPresenceOffline(previousOwner)
            viewModelScope.launch(Dispatchers.IO) {
                syncRepo.upsertPresence(previousOwner, isOnline = false, nowMs = now)
            }
        }
        devicePrefs.edit().remove(KEY_ASSIGNED_MEMBER).apply()
        _session.update {
            it.copy(
                deviceOwner = null,
                activeMember = null,
                isAuthenticated = false,
                isParentalUnlocked = false
            )
        }
        _memberPresence.update { current ->
            if (previousOwner != null) {
                current + (previousOwner to MemberPresenceStatus(
                    memberId = previousOwner,
                    availability = PresenceAvailability.OFFLINE,
                    lastSeenMs = now
                ))
            } else {
                current
            }
        }
        _manualPresenceChoice.value = PresenceAvailability.ONLINE
        _loginUiState.value = LoginUiState()
        _uiFeedback.tryEmit("Signed out of Holy Moly !")
    }

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
        _manualPresenceChoice.value = PresenceAvailability.ONLINE
        lastUserInteractionMs.set(System.currentTimeMillis())
        val now = System.currentTimeMillis()
        _memberPresence.update { current ->
            current.mapValues { (id, status) ->
                when (id) {
                    member -> MemberPresenceStatus(
                        memberId = id,
                        availability = if (_isOnline.value) PresenceAvailability.ONLINE else PresenceAvailability.OFFLINE,
                        lastSeenMs = now
                    )
                    previousOwner -> status.copy(availability = PresenceAvailability.OFFLINE, statusNote = null)
                    else -> status
                }
            }
        }
        if (_isOnline.value) {
            realtimeSignaling.connectIfConfigured(
                member = member,
                availability = PresenceAvailability.ONLINE,
                statusNote = null
            )
            realtimeSignaling.trackPresence(
                member = member,
                availability = PresenceAvailability.ONLINE,
                statusNote = null,
                isHello = true
            )
            viewModelScope.launch(Dispatchers.IO) {
                if (previousOwner != null && previousOwner != member) {
                    syncRepo.upsertPresence(previousOwner, isOnline = false, nowMs = now)
                }
                syncRepo.upsertPresence(member, isOnline = true, nowMs = now)
                hydrateFromSupabase()
            }
        }
        _uiFeedback.tryEmit("Welcome back, ${member.displayName}!")
        if (member == FamilyMemberId.ZAYN) {
            speakHelper(
                english = "Hi Zayn Adam! Welcome to Holy Moly.",
                malayalam = "ഹായ് സെയ്ൻ ആദം! സ്വാഗതം."
            )
        }
    }

    fun reassignDeviceOwner(newOwner: FamilyMemberId) {
        assignThisDeviceToMember(newOwner)
    }

    fun signInWithDeviceBoundAvatar(member: FamilyMemberId): Boolean {
        val currentOwner = _session.value.deviceOwner
        if (currentOwner == null) {
            assignThisDeviceToMember(member)
            return true
        }
        if (currentOwner != member) {
            _uiFeedback.tryEmit("Signed in as ${currentOwner.displayName} (${currentOwner.title}).")
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

    fun startFamilyCall(targets: List<FamilyMemberId>, videoEnabled: Boolean = true) {
        recordUserInteraction()
        val self = _session.value.activeMember ?: FamilyMemberId.MITHUN
        val allInRoom = (listOf(self) + targets).distinct()
        val roomName = if (allInRoom.size >= 3) {
            LiveKitCallEngine.DEFAULT_FAMILY_ROOM
        } else {
            "holymoly-1on1-${allInRoom.map { it.name.lowercase() }.sorted().joinToString("-")}"
        }
        val participantStates = allInRoom.map { member ->
            CallParticipantState(
                memberId = member,
                isConnected = member == self,
                isSpeaking = false,
                audioLevel = if (member == self) 0.25f else 0f,
                isMuted = false,
                isCameraEnabled = member == self && videoEnabled,
                activeFilter = if (member == self) _callState.value.activeFilter else ArFilterType.NONE
            )
        }

        _callState.update {
            it.copy(
                roomName = roomName,
                phase = CallPhase.DialingOutgoing(targets, videoEnabled = videoEnabled),
                participants = participantStates,
                isLocalMicMuted = false,
                isLocalCameraOn = videoEnabled,
                isVoiceCallMode = !videoEnabled,
                isFrontCamera = true,
                durationSeconds = 0L,
                showLiveDrawingOverlay = false,
                networkQualityLabel = if (videoEnabled) "Connecting video call…" else "Connecting voice call…"
            )
        }
        // Immediately show Busy ("Video call" / "Voice call") on Supabase Realtime Presence
        publishSelfPresenceNow(isHello = false)

        // Ring other family devices immediately over Supabase Realtime Broadcast
        realtimeSignaling.broadcastCallInvite(
            caller = self,
            targets = targets.filter { it != self },
            roomName = roomName,
            isThreeWay = allInRoom.size >= 3,
            videoEnabled = videoEnabled
        )

        val primaryTarget = targets.firstOrNull { it != self } ?: targets.firstOrNull() ?: FamilyMemberId.MAFITHA
        telecomManager.startManagedFamilyCall(
            callerOrTarget = primaryTarget,
            isIncoming = false,
            isVideo = videoEnabled,
            onAnswered = { connectCallMedia(allInRoom, roomName, videoEnabled) },
            onDisconnected = { endCallInternal("Call ended", notifyRemote = true) }
        )

        viewModelScope.launch {
            delay(350)
            if (_callState.value.phase is CallPhase.DialingOutgoing) {
                connectCallMedia(allInRoom, roomName, videoEnabled)
            }
        }
    }

    fun simulateIncomingCall(
        caller: FamilyMemberId,
        isThreeWay: Boolean,
        videoEnabled: Boolean = true
    ) {
        telecomManager.postLockScreenIncomingCallNotification(
            caller = caller,
            isThreeWay = isThreeWay,
            videoEnabled = videoEnabled,
            quietHoursActive = _session.value.quietHoursEnabled
        )
        _callState.update {
            it.copy(
                roomName = LiveKitCallEngine.DEFAULT_FAMILY_ROOM,
                isVoiceCallMode = !videoEnabled,
                isLocalCameraOn = videoEnabled,
                phase = CallPhase.RingingIncoming(
                    caller = caller,
                    isThreeWay = isThreeWay,
                    roomName = LiveKitCallEngine.DEFAULT_FAMILY_ROOM,
                    videoEnabled = videoEnabled
                )
            )
        }
    }

    fun acceptIncomingCall(joinWithVideo: Boolean? = null) {
        recordUserInteraction()
        val incoming = _callState.value.phase as? CallPhase.RingingIncoming ?: return
        val self = _session.value.activeMember ?: FamilyMemberId.MAFITHA
        val useVideo = joinWithVideo ?: incoming.videoEnabled
        val roomName = incoming.roomName.ifBlank { LiveKitCallEngine.DEFAULT_FAMILY_ROOM }
        val members = if (incoming.isThreeWay) {
            listOf(self, FamilyMemberId.MITHUN, FamilyMemberId.MAFITHA, FamilyMemberId.ZAYN).distinct()
        } else {
            listOf(self, incoming.caller).distinct()
        }
        telecomManager.cancelIncomingCallNotification()
        _callState.update {
            it.copy(
                isLocalMicMuted = false,
                isLocalCameraOn = useVideo,
                isVoiceCallMode = !useVideo,
                isFrontCamera = true,
                durationSeconds = 0L,
                showLiveDrawingOverlay = false,
                networkQualityLabel = if (useVideo) "Joining video call…" else "Joining voice call…"
            )
        }
        telecomManager.startManagedFamilyCall(
            callerOrTarget = incoming.caller,
            isIncoming = false,
            isVideo = useVideo,
            onAnswered = { },
            onDisconnected = { endCallInternal("Call ended", notifyRemote = false) }
        )
        connectCallMedia(members, roomName, useVideo)
    }

    fun declineIncomingCall() {
        val incoming = _callState.value.phase as? CallPhase.RingingIncoming
        val self = _session.value.activeMember ?: FamilyMemberId.MITHUN
        telecomManager.cancelIncomingCallNotification()
        if (incoming != null) {
            realtimeSignaling.broadcastCallDecline(
                sender = self,
                targets = listOf(incoming.caller),
                roomName = incoming.roomName
            )
            val kind = if (incoming.videoEnabled) "Video" else "Voice"
            val missedLog = CallLogEntry(
                id = UUID.randomUUID().toString(),
                participantsLabel = incoming.caller.displayName,
                callTypeLabel = if (incoming.isThreeWay) "3-Way $kind (Declined)" else "1-on-1 $kind (Declined)",
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
        publishSelfPresenceNow(isHello = false)
    }

    fun inviteThirdMemberMidCall(member: FamilyMemberId) {
        val self = _session.value.activeMember ?: FamilyMemberId.MITHUN
        val currentState = _callState.value
        realtimeSignaling.broadcastCallInvite(
            caller = self,
            targets = listOf(member),
            roomName = currentState.roomName.ifBlank { LiveKitCallEngine.DEFAULT_FAMILY_ROOM },
            isThreeWay = true,
            videoEnabled = currentState.isLocalCameraOn || !currentState.isVoiceCallMode
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

    private fun connectCallMedia(
        members: List<FamilyMemberId>,
        roomName: String,
        videoEnabled: Boolean = _callState.value.isLocalCameraOn
    ) {
        if (_callState.value.phase is CallPhase.Active) return
        val self = _session.value.activeMember ?: FamilyMemberId.MITHUN
        val preferredRoute = _callState.value.audioRoute
        _callState.update { state ->
            state.copy(
                roomName = roomName,
                phase = CallPhase.Active,
                isLocalCameraOn = videoEnabled,
                isVoiceCallMode = !videoEnabled,
                participants = members.map { member ->
                    CallParticipantState(
                        memberId = member,
                        isConnected = member == self,
                        isSpeaking = false,
                        audioLevel = if (member == self) 0.25f else 0f,
                        isMuted = if (member == self) state.isLocalMicMuted else false,
                        isCameraEnabled = member == self && videoEnabled
                    )
                }
            )
        }
        publishSelfPresenceNow(isHello = false)

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
                enableCamera = _callState.value.isLocalCameraOn,
                audioRoute = preferredRoute
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
                            val pulse = ((nextSec + idx) % 3L == 0L) && !p.isMuted && p.isConnected
                            p.copy(
                                isSpeaking = pulse,
                                audioLevel = if (p.isMuted || !p.isConnected) 0f else if (pulse) 0.72f else 0.18f
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
                    if (it.memberId == self) it.copy(isMuted = nextMuted, audioLevel = 0f, isSpeaking = false) else it
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
                isVoiceCallMode = if (nextOn) false else state.isVoiceCallMode,
                participants = state.participants.map {
                    if (it.memberId == self) it.copy(isCameraEnabled = nextOn) else it
                }
            )
        }
        publishSelfPresenceNow(isHello = false)
    }

    fun flipCamera() {
        if (!_callState.value.isLocalCameraOn) return
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
        endCallInternal("Call finished", notifyRemote = true)
    }

    private fun endCallInternal(reason: String, notifyRemote: Boolean = true) {
        val snapshot = _callState.value
        val self = _session.value.activeMember ?: FamilyMemberId.MITHUN
        val remoteConnected = liveKitEngine.connectedLiveMembers.value.any { it != self }

        // If nobody answered yet, cancel ringing on the other family phones
        if (notifyRemote && !remoteConnected && snapshot.phase !is CallPhase.Idle) {
            val targets = snapshot.participants.map { it.memberId }.filter { it != self }
            if (targets.isNotEmpty()) {
                realtimeSignaling.broadcastCallEnded(
                    sender = self,
                    targets = targets,
                    roomName = snapshot.roomName
                )
            }
        }

        callTimerJob?.cancel()
        telecomManager.endManagedCall()
        liveKitEngine.disconnect()
        CallForegroundService.stop(appContext)
        val mins = snapshot.durationSeconds / 60
        val secs = snapshot.durationSeconds % 60
        val callId = "call_${UUID.randomUUID()}"
        val participantsCsv = snapshot.participants.joinToString(",") { it.memberId.name }
            .ifBlank { self.name }
        val kindWord = if (snapshot.isVoiceCallMode && !snapshot.isLocalCameraOn) "Voice" else "Video"
        val log = CallLogEntry(
            id = callId,
            participantsLabel = snapshot.participants.joinToString(", ") { it.memberId.displayName }
                .ifBlank { "Family Call" },
            callTypeLabel = if (snapshot.participants.size >= 3) "3-Way $kindWord" else "1-on-1 $kindWord",
            isMissed = false,
            timestampLabel = "Just now",
            durationLabel = "%02d:%02d".format(mins, secs)
        )
        _callState.update {
            it.copy(
                phase = CallPhase.Idle,
                durationSeconds = 0L,
                isLocalMicMuted = false,
                showLiveDrawingOverlay = false,
                recentCallLogs = listOf(log) + it.recentCallLogs
            )
        }
        publishSelfPresenceNow(isHello = false)
        if (snapshot.durationSeconds > 0L) {
            val caller = _session.value.deviceOwner ?: self
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
        recordUserInteraction()
        val started = voiceCapture.startAacRecording("walkie")
        _isWalkieRecording.value = started != null
        if (_isWalkieRecording.value) {
            publishSelfPresenceNow(isHello = false)
        }
    }

    fun releaseWalkieTalkieHold() {
        if (!_isWalkieRecording.value) return
        _isWalkieRecording.value = false
        publishSelfPresenceNow(isHello = false)
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

    private data class LiveKitRoomSnapshot(
        val connected: Set<FamilyMemberId>,
        val speaking: Set<FamilyMemberId>,
        val tracks: Map<FamilyMemberId, io.livekit.android.room.track.VideoTrack>,
        val muted: Set<FamilyMemberId>,
        val liveActive: Boolean
    )

    companion object {
        private const val PREFS_DEVICE_BINDING = "holymoly_device_binding"
        private const val KEY_ASSIGNED_MEMBER = "logged_in_member_v2"
        private const val PRESENCE_TIMEOUT_MS = 35_000L
        private const val IDLE_AWAY_TIMEOUT_MS = 180_000L
    }
}
