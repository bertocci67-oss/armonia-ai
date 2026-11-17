package com.armonia.app.domain.scores

import com.armonia.app.domain.model.Hand
import com.armonia.app.domain.model.Level

object ScoreLibrary {

    private fun n(
        midi: Int,
        startBeats: Float,
        durationBeats: Float = 1f,
        hand: Hand = Hand.RIGHT,
        finger: Int? = null
    ): ScoreNote = ScoreNote(
        midi = midi,
        startBeats = startBeats,
        durationBeats = durationBeats,
        hand = hand,
        finger = finger
    )

    private fun right(
        midi: Int,
        startBeats: Float,
        durationBeats: Float = 1f,
        finger: Int? = null
    ): ScoreNote = n(midi, startBeats, durationBeats, Hand.RIGHT, finger)

    private fun left(
        midi: Int,
        startBeats: Float,
        durationBeats: Float = 4f
    ): ScoreNote = n(midi, startBeats, durationBeats, Hand.LEFT, 5)

    private val odeToJoy = Score(
        id = "ode-to-joy",
        title = "Inno alla gioia",
        composer = "Ludwig van Beethoven",
        sourceNote = "Tema dalla Sinfonia n. 9, arrangiamento didattico",
        license = "Pubblico dominio · arrangiamento originale",
        tempoBpm = 96,
        beatsPerBar = 4,
        beatUnit = 4,
        level = Level.BEGINNER,
        notes = listOf(
            right(64, 0f, 1f, 3),
            right(65, 1f, 1f, 4),
            right(67, 2f, 1f, 5),
            right(67, 3f, 1f, 5),
            right(67, 4f, 1f, 5),
            right(65, 5f, 1f, 4),
            right(64, 6f, 1f, 3),
            right(62, 7f, 1f, 2),
            right(60, 8f, 1f, 1),
            right(60, 9f, 1f, 1),
            right(62, 10f, 1f, 2),
            right(64, 11f, 1f, 3),
            right(64, 12f, 1.5f, 3),
            right(62, 13.5f, 0.5f, 2),
            right(62, 14f, 2f, 2),
            right(64, 16f, 1f, 3),
            right(64, 17f, 1f, 3),
            right(65, 18f, 1f, 4),
            right(67, 19f, 1f, 5),
            right(67, 20f, 1f, 5),
            right(65, 21f, 1f, 4),
            right(64, 22f, 1f, 3),
            right(62, 23f, 1f, 2),
            right(60, 24f, 1f, 1),
            right(60, 25f, 1f, 1),
            right(62, 26f, 1f, 2),
            right(64, 27f, 1f, 3),
            right(62, 28f, 1.5f, 2),
            right(60, 29.5f, 0.5f, 1),
            right(60, 30f, 2f, 1),
            left(48, 0f),
            left(43, 4f),
            left(48, 8f),
            left(43, 12f),
            left(48, 16f),
            left(43, 20f),
            left(48, 24f),
            left(48, 28f)
        )
    )

    private val twinkle = Score(
        id = "twinkle",
        title = "Stella stellina",
        composer = "Tradizionale francese",
        sourceNote = "Ah! Vous dirai-je, Maman, arrangiamento didattico",
        license = "Pubblico dominio · arrangiamento originale",
        tempoBpm = 100,
        beatsPerBar = 4,
        beatUnit = 4,
        level = Level.BEGINNER,
        notes = listOf(
            right(60, 0f, 1f, 1),
            right(60, 1f, 1f, 1),
            right(67, 2f, 1f, 5),
            right(67, 3f, 1f, 5),
            right(69, 4f, 1f, 5),
            right(69, 5f, 1f, 5),
            right(67, 6f, 2f, 5),
            right(65, 8f, 1f, 4),
            right(65, 9f, 1f, 4),
            right(64, 10f, 1f, 3),
            right(64, 11f, 1f, 3),
            right(62, 12f, 1f, 2),
            right(62, 13f, 1f, 2),
            right(60, 14f, 2f, 1),
            right(67, 16f, 1f, 5),
            right(67, 17f, 1f, 5),
            right(65, 18f, 1f, 4),
            right(65, 19f, 1f, 4),
            right(64, 20f, 1f, 3),
            right(64, 21f, 1f, 3),
            right(62, 22f, 2f, 2),
            right(67, 24f, 1f, 5),
            right(67, 25f, 1f, 5),
            right(65, 26f, 1f, 4),
            right(65, 27f, 1f, 4),
            right(64, 28f, 1f, 3),
            right(64, 29f, 1f, 3),
            right(62, 30f, 2f, 2),
            left(48, 0f),
            left(48, 4f),
            left(53, 8f),
            left(48, 12f),
            left(43, 16f),
            left(48, 20f),
            left(43, 24f),
            left(48, 28f)
        )
    )

