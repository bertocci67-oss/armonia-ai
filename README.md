# Armonia AI

App Android per imparare la musica a casa, con un percorso guidato e intelligenza artificiale **on-device**. Questo repository contiene la **prima MVP** in Kotlin e Jetpack Compose: accordatore, metronomo, spartiti interattivi, micro-lezioni, micro-studi progressivi e storico dei progressi.

> **Stato:** MVP tecnica, non pubblicata. Non contiene backend, account, pubblicità o tracciamento. Tutto il contenuto è incluso nell'app e funziona **offline**.

---

## Il percorso obbligatorio

Al primo avvio l'app mostra un onboarding a schermo intero e **non permette di proseguire** finché non sono state scelte entrambe le informazioni:

1. **Strumento** — pianoforte, chitarra, canto, violino, batteria, basso, ukulele, flauto.
2. **Livello** — principiante, intermedio, avanzato.

Le scelte vengono salvate in locale con DataStore. Sono modificabili da **Profilo e privacy**, ma non possono essere saltate: senza profilo la navigazione principale non viene creata.

## Cosa c'è nella MVP

| Funzione | Stato MVP |
|---|---|
| Onboarding obbligatorio (strumento + livello) | ✅ |
| Accordatore cromatico con rilevamento dell'intonazione | ✅ microfono, YIN, analisi in tempo reale |
| Metronomo intelligente | ✅ BPM, tempi, suddivisioni, accento, tap tempo |
| Spartiti interattivi | ✅ 6 brani/arrangiamenti, lettura, loop, trasposizione, melodia/mani, diteggiatura, playback |
| Pratica guidata con microfono | ✅ ascolto della voce principale, evidenziazione, errori e rallentamento automatico |
| Micro-lezioni | ✅ note, ritmo, accordi, teoria, ear training, interpretazione |
| Quiz ed ear training con feedback immediato | ✅ |
| Generatore di micro-studi progressivi | ✅ intonazione, ritmo, orecchio |
| Storico dei progressi | ✅ XP, streak, minuti, precisione per abilità, sessioni recenti |
| Privacy by design e offline | ✅ nessun permesso di rete, audio mai salvato |
| Trascrizione audio → spartito, coach avanzato, armonia/contrappunto, esami, freemium | 🗺️ roadmap (vedi sotto) |

### Accordatore

- Cattura PCM 16 bit dal microfono e stima la frequenza con l'algoritmo **YIN** (differenza cumulativa normalizzata + interpolazione parabolica).
- Mostra nota, frequenza, cent e uno strumento analogico con isteresi visiva.
- Riferimenti sonori per le accordature di chitarra, basso, violino e ukulele.
- Modalità a basso consumo: campionamento e buffer più piccoli, meno frame al secondo.
- Il microfono si spegne quando si lascia la schermata o l'app va in background.

### Metronomo

- 40–240 BPM, tempi 2/4, 3/4, 4/4 e 6/8, suddivisioni in ottavi e terzine, accento sul primo battito.
- Click generati localmente e caricati in `SoundPool`, senza file audio di grandi dimensioni.
- Tap tempo e conteggio visivo dei battiti.
- Le sessioni sufficientemente lunghe vengono registrate nello storico.

### Spartiti interattivi

- Motore di rendering su `Canvas` (niente librerie pesanti): pentagramma, note, gambi, alterazioni, diteggiatura, battute e numeri di battuta.
- Riproduzione sintetizzata con evidenziazione della nota corrente.
- Loop su una sezione di battute, trasposizione, isolamento mano destra/sinistra, semplificazione a melodia.
- **Pratica guidata:** il microfono ascolta la voce principale, confronta la nota con quella attesa e mostra l'intonazione; dopo tre errori il tempo scende automaticamente del 10% (fino al 50%).
- Brani di **pubblico dominio** in arrangiamento didattico originale e studi originali, per evitare problemi di copyright.

### Micro-lezioni

- Categorie: note, ritmo, accordi, teoria, orecchio, interpretazione.
- Lezioni brevi con teoria, quiz a risposta multipla ed esercizi di ascolto (intervalli, maggiore/minore).
- Feedback immediato con spiegazione; XP alla prima risposta corretta.
- Contenuti per tutti i livelli, compresa un'introduzione al fraseggio e al contrappunto.

