package com.armonia.app.ui.lessons

import android.app.Application
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.armonia.app.ArmoniaApp
import com.armonia.app.domain.lessons.Lesson
import com.armonia.app.domain.lessons.LessonCatalog
import com.armonia.app.domain.lessons.LessonCategory
import com.armonia.app.domain.model.Profile
import com.armonia.app.domain.model.SessionKind
import com.armonia.app.ui.components.ArmoniaTopBar
import com.armonia.app.ui.components.LevelPill
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LessonsViewModel(application: Application) : AndroidViewModel(application) {

    private val progressRepository = (application as ArmoniaApp).container.progressRepository

    val completedIds = progressRepository.sessions
        .map { sessions ->
            sessions
                .filter { it.kind == SessionKind.LESSON.name }
                .map { it.refId }
                .toSet()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet()
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonsScreen(
    profile: Profile,
    onOpenLesson: (String) -> Unit
) {
    val lessonsViewModel: LessonsViewModel = viewModel()
    val completedIds by lessonsViewModel.completedIds.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf<LessonCategory?>(null) }

    val lessons = remember(profile.level, filter) {
        LessonCatalog.suggestedFor(profile.level).filter { lesson ->
            filter == null || lesson.category == filter
        }
    }
    val completedCount = LessonCatalog.lessons.count { it.id in completedIds }

    Column(modifier = Modifier.fillMaxSize()) {
        ArmoniaTopBar(title = "Micro-lezioni")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cinque minuti, una scoperta",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Teoria in pillole, quiz e ascolti. Ogni risposta riceve un feedback immediato: " +
                                "sbagliare fa parte dell'orecchio che cresce.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "$completedCount di ${LessonCatalog.lessons.size} lezioni completate",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = {
                                completedCount.toFloat() / LessonCatalog.lessons.size
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == null,
                    onClick = { filter = null },
                    label = { Text("Tutte") }
                )
                LessonCategory.entries.forEach { category ->
                    FilterChip(
                        selected = filter == category,
                        onClick = { filter = category },
                        label = { Text(category.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            lessons.forEach { lesson ->
                LessonCard(
                    lesson = lesson,
                    completed = lesson.id in completedIds,
                    onClick = { onOpenLesson(lesson.id) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Le lezioni sono contenuti originali. Il livello consigliato segue il tuo profilo, " +
                    "ma puoi aprire qualsiasi argomento.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LessonCard(
    lesson: Lesson,
    completed: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (completed) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (completed) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Completata",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.size(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = lesson.category.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LevelPill(level = lesson.level)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = lesson.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${lesson.minutes} min · ${lesson.xp} XP" +
                    if (completed) " · ripassa quando vuoi" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
