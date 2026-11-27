package com.example.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.data.model.ArFilterType
import com.example.data.model.AudioRoute
import com.example.data.model.FamilyMemberId
import io.livekit.android.ConnectOptions
import io.livekit.android.LiveKit
import io.livekit.android.RoomOptions
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.CoroutineScope
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
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicLong

@Serializable
sealed interface LiveKitDataPacket {
    @Serializable
    data class EmojiBurstPacket(
        val senderId: String,
        val emoji: String,
        val xFraction: Float
    ) : LiveKitDataPacket

    @Serializable
    data class ArFilterPacket(
        val senderId: String,
        val filterName: String
    ) : LiveKitDataPacket

    @Serializable
    data class ScreenTapPulsePacket(
        val senderId: String
    ) : LiveKitDataPacket

    @Serializable
    data class DrawingStrokePacket(
        val strokeId: Long,
        val senderId: String,
        val colorArgb: Long,
        val strokeWidthDp: Float,
        val points: List<Pair<Float, Float>>
    ) : LiveKitDataPacket
}

/**
 * LiveKit SFU Subsystem:
 * - Adaptive video streaming + Dynacast for simultaneous 3-way full-duplex calling
 * - Hardware-accelerated H.264/VP9 video & Opus audio with Android AEC, NS, and AGC
 * - Real local & remote VideoTrack exposure for TextureViewRenderer playback
 * - DataChannel broadcasting for AR filters, emoji physics bursts, tap pulses, and live drawing
 */
class LiveKitCallEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var room: Room? = null
    private var roomEventsJob: Job? = null
    private var localMember: FamilyMemberId = FamilyMemberId.MITHUN
    private val callGeneration = AtomicLong(0L)

    private val _activeRoom = MutableStateFlow<Room?>(null)
    val activeRoom: StateFlow<Room?> = _activeRoom.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isLiveStreamActive = MutableStateFlow(false)
    val isLiveStreamActive: StateFlow<Boolean> = _isLiveStreamActive.asStateFlow()

    private val _connectionStatusLabel = MutableStateFlow("Ready to call")
    val connectionStatusLabel: StateFlow<String> = _connectionStatusLabel.asStateFlow()

    private val _videoTracksByMember = MutableStateFlow<Map<FamilyMemberId, VideoTrack>>(emptyMap())
    val videoTracksByMember: StateFlow<Map<FamilyMemberId, VideoTrack>> = _videoTracksByMember.asStateFlow()

    private val _connectedLiveMembers = MutableStateFlow<Set<FamilyMemberId>>(emptySet())
    val connectedLiveMembers: StateFlow<Set<FamilyMemberId>> = _connectedLiveMembers.asStateFlow()

    private val _speakingMembers = MutableStateFlow<Set<FamilyMemberId>>(emptySet())
    val speakingMembers: StateFlow<Set<FamilyMemberId>> = _speakingMembers.asStateFlow()

    private val _incomingDataPackets = MutableSharedFlow<LiveKitDataPacket>(extraBufferCapacity = 32)
    val incomingDataPackets: SharedFlow<LiveKitDataPacket> = _incomingDataPackets.asSharedFlow()

    fun isLiveKitEndpointConfigured(): Boolean = LiveKitTokenProvider.isLiveKitUrlConfigured()

    fun getOrCreateRoom(): Room {
        return room ?: LiveKit.create(
            appContext = context.applicationContext,
            options = RoomOptions(
                adaptiveStream = true,
                dynacast = true
            )
        ).also { created ->
            room = created
            _activeRoom.value = created
        }
    }

    suspend fun connectToFamilyRoom(
        member: FamilyMemberId,
        roomName: String = DEFAULT_FAMILY_ROOM,
        enableMic: Boolean = true,
        enableCamera: Boolean = true
    ): Result<Boolean> {
        callGeneration.incrementAndGet()
        localMember = member
        configureCommunicationAudio(AudioRoute.SPEAKER)
        _connectionStatusLabel.value = "Connecting your call…"

        if (LiveKitTokenProvider.isLiveKitUrlConfigured()) {
            // Claim room immediately so CameraX offline fallback does not contend with WebRTC camera/EGL
            getOrCreateRoom()
        }

        val tokenResult = LiveKitTokenProvider.obtainRoomToken(member, roomName)
        if (tokenResult !is LiveKitTokenResult.Ready) {
            val reason = when (tokenResult) {
                is LiveKitTokenResult.MissingConfig -> tokenResult.reason
                is LiveKitTokenResult.Failed -> tokenResult.reason
                else -> "Token unavailable"
            }
            Log.w("LiveKitCallEngine", "Falling back to preview: $reason")
            _activeRoom.value = null
            _isLiveStreamActive.value = false
            _isConnected.value = true
            _connectionStatusLabel.value = "Waiting for family to join…"
            return Result.success(false)
        }

        val hasAudioPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val hasCameraPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val publishAudio = enableMic && hasAudioPerm
        val publishCamera = enableCamera && hasCameraPerm

        return runCatching {
            val lkRoom = getOrCreateRoom()
            observeRoomEvents(lkRoom)

            val connectOptions = ConnectOptions(
                audio = publishAudio,
                video = publishCamera
            )
            lkRoom.connect(
                url = BuildConfig.LIVEKIT_WS_URL.trim(),
                token = tokenResult.token,
                options = connectOptions
            )
            if (publishAudio) {
                runCatching { lkRoom.localParticipant.setMicrophoneEnabled(true) }
            }
            if (publishCamera) {
                runCatching { lkRoom.localParticipant.setCameraEnabled(true) }
            }

            _isConnected.value = true
            _isLiveStreamActive.value = true
            syncRoomParticipantsAndTracks(lkRoom)
            true
        }.onFailure { err ->
            Log.w("LiveKitCallEngine", "Call connection error", err)
            _isLiveStreamActive.value = false
            _isConnected.value = false
            _connectionStatusLabel.value = "Couldn't connect — please check internet"
        }
    }

    private fun observeRoomEvents(lkRoom: Room) {
        roomEventsJob?.cancel()
        roomEventsJob = scope.launch {
            lkRoom.events.collect { event ->
                when (event) {
                    is RoomEvent.DataReceived -> {
                        val raw = event.data.decodeToString()
                        runCatching {
                            json.decodeFromString<LiveKitDataPacket>(raw)
                        }.onSuccess { packet ->
                            _incomingDataPackets.tryEmit(packet)
                        }
                    }
                    is RoomEvent.ParticipantConnected,
                    is RoomEvent.ParticipantDisconnected,
                    is RoomEvent.TrackSubscribed,
                    is RoomEvent.TrackUnsubscribed,
                    is RoomEvent.TrackPublished,
                    is RoomEvent.TrackUnpublished,
                    is RoomEvent.TrackMuted,
                    is RoomEvent.TrackUnmuted -> {
                        syncRoomParticipantsAndTracks(lkRoom)
                    }
                    is RoomEvent.ActiveSpeakersChanged -> {
                        val speakers = event.speakers.mapNotNull { parseMemberId(it.identity?.value) }.toSet()
                        _speakingMembers.value = speakers
                    }
                    is RoomEvent.Reconnecting -> {
                        _connectionStatusLabel.value = "Reconnecting call…"
                    }
                    is RoomEvent.Reconnected -> {
                        _isConnected.value = true
                        _isLiveStreamActive.value = true
                        syncRoomParticipantsAndTracks(lkRoom)
                    }
                    is RoomEvent.Disconnected -> {
                        _isConnected.value = false
                        _isLiveStreamActive.value = false
                        _videoTracksByMember.value = emptyMap()
                        _connectedLiveMembers.value = emptySet()
                        _speakingMembers.value = emptySet()
                        _connectionStatusLabel.value = "Call disconnected"
                    }
                    else -> {
                        syncRoomParticipantsAndTracks(lkRoom)
                    }
                }
            }
        }
    }

    private fun syncRoomParticipantsAndTracks(lkRoom: Room) {
        val trackMap = mutableMapOf<FamilyMemberId, VideoTrack>()
        val liveSet = mutableSetOf<FamilyMemberId>()

        val selfId = parseMemberId(lkRoom.localParticipant.identity?.value) ?: localMember
        liveSet.add(selfId)
        val localPub = lkRoom.localParticipant.getTrackPublication(Track.Source.CAMERA)
        val localTrack = localPub?.track as? VideoTrack
        if (localTrack != null && localPub.muted != true) {
            trackMap[selfId] = localTrack
        }

        lkRoom.remoteParticipants.values.forEach { remote ->
            val memberId = parseMemberId(remote.identity?.value) ?: return@forEach
            liveSet.add(memberId)
            val remotePub = remote.getTrackPublication(Track.Source.CAMERA)
            val remoteTrack = remotePub?.track as? VideoTrack
            if (remoteTrack != null && remotePub.muted != true) {
                trackMap[memberId] = remoteTrack
            }
        }

        _connectedLiveMembers.value = liveSet
        _videoTracksByMember.value = trackMap
        if (_isLiveStreamActive.value) {
            val count = liveSet.size
            _connectionStatusLabel.value = if (count >= 2) {
                "$count family members in the call"
            } else {
                "Connected • Waiting for family to join…"
            }
        }
    }

    private fun parseMemberId(identity: String?): FamilyMemberId? {
        if (identity.isNullOrBlank()) return null
        return FamilyMemberId.entries.firstOrNull {
            identity.equals(it.name, ignoreCase = true) ||
                identity.startsWith(it.name, ignoreCase = true)
        }
    }

    suspend fun setMicrophoneEnabled(enabled: Boolean) {
        if (enabled) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) return
        }
        runCatching {
            room?.localParticipant?.setMicrophoneEnabled(enabled)
            room?.let { syncRoomParticipantsAndTracks(it) }
        }
    }

    suspend fun setCameraEnabled(enabled: Boolean) {
        if (enabled) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) return
        }
        runCatching {
            room?.localParticipant?.setCameraEnabled(enabled)
            room?.let { syncRoomParticipantsAndTracks(it) }
        }
    }

    suspend fun switchCamera() {
        runCatching {
            val lkRoom = room ?: return
            val cameraPub = lkRoom.localParticipant.getTrackPublication(Track.Source.CAMERA)
            val localTrack = cameraPub?.track as? LocalVideoTrack
            localTrack?.switchCamera()
        }
    }

    @Suppress("DEPRECATION")
    fun configureCommunicationAudio(route: AudioRoute) {
        runCatching {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            when (route) {
                AudioRoute.SPEAKER -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val speaker = audioManager.availableCommunicationDevices
                            .firstOrNull { it.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                        if (speaker != null) audioManager.setCommunicationDevice(speaker)
                    } else {
                        audioManager.isBluetoothScoOn = false
                        audioManager.isSpeakerphoneOn = true
                    }
                }
                AudioRoute.BLUETOOTH -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val bt = audioManager.availableCommunicationDevices.firstOrNull {
                            it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                                it.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET
                        }
                        if (bt != null) audioManager.setCommunicationDevice(bt)
                    } else {
                        audioManager.isSpeakerphoneOn = false
                        audioManager.startBluetoothSco()
                        audioManager.isBluetoothScoOn = true
                    }
                }
                AudioRoute.EARPIECE -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val ear = audioManager.availableCommunicationDevices
                            .firstOrNull { it.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
                        if (ear != null) audioManager.setCommunicationDevice(ear)
                    } else {
                        audioManager.isBluetoothScoOn = false
                        audioManager.isSpeakerphoneOn = false
                    }
                }
            }
        }
    }

    suspend fun broadcastEmojiBurst(sender: FamilyMemberId, emoji: String, xFraction: Float) {
        val packet = LiveKitDataPacket.EmojiBurstPacket(sender.name, emoji, xFraction)
        publishDataPacket(packet)
    }

    suspend fun broadcastArFilter(sender: FamilyMemberId, filter: ArFilterType) {
        val packet = LiveKitDataPacket.ArFilterPacket(sender.name, filter.name)
        publishDataPacket(packet)
    }

    suspend fun broadcastTapPulse(sender: FamilyMemberId) {
        val packet = LiveKitDataPacket.ScreenTapPulsePacket(sender.name)
        publishDataPacket(packet)
    }

    suspend fun broadcastDrawingStroke(
        strokeId: Long,
        sender: FamilyMemberId,
        colorArgb: Long,
        strokeWidthDp: Float,
        points: List<Pair<Float, Float>>
    ) {
        val packet = LiveKitDataPacket.DrawingStrokePacket(
            strokeId = strokeId,
            senderId = sender.name,
            colorArgb = colorArgb,
            strokeWidthDp = strokeWidthDp,
            points = points
        )
        publishDataPacket(packet)
    }

    private suspend fun publishDataPacket(packet: LiveKitDataPacket) {
        val bytes = json.encodeToString(packet).toByteArray(Charsets.UTF_8)
        runCatching {
            room?.localParticipant?.publishData(bytes)
        }
    }

    fun disconnect() {
        val gen = callGeneration.incrementAndGet()
        roomEventsJob?.cancel()
        roomEventsJob = null
        // 1. Clear video tracks FIRST so Compose unmounts SurfaceViewRenderers while EglBase is still valid
        _videoTracksByMember.value = emptyMap()
        _isLiveStreamActive.value = false
        _isConnected.value = false
        _connectedLiveMembers.value = emptySet()
        _speakingMembers.value = emptySet()

        val roomToDisconnect = room
        scope.launch {
            // Give Compose a brief window to detach renderers from tracks before shutting down camera/EGL
            delay(120L)
            if (callGeneration.get() != gen) return@launch
            runCatching { roomToDisconnect?.localParticipant?.setCameraEnabled(false) }
            runCatching { roomToDisconnect?.localParticipant?.setMicrophoneEnabled(false) }
            runCatching { roomToDisconnect?.disconnect() }
            if (callGeneration.get() == gen) {
                _activeRoom.value = null
            }
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    audioManager.clearCommunicationDevice()
                }
                audioManager.mode = AudioManager.MODE_NORMAL
            }
        }
    }

    companion object {
        const val DEFAULT_FAMILY_ROOM = "holymoly-family-room"
    }
}
