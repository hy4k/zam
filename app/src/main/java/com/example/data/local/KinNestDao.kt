package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KinNestDao {

    @Query("SELECT * FROM user_profiles ORDER BY CASE memberId WHEN 'MITHUN' THEN 0 WHEN 'MAFITHA' THEN 1 ELSE 2 END")
    fun observeProfiles(): Flow<List<UserProfileEntity>>

    @Query("SELECT COUNT(*) FROM user_profiles")
    suspend fun countProfiles(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfiles(profiles: List<UserProfileEntity>)

    @Query("SELECT * FROM encrypted_messages WHERE threadId = :threadId ORDER BY timestampMs ASC")
    fun observeMessagesByThread(threadId: String): Flow<List<EncryptedMessageEntity>>

    @Query("SELECT * FROM encrypted_messages ORDER BY timestampMs DESC")
    fun observeAllMessages(): Flow<List<EncryptedMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(message: EncryptedMessageEntity)

    @Query("UPDATE encrypted_messages SET reactionEmoji = :emoji WHERE id = :messageId")
    suspend fun updateMessageReaction(messageId: String, emoji: String?)

    @Query("UPDATE encrypted_messages SET deliveryStatus = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    @Query("SELECT * FROM memory_capsules ORDER BY createdAtMs DESC")
    fun observeMemoryCapsules(): Flow<List<MemoryCapsuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCapsule(capsule: MemoryCapsuleEntity)

    @Query("UPDATE memory_capsules SET isVaultOfflinePinned = :pinned WHERE id = :capsuleId")
    suspend fun updateCapsuleOfflinePin(capsuleId: String, pinned: Boolean)

    @Query("SELECT * FROM offline_sync_queue ORDER BY createdAtMs ASC")
    fun observeSyncQueue(): Flow<List<OfflineSyncQueueEntity>>

    @Query("SELECT * FROM offline_sync_queue WHERE status != 'SYNCED' ORDER BY createdAtMs ASC")
    suspend fun getPendingSyncItems(): List<OfflineSyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueSyncItem(item: OfflineSyncQueueEntity)

    @Query("UPDATE offline_sync_queue SET status = :status, retryCount = :retryCount, lastError = :lastError WHERE id = :id")
    suspend fun updateSyncItemStatus(id: String, status: String, retryCount: Int, lastError: String?)

    @Query("DELETE FROM offline_sync_queue WHERE status = 'SYNCED'")
    suspend fun pruneSyncedItems()

    @Query("SELECT * FROM family_activities WHERE kind = :kind ORDER BY timestampMs DESC")
    fun observeActivitiesByKind(kind: String): Flow<List<FamilyActivityEntity>>

    @Query("SELECT * FROM family_activities ORDER BY timestampMs DESC")
    fun observeAllActivities(): Flow<List<FamilyActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivity(activity: FamilyActivityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivities(activities: List<FamilyActivityEntity>)

    @Query("UPDATE family_activities SET votesCount = votesCount + 1 WHERE id = :id")
    suspend fun incrementActivityVote(id: String)

    @Query("UPDATE family_activities SET isCompleted = :completed WHERE id = :id")
    suspend fun setActivityCompleted(id: String, completed: Boolean)

    @Query("SELECT * FROM daily_comic_photos ORDER BY dateKey DESC")
    fun observeDailyComicPhotos(): Flow<List<DailyComicPhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyComicPhoto(photo: DailyComicPhotoEntity)

    @Query("UPDATE daily_comic_photos SET reactionEmoji = :emoji WHERE dateKey = :dateKey")
    suspend fun updateDailyComicReaction(dateKey: String, emoji: String?)
}
