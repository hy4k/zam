package com.example.media

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.webrtc.PeerConnection
import java.net.HttpURLConnection
import java.net.URL

@Serializable
private data class TurnServerDto(
    val urls: List<String> = emptyList(),
    val username: String = "",
    val credential: String = ""
)

@Serializable
private data class TurnCredentialsResponseDto(
    val ttl: Long = 600L,
    val expiresAtMs: Long = 0L,
    val iceServers: List<TurnServerDto> = emptyList()
)

/**
 * Provides STUN + short-TTL TURN ICE servers for native WebRTC PeerConnections.
 *
 * Resolution order:
 * 1. Google's public STUN servers (always included for fast direct P2P hole-punching).
 * 2. Dynamic time-limited coturn credentials fetched from the configured backend endpoint
 *    (`WEBRTC_TURN_ENDPOINT`, or auto-derived from `${SUPABASE_URL}/functions/v1/webrtc-turn-credentials`),
 *    cached in memory until 60 seconds before expiry. Supports UDP 3478, TCP 3478, and TLS 443.
 * 3. Optional static TURN fallback from AI Studio Secrets (`WEBRTC_TURN_URL` / `USERNAME` / `CREDENTIAL`),
 *    safely ignoring `.env.example` placeholder strings.
 */
class TurnCredentialsProvider {

    private val json = Json { ignoreUnknownKeys = true }
    private val cacheMutex = Mutex()

    private var cachedDynamicServers: List<PeerConnection.IceServer> = emptyList()
    private var cachedExpiresAtMs: Long = 0L

    /**
     * Returns the current list of ICE servers, refreshing short-TTL TURN credentials
     * when missing, expired, or when [forceRefresh] is true (e.g. after a network switch).
     */
    suspend fun getIceServers(
        identity: String,
        roomName: String,
        forceRefresh: Boolean = false
    ): List<PeerConnection.IceServer> {
        val baseStun = defaultStunServers()
        val now = System.currentTimeMillis()

        val dynamicTurn = cacheMutex.withLock {
            val cacheValid = !forceRefresh &&
                cachedDynamicServers.isNotEmpty() &&
                now < (cachedExpiresAtMs - REFRESH_MARGIN_MS)

            if (cacheValid) {
                cachedDynamicServers
            } else {
                val fetched = fetchShortLivedTurnCredentials(identity = identity, roomName = roomName)
                if (fetched != null && fetched.first.isNotEmpty()) {
                    cachedDynamicServers = fetched.first
                    cachedExpiresAtMs = fetched.second
                    fetched.first
                } else if (now < cachedExpiresAtMs && cachedDynamicServers.isNotEmpty()) {
                    // Keep still-valid cached credentials if refresh transiently failed during handoff
                    cachedDynamicServers
                } else {
                    emptyList()
                }
            }
        }

        if (dynamicTurn.isNotEmpty()) {
            return baseStun + dynamicTurn
        }

        val staticFallback = buildStaticFallbackTurnServers()
        return baseStun + staticFallback
    }

    /**
     * Synchronous fallback snapshot (STUN + cached/static TURN) for non-suspending callers.
     */
    fun getCachedOrDefaultIceServers(): List<PeerConnection.IceServer> {
        val baseStun = defaultStunServers()
        val now = System.currentTimeMillis()
        if (cachedDynamicServers.isNotEmpty() && now < cachedExpiresAtMs) {
            return baseStun + cachedDynamicServers
        }
        return baseStun + buildStaticFallbackTurnServers()
    }

    fun invalidateCache() {
        cachedExpiresAtMs = 0L
    }

    private fun resolveTurnEndpoint(): Pair<String, Boolean>? {
        val explicit = BuildConfig.WEBRTC_TURN_ENDPOINT.trim()
        if (explicit.startsWith("https://") && !explicit.contains("your-project-ref")) {
            return explicit to false
        }
        val signalingUrl = BuildConfig.WEBRTC_SIGNALING_URL.trim().trimEnd('/')
        if ((signalingUrl.startsWith("https://") || signalingUrl.startsWith("http://")) &&
            !signalingUrl.contains("your-signaling-server")
        ) {
            return "$signalingUrl/api/turn-credentials" to true
        }
        val supabaseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        if (supabaseUrl.startsWith("https://") && !supabaseUrl.contains("your-project-ref")) {
            return "$supabaseUrl/functions/v1/webrtc-turn-credentials" to false
        }
        return null
    }

