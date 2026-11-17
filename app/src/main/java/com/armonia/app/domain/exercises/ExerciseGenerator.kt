package com.armonia.app.domain.exercises

import com.armonia.app.domain.model.ExerciseFocus
import com.armonia.app.domain.model.ExerciseKind
import com.armonia.app.domain.model.ExerciseKind.EAR
import com.armonia.app.domain.model.ExerciseKind.PITCH
import com.armonia.app.domain.model.ExerciseKind.RHYTHM
import com.armonia.app.domain.model.Instrument
import com.armonia.app.domain.model.Level
import com.armonia.app.domain.music.MusicTheory
import kotlin.random.Random

data class Exercise(
    val id: String,
    val kind: ExerciseKind,
    val focus: ExerciseFocus,
    val title: String,
    val instructions: String,
    val level: Level,
    val instrument: Instrument,
    val bpm: Int,
    val targetNotes: List<Int> = emptyList(),
    val rhythmBeats: List<Float> = emptyList(),
    val options: List<String> = emptyList(),
    val correctIndex: Int = -1,
    val rootMidi: Int = 60,
    val intervalSemitones: Int = 0
) {
    val xp: Int
        get() = when (level) {
            Level.BEGINNER -> 15
            Level.INTERMEDIATE -> 25
            Level.ADVANCED -> 35
        }
}

object ExerciseGenerator {

    fun generate(
        instrument: Instrument,
        level: Level,
        focus: ExerciseFocus,
        seed: Long = System.nanoTime()
    ): Exercise {
        val random = Random(seed)
        val effectiveFocus = if (!instrument.isMelodic) {
            ExerciseFocus.RHYTHM
        } else {
            focus
        }

        return when (effectiveFocus.kind) {
            PITCH -> pitchExercise(instrument, level, effectiveFocus, seed, random)
            RHYTHM -> rhythmExercise(instrument, level, effectiveFocus, seed, random)
            EAR -> earExercise(instrument, level, effectiveFocus, seed, random)
        }
    }

    private fun pitchExercise(
        instrument: Instrument,
        level: Level,
        focus: ExerciseFocus,
        seed: Long,
        random: Random
    ): Exercise {
        val range = rangeFor(instrument)
        val scale = when (level) {
            Level.BEGINNER -> intArrayOf(0, 2, 4, 7, 9)
            Level.INTERMEDIATE -> intArrayOf(0, 2, 4, 5, 7, 9, 11, 12)
            Level.ADVANCED -> intArrayOf(0, 2, 3, 5, 7, 8, 10, 12)
        }
        val degrees = when (level) {
            Level.BEGINNER -> listOf(0, 1, 2, 1)
            Level.INTERMEDIATE -> listOf(0, 1, 2, 3, 2, 0)
            Level.ADVANCED -> listOf(0, 1, 2, 3, 4, 3, 2, 0)
        }
        val maxOffset = scale.last()
        val lowestRoot = range.first
        val highestRoot = (range.last - maxOffset).coerceAtLeast(lowestRoot)
        val root = lowestRoot + random.nextInt(0, (highestRoot - lowestRoot).coerceAtLeast(0) + 1)
        val orderedDegrees = if (random.nextBoolean()) degrees else degrees.reversed()
        val targets = orderedDegrees.map { degree -> root + scale[degree] }

        val bpm = when (level) {
            Level.BEGINNER -> 60
            Level.INTERMEDIATE -> 72
            Level.ADVANCED -> 84
        }

        return Exercise(
            id = "ex-pitch-$seed",
            kind = PITCH,
            focus = focus,
            title = "Micro-studio di intonazione",
            instructions = "Suona le note nell'ordine indicato. Il microfono ascolta una nota alla volta: " +
                "prenditi il tempo che serve per trovare un suono stabile.",
            level = level,
            instrument = instrument,
            bpm = bpm,
            targetNotes = targets
        )
    }

