package com.armonia.app.domain.model

enum class Instrument(val label: String, val symbol: String) {
    PIANO("Pianoforte", "🎹"),
    GUITAR("Chitarra", "🎸"),
    VOICE("Canto", "🎤"),
    VIOLIN("Violino", "🎻"),
    DRUMS("Batteria", "🥁"),
    BASS("Basso", "🎸"),
    UKULELE("Ukulele", "🪕"),
    FLUTE("Flauto", "🪈");

    val isMelodic: Boolean get() = this != DRUMS
}

enum class Level(val label: String, val shortLabel: String, val description: String) {
    BEGINNER(
        "Principiante",
        "Base",
        "Le prime note, il ritmo e la postura."
    ),
    INTERMEDIATE(
        "Intermedio",
        "Intermedio",
        "Brani completi, accordi e orecchio musicale."
    ),
    ADVANCED(
        "Avanzato",
        "Pro",
        "Interpretazione, armonia e performance."
    )
}

data class Profile(
    val instrument: Instrument,
    val level: Level
)

enum class Hand(val label: String) {
    RIGHT("Destra"),
    LEFT("Sinistra"),
    BOTH("Entrambe")
}

enum class Skill(val label: String) {
    TUNING("Accordatura"),
    RHYTHM("Ritmo"),
    NOTES("Note e lettura"),
    CHORDS("Accordi"),
    EAR("Orecchio"),
    REPERTOIRE("Repertorio"),
    EXPRESSION("Espressività")
}

enum class SessionKind(val label: String) {
    TUNER("Accordatore"),
    METRONOME("Metronomo"),
    SCORE("Spartito"),
    LESSON("Lezione"),
    EXERCISE("Esercizio")
}

enum class ExerciseKind {
    PITCH,
    RHYTHM,
    EAR
}

enum class ExerciseFocus(
    val label: String,
    val description: String,
    val kind: ExerciseKind,
    val skill: Skill
) {
    INTONATION(
        "Intonazione",
        "Note e piccoli fraseggi verificati dal microfono.",
        ExerciseKind.PITCH,
        Skill.NOTES
    ),
    RHYTHM(
        "Ritmo",
        "Attacchi precisi sul tempo, senza rincorrere il click.",
        ExerciseKind.RHYTHM,
        Skill.RHYTHM
    ),
    EAR(
        "Orecchio",
        "Riconosci intervalli e colori musicali.",
        ExerciseKind.EAR,
        Skill.EAR
    )
}

data class ProgressStats(
    val totalXp: Int = 0,
    val currentStreak: Int = 0,
    val sessionsCount: Int = 0,
    val totalMinutes: Int = 0,
    val weekMinutes: List<Int> = List(7) { 0 },
    val skillAccuracy: Map<Skill, Float> = emptyMap(),
    val averageAccuracy: Float = 0f
)
