package com.armonia.app.domain.lessons

import com.armonia.app.domain.model.Level

enum class LessonCategory(val label: String) {
    NOTES("Note"),
    RHYTHM("Ritmo"),
    CHORDS("Accordi"),
    THEORY("Teoria"),
    EAR("Orecchio"),
    EXPRESSION("Interpretazione")
}

sealed interface LessonPage {

    data class Theory(
        val heading: String,
        val body: String
    ) : LessonPage

    data class Quiz(
        val question: String,
        val options: List<String>,
        val correctIndex: Int,
        val explanation: String
    ) : LessonPage

    data class Listening(
        val question: String,
        val notes: List<Int>,
        val together: Boolean,
        val options: List<String>,
        val correctIndex: Int,
        val explanation: String
    ) : LessonPage
}

data class Lesson(
    val id: String,
    val title: String,
    val summary: String,
    val category: LessonCategory,
    val level: Level,
    val minutes: Int,
    val xp: Int,
    val pages: List<LessonPage>
)

object LessonCatalog {

    private val mapOfNotes = Lesson(
        id = "mappa-delle-note",
        title = "La mappa delle note",
        summary = "Do, re, mi e le ottave: orientati sul pentagramma.",
        category = LessonCategory.NOTES,
        level = Level.BEGINNER,
        minutes = 4,
        xp = 20,
        pages = listOf(
            LessonPage.Theory(
                heading = "Sette nomi, infinite altezze",
                body = "La musica usa sette nomi di nota: Do, Re, Mi, Fa, Sol, La, Si. " +
                    "Dopo il Si si ricomincia dal Do, ma un'ottava più in alto: le due note hanno lo stesso nome e una frequenza doppia. " +
                    "Sul pianoforte i tasti bianchi seguono questi nomi; i tasti neri aggiungono i diesis (♯) e i bemolle (♭). " +
                    "In questo percorso usiamo i nomi italiani: Do♯ è il tasto nero tra Do e Re."
            ),
            LessonPage.Theory(
                heading = "Il pentagramma è una mappa",
                body = "Le note si scrivono su cinque linee e quattro spazi. La chiave di violino, detta anche chiave di Sol, " +
                    "indica che la seconda linea da sinistra è un Sol. Più una nota sale nel pentagramma, più il suono è acuto. " +
                    "Quando le note escono dal pentagramma si usano i tagli addizionali: piccole linee che prolungano la mappa."
            ),
            LessonPage.Quiz(
                question = "Quante note diverse hanno un nome di base, prima di ricominciare dal Do?",
                options = listOf("5", "7", "8"),
                correctIndex = 1,
                explanation = "Sono sette: Do, Re, Mi, Fa, Sol, La, Si. L'ottava successiva ricomincia dal Do."
            ),
            LessonPage.Quiz(
                question = "Sul pentagramma, una nota più in alto...",
                options = listOf("suona più grave", "suona più acuta", "ha sempre un diesis"),
                correctIndex = 1,
                explanation = "L'altezza sul pentagramma corrisponde all'acuto e al grave: più in alto significa più acuto."
            )
        )
    )

    private val rhythmAndDurations = Lesson(
        id = "ritmo-e-durate",
        title = "Il ritmo è movimento",
        summary = "Pulsazione e durate: note intere, metà, quarti e ottavi.",
        category = LessonCategory.RHYTHM,
        level = Level.BEGINNER,
        minutes = 4,
        xp = 20,
        pages = listOf(
            LessonPage.Theory(
                heading = "Il battito che resta",
                body = "Il ritmo nasce da una pulsazione regolare, come un cuore che batte. " +
                    "Sopra quella pulsazione le note durano più o meno a lungo. " +
                    "Una nota intera dura quattro battiti, una metà due, un quarto uno e un ottavo mezzo battito. " +
                    "Le durate non sono velocità: la velocità del battito è il tempo, la durata dice quanto resta ogni nota."
            ),
            LessonPage.Theory(
                heading = "Contare ad alta voce",
                body = "Contare è il primo strumento del musicista. In 4/4 puoi contare «1, 2, 3, 4» e dividere ogni numero in due per gli ottavi: " +
                    "«1 e 2 e 3 e 4 e». Dirlo insieme al metronomo allena l'indipendenza tra voce e mani. " +
                    "Nel nostro generatore trovi micro-studi ritmici: il punteggio premia la precisione degli attacchi, non la forza del tap."
            ),
            LessonPage.Quiz(
                question = "Quanti quarti stanno in una battuta da 4/4?",
                options = listOf("2", "3", "4"),
                correctIndex = 2,
                explanation = "In 4/4 ci sono quattro pulsazioni da un quarto per battuta."
            ),
            LessonPage.Quiz(
                question = "Un ottavo dura...",
                options = listOf("il doppio di un quarto", "la metà di un quarto", "quanto due quarti"),
                correctIndex = 1,
                explanation = "Due ottavi riempiono un quarto, quindi un ottavo dura la metà di un quarto."
            )
        )
    )

