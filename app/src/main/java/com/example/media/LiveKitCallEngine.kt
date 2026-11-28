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
import kotlinx.coroutines.CoroutineExceptionHandler
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
 * LiveKit SFU Subsystem for Full-Duplex 1-on-1 & 3-Way Family Video and Voice Calls:
 * - Adaptive video streaming + Dynacast with automatic audio & video track subscription
 * - Clean per-call Room lifecycle so consecutive Voice <-> Video calls never hit stale EGL/tracks
 * - Deferred permission-aware mic/camera publishing (automatically publishes as soon as granted)
 * - Post-connect Android AudioManager route enforcement (Speaker, Bluetooth, Earpiece)
 * - DataChannel broadcasting for AR filters, emoji physics bursts, tap pulses, and live drawing
 */
class LiveKitCallEngine(private val context: Context) {

    private val coroutineErrorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.w("LiveKitCallEngine", "LiveKit background coroutine error", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineErrorHandler)
    private val json = Json { ignoreUnknownKeys = true }
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Volatile
    private var room: Room? = null
    private var roomEventsJob: Job? = null
    private var localMember: FamilyMemberId = FamilyMemberId.MITHUN
    private val callGeneration = AtomicLong(0L)

    @Volatile
    private var desiredMicEnabled: Boolean = true

    @Volatile
    private var desiredCameraEnabled: Boolean = true

    @Volatile
    private var currentAudioRoute: AudioRoute = AudioRoute.SPEAKER

    private val _activeRoom = MutableStateFlow<Room?>(null)
    val activeRoom: StateFlow<Room?> = _activeRoom.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isReconnecting = MutableStateFlow(false)
    val isReconnecting: StateFlow<Boolean> = _isReconnecting.asStateFlow()

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

    private val _mutedMembers = MutableStateFlow<Set<FamilyMemberId>>(emptySet())
    val mutedMembers: StateFlow<Set<FamilyMemberId>> = _mutedMembers.asStateFlow()

    private val _incomingDataPackets = MutableSharedFlow<LiveKitDataPacket>(extraBufferCapacity = 32)
    val incomingDataPackets: SharedFlow<LiveKitDataPacket> = _incomingDataPackets.asSharedFlow()

    fun isLiveKitEndpointConfigured(): Boolean = LiveKitTokenProvider.isLiveKitUrlConfigured()

