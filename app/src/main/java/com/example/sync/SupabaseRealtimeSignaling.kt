package com.example.sync

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.FamilyMemberId
import com.example.data.model.PresenceAvailability
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@Serializable
data class FamilyRealtimeSignal(
    val kind: String, // CALL_INVITE, CALL_DECLINE, CALL_END, CHAT_MESSAGE, HEARTBEAT, DAILY_COMIC, PRESENCE_*
    val senderId: String,
    val targetIdsCsv: String = "MITHUN,MAFITHA,ZAYN",
    val roomName: String = "holymoly-family-room",
    val isThreeWay: Boolean = true,
    val videoEnabled: Boolean = true,
    val threadId: String = "family_group",
    val messageId: String = "",
    val text: String = "",
    val messageType: String = "TEXT",
    val isSafetyAlert: Boolean = false,
    val dateKey: String = "",
    val comicStyle: String = "",
    val imageBase64: String = "",
    val mediaUrl: String = "",
    val presenceStatus: String = "",
    val statusNote: String = "",
    val timestampMs: Long = 0L
)

@Serializable
data class WebRtcSignalEnvelope(
    val type: String, // JOIN, JOIN_ACK, ROOM_FULL, PEER_HEARTBEAT, ICE_RESTART_REQ, OFFER, ANSWER, ICE, LEAVE, MEDIA_STATE, DATA_PACKET
    val roomName: String,
    val senderId: String,
    val targetId: String = "",
    val sessionId: String = "",
    val peerIdsCsv: String = "",
    val reason: String = "",
    val sdp: String = "",
    val iceMid: String = "",
    val iceMLineIndex: Int = 0,
    val iceCandidate: String = "",
    val cameraEnabled: Boolean = true,
    val micEnabled: Boolean = true,
    val dataPayload: String = "",
    val timestampMs: Long = 0L
)

data class RealtimePresenceEntry(
    val memberId: FamilyMemberId,
    val availability: PresenceAvailability,
    val statusNote: String? = null,
    val updatedAtMs: Long = System.currentTimeMillis()
)

/**
 * Supabase Realtime Signaling + Native Realtime Presence Hub for Mithun (Vapi), Mafitha (Ummi),
 * and Zayn Adam:
 * - Native Supabase Realtime Presence (`presenceChangeFlow` + `track` / `untrack`) automatically
 *   syncs 'Online', 'Away', and 'Busy' state and detects abrupt disconnects via server `leaves`.
 * - Broadcast channel delivers sub-second video/voice call invites, declines, cancellations,
 *   chat messages, XOXO moves, and redundant presence pings for instant responsiveness.
 */
class SupabaseRealtimeSignaling {

