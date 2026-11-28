package com.example.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.AudioRoute
import com.example.data.model.FamilyMemberId
import kotlinx.coroutines.CompletableDeferred
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnectionFactory
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Modular WebRTC local media manager handling:
 * - Shared hardware [EglBase] & [PeerConnectionFactory] lifecycle
 * - Full-duplex hardware AEC + Noise Suppression via [JavaAudioDeviceModule]
 * - Local microphone capture & mute/unmute
 * - Camera2/Camera1 video capture (640x480 @ 24fps) & front/back switching
 * - Android [AudioManager] communication audio routing (Speaker / Bluetooth / Earpiece)
 */
class WebRtcMediaManager(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val rootEglBase: EglBase by lazy { EglBase.create() }
    val eglBaseContext: EglBase.Context
        get() = rootEglBase.eglBaseContext

    @Volatile
    private var factory: PeerConnectionFactory? = null

    @Volatile
    var isFrontCamera: Boolean = true
        private set

    @Volatile
    var currentAudioRoute: AudioRoute = AudioRoute.SPEAKER
        private set

    private var audioSource: AudioSource? = null
    @Volatile
    var localAudioTrack: AudioTrack? = null
        private set

    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var videoSource: VideoSource? = null
    @Volatile
    var localVideoTrack: VideoTrack? = null
        private set

    fun ensureFactory(): PeerConnectionFactory {
        factory?.let { return it }
        if (webrtcInitialized.compareAndSet(false, true)) {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(appContext)
                    .setEnableInternalTracer(false)
                    .createInitializationOptions()
            )
        }
        val audioDeviceModule = JavaAudioDeviceModule.builder(appContext)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()

        val encoderFactory = DefaultVideoEncoderFactory(
            rootEglBase.eglBaseContext,
            /* enableIntelVp8Encoder = */ true,
            /* enableH264HighProfile = */ true
        )
        val decoderFactory = DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)

        return PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
            .also {
                factory = it
                audioDeviceModule.release()
            }
    }

    fun hasAudioPermission(): Boolean = ContextCompat.checkSelfPermission(
        appContext,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    fun hasCameraPermission(): Boolean = ContextCompat.checkSelfPermission(
        appContext,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    /**
     * Initializes local audio track if microphone permission is granted.
     * Returns true if a brand-new [AudioTrack] was created.
     */
    fun ensureLocalAudio(member: FamilyMemberId, enabled: Boolean): Boolean {
        if (!hasAudioPermission()) return false
        var created = false
        if (localAudioTrack == null) {
            val pcFactory = ensureFactory()
            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            }
            audioSource = pcFactory.createAudioSource(constraints)
            localAudioTrack = pcFactory.createAudioTrack("holymoly_audio_${member.name}", audioSource)
            created = true
        }
        localAudioTrack?.setEnabled(enabled)
        return created
    }

    /**
     * Initializes local camera video capture if camera permission is granted and [enabled] is true.
     * Returns true if a brand-new [VideoTrack] was created.
     */
    fun ensureLocalVideo(member: FamilyMemberId, enabled: Boolean, resetFrontCamera: Boolean = false): Boolean {
        if (resetFrontCamera) isFrontCamera = true
        if (!enabled || !hasCameraPermission()) {
            localVideoTrack?.setEnabled(false)
            return false
        }
        if (localVideoTrack != null) {
            localVideoTrack?.setEnabled(true)
            return false
        }
        val pcFactory = ensureFactory()
        val capturer = createCameraCapturer(preferFront = isFrontCamera) ?: return false
        val helper = SurfaceTextureHelper.create("HolyMolyCaptureThread", rootEglBase.eglBaseContext)
        val source = pcFactory.createVideoSource(capturer.isScreencast)
        capturer.initialize(helper, appContext, source.capturerObserver)
        runCatching {
            // 640x480 @ 24fps gives crisp, smooth, low-heat video across 3-way mobile mesh calls
            capturer.startCapture(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_FPS)
        }.onFailure { err ->
            Log.w(TAG, "Camera startCapture failed", err)
        }
        val track = pcFactory.createVideoTrack("holymoly_video_${member.name}", source)
        track.setEnabled(true)

        surfaceTextureHelper = helper
        videoCapturer = capturer
        videoSource = source
        localVideoTrack = track
        return true
    }

    fun setLocalAudioEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun setLocalVideoEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    suspend fun switchCamera(): Boolean {
        val capturer = videoCapturer ?: return false
        val done = CompletableDeferred<Boolean>()
        runCatching {
            capturer.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
                override fun onCameraSwitchDone(isFront: Boolean) {
                    isFrontCamera = isFront
                    done.complete(true)
                }

                override fun onCameraSwitchError(errorDescription: String?) {
                    Log.w(TAG, "Camera switch error: $errorDescription")
                    done.complete(false)
                }
            })
        }.onFailure { done.complete(false) }
        return done.await()
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
                            .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                        if (speaker != null) audioManager.setCommunicationDevice(speaker)
                    } else {
                        audioManager.isBluetoothScoOn = false
                        audioManager.isSpeakerphoneOn = true
                    }
                }
                AudioRoute.BLUETOOTH -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val bt = audioManager.availableCommunicationDevices.firstOrNull {
                            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                        }
                        if (bt != null) {
                            audioManager.setCommunicationDevice(bt)
                        } else {
                            val speaker = audioManager.availableCommunicationDevices
                                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
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
                            .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
                        if (ear != null) audioManager.setCommunicationDevice(ear)
                    } else {
                        audioManager.isBluetoothScoOn = false
                        audioManager.isSpeakerphoneOn = false
                    }
                }
            }
        }
    }

    fun restoreNormalAudio() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            }
            audioManager.mode = AudioManager.MODE_NORMAL
        }
    }

    fun releaseLocalMedia() {
        runCatching { videoCapturer?.stopCapture() }
        runCatching { videoCapturer?.dispose() }
        videoCapturer = null

        runCatching { localVideoTrack?.dispose() }
        localVideoTrack = null

        runCatching { videoSource?.dispose() }
        videoSource = null

        runCatching { surfaceTextureHelper?.stopListening() }
        runCatching { surfaceTextureHelper?.dispose() }
        surfaceTextureHelper = null

        runCatching { localAudioTrack?.dispose() }
        localAudioTrack = null

        runCatching { audioSource?.dispose() }
        audioSource = null
    }

    private fun createCameraCapturer(preferFront: Boolean): CameraVideoCapturer? {
        val enumerator: CameraEnumerator = if (Camera2Enumerator.isSupported(appContext)) {
            Camera2Enumerator(appContext)
        } else {
            Camera1Enumerator(true)
        }
        val names = enumerator.deviceNames
        val preferred = names.firstOrNull { name ->
            if (preferFront) enumerator.isFrontFacing(name) else enumerator.isBackFacing(name)
        } ?: names.firstOrNull() ?: return null
        return enumerator.createCapturer(preferred, null)
    }

    companion object {
        private const val TAG = "WebRtcMediaManager"
        private const val VIDEO_WIDTH = 640
        private const val VIDEO_HEIGHT = 480
        private const val VIDEO_FPS = 24
        private val webrtcInitialized = AtomicBoolean(false)
    }
}
