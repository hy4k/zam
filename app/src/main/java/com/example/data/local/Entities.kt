package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val memberId: String,
    val displayName: String,
    val roleLabel: String,
    val customPhotoUri: String?,
    val badgeElement: String,
    val haloColorHex: Long,
    val deviceKeyAlias: String,
    val devicePublicFingerprint: String,
    val isDeviceTrusted: Boolean,
    val biometricEnabled: Boolean
)

@Entity(tableName = "encrypted_messages")
data class EncryptedMessageEntity(
    @PrimaryKey val id: String,
    val threadId: String,
    val senderId: String,
    val recipientIdsCsv: String,
    val ciphertextPayload: String,
    val ivBase64: String,
    val decryptedPreview: String,
    val messageType: String,
    val mediaLocalPath: String?,
    val mediaRemoteUrl: String?,
    val reactionEmoji: String?,
    val deliveryStatus: String,
    val readByCsv: String,
    val timestampMs: Long
)

@Entity(tableName = "memory_capsules")
data class MemoryCapsuleEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val title: String,
    val caption: String,
    val mediaType: String,
    val mediaUri: String?,
    val unlockAtMs: Long?,
    val isVaultOfflinePinned: Boolean,
    val createdAtMs: Long
)

@Entity(tableName = "offline_sync_queue")
data class OfflineSyncQueueEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val payloadJson: String,
    val priority: String,
    val retryCount: Int,
    val status: String,
    val lastError: String?,
    val createdAtMs: Long
)

@Entity(tableName = "family_activities")
data class FamilyActivityEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val title: String,
    val subtitle: String,
    val categoryOrTag: String,
    val authorId: String,
    val votesCount: Int,
    val costEstimate: String?,
    val targetDateLabel: String?,
    val isCompleted: Boolean,
    val timestampMs: Long
)
