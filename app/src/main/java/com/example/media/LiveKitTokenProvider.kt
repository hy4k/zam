package com.example.media

import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.FamilyMemberId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

sealed interface LiveKitTokenResult {
    data class Ready(val token: String, val source: String) : LiveKitTokenResult
    data class MissingConfig(val reason: String) : LiveKitTokenResult
    data class Failed(val reason: String) : LiveKitTokenResult
}

/**
 * Provides signed LiveKit JWT room access tokens for Mithun (Vapi), Mafitha (Ummi), and Zayn Adam.
 * Priority 1: Direct HS256 JWT minting when LIVEKIT_API_KEY & LIVEKIT_API_SECRET are provided in AI Studio Secrets.
 * Priority 2: Supabase Edge Function (`${SUPABASE_URL}/functions/v1/livekit-token`) when deployed on Supabase.
 */
object LiveKitTokenProvider {

    fun isLiveKitUrlConfigured(): Boolean {
        val url = BuildConfig.LIVEKIT_WS_URL.trim()
        return (url.startsWith("wss://") || url.startsWith("ws://")) &&
            !url.contains("your-livekit-instance")
    }

    fun isDirectKeySecretConfigured(): Boolean {
        val key = BuildConfig.LIVEKIT_API_KEY.trim()
        val secret = BuildConfig.LIVEKIT_API_SECRET.trim()
        return key.isNotBlank() &&
            !key.contains("your-livekit-api-key") &&
            secret.isNotBlank() &&
            !secret.contains("your-livekit-api-secret")
    }

    private fun isSupabaseConfigured(): Boolean {
        val url = BuildConfig.SUPABASE_URL.trim()
        val key = BuildConfig.SUPABASE_ANON_KEY.trim()
        return url.startsWith("https://") &&
            !url.contains("your-project-ref") &&
            key.isNotBlank() &&
            !key.contains("your-supabase-anon-key")
    }

    suspend fun obtainRoomToken(
        member: FamilyMemberId,
        roomName: String
    ): LiveKitTokenResult = withContext(Dispatchers.IO) {
        if (!isLiveKitUrlConfigured()) {
            return@withContext LiveKitTokenResult.MissingConfig(
                "Add LIVEKIT_WS_URL in Secrets to enable live video."
            )
        }

        // Priority 1: Local HS256 JWT signing via LiveKit API Key + Secret from Secrets panel
        if (isDirectKeySecretConfigured()) {
            return@withContext runCatching {
                val jwt = mintLiveKitJwt(
                    apiKey = BuildConfig.LIVEKIT_API_KEY.trim(),
                    apiSecret = BuildConfig.LIVEKIT_API_SECRET.trim(),
                    identity = member.name,
                    displayName = "${member.displayName} (${member.title})",
                    roomName = roomName
                )
                LiveKitTokenResult.Ready(jwt, "LiveKit HS256 Key")
            }.getOrElse { err ->
                LiveKitTokenResult.Failed("Token signing error: ${err.localizedMessage ?: "unknown"}")
            }
        }

        // Priority 2: Supabase Edge Function (`livekit-token`) if deployed
        if (isSupabaseConfigured()) {
            val edgeResult = fetchFromSupabaseEdgeFunction(member, roomName)
            if (edgeResult is LiveKitTokenResult.Ready) {
                return@withContext edgeResult
            }
        }

        LiveKitTokenResult.MissingConfig(
            "Add LIVEKIT_API_KEY & LIVEKIT_API_SECRET in AI Studio Secrets (or deploy Supabase livekit-token function)."
        )
    }

    private fun fetchFromSupabaseEdgeFunction(
        member: FamilyMemberId,
        roomName: String
    ): LiveKitTokenResult {
        return runCatching {
            val endpoint = "${BuildConfig.SUPABASE_URL.trim().trimEnd('/')}/functions/v1/livekit-token"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 4_000
                readTimeout = 4_500
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY.trim())
                setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY.trim()}")
            }

            val body = JSONObject().apply {
                put("roomName", roomName)
                put("room", roomName)
                put("identity", member.name)
                put("participantName", "${member.displayName} (${member.title})")
                put("name", "${member.displayName} (${member.title})")
            }.toString()

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body)
                writer.flush()
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            conn.disconnect()

            if (code !in 200..299) {
                return LiveKitTokenResult.Failed("Edge token HTTP $code")
            }

            val json = JSONObject(responseText)
            val token = json.optString("token").ifBlank { json.optString("accessToken") }
            if (token.isNotBlank()) {
                LiveKitTokenResult.Ready(token, "Supabase Edge Function")
            } else {
                LiveKitTokenResult.Failed("Edge function returned empty token")
            }
        }.getOrElse { err ->
            LiveKitTokenResult.Failed(err.localizedMessage ?: "Edge token request failed")
        }
    }

    private fun mintLiveKitJwt(
        apiKey: String,
        apiSecret: String,
        identity: String,
        displayName: String,
        roomName: String,
        ttlSeconds: Long = 6L * 3600L
    ): String {
        val nowSec = System.currentTimeMillis() / 1000L

        val headerJson = JSONObject().apply {
            put("alg", "HS256")
            put("typ", "JWT")
        }.toString()

        val videoGrant = JSONObject().apply {
            put("roomJoin", true)
            put("room", roomName)
            put("canPublish", true)
            put("canSubscribe", true)
            put("canPublishData", true)
        }

        val payloadJson = JSONObject().apply {
            put("iss", apiKey)
            put("sub", identity)
            put("name", displayName)
            put("jti", "$identity-$nowSec")
            put("nbf", nowSec - 15L)
            put("exp", nowSec + ttlSeconds)
            put("video", videoGrant)
        }.toString()

        val encodedHeader = base64Url(headerJson.toByteArray(Charsets.UTF_8))
        val encodedPayload = base64Url(payloadJson.toByteArray(Charsets.UTF_8))
        val signingInput = "$encodedHeader.$encodedPayload"

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(apiSecret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val signature = mac.doFinal(signingInput.toByteArray(Charsets.UTF_8))
        val encodedSignature = base64Url(signature)

        return "$signingInput.$encodedSignature"
    }

    private fun base64Url(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}
