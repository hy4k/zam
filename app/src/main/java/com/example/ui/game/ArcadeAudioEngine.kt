package com.example.ui.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Lightweight, zero-asset procedural Web-Audio-style PCM synth engine for Holy Moly ! games.
 * Synthesizes soft warm clay-marimba tones, pentatonic catch chimes, and cartoon bump effects
 * off the main thread with safe mute support.
 */
object ArcadeAudioEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private const val SAMPLE_RATE = 22050

    @Volatile
    var isMuted: Boolean = false

    // Pentatonic C-major scale (Hz) for joyful rising combo chimes
    private val pentatonicHz = doubleArrayOf(523.25, 587.33, 659.25, 783.99, 880.0, 1046.5, 1174.66, 1318.51)

    fun playStarCatch(combo: Int) {
        if (isMuted) return
        val base = pentatonicHz[combo.coerceIn(0, pentatonicHz.lastIndex)]
        playToneSequence(
            notes = listOf(
                SynthNote(base, 0.085, 0.28f),
                SynthNote(base * 1.25, 0.11, 0.24f)
            )
        )
    }

    fun playSuperGem() {
        if (isMuted) return
        playToneSequence(
            notes = listOf(
                SynthNote(659.25, 0.07, 0.26f),
                SynthNote(880.0, 0.07, 0.26f),
                SynthNote(1046.5, 0.08, 0.28f),
                SynthNote(1318.51, 0.16, 0.30f)
            )
        )
    }

    fun playHeartPickup() {
        if (isMuted) return
        playToneSequence(
            notes = listOf(
                SynthNote(587.33, 0.09, 0.25f),
                SynthNote(880.0, 0.14, 0.28f)
            )
        )
    }

    fun playHazardBump() {
        if (isMuted) return
        playSweep(startHz = 220.0, endHz = 92.0, durationSec = 0.20, volume = 0.34f)
    }

    fun playGameOver() {
        if (isMuted) return
        playToneSequence(
            notes = listOf(
                SynthNote(440.0, 0.14, 0.26f),
                SynthNote(392.0, 0.14, 0.25f),
                SynthNote(329.63, 0.26, 0.26f)
            )
        )
    }

    fun playVictoryFanfare() {
        if (isMuted) return
        playToneSequence(
            notes = listOf(
                SynthNote(523.25, 0.09, 0.26f),
                SynthNote(659.25, 0.09, 0.26f),
                SynthNote(783.99, 0.09, 0.28f),
                SynthNote(1046.5, 0.24, 0.32f)
            )
        )
    }

    fun playBoardTap(isPlayerX: Boolean) {
        if (isMuted) return
        val hz = if (isPlayerX) 620.0 else 490.0
        playToneSequence(listOf(SynthNote(hz, 0.075, 0.24f)))
    }

    private data class SynthNote(val freqHz: Double, val durationSec: Double, val volume: Float)

    private fun playToneSequence(notes: List<SynthNote>) {
        scope.launch {
            runCatching {
                val totalSamples = notes.sumOf { (it.durationSec * SAMPLE_RATE).toInt() }
                if (totalSamples <= 0) return@runCatching
                val pcm = ShortArray(totalSamples)
                var cursor = 0
                for (note in notes) {
                    val count = (note.durationSec * SAMPLE_RATE).toInt()
                    for (i in 0 until count) {
                        val t = i.toDouble() / SAMPLE_RATE
                        val env = exp(-t * 11.0) * (1.0 - exp(-t * 220.0))
                        val wave = sin(2.0 * PI * note.freqHz * t) * 0.78 +
                            sin(2.0 * PI * note.freqHz * 2.0 * t) * 0.22
                        val sample = (wave * env * note.volume * Short.MAX_VALUE).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        pcm[cursor + i] = sample.toShort()
                    }
                    cursor += count
                }
                dispatchPcm(pcm)
            }
        }
    }

    private fun playSweep(startHz: Double, endHz: Double, durationSec: Double, volume: Float) {
        scope.launch {
            runCatching {
                val count = (durationSec * SAMPLE_RATE).toInt().coerceAtLeast(1)
                val pcm = ShortArray(count)
                var phase = 0.0
                for (i in 0 until count) {
                    val frac = i.toDouble() / count
                    val freq = startHz + (endHz - startHz) * frac
                    phase += 2.0 * PI * freq / SAMPLE_RATE
                    val env = exp(-frac * 4.5) * (1.0 - exp(-frac * 35.0))
                    val sample = (sin(phase) * env * volume * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    pcm[i] = sample.toShort()
                }
                dispatchPcm(pcm)
            }
        }
    }

    private fun dispatchPcm(pcm: ShortArray) {
        val byteCount = pcm.size * 2
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(byteCount)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(pcm, 0, pcm.size)
        track.setNotificationMarkerPosition(pcm.size)
        track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(t: AudioTrack?) {
                runCatching { t?.release() }
            }
            override fun onPeriodicNotification(t: AudioTrack?) = Unit
        })
        track.play()
    }
}
