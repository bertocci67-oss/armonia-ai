package com.armonia.app.ui.progress

import android.app.Application
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.armonia.app.ArmoniaApp
import com.armonia.app.data.PracticeSession
import com.armonia.app.domain.model.ProgressStats
import com.armonia.app.domain.model.SessionKind
import com.armonia.app.domain.model.Skill
import com.armonia.app.ui.components.ArmoniaTopBar
import com.armonia.app.ui.components.EmptyState
import com.armonia.app.ui.components.StatTile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class ProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val progressRepository = (application as ArmoniaApp).container.progressRepository

    val stats = progressRepository.stats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProgressStats()
    )

    val sessions = progressRepository.sessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
}

@Composable
fun ProgressScreen() {
    val progressViewModel: ProgressViewModel = viewModel()
    val stats by progressViewModel.stats.collectAsStateWithLifecycle()
    val sessions by progressViewModel.sessions.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        ArmoniaTopBar(title = "I tuoi progressi")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "La costanza conta più della perfezione. Ogni sessione lascia una traccia.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "giorni di fila",
                    value = stats.currentStreak.toString(),
                    icon = Icons.Outlined.LocalFireDepartment,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "punti XP",
                    value = stats.totalXp.toString(),
                    icon = Icons.Outlined.Bolt,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "minuti totali",
                    value = stats.totalMinutes.toString(),
                    icon = Icons.Outlined.Schedule,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Insights,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ultimi 7 giorni",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${stats.weekMinutes.sum()} minuti questa settimana",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    WeeklyChart(minutes = stats.weekMinutes)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Precisione media ${(stats.averageAccuracy * 100).roundToInt()}% su " +
                            "${stats.sessionsCount} sessioni",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Precisione per abilità",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (stats.skillAccuracy.isEmpty()) {
                EmptyState(
                    title = "Ancora nessun dato",
                    message = "Completa un esercizio, una lezione o una sessione di accordatura per vedere qui la tua precisione."
                )
            } else {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        stats.skillAccuracy.entries
                            .sortedBy { it.key.ordinal }
                            .forEach { (skill, accuracy) ->
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = skill.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${(accuracy * 100).roundToInt()}%",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { accuracy },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                    )
                                }
                            }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Sessioni recenti",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (sessions.isEmpty()) {
                EmptyState(
                    title = "Il tuo diario inizia qui",
                    message = "Accorda lo strumento, suona uno spartito o prova un micro-studio: ogni attività verrà registrata in locale."
                )
            } else {
                sessions.take(12).forEach { session ->
                    SessionRow(session)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "I dati sono salvati solo sul dispositivo. Nessuna sincronizzazione, nessun tracciamento.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WeeklyChart(minutes: List<Int>) {
    val max = (minutes.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(134.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        minutes.forEachIndexed { index, value ->
            val day = LocalDate.now().minusDays((6 - index).toLong())
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = if (value > 0) value.toString() else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height((8 + (value.toFloat() / max) * 78).dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (index == 6) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            }
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = day.dayOfWeek
                        .getDisplayName(TextStyle.NARROW, Locale.ITALIAN)
                        .uppercase(Locale.ITALIAN),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SessionRow(session: PracticeSession) {
    val kind = runCatching { SessionKind.valueOf(session.kind) }.getOrNull()
    val icon: ImageVector = when (kind) {
        SessionKind.TUNER -> Icons.Outlined.GraphicEq
        SessionKind.METRONOME -> Icons.Outlined.Timer
        SessionKind.SCORE -> Icons.Outlined.LibraryMusic
        SessionKind.LESSON -> Icons.Outlined.School
        SessionKind.EXERCISE -> Icons.Outlined.FitnessCenter
        null -> Icons.Outlined.Insights
    }
    val tint = when (kind) {
        SessionKind.TUNER -> MaterialTheme.colorScheme.primary
        SessionKind.METRONOME -> MaterialTheme.colorScheme.secondary
        SessionKind.SCORE -> MaterialTheme.colorScheme.tertiary
        SessionKind.LESSON -> MaterialTheme.colorScheme.primary
        SessionKind.EXERCISE -> MaterialTheme.colorScheme.secondary
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val date = SimpleDateFormat("d MMM", Locale.ITALIAN).format(Date(session.createdAt))

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${kind?.label ?: session.kind} · $date · " +
                        "${(session.durationSec / 60.0).roundToInt()} min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (kind != SessionKind.METRONOME) {
                    Text(
                        text = "${(session.accuracy * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = tint
                    )
                }
                Text(
                    text = "+${session.xp} XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
