package com.example.media

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.FamilyMemberId
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

data class SignalingPeerInfo(
    val socketId: String,
    val memberId: FamilyMemberId,
    val cameraEnabled: Boolean = true,
    val micEnabled: Boolean = true,
    val shouldInitiateOffer: Boolean = false,
    val polite: Boolean = false
)

sealed interface SocketIoSignalEvent {
    data class RoomPeers(
        val roomId: String,
        val peers: List<SignalingPeerInfo>
    ) : SocketIoSignalEvent

    data class PeerJoined(
        val roomId: String,
        val peer: SignalingPeerInfo
    ) : SocketIoSignalEvent

    data class PeerLeft(
        val roomId: String,
        val socketId: String,
        val memberId: FamilyMemberId,
        val replaced: Boolean = false
    ) : SocketIoSignalEvent

    data class RoomFull(
        val roomId: String,
        val message: String
    ) : SocketIoSignalEvent

    data class RemoteOffer(
        val roomId: String,
        val fromSocketId: String,
        val fromMemberId: FamilyMemberId,
        val sdp: SessionDescription,
        val iceRestart: Boolean = false
    ) : SocketIoSignalEvent

    data class RemoteAnswer(
        val roomId: String,
        val fromSocketId: String,
        val fromMemberId: FamilyMemberId,
        val sdp: SessionDescription
    ) : SocketIoSignalEvent

    data class RemoteIceCandidate(
        val roomId: String,
        val fromSocketId: String,
        val fromMemberId: FamilyMemberId,
        val candidate: IceCandidate
    ) : SocketIoSignalEvent

    data class IceRestartRequested(
        val roomId: String,
        val fromSocketId: String,
        val fromMemberId: FamilyMemberId,
        val reason: String
    ) : SocketIoSignalEvent

    data class RemoteMediaState(
        val roomId: String,
        val memberId: FamilyMemberId,
        val cameraEnabled: Boolean,
        val micEnabled: Boolean
    ) : SocketIoSignalEvent

    data class SessionReplaced(
        val roomId: String,
        val message: String
    ) : SocketIoSignalEvent
}

/**
 * Android Socket.io signaling client matching the Node.js server in `signaling-server/`.
 *
 * Authenticates via handshake `auth: { memberId, token }` (using `WEBRTC_SIGNALING_URL` and
 * `WEBRTC_SIGNALING_SECRET` from AI Studio Secrets / BuildConfig) and exchanges room, SDP,
 * ICE candidate, ICE restart, heartbeat, and media-state events for 3-way family mesh calls.
 */
