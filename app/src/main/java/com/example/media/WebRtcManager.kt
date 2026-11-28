package com.example.media

import android.content.Context
import android.util.Log
import com.example.data.model.AudioRoute
import com.example.data.model.FamilyMemberId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
import org.webrtc.AudioTrack
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

sealed interface WebRtcConnectionState {
    data object Idle : WebRtcConnectionState
    data object Connecting : WebRtcConnectionState
    data class Connected(val peerCount: Int) : WebRtcConnectionState
    data class Reconnecting(val reason: String) : WebRtcConnectionState
    data class Error(val message: String) : WebRtcConnectionState
}

/**
 * Android WebRTC Manager for 3-way family peer-to-peer mesh calls.
 *
 * Manages:
 * 1. **RTCPeerConnection creation & lifecycle** (Unified Plan, MAXBUNDLE, STUN + short-TTL TURN)
 *    for up to 2 remote family peers (3 total participants per room).
 * 2. **Local camera & microphone media capture** via [WebRtcMediaManager] (`Camera2`/`Camera1`
 *    at 640x480@24fps + hardware AEC/NS audio) and attaching local media tracks to every peer.
 * 3. **SDP offer/answer & ICE candidate exchange** over the Node.js Socket.io signaling server
 *    ([SocketIoSignalingClient]) using the Perfect Negotiation pattern (polite rollback on glare),
 *    ICE candidate buffering, automatic ICE restart on Wi-Fi <-> Cellular handoff, and full
 *    renegotiation fallback.
 */
