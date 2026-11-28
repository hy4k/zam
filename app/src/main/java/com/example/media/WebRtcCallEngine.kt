package com.example.media

import android.content.Context
import android.util.Log
import com.example.data.model.ArFilterType
import com.example.data.model.AudioRoute
import com.example.data.model.FamilyMemberId
import com.example.sync.SupabaseRealtimeSignaling
import com.example.sync.WebRtcSignalEnvelope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.webrtc.DataChannel
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import org.webrtc.RtpSender
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Serializable
sealed interface WebRtcDataPacket {
    @Serializable
    data class EmojiBurstPacket(
        val senderId: String,
        val emoji: String,
        val xFraction: Float
    ) : WebRtcDataPacket

    @Serializable
    data class ArFilterPacket(
        val senderId: String,
        val filterName: String
    ) : WebRtcDataPacket

    @Serializable
    data class ScreenTapPulsePacket(
        val senderId: String
    ) : WebRtcDataPacket

    @Serializable
    data class DrawingStrokePacket(
        val strokeId: Long,
        val senderId: String,
        val colorArgb: Long,
        val strokeWidthDp: Float,
        val points: List<Pair<Float, Float>>
    ) : WebRtcDataPacket
}

/**
 * Native WebRTC PeerConnection 3-Way Mesh Engine for Mithun, Mafitha & Zayn Adam.
 *
 * Modular architecture:
 * - [TurnCredentialsProvider]: Google public STUN + dynamic short-TTL coturn TURN (UDP/TCP/TLS:443)
 * - [WebRtcMediaManager]: PeerConnectionFactory, shared EglBase, hardware AEC/NS audio, Camera2 capture
 * - [WebRtcSignalingClient]: Room-scoped WebSocket signaling with max-3 cap, peer lists & heartbeats
 *
 * Reliability features:
 * - True Perfect Negotiation (polite vs. impolite rollback on mid-call SDP glare)
 * - Automatic ICE restart on Wi-Fi <-> Cellular network switches
 * - Escalating failure recovery: ICE disconnected -> ICE restart -> full PeerConnection renegotiation -> graceful dead-peer removal & seamless rejoin
 * - Real-time WebRTC DataChannels (with signaling fallback) for AR filters, emojis, taps & drawing
 */