    private val timeAndMetronome = Lesson(
        id = "tempo-e-metronomo",
        title = "Tempo, metro e metronomo",
        summary = "Come usare i BPM e sentire l'accento giusto.",
        category = LessonCategory.RHYTHM,
        level = Level.BEGINNER,
        minutes = 5,
        xp = 20,
        pages = listOf(
            LessonPage.Theory(
                heading = "BPM: il battito al minuto",
                body = "I BPM indicano quante pulsazioni ci sono in un minuto. A 60 BPM ogni battito dura un secondo; a 120 BPM dura mezzo secondo. " +
                    "Il metro organizza i battiti in gruppi: 4/4 ha quattro quarti, 3/4 tre quarti, 6/8 sei ottavi. " +
                    "Il primo battito del gruppo porta l'accento e dà il senso di rotazione."
            ),
            LessonPage.Theory(
                heading = "Il metronomo come insegnante",
                body = "Il metronomo non deve spaventare: serve a rendere visibile una pulsazione stabile. " +
                    "Parti lento, ascolta il click e suona solo poche note. Quando il passaggio è pulito, alza di 4–8 BPM. " +
                    "Se perdi il tempo, torna indietro: la velocità arriva dalla precisione, non dalla fretta."
            ),
            LessonPage.Quiz(
                question = "A 60 BPM, un battito dura...",
                options = listOf("mezzo secondo", "un secondo", "due secondi"),
                correctIndex = 1,
                explanation = "60 battiti in 60 secondi: ogni battito dura un secondo."
            ),
            LessonPage.Quiz(
                question = "In 3/4, l'accento principale cade...",
                options = listOf("sul primo battito", "sul secondo", "sul terzo"),
                correctIndex = 0,
                explanation = "Il primo battito del gruppo è quello accentato; gli altri due scorrono verso il successivo."
            )
        )
    )

    private val firstTriads = Lesson(
        id = "prime-triadi",
        title = "Le prime triadi",
        summary = "Maggiore e minore: due colori, tre note.",
        category = LessonCategory.CHORDS,
        level = Level.BEGINNER,
        minutes = 5,
        xp = 25,
        pages = listOf(
            LessonPage.Theory(
                heading = "Tre note, un colore",
                body = "Una triade è un accordo di tre note: fondamentale, terza e quinta. " +
                    "La terza decide il colore. Una terza maggiore (4 semitoni) dà un accordo maggiore, luminoso; " +
                    "una terza minore (3 semitoni) dà un accordo minore, più raccolto. " +
                    "Per esempio Do–Mi–Sol è Do maggiore, mentre Do–Mi♭–Sol è Do minore."
            ),
            LessonPage.Theory(
                heading = "Non solo teoria",
                body = "Suona le triadi lentamente e ascolta la differenza. Al pianoforte puoi appoggiare le tre note insieme; " +
                    "alla chitarra prova forme semplici di accordo e lascia risuonare le corde. " +
                    "Cantando, intona la fondamentale e poi la terza: è lì che senti il colore."
            ),
            LessonPage.Quiz(
                question = "Una triade maggiore è formata da...",
                options = listOf(
                    "fondamentale, terza maggiore e quinta giusta",
                    "tre note uguali",
                    "solo fondamentale e ottava"
                ),
                correctIndex = 0,
                explanation = "La struttura è fondamentale + 4 semitoni + 3 semitoni fino alla quinta: è la triade maggiore."
            ),
            LessonPage.Quiz(
                question = "Do–Mi♭–Sol forma un accordo...",
                options = listOf("maggiore", "minore", "diminuito"),
                correctIndex = 1,
                explanation = "La terza è minore (Do–Mi♭): l'accordo è Do minore."
            )
        )
    )

