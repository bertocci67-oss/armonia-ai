package com.armonia.app.ui.exercises

import android.Manifest
import android.app.Application
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.armonia.app.ArmoniaApp
import com.armonia.app.domain.audio.MetronomeEngine
import com.armonia.app.domain.audio.PitchReading
import com.armonia.app.domain.audio.TonePlayer
import com.armonia.app.domain.audio.TunerEngine
import com.armonia.app.domain.exercises.Exercise
import com.armonia.app.domain.exercises.ExerciseGenerator
import com.armonia.app.domain.model.ExerciseFocus
import com.armonia.app.domain.model.ExerciseKind
import com.armonia.app.domain.model.Instrument
import com.armonia.app.domain.model.Level
import com.armonia.app.domain.model.Profile
import com.armonia.app.domain.model.SessionKind
import com.armonia.app.domain.music.MusicTheory
import com.armonia.app.ui.components.ArmoniaTopBar
import com.armonia.app.ui.components.InfoPill
import com.armonia.app.ui.components.LevelPill
import com.armonia.app.ui.components.hasRecordingPermission
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

data class ExerciseUiState(
    val instrument: Instrument = Instrument.PIANO,
    val level: Level = Level.BEGINNER,
    val focus: ExerciseFocus = ExerciseFocus.INTONATION,
    val exercise: Exercise? = null,
    val sessionActive: Boolean = false,
    val finished: Boolean = false,
    val accuracy: Float = 0f,
    val xpEarned: Int = 0,
    val currentIndex: Int = 0,
    val wrongIndices: Set<Int> = emptySet(),
    val micActive: Boolean = false,
    val reading: PitchReading? = null,
    val feedback: String? = null,
    val micError: String? = null,
    val countIn: Int = 0,
    val tapsRecording: Boolean = false,
    val tapCount: Int = 0,
    val selectedAnswer: Int? = null,
    val answered: Boolean = false,
    val isPlayingReference: Boolean = false,
    val metronomeReady: Boolean = false
)

class ExerciseViewModel(application: Application) : AndroidViewModel(application) {

    private val progressRepository = (application as ArmoniaApp).container.progressRepository
    private val tonePlayer = TonePlayer()
    private val metronome = MetronomeEngine(application)

    private val _state = MutableStateFlow(ExerciseUiState())
    val state = _state.asStateFlow()

    private var instrument = Instrument.PIANO
    private var level = Level.BEGINNER
    private var micJob: Job? = null
    private var rhythmJob: Job? = null
    private var referenceJob: Job? = null
    private var startedAt = 0L
    private var rhythmStartedAt = 0L
    private var stableFrames = 0
    private var lastWrongAt = 0L
    private var lastReadingUiAt = 0L
    private var logged = false
    private val tapTimes = mutableListOf<Long>()

    init {
        metronome.prepare {
            _state.update { it.copy(metronomeReady = true) }
        }
    }

    fun configure(instrument: Instrument, level: Level) {
        if (this.instrument == instrument && this.level == level && _state.value.exercise != null) return
        this.instrument = instrument
        this.level = level
        val focus = if (instrument.isMelodic) _state.value.focus else ExerciseFocus.RHYTHM
        _state.update { it.copy(instrument = instrument, level = level, focus = focus) }
        generate(focus)
    }

    fun setFocus(focus: ExerciseFocus) {
        val effective = if (!instrument.isMelodic) ExerciseFocus.RHYTHM else focus
        if (effective == _state.value.focus && _state.value.exercise != null) return
        stopAllAudio()
        _state.update { it.copy(focus = effective) }
        generate(effective)
    }

    fun generate(focus: ExerciseFocus = _state.value.focus) {
        stopAllAudio()
        val exercise = ExerciseGenerator.generate(instrument, level, focus)
        logged = false
        _state.update {
            ExerciseUiState(
                instrument = instrument,
                level = level,
                focus = exercise.focus,
                exercise = exercise,
                metronomeReady = it.metronomeReady
            )
        }
    }

