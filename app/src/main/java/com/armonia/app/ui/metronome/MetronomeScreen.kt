package com.armonia.app.ui.metronome

import android.app.Application
import android.os.SystemClock
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.armonia.app.ArmoniaApp
import com.armonia.app.domain.audio.MetronomeEngine
import com.armonia.app.domain.model.SessionKind
import com.armonia.app.domain.model.Skill
import com.armonia.app.ui.components.ArmoniaTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class MetronomeSubdivision(val label: String, val perBeat: Int) {
    NONE("Nessuna", 1),
    EIGHTHS("Ottavi", 2),
    TRIPLETS("Terzine", 3)
}

data class MetronomeUiState(
    val bpm: Int = 80,
    val beatsPerBar: Int = 4,
    val subdivision: MetronomeSubdivision = MetronomeSubdivision.NONE,
    val accentFirst: Boolean = true,
    val isPlaying: Boolean = false,
    val currentBeat: Int = 0,
    val isReady: Boolean = false
)

class MetronomeViewModel(application: Application) : AndroidViewModel(application) {

    private val progressRepository = (application as ArmoniaApp).container.progressRepository
    private val engine = MetronomeEngine(application)
    private val _state = MutableStateFlow(MetronomeUiState())
    val state = _state.asStateFlow()

    private var job: Job? = null
    private var playbackStartedAt = 0L
    private val taps = ArrayDeque<Long>()

    init {
        engine.prepare {
            _state.update { it.copy(isReady = true) }
        }
    }

    fun toggle() {
        if (_state.value.isPlaying) {
            stop(logSession = true)
        } else {
            start()
        }
    }

    fun setBpm(value: Int) {
        val clamped = value.coerceIn(40, 240)
        if (clamped == _state.value.bpm) return
        val restart = _state.value.isPlaying
        if (restart) stop(logSession = false)
        _state.update { it.copy(bpm = clamped) }
        if (restart) start()
    }

    fun setBeatsPerBar(beats: Int) {
        if (beats == _state.value.beatsPerBar) return
        val restart = _state.value.isPlaying
        if (restart) stop(logSession = false)
        _state.update { it.copy(beatsPerBar = beats, currentBeat = 0) }
        if (restart) start()
    }

    fun setSubdivision(subdivision: MetronomeSubdivision) {
        if (subdivision == _state.value.subdivision) return
        val restart = _state.value.isPlaying
        if (restart) stop(logSession = false)
        _state.update { it.copy(subdivision = subdivision) }
        if (restart) start()
    }

    fun setAccentFirst(enabled: Boolean) {
        if (enabled == _state.value.accentFirst) return
        val restart = _state.value.isPlaying
        if (restart) stop(logSession = false)
        _state.update { it.copy(accentFirst = enabled) }
        if (restart) start()
    }

    fun tapTempo() {
        val now = SystemClock.elapsedRealtime()
        if (taps.isNotEmpty() && now - taps.last() > 2_500L) {
            taps.clear()
        }
        taps.addLast(now)
        while (taps.size > 5) {
            taps.removeFirst()
        }
        if (taps.size >= 2) {
            val averageInterval = taps.toList().zipWithNext { first, second -> second - first }
                .average()
            if (averageInterval > 0.0) {
                setBpm((60_000.0 / averageInterval).roundToInt())
            }
        }
    }

    private fun start() {
        if (!_state.value.isReady || job?.isActive == true) return

        val bpm = _state.value.bpm
        val beatsPerBar = _state.value.beatsPerBar
        val subdivision = _state.value.subdivision
        val accentFirst = _state.value.accentFirst
        playbackStartedAt = SystemClock.elapsedRealtime()

        job = viewModelScope.launch(Dispatchers.Default) {
            _state.update { it.copy(isPlaying = true, currentBeat = 0) }
            val tickNanos = 60_000_000_000L / bpm / subdivision.perBeat
            var tick = 0
            var nextTick = System.nanoTime()

            while (isActive) {
                val mainBeat = tick % subdivision.perBeat == 0
                val beat = (tick / subdivision.perBeat) % beatsPerBar

                if (mainBeat) {
                    engine.tick(accent = beat == 0 && accentFirst, volume = 1f)
                    _state.update { it.copy(currentBeat = beat) }
                } else {
                    engine.tick(accent = false, volume = 0.45f)
                }

                tick++
                nextTick += tickNanos
                val waitMs = (nextTick - System.nanoTime()) / 1_000_000L
                if (waitMs > 0) {
                    delay(waitMs)
                } else {
                    delay(1)
                }
            }
        }
    }