class WebRtcCallEngine(
    context: Context,
    realtimeSignaling: SupabaseRealtimeSignaling,
    private val mediaManager: WebRtcMediaManager = WebRtcMediaManager(context),
    private val signalingClient: WebRtcSignalingClient = WebRtcSignalingClient(realtimeSignaling),
    private val turnProvider: TurnCredentialsProvider = TurnCredentialsProvider()
) {
    private val coroutineErrorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.w(TAG, "WebRTC background coroutine error", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineErrorHandler)
    private val json = Json { ignoreUnknownKeys = true }
    private val lifecycleMutex = Mutex()
    private val callGeneration = AtomicLong(0L)

    val eglBaseContext: EglBase.Context
        get() = mediaManager.eglBaseContext

    @Volatile
    private var localMember: FamilyMemberId = FamilyMemberId.MITHUN

    @Volatile
    private var activeRoomName: String = DEFAULT_FAMILY_ROOM

    @Volatile
    private var expectedMembers: Set<FamilyMemberId> = emptySet()

    @Volatile
    private var desiredMicEnabled: Boolean = true

    @Volatile
    private var desiredCameraEnabled: Boolean = true

    @Volatile
    private var cachedIceServers: List<PeerConnection.IceServer> = turnProvider.getCachedOrDefaultIceServers()

    @Volatile
    private var lastNetworkRecoveryAtMs: Long = 0L

    // 3-Way Mesh Peer Slots (strictly capped at MAX_ROOM_PARTICIPANTS - 1 = 2 remote peers)
    private inner class PeerSlot(
        val remoteMember: FamilyMemberId,
        val pc: PeerConnection,
        @Volatile var remoteSessionId: String = ""
    ) {
        val mutex = Mutex()
        val pendingRemoteIce = mutableListOf<IceCandidate>()
        @Volatile var remoteDescriptionSet: Boolean = false
        @Volatile var isMakingOffer: Boolean = false
        @Volatile var ignoreOffer: Boolean = false
        @Volatile var isConnected: Boolean = false
        @Volatile var remoteCameraEnabled: Boolean = true
        @Volatile var remoteMicEnabled: Boolean = true
        @Volatile var remoteVideoTrack: VideoTrack? = null
        @Volatile var remoteAudioTrack: org.webrtc.AudioTrack? = null
        @Volatile var videoSender: RtpSender? = null
        @Volatile var audioSender: RtpSender? = null
        @Volatile var dataChannel: DataChannel? = null
        @Volatile var iceRestartAttempts: Int = 0
        @Volatile var fullRenegotiationAttempts: Int = 0
        @Volatile var lastHeartbeatAtMs: Long = System.currentTimeMillis()
        @Volatile var disconnectedSinceMs: Long = 0L
        var reconnectTimerJob: Job? = null
    }

    private val peers = ConcurrentHashMap<FamilyMemberId, PeerSlot>()
    private var signalCollectorJob: Job? = null
    private var joinBeaconJob: Job? = null
    private var heartbeatMonitorJob: Job? = null
    private var audioLevelPollJob: Job? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isReconnecting = MutableStateFlow(false)
    val isReconnecting: StateFlow<Boolean> = _isReconnecting.asStateFlow()

    private val _isLiveStreamActive = MutableStateFlow(false)
    val isLiveStreamActive: StateFlow<Boolean> = _isLiveStreamActive.asStateFlow()

    private val _connectionStatusLabel = MutableStateFlow("Ready to call")
    val connectionStatusLabel: StateFlow<String> = _connectionStatusLabel.asStateFlow()

    private val _videoTracksByMember = MutableStateFlow<Map<FamilyMemberId, VideoTrack>>(emptyMap())
    val videoTracksByMember: StateFlow<Map<FamilyMemberId, VideoTrack>> = _videoTracksByMember.asStateFlow()

    private val _connectedLiveMembers = MutableStateFlow<Set<FamilyMemberId>>(emptySet())
    val connectedLiveMembers: StateFlow<Set<FamilyMemberId>> = _connectedLiveMembers.asStateFlow()

    private val _speakingMembers = MutableStateFlow<Set<FamilyMemberId>>(emptySet())
    val speakingMembers: StateFlow<Set<FamilyMemberId>> = _speakingMembers.asStateFlow()

    private val _mutedMembers = MutableStateFlow<Set<FamilyMemberId>>(emptySet())
    val mutedMembers: StateFlow<Set<FamilyMemberId>> = _mutedMembers.asStateFlow()

    private val _incomingDataPackets = MutableSharedFlow<WebRtcDataPacket>(extraBufferCapacity = 32)
    val incomingDataPackets: SharedFlow<WebRtcDataPacket> = _incomingDataPackets.asSharedFlow()

    private val _roomFullEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val roomFullEvents: SharedFlow<String> = _roomFullEvents.asSharedFlow()

    private val _peerDroppedEvents = MutableSharedFlow<FamilyMemberId>(extraBufferCapacity = 8)
    val peerDroppedEvents: SharedFlow<FamilyMemberId> = _peerDroppedEvents.asSharedFlow()

    fun isSignalingConfigured(): Boolean = signalingClient.isConfigured()

    suspend fun connectToFamilyRoom(
        member: FamilyMemberId,
        roomName: String = DEFAULT_FAMILY_ROOM,
        participants: List<FamilyMemberId> = FamilyMemberId.entries,
        enableMic: Boolean = true,
        enableCamera: Boolean = true,
        audioRoute: AudioRoute = AudioRoute.SPEAKER
    ): Result<Boolean> = lifecycleMutex.withLock {
        val gen = callGeneration.incrementAndGet()
        teardownInternal(notifyLeave = false)

        localMember = member
        activeRoomName = roomName.ifBlank { DEFAULT_FAMILY_ROOM }
        expectedMembers = (participants.toSet() + member).take(WebRtcSignalingClient.MAX_ROOM_PARTICIPANTS).toSet()
        desiredMicEnabled = enableMic
        desiredCameraEnabled = enableCamera
        signalingClient.rotateSessionId()

        _isReconnecting.value = false
        _mutedMembers.value = if (!enableMic) setOf(member) else emptySet()
        _connectionStatusLabel.value = if (enableCamera) "Connecting video call…" else "Connecting voice call…"

        mediaManager.configureCommunicationAudio(audioRoute)

        return@withLock runCatching {
            mediaManager.ensureFactory()
            if (callGeneration.get() != gen) return@runCatching false

            // 1. Initialize local mic & camera immediately so self video renders with zero delay
            mediaManager.ensureLocalAudio(member = member, enabled = enableMic)
            mediaManager.ensureLocalVideo(member = member, enabled = enableCamera, resetFrontCamera = true)

            _isConnected.value = true
            _isLiveStreamActive.value = true
            syncPublishedState()

            // 2. Fetch short-TTL dynamic TURN credentials (falls back gracefully to STUN / static TURN)
            cachedIceServers = turnProvider.getIceServers(
                identity = member.name,
                roomName = activeRoomName,
                forceRefresh = false
            )
            if (callGeneration.get() != gen) return@runCatching false

            // 3. Ensure WebSocket signaling channel is subscribed for instant SDP/ICE exchange
            val subscribed = signalingClient.ensureConnected()
            if (callGeneration.get() != gen) return@runCatching false

            if (!subscribed) {
                _connectionStatusLabel.value = "Waiting for internet signaling…"
            }

            // 4. Start room-scoped signaling collector, join beacon, heartbeat monitor, and audio meter
            startSignalCollector(gen)
            startJoinBeacon(gen)
            startHeartbeatAndDeadPeerMonitor(gen)
            startAudioStatsLoop(gen)

            // 5. Re-enforce chosen audio route once WebRTC audio subsystem settles
            scope.launch {
                delay(350L)
                if (callGeneration.get() == gen && _isConnected.value) {
                    mediaManager.configureCommunicationAudio(mediaManager.currentAudioRoute)
                }
            }
            true
        }.onFailure { err ->
            Log.w(TAG, "connectToFamilyRoom failed", err)
            if (callGeneration.get() == gen) {
                _isLiveStreamActive.value = false
                _isConnected.value = false
                _isReconnecting.value = false
                _connectionStatusLabel.value = "Couldn't start WebRTC call — please retry"
            }
        }
    }

    /**
     * Triggered by [android.net.ConnectivityManager] when the phone switches networks
     * (e.g. Wi-Fi <-> 4G/5G) or regains connectivity mid-call.
     * Refreshes TURN credentials if needed, re-subscribes signaling, and performs ICE restart
     * across every active mesh peer so the call survives without dropping.
     */
    fun onNetworkChanged() {
        val gen = callGeneration.get()
        if (!_isLiveStreamActive.value) return
        val now = System.currentTimeMillis()
        if (now - lastNetworkRecoveryAtMs < NETWORK_SWITCH_DEBOUNCE_MS) return
        lastNetworkRecoveryAtMs = now

        scope.launch {
            if (callGeneration.get() != gen || !_isLiveStreamActive.value) return@launch
            Log.i(TAG, "Network change detected during active call — recovering signaling + ICE restart")
            if (peers.isNotEmpty()) {
                _isReconnecting.value = true
                _connectionStatusLabel.value = "Switching network — reconnecting…"
            }

            // 1. Refresh short-TTL TURN credentials on the new network path
            cachedIceServers = turnProvider.getIceServers(
                identity = localMember.name,
                roomName = activeRoomName,
                forceRefresh = true
            )

            // 2. Reset and re-subscribe Supabase Realtime signaling socket on the new transport
            signalingClient.reconnectAfterNetworkSwitch()
            if (callGeneration.get() != gen || !_isLiveStreamActive.value) return@launch

            // 3. Re-announce our presence in the room and trigger ICE restart for all active peers
            if (peers.isEmpty()) {
                signalingClient.sendJoin(
                    roomName = activeRoomName,
                    sender = localMember,
                    cameraEnabled = isLocalCameraPublished(),
                    micEnabled = isLocalMicPublished(),
                    activePeers = currentActiveMembers()
                )
            } else {
                peers.values.toList().forEach { slot ->
                    slot.iceRestartAttempts = 0
                    triggerIceRestartForSlot(slot, gen, reason = "network_switch")
                }
            }
        }
    }

    private fun startSignalCollector(gen: Long) {
        signalCollectorJob?.cancel()
        signalCollectorJob = scope.launch {
            signalingClient.incomingForRoom(activeRoomName, localMember).collect { envelope ->
                if (callGeneration.get() != gen) return@collect

                if (envelope.type == WebRtcSignalingClient.TYPE_ROOM_FULL) {
                    handleRoomFull(envelope)
                    return@collect
                }

                // Detect a 4th device trying to join with the SAME FamilyMemberId while room is already at capacity
                val sameMemberDifferentSession = envelope.senderId.equals(localMember.name, ignoreCase = true) &&
                    envelope.sessionId.isNotBlank() &&
                    envelope.sessionId != signalingClient.currentSessionId
                if (sameMemberDifferentSession) {
                    if (envelope.type == WebRtcSignalingClient.TYPE_JOIN &&
                        currentActiveMembers().size >= WebRtcSignalingClient.MAX_ROOM_PARTICIPANTS
                    ) {
                        signalingClient.sendRoomFull(
                            roomName = activeRoomName,
                            sender = localMember,
                            targetSessionOrId = envelope.sessionId,
                            activePeers = currentActiveMembers()
                        )
                    }
                    return@collect
                }

                val remote = runCatching { FamilyMemberId.valueOf(envelope.senderId) }.getOrNull()
                    ?: return@collect
                if (remote == localMember) return@collect

                peers[remote]?.let { slot ->
                    slot.lastHeartbeatAtMs = System.currentTimeMillis()
                    if (envelope.sessionId.isNotBlank()) {
                        slot.remoteSessionId = envelope.sessionId
                    }
                }

                when (envelope.type) {
                    WebRtcSignalingClient.TYPE_JOIN -> handleRemoteJoin(remote, envelope, gen)
                    WebRtcSignalingClient.TYPE_JOIN_ACK -> handleRemoteJoinAck(remote, envelope, gen)
                    WebRtcSignalingClient.TYPE_PEER_HEARTBEAT -> handleRemoteHeartbeat(remote, envelope)
                    WebRtcSignalingClient.TYPE_ICE_RESTART_REQ -> handleRemoteIceRestartReq(remote, gen)
                    WebRtcSignalingClient.TYPE_OFFER -> handleRemoteOffer(remote, envelope, gen)
                    WebRtcSignalingClient.TYPE_ANSWER -> handleRemoteAnswer(remote, envelope, gen)
                    WebRtcSignalingClient.TYPE_ICE -> handleRemoteIce(remote, envelope, gen)
                    WebRtcSignalingClient.TYPE_LEAVE -> handleRemoteLeave(remote)
                    WebRtcSignalingClient.TYPE_MEDIA_STATE -> handleRemoteMediaState(remote, envelope)
                    WebRtcSignalingClient.TYPE_DATA_PACKET -> handleRemoteFallbackData(envelope.dataPayload)
                }
            }
        }
    }

    private fun startJoinBeacon(gen: Long) {
        joinBeaconJob?.cancel()
        joinBeaconJob = scope.launch {
            var attempts = 0
            while (isActive && callGeneration.get() == gen && attempts < 30) {
                val otherExpected = expectedMembers.filter { it != localMember }
                val allConnected = otherExpected.isNotEmpty() &&
                    otherExpected.all { peers[it]?.isConnected == true }
                if (!allConnected) {
                    signalingClient.sendJoin(
                        roomName = activeRoomName,
                        sender = localMember,
                        cameraEnabled = isLocalCameraPublished(),
                        micEnabled = isLocalMicPublished(),
                        activePeers = currentActiveMembers()
                    )
                }
                attempts++
                delay(if (attempts < 4) 1_500L else 3_000L)
            }
        }
    }

    /**
     * Sends periodic peer heartbeats and cleans up dead/crashed peers whose ICE stays
     * disconnected beyond [DEAD_PEER_TIMEOUT_MS] without heartbeats so their tile is
     * cleanly removed without disturbing remaining participants.
     */
    private fun startHeartbeatAndDeadPeerMonitor(gen: Long) {
        heartbeatMonitorJob?.cancel()
        heartbeatMonitorJob = scope.launch {
            while (isActive && callGeneration.get() == gen) {
                delay(HEARTBEAT_INTERVAL_MS)
                if (callGeneration.get() != gen || !_isLiveStreamActive.value) break

                signalingClient.sendPeerHeartbeat(
                    roomName = activeRoomName,
                    sender = localMember,
                    cameraEnabled = isLocalCameraPublished(),
                    micEnabled = isLocalMicPublished(),
                    activePeers = currentActiveMembers()
                )

                val now = System.currentTimeMillis()
                peers.values.toList().forEach { slot ->
                    val disconnectedTooLong = !slot.isConnected &&
                        slot.disconnectedSinceMs > 0L &&
                        (now - slot.disconnectedSinceMs) >= DEAD_PEER_TIMEOUT_MS
                    val silentTooLong = !slot.isConnected &&
                        (now - slot.lastHeartbeatAtMs) >= DEAD_PEER_TIMEOUT_MS
                    if (disconnectedTooLong || silentTooLong) {
                        Log.w(TAG, "Peer ${slot.remoteMember} timed out — dropping cleanly so rejoin works")
                        dropDeadPeer(slot.remoteMember)
                    }
                }
            }
        }
    }

    private fun startAudioStatsLoop(gen: Long) {
        audioLevelPollJob?.cancel()
        audioLevelPollJob = scope.launch {
            while (isActive && callGeneration.get() == gen) {
                delay(700L)
                val activeSpeakers = mutableSetOf<FamilyMemberId>()
                peers.values.forEach { slot ->
                    if (!slot.isConnected) return@forEach
                    val speakingDeferred = CompletableDeferred<Boolean>()
                    runCatching {
                        slot.pc.getStats { report ->
                            var loud = false
                            report.statsMap.values.forEach { stat ->
                                if (stat.type == "inbound-rtp" && stat.members["kind"] == "audio") {
                                    val level = (stat.members["audioLevel"] as? Number)?.toDouble() ?: 0.0
                                    if (level > 0.035) loud = true
                                }
                            }
                            speakingDeferred.complete(loud)
                        }
                    }.onFailure { speakingDeferred.complete(false) }
                    if (speakingDeferred.await() && slot.remoteMicEnabled) {
                        activeSpeakers.add(slot.remoteMember)
                    }
                }
                _speakingMembers.value = activeSpeakers
            }
        }
    }

    /**
     * Deterministic Perfect Negotiation role assignment:
     * - Impolite (initial offerer): alphabetically smaller name (`MAFITHA < MITHUN < ZAYN`)
     * - Polite (rolls back on SDP glare): alphabetically larger name (`localMember.name > remote.name`)
     */
    private fun isOffererFor(remote: FamilyMemberId): Boolean = localMember.name < remote.name
    private fun isPoliteFor(remote: FamilyMemberId): Boolean = localMember.name > remote.name

    private suspend fun getOrCreatePeerSlot(
        remote: FamilyMemberId,
        gen: Long,
        remoteSessionId: String = "",
        forceRecreate: Boolean = false
    ): PeerSlot? {
        // Enforce strict max-3 participant room cap (local + up to 2 remote peers)
        if (!peers.containsKey(remote) &&
            peers.size >= (WebRtcSignalingClient.MAX_ROOM_PARTICIPANTS - 1)
        ) {
            signalingClient.sendRoomFull(
                roomName = activeRoomName,
                sender = localMember,
                targetSessionOrId = remoteSessionId.ifBlank { remote.name },
                activePeers = currentActiveMembers()
            )
            return null
        }

        if (!forceRecreate) {
            peers[remote]?.let { existing ->
                val state = existing.pc.connectionState()
                val sessionSwitched = remoteSessionId.isNotBlank() &&
                    existing.remoteSessionId.isNotBlank() &&
                    existing.remoteSessionId != remoteSessionId
                if (!sessionSwitched &&
                    state != PeerConnection.PeerConnectionState.CLOSED &&
                    state != PeerConnection.PeerConnectionState.FAILED
                ) {
                    return existing
                }
                closePeerSlot(remote)
            }
        } else {
            closePeerSlot(remote)
        }

        val factory = mediaManager.ensureFactory()
        val iceServers = cachedIceServers.ifEmpty { turnProvider.getCachedOrDefaultIceServers() }
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            iceCandidatePoolSize = 4
        }

        var slotRef: PeerSlot? = null
        val observer = object : PeerConnection.Observer {
            override fun onSignalingChange(newState: PeerConnection.SignalingState?) = Unit

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                val slot = slotRef ?: return
                Log.d(TAG, "ICE[$remote] -> $newState")
                when (newState) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED -> {
                        slot.isConnected = true
                        slot.iceRestartAttempts = 0
                        slot.fullRenegotiationAttempts = 0
                        slot.disconnectedSinceMs = 0L
                        slot.lastHeartbeatAtMs = System.currentTimeMillis()
                        slot.reconnectTimerJob?.cancel()
                        slot.reconnectTimerJob = null
                        updateReconnectingFlag()
                        mediaManager.configureCommunicationAudio(mediaManager.currentAudioRoute)
                        syncPublishedState()
                        sendMediaStateTo(remote)
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED -> {
                        if (slot.disconnectedSinceMs == 0L) {
                            slot.disconnectedSinceMs = System.currentTimeMillis()
                        }
                        _isReconnecting.value = true
                        _connectionStatusLabel.value = "Reconnecting ${remote.displayName}…"
                        scheduleRecovery(slot, gen, immediate = false)
                    }
                    PeerConnection.IceConnectionState.FAILED -> {
                        slot.isConnected = false
                        if (slot.disconnectedSinceMs == 0L) {
                            slot.disconnectedSinceMs = System.currentTimeMillis()
                        }
                        _isReconnecting.value = true
                        _connectionStatusLabel.value = "Reconnecting ${remote.displayName}…"
                        syncPublishedState()
                        scheduleRecovery(slot, gen, immediate = true)
                    }
                    PeerConnection.IceConnectionState.CLOSED -> {
                        slot.isConnected = false
                        syncPublishedState()
                    }
                    else -> Unit
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                val slot = slotRef ?: return
                when (newState) {
                    PeerConnection.PeerConnectionState.CONNECTED -> {
                        slot.isConnected = true
                        slot.iceRestartAttempts = 0
                        slot.fullRenegotiationAttempts = 0
                        slot.disconnectedSinceMs = 0L
                        slot.lastHeartbeatAtMs = System.currentTimeMillis()
                        updateReconnectingFlag()
                        mediaManager.configureCommunicationAudio(mediaManager.currentAudioRoute)
                        syncPublishedState()
                    }
                    PeerConnection.PeerConnectionState.DISCONNECTED -> {
                        if (slot.disconnectedSinceMs == 0L) {
                            slot.disconnectedSinceMs = System.currentTimeMillis()
                        }
                        _isReconnecting.value = true
                        _connectionStatusLabel.value = "Reconnecting ${remote.displayName}…"
                        scheduleRecovery(slot, gen, immediate = false)
                    }
                    PeerConnection.PeerConnectionState.FAILED -> {
                        slot.isConnected = false
                        if (slot.disconnectedSinceMs == 0L) {
                            slot.disconnectedSinceMs = System.currentTimeMillis()
                        }
                        _isReconnecting.value = true
                        syncPublishedState()
                        scheduleRecovery(slot, gen, immediate = true)
                    }
                    else -> Unit
                }
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) = Unit

            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate == null || callGeneration.get() != gen) return
                signalingClient.sendIceCandidate(
                    roomName = activeRoomName,
                    sender = localMember,
                    target = remote,
                    sdpMid = candidate.sdpMid ?: "",
                    sdpMLineIndex = candidate.sdpMLineIndex,
                    sdp = candidate.sdp
                )
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit

            override fun onAddStream(stream: MediaStream?) {
                val slot = slotRef ?: return
                stream?.videoTracks?.firstOrNull()?.let { track ->
                    track.setEnabled(true)
                    slot.remoteVideoTrack = track
                }
                stream?.audioTracks?.firstOrNull()?.let { track ->
                    track.setEnabled(true)
                    slot.remoteAudioTrack = track
                }
                syncPublishedState()
            }

            override fun onRemoveStream(stream: MediaStream?) = Unit

            override fun onTrack(transceiver: RtpTransceiver?) {
                val slot = slotRef ?: return
                when (val track = transceiver?.receiver?.track()) {
                    is VideoTrack -> {
                        track.setEnabled(true)
                        slot.remoteVideoTrack = track
                        syncPublishedState()
                    }
                    is org.webrtc.AudioTrack -> {
                        track.setEnabled(true)
                        slot.remoteAudioTrack = track
                        mediaManager.configureCommunicationAudio(mediaManager.currentAudioRoute)
                        syncPublishedState()
                    }
                }
            }

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                val slot = slotRef ?: return
                when (val track = receiver?.track()) {
                    is VideoTrack -> {
                        track.setEnabled(true)
                        slot.remoteVideoTrack = track
                        syncPublishedState()
                    }
                    is org.webrtc.AudioTrack -> {
                        track.setEnabled(true)
                        slot.remoteAudioTrack = track
                        syncPublishedState()
                    }
                }
            }

            override fun onDataChannel(dc: DataChannel?) {
                val slot = slotRef ?: return
                if (dc != null) {
                    attachDataChannel(slot, dc)
                }
            }

            override fun onRenegotiationNeeded() = Unit
        }

        val pc = factory.createPeerConnection(rtcConfig, observer) ?: return null
        val slot = PeerSlot(remote, pc, remoteSessionId)
        slotRef = slot

        val streamIds = listOf("holymoly_stream_${localMember.name}")
        mediaManager.localAudioTrack?.let { audio ->
            slot.audioSender = runCatching { pc.addTrack(audio, streamIds) }.getOrNull()
        }
        mediaManager.localVideoTrack?.let { video ->
            slot.videoSender = runCatching { pc.addTrack(video, streamIds) }.getOrNull()
        }

        if (isOffererFor(remote)) {
            val init = DataChannel.Init().apply { ordered = true }
            val dc = runCatching { pc.createDataChannel("family-data", init) }.getOrNull()
            if (dc != null) {
                attachDataChannel(slot, dc)
            }
        }

        peers[remote] = slot
        return slot
    }

    private fun attachDataChannel(slot: PeerSlot, dc: DataChannel) {
        slot.dataChannel = dc
        dc.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) = Unit
            override fun onStateChange() {
                if (dc.state() == DataChannel.State.OPEN) {
                    sendMediaStateTo(slot.remoteMember)
                }
            }

            override fun onMessage(buffer: DataChannel.Buffer) {
                val data = buffer.data
                val bytes = ByteArray(data.remaining())
                data.get(bytes)
                val raw = bytes.decodeToString()
                runCatching { json.decodeFromString<WebRtcDataPacket>(raw) }
                    .onSuccess { packet -> _incomingDataPackets.tryEmit(packet) }
            }
        })
    }

    /**
     * Escalating per-peer failure recovery:
     * 1. Attempts 1..2: ICE restart (impolite peer creates ICE-restart offer; polite peer sends ICE_RESTART_REQ)
     * 2. Attempt 3: Full renegotiation fallback (rebuild fresh PeerConnection with fresh TURN servers & re-offer)
     * 3. If still dead after full renegotiation: cleanly drop dead peer so remaining participants continue uninterrupted
     */
    private fun scheduleRecovery(slot: PeerSlot, gen: Long, immediate: Boolean) {
        slot.reconnectTimerJob?.cancel()
        slot.reconnectTimerJob = scope.launch {
            if (!immediate) delay(ICE_DISCONNECTED_GRACE_MS)
            if (callGeneration.get() != gen || slot.isConnected || peers[slot.remoteMember] !== slot) {
                return@launch
            }

            if (slot.iceRestartAttempts < MAX_ICE_RESTART_ATTEMPTS) {
                slot.iceRestartAttempts++
                Log.i(TAG, "ICE restart attempt ${slot.iceRestartAttempts} for ${slot.remoteMember}")
                triggerIceRestartForSlot(slot, gen, reason = "ice_recovery_${slot.iceRestartAttempts}")
                // Follow up if this ICE restart doesn't recover within 4.5 seconds
                delay(4_500L)
                if (callGeneration.get() == gen && !slot.isConnected && peers[slot.remoteMember] === slot) {
                    scheduleRecovery(slot, gen, immediate = true)
                }
            } else if (slot.fullRenegotiationAttempts < MAX_FULL_RENEGOTIATION_ATTEMPTS) {
                slot.fullRenegotiationAttempts++
                Log.w(TAG, "ICE restart exhausted for ${slot.remoteMember} — falling back to full PeerConnection renegotiation")
                performFullRenegotiation(slot.remoteMember, gen)
            } else {
                Log.w(TAG, "Full renegotiation failed for ${slot.remoteMember} — removing dead peer gracefully")
                dropDeadPeer(slot.remoteMember)
            }
        }
    }

    private suspend fun triggerIceRestartForSlot(slot: PeerSlot, gen: Long, reason: String) {
        if (callGeneration.get() != gen) return
        if (isOffererFor(slot.remoteMember)) {
            createAndSendOffer(slot, iceRestart = true, gen = gen)
        } else {
            // Polite peer asks impolite peer for an ICE restart AND triggers its own ICE restart
            // offer after a short beat if the impolite peer hasn't responded (Perfect Negotiation handles glare)
            signalingClient.sendIceRestartRequest(
                roomName = activeRoomName,
                sender = localMember,
                target = slot.remoteMember,
                reason = reason
            )
            delay(800L)
            if (callGeneration.get() == gen &&
                !slot.isConnected &&
                peers[slot.remoteMember] === slot &&
                slot.pc.signalingState() == PeerConnection.SignalingState.STABLE
            ) {
                createAndSendOffer(slot, iceRestart = true, gen = gen)
            }
        }
    }

    private suspend fun performFullRenegotiation(remote: FamilyMemberId, gen: Long) {
        if (callGeneration.get() != gen || !_isLiveStreamActive.value) return
        cachedIceServers = turnProvider.getIceServers(
            identity = localMember.name,
            roomName = activeRoomName,
            forceRefresh = true
        )
        val freshSlot = getOrCreatePeerSlot(remote, gen, forceRecreate = true) ?: return
        freshSlot.fullRenegotiationAttempts = 1
        freshSlot.disconnectedSinceMs = System.currentTimeMillis()
        if (isOffererFor(remote)) {
            createAndSendOffer(freshSlot, iceRestart = false, gen = gen)
        } else {
            signalingClient.sendJoinAck(
                roomName = activeRoomName,
                sender = localMember,
                target = remote,
                cameraEnabled = isLocalCameraPublished(),
                micEnabled = isLocalMicPublished(),
                activePeers = currentActiveMembers()
            )
        }
    }

    private fun dropDeadPeer(remote: FamilyMemberId) {
        closePeerSlot(remote)
        updateReconnectingFlag()
        syncPublishedState()
        _peerDroppedEvents.tryEmit(remote)
    }

    private fun updateReconnectingFlag() {
        val anyReconnecting = peers.values.any { !it.isConnected && it.disconnectedSinceMs > 0L }
        _isReconnecting.value = anyReconnecting
    }

    private fun handleRoomFull(envelope: WebRtcSignalEnvelope) {
        val msg = envelope.reason.ifBlank {
            "Room is full (max ${WebRtcSignalingClient.MAX_ROOM_PARTICIPANTS} participants)"
        }
        Log.w(TAG, "Rejected from room $activeRoomName: $msg")
        _connectionStatusLabel.value = msg
        _roomFullEvents.tryEmit(msg)
        disconnect()
    }

    private suspend fun handleRemoteJoin(remote: FamilyMemberId, envelope: WebRtcSignalEnvelope, gen: Long) {
        val existing = peers[remote]
        val isSameSession = existing != null &&
            (envelope.sessionId.isBlank() || existing.remoteSessionId.isBlank() || existing.remoteSessionId == envelope.sessionId)
        if (existing != null && existing.isConnected && isSameSession) {
            existing.remoteCameraEnabled = envelope.cameraEnabled
            existing.remoteMicEnabled = envelope.micEnabled
            existing.lastHeartbeatAtMs = System.currentTimeMillis()
            syncPublishedState()
            sendMediaStateTo(remote)
            return
        }

        val slot = getOrCreatePeerSlot(
            remote = remote,
            gen = gen,
            remoteSessionId = envelope.sessionId,
            forceRecreate = existing != null && !isSameSession
        ) ?: return

        slot.remoteCameraEnabled = envelope.cameraEnabled
        slot.remoteMicEnabled = envelope.micEnabled
        slot.lastHeartbeatAtMs = System.currentTimeMillis()
        syncPublishedState()

        if (isOffererFor(remote)) {
            createAndSendOffer(slot, iceRestart = false, gen = gen)
        } else {
            signalingClient.sendJoinAck(
                roomName = activeRoomName,
                sender = localMember,
                target = remote,
                cameraEnabled = isLocalCameraPublished(),
                micEnabled = isLocalMicPublished(),
                activePeers = currentActiveMembers()
            )
        }
    }

    private suspend fun handleRemoteJoinAck(remote: FamilyMemberId, envelope: WebRtcSignalEnvelope, gen: Long) {
        val slot = getOrCreatePeerSlot(remote, gen, remoteSessionId = envelope.sessionId) ?: return
        slot.remoteCameraEnabled = envelope.cameraEnabled
        slot.remoteMicEnabled = envelope.micEnabled
        slot.lastHeartbeatAtMs = System.currentTimeMillis()
        syncPublishedState()
        if (isOffererFor(remote) && !slot.isConnected) {
            createAndSendOffer(slot, iceRestart = false, gen = gen)
        }
    }

    private fun handleRemoteHeartbeat(remote: FamilyMemberId, envelope: WebRtcSignalEnvelope) {
        val slot = peers[remote] ?: return
        slot.lastHeartbeatAtMs = System.currentTimeMillis()
        slot.remoteCameraEnabled = envelope.cameraEnabled
        slot.remoteMicEnabled = envelope.micEnabled
    }

    private suspend fun handleRemoteIceRestartReq(remote: FamilyMemberId, gen: Long) {
        val slot = peers[remote] ?: return
        Log.i(TAG, "Received ICE_RESTART_REQ from $remote — issuing ICE restart offer")
        createAndSendOffer(slot, iceRestart = true, gen = gen)
    }

    private suspend fun createAndSendOffer(slot: PeerSlot, iceRestart: Boolean, gen: Long) {
        slot.mutex.withLock {
            if (callGeneration.get() != gen) return
            if (slot.isMakingOffer) return
            if (!iceRestart && slot.pc.signalingState() != PeerConnection.SignalingState.STABLE) return

            slot.isMakingOffer = true
            try {
                val constraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                    if (iceRestart) {
                        mandatory.add(MediaConstraints.KeyValuePair("IceRestart", "true"))
                    }
                }
                val offer = createOfferAsync(slot.pc, constraints) ?: return
                if (callGeneration.get() != gen) return
                val setOk = setLocalDescriptionAsync(slot.pc, offer)
                if (!setOk || callGeneration.get() != gen) return

                signalingClient.sendOffer(
                    roomName = activeRoomName,
                    sender = localMember,
                    target = slot.remoteMember,
                    sdp = offer.description,
                    cameraEnabled = isLocalCameraPublished(),
                    micEnabled = isLocalMicPublished()
                )
            } finally {
                slot.isMakingOffer = false
            }
        }
    }

    /**
     * True Perfect Negotiation offer handler:
     * Detects offer collision (`isMakingOffer || signalingState != STABLE`):
     * - Impolite peer ignores the colliding remote offer and lets its own offer win.
     * - Polite peer performs SDP ROLLBACK (`SessionDescription.Type.ROLLBACK`) before
     *   applying the remote offer and replying with a fresh answer.
     */
    private suspend fun handleRemoteOffer(remote: FamilyMemberId, envelope: WebRtcSignalEnvelope, gen: Long) {
        if (envelope.sdp.isBlank()) return
        val slot = getOrCreatePeerSlot(remote, gen, remoteSessionId = envelope.sessionId) ?: return
        slot.remoteCameraEnabled = envelope.cameraEnabled
        slot.remoteMicEnabled = envelope.micEnabled

        slot.mutex.withLock {
            if (callGeneration.get() != gen) return
            val polite = isPoliteFor(remote)
            val currentState = slot.pc.signalingState()
            val offerCollision = slot.isMakingOffer || currentState != PeerConnection.SignalingState.STABLE

            slot.ignoreOffer = !polite && offerCollision
            if (slot.ignoreOffer) {
                Log.d(TAG, "Perfect Negotiation: impolite peer ignoring colliding offer from $remote")
                return
            }

            if (offerCollision && polite) {
                Log.d(TAG, "Perfect Negotiation: polite peer rolling back local offer for $remote")
                val rollbackOk = setLocalDescriptionAsync(
                    slot.pc,
                    SessionDescription(SessionDescription.Type.ROLLBACK, "")
                )
                if (!rollbackOk) {
                    Log.w(TAG, "Rollback failed for $remote — skipping colliding offer")
                    return
                }
            }

            val remoteSdp = SessionDescription(SessionDescription.Type.OFFER, envelope.sdp)
            val setRemoteOk = setRemoteDescriptionAsync(slot.pc, remoteSdp)
            if (!setRemoteOk) return

            slot.remoteDescriptionSet = true
            flushPendingIceCandidates(slot)

            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            }
            val answer = createAnswerAsync(slot.pc, constraints) ?: return
            val setLocalOk = setLocalDescriptionAsync(slot.pc, answer)
            if (!setLocalOk || callGeneration.get() != gen) return

            signalingClient.sendAnswer(
                roomName = activeRoomName,
                sender = localMember,
                target = remote,
                sdp = answer.description,
                cameraEnabled = isLocalCameraPublished(),
                micEnabled = isLocalMicPublished()
            )
        }
        syncPublishedState()
    }

    private suspend fun handleRemoteAnswer(remote: FamilyMemberId, envelope: WebRtcSignalEnvelope, gen: Long) {
        if (envelope.sdp.isBlank()) return
        val slot = peers[remote] ?: return
        slot.remoteCameraEnabled = envelope.cameraEnabled
        slot.remoteMicEnabled = envelope.micEnabled

        slot.mutex.withLock {
            if (callGeneration.get() != gen) return
            if (slot.pc.signalingState() != PeerConnection.SignalingState.HAVE_LOCAL_OFFER) return
            val remoteSdp = SessionDescription(SessionDescription.Type.ANSWER, envelope.sdp)
            val setOk = setRemoteDescriptionAsync(slot.pc, remoteSdp)
            if (setOk) {
                slot.remoteDescriptionSet = true
                flushPendingIceCandidates(slot)
            }
        }
        syncPublishedState()
    }

    private suspend fun handleRemoteIce(remote: FamilyMemberId, envelope: WebRtcSignalEnvelope, gen: Long) {
        if (envelope.iceCandidate.isBlank()) return
        val slot = getOrCreatePeerSlot(remote, gen, remoteSessionId = envelope.sessionId) ?: return
        val candidate = IceCandidate(envelope.iceMid, envelope.iceMLineIndex, envelope.iceCandidate)
        slot.mutex.withLock {
            if (slot.ignoreOffer) return
            if (slot.remoteDescriptionSet) {
                runCatching { slot.pc.addIceCandidate(candidate) }
            } else {
                slot.pendingRemoteIce.add(candidate)
            }
        }
    }

    private fun flushPendingIceCandidates(slot: PeerSlot) {
        if (slot.pendingRemoteIce.isEmpty()) return
        val queued = slot.pendingRemoteIce.toList()
        slot.pendingRemoteIce.clear()
        queued.forEach { candidate ->
            runCatching { slot.pc.addIceCandidate(candidate) }
        }
    }

    private fun handleRemoteMediaState(remote: FamilyMemberId, envelope: WebRtcSignalEnvelope) {
        val slot = peers[remote] ?: return
        slot.remoteCameraEnabled = envelope.cameraEnabled
        slot.remoteMicEnabled = envelope.micEnabled
        syncPublishedState()
    }

    private fun handleRemoteLeave(remote: FamilyMemberId) {
        dropDeadPeer(remote)
    }

    private fun handleRemoteFallbackData(raw: String) {
        if (raw.isBlank()) return
        runCatching { json.decodeFromString<WebRtcDataPacket>(raw) }
            .onSuccess { packet -> _incomingDataPackets.tryEmit(packet) }
    }

    private fun closePeerSlot(remote: FamilyMemberId) {
        val slot = peers.remove(remote) ?: return
        slot.reconnectTimerJob?.cancel()
        slot.reconnectTimerJob = null
        slot.isConnected = false
        slot.remoteVideoTrack = null
        slot.remoteAudioTrack = null
        runCatching { slot.dataChannel?.close() }
        runCatching { slot.dataChannel?.dispose() }
        runCatching { slot.pc.close() }
        runCatching { slot.pc.dispose() }
    }

    private fun isLocalCameraPublished(): Boolean = desiredCameraEnabled && mediaManager.localVideoTrack != null
    private fun isLocalMicPublished(): Boolean = desiredMicEnabled && mediaManager.localAudioTrack != null
    private fun currentActiveMembers(): Set<FamilyMemberId> = buildSet {
        add(localMember)
        peers.keys.forEach { add(it) }
    }

    private fun sendMediaStateTo(target: FamilyMemberId? = null) {
        signalingClient.sendMediaState(
            roomName = activeRoomName,
            sender = localMember,
            target = target,
            cameraEnabled = isLocalCameraPublished(),
            micEnabled = isLocalMicPublished()
        )
    }

    private fun syncPublishedState() {
        val trackMap = mutableMapOf<FamilyMemberId, VideoTrack>()
        val liveSet = mutableSetOf<FamilyMemberId>()
        val mutedSet = mutableSetOf<FamilyMemberId>()

        if (_isLiveStreamActive.value) {
            liveSet.add(localMember)
            val localVid = mediaManager.localVideoTrack
            if (localVid != null && desiredCameraEnabled) {
                trackMap[localMember] = localVid
            }
            if (!desiredMicEnabled || mediaManager.localAudioTrack == null) {
                mutedSet.add(localMember)
            }
        }

        peers.values.forEach { slot ->
            if (slot.isConnected) {
                liveSet.add(slot.remoteMember)
                val remoteVid = slot.remoteVideoTrack
                if (remoteVid != null && slot.remoteCameraEnabled) {
                    trackMap[slot.remoteMember] = remoteVid
                }
                if (!slot.remoteMicEnabled) {
                    mutedSet.add(slot.remoteMember)
                }
            }
        }

        _connectedLiveMembers.value = liveSet
        _videoTracksByMember.value = trackMap
        _mutedMembers.value = mutedSet

        if (_isLiveStreamActive.value && !_isReconnecting.value) {
            val count = liveSet.size
            val modeWord = if (trackMap.isNotEmpty()) "video" else "voice"
            _connectionStatusLabel.value = if (count >= 2) {
                "$count family members live • P2P $modeWord"
            } else {
                "Waiting for family to join…"
            }
        }
    }

    suspend fun setMicrophoneEnabled(enabled: Boolean): Boolean = lifecycleMutex.withLock {
        desiredMicEnabled = enabled
        if (enabled && !mediaManager.hasAudioPermission()) {
            syncPublishedState()
            return@withLock false
        }
        if (!_isLiveStreamActive.value) return@withLock false
        val created = if (enabled) mediaManager.ensureLocalAudio(localMember, true) else false
        if (created) {
            val track = mediaManager.localAudioTrack
            if (track != null) {
                val streamIds = listOf("holymoly_stream_${localMember.name}")
                val gen = callGeneration.get()
                peers.values.forEach { slot ->
                    if (slot.audioSender == null) {
                        slot.audioSender = runCatching { slot.pc.addTrack(track, streamIds) }.getOrNull()
                        scope.launch { createAndSendOffer(slot, iceRestart = false, gen = gen) }
                    }
                }
            }
        } else {
            mediaManager.setLocalAudioEnabled(enabled)
        }
        if (enabled) {
            mediaManager.configureCommunicationAudio(mediaManager.currentAudioRoute)
        }
        syncPublishedState()
        sendMediaStateTo(null)
        true
    }

    suspend fun setCameraEnabled(enabled: Boolean): Boolean = lifecycleMutex.withLock {
        desiredCameraEnabled = enabled
        if (enabled && !mediaManager.hasCameraPermission()) {
            syncPublishedState()
            return@withLock false
        }
        if (!_isLiveStreamActive.value) return@withLock false
        val created = if (enabled) mediaManager.ensureLocalVideo(localMember, true) else false
        if (created) {
            val track = mediaManager.localVideoTrack
            if (track != null) {
                val streamIds = listOf("holymoly_stream_${localMember.name}")
                val gen = callGeneration.get()
                peers.values.forEach { slot ->
                    if (slot.videoSender == null) {
                        slot.videoSender = runCatching { slot.pc.addTrack(track, streamIds) }.getOrNull()
                        scope.launch { createAndSendOffer(slot, iceRestart = false, gen = gen) }
                    }
                }
            }
        } else {
            mediaManager.setLocalVideoEnabled(enabled)
        }
        syncPublishedState()
        sendMediaStateTo(null)
        true
    }

    suspend fun switchCamera(): Boolean = lifecycleMutex.withLock {
        mediaManager.switchCamera()
    }

    fun configureCommunicationAudio(route: AudioRoute) {
        mediaManager.configureCommunicationAudio(route)
    }

    suspend fun broadcastEmojiBurst(sender: FamilyMemberId, emoji: String, xFraction: Float) {
        publishDataPacket(WebRtcDataPacket.EmojiBurstPacket(sender.name, emoji, xFraction))
    }

    suspend fun broadcastArFilter(sender: FamilyMemberId, filter: ArFilterType) {
        publishDataPacket(WebRtcDataPacket.ArFilterPacket(sender.name, filter.name))
    }

    suspend fun broadcastTapPulse(sender: FamilyMemberId) {
        publishDataPacket(WebRtcDataPacket.ScreenTapPulsePacket(sender.name))
    }

    suspend fun broadcastDrawingStroke(
        strokeId: Long,
        sender: FamilyMemberId,
        colorArgb: Long,
        strokeWidthDp: Float,
        points: List<Pair<Float, Float>>
    ) {
        publishDataPacket(
            WebRtcDataPacket.DrawingStrokePacket(
                strokeId = strokeId,
                senderId = sender.name,
                colorArgb = colorArgb,
                strokeWidthDp = strokeWidthDp,
                points = points
            )
        )
    }

    private fun publishDataPacket(packet: WebRtcDataPacket) {
        val raw = json.encodeToString(packet)
        val bytes = raw.toByteArray(Charsets.UTF_8)
        var sentAllViaDc = peers.isNotEmpty()
        peers.values.forEach { slot ->
            val dc = slot.dataChannel
            if (dc != null && dc.state() == DataChannel.State.OPEN) {
                runCatching { dc.send(DataChannel.Buffer(ByteBuffer.wrap(bytes), false)) }
                    .onFailure { sentAllViaDc = false }
            } else {
                sentAllViaDc = false
            }
        }
        if (!sentAllViaDc && _isLiveStreamActive.value) {
            signalingClient.sendFallbackDataPacket(
                roomName = activeRoomName,
                sender = localMember,
                payloadJson = raw
            )
        }
    }

    fun disconnect() {
        val gen = callGeneration.incrementAndGet()
        _videoTracksByMember.value = emptyMap()
        _isLiveStreamActive.value = false
        _isConnected.value = false
        _isReconnecting.value = false
        _connectedLiveMembers.value = emptySet()
        _speakingMembers.value = emptySet()
        _mutedMembers.value = emptySet()

        scope.launch {
            lifecycleMutex.withLock {
                if (callGeneration.get() == gen) {
                    teardownInternal(notifyLeave = true)
                    mediaManager.restoreNormalAudio()
                }
            }
        }
    }

    private fun teardownInternal(notifyLeave: Boolean) {
        signalCollectorJob?.cancel()
        signalCollectorJob = null
        joinBeaconJob?.cancel()
        joinBeaconJob = null
        heartbeatMonitorJob?.cancel()
        heartbeatMonitorJob = null
        audioLevelPollJob?.cancel()
        audioLevelPollJob = null

        if (notifyLeave && peers.isNotEmpty()) {
            signalingClient.sendLeave(roomName = activeRoomName, sender = localMember)
        }

        peers.keys.toList().forEach { remote -> closePeerSlot(remote) }
        peers.clear()
        mediaManager.releaseLocalMedia()
    }

    private suspend fun createOfferAsync(pc: PeerConnection, constraints: MediaConstraints): SessionDescription? {
        val deferred = CompletableDeferred<SessionDescription?>()
        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) { deferred.complete(sdp) }
            override fun onCreateFailure(error: String?) {
                Log.w(TAG, "createOffer failed: $error")
                deferred.complete(null)
            }
            override fun onSetSuccess() = Unit
            override fun onSetFailure(error: String?) = Unit
        }, constraints)
        return deferred.await()
    }

    private suspend fun createAnswerAsync(pc: PeerConnection, constraints: MediaConstraints): SessionDescription? {
        val deferred = CompletableDeferred<SessionDescription?>()
        pc.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) { deferred.complete(sdp) }
            override fun onCreateFailure(error: String?) {
                Log.w(TAG, "createAnswer failed: $error")
                deferred.complete(null)
            }
            override fun onSetSuccess() = Unit
            override fun onSetFailure(error: String?) = Unit
        }, constraints)
        return deferred.await()
    }

    private suspend fun setLocalDescriptionAsync(pc: PeerConnection, sdp: SessionDescription): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        pc.setLocalDescription(object : SdpObserver {
            override fun onSetSuccess() { deferred.complete(true) }
            override fun onSetFailure(error: String?) {
                Log.w(TAG, "setLocalDescription failed: $error")
                deferred.complete(false)
            }
            override fun onCreateSuccess(sdp: SessionDescription?) = Unit
            override fun onCreateFailure(error: String?) = Unit
        }, sdp)
        return deferred.await()
    }

    private suspend fun setRemoteDescriptionAsync(pc: PeerConnection, sdp: SessionDescription): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        pc.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() { deferred.complete(true) }
            override fun onSetFailure(error: String?) {
                Log.w(TAG, "setRemoteDescription failed: $error")
                deferred.complete(false)
            }
            override fun onCreateSuccess(sdp: SessionDescription?) = Unit
            override fun onCreateFailure(error: String?) = Unit
        }, sdp)
        return deferred.await()
    }

    companion object {
        private const val TAG = "WebRtcCallEngine"
        const val DEFAULT_FAMILY_ROOM = "holymoly-family-room"

        private const val NETWORK_SWITCH_DEBOUNCE_MS = 1_500L
        private const val ICE_DISCONNECTED_GRACE_MS = 2_500L
        private const val HEARTBEAT_INTERVAL_MS = 5_000L
        private const val DEAD_PEER_TIMEOUT_MS = 15_000L
        private const val MAX_ICE_RESTART_ATTEMPTS = 2
        private const val MAX_FULL_RENEGOTIATION_ATTEMPTS = 1
    }
}
