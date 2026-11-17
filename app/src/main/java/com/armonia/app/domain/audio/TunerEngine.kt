package com.armonia.app.domain.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

/**
 * Un solo AudioRecord attivo alla volta. Il flusso termina quando viene cancellato
 * e rilascia sempre la risorsa audio.
 */
class TunerEngine(
    private val lowPower: Boolean = false
) {

    fun stream(): Flow<PitchReading> = flow {
        val sampleRate = if (lowPower) 22_050 else 44_100
        val frameSize = if (lowPower) 2_048 else 4_096

        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            throw IllegalStateException("Microfono non disponibile su questo dispositivo.")
        }

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer * 2, frameSize * 2)
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw IllegalStateException("Impossibile inizializzare il microfono.")
        }

        val detector = PitchDetector(sampleRate = sampleRate)
        val shorts = ShortArray(frameSize)
        val floats = FloatArray(frameSize)

        try {
            record.startRecording()
            while (currentCoroutineContext().isActive) {
                val read = record.read(shorts, 0, shorts.size)
                if (read <= 0) continue

                val usable = if (read == shorts.size) floats else FloatArray(read)
                for (i in 0 until read) {
                    usable[i] = shorts[i] / 32_768f
                }

                val reading = detector.detect(usable)
                if (reading != null && reading.clarity > 0.55f) {
                    emit(reading)
                }
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }
    }.flowOn(Dispatchers.Default)
}
