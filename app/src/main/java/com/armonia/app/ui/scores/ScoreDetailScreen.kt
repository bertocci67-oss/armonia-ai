package com.armonia.app.ui.scores

import android.Manifest
import android.app.Application
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.armonia.app.ArmoniaApp
import com.armonia.app.domain.audio.PitchReading
import com.armonia.app.domain.audio.TonePlayer
import com.armonia.app.domain.audio.TunerEngine
import com.armonia.app.domain.model.Hand
import com.armonia.app.domain.model.Level
import com.armonia.app.domain.model.SessionKind
import com.armonia.app.domain.model.Skill
import com.armonia.app.domain.music.MusicTheory
import com.armonia.app.domain.scores.Score
import com.armonia.app.domain.scores.ScoreLibrary
import com.armonia.app.domain.scores.ScoreNote
import com.armonia.app.ui.components.ArmoniaTopBar
import com.armonia.app.ui.components.EmptyState
import com.armonia.app.ui.components.InfoPill
import com.armonia.app.ui.components.LevelPill
import com.armonia.app.ui.components.hasRecordingPermission
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

enum class PracticeMode(val label: String) {
    LISTEN("Ascolto"),
    PLAY("Suona con feedback")
}

data class ScoreUiState(
    val scoreAvailable: Boolean = false,
    val title: String = "",
    val composer: String = "",
    val sourceNote: String = "",
    val license: String = "",
    val level: Level = Level.BEGINNER,
    val beatsPerBar: Int = 4,
    val beatUnit: Int = 4,
    val originalTempoBpm: Int = 80,
    val originalBars: Int = 1,
    val displayBars: Int = 1,
    val notes: List<ScoreNote> = emptyList(),
    val hand: Hand = Hand.BOTH,
    val melodyOnly: Boolean = false,
    val semitones: Int = 0,
    val loop: Boolean = true,
    val loopStartBar: Int = 1,
    val loopEndBar: Int = 1,
    val tempoPercent: Int = 100,
    val mode: PracticeMode = PracticeMode.LISTEN,
    val isPlaying: Boolean = false,
    val currentIndex: Int = -1,
    val expectedIndex: Int = -1,
    val wrongIndices: Set<Int> = emptySet(),
    val micActive: Boolean = false,
    val reading: PitchReading? = null,
    val feedback: String? = null,
    val errors: Int = 0,
    val autoSlowed: Boolean = false,
    val practiceComplete: Boolean = false,
    val lastAccuracy: Float = 0f,
    val lastXp: Int = 0,
    val micError: String? = null
)