### Micro-studi progressivi

- Generatore locale che adatta estensione, difficoltà ritmica e intervalli a strumento e livello.
- Tre famiglie: intonazione con verifica dal microfono, ritmo con valutazione temporale dei tap, ear training con scelta dell'intervallo.
- XP proporzionali alla precisione e registrazione automatica della sessione.

### Progressi

- Database Room locale con sessioni (tipo, brano/lezione, abilità, precisione, XP, durata).
- Streak giornaliero, XP totale, minuti dell'ultima settimana, precisione media e per abilità.
- Grafico settimanale e cronologia recente.

## Anteprime dell'interfaccia

> **Anteprime grafiche (mockup).** Riproducono i layout reali dell'app, ma non sono catture da un dispositivo fisico.

<p align='center'>
  <img src='https://files.catbox.moe/agdfbm.png' width='235' alt='Home di Armonia AI con il percorso suggerito'>
  <img src='https://files.catbox.moe/oj89db.png' width='235' alt='Catalogo degli spartiti interattivi'>
  <img src='https://files.catbox.moe/fr1pvh.png' width='235' alt='Accordatore cromatico con La4 a 440 Hz'>
</p>

## Installare l'APK

L'APK di debug (`armonia-ai-debug.apk`, circa 18 MB) è scaricabile da **[questo link](https://files.catbox.moe/fvezxs.apk)**. Non è pubblicato sul Play Store.