    fun startExercise() {
        val exercise = _state.value.exercise ?: return
        if (_state.value.sessionActive) return

        if (exercise.kind == ExerciseKind.RHYTHM && !_state.value.metronomeReady) {
            _state.update {
                it.copy(feedback = "Preparazione audio in corso: riprova tra un istante.")
            }
            return
        }

        stopAllAudio()
        logged = false
        startedAt = SystemClock.elapsedRealtime()
        tapTimes.clear()
        _state.update {
            it.copy(
                sessionActive = true,
                finished = false,
                accuracy = 0f,
                xpEarned = 0,
                currentIndex = 0,
                wrongIndices = emptySet(),
                feedback = null,
                micError = null,
                countIn = 0,
                tapsRecording = false,
                tapCount = 0,
                selectedAnswer = null,
                answered = false,
                reading = null,
                micActive = false,
                isPlayingReference = false
            )
        }

        when (exercise.kind) {
            ExerciseKind.PITCH -> startMic()
            ExerciseKind.RHYTHM -> startRhythm(exercise)
            ExerciseKind.EAR -> playReference()
        }
    }

    private fun startMic() {
        if (micJob?.isActive == true) return
        stableFrames = 0
        lastWrongAt = 0L
        lastReadingUiAt = 0L
        _state.update {
            it.copy(
                micActive = true,
                feedback = "Suona le note nell'ordine. Una nota alla volta."
            )
        }
        micJob = viewModelScope.launch {
            try {
                TunerEngine().stream().collect { reading -> handlePitchReading(reading) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        micActive = false,
                        micError = error.message ?: "Microfono non disponibile."
                    )
                }
            }
        }
    }

    private fun handlePitchReading(reading: PitchReading) {
        val snapshot = _state.value
        val exercise = snapshot.exercise ?: return
        if (!snapshot.sessionActive || snapshot.finished || exercise.kind != ExerciseKind.PITCH) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastReadingUiAt >= 150) {
            lastReadingUiAt = now
            _state.update { it.copy(reading = reading) }
        }

        val target = exercise.targetNotes.getOrNull(snapshot.currentIndex) ?: return
        val deviation = MusicTheory.cents(
            frequency = reading.frequency,
            targetFrequency = MusicTheory.frequency(target)
        )

        if (abs(deviation) <= 30.0) {
            stableFrames++
            if (stableFrames >= 2) {
                stableFrames = 0
                advancePitch(snapshot, exercise)
            }
        } else {
            stableFrames = 0
            if (reading.midi == target) {
                _state.update {
                    it.copy(feedback = "Quasi: cerca il centro della nota.")
                }
            } else if (now - lastWrongAt >= 800L) {
                lastWrongAt = now
                _state.update {
                    it.copy(
                        wrongIndices = it.wrongIndices + it.currentIndex,
                        feedback = "Non è questa la nota. Riprova."
                    )
                }
            }
        }
    }

    private fun advancePitch(
        snapshot: ExerciseUiState,
        exercise: Exercise
    ) {
        val next = snapshot.currentIndex + 1
        if (next >= exercise.targetNotes.size) {
            val wrong = snapshot.wrongIndices.size.coerceAtMost(exercise.targetNotes.size)
            val accuracy = if (exercise.targetNotes.isEmpty()) {
                0f
            } else {
                (exercise.targetNotes.size - wrong).toFloat() / exercise.targetNotes.size
            }
            finishSession(accuracy)
        } else {
            _state.update {
                it.copy(
                    currentIndex = next,
                    reading = null,
                    feedback = "Bene! Continua."
                )
            }
        }
    }

    private fun startRhythm(exercise: Exercise) {
        rhythmJob = viewModelScope.launch(Dispatchers.Default) {
            _state.update {
                it.copy(
                    countIn = 4,
                    tapsRecording = false,
                    tapCount = 0,
                    feedback = "Ascolta il conteggio di quattro battiti."
                )
            }
            val beatMs = 60_000.0 / exercise.bpm
            for (countdown in 4 downTo 1) {
                metronome.tick(accent = countdown == 4)
                _state.update { it.copy(countIn = countdown - 1) }
                delay(beatMs.roundToLong())
            }

            rhythmStartedAt = System.nanoTime()
            tapTimes.clear()
            _state.update {
                it.copy(
                    countIn = 0,
                    tapsRecording = true,
                    feedback = "Batti il ritmo sul pulsante TAP."
                )
            }
            val totalMs = (exercise.rhythmBeats.sum() * beatMs).roundToLong()
            delay(totalMs + 700L)
            _state.update { it.copy(tapsRecording = false) }
            evaluateRhythm(exercise, beatMs)
        }
    }

    fun tapRhythm() {
        if (!_state.value.tapsRecording) return
        tapTimes.add((System.nanoTime() - rhythmStartedAt) / 1_000_000L)
        _state.update { it.copy(tapCount = tapTimes.size) }
    }

    private fun evaluateRhythm(exercise: Exercise, beatMs: Double) {
        val onsets = mutableListOf<Double>()
        var cursor = 0.0
        exercise.rhythmBeats.forEach { duration ->
            onsets.add(cursor)
            cursor += duration * beatMs
        }

        val used = BooleanArray(tapTimes.size)
        var matched = 0
        for (onset in onsets) {
            var bestIndex = -1
            var bestDelta = Double.MAX_VALUE
            tapTimes.forEachIndexed { index, tap ->
                if (!used[index]) {
                    val delta = abs(tap - onset)
                    if (delta < bestDelta) {
                        bestDelta = delta
                        bestIndex = index
                    }
                }
            }
            if (bestIndex >= 0 && bestDelta <= 180.0) {
                used[bestIndex] = true
                matched++
            }
        }

        val extras = (tapTimes.size - matched).coerceAtLeast(0)
        val raw = if (onsets.isEmpty()) {
            0.0
        } else {
            (matched - extras * 0.25) / onsets.size.toDouble()
        }
        finishSession(raw.coerceIn(0.0, 1.0).toFloat())
    }

    fun pauseMicForReference() {
        if (!_state.value.micActive) return
        micJob?.cancel()
        micJob = null
        _state.update {
            it.copy(
                micActive = false,
                feedback = "Microfono in pausa: ascolta il riferimento, poi riprendi."
            )
        }
    }

    fun resumeMic() {
        val snapshot = _state.value
        if (!snapshot.sessionActive) return
        if (snapshot.exercise?.kind != ExerciseKind.PITCH) return
        startMic()
    }

    fun playReference() {
        val exercise = _state.value.exercise ?: return
        referenceJob?.cancel()
        referenceJob = viewModelScope.launch {
            _state.update { it.copy(isPlayingReference = true) }
            when (exercise.kind) {
                ExerciseKind.PITCH -> tonePlayer.playSequence(
                    midiNotes = exercise.targetNotes,
                    noteMs = 420,
                    gapMs = 100
                )

                ExerciseKind.EAR -> tonePlayer.playSequence(
                    midiNotes = listOf(
                        exercise.rootMidi,
                        exercise.rootMidi + exercise.intervalSemitones
                    ),
                    noteMs = 650,
                    gapMs = 180
                )

                ExerciseKind.RHYTHM -> {
                    val beatMs = 60_000.0 / exercise.bpm
                    exercise.rhythmBeats.forEachIndexed { index, duration ->
                        metronome.tick(accent = index == 0)
                        delay((duration * beatMs).roundToLong().coerceAtLeast(80L))
                    }
                }
            }
            _state.update { it.copy(isPlayingReference = false) }
        }
    }

    fun selectAnswer(index: Int) {
        val snapshot = _state.value
        val exercise = snapshot.exercise ?: return
        if (exercise.kind != ExerciseKind.EAR || snapshot.answered) return
        val correct = index == exercise.correctIndex
        _state.update {
            it.copy(
                selectedAnswer = index,
                answered = true,
                feedback = if (correct) {
                    "Esatto! È ${MusicTheory.intervalName(exercise.intervalSemitones)}."
                } else {
                    "Non è corretto: era ${MusicTheory.intervalName(exercise.intervalSemitones)}."
                }
            )
        }
    }

    fun finishEar() {
        val snapshot = _state.value
        val exercise = snapshot.exercise ?: return
        if (exercise.kind != ExerciseKind.EAR || !snapshot.answered) return
        finishSession(if (snapshot.selectedAnswer == exercise.correctIndex) 1f else 0f)
    }

    private fun finishSession(accuracy: Float) {
        if (logged) return
        logged = true
        val snapshot = _state.value
        val exercise = snapshot.exercise ?: return
        stopAllAudio()

        val durationSec = ((SystemClock.elapsedRealtime() - startedAt) / 1_000)
            .toInt()
            .coerceAtLeast(1)
        val safeAccuracy = accuracy.coerceIn(0f, 1f)
        val xp = (exercise.xp * (0.6f + safeAccuracy * 0.4f))
            .roundToInt()
            .coerceAtLeast(5)

        _state.update {
            it.copy(
                sessionActive = false,
                finished = true,
                micActive = false,
                isPlayingReference = false,
                accuracy = safeAccuracy,
                xpEarned = xp,
                feedback = null,
                reading = null,
                tapsRecording = false
            )
        }

        viewModelScope.launch {
            progressRepository.log(
                kind = SessionKind.EXERCISE,
                refId = exercise.id,
                title = exercise.title,
                skill = exercise.focus.skill,
                accuracy = safeAccuracy,
                xp = xp,
                durationSec = durationSec
            )
        }
    }

    fun dismissSession() {
        stopAllAudio()
        _state.update {
            it.copy(
                sessionActive = false,
                finished = false,
                micActive = false,
                feedback = null,
                reading = null,
                selectedAnswer = null,
                answered = false,
                tapsRecording = false,
                countIn = 0
            )
        }
    }

    fun onScreenStopped() {
        if (_state.value.sessionActive) {
            dismissSession()
        } else {
            stopAllAudio()
        }
    }

    private fun stopAllAudio() {
        micJob?.cancel()
        micJob = null
        rhythmJob?.cancel()
        rhythmJob = null
        referenceJob?.cancel()
        referenceJob = null
        tonePlayer.stop()
        _state.update { it.copy(micActive = false, isPlayingReference = false) }
    }

    override fun onCleared() {
        stopAllAudio()
        tonePlayer.release()
        metronome.release()
        super.onCleared()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(profile: Profile) {
    val context = LocalContext.current
    val exerciseViewModel: ExerciseViewModel = viewModel()
    val ui by exerciseViewModel.state.collectAsStateWithLifecycle()
    var permissionDenied by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionDenied = !granted
        if (granted) exerciseViewModel.startExercise()
    }

    val startExercise = {
        if (
            ui.exercise?.kind == ExerciseKind.PITCH &&
            !hasRecordingPermission(context)
        ) {
            permissionDenied = false
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            exerciseViewModel.startExercise()
        }
    }

    LaunchedEffect(profile.instrument, profile.level) {
        exerciseViewModel.configure(profile.instrument, profile.level)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        exerciseViewModel.onScreenStopped()
    }

    DisposableEffect(Unit) {
        onDispose { exerciseViewModel.onScreenStopped() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ArmoniaTopBar(title = "Micro-studi")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Esercizi generati su misura per ${profile.instrument.label.lowercase()} e livello " +
                    "${profile.level.label.lowercase()}. La difficoltà cresce con te.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (!ui.sessionActive && !ui.finished) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExerciseFocus.entries.forEach { focus ->
                        FilterChip(
                            selected = ui.focus == focus,
                            onClick = { exerciseViewModel.setFocus(focus) },
                            label = { Text(focus.label) },
                            enabled = profile.instrument.isMelodic || focus == ExerciseFocus.RHYTHM
                        )
                    }
                }
                if (!profile.instrument.isMelodic) {
                    Text(
                        text = "La batteria lavora sul ritmo: gli altri focus non sono disponibili.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            val exercise = ui.exercise
            when {
                exercise == null -> {
                    Text("Preparazione esercizio…", style = MaterialTheme.typography.bodyMedium)
                }

                ui.finished -> {
                    ExerciseResult(
                        ui = ui,
                        exercise = exercise,
                        onRestart = startExercise,
                        onNew = { exerciseViewModel.generate(ui.focus) }
                    )
                }

                ui.sessionActive -> {
                    ActiveExercise(
                        ui = ui,
                        exercise = exercise,
                        permissionDenied = permissionDenied,
                        onTap = exerciseViewModel::tapRhythm,
                        onPlay = exerciseViewModel::playReference,
                        onPauseMic = exerciseViewModel::pauseMicForReference,
                        onResumeMic = exerciseViewModel::resumeMic,
                        onAnswer = exerciseViewModel::selectAnswer,
                        onFinishEar = exerciseViewModel::finishEar,
                        onExit = exerciseViewModel::dismissSession
                    )
                }

                else -> {
                    ExercisePreview(
                        ui = ui,
                        exercise = exercise,
                        permissionDenied = permissionDenied,
                        onGenerate = { exerciseViewModel.generate(ui.focus) },
                        onPlay = exerciseViewModel::playReference,
                        onStart = startExercise
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ExercisePreview(
    ui: ExerciseUiState,
    exercise: Exercise,
    permissionDenied: Boolean,
    onGenerate: () -> Unit,
    onPlay: () -> Unit,
    onStart: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InfoPill(text = exercise.focus.label)
                Spacer(modifier = Modifier.size(8.dp))
                LevelPill(level = exercise.level)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = exercise.title,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = exercise.instructions,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            when (exercise.kind) {
                ExerciseKind.PITCH -> {
                    Text(
                        text = "Sequenza · ${exercise.targetNotes.size} note · ${exercise.bpm} BPM consigliati",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    NoteStrip(
                        notes = exercise.targetNotes,
                        currentIndex = -1,
                        wrongIndices = emptySet()
                    )
                }

                ExerciseKind.RHYTHM -> {
                    Text(
                        text = "4/4 · ${exercise.bpm} BPM · ${exercise.rhythmBeats.size} attacchi",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RhythmStrip(rhythmBeats = exercise.rhythmBeats)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onPlay,
                        enabled = !ui.isPlayingReference && ui.metronomeReady
                    ) {
                        Icon(Icons.Outlined.VolumeUp, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(if (ui.isPlayingReference) "Riproduzione…" else "Ascolta il ritmo")
                    }
                }

                ExerciseKind.EAR -> {
                    Text(
                        text = if (ui.answered) {
                            "Ascolto completato"
                        } else {
                            "Ascolta le due note e riconosci l'intervallo."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onPlay,
                        enabled = !ui.isPlayingReference
                    ) {
                        Icon(Icons.Outlined.VolumeUp, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(if (ui.isPlayingReference) "Riproduzione…" else "Ascolta l'esempio")
                    }
                }
            }

            if (permissionDenied) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Permesso microfono non concesso. Puoi riprovare: serve solo per ascoltare l'intonazione.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (exercise.kind == ExerciseKind.RHYTHM && !ui.metronomeReady) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Preparazione dei click in corso…",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onGenerate,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Altro")
                }
                Button(
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                    enabled = exercise.kind != ExerciseKind.RHYTHM || ui.metronomeReady
                ) {
                    Text("Inizia")
                }
            }
        }
    }
}

@Composable
private fun ActiveExercise(
    ui: ExerciseUiState,
    exercise: Exercise,
    permissionDenied: Boolean,
    onTap: () -> Unit,
    onPlay: () -> Unit,
    onPauseMic: () -> Unit,
    onResumeMic: () -> Unit,
    onAnswer: (Int) -> Unit,
    onFinishEar: () -> Unit,
    onExit: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Esercizio in corso",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = exercise.focus.label,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                TextButton(onClick = onExit) {
                    Text("Esci")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (exercise.kind) {
                ExerciseKind.PITCH -> PitchExerciseContent(
                    ui = ui,
                    exercise = exercise,
                    permissionDenied = permissionDenied,
                    onPlay = onPlay,
                    onPauseMic = onPauseMic,
                    onResumeMic = onResumeMic
                )

                ExerciseKind.RHYTHM -> RhythmExerciseContent(
                    ui = ui,
                    exercise = exercise,
                    onTap = onTap
                )

                ExerciseKind.EAR -> EarExerciseContent(
                    ui = ui,
                    exercise = exercise,
                    onPlay = onPlay,
                    onAnswer = onAnswer,
                    onFinish = onFinishEar
                )
            }
        }
    }
}

@Composable
private fun PitchExerciseContent(
    ui: ExerciseUiState,
    exercise: Exercise,
    permissionDenied: Boolean,
    onPlay: () -> Unit,
    onPauseMic: () -> Unit,
    onResumeMic: () -> Unit
) {
    LinearProgressIndicator(
        progress = {
            if (exercise.targetNotes.isEmpty()) 0f
            else ui.currentIndex.toFloat() / exercise.targetNotes.size
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "Nota ${ui.currentIndex + 1} di ${exercise.targetNotes.size}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
    NoteStrip(
        notes = exercise.targetNotes,
        currentIndex = ui.currentIndex,
        wrongIndices = ui.wrongIndices
    )

    Spacer(modifier = Modifier.height(12.dp))

    val target = exercise.targetNotes.getOrNull(ui.currentIndex)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.Mic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.size(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Nota attesa",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = target?.let { MusicTheory.noteName(it) } ?: "—",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (ui.micActive) "In ascolto" else "Microfono spento",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val reading = ui.reading
            Text(
                text = reading?.let { "${MusicTheory.noteName(it.midi)} · ${it.cents.roundToInt()} cent" }
                    ?: "—",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    ui.feedback?.let { feedback ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = feedback,
            style = MaterialTheme.typography.bodyMedium
        )
    }

    if (permissionDenied) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Permesso microfono negato. Esci e riprova per concederlo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }

    ui.micError?.let { error ->
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }

    Spacer(modifier = Modifier.height(12.dp))
    OutlinedButton(
        onClick = {
            if (ui.micActive) onPauseMic()
            onPlay()
        },
        enabled = !ui.isPlayingReference,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Outlined.VolumeUp, contentDescription = null)
        Spacer(modifier = Modifier.size(6.dp))
        Text(if (ui.isPlayingReference) "Ascolto…" else "Ascolta il riferimento")
    }
    if (!ui.micActive) {
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onResumeMic,
            enabled = !ui.isPlayingReference,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.Mic, contentDescription = null)
            Spacer(modifier = Modifier.size(6.dp))
            Text("Riprendi microfono")
        }
    }
}

@Composable
private fun RhythmExerciseContent(
    ui: ExerciseUiState,
    exercise: Exercise,
    onTap: () -> Unit
) {
    Text(
        text = if (ui.countIn > 0) "Conteggio: ${ui.countIn}" else "Riproduci il ritmo",
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(modifier = Modifier.height(8.dp))
    RhythmStrip(rhythmBeats = exercise.rhythmBeats)
    Spacer(modifier = Modifier.height(14.dp))
    Button(
        onClick = onTap,
        enabled = ui.tapsRecording,
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "TAP",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "${ui.tapCount} colpi",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = ui.feedback ?: "Attendi il conteggio, poi segui la sequenza.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun EarExerciseContent(
    ui: ExerciseUiState,
    exercise: Exercise,
    onPlay: () -> Unit,
    onAnswer: (Int) -> Unit,
    onFinish: () -> Unit
) {
    Text(
        text = "Riconosci l'intervallo che hai ascoltato.",
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(
        onClick = onPlay,
        enabled = !ui.isPlayingReference
    ) {
        Icon(Icons.Outlined.VolumeUp, contentDescription = null)
        Spacer(modifier = Modifier.size(6.dp))
        Text(if (ui.isPlayingReference) "Ascolto…" else "Ascolta l'intervallo")
    }
    Spacer(modifier = Modifier.height(12.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        exercise.options.forEachIndexed { index, option ->
            val selected = ui.selectedAnswer == index
            val correct = index == exercise.correctIndex
            val container = when {
                ui.answered && correct -> MaterialTheme.colorScheme.tertiaryContainer
                ui.answered && selected -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(enabled = !ui.answered) { onAnswer(index) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = container)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (ui.answered && correct) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    } else if (ui.answered && selected) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    ui.feedback?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = it, style = MaterialTheme.typography.bodyMedium)
    }

    if (ui.answered) {
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Concludi esercizio")
        }
    }
}

@Composable
private fun NoteStrip(
    notes: List<Int>,
    currentIndex: Int,
    wrongIndices: Set<Int>
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        notes.forEachIndexed { index, midi ->
            val wrong = index in wrongIndices
            val current = index == currentIndex
            val background = when {
                wrong -> MaterialTheme.colorScheme.errorContainer
                current -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
            Card(
                modifier = Modifier.border(
                    width = if (current) 2.dp else 1.dp,
                    color = if (current) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    shape = RoundedCornerShape(14.dp)
                ),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = background)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = MusicTheory.noteName(midi),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RhythmStrip(rhythmBeats: List<Float>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        rhythmBeats.forEach { duration ->
            val width = (duration * 34f).coerceAtLeast(20f).dp
            Box(
                modifier = Modifier
                    .width(width)
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = durationLabel(duration),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

private fun durationLabel(duration: Float): String = when (duration) {
    0.5f -> "½"
    1.5f -> "1½"
    2f -> "2"
    else -> duration.roundToInt().toString()
}

@Composable
private fun ExerciseResult(
    ui: ExerciseUiState,
    exercise: Exercise,
    onRestart: () -> Unit,
    onNew: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.GraphicEq,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(46.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = when {
                    ui.accuracy >= 0.85f -> "Ottimo lavoro"
                    ui.accuracy >= 0.6f -> "Buon lavoro"
                    else -> "Ci siamo quasi"
                },
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = exercise.focus.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${(ui.accuracy * 100).roundToInt()}%",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = "precisione · +${ui.xpEarned} XP",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Sessione salvata nei progressi.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onNew,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Nuovo studio")
                }
                Button(
                    onClick = onRestart,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ripeti")
                }
            }
        }
    }
}

