package com.example.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.core.content.ContextCompat
import com.example.data.model.VoiceLanguage
import java.io.File
import java.util.Locale

sealed interface StopRecordingResult {
    data object Idle : StopRecordingResult
    data object TooShort : StopRecordingResult
    data object Failed : StopRecordingResult
    data class Saved(val file: File, val durationMs: Long) : StopRecordingResult
}

/**
 * Handles Walkie-Talkie AAC voice clip recording (MediaRecorder),
 * playback (MediaPlayer), and native English + Malayalam (ml-IN) TextToSpeech guidance for Zayn Adam.
 */
class VoiceAndMediaCapture(private val context: Context) {

    private val recorderLock = Any()
    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var isRecording: Boolean = false
    private var recordingStartedAtMs: Long = 0L

    private var player: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        runCatching {
            tts = TextToSpeech(context.applicationContext) { status ->
                ttsReady = status == TextToSpeech.SUCCESS
            }
        }
    }

    fun startAacRecording(filePrefix: String = "walkie"): File? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        synchronized(recorderLock) {
            abortRecordingLocked()
            val dir = File(context.filesDir, "voice_clips").apply { mkdirs() }
            val file = File(dir, "${filePrefix}_${System.currentTimeMillis()}.m4a")
            return runCatching {
                val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }
                mr.setAudioSource(MediaRecorder.AudioSource.MIC)
                mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                mr.setAudioChannels(1)
                mr.setAudioEncodingBitRate(64_000)
                mr.setAudioSamplingRate(44_100)
                mr.setOutputFile(file.absolutePath)
                mr.prepare()
                mr.start()
                recorder = mr
                currentOutputFile = file
                isRecording = true
                recordingStartedAtMs = System.currentTimeMillis()
                file
            }.onFailure {
                abortRecordingLocked()
                runCatching { file.delete() }
            }.getOrNull()
        }
    }

    fun stopRecording(): StopRecordingResult {
        synchronized(recorderLock) {
            val mr = recorder ?: return StopRecordingResult.Idle
            val file = currentOutputFile
            if (!isRecording) {
                abortRecordingLocked()
                return StopRecordingResult.Idle
            }

            val elapsedMs = (System.currentTimeMillis() - recordingStartedAtMs).coerceAtLeast(0L)
            if (elapsedMs < MIN_RECORDING_DURATION_MS) {
                // Reset without calling stop() so MPEG4Writer never throws -1007 on quick taps
                abortRecordingLocked()
                return StopRecordingResult.TooShort
            }

            return runCatching {
                mr.stop()
                mr.release()
                recorder = null
                isRecording = false
                currentOutputFile = null
                if (file != null && file.exists() && file.length() > 0L) {
                    StopRecordingResult.Saved(file, elapsedMs)
                } else {
                    runCatching { file?.delete() }
                    StopRecordingResult.Failed
                }
            }.getOrElse {
                runCatching { mr.reset() }
                runCatching { mr.release() }
                recorder = null
                isRecording = false
                currentOutputFile = null
                runCatching { file?.delete() }
                StopRecordingResult.Failed
            }
        }
    }

    private fun abortRecordingLocked() {
        recorder?.let { mr ->
            runCatching { mr.reset() }
            runCatching { mr.release() }
        }
        recorder = null
        isRecording = false
        currentOutputFile?.let { file ->
            runCatching { if (file.exists()) file.delete() }
        }
        currentOutputFile = null
        recordingStartedAtMs = 0L
    }

    fun playVoiceFile(path: String?, onComplete: () -> Unit = {}) {
        if (path.isNullOrBlank()) {
            onComplete()
            return
        }
        val file = File(path)
        if (!file.exists() || file.length() == 0L) {
            onComplete()
            return
        }
        runCatching {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    it.release()
                    player = null
                    onComplete()
                }
                prepare()
                start()
            }
        }.onFailure {
            onComplete()
        }
    }

    fun speakGuidance(
        englishText: String,
        malayalamText: String,
        language: VoiceLanguage,
        quietHoursActive: Boolean
    ) {
        if (!ttsReady || quietHoursActive) return
        val engine = tts ?: return
        val targetLocale = if (language == VoiceLanguage.MALAYALAM) {
            Locale.forLanguageTag("ml-IN")
        } else {
            Locale.US
        }
        val langResult = engine.setLanguage(targetLocale)
        val spoken = if (
            language == VoiceLanguage.MALAYALAM &&
            langResult != TextToSpeech.LANG_MISSING_DATA &&
            langResult != TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            malayalamText
        } else {
            englishText
        }
        engine.setSpeechRate(0.95f)
        engine.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "kinnest_tts_${System.currentTimeMillis()}")
    }

    fun release() {
        synchronized(recorderLock) {
            abortRecordingLocked()
        }
        runCatching {
            player?.release()
            tts?.stop()
            tts?.shutdown()
        }
    }

    companion object {
        private const val MIN_RECORDING_DURATION_MS = 700L
    }
}