class WebRtcManager(
    context: Context,
    private val mediaManager: WebRtcMediaManager = WebRtcMediaManager(context),
    private val signalingClient: SocketIoSignalingClient = SocketIoSignalingClient(),
    private val turnProvider: TurnCredentialsProvider = TurnCredentialsProvider()
) {
    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.w(TAG, "WebRtcManager coroutine error", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + errorHandler)
    private val lifecycleMutex = Mutex()
    private val sessionGeneration = AtomicLong(0L)

    val eglBaseContext: EglBase.Context
        get() = mediaManager.eglBaseContext

    @Volatile
    private var localMember: FamilyMemberId = FamilyMemberId.MITHUN

    @Volatile
    private var activeRoomId: String = DEFAULT_ROOM_ID

    @Volatile
    private var activePin: String? = null

    @Volatile
    private var cachedIceServers: List<PeerConnection.IceServer> = turnProvider.getCachedOrDefaultIceServers()

    @Volatile
    private var lastNetworkSwitchAtMs: Long = 0L

    private inner class MeshPeer(
        val memberId: FamilyMemberId,
        val peerConnection: PeerConnection,
        @Volatile var remoteSocketId: String = ""
    ) {
        val negotiationMutex = Mutex()
        val pendingRemoteIce = mutableListOf<IceCandidate>()
        @Volatile var remoteDescriptionSet: Boolean = false
        @Volatile var isMakingOffer: Boolean = false
        @Volatile var ignoreOffer: Boolean = false
        @Volatile var isConnected: Boolean = false
        @Volatile var remoteCameraEnabled: Boolean = true
        @Volatile var remoteMicEnabled: Boolean = true
        @Volatile var remoteVideoTrack: VideoTrack? = null
        @Volatile var remoteAudioTrack: AudioTrack? = null
        @Volatile var audioSender: RtpSender? = null
        @Volatile var videoSender: RtpSender? = null
        @Volatile var iceRestartAttempts: Int = 0
        @Volatile var fullRenegotiations: Int = 0
        var recoveryJob: Job? = null
    }

    private val peers = ConcurrentHashMap<FamilyMemberId, MeshPeer>()
    private var eventCollectorJob: Job? = null
    private var heartbeatJob: Job? = null

    private val _connectionState = MutableStateFlow<WebRtcConnectionState>(WebRtcConnectionState.Idle)
    val connectionState: StateFlow<WebRtcConnectionState> = _connectionState.asStateFlow()

    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack: StateFlow<VideoTrack?> = _localVideoTrack.asStateFlow()

    private val _remoteVideoTracks = MutableStateFlow<Map<FamilyMemberId, VideoTrack>>(emptyMap())
    val remoteVideoTracks: StateFlow<Map<FamilyMemberId, VideoTrack>> = _remoteVideoTracks.asStateFlow()

    private val _connectedPeers = MutableStateFlow<Set<FamilyMemberId>>(emptySet())
    val connectedPeers: StateFlow<Set<FamilyMemberId>> = _connectedPeers.asStateFlow()

    private val _isMicEnabled = MutableStateFlow(true)
    val isMicEnabled: StateFlow<Boolean> = _isMicEnabled.asStateFlow()

    private val _isCameraEnabled = MutableStateFlow(true)
    val isCameraEnabled: StateFlow<Boolean> = _isCameraEnabled.asStateFlow()

    private val _roomFullEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val roomFullEvents: SharedFlow<String> = _roomFullEvents.asSharedFlow()

    fun isSignalingServerConfigured(): Boolean = signalingClient.isConfigured()

    /**
     * Initializes local camera/mic capture, fetches STUN/TURN credentials, connects to the
     * Node.js Socket.io signaling server, and joins [roomId] (enforcing max 3 participants).
     */
    suspend fun joinRoom(
        member: FamilyMemberId,
        roomId: String = DEFAULT_ROOM_ID,
        pin: String? = null,
        enableMic: Boolean = true,
        enableCamera: Boolean = true,
        audioRoute: AudioRoute = AudioRoute.SPEAKER
    ): Result<Boolean> = lifecycleMutex.withLock {
        val gen = sessionGeneration.incrementAndGet()
        teardownInternal(notifyServer = false)

        localMember = member
        activeRoomId = roomId.ifBlank { DEFAULT_ROOM_ID }
        activePin = pin
        _isMicEnabled.value = enableMic
        _isCameraEnabled.value = enableCamera
        _connectionState.value = WebRtcConnectionState.Connecting

        mediaManager.configureCommunicationAudio(audioRoute)

        return@withLock runCatching {
            // 1. Initialize hardware PeerConnectionFactory + local camera & microphone streams
            mediaManager.ensureFactory()
            mediaManager.ensureLocalAudio(member = member, enabled = enableMic)
            mediaManager.ensureLocalVideo(member = member, enabled = enableCamera, resetFrontCamera = true)
            _localVideoTrack.value = if (enableCamera) mediaManager.localVideoTrack else null

            // 2. Fetch short-TTL TURN credentials (or fall back gracefully to STUN / static TURN)
            cachedIceServers = turnProvider.getIceServers(
                identity = member.name,
                roomName = activeRoomId,
                forceRefresh = false
            )
            if (sessionGeneration.get() != gen) return@runCatching false

            // 3. Connect to Socket.io signaling server and start collecting SDP/ICE/room events
            startEventCollector(gen)
            val connected = signalingClient.connect(member = member, pin = pin)
            if (!connected || sessionGeneration.get() != gen) {
                _connectionState.value = WebRtcConnectionState.Error("Could not reach signaling server")
                return@runCatching false
            }

            // 4. Join room (server rejects unauthorized users or 4th participant with room-full)
            val joined = signalingClient.joinRoom(
                roomId = activeRoomId,
                cameraEnabled = enableCamera && mediaManager.localVideoTrack != null,
                micEnabled = enableMic && mediaManager.localAudioTrack != null
            )
            if (!joined || sessionGeneration.get() != gen) {
                return@runCatching false
            }

            // 5. Keep liveness fresh with periodic signaling heartbeats
            startHeartbeatLoop(gen)
            publishState()
            true
        }.onFailure { err ->
            Log.w(TAG, "joinRoom failed", err)
            if (sessionGeneration.get() == gen) {
                _connectionState.value = WebRtcConnectionState.Error(err.message ?: "WebRTC join failed")
            }
        }
    }

    /**
     * Toggles local microphone capture on/off and notifies all peers in the room.
     */
    suspend fun setMicEnabled(enabled: Boolean): Boolean = lifecycleMutex.withLock {
        _isMicEnabled.value = enabled
        if (enabled && !mediaManager.hasAudioPermission()) return@withLock false
        val created = if (enabled) mediaManager.ensureLocalAudio(localMember, true) else false
        if (created) {
            attachLocalAudioToAllPeers(sessionGeneration.get())
        } else {
            mediaManager.setLocalAudioEnabled(enabled)
        }
        signalingClient.sendMediaState(
            cameraEnabled = _isCameraEnabled.value && mediaManager.localVideoTrack != null,
            micEnabled = enabled && mediaManager.localAudioTrack != null
        )
        true
    }

    /**
     * Toggles local camera capture on/off, attaches the track if newly started, and notifies peers.
     */
    suspend fun setCameraEnabled(enabled: Boolean): Boolean = lifecycleMutex.withLock {
        _isCameraEnabled.value = enabled
        if (enabled && !mediaManager.hasCameraPermission()) return@withLock false
        val created = if (enabled) mediaManager.ensureLocalVideo(localMember, true) else false
        if (created) {
            attachLocalVideoToAllPeers(sessionGeneration.get())
        } else {
            mediaManager.setLocalVideoEnabled(enabled)
        }
        _localVideoTrack.value = if (enabled) mediaManager.localVideoTrack else null
        signalingClient.sendMediaState(
            cameraEnabled = enabled && mediaManager.localVideoTrack != null,
            micEnabled = _isMicEnabled.value && mediaManager.localAudioTrack != null
        )
        true
    }

    suspend fun switchCamera(): Boolean = lifecycleMutex.withLock {
        mediaManager.switchCamera()
    }

    fun setAudioRoute(route: AudioRoute) {
        mediaManager.configureCommunicationAudio(route)
    }

    /**
     * Call on Wi-Fi <-> Cellular transport switches to refresh TURN credentials,
     * reconnect signaling, and perform ICE restarts across all active mesh peers.
     */
    fun onNetworkChanged() {
        val gen = sessionGeneration.get()
        if (_connectionState.value is WebRtcConnectionState.Idle) return
        val now = System.currentTimeMillis()
        if (now - lastNetworkSwitchAtMs < NETWORK_SWITCH_DEBOUNCE_MS) return
        lastNetworkSwitchAtMs = now

        scope.launch {
            if (sessionGeneration.get() != gen) return@launch
            _connectionState.value = WebRtcConnectionState.Reconnecting("Switching network…")

            cachedIceServers = turnProvider.getIceServers(
                identity = localMember.name,
                roomName = activeRoomId,
                forceRefresh = true
            )

            val reconnected = signalingClient.reconnectAfterNetworkSwitch(localMember, activePin)
            if (reconnected && sessionGeneration.get() == gen) {
                signalingClient.joinRoom(
                    roomId = activeRoomId,
                    cameraEnabled = _isCameraEnabled.value && mediaManager.localVideoTrack != null,
                    micEnabled = _isMicEnabled.value && mediaManager.localAudioTrack != null
                )
                peers.values.toList().forEach { peer ->
                    peer.iceRestartAttempts = 0
                    triggerIceRestart(peer, gen, reason = "network_switch")
                }
            }
        }
    }

    fun leaveRoom() {
        val gen = sessionGeneration.incrementAndGet()
        _localVideoTrack.value = null
        _remoteVideoTracks.value = emptyMap()
        _connectedPeers.value = emptySet()
        _connectionState.value = WebRtcConnectionState.Idle

        scope.launch {
            lifecycleMutex.withLock {
                if (sessionGeneration.get() == gen) {
                    teardownInternal(notifyServer = true)
                    mediaManager.restoreNormalAudio()
                }
            }
        }
    }

    fun release() {
        leaveRoom()
        scope.cancel()
    }

    // -------------------------------------------------------------------------
    // Signaling Event Handling
    // -------------------------------------------------------------------------

    private fun startEventCollector(gen: Long) {
        eventCollectorJob?.cancel()
        eventCollectorJob = scope.launch {
            signalingClient.events.collect { event ->
                if (sessionGeneration.get() != gen) return@collect
                when (event) {
                    is SocketIoSignalEvent.RoomPeers -> {
                        if (event.roomId != activeRoomId) return@collect
                        event.peers.forEach { info ->
                            if (info.memberId != localMember) {
                                val peer = getOrCreatePeer(info.memberId, info.socketId, gen) ?: return@forEach
                                peer.remoteCameraEnabled = info.cameraEnabled
                                peer.remoteMicEnabled = info.micEnabled
                                if (isOffererFor(info.memberId)) {
                                    createAndSendOffer(peer, iceRestart = false, gen = gen)
                                }
                            }
                        }
                        publishState()
                    }
                    is SocketIoSignalEvent.PeerJoined -> {
                        if (event.roomId != activeRoomId || event.peer.memberId == localMember) return@collect
                        val peer = getOrCreatePeer(event.peer.memberId, event.peer.socketId, gen) ?: return@collect
                        peer.remoteCameraEnabled = event.peer.cameraEnabled
                        peer.remoteMicEnabled = event.peer.micEnabled
                        if (isOffererFor(event.peer.memberId)) {
                            createAndSendOffer(peer, iceRestart = false, gen = gen)
                        }
                        publishState()
                    }
                    is SocketIoSignalEvent.PeerLeft -> {
                        if (event.roomId != activeRoomId) return@collect
                        closePeer(event.memberId)
                        publishState()
                    }
                    is SocketIoSignalEvent.RoomFull -> {
                        _roomFullEvents.tryEmit(event.message)
                        _connectionState.value = WebRtcConnectionState.Error(event.message)
                        leaveRoom()
                    }
                    is SocketIoSignalEvent.SessionReplaced -> {
                        _connectionState.value = WebRtcConnectionState.Error(event.message)
                        leaveRoom()
                    }
                    is SocketIoSignalEvent.RemoteOffer -> {
                        if (event.roomId != activeRoomId) return@collect
                        handleRemoteOffer(event, gen)
                    }
                    is SocketIoSignalEvent.RemoteAnswer -> {
                        if (event.roomId != activeRoomId) return@collect
                        handleRemoteAnswer(event, gen)
                    }
                    is SocketIoSignalEvent.RemoteIceCandidate -> {
                        if (event.roomId != activeRoomId) return@collect
                        handleRemoteIceCandidate(event, gen)
                    }
                    is SocketIoSignalEvent.IceRestartRequested -> {
                        if (event.roomId != activeRoomId) return@collect
                        peers[event.fromMemberId]?.let { peer ->
                            createAndSendOffer(peer, iceRestart = true, gen = gen)
                        }
                    }
                    is SocketIoSignalEvent.RemoteMediaState -> {
                        if (event.roomId != activeRoomId) return@collect
                        peers[event.memberId]?.let { peer ->
                            peer.remoteCameraEnabled = event.cameraEnabled
                            peer.remoteMicEnabled = event.micEnabled
                            publishState()
                        }
                    }
                }
            }
        }
    }

    private fun startHeartbeatLoop(gen: Long) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && sessionGeneration.get() == gen) {
                delay(HEARTBEAT_INTERVAL_MS)
                if (sessionGeneration.get() != gen) break
                signalingClient.sendHeartbeat()
            }
        }
    }

    // -------------------------------------------------------------------------
    // RTCPeerConnection Creation & Media Track Attachment
    // -------------------------------------------------------------------------

    private fun isOffererFor(remote: FamilyMemberId): Boolean = localMember.name < remote.name
    private fun isPoliteFor(remote: FamilyMemberId): Boolean = localMember.name > remote.name

    private fun getOrCreatePeer(
        remoteMember: FamilyMemberId,
        remoteSocketId: String,
        gen: Long,
        forceRecreate: Boolean = false
    ): MeshPeer? {
        // Strict 3-way mesh cap: local + at most 2 remote peers
        if (!peers.containsKey(remoteMember) && peers.size >= (MAX_PARTICIPANTS - 1)) {
            Log.w(TAG, "Ignoring peer $remoteMember: room already at max $MAX_PARTICIPANTS participants")
            return null
        }

        if (!forceRecreate) {
            peers[remoteMember]?.let { existing ->
                val state = existing.peerConnection.connectionState()
                val socketChanged = remoteSocketId.isNotBlank() &&
                    existing.remoteSocketId.isNotBlank() &&
                    existing.remoteSocketId != remoteSocketId
                if (!socketChanged &&
                    state != PeerConnection.PeerConnectionState.CLOSED &&
                    state != PeerConnection.PeerConnectionState.FAILED
                ) {
                    if (remoteSocketId.isNotBlank()) existing.remoteSocketId = remoteSocketId
                    return existing
                }
                closePeer(remoteMember)
            }
        } else {
            closePeer(remoteMember)
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

        var peerRef: MeshPeer? = null
        val observer = object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) = Unit
            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) = Unit
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
            override fun onRemoveStream(stream: MediaStream?) = Unit
            override fun onDataChannel(dc: DataChannel?) = Unit
            override fun onRenegotiationNeeded() = Unit

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                val peer = peerRef ?: return
                Log.d(TAG, "ICE[$remoteMember] -> $newState")
                when (newState) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED -> {
                        peer.isConnected = true
                        peer.iceRestartAttempts = 0
                        peer.fullRenegotiations = 0
                        peer.recoveryJob?.cancel()
                        peer.recoveryJob = null
                        mediaManager.configureCommunicationAudio(mediaManager.currentAudioRoute)
                        publishState()
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED -> {
                        _connectionState.value = WebRtcConnectionState.Reconnecting("Reconnecting ${remoteMember.displayName}…")
                        schedulePeerRecovery(peer, gen, immediate = false)
                    }
                    PeerConnection.IceConnectionState.FAILED -> {
                        peer.isConnected = false
                        _connectionState.value = WebRtcConnectionState.Reconnecting("Reconnecting ${remoteMember.displayName}…")
                        publishState()
                        schedulePeerRecovery(peer, gen, immediate = true)
                    }
                    PeerConnection.IceConnectionState.CLOSED -> {
                        peer.isConnected = false
                        publishState()
                    }
                    else -> Unit
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                val peer = peerRef ?: return
                if (newState == PeerConnection.PeerConnectionState.CONNECTED) {
                    peer.isConnected = true
                    publishState()
                } else if (newState == PeerConnection.PeerConnectionState.FAILED) {
                    peer.isConnected = false
                    publishState()
                    schedulePeerRecovery(peer, gen, immediate = true)
                }
            }

            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate == null || sessionGeneration.get() != gen) return
                val targetSocket = peerRef?.remoteSocketId?.takeIf { it.isNotBlank() }
                signalingClient.sendIceCandidate(
                    targetMember = remoteMember,
                    targetSocketId = targetSocket,
                    candidate = candidate
                )
            }

            override fun onAddStream(stream: MediaStream?) {
                val peer = peerRef ?: return
                stream?.videoTracks?.firstOrNull()?.let { track ->
                    track.setEnabled(true)
                    peer.remoteVideoTrack = track
                }
                stream?.audioTracks?.firstOrNull()?.let { track ->
                    track.setEnabled(true)
                    peer.remoteAudioTrack = track
                }
                publishState()
            }

            override fun onTrack(transceiver: RtpTransceiver?) {
                val peer = peerRef ?: return
                when (val track = transceiver?.receiver?.track()) {
                    is VideoTrack -> {
                        track.setEnabled(true)
                        peer.remoteVideoTrack = track
                        publishState()
                    }
                    is AudioTrack -> {
                        track.setEnabled(true)
                        peer.remoteAudioTrack = track
                        mediaManager.configureCommunicationAudio(mediaManager.currentAudioRoute)
                        publishState()
                    }
                }
            }

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                val peer = peerRef ?: return
                when (val track = receiver?.track()) {
                    is VideoTrack -> {
                        track.setEnabled(true)
                        peer.remoteVideoTrack = track
                        publishState()
                    }
                    is AudioTrack -> {
                        track.setEnabled(true)
                        peer.remoteAudioTrack = track
                        publishState()
                    }
                }
            }
        }

        val pc = factory.createPeerConnection(rtcConfig, observer) ?: return null
        val peer = MeshPeer(remoteMember, pc, remoteSocketId)
        peerRef = peer

        // Attach local microphone and camera tracks to this PeerConnection
        val streamIds = listOf("holymoly_stream_${localMember.name}")
        mediaManager.localAudioTrack?.let { audio ->
            peer.audioSender = runCatching { pc.addTrack(audio, streamIds) }.getOrNull()
        }
        mediaManager.localVideoTrack?.let { video ->
            peer.videoSender = runCatching { pc.addTrack(video, streamIds) }.getOrNull()
        }

        peers[remoteMember] = peer
        return peer
    }

    private fun attachLocalAudioToAllPeers(gen: Long) {
        val track = mediaManager.localAudioTrack ?: return
        val streamIds = listOf("holymoly_stream_${localMember.name}")
        peers.values.forEach { peer ->
            if (peer.audioSender == null) {
                peer.audioSender = runCatching { peer.peerConnection.addTrack(track, streamIds) }.getOrNull()
                scope.launch { createAndSendOffer(peer, iceRestart = false, gen = gen) }
            }
        }
    }

    private fun attachLocalVideoToAllPeers(gen: Long) {
        val track = mediaManager.localVideoTrack ?: return
        val streamIds = listOf("holymoly_stream_${localMember.name}")
        peers.values.forEach { peer ->
            if (peer.videoSender == null) {
                peer.videoSender = runCatching { peer.peerConnection.addTrack(track, streamIds) }.getOrNull()
                scope.launch { createAndSendOffer(peer, iceRestart = false, gen = gen) }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Perfect Negotiation SDP & ICE Exchange
    // -------------------------------------------------------------------------

    private suspend fun createAndSendOffer(peer: MeshPeer, iceRestart: Boolean, gen: Long) {
        peer.negotiationMutex.withLock {
            if (sessionGeneration.get() != gen || peer.isMakingOffer) return
            if (!iceRestart && peer.peerConnection.signalingState() != PeerConnection.SignalingState.STABLE) return

            peer.isMakingOffer = true
            try {
                val constraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                    if (iceRestart) {
                        mandatory.add(MediaConstraints.KeyValuePair("IceRestart", "true"))
                    }
                }
                val offer = createOfferAsync(peer.peerConnection, constraints) ?: return
                if (sessionGeneration.get() != gen) return
                val setOk = setLocalDescriptionAsync(peer.peerConnection, offer)
                if (!setOk || sessionGeneration.get() != gen) return

                signalingClient.sendOffer(
                    targetMember = peer.memberId,
                    targetSocketId = peer.remoteSocketId.takeIf { it.isNotBlank() },
                    sdp = offer,
                    iceRestart = iceRestart
                )
            } finally {
                peer.isMakingOffer = false
            }
        }
    }

    private suspend fun handleRemoteOffer(event: SocketIoSignalEvent.RemoteOffer, gen: Long) {
        val peer = getOrCreatePeer(event.fromMemberId, event.fromSocketId, gen) ?: return
        peer.negotiationMutex.withLock {
            if (sessionGeneration.get() != gen) return
            val polite = isPoliteFor(event.fromMemberId)
            val offerCollision = peer.isMakingOffer ||
                peer.peerConnection.signalingState() != PeerConnection.SignalingState.STABLE

            peer.ignoreOffer = !polite && offerCollision
            if (peer.ignoreOffer) {
                Log.d(TAG, "Perfect Negotiation: impolite peer ignoring colliding offer from ${event.fromMemberId}")
                return
            }

            if (offerCollision && polite) {
                val rollbackOk = setLocalDescriptionAsync(
                    peer.peerConnection,
                    SessionDescription(SessionDescription.Type.ROLLBACK, "")
                )
                if (!rollbackOk) return
            }

            if (!setRemoteDescriptionAsync(peer.peerConnection, event.sdp)) return
            peer.remoteDescriptionSet = true
            flushPendingIce(peer)

            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            }
            val answer = createAnswerAsync(peer.peerConnection, constraints) ?: return
            if (!setLocalDescriptionAsync(peer.peerConnection, answer) || sessionGeneration.get() != gen) return

            signalingClient.sendAnswer(
                targetMember = event.fromMemberId,
                targetSocketId = peer.remoteSocketId.takeIf { it.isNotBlank() },
                sdp = answer
            )
        }
        publishState()
    }

    private suspend fun handleRemoteAnswer(event: SocketIoSignalEvent.RemoteAnswer, gen: Long) {
        val peer = peers[event.fromMemberId] ?: return
        peer.negotiationMutex.withLock {
            if (sessionGeneration.get() != gen) return
            if (peer.peerConnection.signalingState() != PeerConnection.SignalingState.HAVE_LOCAL_OFFER) return
            if (setRemoteDescriptionAsync(peer.peerConnection, event.sdp)) {
                peer.remoteDescriptionSet = true
                flushPendingIce(peer)
            }
        }
        publishState()
    }

    private suspend fun handleRemoteIceCandidate(event: SocketIoSignalEvent.RemoteIceCandidate, gen: Long) {
        val peer = getOrCreatePeer(event.fromMemberId, event.fromSocketId, gen) ?: return
        peer.negotiationMutex.withLock {
            if (peer.ignoreOffer) return
            if (peer.remoteDescriptionSet) {
                runCatching { peer.peerConnection.addIceCandidate(event.candidate) }
            } else {
                peer.pendingRemoteIce.add(event.candidate)
            }
        }
    }

    private fun flushPendingIce(peer: MeshPeer) {
        if (peer.pendingRemoteIce.isEmpty()) return
        val queued = peer.pendingRemoteIce.toList()
        peer.pendingRemoteIce.clear()
        queued.forEach { candidate ->
            runCatching { peer.peerConnection.addIceCandidate(candidate) }
        }
    }

    // -------------------------------------------------------------------------
    // Escalating Stability Recovery (ICE Restart -> Full Renegotiation -> Drop)
    // -------------------------------------------------------------------------

    private fun schedulePeerRecovery(peer: MeshPeer, gen: Long, immediate: Boolean) {
        peer.recoveryJob?.cancel()
        peer.recoveryJob = scope.launch {
            if (!immediate) delay(ICE_DISCONNECTED_GRACE_MS)
            if (sessionGeneration.get() != gen || peer.isConnected || peers[peer.memberId] !== peer) return@launch

            if (peer.iceRestartAttempts < MAX_ICE_RESTART_ATTEMPTS) {
                peer.iceRestartAttempts++
                triggerIceRestart(peer, gen, reason = "ice_recovery_${peer.iceRestartAttempts}")
                delay(4_500L)
                if (sessionGeneration.get() == gen && !peer.isConnected && peers[peer.memberId] === peer) {
                    schedulePeerRecovery(peer, gen, immediate = true)
                }
            } else if (peer.fullRenegotiations < MAX_FULL_RENEGOTIATIONS) {
                peer.fullRenegotiations++
                cachedIceServers = turnProvider.getIceServers(
                    identity = localMember.name,
                    roomName = activeRoomId,
                    forceRefresh = true
                )
                val fresh = getOrCreatePeer(peer.memberId, peer.remoteSocketId, gen, forceRecreate = true) ?: return@launch
                fresh.fullRenegotiations = 1
                if (isOffererFor(peer.memberId)) {
                    createAndSendOffer(fresh, iceRestart = false, gen = gen)
                } else {
                    signalingClient.sendIceRestartRequest(
                        targetMember = peer.memberId,
                        targetSocketId = fresh.remoteSocketId.takeIf { it.isNotBlank() },
                        reason = "full_renegotiation"
                    )
                }
            } else {
                closePeer(peer.memberId)
                publishState()
            }
        }
    }

    private suspend fun triggerIceRestart(peer: MeshPeer, gen: Long, reason: String) {
        if (sessionGeneration.get() != gen) return
        if (isOffererFor(peer.memberId)) {
            createAndSendOffer(peer, iceRestart = true, gen = gen)
        } else {
            signalingClient.sendIceRestartRequest(
                targetMember = peer.memberId,
                targetSocketId = peer.remoteSocketId.takeIf { it.isNotBlank() },
                reason = reason
            )
            delay(800L)
            if (sessionGeneration.get() == gen &&
                !peer.isConnected &&
                peers[peer.memberId] === peer &&
                peer.peerConnection.signalingState() == PeerConnection.SignalingState.STABLE
            ) {
                createAndSendOffer(peer, iceRestart = true, gen = gen)
            }
        }
    }

    private fun closePeer(memberId: FamilyMemberId) {
        val peer = peers.remove(memberId) ?: return
        peer.recoveryJob?.cancel()
        peer.recoveryJob = null
        peer.isConnected = false
        peer.remoteVideoTrack = null
        peer.remoteAudioTrack = null
        runCatching { peer.peerConnection.close() }
        runCatching { peer.peerConnection.dispose() }
    }

    private fun teardownInternal(notifyServer: Boolean) {
        eventCollectorJob?.cancel()
        eventCollectorJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null

        if (notifyServer) {
            signalingClient.leaveRoom()
        }
        signalingClient.disconnect()

        peers.keys.toList().forEach(::closePeer)
        peers.clear()
        mediaManager.releaseLocalMedia()
    }

    private fun publishState() {
        val livePeers = mutableSetOf<FamilyMemberId>()
        val tracks = mutableMapOf<FamilyMemberId, VideoTrack>()
        peers.values.forEach { peer ->
            if (peer.isConnected) {
                livePeers.add(peer.memberId)
                val video = peer.remoteVideoTrack
                if (video != null && peer.remoteCameraEnabled) {
                    tracks[peer.memberId] = video
                }
            }
        }
        _connectedPeers.value = livePeers
        _remoteVideoTracks.value = tracks
        _connectionState.value = WebRtcConnectionState.Connected(peerCount = livePeers.size + 1)
    }

    // -------------------------------------------------------------------------
    // Suspending WebRTC SdpObserver Helpers
    // -------------------------------------------------------------------------

    private suspend fun createOfferAsync(pc: PeerConnection, constraints: MediaConstraints): SessionDescription? {
        val deferred = CompletableDeferred<SessionDescription?>()
        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) { deferred.complete(sdp) }
            override fun onCreateFailure(error: String?) { deferred.complete(null) }
            override fun onSetSuccess() = Unit
            override fun onSetFailure(error: String?) = Unit
        }, constraints)
        return deferred.await()
    }

    private suspend fun createAnswerAsync(pc: PeerConnection, constraints: MediaConstraints): SessionDescription? {
        val deferred = CompletableDeferred<SessionDescription?>()
        pc.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) { deferred.complete(sdp) }
            override fun onCreateFailure(error: String?) { deferred.complete(null) }
            override fun onSetSuccess() = Unit
            override fun onSetFailure(error: String?) = Unit
        }, constraints)
        return deferred.await()
    }

    private suspend fun setLocalDescriptionAsync(pc: PeerConnection, sdp: SessionDescription): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        pc.setLocalDescription(object : SdpObserver {
            override fun onSetSuccess() { deferred.complete(true) }
            override fun onSetFailure(error: String?) { deferred.complete(false) }
            override fun onCreateSuccess(sdp: SessionDescription?) = Unit
            override fun onCreateFailure(error: String?) = Unit
        }, sdp)
        return deferred.await()
    }

    private suspend fun setRemoteDescriptionAsync(pc: PeerConnection, sdp: SessionDescription): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        pc.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() { deferred.complete(true) }
            override fun onSetFailure(error: String?) { deferred.complete(false) }
            override fun onCreateSuccess(sdp: SessionDescription?) = Unit
            override fun onCreateFailure(error: String?) = Unit
        }, sdp)
        return deferred.await()
    }

    companion object {
        private const val TAG = "WebRtcManager"
        const val DEFAULT_ROOM_ID = "holymoly-family-room"
        const val MAX_PARTICIPANTS = 3

        private const val HEARTBEAT_INTERVAL_MS = 5_000L
        private const val ICE_DISCONNECTED_GRACE_MS = 2_500L
        private const val NETWORK_SWITCH_DEBOUNCE_MS = 1_500L
        private const val MAX_ICE_RESTART_ATTEMPTS = 2
        private const val MAX_FULL_RENEGOTIATIONS = 1
    }
}