    private fun rhythmExercise(
        instrument: Instrument,
        level: Level,
        focus: ExerciseFocus,
        seed: Long,
        random: Random
    ): Exercise {
        val patterns: List<List<Float>> = when (level) {
            Level.BEGINNER -> listOf(
                listOf(1f, 1f, 1f, 1f),
                listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f),
                listOf(2f, 1f, 1f),
                listOf(1.5f, 0.5f, 1f, 1f),
                listOf(1f, 0.5f, 0.5f, 2f)
            )
            Level.INTERMEDIATE -> listOf(
                listOf(1f, 0.5f, 0.5f, 1f, 1f, 1f, 0.5f, 0.5f, 1f, 1f),
                listOf(2f, 1f, 1f, 2f, 1f, 1f),
                listOf(0.5f, 0.5f, 1f, 2f, 1f, 0.5f, 0.5f, 2f),
                listOf(1.5f, 0.5f, 1f, 1f, 2f, 1f, 1f)
            )
            Level.ADVANCED -> listOf(
                listOf(0.5f, 1f, 0.5f, 1f, 1f, 0.5f, 1f, 0.5f, 1f, 1f),
                listOf(1.5f, 0.5f, 0.5f, 0.5f, 1f, 1.5f, 0.5f, 0.5f, 0.5f, 1f),
                listOf(0.5f, 0.5f, 0.5f, 0.5f, 2f, 2f, 0.5f, 0.5f, 0.5f, 0.5f),
                listOf(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f)
            )
        }
        val pattern = patterns.random(random)
        val bpm = when (level) {
            Level.BEGINNER -> 60
            Level.INTERMEDIATE -> 72
            Level.ADVANCED -> 80
        }

        return Exercise(
            id = "ex-rhythm-$seed",
            kind = RHYTHM,
            focus = focus,
            title = "Micro-studio ritmico",
            instructions = "Ascolta il conteggio di quattro battiti, poi batti il ritmo sul pulsante TAP. " +
                "La valutazione misura la precisione degli attacchi.",
            level = level,
            instrument = instrument,
            bpm = bpm,
            rhythmBeats = pattern
        )
    }

    private fun earExercise(
        instrument: Instrument,
        level: Level,
        focus: ExerciseFocus,
        seed: Long,
        random: Random
    ): Exercise {
        val pool = when (level) {
            Level.BEGINNER -> listOf(4, 7, 12)
            Level.INTERMEDIATE -> listOf(3, 4, 5, 7, 9, 12)
            Level.ADVANCED -> listOf(2, 3, 4, 5, 6, 7, 9, 10, 12)
        }
        val correct = pool.random(random)
        val distractors = pool.filter { it != correct }.shuffled(random).take(2)
        val optionValues = (distractors + correct).shuffled(random)
        val rootMidi = 57 + random.nextInt(0, 8)

        return Exercise(
            id = "ex-ear-$seed",
            kind = EAR,
            focus = focus,
            title = "Orecchio attivo",
            instructions = "Ascolta l'intervallo: prima la nota di partenza, poi quella di arrivo. " +
                "Scegli la risposta; puoi riascoltare prima di rispondere.",
            level = level,
            instrument = instrument,
            bpm = 70,
            options = optionValues.map { MusicTheory.intervalName(it) },
            correctIndex = optionValues.indexOf(correct),
            rootMidi = rootMidi,
            intervalSemitones = correct
        )
    }

    private fun rangeFor(instrument: Instrument): IntRange = when (instrument) {
        Instrument.PIANO -> 48..84
        Instrument.GUITAR -> 52..76
        Instrument.VOICE -> 55..76
        Instrument.VIOLIN -> 55..88
        Instrument.BASS -> 28..55
        Instrument.UKULELE -> 60..79
        Instrument.FLUTE -> 60..86
        Instrument.DRUMS -> 36..50
    }
}
