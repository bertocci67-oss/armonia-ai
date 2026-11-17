package com.armonia.app.domain.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import com.armonia.app.domain.music.MusicTheory
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Sintesi leggera di note/accordi. La durata massima è breve (riferimenti e brani),
 * quindi il buffer può essere generato al volo e riprodotto in MODE_STATIC.
 */
class TonePlayer {

    private val handler = Handler(Looper.getMainLooper())
    private var track: AudioTrack? = null
    private val stopRunnable = Runnable { releaseTrack() }

    @Synchronized
    fun playChord(
        midiNotes: List<Int>,
        durationMs: Int = 700,
        volume: Float = 0.4f
    ) {
        if (midiNotes.isEmpty()) {
            stop()
            return
        }

        handler.removeCallbacks(stopRunnable)
        releaseTrack()

        val sampleRate = 22_050
        val totalSamples = (sampleRate * durationMs / 1_000).coerceAtLeast(sampleRate / 20)
        val samples = ShortArray(totalSamples)
        val attackSamples = min(totalSamples / 4, sampleRate / 100).coerceAtLeast(1)
        val releaseSamples = min(totalSamples / 3, sampleRate / 20).coerceAtLeast(1)
        val perVoiceVolume = volume / midiNotes.size

        for (midi in midiNotes) {
            val frequency = MusicTheory.frequency(midi)
            for (i in 0 until totalSamples) {
                val attack = min(1.0, i.toDouble() / attackSamples)
                val release = min(1.0, (totalSamples - i).toDouble() / releaseSamples)
                val envelope = attack * release
                val phase = 2.0 * PI * frequency * i / sampleRate
                val wave = sin(phase) * 0.7 + sin(phase * 2.0) * 0.3
                val value = (wave * envelope * perVoiceVolume * Short.MAX_VALUE).roundToInt()
                val mixed = samples[i] + value
                samples[i] = mixed.coerceIn(
                    Short.MIN_VALUE.toInt(),
                    Short.MAX_VALUE.toInt()
                ).toShort()
            }
        }

        val audioTrack = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()
        }.getOrNull() ?: return

        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()
        track = audioTrack
        handler.postDelayed(stopRunnable, durationMs + 120L)
    }

    suspend fun playSequence(
        midiNotes: List<Int>,
        noteMs: Int = 450,
        gapMs: Int = 120
    ) {
        for (midi in midiNotes) {
            playChord(listOf(midi), noteMs)
            delay((noteMs + gapMs).toLong())
        }
    }

    @Synchronized
    fun stop() {
        handler.removeCallbacks(stopRunnable)
        releaseTrack()
    }

    @Synchronized
    fun release() {
        handler.removeCallbacks(stopRunnable)
        releaseTrack()
    }

    @Synchronized
    private fun releaseTrack() {
        track?.let { audioTrack ->
            runCatching {
                if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack.stop()
                }
            }
            runCatching { audioTrack.release() }
        }
        track = null
    }
}
