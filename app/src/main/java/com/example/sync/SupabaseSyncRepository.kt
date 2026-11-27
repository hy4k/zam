package com.example.sync

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.BuildConfig
import com.example.data.local.EncryptedMessageEntity
import com.example.data.local.KinNestDao
import com.example.data.local.MemoryCapsuleEntity
import com.example.data.local.OfflineSyncQueueEntity
import com.example.data.model.FamilyMemberId
import com.example.data.model.XoxoGameState
import com.example.data.model.XoxoMark
import com.example.data.model.XoxoPlayMode
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File

@Serializable
data class SupabaseProfileDto(
    val member_id: String,
    val display_name: String,
    val role_label: String,
    val avatar_url: String? = null,
    val badge_element: String = "STARLIGHT",
    val halo_color_hex: Long = 0xFF38BDF8,
    val updated_at_ms: Long = 0L
)

@Serializable
data class SupabaseFamilyMessageDto(
    val id: String,
    val thread_id: String,
    val sender_id: String,
    val recipient_ids_csv: String = "MITHUN,MAFITHA,ZAYN",
    val body: String = "",
    val message_type: String = "TEXT",
    val media_url: String? = null,
    val reaction_emoji: String? = null,
    val delivery_status: String = "DELIVERED",
    val timestamp_ms: Long
)

@Serializable
data class SupabaseXoxoGameDto(
    val id: String = "family_main",
    val board_json: String = "[\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"]",
    val player_x: String = "MITHUN",
    val player_o: String = "ZAYN",
    val current_turn: String = "X",
    val starting_mark: String = "X",
    val winner_mark: String? = null,
    val winning_line_json: String = "[]",
    val is_draw: Boolean = false,
    val x_score: Int = 0,
    val o_score: Int = 0,
    val draw_score: Int = 0,
    val round_number: Int = 1,
    val updated_at_ms: Long = 0L
)

@Serializable
data class SupabasePresenceDto(
    val member_id: String,
    val is_online: Boolean = false,
    val last_seen_ms: Long = 0L
)

@Serializable
data class SupabaseParentLogDto(
    val id: String,
    val author_id: String,
    val title: String,
    val subtitle: String = "",
    val cost_estimate: String? = null,
    val target_date_label: String? = "Adults Only",
    val created_at_ms: Long
)

@Serializable
data class SupabaseCallLogDto(
    val id: String,
    val caller_id: String,
    val participants_csv: String,
    val room_name: String = "holymoly-family-room",
    val is_three_way: Boolean = true,
    val duration_seconds: Long = 0L,
    val started_at_ms: Long
)

@Serializable
data class SupabaseMessageDto(
    val id: String,
    val thread_id: String,
    val sender_id: String,
    val recipient_ids_csv: String,
    val ciphertext_payload: String,
    val iv_base64: String,
    val message_type: String,
    val timestamp_ms: Long
)

@Serializable
data class SupabaseCapsuleDto(
    val id: String,
    val author_id: String,
    val title: String,
    val caption: String,
    val media_type: String,
    val unlock_at_ms: Long?,
    val created_at_ms: Long
)

interface SyncRepository {
    fun isSupabaseConfigured(): Boolean
    fun observeMessages(threadId: String): Flow<List<EncryptedMessageEntity>>
    fun observeCapsules(): Flow<List<MemoryCapsuleEntity>>
    fun observeSyncQueue(): Flow<List<OfflineSyncQueueEntity>>
    suspend fun pushPendingQueueItem(item: OfflineSyncQueueEntity): Result<Unit>
}

/**
 * Full Supabase Backend Repository (Postgrest + Storage) paired with offline-first Room SQLite.
 * Syncs:
 *  - Family profile pictures (Supabase Storage) & profile details (family_profiles)
 *  - Family chat messages, reactions, Walkie-Talkie voice clips, and shared photos/videos (family_messages + Storage)
 *  - Premium XOXO live board & scores (xoxo_games)
 *  - Family online presence & last-seen (family_presence)
 *  - Parent grown-up logs (family_parent_logs)
 *  - Video call logs (family_call_logs)
 */
