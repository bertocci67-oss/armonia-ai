package com.armonia.app.domain.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * I click sono WAV mono generati una sola volta nella cache e caricati in SoundPool.
 * SoundPool è molto più leggero che creare un AudioTrack per ogni battito.
 */
class MetronomeEngine(context: Context) {

    private val appContext = context.applicationContext
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var accentId = 0
    private var softId = 0
    private var loaded = 0
    private var prepared = false
    private var pendingReady: (() -> Unit)? = null

    var isReady: Boolean = false
        private set

    fun prepare(onReady: () -> Unit) {
        if (isReady) {
            onReady()
            return
        }
        pendingReady = onReady
        if (prepared) return
        prepared = true

        soundPool.setOnLoadCompleteListener { _, _, _ ->
            loaded++
            if (loaded >= 2 && !isReady) {
                isReady = true
                pendingReady?.invoke()
                pendingReady = null
            }
        }

        accentId = soundPool.load(writeClick("armonia_click_accent.wav", 1_380.0), 1)
        softId = soundPool.load(writeClick("armonia_click_soft.wav", 920.0), 1)
    }

    fun tick(accent: Boolean, volume: Float = 1f) {
        val sound = if (accent) accentId else softId
        if (sound == 0) return
        soundPool.play(sound, volume, volume, 1, 0, 1f)
    }

    fun release() {
        runCatching { soundPool.release() }
        isReady = false
        prepared = false
    }

    private fun writeClick(name: String, frequency: Double): String {
        val file = File(appContext.cacheDir, name)
        if (file.exists() && file.length() > 100L) return file.absolutePath

        val sampleRate = 22_050
        val durationMs = 45
        val sampleCount = sampleRate * durationMs / 1_000
        val dataSize = sampleCount * 2

        DataOutputStream(
            BufferedOutputStream(FileOutputStream(file))
        ).use { out ->
            out.writeAscii("RIFF")
            out.writeIntLe(36 + dataSize)
            out.writeAscii("WAVE")
            out.writeAscii("fmt ")
            out.writeIntLe(16)
            out.writeShortLe(1) // PCM
            out.writeShortLe(1) // mono
            out.writeIntLe(sampleRate)
            out.writeIntLe(sampleRate * 2)
            out.writeShortLe(2)
            out.writeShortLe(16)
            out.writeAscii("data")
            out.writeIntLe(dataSize)

            for (i in 0 until sampleCount) {
                val seconds = i.toDouble() / sampleRate
                val envelope = exp(-seconds * 55.0)
                val sample = (sin(2.0 * PI * frequency * seconds) * envelope * 0.65 * Short.MAX_VALUE).toInt()
                out.writeShortLe(sample)
            }
        }

        return file.absolutePath
    }

    private fun DataOutputStream.writeAscii(text: String) {
        write(text.toByteArray(Charsets.US_ASCII))
    }

    private fun DataOutputStream.writeIntLe(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
        write((value shr 16) and 0xFF)
        write((value shr 24) and 0xFF)
    }

    private fun DataOutputStream.writeShortLe(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
    }
}
