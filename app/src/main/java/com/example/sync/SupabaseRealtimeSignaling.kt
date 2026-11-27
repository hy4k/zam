package com.example.sync

import com.example.BuildConfig
import com.example.data.model.FamilyMemberId
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@Serializable
data class FamilyRealtimeSignal(
    val kind: String, // CALL_INVITE, CALL_DECLINE, CHAT_MESSAGE, HEARTBEAT, DAILY_COMIC, PRESENCE_*
    val senderId: String,
    val targetIdsCsv: String = "MITHUN,MAFITHA,ZAYN",
    val roomName: String = "holymoly-family-room",
    val isThreeWay: Boolean = true,
    val threadId: String = "family_group",
    val messageId: String = "",
    val text: String = "",
    val messageType: String = "TEXT",
    val isSafetyAlert: Boolean = false,
    val dateKey: String = "",
    val comicStyle: String = "",
    val imageBase64: String = "",
    val mediaUrl: String = "",
    val timestampMs: Long = 0L
)

/**
 * Zero-schema Supabase Realtime Broadcast Signaling Hub for Mithun (Vapi), Mafitha (Ummi), and Zayn Adam.
 * Manages WebSocket lifecycle cleanly across foreground/background and network transitions so
 * Android socket suspension does not cause aborted WebSocket read loops.
 */
class SupabaseRealtimeSignaling {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectionMutex = Mutex()

    @Volatile
    private var shouldStayConnected = false

    private var channel: RealtimeChannel? = null
    private var collectorJob: Job? = null

    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    private val _incomingSignals = MutableSharedFlow<FamilyRealtimeSignal>(extraBufferCapacity = 32)
    val incomingSignals: SharedFlow<FamilyRealtimeSignal> = _incomingSignals.asSharedFlow()

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

    fun connectIfConfigured() {
        if (!isConfigured()) return
        shouldStayConnected = true
        scope.launch {
            ensureConnectedInternal()
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
            collectorJob?.cancel()
            collectorJob = null
            runCatching { supabase.realtime.removeAllChannels() }

            val realtimeChannel = supabase.realtime.channel(FAMILY_SIGNAL_CHANNEL)
            channel = realtimeChannel

            collectorJob = scope.launch {
                realtimeChannel
                    .broadcastFlow<FamilyRealtimeSignal>(event = SIGNAL_EVENT)
                    .catch {
                        _isSubscribed.value = false
                    }
                    .collect { payload ->
                        _incomingSignals.tryEmit(payload)
                    }
            }

            withTimeoutOrNull(10_000L) {
                realtimeChannel.subscribe(blockUntilSubscribed = true)
            }
            if (!shouldStayConnected) {
                runCatching { realtimeChannel.unsubscribe() }
                runCatching { supabase.realtime.disconnect() }
                channel = null
                _isSubscribed.value = false
                return@runCatching null
            }
            _isSubscribed.value = true
            realtimeChannel
        }.getOrElse {
            _isSubscribed.value = false
            null
        }
    }

    fun broadcastCallInvite(
        caller: FamilyMemberId,
        targets: List<FamilyMemberId>,
        roomName: String,
        isThreeWay: Boolean
    ) {
        val payload = FamilyRealtimeSignal(
            kind = KIND_CALL_INVITE,
            senderId = caller.name,
            targetIdsCsv = targets.joinToString(",") { it.name },
            roomName = roomName,
            isThreeWay = isThreeWay,
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
        isHello: Boolean = false
    ) {
        val payload = FamilyRealtimeSignal(
            kind = KIND_PRESENCE_PING,
            senderId = member.name,
            text = if (isHello) "HELLO" else "ONLINE",
            timestampMs = System.currentTimeMillis()
        )
        sendSignal(payload)
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
            timestampMs = System.currentTimeMillis()
        )
        scope.launch {
            runCatching {
                val activeChannel = channel
                if (activeChannel != null && _isSubscribed.value) {
                    withTimeoutOrNull(1_200L) {
                        activeChannel.broadcast(event = SIGNAL_EVENT, message = payload)
                    }
                }
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
     * Cleanly broadcasts an optional offline presence notice and closes the Realtime WebSocket
     * before Android suspends network sockets in the background or on connectivity loss.
     */
    fun disconnectGracefully(offlineMember: FamilyMemberId? = null) {
        shouldStayConnected = false
        scope.launch {
            connectionMutex.withLock {
                val activeChannel = channel
                if (offlineMember != null && activeChannel != null && _isSubscribed.value) {
                    runCatching {
                        withTimeoutOrNull(1_000L) {
                            activeChannel.broadcast(
                                event = SIGNAL_EVENT,
                                message = FamilyRealtimeSignal(
                                    kind = KIND_PRESENCE_OFFLINE,
                                    senderId = offlineMember.name,
                                    text = "OFFLINE",
                                    timestampMs = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
                collectorJob?.cancel()
                collectorJob = null
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
        private const val FAMILY_SIGNAL_CHANNEL = "holymoly-family-signal"
        private const val SIGNAL_EVENT = "family_signal"

        const val KIND_CALL_INVITE = "CALL_INVITE"
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