    private val majorOrMinor = Lesson(
        id = "maggiore-o-minore",
        title = "Maggiore o minore?",
        summary = "Allena l'orecchio a riconoscere il colore di un accordo.",
        category = LessonCategory.EAR,
        level = Level.BEGINNER,
        minutes = 4,
        xp = 20,
        pages = listOf(
            LessonPage.Theory(
                heading = "Il carattere degli accordi",
                body = "Non serve saper leggere la musica per sentire la differenza tra maggiore e minore. " +
                    "Il maggiore tende a suonare aperto, stabile, luminoso. Il minore può sembrare più intimo, " +
                    "malinconico o teso. Ascolta la terza: è la nota che cambia tutto."
            ),
            LessonPage.Listening(
                question = "Ascolta l'accordo. È maggiore o minore?",
                notes = listOf(60, 64, 67),
                together = true,
                options = listOf("Maggiore", "Minore"),
                correctIndex = 0,
                explanation = "Do–Mi–Sol ha la terza maggiore (Mi naturale): è Do maggiore."
            ),
            LessonPage.Listening(
                question = "E questo accordo?",
                notes = listOf(60, 63, 67),
                together = true,
                options = listOf("Maggiore", "Minore"),
                correctIndex = 1,
                explanation = "Do–Mi♭–Sol ha la terza minore: è Do minore. Il colore è più raccolto."
            )
        )
    )

    private val intervals = Lesson(
        id = "intervalli",
        title = "La distanza tra due note",
        summary = "Terze e quinte: riconosci gli intervalli con l'ascolto.",
        category = LessonCategory.EAR,
        level = Level.INTERMEDIATE,
        minutes = 5,
        xp = 25,
        pages = listOf(
            LessonPage.Theory(
                heading = "Intervalli: lo spazio che senti",
                body = "Un intervallo è la distanza tra due note, misurata in semitoni. " +
                    "La terza maggiore (4 semitoni) ha un suono aperto; la terza minore (3) è più vicina e raccolta. " +
                    "La quinta giusta (7 semitoni) è stabile e rotonda: è la distanza che senti tra la prima e la seconda nota di «Twinkle Twinkle»."
            ),
            LessonPage.Listening(
                question = "Ascolta due note in sequenza. Quale intervallo senti?",
                notes = listOf(60, 64),
                together = false,
                options = listOf("Terza maggiore", "Terza minore", "Quinta giusta"),
                correctIndex = 0,
                explanation = "Do–Mi sono 4 semitoni: terza maggiore."
            ),
            LessonPage.Listening(
                question = "E questo intervallo?",
                notes = listOf(60, 67),
                together = false,
                options = listOf("Quarta giusta", "Quinta giusta", "Sesta maggiore"),
                correctIndex = 1,
                explanation = "Do–Sol sono 7 semitoni: quinta giusta."
            )
        )
    )

    private val harmonicTurnaround = Lesson(
        id = "giro-armonico",
        title = "Il giro I–IV–V–I",
        summary = "Il motore dell'armonia tonale, con quattro accordi.",
        category = LessonCategory.CHORDS,
        level = Level.INTERMEDIATE,
        minutes = 6,
        xp = 30,
        pages = listOf(
            LessonPage.Theory(
                heading = "Costruire una casa armonica",
                body = "In una tonalità, gli accordi si costruiscono sui gradi della scala e si indicano con i numeri romani. " +
                    "In Do maggiore: I = Do, IV = Fa, V = Sol. Il giro I–IV–V–I è una specie di viaggio che parte da casa, " +
                    "si allontana, crea tensione sul V e torna alla tonica. È la base di migliaia di canzoni."
            ),
            LessonPage.Theory(
                heading = "Perché funziona",
                body = "Il V contiene una nota che «tira» verso la tonica: la sensibile. In Do maggiore, l'accordo di Sol (Sol–Si–Re) " +
                    "contiene il Si, che vuole salire al Do. Questa tensione e risoluzione danno la sensazione di chiusura. " +
                    "Quando improvvisi, prova a lasciare il V sospeso per qualche battito: l'orecchio chiederà la tonica."
            ),
            LessonPage.Quiz(
                question = "In Do maggiore, il grado V è...",
                options = listOf("Fa", "Sol", "La"),
                correctIndex = 1,
                explanation = "La scala di Do è Do Re Mi Fa Sol La Si: il quinto grado è Sol."
            ),
            LessonPage.Quiz(
                question = "La successione I–IV–V–I...",
                options = listOf("torna alla tonica", "non ha tensione", "è sempre minore"),
                correctIndex = 0,
                explanation = "Il V crea tensione e il ritorno al I la risolve: è una cadenza."
            )
        )
    )