class ScoreViewModel(
    application: Application,
    scoreId: String
) : AndroidViewModel(application) {

    private val baseScore: Score? = ScoreLibrary.byId(scoreId)
    private val progressRepository = (application as ArmoniaApp).container.progressRepository
    private val tonePlayer = TonePlayer()

    private var micJob: Job? = null
    private var playbackJob: Job? = null
    private var practiceStartedAt = 0L
    private var stableFrames = 0
    private var lastWrongAt = 0L
    private var lastReadingUiAt = 0L

    private val _uiState = MutableStateFlow(initialState())
    val uiState = _uiState.asStateFlow()

    init {
        if (baseScore != null) rebuild()
    }

    private fun initialState(): ScoreUiState {
        val score = baseScore
        return if (score == null) {
            ScoreUiState()
        } else {
            ScoreUiState(
                scoreAvailable = true,
                title = score.title,
                composer = score.composer,
                sourceNote = score.sourceNote,
                license = score.license,
                level = score.level,
                beatsPerBar = score.beatsPerBar,
                beatUnit = score.beatUnit,
                originalTempoBpm = score.tempoBpm,
                originalBars = score.barCount,
                loopStartBar = 1,
                loopEndBar = score.barCount
            )
        }
    }

    private fun rebuild() {
        val score = baseScore ?: return
        val snapshot = _uiState.value
        val startBar = if (snapshot.loop) {
            snapshot.loopStartBar.coerceIn(1, score.barCount)
        } else {
            1
        }
        val endBar = if (snapshot.loop) {
            snapshot.loopEndBar.coerceIn(startBar, score.barCount)
        } else {
            score.barCount
        }

        val prepared = score.prepared(
            hand = snapshot.hand,
            melody = snapshot.melodyOnly || snapshot.mode == PracticeMode.PLAY,
            startBar = startBar,
            endBar = endBar,
            semitones = snapshot.semitones
        )

        _uiState.update { current ->
            current.copy(
                notes = prepared.notes,
                displayBars = endBar - startBar + 1,
                loopStartBar = startBar,
                loopEndBar = endBar,
                currentIndex = if (current.mode == PracticeMode.PLAY) 0 else -1,
                expectedIndex = if (current.mode == PracticeMode.PLAY) 0 else -1,
                wrongIndices = emptySet(),
                errors = 0,
                practiceComplete = false,
                lastAccuracy = 0f,
                lastXp = 0,
                autoSlowed = false,
                feedback = if (current.mode == PracticeMode.PLAY) {
                    "Suona la nota evidenziata."
                } else {
                    null
                },
                reading = null,
                micActive = false,
                isPlaying = false
            )
        }
    }

    fun setHand(hand: Hand) {
        if (hand == _uiState.value.hand) return
        stopAudio()
        _uiState.update { it.copy(hand = hand) }
        rebuild()
    }

    fun setMelodyOnly(enabled: Boolean) {
        if (_uiState.value.mode == PracticeMode.PLAY) return
        if (enabled == _uiState.value.melodyOnly) return
        stopAudio()
        _uiState.update { it.copy(melodyOnly = enabled) }
        rebuild()
    }

    fun transposeBy(delta: Int) {
        val updated = (_uiState.value.semitones + delta).coerceIn(-6, 6)
        if (updated == _uiState.value.semitones) return
        stopAudio()
        _uiState.update { it.copy(semitones = updated) }
        rebuild()
    }

    fun changeTempo(delta: Int) {
        _uiState.update {
            it.copy(tempoPercent = (it.tempoPercent + delta).coerceIn(50, 120))
        }
    }

    fun setLoop(enabled: Boolean) {
        if (enabled == _uiState.value.loop) return
        stopAudio()
        _uiState.update { it.copy(loop = enabled) }
        rebuild()
    }

    fun adjustLoopStart(delta: Int) {
        val current = _uiState.value
        if (!current.loop) return
        val newStart = (current.loopStartBar + delta).coerceIn(1, current.loopEndBar)
        if (newStart == current.loopStartBar) return
        stopAudio()
        _uiState.update { it.copy(loopStartBar = newStart) }
        rebuild()
    }

    fun adjustLoopEnd(delta: Int) {
        val current = _uiState.value
        if (!current.loop) return
        val newEnd = (current.loopEndBar + delta)
            .coerceIn(current.loopStartBar, current.originalBars)
        if (newEnd == current.loopEndBar) return
        stopAudio()
        _uiState.update { it.copy(loopEndBar = newEnd) }
        rebuild()
    }

    fun setMode(mode: PracticeMode) {
        if (mode == _uiState.value.mode) return
        stopAudio()
        _uiState.update { it.copy(mode = mode, practiceComplete = false) }
        rebuild()
    }

    fun togglePlayback() {
        if (_uiState.value.isPlaying) {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        val snapshot = _uiState.value
        if (snapshot.notes.isEmpty()) return

        stopMic()
        val groups = snapshot.notes
            .withIndex()
            .groupBy { it.value.startBeats }
            .toSortedMap()

        playbackJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPlaying = true,
                    practiceComplete = false,
                    currentIndex = groups.values.firstOrNull()?.firstOrNull()?.index ?: -1
                )
            }

            do {
                for ((startBeats, indexedNotes) in groups) {
                    if (!isActive) break

                    val firstIndex = indexedNotes.first().index
                    _uiState.update { it.copy(currentIndex = firstIndex) }

                    val chord = indexedNotes.map { it.value }
                    val durationBeats = (
                        chord.maxOf { it.startBeats + it.durationBeats } - startBeats
                        ).coerceAtLeast(0.25f)
                    val tempoPercent = _uiState.value.tempoPercent.coerceIn(50, 120)
                    val beatMs = 60_000.0 / (snapshot.originalTempoBpm * tempoPercent / 100.0)
                    val durationMs = (durationBeats * beatMs).roundToInt().coerceAtLeast(80)

                    tonePlayer.playChord(
                        midiNotes = chord.map { it.midi },
                        durationMs = durationMs,
                        volume = 0.35f
                    )
                    delay((durationBeats * beatMs).roundToInt().toLong().coerceAtLeast(80))
                }
            } while (isActive && _uiState.value.loop)

            if (isActive) {
                _uiState.update { it.copy(isPlaying = false, currentIndex = -1) }
            }
        }
    }

    fun startMic() {
        val snapshot = _uiState.value
        if (snapshot.mode != PracticeMode.PLAY || snapshot.notes.isEmpty()) return
        if (micJob?.isActive == true) return

        stopPlayback()
        if (snapshot.practiceComplete) {
            _uiState.update {
                it.copy(
                    practiceComplete = false,
                    expectedIndex = 0,
                    currentIndex = 0,
                    wrongIndices = emptySet(),
                    errors = 0,
                    lastAccuracy = 0f,
                    lastXp = 0,
                    autoSlowed = false
                )
            }
            practiceStartedAt = 0L
        }

        if (practiceStartedAt == 0L) {
            practiceStartedAt = SystemClock.elapsedRealtime()
        }
        stableFrames = 0
        lastWrongAt = 0L
        lastReadingUiAt = 0L

        _uiState.update {
            it.copy(
                micActive = true,
                micError = null,
                feedback = "Suona la nota evidenziata.",
                errors = 0,
                expectedIndex = it.expectedIndex.takeIf { index -> index >= 0 } ?: 0,
                currentIndex = it.expectedIndex.takeIf { index -> index >= 0 } ?: 0
            )
        }

        micJob = viewModelScope.launch {
            try {
                TunerEngine().stream().collect { reading -> handleReading(reading) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        micActive = false,
                        micError = error.message ?: "Microfono non disponibile."
                    )
                }
            }
        }
    }

    private fun handleReading(reading: PitchReading) {
        val snapshot = _uiState.value
        if (!snapshot.micActive || snapshot.mode != PracticeMode.PLAY) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastReadingUiAt >= 150) {
            lastReadingUiAt = now
            _uiState.update { it.copy(reading = reading) }
        }

        val target = snapshot.notes.getOrNull(snapshot.expectedIndex) ?: return
        val deviation = MusicTheory.cents(
            frequency = reading.frequency,
            targetFrequency = MusicTheory.frequency(target.midi)
        )

        if (abs(deviation) <= 30.0) {
            stableFrames++
            if (stableFrames >= 2) {
                stableFrames = 0
                advancePractice()
            }
        } else {
            stableFrames = 0
            if (reading.midi != target.midi && now - lastWrongAt >= 800L) {
                lastWrongAt = now
                registerWrong()
            }
        }
    }

    private fun registerWrong() {
        val snapshot = _uiState.value
        val newErrors = snapshot.errors + 1
        val newWrong = snapshot.wrongIndices + snapshot.expectedIndex

        if (newErrors >= 3) {
            val newTempo = (snapshot.tempoPercent - 10).coerceAtLeast(50)
            _uiState.update {
                it.copy(
                    errors = 0,
                    wrongIndices = newWrong,
                    tempoPercent = newTempo,
                    autoSlowed = true,
                    feedback = "Rallentato al $newTempo%: respira e riprova."
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    errors = newErrors,
                    wrongIndices = newWrong,
                    feedback = "Non è la nota giusta. Riprova con calma."
                )
            }
        }
    }

    private fun advancePractice() {
        val snapshot = _uiState.value
        val next = snapshot.expectedIndex + 1
        if (next >= snapshot.notes.size) {
            finishPractice()
        } else {
            _uiState.update {
                it.copy(
                    expectedIndex = next,
                    currentIndex = next,
                    errors = 0,
                    feedback = "Bene! Continua."
                )
            }
        }
    }

    fun skipNote() {
        val snapshot = _uiState.value
        if (snapshot.mode != PracticeMode.PLAY || snapshot.notes.isEmpty()) return
        _uiState.update {
            it.copy(
                wrongIndices = it.wrongIndices + it.expectedIndex,
                feedback = "Nota saltata: la ritroverai nel conteggio finale."
            )
        }
        advancePractice()
    }

    fun playTargetNote() {
        val snapshot = _uiState.value
        val target = snapshot.notes.getOrNull(snapshot.expectedIndex) ?: return
        tonePlayer.playChord(listOf(target.midi), durationMs = 900, volume = 0.35f)
    }

    private fun finishPractice() {
        stopMic()
        val snapshot = _uiState.value
        val wrong = snapshot.wrongIndices.size.coerceAtMost(snapshot.notes.size)
        val accuracy = if (snapshot.notes.isEmpty()) {
            0f
        } else {
            (snapshot.notes.size - wrong).toFloat() / snapshot.notes.size
        }
        val baseXp = when (snapshot.level) {
            Level.BEGINNER -> 20
            Level.INTERMEDIATE -> 30
            Level.ADVANCED -> 40
        }
        val earnedXp = baseXp + (accuracy * 10).roundToInt()
        val durationSec = ((SystemClock.elapsedRealtime() - practiceStartedAt) / 1_000)
            .toInt()
            .coerceAtLeast(1)
        practiceStartedAt = 0L

        _uiState.update {
            it.copy(
                practiceComplete = true,
                micActive = false,
                isPlaying = false,
                expectedIndex = -1,
                currentIndex = -1,
                lastAccuracy = accuracy,
                lastXp = earnedXp,
                feedback = "Sezione completata!"
            )
        }

        viewModelScope.launch {
            progressRepository.log(
                kind = SessionKind.SCORE,
                refId = baseScore?.id.orEmpty(),
                title = baseScore?.title ?: "Spartito",
                skill = Skill.REPERTOIRE,
                accuracy = accuracy,
                xp = earnedXp,
                durationSec = durationSec
            )
        }
    }

    fun restartPractice() {
        stopAudio()
        _uiState.update {
            it.copy(
                practiceComplete = false,
                expectedIndex = 0,
                currentIndex = 0,
                wrongIndices = emptySet(),
                errors = 0,
                lastAccuracy = 0f,
                lastXp = 0,
                autoSlowed = false,
                feedback = "Si riparte dalla prima nota.",
                reading = null
            )
        }
        practiceStartedAt = 0L
        stableFrames = 0
    }

    fun stopMic() {
        micJob?.cancel()
        micJob = null
        _uiState.update { it.copy(micActive = false) }
    }

    private fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        tonePlayer.stop()
        _uiState.update {
            it.copy(
                isPlaying = false,
                currentIndex = if (it.mode == PracticeMode.LISTEN) -1 else it.expectedIndex
            )
        }
    }

    fun stopAudio() {
        stopMic()
        stopPlayback()
    }

    override fun onCleared() {
        stopAudio()
        tonePlayer.release()
        super.onCleared()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreDetailScreen(
    scoreId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val scoreViewModel: ScoreViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ScoreViewModel(application, scoreId) }
        }
    )
    val ui by scoreViewModel.uiState.collectAsStateWithLifecycle()
    val scoreScroll = rememberScrollState()
    val density = LocalDensity.current

    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionDenied = !granted
        if (granted) scoreViewModel.startMic()
    }

    val activateMic = {
        if (hasRecordingPermission(context)) {
            scoreViewModel.startMic()
        } else {
            permissionDenied = false
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        scoreViewModel.stopAudio()
    }

    LaunchedEffect(ui.currentIndex, ui.notes) {
        val current = ui.notes.getOrNull(ui.currentIndex) ?: return@LaunchedEffect
        val targetPx = with(density) {
            (ScoreLayout.START_PADDING_DP + current.startBeats * ScoreLayout.BEAT_WIDTH_DP).dp.toPx() -
                150.dp.toPx()
        }
        scoreScroll.animateScrollTo(targetPx.toInt().coerceAtLeast(0))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ArmoniaTopBar(title = "Spartito", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            if (!ui.scoreAvailable) {
                EmptyState(
                    title = "Spartito non trovato",
                    message = "Torna alla libreria e scegli un altro brano."
                )
            } else {
                Text(
                    text = ui.title,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = ui.composer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LevelPill(level = ui.level)
                    Text(
                        text = ui.license,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = ui.sourceNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = ui.mode == PracticeMode.LISTEN,
                        onClick = { scoreViewModel.setMode(PracticeMode.LISTEN) },
                        label = { Text(PracticeMode.LISTEN.label) }
                    )
                    FilterChip(
                        selected = ui.mode == PracticeMode.PLAY,
                        onClick = {
                            if (ui.mode != PracticeMode.PLAY) {
                                scoreViewModel.setMode(PracticeMode.PLAY)
                                activateMic()
                            }
                        },
                        label = { Text(PracticeMode.PLAY.label) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(
                            text = "Assetto",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Hand.entries.forEach { hand ->
                                FilterChip(
                                    selected = ui.hand == hand,
                                    onClick = { scoreViewModel.setHand(hand) },
                                    label = { Text(hand.label) }
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Semplifica a melodia",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "In pratica guidata la verifica segue sempre la voce principale.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = ui.melodyOnly || ui.mode == PracticeMode.PLAY,
                                onCheckedChange = scoreViewModel::setMelodyOnly,
                                enabled = ui.mode == PracticeMode.LISTEN
                            )
                        }

                        StepRow(
                            label = "Trasposizione",
                            value = when {
                                ui.semitones == 0 -> "0"
                                ui.semitones > 0 -> "+${ui.semitones}"
                                else -> "−${abs(ui.semitones)}"
                            },
                            onDecrease = { scoreViewModel.transposeBy(-1) },
                            onIncrease = { scoreViewModel.transposeBy(1) },
                            decreaseEnabled = ui.semitones > -6,
                            increaseEnabled = ui.semitones < 6
                        )

                        StepRow(
                            label = "Tempo",
                            value = "${ui.tempoPercent}%",
                            onDecrease = { scoreViewModel.changeTempo(-5) },
                            onIncrease = { scoreViewModel.changeTempo(5) },
                            decreaseEnabled = ui.tempoPercent > 50,
                            increaseEnabled = ui.tempoPercent < 120,
                            supporting = "Il rallentamento automatico scende fino al 50%."
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Loop",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (ui.loop) {
                                        "Ripete le battute ${ui.loopStartBar}–${ui.loopEndBar}."
                                    } else {
                                        "Suona il brano una volta."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = ui.loop,
                                onCheckedChange = scoreViewModel::setLoop
                            )
                        }

                        if (ui.loop) {
                            StepRow(
                                label = "Dalla battuta",
                                value = ui.loopStartBar.toString(),
                                onDecrease = { scoreViewModel.adjustLoopStart(-1) },
                                onIncrease = { scoreViewModel.adjustLoopStart(1) },
                                decreaseEnabled = ui.loopStartBar > 1,
                                increaseEnabled = ui.loopStartBar < ui.loopEndBar
                            )
                            StepRow(
                                label = "Alla battuta",
                                value = ui.loopEndBar.toString(),
                                onDecrease = { scoreViewModel.adjustLoopEnd(-1) },
                                onIncrease = { scoreViewModel.adjustLoopEnd(1) },
                                decreaseEnabled = ui.loopEndBar > ui.loopStartBar,
                                increaseEnabled = ui.loopEndBar < ui.originalBars
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(vertical = 10.dp)) {
                        if (ui.notes.isEmpty()) {
                            Box(modifier = Modifier.padding(16.dp)) {
                                EmptyState(
                                    title = "Nessuna nota",
                                    message = "Questa mano è vuota in questo brano. Prova a cambiare mano o disattivare la melodia."
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(scoreScroll)
                            ) {
                                ScoreCanvas(
                                    notes = ui.notes,
                                    beatsPerBar = ui.beatsPerBar,
                                    beatUnit = ui.beatUnit,
                                    barCount = ui.displayBars,
                                    currentIndex = ui.currentIndex,
                                    expectedIndex = ui.expectedIndex,
                                    wrongIndices = ui.wrongIndices
                                )
                            }
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LegendDot(
                                    color = MaterialTheme.colorScheme.primary,
                                    label = "Nota attesa"
                                )
                                LegendDot(
                                    color = MaterialTheme.colorScheme.error,
                                    label = "Errore"
                                )
                                LegendDot(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    label = "Diteggiatura sopra/sotto"
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (ui.mode == PracticeMode.LISTEN) {
                    Button(
                        onClick = scoreViewModel::togglePlayback,
                        enabled = ui.notes.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = if (ui.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(if (ui.isPlaying) "Pausa" else "Ascolta lo spartito")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Riproduzione sintetizzata · ${ui.tempoPercent}% del tempo originale · " +
                            if (ui.loop) "loop ${ui.loopStartBar}–${ui.loopEndBar}" else "una passata",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    PracticePanel(
                        ui = ui,
                        permissionDenied = permissionDenied,
                        onActivateMic = activateMic,
                        onStopMic = scoreViewModel::stopMic,
                        onPlayTarget = scoreViewModel::playTargetNote,
                        onSkip = scoreViewModel::skipNote,
                        onRestart = {
                            scoreViewModel.restartPractice()
                            activateMic()
                        },
                        onBack = onBack
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Suggerimento: parti dal 60–70% del tempo, poi alza. La fretta nasconde le insicurezze.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PracticePanel(
    ui: ScoreUiState,
    permissionDenied: Boolean,
    onActivateMic: () -> Unit,
    onStopMic: () -> Unit,
    onPlayTarget: () -> Unit,
    onSkip: () -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    val target = ui.notes.getOrNull(ui.expectedIndex)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = if (ui.micActive) "Microfono in ascolto" else "Microfono spento",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(
                            if (ui.micActive) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                )
            }

            if (ui.practiceComplete) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Sezione completata",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Precisione ${(ui.lastAccuracy * 100).roundToInt()}% · +${ui.lastXp} XP",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { ui.lastAccuracy },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onRestart,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Replay, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Ripeti")
                    }
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Fine")
                    }
                }
            } else if (ui.notes.isEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Non ci sono note per la mano selezionata. Scegli un'altra mano per iniziare.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (target != null) {
                                "Nota attesa ${ui.expectedIndex + 1}/${ui.notes.size}"
                            } else {
                                "Pronto"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = target?.let { MusicTheory.noteName(it.midi) } ?: "—",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onPlayTarget,
                        enabled = !ui.micActive
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VolumeUp,
                            contentDescription = "Ascolta la nota attesa"
                        )
                    }
                }

                val reading = ui.reading
                if (reading != null && ui.micActive) {
                    val readingColor = if (
                        target != null && reading.midi == target.midi &&
                        abs(reading.cents) <= 25.0
                    ) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                    Text(
                        text = "Stai suonando ${MusicTheory.noteName(reading.midi)} · " +
                            "${reading.cents.roundToInt()} cent",
                        style = MaterialTheme.typography.bodyMedium,
                        color = readingColor
                    )
                } else {
                    Text(
                        text = if (ui.micActive) "In ascolto… suona con calma." else "Attiva il microfono per iniziare.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ui.feedback?.let { feedback ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = feedback,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (ui.autoSlowed) {
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoPill(text = "Tempo ridotto al ${ui.tempoPercent}%")
                }

                if (permissionDenied) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Permesso microfono non concesso. Puoi riprovare o abilitarlo dalle impostazioni Android.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                ui.micError?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (ui.micActive) {
                    Button(
                        onClick = onStopMic,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Outlined.StopCircle, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Ferma il microfono")
                    }
                } else {
                    Button(
                        onClick = onActivateMic,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Outlined.Mic, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Attiva il microfono")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = onSkip, enabled = ui.micActive) {
                        Icon(Icons.Outlined.SkipNext, contentDescription = null)
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Salta nota")
                    }
                }

                Text(
                    text = "La verifica è monofonica: ascolta una nota alla volta. Accordi e pedale non vengono valutati.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StepRow(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseEnabled: Boolean = true,
    increaseEnabled: Boolean = true,
    supporting: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(
            onClick = onDecrease,
            enabled = decreaseEnabled
        ) {
            Icon(Icons.Filled.Remove, contentDescription = "Diminuisci $label")
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 44.dp)
        )
        IconButton(
            onClick = onIncrease,
            enabled = increaseEnabled
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Aumenta $label")
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