    private suspend fun fetchShortLivedTurnCredentials(
        identity: String,
        roomName: String
    ): Pair<List<PeerConnection.IceServer>, Long>? = withContext(Dispatchers.IO) {
        val (endpoint, useSignalingSecret) = resolveTurnEndpoint() ?: return@withContext null
        val anonKey = BuildConfig.SUPABASE_ANON_KEY.trim()
        val signalingSecret = BuildConfig.WEBRTC_SIGNALING_SECRET.trim()

        runCatching {
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 4_000
                readTimeout = 4_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                if (useSignalingSecret) {
                    if (signalingSecret.isNotBlank() && !signalingSecret.contains("your-signaling-shared-secret")) {
                        setRequestProperty("Authorization", "Bearer $signalingSecret")
                    }
                } else if (anonKey.isNotBlank() && !anonKey.contains("your-supabase-anon-key")) {
                    setRequestProperty("apikey", anonKey)
                    setRequestProperty("Authorization", "Bearer $anonKey")
                }
            }
            try {
                val safeIdentity = identity.replace("\"", "")
                val safeRoom = roomName.replace("\"", "")
                val body = """{"identity":"$safeIdentity","roomName":"$safeRoom"}"""
                connection.outputStream.use { os ->
                    os.write(body.toByteArray(Charsets.UTF_8))
                }
                val code = connection.responseCode
                if (code !in 200..299) {
                    Log.d(TAG, "Dynamic TURN endpoint returned HTTP $code; falling back to static/STUN")
                    return@runCatching null
                }
                val raw = connection.inputStream.bufferedReader().use { it.readText() }
                val dto = json.decodeFromString<TurnCredentialsResponseDto>(raw)
                val turnOnly = dto.iceServers.mapNotNull { entry ->
                    val validUrls = entry.urls
                        .map { it.trim() }
                        .filter { it.startsWith("turn:") || it.startsWith("turns:") }
                    if (validUrls.isEmpty()) return@mapNotNull null
                    val builder = PeerConnection.IceServer.builder(validUrls)
                    if (entry.username.isNotBlank()) builder.setUsername(entry.username)
                    if (entry.credential.isNotBlank()) builder.setPassword(entry.credential)
                    builder.createIceServer()
                }
                val computedExpiry = if (dto.expiresAtMs > 0L) {
                    dto.expiresAtMs
                } else {
                    System.currentTimeMillis() + (dto.ttl.coerceAtLeast(120L) * 1_000L)
                }
                if (turnOnly.isNotEmpty()) {
                    Log.d(TAG, "Fetched ${turnOnly.size} short-TTL TURN server entries (ttl=${dto.ttl}s)")
                    turnOnly to computedExpiry
                } else {
                    null
                }
            } finally {
                connection.disconnect()
            }
        }.onFailure { err ->
            Log.d(TAG, "Dynamic TURN fetch unavailable (${err.message}); using STUN/static fallback")
        }.getOrNull()
    }

    private fun buildStaticFallbackTurnServers(): List<PeerConnection.IceServer> {
        val rawUrls = BuildConfig.WEBRTC_TURN_URL.trim()
        val turnUser = BuildConfig.WEBRTC_TURN_USERNAME.trim()
        val turnCred = BuildConfig.WEBRTC_TURN_CREDENTIAL.trim()
        if (rawUrls.isBlank() || rawUrls.contains("your-turn-server")) return emptyList()

        val urls = rawUrls.split(",")
            .map { it.trim() }
            .filter { it.startsWith("turn:") || it.startsWith("turns:") }
        if (urls.isEmpty()) return emptyList()

        val builder = PeerConnection.IceServer.builder(urls)
        if (turnUser.isNotBlank() && !turnUser.contains("your-turn-username")) {
            builder.setUsername(turnUser)
        }
        if (turnCred.isNotBlank() && !turnCred.contains("your-turn-credential")) {
            builder.setPassword(turnCred)
        }
        return listOf(builder.createIceServer())
    }

    private fun defaultStunServers(): List<PeerConnection.IceServer> = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer()
    )

    companion object {
        private const val TAG = "TurnCredentialsProvider"
        private const val REFRESH_MARGIN_MS = 60_000L
    }
}