    private val phrasingAndDynamics = Lesson(
        id = "fraseggio-e-dinamica",
        title = "Fraseggio, dinamica e rubato",
        summary = "Dare respiro a una linea musicale, non solo note giuste.",
        category = LessonCategory.EXPRESSION,
        level = Level.INTERMEDIATE,
        minutes = 6,
        xp = 30,
        pages = listOf(
            LessonPage.Theory(
                heading = "Una frase respira",
                body = "Suonare una frase è come dire una frase: si parte, si costruisce, si arriva e si lascia uno spazio. " +
                    "La dinamica (piano, forte, crescendo) crea il percorso; l'articolazione decide come iniziano e finiscono le note. " +
                    "Il rubato è una piccola flessione del tempo: rubare un po' di velocità per restituirla subito dopo, senza perdere la pulsazione."
            ),
            LessonPage.Theory(
                heading = "Pedale e intonazione",
                body = "Al pianoforte il pedale di risonanza unisce i suoni. Va cambiato quando cambia l'armonia, altrimenti tutto diventa confuso. " +
                    "Agli archi e al canto, l'intonazione si costruisce ascoltando gli intervalli e il centro tonale. " +
                    "Il coach di interpretazione della versione completa userà l'audio per valutare questi aspetti; nella MVP puoi allenarli con orecchio, metronomo e registrazioni mentali."
            ),
            LessonPage.Quiz(
                question = "Il rubato è...",
                options = listOf("una velocità fissa", "una flessione espressiva del tempo", "un tipo di pedale"),
                correctIndex = 1,
                explanation = "Il rubato è una libertà controllata: si prende tempo e lo si restituisce, mantenendo il respiro della frase."
            ),
            LessonPage.Quiz(
                question = "Il pedale di risonanza va cambiato...",
                options = listOf("a ogni nota comunque", "quando l'armonia cambia", "solo alla fine del brano"),
                correctIndex = 1,
                explanation = "Per non accumulare armonie diverse, il pedale si cambia quando cambia l'accordo o il colore armonico."
            )
        )
    )

    private val counterpoint = Lesson(
        id = "contrappunto",
        title = "Contrappunto: due voci che dialogano",
        summary = "Indipendenza, consonanza e imitazione in poche regole.",
        category = LessonCategory.THEORY,
        level = Level.ADVANCED,
        minutes = 7,
        xp = 40,
        pages = listOf(
            LessonPage.Theory(
                heading = "Due linee, non una melodia con accompagnamento",
                body = "Nel contrappunto le voci sono indipendenti ma devono stare insieme. Una melodia principale non basta: " +
                    "ogni voce ha un profilo riconoscibile. Si cercano consonanze sugli attacchi forti, si controllano gli intervalli " +
                    "e si evita che le voci si muovano sempre in parallelo. L'imitazione è uno dei giochi più potenti: una voce propone, l'altra risponde."
            ),
            LessonPage.Theory(
                heading = "Le quinte parallele",
                body = "Nello stile classico, due voci che scendono insieme per quinte giuste indeboliscono l'indipendenza. " +
                    "Non è un divieto assoluto in ogni linguaggio, ma è un ottimo esercizio di attenzione. " +
                    "Prova a comporre una battuta a due voci e controlla: moto contrario? consonanze? ogni voce ha un senso da sola?"
            ),
            LessonPage.Quiz(
                question = "Nel contrappunto, le voci...",
                options = listOf(
                    "devono muoversi sempre all'unisono",
                    "mantengono una certa indipendenza",
                    "suonano solo in ottava"
                ),
                correctIndex = 1,
                explanation = "L'indipendenza delle linee è il cuore del contrappunto, insieme al controllo delle consonanze."
            ),
            LessonPage.Quiz(
                question = "Una quinta parallela tra due voci è...",
                options = listOf("da evitare nello stile classico", "obbligatoria", "un tipo di accordo"),
                correctIndex = 0,
                explanation = "Nella scrittura classica le quinte parallele sono generalmente evitate perché riducono l'indipendenza delle voci."
            )
        )
    )

    val lessons: List<Lesson> = listOf(
        mapOfNotes,
        rhythmAndDurations,
        timeAndMetronome,
        firstTriads,
        majorOrMinor,
        intervals,
        harmonicTurnaround,
        phrasingAndDynamics,
        counterpoint
    )

    fun byId(id: String): Lesson? = lessons.firstOrNull { it.id == id }

    fun suggestedFor(level: Level): List<Lesson> = lessons.sortedBy {
        kotlin.math.abs(it.level.ordinal - level.ordinal)
    }
}