class KinNestSyncRepository(
    private val dao: KinNestDao
) : SyncRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val supabaseClient: SupabaseClient? by lazy {
        if (!isSupabaseConfigured()) return@lazy null
        runCatching {
            createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL.trim(),
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY.trim()
            ) {
                install(Auth)
                install(Postgrest)
                install(Realtime)
                install(Storage)
            }
        }.getOrNull()
    }

    override fun isSupabaseConfigured(): Boolean {
        val url = BuildConfig.SUPABASE_URL.trim()
        val key = BuildConfig.SUPABASE_ANON_KEY.trim()
        return url.startsWith("https://") &&
            !url.contains("your-project-ref") &&
            key.isNotBlank() &&
            !key.contains("your-supabase-anon-key")
    }

    override fun observeMessages(threadId: String): Flow<List<EncryptedMessageEntity>> =
        dao.observeMessagesByThread(threadId)

    override fun observeCapsules(): Flow<List<MemoryCapsuleEntity>> =
        dao.observeMemoryCapsules()

    override fun observeSyncQueue(): Flow<List<OfflineSyncQueueEntity>> =
        dao.observeSyncQueue()

    override suspend fun pushPendingQueueItem(item: OfflineSyncQueueEntity): Result<Unit> {
        val client = supabaseClient ?: return Result.success(Unit)
        runCatching {
            if (item.entityType == "MESSAGE") {
                val localMsg = dao.getMessageById(item.entityId)
                if (localMsg != null) {
                    upsertFamilyMessage(
                        SupabaseFamilyMessageDto(
                            id = localMsg.id,
                            thread_id = localMsg.threadId,
                            sender_id = localMsg.senderId,
                            recipient_ids_csv = localMsg.recipientIdsCsv,
                            body = localMsg.decryptedPreview,
                            message_type = localMsg.messageType,
                            media_url = localMsg.mediaRemoteUrl,
                            reaction_emoji = localMsg.reactionEmoji,
                            delivery_status = "DELIVERED",
                            timestamp_ms = localMsg.timestampMs
                        )
                    )
                }
                dao.updateMessageStatus(item.entityId, "DELIVERED")
            }
        }
        return Result.success(Unit)
    }

    // --- 1. Supabase Storage Uploads (Profile Pics, Voice Notes, Chat Photos/Videos) ---

    suspend fun uploadAvatarPhoto(
        context: Context,
        member: FamilyMemberId,
        uriString: String
    ): String? = withContext(Dispatchers.IO) {
        val client = supabaseClient ?: return@withContext null
        if (uriString.startsWith("https://")) return@withContext uriString
        val jpegBytes = compressImageUriToJpeg(context, uriString, maxSidePx = 720, quality = 82)
            ?: return@withContext null
        val storagePath = "avatars/${member.name.lowercase()}.jpg"
        runCatching {
            val bucket = client.storage.from(MEDIA_BUCKET)
            bucket.upload(path = storagePath, data = jpegBytes) {
                upsert = true
                contentType = ContentType.Image.JPEG
            }
            "${bucket.publicUrl(storagePath)}?v=${System.currentTimeMillis()}"
        }.getOrNull()
    }

    suspend fun uploadVoiceClip(
        messageId: String,
        localFilePath: String
    ): String? = withContext(Dispatchers.IO) {
        val client = supabaseClient ?: return@withContext null
        val file = File(localFilePath)
        if (!file.exists() || file.length() <= 0L || file.length() > MAX_VOICE_BYTES) {
            return@withContext null
        }
        val safeId = sanitizePathSegment(messageId)
        val storagePath = "voice/$safeId.m4a"
        runCatching {
            val bytes = file.readBytes()
            val bucket = client.storage.from(MEDIA_BUCKET)
            bucket.upload(path = storagePath, data = bytes) {
                upsert = true
                contentType = ContentType("audio", "mp4")
            }
            bucket.publicUrl(storagePath)
        }.getOrNull()
    }

    suspend fun uploadChatPhoto(
        context: Context,
        messageId: String,
        uriString: String
    ): String? = withContext(Dispatchers.IO) {
        val client = supabaseClient ?: return@withContext null
        if (uriString.startsWith("https://")) return@withContext uriString
        val jpegBytes = compressImageUriToJpeg(context, uriString, maxSidePx = 1080, quality = 80)
            ?: return@withContext null
        val safeId = sanitizePathSegment(messageId)
        val storagePath = "chat/$safeId.jpg"
        runCatching {
            val bucket = client.storage.from(MEDIA_BUCKET)
            bucket.upload(path = storagePath, data = jpegBytes) {
                upsert = true
                contentType = ContentType.Image.JPEG
            }
            bucket.publicUrl(storagePath)
        }.getOrNull()
    }

    suspend fun uploadChatVideo(
        context: Context,
        messageId: String,
        uriString: String
    ): String? = withContext(Dispatchers.IO) {
        val client = supabaseClient ?: return@withContext null
        if (uriString.startsWith("https://")) return@withContext uriString
        val bytes = readUriBytesBounded(context, uriString, MAX_VIDEO_BYTES) ?: return@withContext null
        val safeId = sanitizePathSegment(messageId)
        val storagePath = "chat/$safeId.mp4"
        runCatching {
            val bucket = client.storage.from(MEDIA_BUCKET)
            bucket.upload(path = storagePath, data = bytes) {
                upsert = true
                contentType = ContentType.Video.MP4
            }
            bucket.publicUrl(storagePath)
        }.getOrNull()
    }

    // --- 2. Family Profiles (family_profiles) ---

    suspend fun fetchAllProfiles(): List<SupabaseProfileDto> = withContext(Dispatchers.IO) {
        val client = supabaseClient ?: return@withContext emptyList()
        runCatching {
            client.from(TABLE_PROFILES).select().decodeList<SupabaseProfileDto>()
        }.getOrDefault(emptyList())
    }

    suspend fun upsertProfile(dto: SupabaseProfileDto): Boolean = withContext(Dispatchers.IO) {
        val client = supabaseClient ?: return@withContext false
        runCatching {
            client.from(TABLE_PROFILES).upsert(dto)
            true
        }.getOrDefault(false)
    }

    // --- 3. Family Messages & Reactions (family_messages) ---

    suspend fun fetchRecentMessages(limit: Long = 150L): List<SupabaseFamilyMessageDto> =
        withContext(Dispatchers.IO) {
            val client = supabaseClient ?: return@withContext emptyList()
            runCatching {
                client.from(TABLE_MESSAGES).select {
                    order(column = "timestamp_ms", order = Order.DESCENDING)
                    limit(limit)
                }.decodeList<SupabaseFamilyMessageDto>()
            }.getOrDefault(emptyList())
        }

    suspend fun upsertFamilyMessage(dto: SupabaseFamilyMessageDto): Boolean =
        withContext(Dispatchers.IO) {
            val client = supabaseClient ?: return@withContext false
            runCatching {
                client.from(TABLE_MESSAGES).upsert(dto)
                true
            }.getOrDefault(false)
        }

    suspend fun updateRemoteMessageReaction(messageId: String, emoji: String?): Boolean =
        withContext(Dispatchers.IO) {
            val client = supabaseClient ?: return@withContext false
            runCatching {
                client.from(TABLE_MESSAGES).update({
                    set("reaction_emoji", emoji)
                }) {
                    filter {
                        eq("id", messageId)
                    }
                }
                true
            }.getOrDefault(false)
        }

    // --- 4. XOXO Game State (xoxo_games) ---

    suspend fun fetchXoxoGame(): XoxoGameState? = withContext(Dispatchers.IO) {
        val client = supabaseClient ?: return@withContext null
        runCatching {
            val dto = client.from(TABLE_XOXO).select {
                filter { eq("id", XOXO_MAIN_ID) }
                limit(1)
            }.decodeList<SupabaseXoxoGameDto>().firstOrNull() ?: return@runCatching null

            val board = runCatching { json.decodeFromString<List<String>>(dto.board_json) }
                .getOrDefault(List(9) { "" })
                .let { if (it.size == 9) it else List(9) { "" } }
            val winLine = runCatching { json.decodeFromString<List<Int>>(dto.winning_line_json) }
                .getOrDefault(emptyList())
            val px = runCatching { FamilyMemberId.valueOf(dto.player_x) }.getOrDefault(FamilyMemberId.MITHUN)
            val po = runCatching { FamilyMemberId.valueOf(dto.player_o) }.getOrDefault(FamilyMemberId.ZAYN)
            val turn = runCatching { XoxoMark.valueOf(dto.current_turn) }.getOrDefault(XoxoMark.X)
            val startMark = runCatching { XoxoMark.valueOf(dto.starting_mark) }.getOrDefault(XoxoMark.X)
            val winner = dto.winner_mark?.let { runCatching { XoxoMark.valueOf(it) }.getOrNull() }

            XoxoGameState(
                board = board,
                playerX = px,
                playerO = if (po == px) FamilyMemberId.entries.first { it != px } else po,
                currentTurn = turn,
                startingMark = startMark,
                winnerMark = winner,
                winningLine = winLine,
                isDraw = dto.is_draw,
                xScore = dto.x_score,
                oScore = dto.o_score,
                drawScore = dto.draw_score,
                roundNumber = dto.round_number.coerceAtLeast(1),
                playMode = XoxoPlayMode.LIVE_TWO_PHONES,
                lastUpdatedMs = dto.updated_at_ms
            )
        }.getOrNull()
    }

    suspend fun upsertXoxoGame(state: XoxoGameState): Boolean = withContext(Dispatchers.IO) {
        val client = supabaseClient ?: return@withContext false
        runCatching {
            val dto = SupabaseXoxoGameDto(
                id = XOXO_MAIN_ID,
                board_json = json.encodeToString(state.board),
                player_x = state.playerX.name,
                player_o = state.playerO.name,
                current_turn = state.currentTurn.name,
                starting_mark = state.startingMark.name,
                winner_mark = state.winnerMark?.name,
                winning_line_json = json.encodeToString(state.winningLine),
                is_draw = state.isDraw,
                x_score = state.xScore,
                o_score = state.oScore,
                draw_score = state.drawScore,
                round_number = state.roundNumber,
                updated_at_ms = state.lastUpdatedMs
            )
            client.from(TABLE_XOXO).upsert(dto)
            true
        }.getOrDefault(false)
    }

    // --- 5. Live Presence (family_presence) ---

    suspend fun fetchAllPresence(): List<SupabasePresenceDto> = withContext(Dispatchers.IO) {
        val client = supabaseClient ?: return@withContext emptyList()
        runCatching {
            client.from(TABLE_PRESENCE).select().decodeList<SupabasePresenceDto>()
        }.getOrDefault(emptyList())
    }

    suspend fun upsertPresence(member: FamilyMemberId, isOnline: Boolean, nowMs: Long): Boolean =
        withContext(Dispatchers.IO) {
            val client = supabaseClient ?: return@withContext false
            runCatching {
                client.from(TABLE_PRESENCE).upsert(
                    SupabasePresenceDto(
                        member_id = member.name,
                        is_online = isOnline,
                        last_seen_ms = nowMs
                    )
                )
                true
            }.getOrDefault(false)
        }

    // --- 6. Parent Grown-Up Logs (family_parent_logs) ---

    suspend fun fetchParentLogs(): List<SupabaseParentLogDto> = withContext(Dispatchers.IO) {
        val client = supabaseClient ?: return@withContext emptyList()
        runCatching {
            client.from(TABLE_PARENT_LOGS).select {
                order(column = "created_at_ms", order = Order.DESCENDING)
                limit(50)
            }.decodeList<SupabaseParentLogDto>()
        }.getOrDefault(emptyList())
    }

    suspend fun upsertParentLog(dto: SupabaseParentLogDto): Boolean = withContext(Dispatchers.IO) {
        val client = supabaseClient ?: return@withContext false
        runCatching {
            client.from(TABLE_PARENT_LOGS).upsert(dto)
            true
        }.getOrDefault(false)
    }

    // --- 7. Video Call Logs (family_call_logs) ---

    suspend fun fetchRecentCallLogs(limit: Long = 20L): List<SupabaseCallLogDto> =
        withContext(Dispatchers.IO) {
            val client = supabaseClient ?: return@withContext emptyList()
            runCatching {
                client.from(TABLE_CALL_LOGS).select {
                    order(column = "started_at_ms", order = Order.DESCENDING)
                    limit(limit)
                }.decodeList<SupabaseCallLogDto>()
            }.getOrDefault(emptyList())
        }

    suspend fun insertCallLog(dto: SupabaseCallLogDto): Boolean = withContext(Dispatchers.IO) {
        val client = supabaseClient ?: return@withContext false
        runCatching {
            client.from(TABLE_CALL_LOGS).upsert(dto)
            true
        }.getOrDefault(false)
    }

    // --- Safe Media Helpers ---

    private fun sanitizePathSegment(raw: String): String =
        raw.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(64).ifBlank { "item" }

    private fun compressImageUriToJpeg(
        context: Context,
        uriString: String,
        maxSidePx: Int,
        quality: Int
    ): ByteArray? = runCatching {
        val uri = Uri.parse(uriString)
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: File(uriString).takeIf { it.exists() }?.readBytes()
            ?: return@runCatching null
        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            ?: return@runCatching null
        val maxDim = maxOf(decoded.width, decoded.height).coerceAtLeast(1)
        val scaled = if (maxDim > maxSidePx) {
            val ratio = maxSidePx.toFloat() / maxDim.toFloat()
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * ratio).toInt().coerceAtLeast(1),
                (decoded.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else {
            decoded
        }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()
        out.toByteArray()
    }.getOrNull()

    private fun readUriBytesBounded(
        context: Context,
        uriString: String,
        maxBytes: Long
    ): ByteArray? = runCatching {
        val uri = Uri.parse(uriString)
        val stream = context.contentResolver.openInputStream(uri) ?: return@runCatching null
        stream.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                total += read
                if (total > maxBytes) return@runCatching null
                out.write(buffer, 0, read)
            }
            out.toByteArray()
        }
    }.getOrNull()

    companion object {
        private const val MEDIA_BUCKET = "family-media"
        private const val TABLE_PROFILES = "family_profiles"
        private const val TABLE_MESSAGES = "family_messages"
        private const val TABLE_XOXO = "xoxo_games"
        private const val TABLE_PRESENCE = "family_presence"
        private const val TABLE_PARENT_LOGS = "family_parent_logs"
        private const val TABLE_CALL_LOGS = "family_call_logs"
        private const val XOXO_MAIN_ID = "family_main"

        private const val MAX_VOICE_BYTES = 8L * 1024L * 1024L
        private const val MAX_VIDEO_BYTES = 25L * 1024L * 1024L
    }
}
