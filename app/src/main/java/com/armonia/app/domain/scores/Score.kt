package com.armonia.app.domain.scores

import com.armonia.app.domain.model.Hand
import com.armonia.app.domain.model.Level
import kotlin.math.ceil

data class ScoreNote(
    val midi: Int,
    val startBeats: Float,
    val durationBeats: Float,
    val hand: Hand = Hand.RIGHT,
    val finger: Int? = null
)

data class Score(
    val id: String,
    val title: String,
    val composer: String,
    val sourceNote: String,
    val license: String,
    val tempoBpm: Int,
    val beatsPerBar: Int,
    val beatUnit: Int,
    val level: Level,
    val notes: List<ScoreNote>
) {

    val totalBeats: Float
        get() = maxOf(
            beatsPerBar.toFloat(),
            notes.maxOfOrNull { it.startBeats + it.durationBeats } ?: 0f
        )

    val barCount: Int
        get() = ceil(totalBeats / beatsPerBar).toInt().coerceAtLeast(1)

    fun transposed(semitones: Int): Score {
        if (semitones == 0) return this
        return copy(
            notes = notes.map { note ->
                note.copy(midi = (note.midi + semitones).coerceIn(0, 127))
            }
        )
    }

    fun withHand(hand: Hand): Score {
        if (hand == Hand.BOTH) return this
        return copy(notes = notes.filter { it.hand == hand })
    }

    /** Mantiene solo la nota più acuta per ogni attacco: utile per la melodia. */
    fun melodyOnly(): Score {
        if (notes.isEmpty()) return this
        val melody = notes
            .groupBy { it.startBeats }
            .values
            .mapNotNull { chord -> chord.maxByOrNull { it.midi } }
            .sortedBy { it.startBeats }
        return copy(notes = melody)
    }

    /**
     * Estrae le battute richieste e normalizza l'inizio della sezione a zero,
     * così il renderer può disegnare una sezione di loop senza spazi vuoti.
     */
    fun inBars(startBar: Int, endBar: Int): Score {
        val safeStart = startBar.coerceIn(1, barCount)
        val safeEnd = endBar.coerceIn(safeStart, barCount)
        val startBeat = (safeStart - 1) * beatsPerBar.toFloat()
        val endBeat = safeEnd * beatsPerBar.toFloat()

        return copy(
            notes = notes
                .filter { it.startBeats >= startBeat && it.startBeats < endBeat }
                .map { it.copy(startBeats = it.startBeats - startBeat) }
                .sortedWith(compareBy({ it.startBeats }, { it.midi }))
        )
    }

    fun prepared(
        hand: Hand,
        melody: Boolean,
        startBar: Int,
        endBar: Int,
        semitones: Int
    ): Score {
        var result = inBars(startBar, endBar).withHand(hand)
        if (melody) {
            result = result.melodyOnly()
        }
        return result.transposed(semitones)
    }
}