    private fun stop(logSession: Boolean) {
        if (job == null && !_state.value.isPlaying) return

        val sessionBpm = _state.value.bpm
        job?.cancel()
        job = null
        _state.update { it.copy(isPlaying = false, currentBeat = 0) }

        if (logSession) {
            val durationSec = ((SystemClock.elapsedRealtime() - playbackStartedAt) / 1_000).toInt()
            if (durationSec >= 15) {
                viewModelScope.launch {
                    progressRepository.log(
                        kind = SessionKind.METRONOME,
                        refId = "metronome",
                        title = "Metronomo · $sessionBpm BPM",
                        skill = Skill.RHYTHM,
                        accuracy = 1f,
                        xp = 10,
                        durationSec = durationSec
                    )
                }
            }
        }
    }

    override fun onCleared() {
        job?.cancel()
        engine.release()
        super.onCleared()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetronomeScreen(onBack: () -> Unit) {
    val metronomeViewModel: MetronomeViewModel = viewModel()
    val state by metronomeViewModel.state.collectAsStateWithLifecycle()
    var sliderValue by remember(state.bpm) { mutableFloatStateOf(state.bpm.toFloat()) }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        metronomeViewModel.toggleIfPlaying()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ArmoniaTopBar(title = "Metronomo", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Il tempo è il tuo spazio. Scegli la pulsazione, ascolta e respira.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(onClick = { metronomeViewModel.setBpm(state.bpm - 1) }) {
                            Icon(Icons.Filled.Remove, contentDescription = "Riduci BPM")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.bpm.toString(),
                                fontSize = 64.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "BATTITI AL MINUTO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { metronomeViewModel.setBpm(state.bpm + 1) }) {
                            Icon(Icons.Filled.Add, contentDescription = "Aumenta BPM")
                        }
                    }

                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = {
                            metronomeViewModel.setBpm(sliderValue.roundToInt())
                        },
                        valueRange = 40f..240f
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(state.beatsPerBar) { beat ->
                            val active = state.isPlaying && state.currentBeat == beat
                            Box(
                                modifier = Modifier
                                    .size(if (beat == 0) 26.dp else 22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            !state.isPlaying -> MaterialTheme.colorScheme.outlineVariant
                                            active && beat == 0 -> MaterialTheme.colorScheme.secondary
                                            active -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        }
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = metronomeViewModel::toggle,
                        enabled = state.isReady,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(if (state.isPlaying) "Ferma" else "Avvia il metronomo")
                    }

                    if (!state.isReady) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Preparazione dei click…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = metronomeViewModel::tapTempo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("TAP · imposta il tempo")
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Tempo",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2, 3, 4, 6).forEach { beats ->
                    FilterChip(
                        selected = state.beatsPerBar == beats,
                        onClick = { metronomeViewModel.setBeatsPerBar(beats) },
                        label = { Text(if (beats == 6) "6/8" else "$beats/4") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetronomeSubdivision.entries.forEach { subdivision ->
                    FilterChip(
                        selected = state.subdivision == subdivision,
                        onClick = { metronomeViewModel.setSubdivision(subdivision) },
                        label = { Text(subdivision.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Accento sul primo battito",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Utile per sentire l'inizio della battuta.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.accentFirst,
                        onCheckedChange = metronomeViewModel::setAccentFirst
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Le sessioni di almeno 15 secondi vengono aggiunte allo storico dei progressi. " +
                    "Se cambi tempo o suddivisione mentre suoni, il metronomo riparte dal primo battito.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun MetronomeViewModel.toggleIfPlaying() {
    val current = state.value
    if (current.isPlaying) {
        toggle()
    }
}
