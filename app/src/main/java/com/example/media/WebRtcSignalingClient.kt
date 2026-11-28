package com.example.media

import com.example.data.model.FamilyMemberId
import com.example.sync.SupabaseRealtimeSignaling
import com.example.sync.WebRtcSignalEnvelope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import java.util.UUID

/**
 * Modular WebRTC signaling client built on top of the app's existing Supabase Realtime
 * WebSocket channel (`webrtc_signal`).
 *
 * Responsibilities:
 * - Room-ID scoped SDP offer/answer, ICE candidate, media-state, and DataChannel fallback exchange
 * - Explicit max-3 participant room guard (`MAX_ROOM_PARTICIPANTS = 3`) with `ROOM_FULL` signaling
 * - Peer list broadcasting (`peerIdsCsv`) & periodic peer heartbeat signaling for dead-peer cleanup
 * - Instant WebSocket re-subscription after Wi-Fi <-> Cellular handoffs
 */
class WebRtcSignalingClient(
    private val realtimeSignaling: SupabaseRealtimeSignaling
) {
    @Volatile
    var currentSessionId: String = UUID.randomUUID().toString().take(12)
        private set

    fun isConfigured(): Boolean = realtimeSignaling.isConfigured()

    fun rotateSessionId(): String {
        val fresh = UUID.randomUUID().toString().take(12)
        currentSessionId = fresh
        return fresh
    }

    suspend fun ensureConnected(): Boolean = realtimeSignaling.ensureSubscribedForCall()

    suspend fun reconnectAfterNetworkSwitch(): Boolean = realtimeSignaling.reconnectForNetworkSwitch()

    /**
     * Returns a room-scoped flow of signaling envelopes targeted to [localMember] (or broadcast),
     * automatically ignoring echoes from this device's own [currentSessionId].
     */
    fun incomingForRoom(
        roomName: String,
        localMember: FamilyMemberId
    ): Flow<WebRtcSignalEnvelope> = realtimeSignaling.incomingWebRtcSignals.filter { envelope ->
        if (envelope.roomName != roomName) return@filter false
        // Ignore our own exact session echo; allow same memberId from a DIFFERENT sessionId
        // so a duplicate 4th device can be cleanly detected and rejected with ROOM_FULL.
        if (envelope.sessionId.isNotBlank() && envelope.sessionId == currentSessionId) {
            return@filter false
        }
        if (envelope.sessionId.isBlank() &&
            envelope.senderId.equals(localMember.name, ignoreCase = true)
        ) {
            return@filter false
        }
        envelope.targetId.isBlank() ||
            envelope.targetId.equals(localMember.name, ignoreCase = true) ||
            envelope.targetId == currentSessionId
    }

    fun sendJoin(
        roomName: String,
        sender: FamilyMemberId,
        cameraEnabled: Boolean,
        micEnabled: Boolean,
        activePeers: Set<FamilyMemberId>
    ) {
        realtimeSignaling.sendWebRtcSignal(
            WebRtcSignalEnvelope(
                type = TYPE_JOIN,
                roomName = roomName,
                senderId = sender.name,
                sessionId = currentSessionId,
                peerIdsCsv = activePeers.joinToString(",") { it.name },
                cameraEnabled = cameraEnabled,
                micEnabled = micEnabled,
                timestampMs = System.currentTimeMillis()
            )
        )
    }

    fun sendJoinAck(
        roomName: String,
        sender: FamilyMemberId,
        target: FamilyMemberId,
        cameraEnabled: Boolean,
        micEnabled: Boolean,
        activePeers: Set<FamilyMemberId>
    ) {
        realtimeSignaling.sendWebRtcSignal(
            WebRtcSignalEnvelope(
                type = TYPE_JOIN_ACK,
                roomName = roomName,
                senderId = sender.name,
                targetId = target.name,
                sessionId = currentSessionId,
                peerIdsCsv = activePeers.joinToString(",") { it.name },
                cameraEnabled = cameraEnabled,
                micEnabled = micEnabled,
                timestampMs = System.currentTimeMillis()
            )
        )
    }

    fun sendRoomFull(
        roomName: String,
        sender: FamilyMemberId,
        targetSessionOrId: String,
        activePeers: Set<FamilyMemberId>
    ) {
        realtimeSignaling.sendWebRtcSignal(
            WebRtcSignalEnvelope(
                type = TYPE_ROOM_FULL,
                roomName = roomName,
                senderId = sender.name,
                targetId = targetSessionOrId,
                sessionId = currentSessionId,
                peerIdsCsv = activePeers.joinToString(",") { it.name },
                reason = "Room is full (max $MAX_ROOM_PARTICIPANTS participants)",
                timestampMs = System.currentTimeMillis()
            )
        )
    }

    fun sendPeerHeartbeat(
        roomName: String,
        sender: FamilyMemberId,
        cameraEnabled: Boolean,
        micEnabled: Boolean,
        activePeers: Set<FamilyMemberId>
    ) {
        realtimeSignaling.sendWebRtcSignal(
            WebRtcSignalEnvelope(
                type = TYPE_PEER_HEARTBEAT,
                roomName = roomName,
                senderId = sender.name,
                sessionId = currentSessionId,
                peerIdsCsv = activePeers.joinToString(",") { it.name },
                cameraEnabled = cameraEnabled,
                micEnabled = micEnabled,
                timestampMs = System.currentTimeMillis()
            )
        )
    }

    fun sendIceRestartRequest(
        roomName: String,
        sender: FamilyMemberId,
        target: FamilyMemberId,
        reason: String
    ) {
        realtimeSignaling.sendWebRtcSignal(
            WebRtcSignalEnvelope(
                type = TYPE_ICE_RESTART_REQ,
                roomName = roomName,
                senderId = sender.name,
                targetId = target.name,
                sessionId = currentSessionId,
                reason = reason,
                timestampMs = System.currentTimeMillis()
            )
        )
    }

    fun sendOffer(
        roomName: String,
        sender: FamilyMemberId,
        target: FamilyMemberId,
        sdp: String,
        cameraEnabled: Boolean,
        micEnabled: Boolean
    ) {
        realtimeSignaling.sendWebRtcSignal(
            WebRtcSignalEnvelope(
                type = TYPE_OFFER,
                roomName = roomName,
                senderId = sender.name,
                targetId = target.name,
                sessionId = currentSessionId,
                sdp = sdp,
                cameraEnabled = cameraEnabled,
                micEnabled = micEnabled,
                timestampMs = System.currentTimeMillis()
            )
        )
    }

    fun sendAnswer(
        roomName: String,
        sender: FamilyMemberId,
        target: FamilyMemberId,
        sdp: String,
        cameraEnabled: Boolean,
        micEnabled: Boolean
    ) {
        realtimeSignaling.sendWebRtcSignal(
            WebRtcSignalEnvelope(
                type = TYPE_ANSWER,
                roomName = roomName,
                senderId = sender.name,
                targetId = target.name,
                sessionId = currentSessionId,
                sdp = sdp,
                cameraEnabled = cameraEnabled,
                micEnabled = micEnabled,
                timestampMs = System.currentTimeMillis()
            )
        )
    }

    fun sendIceCandidate(
        roomName: String,
        sender: FamilyMemberId,
        target: FamilyMemberId,
        sdpMid: String,
        sdpMLineIndex: Int,
        sdp: String
    ) {
        realtimeSignaling.sendWebRtcSignal(
            WebRtcSignalEnvelope(
                type = TYPE_ICE,
                roomName = roomName,
                senderId = sender.name,
                targetId = target.name,
                sessionId = currentSessionId,
                iceMid = sdpMid,
                iceMLineIndex = sdpMLineIndex,
                iceCandidate = sdp,
                timestampMs = System.currentTimeMillis()
            )
        )
    }

    fun sendMediaState(
        roomName: String,
        sender: FamilyMemberId,
        target: FamilyMemberId?,
        cameraEnabled: Boolean,
        micEnabled: Boolean
    ) {
        realtimeSignaling.sendWebRtcSignal(
            WebRtcSignalEnvelope(
                type = TYPE_MEDIA_STATE,
                roomName = roomName,
                senderId = sender.name,
                targetId = target?.name.orEmpty(),
                sessionId = currentSessionId,
                cameraEnabled = cameraEnabled,
                micEnabled = micEnabled,
                timestampMs = System.currentTimeMillis()
            )
        )
    }

    fun sendFallbackDataPacket(
        roomName: String,
        sender: FamilyMemberId,
        payloadJson: String
    ) {
        realtimeSignaling.sendWebRtcSignal(
            WebRtcSignalEnvelope(
                type = TYPE_DATA_PACKET,
                roomName = roomName,
                senderId = sender.name,
                sessionId = currentSessionId,
                dataPayload = payloadJson,
                timestampMs = System.currentTimeMillis()
            )
        )
    }

    fun sendLeave(roomName: String, sender: FamilyMemberId) {
        realtimeSignaling.sendWebRtcSignal(
            WebRtcSignalEnvelope(
                type = TYPE_LEAVE,
                roomName = roomName,
                senderId = sender.name,
                sessionId = currentSessionId,
                timestampMs = System.currentTimeMillis()
            )
        )
    }

    companion object {
        const val MAX_ROOM_PARTICIPANTS = 3

        const val TYPE_JOIN = "JOIN"
        const val TYPE_JOIN_ACK = "JOIN_ACK"
        const val TYPE_ROOM_FULL = "ROOM_FULL"
        const val TYPE_PEER_HEARTBEAT = "PEER_HEARTBEAT"
        const val TYPE_ICE_RESTART_REQ = "ICE_RESTART_REQ"
        const val TYPE_OFFER = "OFFER"
        const val TYPE_ANSWER = "ANSWER"
        const val TYPE_ICE = "ICE"
        const val TYPE_LEAVE = "LEAVE"
        const val TYPE_MEDIA_STATE = "MEDIA_STATE"
        const val TYPE_DATA_PACKET = "DATA_PACKET"
    }
}
