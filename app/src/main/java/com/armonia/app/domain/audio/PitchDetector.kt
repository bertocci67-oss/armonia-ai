package com.armonia.app.domain.audio

import com.armonia.app.domain.music.MusicTheory
import kotlin.math.min
import kotlin.math.sqrt

data class PitchReading(
    val frequency: Double,
    val midi: Int,
    val cents: Double,
    val clarity: Float
)

/**
 * Rilevamento monofonico dell'altezza con YIN semplificato.
 * I buffer sono riutilizzati tra un frame e l'altro per ridurre la pressione sul GC.
 */
class PitchDetector(
    private val sampleRate: Int = 44_100,
    private val minFrequency: Double = 60.0,
    private val maxFrequency: Double = 1_200.0
) {

    private val threshold = 0.15f
    private var difference = FloatArray(0)

    fun detect(samples: FloatArray): PitchReading? {
        val window = samples.size / 2
        if (window < 64) return null

        val minTau = (sampleRate / maxFrequency).toInt().coerceAtLeast(2)
        val maxTau = min(window - 1, (sampleRate / minFrequency).toInt())
        if (maxTau <= minTau + 2) return null

        if (difference.size <= maxTau) {
            difference = FloatArray(maxTau + 1)
        }

        var energy = 0.0
        for (i in 0 until window) {
            energy += samples[i] * samples[i]
        }
        val rms = sqrt(energy / window)
        if (rms < 0.008) return null

        for (tau in 1..maxTau) {
            var sum = 0.0
            var index = 0
            while (index < window) {
                val delta = samples[index] - samples[index + tau]
                sum += delta * delta
                index++
            }
            difference[tau] = sum.toFloat()
        }

        difference[0] = 1f
        var runningSum = 0f
        for (tau in 1..maxTau) {
            runningSum += difference[tau]
            difference[tau] = if (runningSum == 0f) {
                1f
            } else {
                difference[tau] * tau / runningSum
            }
        }

        var tau = minTau
        var candidate = -1
        while (tau < maxTau) {
            if (difference[tau] < threshold) {
                while (tau + 1 < maxTau && difference[tau + 1] < difference[tau]) {
                    tau++
                }
                candidate = tau
                break
            }
            tau++
        }
        if (candidate == -1) return null

        val refinedTau = parabolicInterpolation(candidate, maxTau)
        if (refinedTau <= 0.0) return null

        val frequency = sampleRate / refinedTau
        if (frequency < minFrequency || frequency > maxFrequency) return null

        val midi = MusicTheory.nearestMidi(frequency)
        val target = MusicTheory.frequency(midi)
        val cents = MusicTheory.cents(frequency, target)
        val clarity = (1f - difference[candidate]).coerceIn(0f, 1f)

        return PitchReading(
            frequency = frequency,
            midi = midi,
            cents = cents,
            clarity = clarity
        )
    }

    private fun parabolicInterpolation(tau: Int, maxTau: Int): Double {
        if (tau <= 0 || tau >= maxTau) return tau.toDouble()

        val previous = difference[tau - 1].toDouble()
        val current = difference[tau].toDouble()
        val next = difference[tau + 1].toDouble()

        val denominator = 2.0 * (2.0 * current - previous - next)
        if (denominator == 0.0) return tau.toDouble()

        val offset = (previous - next) / denominator
        return tau + offset.coerceIn(-1.0, 1.0)
    }
}