class SocketIoSignalingClient(
    private val serverUrlProvider: () -> String = { BuildConfig.WEBRTC_SIGNALING_URL.trim() },
    private val sharedSecretProvider: () -> String = { BuildConfig.WEBRTC_SIGNALING_SECRET.trim() }
) {
    @Volatile
    private var socket: Socket? = null

    @Volatile
    private var connectedMember: FamilyMemberId? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _events = MutableSharedFlow<SocketIoSignalEvent>(extraBufferCapacity = 128)
    val events: SharedFlow<SocketIoSignalEvent> = _events.asSharedFlow()

    fun isConfigured(): Boolean {
        val url = serverUrlProvider()
        return (url.startsWith("https://") || url.startsWith("http://")) &&
            !url.contains("your-signaling-server")
    }

    /**
     * Opens an authenticated Socket.io connection to the signaling server.
     */
    suspend fun connect(
        member: FamilyMemberId,
        pin: String? = null,
        timeoutMs: Long = 8_000L
    ): Boolean {
        val url = serverUrlProvider()
        if (!isConfigured()) {
            Log.w(TAG, "WEBRTC_SIGNALING_URL is not configured")
            return false
        }

        // Reuse live socket if already connected for the same authorized family member
        val existing = socket
        if (existing != null && existing.connected() && connectedMember == member) {
            return true
        }

        disconnect()
        connectedMember = member

        val secret = sharedSecretProvider().takeUnless { it.contains("your-signaling-shared-secret") }.orEmpty()
        val authMap = buildMap<String, String> {
            put("memberId", member.name)
            if (secret.isNotBlank()) put("token", secret)
            if (!pin.isNullOrBlank()) put("pin", pin.trim())
        }

        val options = IO.Options.builder()
            .setTransports(arrayOf("websocket"))
            .setReconnection(true)
            .setReconnectionAttempts(Int.MAX_VALUE)
            .setReconnectionDelay(1_000L)
            .setReconnectionDelayMax(5_000L)
            .setTimeout(timeoutMs)
            .setAuth(authMap)
            .build()

        val client = runCatching { IO.socket(url, options) }.getOrElse { err ->
            Log.w(TAG, "Failed to initialize Socket.io client: ${err.message}")
            return false
        }
        socket = client

        val connectedDeferred = CompletableDeferred<Boolean>()
        registerListeners(client, connectedDeferred)
        client.connect()

        return withTimeoutOrNull(timeoutMs) { connectedDeferred.await() } ?: false
    }

    suspend fun joinRoom(
        roomId: String,
        cameraEnabled: Boolean,
        micEnabled: Boolean,
        createIfMissing: Boolean = false,
        timeoutMs: Long = 5_000L
    ): Boolean {
        val active = socket ?: return false
        val ackDeferred = CompletableDeferred<Boolean>()
        val payload = JSONObject().apply {
            put("roomId", roomId)
            put("cameraEnabled", cameraEnabled)
            put("micEnabled", micEnabled)
        }
        val eventName = if (createIfMissing) "create-room" else "join-room"
        active.emit(eventName, arrayOf(payload), Ack { args ->
            val response = args.firstOrNull() as? JSONObject
            val ok = response?.optBoolean("ok", false) ?: false
            if (!ok) {
                val msg = response?.optString("message").orEmpty()
                Log.w(TAG, "join-room rejected for $roomId: $msg")
            }
            ackDeferred.complete(ok)
        })
        return withTimeoutOrNull(timeoutMs) { ackDeferred.await() } ?: false
    }

    fun leaveRoom() {
        socket?.emit("leave-room", JSONObject())
    }

    fun sendOffer(
        targetMember: FamilyMemberId,
        targetSocketId: String?,
        sdp: SessionDescription,
        iceRestart: Boolean = false
    ) {
        val payload = JSONObject().apply {
            put("targetMemberId", targetMember.name)
            if (!targetSocketId.isNullOrBlank()) put("targetSocketId", targetSocketId)
            put("iceRestart", iceRestart)
            put(
                "sdp",
                JSONObject().apply {
                    put("type", "offer")
                    put("sdp", sdp.description)
                }
            )
        }
        socket?.emit("offer", payload)
    }

    fun sendAnswer(
        targetMember: FamilyMemberId,
        targetSocketId: String?,
        sdp: SessionDescription
    ) {
        val payload = JSONObject().apply {
            put("targetMemberId", targetMember.name)
            if (!targetSocketId.isNullOrBlank()) put("targetSocketId", targetSocketId)
            put(
                "sdp",
                JSONObject().apply {
                    put("type", "answer")
                    put("sdp", sdp.description)
                }
            )
        }
        socket?.emit("answer", payload)
    }

    fun sendIceCandidate(
        targetMember: FamilyMemberId,
        targetSocketId: String?,
        candidate: IceCandidate
    ) {
        val payload = JSONObject().apply {
            put("targetMemberId", targetMember.name)
            if (!targetSocketId.isNullOrBlank()) put("targetSocketId", targetSocketId)
            put(
                "candidate",
                JSONObject().apply {
                    put("candidate", candidate.sdp)
                    put("sdpMid", candidate.sdpMid ?: "")
                    put("sdpMLineIndex", candidate.sdpMLineIndex)
                }
            )
        }
        socket?.emit("ice-candidate", payload)
    }

    fun sendIceRestartRequest(
        targetMember: FamilyMemberId,
        targetSocketId: String?,
        reason: String
    ) {
        val payload = JSONObject().apply {
            put("targetMemberId", targetMember.name)
            if (!targetSocketId.isNullOrBlank()) put("targetSocketId", targetSocketId)
            put("reason", reason)
        }
        socket?.emit("ice-restart-request", payload)
    }

    fun sendMediaState(cameraEnabled: Boolean, micEnabled: Boolean) {
        val payload = JSONObject().apply {
            put("cameraEnabled", cameraEnabled)
            put("micEnabled", micEnabled)
        }
        socket?.emit("media-state", payload)
    }

    fun sendHeartbeat() {
        socket?.emit("heartbeat", JSONObject())
    }

    suspend fun reconnectAfterNetworkSwitch(member: FamilyMemberId, pin: String? = null): Boolean {
        disconnect()
        return connect(member = member, pin = pin)
    }

    fun disconnect() {
        val active = socket
        socket = null
        connectedMember = null
        _isConnected.value = false
        if (active != null) {
            runCatching {
                active.off()
                active.disconnect()
                active.close()
            }
        }
    }

    private fun registerListeners(client: Socket, connectedDeferred: CompletableDeferred<Boolean>) {
        client.on(Socket.EVENT_CONNECT) {
            _isConnected.value = true
            if (!connectedDeferred.isCompleted) connectedDeferred.complete(true)
        }

        client.on(Socket.EVENT_CONNECT_ERROR) { args ->
            _isConnected.value = false
            val err = args.firstOrNull()?.toString() ?: "Unknown connection error"
            Log.w(TAG, "Socket.io connect error: $err")
            if (!connectedDeferred.isCompleted) connectedDeferred.complete(false)
        }

        client.on(Socket.EVENT_DISCONNECT) {
            _isConnected.value = false
        }

        client.on("room-peers") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            val roomId = obj.optString("roomId")
            val peers = parsePeerArray(obj.optJSONArray("peers"))
            _events.tryEmit(SocketIoSignalEvent.RoomPeers(roomId = roomId, peers = peers))
        }

        client.on("peer-joined") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            val roomId = obj.optString("roomId")
            val peerObj = obj.optJSONObject("peer") ?: return@on
            val peer = parsePeer(peerObj) ?: return@on
            _events.tryEmit(SocketIoSignalEvent.PeerJoined(roomId = roomId, peer = peer))
        }

        client.on("peer-left") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            val roomId = obj.optString("roomId")
            val member = parseMemberId(obj.optString("memberId")) ?: return@on
            _events.tryEmit(
                SocketIoSignalEvent.PeerLeft(
                    roomId = roomId,
                    socketId = obj.optString("socketId"),
                    memberId = member,
                    replaced = obj.optBoolean("replaced", false)
                )
            )
        }

        client.on("room-full") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            _events.tryEmit(
                SocketIoSignalEvent.RoomFull(
                    roomId = obj.optString("roomId"),
                    message = obj.optString("message", "Room is full (max 3 family members)")
                )
            )
        }

        client.on("session-replaced") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            _events.tryEmit(
                SocketIoSignalEvent.SessionReplaced(
                    roomId = obj.optString("roomId"),
                    message = obj.optString("message", "Session reconnected elsewhere")
                )
            )
        }

        client.on("offer") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            val fromMember = parseMemberId(obj.optString("fromMemberId")) ?: return@on
            val sdpObj = obj.optJSONObject("sdp") ?: return@on
            val sdpStr = sdpObj.optString("sdp")
            if (sdpStr.isBlank()) return@on
            _events.tryEmit(
                SocketIoSignalEvent.RemoteOffer(
                    roomId = obj.optString("roomId"),
                    fromSocketId = obj.optString("fromSocketId"),
                    fromMemberId = fromMember,
                    sdp = SessionDescription(SessionDescription.Type.OFFER, sdpStr),
                    iceRestart = obj.optBoolean("iceRestart", false)
                )
            )
        }

        client.on("answer") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            val fromMember = parseMemberId(obj.optString("fromMemberId")) ?: return@on
            val sdpObj = obj.optJSONObject("sdp") ?: return@on
            val sdpStr = sdpObj.optString("sdp")
            if (sdpStr.isBlank()) return@on
            _events.tryEmit(
                SocketIoSignalEvent.RemoteAnswer(
                    roomId = obj.optString("roomId"),
                    fromSocketId = obj.optString("fromSocketId"),
                    fromMemberId = fromMember,
                    sdp = SessionDescription(SessionDescription.Type.ANSWER, sdpStr)
                )
            )
        }

        client.on("ice-candidate") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            val fromMember = parseMemberId(obj.optString("fromMemberId")) ?: return@on
            val candObj = obj.optJSONObject("candidate") ?: return@on
            val sdp = candObj.optString("candidate")
            if (sdp.isBlank()) return@on
            val ice = IceCandidate(
                candObj.optString("sdpMid", ""),
                candObj.optInt("sdpMLineIndex", 0),
                sdp
            )
            _events.tryEmit(
                SocketIoSignalEvent.RemoteIceCandidate(
                    roomId = obj.optString("roomId"),
                    fromSocketId = obj.optString("fromSocketId"),
                    fromMemberId = fromMember,
                    candidate = ice
                )
            )
        }

        client.on("ice-restart-request") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            val fromMember = parseMemberId(obj.optString("fromMemberId")) ?: return@on
            _events.tryEmit(
                SocketIoSignalEvent.IceRestartRequested(
                    roomId = obj.optString("roomId"),
                    fromSocketId = obj.optString("fromSocketId"),
                    fromMemberId = fromMember,
                    reason = obj.optString("reason", "network_change")
                )
            )
        }

        client.on("media-state") { args ->
            val obj = args.firstOrNull() as? JSONObject ?: return@on
            val member = parseMemberId(obj.optString("memberId")) ?: return@on
            _events.tryEmit(
                SocketIoSignalEvent.RemoteMediaState(
                    roomId = obj.optString("roomId"),
                    memberId = member,
                    cameraEnabled = obj.optBoolean("cameraEnabled", true),
                    micEnabled = obj.optBoolean("micEnabled", true)
                )
            )
        }
    }

    private fun parsePeerArray(array: JSONArray?): List<SignalingPeerInfo> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                parsePeer(item)?.let(::add)
            }
        }
    }

    private fun parsePeer(obj: JSONObject): SignalingPeerInfo? {
        val member = parseMemberId(obj.optString("memberId")) ?: return null
        return SignalingPeerInfo(
            socketId = obj.optString("socketId"),
            memberId = member,
            cameraEnabled = obj.optBoolean("cameraEnabled", true),
            micEnabled = obj.optBoolean("micEnabled", true),
            shouldInitiateOffer = obj.optBoolean("shouldInitiateOffer", false),
            polite = obj.optBoolean("polite", false)
        )
    }

    private fun parseMemberId(raw: String?): FamilyMemberId? {
        if (raw.isNullOrBlank()) return null
        return runCatching { FamilyMemberId.valueOf(raw.trim().uppercase()) }.getOrNull()
    }

    companion object {
        private const val TAG = "SocketIoSignaling"
    }
}