    private val coroutineErrorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.w(TAG, "Supabase realtime background error", throwable)
        _isSubscribed.value = false
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineErrorHandler)
    private val connectionMutex = Mutex()
    private val presenceMutex = Mutex()

    @Volatile
    private var shouldStayConnected = false

    @Volatile
    private var trackedMember: FamilyMemberId? = null

    @Volatile
    private var trackedAvailability: PresenceAvailability = PresenceAvailability.ONLINE

    @Volatile
    private var trackedStatusNote: String? = null

    private var channel: RealtimeChannel? = null
    private var broadcastCollectorJob: Job? = null
    private var webRtcCollectorJob: Job? = null
    private var presenceCollectorJob: Job? = null
    private var pendingPresenceJob: Job? = null

    // Tracks the last state actually sent to Supabase Realtime Presence on the current channel
    // so identical/burst calls never exceed Supabase's client presence rate limit.
    private var lastTrackedChannel: RealtimeChannel? = null
    private var lastTrackedMember: FamilyMemberId? = null
    private var lastTrackedAvailability: PresenceAvailability? = null
    private var lastTrackedStatusNote: String? = null
    private var lastTrackSentAtMs: Long = 0L
    private var lastBroadcastSignature: String? = null
    private var lastBroadcastSentAtMs: Long = 0L

    // Tracks presence entries keyed by Supabase presence key so multi-tab/reconnect leaves stay accurate
    private val activePresenceByKey = ConcurrentHashMap<String, RealtimePresenceEntry>()

    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    private val _incomingSignals = MutableSharedFlow<FamilyRealtimeSignal>(extraBufferCapacity = 48)
    val incomingSignals: SharedFlow<FamilyRealtimeSignal> = _incomingSignals.asSharedFlow()

    private val _incomingWebRtcSignals = MutableSharedFlow<WebRtcSignalEnvelope>(extraBufferCapacity = 128)
    val incomingWebRtcSignals: SharedFlow<WebRtcSignalEnvelope> = _incomingWebRtcSignals.asSharedFlow()

    private val _realtimePresenceMap = MutableStateFlow<Map<FamilyMemberId, RealtimePresenceEntry>>(emptyMap())
    val realtimePresenceMap: StateFlow<Map<FamilyMemberId, RealtimePresenceEntry>> =
        _realtimePresenceMap.asStateFlow()

    private val client: SupabaseClient? by lazy {
        if (!isConfigured()) return@lazy null
        runCatching {
            createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL.trim(),
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY.trim()
            ) {
                httpEngine = OkHttp.create {
                    config {
                        pingInterval(20, TimeUnit.SECONDS)
                        retryOnConnectionFailure(true)
                    }
                }
                install(Realtime) {
                    disconnectOnNoSubscriptions = true
                    heartbeatInterval = 15.seconds
                    reconnectDelay = 5.seconds
                }
            }
        }.getOrNull()
    }

    fun isConfigured(): Boolean {
        val url = BuildConfig.SUPABASE_URL.trim()
        val key = BuildConfig.SUPABASE_ANON_KEY.trim()
        return url.startsWith("https://") &&
            !url.contains("your-project-ref") &&
            key.isNotBlank() &&
            !key.contains("your-supabase-anon-key")
    }

    fun connectIfConfigured(
        member: FamilyMemberId? = trackedMember,
        availability: PresenceAvailability = trackedAvailability,
        statusNote: String? = trackedStatusNote,
        sendHello: Boolean = false
    ) {
        if (!isConfigured()) return
        if (member != null) {
            trackedMember = member
            trackedAvailability = availability
            trackedStatusNote = statusNote
        }
        shouldStayConnected = true
        scope.launch {
            runCatching {
                val active = ensureConnectedInternal()
                val m = trackedMember
                if (active != null && m != null) {
                    pushPresenceTrackInternal(active, m, trackedAvailability, trackedStatusNote)
                    if (sendHello) {
                        broadcastPresenceIfAllowed(active, m, trackedAvailability, trackedStatusNote, isHello = true)
                    }
                }
            }.onFailure { err ->
                Log.w(TAG, "connectIfConfigured failed", err)
            }
        }
    }

    private suspend fun ensureConnectedInternal(): RealtimeChannel? = connectionMutex.withLock {
        val supabase = client ?: return null
        if (!shouldStayConnected) return null

        val existing = channel
        if (_isSubscribed.value && existing != null) {
            return existing
        }

        return runCatching {
            broadcastCollectorJob?.cancel()
            broadcastCollectorJob = null
            webRtcCollectorJob?.cancel()
            webRtcCollectorJob = null
            presenceCollectorJob?.cancel()
            presenceCollectorJob = null
            resetPresenceTrackingState()
            runCatching { supabase.realtime.removeAllChannels() }

            val presenceKey = trackedMember?.name ?: "family_device_${System.currentTimeMillis()}"
            val realtimeChannel = supabase.realtime.channel(FAMILY_SIGNAL_CHANNEL) {
                presence {
                    key = presenceKey
                }
            }
            channel = realtimeChannel

            // 1. Register Broadcast & Presence flows BEFORE calling subscribe() to avoid
            // IllegalStateException ("cannot call presenceChangeFlow/broadcastFlow after joining")
            val broadcastFlow = runCatching {
                realtimeChannel.broadcastFlow<FamilyRealtimeSignal>(event = SIGNAL_EVENT)
            }.getOrNull()
            val webRtcFlow = runCatching {
                realtimeChannel.broadcastFlow<WebRtcSignalEnvelope>(event = WEBRTC_SIGNAL_EVENT)
            }.getOrNull()
            val presenceFlow = runCatching {
                realtimeChannel.presenceChangeFlow()
            }.getOrNull()

            if (broadcastFlow != null) {
                broadcastCollectorJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    runCatching {
                        broadcastFlow
                            .catch { err ->
                                Log.w(TAG, "Broadcast flow error", err)
                                _isSubscribed.value = false
                            }
                            .collect { payload ->
                                _incomingSignals.tryEmit(payload)
                            }
                    }
                }
            }

            if (webRtcFlow != null) {
                webRtcCollectorJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    runCatching {
                        webRtcFlow
                            .catch { err ->
                                Log.w(TAG, "WebRTC broadcast flow error", err)
                            }
                            .collect { envelope ->
                                _incomingWebRtcSignals.tryEmit(envelope)
                            }
                    }
                }
            }

            if (presenceFlow != null) {
                presenceCollectorJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    runCatching {
                        presenceFlow
                            .catch { err ->
                                Log.w(TAG, "Presence flow error", err)
                                _isSubscribed.value = false
                            }
                            .collect { action ->
                                action.leaves.forEach { (key, presence) ->
                                    val stateMember = parsePresenceState(presence.state)?.memberId
                                    val removed = activePresenceByKey.remove(key)
                                    if (removed == null && stateMember != null) {
                                        activePresenceByKey.entries.removeAll { it.value.memberId == stateMember }
                                    }
                                }
                                action.joins.forEach { (key, presence) ->
                                    val parsed = parsePresenceState(presence.state)
                                    if (parsed != null) {
                                        activePresenceByKey[key] = parsed
                                    }
                                }
                                publishPresenceSnapshot()
                            }
                    }
                }
            }

            val subscribed = withTimeoutOrNull(10_000L) {
                realtimeChannel.subscribe(blockUntilSubscribed = true)
                true
            } ?: false
            if (!shouldStayConnected || !subscribed) {
                runCatching { realtimeChannel.unsubscribe() }
                if (!shouldStayConnected) {
                    runCatching { supabase.realtime.disconnect() }
                }
                channel = null
                _isSubscribed.value = false
                return@runCatching null
            }
            _isSubscribed.value = true

            // Immediately publish our own Realtime Presence state on subscribe/reconnect
            val self = trackedMember
            if (self != null) {
                pushPresenceTrackInternal(realtimeChannel, self, trackedAvailability, trackedStatusNote)
            }
            realtimeChannel
        }.getOrElse { err ->
            Log.w(TAG, "ensureConnectedInternal failed", err)
            _isSubscribed.value = false
            null
        }
    }

    private fun parsePresenceState(state: JsonObject): RealtimePresenceEntry? {
        val memberRaw = state["member_id"]?.jsonPrimitive?.contentOrNull ?: return null
        val member = runCatching { FamilyMemberId.valueOf(memberRaw) }.getOrNull() ?: return null
        val statusRaw = state["status"]?.jsonPrimitive?.contentOrNull
        val availability = statusRaw?.let {
            runCatching { PresenceAvailability.valueOf(it) }.getOrNull()
        } ?: PresenceAvailability.ONLINE
        val note = state["note"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        val updated = state["updated_at_ms"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis()
        return RealtimePresenceEntry(
            memberId = member,
            availability = availability,
            statusNote = note,
            updatedAtMs = updated
        )
    }

    private fun publishPresenceSnapshot() {
        val grouped = mutableMapOf<FamilyMemberId, RealtimePresenceEntry>()
        activePresenceByKey.values.forEach { entry ->
            val existing = grouped[entry.memberId]
            if (existing == null || entry.updatedAtMs >= existing.updatedAtMs) {
                grouped[entry.memberId] = entry
            }
        }
        _realtimePresenceMap.value = grouped
    }

    private suspend fun resetPresenceTrackingState() {
        presenceMutex.withLock {
            pendingPresenceJob?.cancel()
            pendingPresenceJob = null
            lastTrackedChannel = null
            lastTrackedMember = null
            lastTrackedAvailability = null
            lastTrackedStatusNote = null
        }
    }

    private suspend fun pushPresenceTrackInternal(
        activeChannel: RealtimeChannel,
        member: FamilyMemberId,
        availability: PresenceAvailability,
        statusNote: String?
    ) {
        val normalizedNote = statusNote?.takeIf { it.isNotBlank() }
        var delayToScheduleMs: Long? = null

        presenceMutex.withLock {
            if (!shouldStayConnected || channel !== activeChannel || !_isSubscribed.value) {
                return
            }

            // Skip redundant track() calls when the server already holds this exact presence state.
            val sameStateOnSameChannel = lastTrackedChannel === activeChannel &&
                lastTrackedMember == member &&
                lastTrackedAvailability == availability &&
                lastTrackedStatusNote == normalizedNote
            if (sameStateOnSameChannel) {
                pendingPresenceJob?.cancel()
                pendingPresenceJob = null
                return
            }

            // If we haven't tracked anything on this channel yet, no need to call untrack() for OFFLINE.
            if (availability == PresenceAvailability.OFFLINE &&
                (lastTrackedChannel !== activeChannel || lastTrackedAvailability == PresenceAvailability.OFFLINE)
            ) {
                lastTrackedChannel = activeChannel
                lastTrackedMember = member
                lastTrackedAvailability = PresenceAvailability.OFFLINE
                lastTrackedStatusNote = null
                return
            }

            val now = System.currentTimeMillis()
            val elapsed = now - lastTrackSentAtMs
            if (lastTrackSentAtMs > 0L && elapsed < MIN_PRESENCE_TRACK_INTERVAL_MS) {
                delayToScheduleMs = MIN_PRESENCE_TRACK_INTERVAL_MS - elapsed
            } else {
                pendingPresenceJob?.cancel()
                pendingPresenceJob = null
                val succeeded = runCatching {
                    if (availability == PresenceAvailability.OFFLINE) {
                        activeChannel.untrack()
                    } else {
                        activeChannel.track(
                            buildJsonObject {
                                put("member_id", member.name)
                                put("status", availability.name)
                                put("note", normalizedNote.orEmpty())
                                put("updated_at_ms", now)
                            }
                        )
                    }
                    true
                }.onFailure { err ->
                    Log.w(TAG, "Presence track/untrack failed", err)
                }.getOrDefault(false)

                if (succeeded) {
                    lastTrackedChannel = activeChannel
                    lastTrackedMember = member
                    lastTrackedAvailability = availability
                    lastTrackedStatusNote = normalizedNote
                    lastTrackSentAtMs = System.currentTimeMillis()
                }
            }
        }

        // Coalesce rapid state changes into a single trailing track() once the cooldown elapses.
        val waitMs = delayToScheduleMs
        if (waitMs != null) {
            presenceMutex.withLock {
                if (pendingPresenceJob?.isActive != true) {
                    pendingPresenceJob = scope.launch {
                        delay(waitMs)
                        val latestChannel = channel
                        val latestMember = trackedMember
                        if (shouldStayConnected && latestChannel != null && _isSubscribed.value && latestMember != null) {
                            pushPresenceTrackInternal(
                                activeChannel = latestChannel,
                                member = latestMember,
                                availability = trackedAvailability,
                                statusNote = trackedStatusNote
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun broadcastPresenceIfAllowed(
        activeChannel: RealtimeChannel,
        member: FamilyMemberId,
        availability: PresenceAvailability,
        statusNote: String?,
        isHello: Boolean
    ) {
        val normalizedNote = statusNote?.takeIf { it.isNotBlank() }.orEmpty()
        val signature = "${member.name}|${availability.name}|$normalizedNote|$isHello"
        val now = System.currentTimeMillis()
        val shouldSend = presenceMutex.withLock {
            val sameRecent = signature == lastBroadcastSignature &&
                (now - lastBroadcastSentAtMs) < MIN_PRESENCE_BROADCAST_INTERVAL_MS
            if (sameRecent) {
                false
            } else {
                lastBroadcastSignature = signature
                lastBroadcastSentAtMs = now
                true
            }
        }
        if (!shouldSend) return

        val broadcastPayload = FamilyRealtimeSignal(
            kind = if (availability == PresenceAvailability.OFFLINE) KIND_PRESENCE_OFFLINE else KIND_PRESENCE_PING,
            senderId = member.name,
            text = if (isHello) "HELLO" else availability.name,
            presenceStatus = availability.name,
            statusNote = normalizedNote,
            timestampMs = now
        )
        runCatching {
            activeChannel.broadcast(event = SIGNAL_EVENT, message = broadcastPayload)
        }.onFailure { err ->
            Log.w(TAG, "Presence broadcast failed", err)
        }
    }

    /**
     * Tracks the member's live status ('Online', 'Away', or 'Busy') in Supabase Realtime Presence
     * (deduplicated and rate-limited) and broadcasts a lightweight status signal on changes.
     */
    fun trackPresence(
        member: FamilyMemberId,
        availability: PresenceAvailability,
        statusNote: String? = null,
        isHello: Boolean = false
    ) {
        trackedMember = member
        trackedAvailability = availability
        trackedStatusNote = statusNote
        if (!isConfigured() || !shouldStayConnected) return

        scope.launch {
            runCatching {
                val activeChannel = if (channel != null && _isSubscribed.value) {
                    channel
                } else {
                    ensureConnectedInternal()
                } ?: return@launch
                pushPresenceTrackInternal(activeChannel, member, availability, statusNote)
                broadcastPresenceIfAllowed(activeChannel, member, availability, statusNote, isHello = isHello)
            }
        }
    }

    /**
     * Replies to another member's HELLO ping with a lightweight broadcast only — does NOT call
     * Realtime Presence `track()` because native Presence already syncs tracked state on join.
     */
    fun broadcastPresenceReplyOnly(
        member: FamilyMemberId,
        availability: PresenceAvailability,
        statusNote: String? = null
    ) {
        if (!isConfigured() || !shouldStayConnected) return
        scope.launch {
            runCatching {
                val activeChannel = channel
                if (activeChannel != null && _isSubscribed.value) {
                    broadcastPresenceIfAllowed(
                        activeChannel = activeChannel,
                        member = member,
                        availability = availability,
                        statusNote = statusNote,
                        isHello = false
                    )
                }
            }
        }
    }

    fun broadcastCallInvite(
        caller: FamilyMemberId,
        targets: List<FamilyMemberId>,
        roomName: String,
        isThreeWay: Boolean,
        videoEnabled: Boolean = true
    ) {
        val payload = FamilyRealtimeSignal(
            kind = KIND_CALL_INVITE,
            senderId = caller.name,
            targetIdsCsv = targets.joinToString(",") { it.name },
            roomName = roomName,
            isThreeWay = isThreeWay,
            videoEnabled = videoEnabled,
            timestampMs = System.currentTimeMillis()
        )
        sendSignal(payload)
    }

    fun broadcastCallDecline(
        sender: FamilyMemberId,
        targets: List<FamilyMemberId>,
        roomName: String
    ) {
        val payload = FamilyRealtimeSignal(
            kind = KIND_CALL_DECLINE,
            senderId = sender.name,
            targetIdsCsv = targets.joinToString(",") { it.name },
            roomName = roomName,
            timestampMs = System.currentTimeMillis()
        )
        sendSignal(payload)
    }

    fun broadcastCallEnded(
        sender: FamilyMemberId,
        targets: List<FamilyMemberId>,
        roomName: String
    ) {
        val payload = FamilyRealtimeSignal(
            kind = KIND_CALL_END,
            senderId = sender.name,
            targetIdsCsv = targets.joinToString(",") { it.name },
            roomName = roomName,
            timestampMs = System.currentTimeMillis()
        )
        sendSignal(payload)
    }

    fun broadcastHeartbeat(
        sender: FamilyMemberId,
        label: String,
        isSafetyAlert: Boolean
    ) {
        val payload = FamilyRealtimeSignal(
            kind = KIND_HEARTBEAT,
            senderId = sender.name,
            text = label,
            isSafetyAlert = isSafetyAlert,
            timestampMs = System.currentTimeMillis()
        )
        sendSignal(payload)
    }

    fun broadcastChatMessage(
        messageId: String,
        sender: FamilyMemberId,
        threadId: String,
        recipientsCsv: String,
        text: String,
        messageType: String,
        timestampMs: Long,
        mediaUrl: String = ""
    ) {
        val payload = FamilyRealtimeSignal(
            kind = KIND_CHAT_MESSAGE,
            senderId = sender.name,
            targetIdsCsv = recipientsCsv,
            threadId = threadId,
            messageId = messageId,
            text = text,
            messageType = messageType,
            mediaUrl = mediaUrl,
            timestampMs = timestampMs
        )
        sendSignal(payload)
    }

    fun broadcastProfileUpdate(
        member: FamilyMemberId,
        badgeElement: String,
        avatarUrl: String?
    ) {
        val payload = FamilyRealtimeSignal(
            kind = KIND_PROFILE_UPDATE,
            senderId = member.name,
            comicStyle = badgeElement,
            mediaUrl = avatarUrl.orEmpty(),
            timestampMs = System.currentTimeMillis()
        )
        sendSignal(payload)
    }

    fun broadcastMessageReaction(
        sender: FamilyMemberId,
        messageId: String,
        emoji: String
    ) {
        val payload = FamilyRealtimeSignal(
            kind = KIND_MESSAGE_REACTION,
            senderId = sender.name,
            messageId = messageId,
            text = emoji,
            timestampMs = System.currentTimeMillis()
        )
        sendSignal(payload)
    }

    fun broadcastParentLog(
        author: FamilyMemberId,
        logId: String,
        title: String,
        subtitle: String,
        costEstimate: String?,
        timestampMs: Long
    ) {
        val payload = FamilyRealtimeSignal(
            kind = KIND_PARENT_LOG,
            senderId = author.name,
            messageId = logId,
            text = title,
            comicStyle = subtitle,
            dateKey = costEstimate.orEmpty(),
            timestampMs = timestampMs
        )
        sendSignal(payload)
    }

    fun broadcastDailyComicPhoto(
        photoId: String,
        dateKey: String,
        author: FamilyMemberId,
        caption: String,
        comicStyle: String,
        imageBase64: String,
        timestampMs: Long
    ) {
        val payload = FamilyRealtimeSignal(
            kind = KIND_DAILY_COMIC,
            senderId = author.name,
            messageId = photoId,
            dateKey = dateKey,
            text = caption,
            comicStyle = comicStyle,
            imageBase64 = imageBase64,
            timestampMs = timestampMs
        )
        sendSignal(payload)
    }

    fun broadcastPresencePing(
        member: FamilyMemberId,
        availability: PresenceAvailability = PresenceAvailability.ONLINE,
        statusNote: String? = null,
        isHello: Boolean = false
    ) {
        trackPresence(
            member = member,
            availability = availability,
            statusNote = statusNote,
            isHello = isHello
        )
    }

    fun broadcastXoxoState(
        sender: FamilyMemberId,
        stateJson: String
    ) {
        val payload = FamilyRealtimeSignal(
            kind = KIND_XOXO_STATE,
            senderId = sender.name,
            text = stateJson,
            timestampMs = System.currentTimeMillis()
        )
        sendSignal(payload)
    }

    fun broadcastPresenceOffline(member: FamilyMemberId) {
        if (!isConfigured()) return
        val payload = FamilyRealtimeSignal(
            kind = KIND_PRESENCE_OFFLINE,
            senderId = member.name,
            text = "OFFLINE",
            presenceStatus = PresenceAvailability.OFFLINE.name,
            timestampMs = System.currentTimeMillis()
        )
        scope.launch {
            runCatching {
                val activeChannel = channel
                if (activeChannel != null && _isSubscribed.value) {
                    withTimeoutOrNull(1_200L) {
                        val shouldUntrack = presenceMutex.withLock {
                            val hadTracked = lastTrackedChannel === activeChannel &&
                                lastTrackedAvailability != null &&
                                lastTrackedAvailability != PresenceAvailability.OFFLINE
                            if (hadTracked) {
                                lastTrackedAvailability = PresenceAvailability.OFFLINE
                                lastTrackSentAtMs = System.currentTimeMillis()
                            }
                            hadTracked
                        }
                        if (shouldUntrack) {
                            runCatching { activeChannel.untrack() }
                        }
                        activeChannel.broadcast(event = SIGNAL_EVENT, message = payload)
                    }
                }
            }
        }
    }

    suspend fun ensureSubscribedForCall(): Boolean {
        if (!isConfigured()) return false
        shouldStayConnected = true
        return ensureConnectedInternal() != null
    }

    /**
     * Forces a fresh WebSocket subscribe after a Wi-Fi <-> Cellular handoff so stale sockets
     * never stall WebRTC ICE restart signaling.
     */
    suspend fun reconnectForNetworkSwitch(): Boolean {
        if (!isConfigured()) return false
        shouldStayConnected = true
        connectionMutex.withLock {
            _isSubscribed.value = false
            runCatching { channel?.unsubscribe() }
            channel = null
        }
        return ensureConnectedInternal() != null
    }

    fun sendWebRtcSignal(envelope: WebRtcSignalEnvelope) {
        if (!isConfigured()) return
        shouldStayConnected = true
        scope.launch {
            runCatching {
                val activeChannel = if (channel != null && _isSubscribed.value) {
                    channel
                } else {
                    ensureConnectedInternal()
                }
                activeChannel?.broadcast(event = WEBRTC_SIGNAL_EVENT, message = envelope)
            }.onFailure { err ->
                Log.w(TAG, "sendWebRtcSignal(${envelope.type}) failed", err)
            }
        }
    }

    private fun sendSignal(payload: FamilyRealtimeSignal) {
        if (!isConfigured() || !shouldStayConnected) return
        scope.launch {
            runCatching {
                val activeChannel = if (channel != null && _isSubscribed.value) {
                    channel
                } else {
                    ensureConnectedInternal()
                }
                activeChannel?.broadcast(event = SIGNAL_EVENT, message = payload)
            }
        }
    }

    /**
     * Cleanly untracks presence, broadcasts an optional away/offline notice, and closes the
     * Realtime WebSocket before Android suspends network sockets in the background.
     */
    fun disconnectGracefully(
        offlineMember: FamilyMemberId? = null,
        finalAvailability: PresenceAvailability = PresenceAvailability.OFFLINE
    ) {
        shouldStayConnected = false
        scope.launch {
            connectionMutex.withLock {
                val activeChannel = channel
                if (offlineMember != null && activeChannel != null && _isSubscribed.value) {
                    runCatching {
                        withTimeoutOrNull(1_000L) {
                            val shouldUntrack = presenceMutex.withLock {
                                lastTrackedChannel === activeChannel &&
                                    lastTrackedAvailability != null &&
                                    lastTrackedAvailability != PresenceAvailability.OFFLINE
                            }
                            if (shouldUntrack) {
                                runCatching { activeChannel.untrack() }
                            }
                            activeChannel.broadcast(
                                event = SIGNAL_EVENT,
                                message = FamilyRealtimeSignal(
                                    kind = if (finalAvailability == PresenceAvailability.OFFLINE) {
                                        KIND_PRESENCE_OFFLINE
                                    } else {
                                        KIND_PRESENCE_PING
                                    },
                                    senderId = offlineMember.name,
                                    text = finalAvailability.name,
                                    presenceStatus = finalAvailability.name,
                                    timestampMs = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
                resetPresenceTrackingState()
                broadcastCollectorJob?.cancel()
                broadcastCollectorJob = null
                webRtcCollectorJob?.cancel()
                webRtcCollectorJob = null
                presenceCollectorJob?.cancel()
                presenceCollectorJob = null
                activePresenceByKey.clear()
                _realtimePresenceMap.value = emptyMap()
                runCatching { activeChannel?.unsubscribe() }
                runCatching { client?.realtime?.removeAllChannels() }
                runCatching { client?.realtime?.disconnect() }
                channel = null
                _isSubscribed.value = false
            }
        }
    }

    fun disconnect() {
        disconnectGracefully(offlineMember = null)
    }

    companion object {
        private const val TAG = "SupabaseRealtime"
        private const val FAMILY_SIGNAL_CHANNEL = "holymoly-family-signal"
        private const val SIGNAL_EVENT = "family_signal"
        private const val WEBRTC_SIGNAL_EVENT = "webrtc_signal"
        private const val MIN_PRESENCE_TRACK_INTERVAL_MS = 2_500L
        private const val MIN_PRESENCE_BROADCAST_INTERVAL_MS = 5_000L

        const val KIND_CALL_INVITE = "CALL_INVITE"
        const val KIND_CALL_DECLINE = "CALL_DECLINE"
        const val KIND_CALL_END = "CALL_END"
        const val KIND_HEARTBEAT = "HEARTBEAT"
        const val KIND_CHAT_MESSAGE = "CHAT_MESSAGE"
        const val KIND_DAILY_COMIC = "DAILY_COMIC"
        const val KIND_PRESENCE_PING = "PRESENCE_PING"
        const val KIND_PRESENCE_OFFLINE = "PRESENCE_OFFLINE"
        const val KIND_XOXO_STATE = "XOXO_STATE"
        const val KIND_PROFILE_UPDATE = "PROFILE_UPDATE"
        const val KIND_MESSAGE_REACTION = "MESSAGE_REACTION"
        const val KIND_PARENT_LOG = "PARENT_LOG"
    }
}
