package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.ui.theme.MafithaRoseHalo
import com.example.ui.theme.MithunSkyHalo
import com.example.ui.theme.ZaynSunHalo
import kotlinx.serialization.Serializable

@Serializable
enum class FamilyMemberId(
    val displayName: String,
    val title: String,
    val isChild: Boolean
) {
    MITHUN("Mithun", "Papa", false),
    MAFITHA("Mafitha", "Mama", false),
    ZAYN("Zayn Adam", "Explorer", true)
}

fun FamilyMemberId.defaultAvatarRes(): Int = when (this) {
    FamilyMemberId.MITHUN -> R.drawable.img_avatar_mithun
    FamilyMemberId.MAFITHA -> R.drawable.img_avatar_mafitha
    FamilyMemberId.ZAYN -> R.drawable.img_avatar_zayn
}

fun FamilyMemberId.haloColor(): Color = when (this) {
    FamilyMemberId.MITHUN -> MithunSkyHalo
    FamilyMemberId.MAFITHA -> MafithaRoseHalo
    FamilyMemberId.ZAYN -> ZaynSunHalo
}

@Serializable
enum class BadgeElement(val label: String, val emoji: String) {
    ROCKET("Rocket", "🚀"),
    STARLIGHT("Starlight", "🌟"),
    DINO("Dino", "🦕"),
    HEARTBEAT("Heart", "💖"),
    COMPASS("Compass", "🧭"),
    CROWN("Crown", "👑")
}

@Serializable
enum class VoiceLanguage(val localeTag: String, val label: String) {
    ENGLISH("en-US", "English"),
    MALAYALAM("ml-IN", "മലയാളം (Malayalam)")
}

@Serializable
enum class ArFilterType(val label: String, val emoji: String) {
    NONE("Clear", "✨"),
    ASTRO_HELMET("Space Helmet", "👩‍🚀"),
    BUNNY_EARS("Bouncy Ears", "🐰"),
    STAR_CROWN("Star Crown", "👑"),
    RAINBOW_GLOW("Prism Aura", "🌈")
}

enum class AudioRoute {
    SPEAKER,
    BLUETOOTH,
    EARPIECE
}

@Serializable
enum class MessageType {
    TEXT,
    VOICE_AAC,
    VIDEO_MP4,
    PHOTO,
    HEARTBEAT_ALERT,
    DRAWING_SNAPSHOT
}

@Serializable
enum class DeliveryStatus {
    QUEUED_OFFLINE,
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

@Serializable
enum class MomentCategory(val title: String, val subtitle: String) {
    CONNECT("Connect", "Warm check-in together"),
    PLAY("Play", "Spontaneous silly spark"),
    REMEMBER("Remember", "Save a little story")
}

data class UserSessionState(
    val activeMember: FamilyMemberId? = null,
    val isAuthenticated: Boolean = false,
    val isDeviceTrusted: Boolean = true,
    val hardwareKeyAlias: String = "",
    val hardwareKeyFingerprint: String = "",
    val isParentalUnlocked: Boolean = false,
    val quietHoursEnabled: Boolean = false,
    val reducedMotionEnabled: Boolean = false,
    val darkModeOverride: Boolean? = null,
    val voiceLanguage: VoiceLanguage = VoiceLanguage.ENGLISH,
    val selectedThreadId: String = "family_group"
)

sealed interface CallPhase {
    data object Idle : CallPhase
    data class DialingOutgoing(val targetMembers: List<FamilyMemberId>) : CallPhase
    data class RingingIncoming(
        val caller: FamilyMemberId,
        val isThreeWay: Boolean,
        val roomName: String
    ) : CallPhase
    data object Active : CallPhase
    data class Reconnecting(val reason: String) : CallPhase
    data class Ended(val summary: String) : CallPhase
}

data class CallParticipantState(
    val memberId: FamilyMemberId,
    val isConnected: Boolean,
    val isSpeaking: Boolean,
    val audioLevel: Float = 0f,
    val isMuted: Boolean = false,
    val isCameraEnabled: Boolean = true,
    val activeFilter: ArFilterType = ArFilterType.NONE
)

data class EmojiBurstEvent(
    val id: Long,
    val emoji: String,
    val sender: FamilyMemberId,
    val startXFraction: Float
)

data class LiveStroke(
    val id: Long,
    val author: FamilyMemberId,
    val colorArgb: Long,
    val strokeWidthDp: Float,
    val points: List<Pair<Float, Float>>
)

data class CallState(
    val phase: CallPhase = CallPhase.Idle,
    val roomName: String = "kinnest-family-room",
    val participants: List<CallParticipantState> = emptyList(),
    val isLocalMicMuted: Boolean = false,
    val isLocalCameraOn: Boolean = true,
    val isFrontCamera: Boolean = true,
    val audioRoute: AudioRoute = AudioRoute.SPEAKER,
    val activeFilter: ArFilterType = ArFilterType.NONE,
    val showLiveDrawingOverlay: Boolean = false,
    val durationSeconds: Long = 0L,
    val networkQualityLabel: String = "H.264 • Opus AEC/NS/AGC",
    val activeEmojiBursts: List<EmojiBurstEvent> = emptyList(),
    val screenTapPulseMember: FamilyMemberId? = null,
    val recentCallLogs: List<CallLogEntry> = emptyList()
)

data class CallLogEntry(
    val id: String,
    val participantsLabel: String,
    val callTypeLabel: String,
    val isMissed: Boolean,
    val timestampLabel: String,
    val durationLabel: String
)

data class OfflineSyncState(
    val isOnline: Boolean = true,
    val pendingQueueCount: Int = 0,
    val failedQueueCount: Int = 0,
    val isSyncingNow: Boolean = false,
    val lastSyncedLabel: String = "Synced just now",
    val supabaseConfigured: Boolean = false,
    val liveKitConfigured: Boolean = false
)