    private val minuet = Score(
        id = "minuet-g",
        title = "Minuetto in Sol",
        composer = "Christian Petzold (attribuito)",
        sourceNote = "Tema da BWV Anh. 114, arrangiamento semplificato",
        license = "Pubblico dominio · arrangiamento originale",
        tempoBpm = 108,
        beatsPerBar = 3,
        beatUnit = 4,
        level = Level.INTERMEDIATE,
        notes = listOf(
            right(74, 0f, 1f, 5),
            right(67, 1f, 1f, 1),
            right(69, 2f, 1f, 2),
            right(71, 3f, 1f, 1),
            right(72, 4f, 1f, 2),
            right(74, 5f, 1f, 3),
            right(67, 6f, 1f, 1),
            right(71, 7f, 1f, 2),
            right(67, 8f, 1f, 1),
            right(74, 9f, 2f, 4),
            right(67, 11f, 1f, 1),
            right(76, 12f, 1f, 5),
            right(74, 13f, 1f, 4),
            right(72, 14f, 1f, 3),
            right(71, 15f, 1f, 2),
            right(69, 16f, 1f, 1),
            right(67, 17f, 1f, 1),
            right(72, 18f, 1f, 3),
            right(74, 19f, 1f, 4),
            right(76, 20f, 1f, 5),
            right(74, 21f, 1.5f, 4),
            right(71, 22.5f, 0.5f, 2),
            right(67, 23f, 1f, 1),
            left(43, 0f, 3f),
            left(50, 3f, 3f),
            left(43, 6f, 3f),
            left(50, 9f, 3f),
            left(48, 12f, 3f),
            left(43, 15f, 3f),
            left(48, 18f, 3f),
            left(43, 21f, 3f)
        )
    )

    private val cadenceStudy = Score(
        id = "giro-armonico",
        title = "Giro armonico in Do",
        composer = "Armonia AI",
        sourceNote = "Studio originale su I–IV–V–I",
        license = "Originale · uso libero nell'app",
        tempoBpm = 84,
        beatsPerBar = 4,
        beatUnit = 4,
        level = Level.BEGINNER,
        notes = listOf(
            right(64, 0f, 4f, 3),
            right(67, 0f, 4f, 5),
            right(65, 4f, 4f, 3),
            right(69, 4f, 4f, 5),
            right(62, 8f, 4f, 2),
            right(67, 8f, 4f, 4),
            right(64, 12f, 4f, 3),
            right(72, 12f, 4f, 5),
            left(48, 0f),
            left(41, 4f),
            left(43, 8f),
            left(48, 12f)
        )
    )

    private val rhythmStudy = Score(
        id = "studio-ritmico",
        title = "Studio ritmico in 4/4",
        composer = "Armonia AI",
        sourceNote = "Studio originale su durate e ottavi",
        license = "Originale · uso libero nell'app",
        tempoBpm = 92,
        beatsPerBar = 4,
        beatUnit = 4,
        level = Level.BEGINNER,
        notes = buildList {
            listOf(0f, 1f, 2f, 3f).forEach { add(right(69, it, 1f, 1)) }
            listOf(4f, 4.5f, 5f, 5.5f, 6f, 6.5f, 7f, 7.5f).forEach { add(right(69, it, 0.5f, 2)) }
            add(right(69, 8f, 1.5f, 1))
            add(right(69, 9.5f, 0.5f, 2))
            add(right(69, 10f, 1.5f, 1))
            add(right(69, 11.5f, 0.5f, 2))
            add(right(69, 12f, 2f, 1))
            add(right(69, 14f, 2f, 1))
            add(right(69, 16f, 1f, 1))
            add(right(69, 17f, 0.5f, 2))
            add(right(69, 17.5f, 0.5f, 2))
            add(right(69, 18f, 1f, 1))
            add(right(69, 19f, 1f, 1))
            listOf(20f, 20.5f, 21f, 21.5f, 22f, 22.5f, 23f, 23.5f)
                .forEach { add(right(69, it, 0.5f, 2)) }
            add(right(69, 24f, 2f, 1))
            add(right(69, 26f, 2f, 1))
            listOf(28f, 29f, 30f, 31f).forEach { add(right(69, it, 1f, 1)) }
        }
    )

    private val arpeggioStudy = Score(
        id = "arpeggi-la-minore",
        title = "Arpeggi in La minore",
        composer = "Armonia AI",
        sourceNote = "Studio originale su arpeggi e cambi armonici",
        license = "Originale · uso libero nell'app",
        tempoBpm = 120,
        beatsPerBar = 4,
        beatUnit = 4,
        level = Level.ADVANCED,
        notes = buildList {
            val bars = listOf(
                0f to listOf(69, 72, 76, 72),
                4f to listOf(65, 69, 72, 69),
                8f to listOf(64, 67, 72, 67),
                12f to listOf(62, 67, 71, 67),
                16f to listOf(69, 72, 76, 72),
                20f to listOf(64, 68, 71, 68)
            )
            bars.forEach { (start, chord) ->
                repeat(2) { cycle ->
                    chord.forEachIndexed { index, midi ->
                        add(right(midi, start + cycle * 2f + index * 0.5f, 0.5f, 1 + index))
                    }
                }
            }
            add(left(45, 0f))
            add(left(41, 4f))
            add(left(48, 8f))
            add(left(43, 12f))
            add(left(45, 16f))
            add(left(40, 20f))
        }
    )

    val scores: List<Score> = listOf(
        odeToJoy,
        twinkle,
        minuet,
        cadenceStudy,
        rhythmStudy,
        arpeggioStudy
    )

    fun byId(id: String): Score? = scores.firstOrNull { it.id == id }

    fun suggestedFor(level: Level): List<Score> = scores.sortedBy {
        kotlin.math.abs(it.level.ordinal - level.ordinal)
    }
}
