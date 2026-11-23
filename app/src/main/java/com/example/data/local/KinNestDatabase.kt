package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BadgeElement
import com.example.data.model.FamilyMemberId
import com.example.security.DeviceKeystoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Database(
    entities = [
        UserProfileEntity::class,
        EncryptedMessageEntity::class,
        MemoryCapsuleEntity::class,
        OfflineSyncQueueEntity::class,
        FamilyActivityEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KinNestDatabase : RoomDatabase() {
    abstract fun kinNestDao(): KinNestDao

    companion object {
        @Volatile
        private var INSTANCE: KinNestDatabase? = null
        private val seedMutex = Mutex()

        fun getInstance(context: Context): KinNestDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): KinNestDatabase {
            return Room.databaseBuilder(
                appContext,
                KinNestDatabase::class.java,
                "kinnest_family_vault.db"
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            ensureSeeded(appContext, getInstance(appContext).kinNestDao())
                        }
                    }
                })
                .build()
        }

        suspend fun ensureSeeded(context: Context, dao: KinNestDao) {
            seedMutex.withLock {
                if (dao.countProfiles() == 0) {
                    seedInitialFamilyData(context, dao)
                }
            }
        }

        private suspend fun seedInitialFamilyData(context: Context, dao: KinNestDao) {
            val keystore = DeviceKeystoreManager(context)
            val now = System.currentTimeMillis()
            val mithunFp = keystore.ensureDeviceIdentityKey("kinnest_device_mithun")
            val mafithaFp = keystore.ensureDeviceIdentityKey("kinnest_device_mafitha")
            val zaynFp = keystore.ensureDeviceIdentityKey("kinnest_device_zayn")

            dao.upsertProfiles(
                listOf(
                    UserProfileEntity(
                        memberId = FamilyMemberId.MITHUN.name,
                        displayName = "Mithun",
                        roleLabel = "Papa",
                        customPhotoUri = null,
                        badgeElement = BadgeElement.COMPASS.name,
                        haloColorHex = 0xFF38BDF8,
                        deviceKeyAlias = "kinnest_device_mithun",
                        devicePublicFingerprint = mithunFp,
                        isDeviceTrusted = true,
                        biometricEnabled = true
                    ),
                    UserProfileEntity(
                        memberId = FamilyMemberId.MAFITHA.name,
                        displayName = "Mafitha",
                        roleLabel = "Mama",
                        customPhotoUri = null,
                        badgeElement = BadgeElement.STARLIGHT.name,
                        haloColorHex = 0xFFFB7185,
                        deviceKeyAlias = "kinnest_device_mafitha",
                        devicePublicFingerprint = mafithaFp,
                        isDeviceTrusted = true,
                        biometricEnabled = true
                    ),
                    UserProfileEntity(
                        memberId = FamilyMemberId.ZAYN.name,
                        displayName = "Zayn Adam",
                        roleLabel = "6yo Explorer",
                        customPhotoUri = null,
                        badgeElement = BadgeElement.ROCKET.name,
                        haloColorHex = 0xFFFBBF24,
                        deviceKeyAlias = "kinnest_device_zayn",
                        devicePublicFingerprint = zaynFp,
                        isDeviceTrusted = true,
                        biometricEnabled = false
                    )
                )
            )

            // Seed starter family activities & spaces
            dao.upsertActivities(
                listOf(
                    FamilyActivityEntity(
                        id = "moment_today_1",
                        kind = "MOMENT",
                        title = "Make a goofy 5-second alien sound for Zayn!",
                        subtitle = "Category: Play • Tap Walkie-Talkie or launch a 3-way call",
                        categoryOrTag = "PLAY",
                        authorId = FamilyMemberId.MAFITHA.name,
                        votesCount = 3,
                        costEstimate = null,
                        targetDateLabel = "Today",
                        isCompleted = false,
                        timestampMs = now
                    ),
                    FamilyActivityEntity(
                        id = "mission_1",
                        kind = "MISSION",
                        title = "Blue Cushion Treasure Hunt",
                        subtitle = "5-min indoor AR camera clue hunt: Find something round, soft, and blue!",
                        categoryOrTag = "AR_CLUE",
                        authorId = FamilyMemberId.MITHUN.name,
                        votesCount = 2,
                        costEstimate = "5 min",
                        targetDateLabel = "Living Room",
                        isCompleted = false,
                        timestampMs = now - 3600_000L
                    ),
                    FamilyActivityEntity(
                        id = "mission_2",
                        kind = "MISSION",
                        title = "Kitchen Spice Detective",
                        subtitle = "Snap a green leaf or cardamom pod with Mama & Papa on call!",
                        categoryOrTag = "MINI_PUZZLE",
                        authorId = FamilyMemberId.MAFITHA.name,
                        votesCount = 3,
                        costEstimate = "7 min",
                        targetDateLabel = "Kitchen",
                        isCompleted = false,
                        timestampMs = now - 7200_000L
                    ),
                    FamilyActivityEntity(
                        id = "travel_1",
                        kind = "TRAVEL_PIN",
                        title = "Munnar Misty Treehouse Resort",
                        subtitle = "Heated infinity pool + strawberry farm walk for Adam",
                        categoryOrTag = "RESORT",
                        authorId = FamilyMemberId.MAFITHA.name,
                        votesCount = 3,
                        costEstimate = "₹14,500 / night",
                        targetDateLabel = "In 18 days",
                        isCompleted = false,
                        timestampMs = now - 10_000L
                    ),
                    FamilyActivityEntity(
                        id = "travel_2",
                        kind = "TRAVEL_PIN",
                        title = "Kochi Sunset Houseboat & Gelato Run",
                        subtitle = "Backwater cruise + big splash deck",
                        categoryOrTag = "ROAD_TRIP",
                        authorId = FamilyMemberId.MITHUN.name,
                        votesCount = 2,
                        costEstimate = "₹9,200",
                        targetDateLabel = "Next Month",
                        isCompleted = false,
                        timestampMs = now - 20_000L
                    ),
                    FamilyActivityEntity(
                        id = "calendar_1",
                        kind = "CALENDAR",
                        title = "Zayn Adam's Reading Star Night",
                        subtitle = "Story Together: Chapter 2 + Malayalam bedtime rhyme",
                        categoryOrTag = "FAMILY_TIME",
                        authorId = FamilyMemberId.ZAYN.name,
                        votesCount = 3,
                        costEstimate = null,
                        targetDateLabel = "Tonight • 7:30 PM",
                        isCompleted = false,
                        timestampMs = now - 30_000L
                    ),
                    FamilyActivityEntity(
                        id = "reward_1",
                        kind = "REWARD",
                        title = "Read 3 Malayalam & English Story Pages",
                        subtitle = "Reward: +1 Rocket Fuel Star toward Zoo Sunday!",
                        categoryOrTag = "READING",
                        authorId = FamilyMemberId.ZAYN.name,
                        votesCount = 5,
                        costEstimate = "+25 Stars",
                        targetDateLabel = "4 / 5 Stars",
                        isCompleted = false,
                        timestampMs = now - 40_000L
                    ),
                    FamilyActivityEntity(
                        id = "study_1",
                        kind = "STUDY",
                        title = "Phonics & Space Counting Board",
                        subtitle = "Tap planets 1–10 and spell rocket words with Papa",
                        categoryOrTag = "LITERACY",
                        authorId = FamilyMemberId.MITHUN.name,
                        votesCount = 2,
                        costEstimate = "6 min",
                        targetDateLabel = "Level 2",
                        isCompleted = false,
                        timestampMs = now - 50_000L
                    ),
                    FamilyActivityEntity(
                        id = "parent_log_1",
                        kind = "PARENT_LOG",
                        title = "Who actually put the Lego bricks away?",
                        subtitle = "Spoiler: Neither of us. Zayn claims the cat moved them.",
                        categoryOrTag = "HOUSEHOLD_SATIRE",
                        authorId = FamilyMemberId.MAFITHA.name,
                        votesCount = 1,
                        costEstimate = "₹0 (emotional toll: priceless)",
                        targetDateLabel = "Papa owes bedtime duty",
                        isCompleted = false,
                        timestampMs = now - 60_000L
                    ),
                    FamilyActivityEntity(
                        id = "parent_log_2",
                        kind = "PARENT_LOG",
                        title = "Secret Ice Cream & Cold Coffee Fund",
                        subtitle = "Split 50/50 after Zayn falls asleep. Do not disclose to 6-year-old.",
                        categoryOrTag = "EXPENSE_SPLIT",
                        authorId = FamilyMemberId.MITHUN.name,
                        votesCount = 2,
                        costEstimate = "₹640 split (₹320 each)",
                        targetDateLabel = "Settled-ish",
                        isCompleted = true,
                        timestampMs = now - 70_000L
                    )
                )
            )

            // Seed starter Memory Capsules (one unlocked, one future birthday-locked)
            val oneYearMs = 365L * 24L * 3600L * 1000L
            dao.upsertCapsule(
                MemoryCapsuleEntity(
                    id = "capsule_1",
                    authorId = FamilyMemberId.ZAYN.name,
                    title = "Adam's Rocket Joke",
                    caption = "\"Papa, the moon is just cheese that forgot to go in the fridge!\"",
                    mediaType = "QUOTE_AUDIO",
                    mediaUri = null,
                    unlockAtMs = null,
                    isVaultOfflinePinned = true,
                    createdAtMs = now - 86_400_000L
                )
            )
            dao.upsertCapsule(
                MemoryCapsuleEntity(
                    id = "capsule_2",
                    authorId = FamilyMemberId.MAFITHA.name,
                    title = "To Zayn on Your 7th Birthday",
                    caption = "Time-locked surprise letter & voice recording from Mama & Papa.",
                    mediaType = "TIME_CAPSULE",
                    mediaUri = null,
                    unlockAtMs = now + oneYearMs,
                    isVaultOfflinePinned = true,
                    createdAtMs = now - 43_200_000L
                )
            )

            // Seed initial encrypted welcome message
            val encryptedWelcome = keystore.encryptMessageForLocalVault(
                "Hey Zayn & Mafitha! Who's ready for a 3-way rocket call tonight? 🚀"
            )
            dao.upsertMessage(
                EncryptedMessageEntity(
                    id = "msg_welcome_1",
                    threadId = "family_group",
                    senderId = FamilyMemberId.MITHUN.name,
                    recipientIdsCsv = "MITHUN,MAFITHA,ZAYN",
                    ciphertextPayload = encryptedWelcome.first,
                    ivBase64 = encryptedWelcome.second,
                    decryptedPreview = "Hey Zayn & Mafitha! Who's ready for a 3-way rocket call tonight? 🚀",
                    messageType = "TEXT",
                    mediaLocalPath = null,
                    mediaRemoteUrl = null,
                    reactionEmoji = "💖",
                    deliveryStatus = "READ",
                    readByCsv = "MITHUN,MAFITHA,ZAYN",
                    timestampMs = now - 120_000L
                )
            )
        }
    }
}