    private fun createFreshRoom(): Room {
        val previous = room
        room = null
        if (previous != null) {
            scope.launch {
                runCatching { previous.disconnect() }
                runCatching { previous.release() }
            }
        }
        return LiveKit.create(
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
        enableCamera: Boolean = true,
        audioRoute: AudioRoute = AudioRoute.SPEAKER
    ): Result<Boolean> {
        val gen = callGeneration.incrementAndGet()
        localMember = member
        desiredMicEnabled = enableMic
        desiredCameraEnabled = enableCamera
        currentAudioRoute = audioRoute

        _isReconnecting.value = false
        _mutedMembers.value = if (!enableMic) setOf(member) else emptySet()
        configureCommunicationAudio(audioRoute)
        _connectionStatusLabel.value = if (enableCamera) "Connecting video call…" else "Connecting voice call…"

        val tokenResult = LiveKitTokenProvider.obtainRoomToken(member, roomName)
        if (callGeneration.get() != gen) {
            return Result.success(false)
        }

        if (tokenResult !is LiveKitTokenResult.Ready) {
            val reason = when (tokenResult) {
                is LiveKitTokenResult.MissingConfig -> tokenResult.reason
                is LiveKitTokenResult.Failed -> tokenResult.reason
            }
            Log.w("LiveKitCallEngine", "LiveKit token unavailable: $reason")
            _activeRoom.value = null
            _isLiveStreamActive.value = false
            _isConnected.value = false
            _connectionStatusLabel.value = if (tokenResult is LiveKitTokenResult.MissingConfig) {
                "Configure LiveKit Secrets for live audio/video"
            } else {
                "Waiting for family room connection…"
            }
            return Result.success(false)
        }

        val hasAudioPerm = hasAudioPermission()
        val hasCameraPerm = hasCameraPermission()
        val publishAudio = desiredMicEnabled && hasAudioPerm
        val publishCamera = desiredCameraEnabled && hasCameraPerm

        return runCatching {
            val lkRoom = createFreshRoom()
            if (callGeneration.get() != gen) {
                runCatching { lkRoom.disconnect() }
                runCatching { lkRoom.release() }
                return Result.success(false)
            }
            observeRoomEvents(lkRoom, gen)

            val connectOptions = ConnectOptions(
                autoSubscribe = true,
                audio = publishAudio,
                video = publishCamera
            )
            lkRoom.connect(
                url = BuildConfig.LIVEKIT_WS_URL.trim(),
                token = tokenResult.token,
                options = connectOptions
            )
            if (callGeneration.get() != gen) {
                runCatching { lkRoom.disconnect() }
                runCatching { lkRoom.release() }
                return Result.success(false)
            }

            // Re-check permissions AFTER connect in case user granted them while connecting
            val micNow = desiredMicEnabled && hasAudioPermission()
            val camNow = desiredCameraEnabled && hasCameraPermission()
            runCatching { lkRoom.localParticipant.setMicrophoneEnabled(micNow) }
            runCatching { lkRoom.localParticipant.setCameraEnabled(camNow) }

            // Re-enforce chosen audio route AFTER WebRTC AudioSwitch initializes
            configureCommunicationAudio(currentAudioRoute)
            scope.launch {
                delay(400L)
                if (callGeneration.get() == gen && _isConnected.value) {
                    configureCommunicationAudio(currentAudioRoute)
                }
            }

            _isConnected.value = true
            _isLiveStreamActive.value = true
            _isReconnecting.value = false
            syncRoomParticipantsAndTracks(lkRoom)
            true
        }.onFailure { err ->
            Log.w("LiveKitCallEngine", "Call connection error", err)
            if (callGeneration.get() == gen) {
                _isLiveStreamActive.value = false
                _isConnected.value = false
                _isReconnecting.value = false
                _connectionStatusLabel.value = "Couldn't connect — check internet & retry"
            }
        }
    }

    private fun observeRoomEvents(lkRoom: Room, gen: Long) {
        roomEventsJob?.cancel()
        roomEventsJob = scope.launch {
            lkRoom.events.collect { event ->
                if (callGeneration.get() != gen) return@collect
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
                        if (event is RoomEvent.TrackSubscribed) {
                            configureCommunicationAudio(currentAudioRoute)
                        }
                    }
                    is RoomEvent.ActiveSpeakersChanged -> {
                        val speakers = event.speakers.mapNotNull { parseMemberId(it.identity?.value) }.toSet()
                        _speakingMembers.value = speakers
                    }
                    is RoomEvent.Reconnecting -> {
                        _isReconnecting.value = true
                        _connectionStatusLabel.value = "Reconnecting call…"
                    }
                    is RoomEvent.Reconnected -> {
                        _isReconnecting.value = false
                        _isConnected.value = true
                        _isLiveStreamActive.value = true
                        configureCommunicationAudio(currentAudioRoute)
                        syncRoomParticipantsAndTracks(lkRoom)
                    }
                    is RoomEvent.Disconnected -> {
                        _isReconnecting.value = false
                        _isConnected.value = false
                        _isLiveStreamActive.value = false
                        _videoTracksByMember.value = emptyMap()
                        _connectedLiveMembers.value = emptySet()
                        _speakingMembers.value = emptySet()
                        _mutedMembers.value = emptySet()
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
        val mutedSet = mutableSetOf<FamilyMemberId>()

        val selfId = parseMemberId(lkRoom.localParticipant.identity?.value) ?: localMember
        liveSet.add(selfId)
        val localCamPub = lkRoom.localParticipant.getTrackPublication(Track.Source.CAMERA)
        val localTrack = localCamPub?.track as? VideoTrack
        if (localTrack != null && localCamPub.muted != true && desiredCameraEnabled) {
            trackMap[selfId] = localTrack
        }
        val localMicPub = lkRoom.localParticipant.getTrackPublication(Track.Source.MICROPHONE)
        if (!desiredMicEnabled || localMicPub == null || localMicPub.muted) {
            mutedSet.add(selfId)
        }

        lkRoom.remoteParticipants.values.forEach { remote ->
            val memberId = parseMemberId(remote.identity?.value) ?: return@forEach
            liveSet.add(memberId)
            val remoteCamPub = remote.getTrackPublication(Track.Source.CAMERA)
            val remoteTrack = remoteCamPub?.track as? VideoTrack
            if (remoteTrack != null && remoteCamPub.muted != true) {
                trackMap[memberId] = remoteTrack
            }
            val remoteMicPub = remote.getTrackPublication(Track.Source.MICROPHONE)
            if (remoteMicPub == null || remoteMicPub.muted) {
                mutedSet.add(memberId)
            }
        }

        _connectedLiveMembers.value = liveSet
        _videoTracksByMember.value = trackMap
        _mutedMembers.value = mutedSet
        if (_isLiveStreamActive.value && !_isReconnecting.value) {
            val count = liveSet.size
            val modeWord = if (trackMap.isNotEmpty()) "video" else "voice"
            _connectionStatusLabel.value = if (count >= 2) {
                "$count family members live ($modeWord)"
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

    private fun hasAudioPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    private fun hasCameraPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    suspend fun setMicrophoneEnabled(enabled: Boolean): Boolean {
        desiredMicEnabled = enabled
        if (enabled && !hasAudioPermission()) return false
        val activeRoom = room ?: return false
        return runCatching {
            activeRoom.localParticipant.setMicrophoneEnabled(enabled)
            if (enabled) {
                configureCommunicationAudio(currentAudioRoute)
            }
            syncRoomParticipantsAndTracks(activeRoom)
            true
        }.getOrDefault(false)
    }

    suspend fun setCameraEnabled(enabled: Boolean): Boolean {
        desiredCameraEnabled = enabled
        if (enabled && !hasCameraPermission()) return false
        val activeRoom = room ?: return false
        return runCatching {
            activeRoom.localParticipant.setCameraEnabled(enabled)
            syncRoomParticipantsAndTracks(activeRoom)
            true
        }.getOrDefault(false)
    }

    suspend fun switchCamera(): Boolean {
        return runCatching {
            val lkRoom = room ?: return false
            val cameraPub = lkRoom.localParticipant.getTrackPublication(Track.Source.CAMERA)
            val localTrack = cameraPub?.track as? LocalVideoTrack ?: return false
            localTrack.switchCamera()
            syncRoomParticipantsAndTracks(lkRoom)
            true
        }.getOrDefault(false)
    }

    @Suppress("DEPRECATION")
    fun configureCommunicationAudio(route: AudioRoute) {
        currentAudioRoute = route
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
                        if (bt != null) {
                            audioManager.setCommunicationDevice(bt)
                        } else {
                            val speaker = audioManager.availableCommunicationDevices
                                .firstOrNull { it.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                            if (speaker != null) audioManager.setCommunicationDevice(speaker)
                        }
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
        // 1. Detach Compose video renderers FIRST while EglBase is still valid
        _videoTracksByMember.value = emptyMap()
        _isLiveStreamActive.value = false
        _isConnected.value = false
        _isReconnecting.value = false
        _connectedLiveMembers.value = emptySet()
        _speakingMembers.value = emptySet()
        _mutedMembers.value = emptySet()

        // 2. Clear room reference immediately so any subsequent call gets a fresh Room instance
        val roomToDisconnect = room
        room = null
        _activeRoom.value = null

        scope.launch {
            delay(140L)
            runCatching { roomToDisconnect?.localParticipant?.setCameraEnabled(false) }
            runCatching { roomToDisconnect?.localParticipant?.setMicrophoneEnabled(false) }
            runCatching { roomToDisconnect?.disconnect() }
            runCatching { roomToDisconnect?.release() }
            if (callGeneration.get() == gen) {
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        audioManager.clearCommunicationDevice()
                    }
                    audioManager.mode = AudioManager.MODE_NORMAL
                }
            }
        }
    }

    companion object {
        const val DEFAULT_FAMILY_ROOM = "holymoly-family-room"
    }
}
