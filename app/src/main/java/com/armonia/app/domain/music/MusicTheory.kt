package com.armonia.app.domain.music

import com.armonia.app.domain.model.Instrument
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

data class TuningString(
    val label: String,
    val midi: Int
)

data class TuningPreset(
    val name: String,
    val strings: List<TuningString>
)

object MusicTheory {

    private val noteNames = arrayOf(
        "Do", "Do♯", "Re", "Re♯", "Mi", "Fa",
        "Fa♯", "Sol", "Sol♯", "La", "La♯", "Si"
    )

    private val sharpPitchClasses = setOf(1, 3, 6, 8, 10)

    /** Posizione diatonica della nota (le alterazioni restano sulla lettera base). */
    private val diatonicSteps = intArrayOf(0, 0, 1, 1, 2, 3, 3, 4, 4, 5, 5, 6)

    fun frequency(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

    fun nearestMidi(frequency: Double): Int =
        (69 + 12 * log2(frequency / 440.0)).roundToInt()

    fun noteName(midi: Int): String {
        val pitchClass = Math.floorMod(midi, 12)
        val octave = Math.floorDiv(midi, 12) - 1
        return "${noteNames[pitchClass]}$octave"
    }

    fun pitchClassName(midi: Int): String = noteNames[Math.floorMod(midi, 12)]

    fun isSharp(midi: Int): Boolean =
        Math.floorMod(midi, 12) in sharpPitchClasses

    fun cents(frequency: Double, targetFrequency: Double): Double {
        if (frequency <= 0.0 || targetFrequency <= 0.0) return 0.0
        return 1200.0 * ln(frequency / targetFrequency) / ln(2.0)
    }

    fun diatonicIndex(midi: Int): Int {
        val pitchClass = Math.floorMod(midi, 12)
        val octave = Math.floorDiv(midi, 12) - 1
        return octave * 7 + diatonicSteps[pitchClass]
    }

    fun intervalName(semitones: Int): String = when (semitones) {
        0 -> "Unisono"
        1 -> "Seconda minore"
        2 -> "Seconda maggiore"
        3 -> "Terza minore"
        4 -> "Terza maggiore"
        5 -> "Quarta giusta"
        6 -> "Tritono"
        7 -> "Quinta giusta"
        8 -> "Sesta minore"
        9 -> "Sesta maggiore"
        10 -> "Settima minore"
        11 -> "Settima maggiore"
        12 -> "Ottava"
        else -> "$semitones semitoni"
    }
}

object Tunings {

    private val chromatic = listOf(
        TuningPreset(
            name = "Cromatica · La4 = 440 Hz",
            strings = listOf(TuningString("La 4", 69))
        )
    )

    fun presetsFor(instrument: Instrument): List<TuningPreset> = when (instrument) {
        Instrument.GUITAR -> listOf(
            TuningPreset(
                "Standard",
                listOf(
                    TuningString("6ª", 40),
                    TuningString("5ª", 45),
                    TuningString("4ª", 50),
                    TuningString("3ª", 55),
                    TuningString("2ª", 59),
                    TuningString("1ª", 64)
                )
            ),
            TuningPreset(
                "Drop D",
                listOf(
                    TuningString("6ª", 38),
                    TuningString("5ª", 45),
                    TuningString("4ª", 50),
                    TuningString("3ª", 55),
                    TuningString("2ª", 59),
                    TuningString("1ª", 64)
                )
            )
        )

        Instrument.BASS -> listOf(
            TuningPreset(
                "Standard",
                listOf(
                    TuningString("4ª", 28),
                    TuningString("3ª", 33),
                    TuningString("2ª", 38),
                    TuningString("1ª", 43)
                )
            )
        )

        Instrument.VIOLIN -> listOf(
            TuningPreset(
                "Standard",
                listOf(
                    TuningString("4ª", 55),
                    TuningString("3ª", 62),
                    TuningString("2ª", 69),
                    TuningString("1ª", 76)
                )
            )
        )

        Instrument.UKULELE -> listOf(
            TuningPreset(
                "Standard (Sol acuto)",
                listOf(
                    TuningString("4ª", 67),
                    TuningString("3ª", 60),
                    TuningString("2ª", 64),
                    TuningString("1ª", 69)
                )
            )
        )

        Instrument.PIANO,
        Instrument.VOICE,
        Instrument.FLUTE -> chromatic

        Instrument.DRUMS -> emptyList()
    }
}
