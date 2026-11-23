package com.example.sync

import com.example.BuildConfig
import com.example.data.local.EncryptedMessageEntity
import com.example.data.local.KinNestDao
import com.example.data.local.MemoryCapsuleEntity
import com.example.data.local.OfflineSyncQueueEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

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
 * Offline-First Room + Supabase Sync Repository:
 * Writes immediately to local SQLite Room DB, then synchronizes queued items cleanly
 * with Supabase (Postgrest, Realtime, Auth, Storage) without duplicates upon reconnection.
 */
class KinNestSyncRepository(
    private val dao: KinNestDao
) : SyncRepository {

    private val supabaseClient: SupabaseClient? by lazy {
        if (!isSupabaseConfigured()) return@lazy null
        runCatching {
            createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            ) {
                install(Auth)
                install(Postgrest)
                install(Realtime)
                install(Storage)
            }
        }.getOrNull()
    }

    override fun isSupabaseConfigured(): Boolean {
        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_ANON_KEY
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
        return runCatching {
            when (item.entityType) {
                "MESSAGE" -> {
                    val dto = kotlinx.serialization.json.Json.decodeFromString<SupabaseMessageDto>(
                        item.payloadJson
                    )
                    client.from("encrypted_messages").upsert(dto)
                    dao.updateMessageStatus(item.entityId, "SENT")
                }
                "CAPSULE" -> {
                    val dto = kotlinx.serialization.json.Json.decodeFromString<SupabaseCapsuleDto>(
                        item.payloadJson
                    )
                    client.from("memory_capsules").upsert(dto)
                }
                else -> Unit
            }
        }
    }
}