![Guida illustrata: scarica, apri, consenti l'installazione, avvia](https://files.catbox.moe/47kvx9.png)

1. **Scarica** `armonia-ai-debug.apk` toccando il link dal telefono. Se il browser chiede conferma, accetta il download.
2. **Apri** il file dalla notifica di download o dall'app File/Download.
3. **Consenti** l'installazione da origini sconosciute per l'app che apre il file (di solito File o Chrome), se Android lo chiede.
4. **Installa e apri**. Concedi il microfono solo quando usi l'accordatore o la pratica guidata.

L'APK è firmato con la chiave di debug: va bene per provare l'app, non per pubblicarla sul Play Store. Se vuoi verificare che il download sia integro, l'impronta SHA-256 è:

```
c58cff85ad287f25483fdebbc0ebf1e331c06cf1809432319aff3e7b07ce1d26
```

Se il link non funziona, puoi generare l'APK dal codice seguendo la sezione [Come provare la MVP](#come-provare-la-mvp).

## Privacy by design

- L'unico permesso sensibile è `RECORD_AUDIO`; **non c'è il permesso INTERNET**.
- L'audio è elaborato in memoria, frame per frame, e non viene mai registrato su disco né caricato.
- I progressi e le preferenze restano sul dispositivo (Room + DataStore).
- Il backup Android è disattivato per non esportare i dati locali.
- Il microfono viene fermato quando la schermata non è visibile.

## Prestazioni su Android

- Modelli DSP leggeri e interamente on-device; nessun modello remoto o rete.
- Un solo flusso audio alla volta, buffer riutilizzati e nessuna allocazione pesante nel loop di analisi.
- Accordatore a 44,1 kHz (o 22,05 kHz in risparmio energetico), con soglia RMS e confidenza per evitare elaborazioni inutili.
- Metronomo con **SoundPool** e click WAV generati una sola volta nella cache.
- Compose con stato limitato alle schermate e raccolta lifecycle-aware.
- R8/minify e rimozione delle risorse in release.
- `minSdk 26` per coprire dispositivi di fascia bassa; nessuna dipendenza da ML Kit o TensorFlow.

## Come provare la MVP

Requisiti: **Android Studio** recente con JDK 17 e Android SDK 35, oppure **Gradle 8.7** installato.

Con Android Studio: apri il progetto e premi **Run**. Da terminale, nella cartella del progetto:

```bash
gradle assembleDebug
```

L'APK di debug viene creato in `app/build/outputs/apk/debug/app-debug.apk`.

> Il repository non include il binario `gradle-wrapper.jar`. Se preferisci usare `./gradlew`, generalo una volta con `gradle wrapper --gradle-version 8.7` (serve Gradle 8.7 installato); in alternativa usa direttamente `gradle`.

Aprire il progetto in Android Studio e avviare su un dispositivo/emulatore con microfono (l'emulatore può usare il microfono dell'host). Concedere il permesso microfono quando richiesto. Le funzioni senza microfono (metronomo, spartiti in ascolto, lezioni) funzionano anche senza permesso.

> La build debug è stata verificata con JDK 17, Gradle 8.7 e Android SDK 35. Non è stata eseguita una prova su dispositivo fisico: microfono e resa dello spartito vanno provati sul tuo telefono.

## Struttura del progetto

```
app/src/main/java/com/armonia/app/
├── ArmoniaApp.kt                 # Application + container manuale
├── MainActivity.kt               # edge-to-edge, gate onboarding
├── data/
│   ├── ProfileRepository.kt      # DataStore: strumento, livello
│   └── ProgressRepository.kt     # Room: sessioni e statistiche
├── domain/
│   ├── audio/                    # YIN, AudioRecord, TonePlayer, metronomo
│   ├── exercises/                # generatore di micro-studi
│   ├── lessons/                  # catalogo lezioni e quiz
│   ├── model/                    # modelli condivisi
│   ├── music/                    # teoria, frequenze, accordature
│   └── scores/                   # modello spartito + libreria
└── ui/
    ├── components/               # componenti riusabili
    ├── exercises/                # generazione e sessione interattiva
    ├── home/
    ├── lessons/
    ├── metronome/
    ├── navigation/
    ├── onboarding/
    ├── profile/
    ├── progress/
    ├── scores/                   # lista, dettaglio, renderer Canvas
    ├── theme/
    └── tuner/
```

Architettura volutamente semplice, senza DI framework: un `AppContainer` creato in `ArmoniaApp`, repository locali e ViewModel AndroidX. Nessuna chiamata di rete.

## Contenuti musicali

- *Inno alla gioia* — Beethoven, tema di pubblico dominio, arrangiamento didattico originale.
- *Ah! Vous dirai-je, Maman* (Twinkle Twinkle) — tradizionale di pubblico dominio, arrangiamento didattico originale.
- *Minuetto in Sol* — tema di pubblico dominio, arrangiamento semplificato originale.
- *Giro armonico in Do* e *Arpeggi in La minore* — studi originali creati per questo progetto.
- Lezioni, quiz e micro-studi sono testi e contenuti originali.

Le attribuzioni sono riportate anche nella scheda di ogni spartito.

## Limiti noti della MVP

- Il rilevamento dell'altezza è **monofonico**: adatto a melodia, canto e note singole, non a un accordo suonato al pianoforte. Per questo la pratica guidata segue la voce principale.
- La trascrizione audio → spartito non è inclusa: richiede un modello di trascrizione (AMS) e una pipeline di quantizzazione; è la prima voce della roadmap.
- Non ci sono account, sincronizzazione cloud, pagamenti o contenuti scaricabili.
- La build debug compila e produce l'APK, ma non è stata eseguita una prova audio su un dispositivo fisico.

## Roadmap verso l'app completa

1. **Spartiti intelligenti**: trascrizione audio→MusicXML con modello leggero on-device, quantizzazione, separazione delle voci, import MusicXML/MIDI, diteggiatura automatica.
2. **Coach di interpretazione**: analisi di dinamica, rubato, pedale, fraseggio e intonazione con modelli audio leggeri e feedback spiegabile.
3. **Armonia e contrappunto**: analisi armonica, esercizi di voce leading, contrappunto a due voci, correzione con regole + ML.
4. **Preparazione esami e audizioni**: simulazioni cronometrate, valutazione multidimensionale, report esportabile.
5. **Piattaforma**: sync opzionale end-to-end, modello freemium/pay-per-use, acquisti in-app, download offline degli spartiti premium.
6. **Performance**: baseline profile, modelli NNAPI/GPU delegati per la trascrizione, test su dispositivi di fascia bassa.

## Licenza e contributi

Il codice è pubblicato per essere esplorato e discusso. Non è ancora stata scelta una licenza open source: prima di riutilizzare il codice o i contenuti, aprire una issue nel repository. I brani di pubblico dominio mantengono il loro stato; gli arrangiamenti e gli studi originali non sono di pubblico dominio.
