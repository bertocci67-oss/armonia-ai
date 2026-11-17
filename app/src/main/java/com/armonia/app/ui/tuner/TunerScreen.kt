package com.armonia.app.ui.tuner

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.armonia.app.ArmoniaApp
import com.armonia.app.domain.audio.PitchReading
import com.armonia.app.domain.audio.PitchDetector
import com.armonia.app.domain.audio.TonePlayer
import com.armonia.app.domain.audio.TunerEngine
import com.armonia.app.domain.model.Instrument
import com.armonia.app.domain.model.SessionKind
import com.armonia.app.domain.model.Skill
import com.armonia.app.domain.music.MusicTheory
import com.armonia.app.domain.music.Tunings
import com.armonia.app.ui.components.ArmoniaTopBar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class TunerUiState(
    val isListening: Boolean = false,
    val reading: PitchReading? = null,
    val inTuneRatio: Float = 0f,
    val lowPower: Boolean = false,
    val errorMessage: String? = null,
    val sessionSaved: Boolean = false,
    val hasReadings: Boolean = false
)

class TunerViewModel(application: Application) : AndroidViewModel(application) {

    private val progressRepository = (application as ArmoniaApp).container.progressRepository
    private val tonePlayer = TonePlayer()
    private val _state = MutableStateFlow(TunerUiState())
    val state = _state.asStateFlow()

    private var job: Job? = null
    private var startedAt = 0L
    private var readings = 0
    private var inTuneReadings = 0

    fun start() {
        if (job?.isActive == true) return

        _state.update {
            it.copy(
                isListening = true,
                reading = null,
                inTuneRatio = 0f,
                errorMessage = null,
                sessionSaved = false,
                hasReadings = false
            )
        }
        startedAt = System.currentTimeMillis()
        readings = 0
        inTuneReadings = 0

        job = viewModelScope.launch {
            try {
                TunerEngine(lowPower = _state.value.lowPower).stream().collect { reading ->
                    readings++
                    if (abs(reading.cents) <= 5.0) {
                        inTuneReadings++
                    }
                    _state.update {
                        it.copy(
                            reading = reading,
                            inTuneRatio = if (readings > 0) {
                                inTuneReadings.toFloat() / readings
                            } else {
                                0f
                            },
                            hasReadings = readings > 0
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        isListening = false,
                        errorMessage = error.message ?: "Impossibile leggere dal microfono."
                    )
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _state.update { it.copy(isListening = false) }
    }

    fun setLowPower(enabled: Boolean) {
        val wasListening = _state.value.isListening
        if (wasListening) stop()
        _state.update { it.copy(lowPower = enabled) }
        if (wasListening) start()
    }

    fun playReference(midi: Int) {
        tonePlayer.playChord(listOf(midi), durationMs = 1_200, volume = 0.35f)
    }

    fun saveSession() {
        val current = _state.value
        if (current.sessionSaved || readings == 0) return

        val durationSec = ((System.currentTimeMillis() - startedAt) / 1_000).toInt().coerceAtLeast(1)
        val accuracy = if (readings > 0) inTuneReadings.toFloat() / readings else 0f

        viewModelScope.launch {
            progressRepository.log(
                kind = SessionKind.TUNER,
                refId = "tuner",
                title = "Sessione di accordatura",
                skill = Skill.TUNING,
                accuracy = accuracy,
                xp = 15,
                durationSec = durationSec
            )
            _state.update { it.copy(sessionSaved = true) }
        }
    }

    override fun onCleared() {
        job?.cancel()
        tonePlayer.release()
        super.onCleared()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunerScreen(
    instrument: Instrument,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val tunerViewModel: TunerViewModel = viewModel()
    val state by tunerViewModel.state.collectAsStateWithLifecycle()

    var permissionGranted by remember { mutableStateOf(hasRecordingPermission(context)) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        permissionDenied = !granted
        if (granted) tunerViewModel.start()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        tunerViewModel.stop()
    }

    val presets = remember(instrument) { Tunings.presetsFor(instrument) }
    var presetIndex by remember(instrument) { mutableIntStateOf(0) }
    val selectedPreset = presets.getOrNull(presetIndex)

    val tuneColor = when {
        !state.isListening -> MaterialTheme.colorScheme.outline
        state.reading == null -> MaterialTheme.colorScheme.onSurfaceVariant
        abs(state.reading?.cents ?: 0.0) <= 5.0 -> MaterialTheme.colorScheme.tertiary
        abs(state.reading?.cents ?: 0.0) <= 15.0 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ArmoniaTopBar(title = "Accordatore", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Suona una nota e guarda il cent. Il microfono ascolta, il dispositivo elabora: nulla esce da qui.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                    Text(
                        text = state.reading?.let { MusicTheory.noteName(it.midi) } ?: "—",
                        fontSize = 46.sp,
                        color = tuneColor,
                        style = MaterialTheme.typography.displayMedium
                    )
                    Text(
                        text = state.reading?.let {
                            String.format(Locale.US, "%.1f Hz", it.frequency)
                        } ?: "In attesa di una nota",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val status = when {
                        !state.isListening && state.reading == null ->
                            "Premi Accordatore e suona una nota"
                        state.reading == null -> "In ascolto…"
                        abs(state.reading?.cents ?: 0.0) <= 5.0 -> "Intonato"
                        (state.reading?.cents ?: 0.0) > 0 -> "Troppo alto"
                        else -> "Troppo basso"
                    }
                    Text(
                        text = status,
                        style = MaterialTheme.typography.titleMedium,
                        color = tuneColor
                    )
                    val cents = state.reading?.cents
                    if (cents != null) {
                        Text(
                            text = String.format(
                                Locale.US,
                                "%s%d cent",
                                if (cents > 0) "+" else "−",
                                abs(cents).roundToInt()
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                    ) {
                        val left = 8.dp.toPx()
                        val right = size.width - 8.dp.toPx()
                        val baseline = size.height - 18.dp.toPx()
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.25f),
                            start = Offset(left, baseline),
                            end = Offset(right, baseline),
                            strokeWidth = 2.dp.toPx()
                        )

                        for (value in -50..50 step 5) {
                            val x = left + ((value + 50) / 100f) * (right - left)
                            val length = if (value % 25 == 0) 12.dp.toPx() else 6.dp.toPx()
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.4f),
                                start = Offset(x, baseline),
                                end = Offset(x, baseline - length),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }

                        val centerX = (left + right) / 2f
                        val currentCents = (state.reading?.cents ?: 0.0).coerceIn(-50.0, 50.0)
                        val needleX = centerX + (currentCents / 50.0).toFloat() * (right - left) / 2f
                        drawLine(
                            color = tuneColor,
                            start = Offset(needleX, baseline),
                            end = Offset(needleX, baseline - 30.dp.toPx()),
                            strokeWidth = 3.dp.toPx()
                        )
                        drawCircle(
                            color = tuneColor,
                            radius = 6.dp.toPx(),
                            center = Offset(needleX, baseline)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("−50", style = MaterialTheme.typography.labelSmall)
                        Text("0", style = MaterialTheme.typography.labelSmall)
                        Text("+50", style = MaterialTheme.typography.labelSmall)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (state.isListening) {
                                tunerViewModel.stop()
                            } else if (hasRecordingPermission(context)) {
                                permissionGranted = true
                                tunerViewModel.start()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isListening) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            contentColor = if (state.isListening) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            }
                        )
                    ) {
                        Icon(
                            imageVector = if (state.isListening) {
                                Icons.Outlined.StopCircle
                            } else {
                                Icons.Outlined.Mic
                            },
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(if (state.isListening) "Ferma l'ascolto" else "Accorda")
                    }

                    if (!permissionGranted) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (permissionDenied) {
                                "Permesso microfono negato. Puoi abilitarlo dalle impostazioni."
                            } else {
                                "Serve il permesso microfono per ascoltare l'accordatura."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (permissionDenied) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("Apri impostazioni")
                            }
                        }
                    }

                    val error = state.errorMessage
                    if (error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (presets.isNotEmpty()) {
                Text(
                    text = "Riferimenti",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Tocca una corda o una nota per sentirne l'altezza.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (presets.size > 1) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEachIndexed { index, preset ->
                            FilterChip(
                                selected = index == presetIndex,
                                onClick = { presetIndex = index },
                                label = { Text(preset.name) }
                            )
                        }
                    }
                }

                if (selectedPreset != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        selectedPreset.strings.forEach { string ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { tunerViewModel.playReference(string.midi) },
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.VolumeUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("${string.label} · ${MusicTheory.noteName(string.midi)}")
                                }
                            }
                        }
                    }
                }
            } else if (instrument == Instrument.DRUMS) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        text = "La batteria non ha un'accordatura standard. Puoi comunque usare l'accordatore cromatico per i tamburi; per il tempo, il metronomo è il tuo migliore amico.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Risparmio energetico",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Analisi più leggera: meno frame al secondo, elaborazione a 22 kHz.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.lowPower,
                        onCheckedChange = tunerViewModel::setLowPower
                    )
                }
            }

            if (state.hasReadings) {
                Spacer(modifier = Modifier.height(18.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Precisione della sessione",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { state.inTuneRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${(state.inTuneRatio * 100).roundToInt()}% delle letture entro ±5 cent",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (state.sessionSaved) {
                            Text(
                                text = "Sessione salvata nei progressi ✓",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        } else {
                            OutlinedButton(
                                onClick = tunerViewModel::saveSession,
                                enabled = !state.isListening
                            ) {
                                Text("Salva la sessione")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Privacy: l'audio viene analizzato in tempo reale in memoria. Nessuna registrazione viene salvata o caricata.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun hasRecordingPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
