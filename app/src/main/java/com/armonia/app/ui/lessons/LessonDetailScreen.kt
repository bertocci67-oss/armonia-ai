package com.armonia.app.ui.lessons

import android.app.Application
import android.os.SystemClock
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.armonia.app.ArmoniaApp
import com.armonia.app.domain.audio.TonePlayer
import com.armonia.app.domain.lessons.Lesson
import com.armonia.app.domain.lessons.LessonCatalog
import com.armonia.app.domain.lessons.LessonCategory
import com.armonia.app.domain.lessons.LessonPage
import com.armonia.app.domain.model.SessionKind
import com.armonia.app.domain.model.Skill
import com.armonia.app.ui.components.ArmoniaTopBar
import com.armonia.app.ui.components.EmptyState
import com.armonia.app.ui.components.InfoPill
import com.armonia.app.ui.components.LevelPill
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class LessonUiState(
    val pageIndex: Int = 0,
    val selectedOption: Int? = null,
    val answered: Boolean = false,
    val correctAnswers: Int = 0,
    val completed: Boolean = false,
    val alreadyCompleted: Boolean = false,
    val xpEarned: Int = 0,
    val isPlayingAudio: Boolean = false
)

class LessonViewModel(
    application: Application,
    lessonId: String
) : AndroidViewModel(application) {

    val lesson: Lesson? = LessonCatalog.byId(lessonId)
    private val progressRepository = (application as ArmoniaApp).container.progressRepository
    private val tonePlayer = TonePlayer()
    private val _state = MutableStateFlow(LessonUiState())
    val state = _state.asStateFlow()

    private val startedAt = SystemClock.elapsedRealtime()
    private var audioJob: Job? = null
    private var finishing = false

    fun selectOption(optionIndex: Int) {
        val snapshot = _state.value
        val currentLesson = lesson ?: return
        if (snapshot.answered || snapshot.completed) return
        val page = currentLesson.pages.getOrNull(snapshot.pageIndex) ?: return
        val correctIndex = when (page) {
            is LessonPage.Quiz -> page.correctIndex
            is LessonPage.Listening -> page.correctIndex
            is LessonPage.Theory -> return
        }
        val isCorrect = optionIndex == correctIndex
        _state.update {
            it.copy(
                selectedOption = optionIndex,
                answered = true,
                correctAnswers = it.correctAnswers + if (isCorrect) 1 else 0
            )
        }
    }

    fun next() {
        val currentLesson = lesson ?: return
        val snapshot = _state.value
        if (snapshot.completed) return

        val page = currentLesson.pages.getOrNull(snapshot.pageIndex) ?: return
        val needsAnswer = page is LessonPage.Quiz || page is LessonPage.Listening
        if (needsAnswer && !snapshot.answered) return

        if (snapshot.pageIndex == currentLesson.pages.lastIndex) {
            finish(currentLesson)
        } else {
            _state.update {
                it.copy(
                    pageIndex = it.pageIndex + 1,
                    selectedOption = null,
                    answered = false
                )
            }
        }
    }

    fun playCurrentAudio() {
        val currentLesson = lesson ?: return
        val page = currentLesson.pages.getOrNull(_state.value.pageIndex) as? LessonPage.Listening
            ?: return

        audioJob?.cancel()
        audioJob = viewModelScope.launch {
            _state.update { it.copy(isPlayingAudio = true) }
            if (page.together) {
                tonePlayer.playChord(page.notes, durationMs = 1_300, volume = 0.4f)
                delay(1_450)
            } else {
                tonePlayer.playSequence(page.notes, noteMs = 550, gapMs = 160)
            }
            _state.update { it.copy(isPlayingAudio = false) }
        }
    }

    private fun finish(currentLesson: Lesson) {
        if (finishing) return
        finishing = true

        val snapshot = _state.value
        val quizTotal = currentLesson.pages.count {
            it is LessonPage.Quiz || it is LessonPage.Listening
        }
        val accuracy = if (quizTotal == 0) {
            1f
        } else {
            snapshot.correctAnswers.toFloat() / quizTotal
        }

        _state.update { it.copy(completed = true) }

        viewModelScope.launch {
            val alreadyCompleted = progressRepository.wasCompleted(
                kind = SessionKind.LESSON,
                refId = currentLesson.id
            )
            if (!alreadyCompleted) {
                val durationSec = ((SystemClock.elapsedRealtime() - startedAt) / 1_000)
                    .toInt()
                    .coerceAtLeast(1)
                progressRepository.log(
                    kind = SessionKind.LESSON,
                    refId = currentLesson.id,
                    title = currentLesson.title,
                    skill = skillFor(currentLesson.category),
                    accuracy = accuracy,
                    xp = currentLesson.xp,
                    durationSec = durationSec
                )
            }
            _state.update {
                it.copy(
                    alreadyCompleted = alreadyCompleted,
                    xpEarned = if (alreadyCompleted) 0 else currentLesson.xp
                )
            }
        }
    }

    private fun skillFor(category: LessonCategory): Skill = when (category) {
        LessonCategory.NOTES -> Skill.NOTES
        LessonCategory.RHYTHM -> Skill.RHYTHM
        LessonCategory.CHORDS -> Skill.CHORDS
        LessonCategory.THEORY -> Skill.NOTES
        LessonCategory.EAR -> Skill.EAR
        LessonCategory.EXPRESSION -> Skill.EXPRESSION
    }

    override fun onCleared() {
        audioJob?.cancel()
        tonePlayer.release()
        super.onCleared()
    }
}

@Composable
fun LessonDetailScreen(
    lessonId: String,
    onBack: () -> Unit
) {
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as Application
    val lessonViewModel: LessonViewModel = viewModel(
        factory = viewModelFactory {
            initializer { LessonViewModel(application, lessonId) }
        }
    )
    val ui by lessonViewModel.state.collectAsStateWithLifecycle()
    val lesson = lessonViewModel.lesson

    Column(modifier = Modifier.fillMaxSize()) {
        ArmoniaTopBar(title = "Micro-lezione", onBack = onBack)

        if (lesson == null) {
            Box(modifier = Modifier.padding(20.dp)) {
                EmptyState(
                    title = "Lezione non trovata",
                    message = "Torna all'elenco e scegli un altro argomento."
                )
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            if (ui.completed) {
                CompletionContent(
                    lesson = lesson,
                    ui = ui,
                    onBack = onBack
                )
                return@Column
            }

            Text(
                text = lesson.title,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = lesson.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LevelPill(lesson.level)
                InfoPill(text = lesson.category.label)
                InfoPill(text = "${lesson.minutes} min")
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { (ui.pageIndex + 1f) / lesson.pages.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pagina ${ui.pageIndex + 1} di ${lesson.pages.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (val page = lesson.pages[ui.pageIndex]) {
                is LessonPage.Theory -> TheoryPage(page)
                is LessonPage.Quiz -> QuizPage(ui, page, lessonViewModel::selectOption)
                is LessonPage.Listening -> ListeningPage(
                    ui = ui,
                    page = page,
                    onPlay = lessonViewModel::playCurrentAudio,
                    onSelect = lessonViewModel::selectOption
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            val page = lesson.pages[ui.pageIndex]
            val needsAnswer = page is LessonPage.Quiz || page is LessonPage.Listening
            val canContinue = !needsAnswer || ui.answered
            val isLastPage = ui.pageIndex == lesson.pages.lastIndex

            Button(
                onClick = lessonViewModel::next,
                enabled = canContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    when {
                        isLastPage -> "Completa la lezione"
                        needsAnswer && !ui.answered -> "Rispondi per continuare"
                        else -> "Avanti"
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TheoryPage(page: LessonPage.Theory) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Icon(
                imageVector = Icons.Outlined.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = page.heading,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = page.body,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun QuizPage(
    ui: LessonUiState,
    page: LessonPage.Quiz,
    onSelect: (Int) -> Unit
) {
    Column {
        Text(
            text = page.question,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        OptionList(
            options = page.options,
            selectedOption = ui.selectedOption,
            answered = ui.answered,
            correctIndex = page.correctIndex,
            onSelect = onSelect
        )
        if (ui.answered) {
            Spacer(modifier = Modifier.height(12.dp))
            ExplanationCard(page.explanation)
        }
    }
}

@Composable
private fun ListeningPage(
    ui: LessonUiState,
    page: LessonPage.Listening,
    onPlay: () -> Unit,
    onSelect: (Int) -> Unit
) {
    Column {
        Text(
            text = page.question,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (ui.isPlayingAudio) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Icon(
                        imageVector = Icons.Outlined.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (page.together) "Accordo" else "Due note in sequenza",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Ascolta con calma, puoi riascoltare.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onPlay,
                    enabled = !ui.isPlayingAudio
                ) {
                    Text(if (ui.isPlayingAudio) "Ascolto…" else "Ascolta")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OptionList(
            options = page.options,
            selectedOption = ui.selectedOption,
            answered = ui.answered,
            correctIndex = page.correctIndex,
            onSelect = onSelect
        )

        if (ui.answered) {
            Spacer(modifier = Modifier.height(12.dp))
            ExplanationCard(page.explanation)
        }
    }
}

@Composable
private fun OptionList(
    options: List<String>,
    selectedOption: Int?,
    answered: Boolean,
    correctIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { index, option ->
            val isSelected = selectedOption == index
            val isCorrect = index == correctIndex
            val containerColor = when {
                answered && isCorrect -> MaterialTheme.colorScheme.tertiaryContainer
                answered && isSelected -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
            val contentColor = when {
                answered && isCorrect -> MaterialTheme.colorScheme.onTertiaryContainer
                answered && isSelected -> MaterialTheme.colorScheme.onErrorContainer
                else -> MaterialTheme.colorScheme.onSurface
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !answered) { onSelect(index) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ('A' + index).toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = contentColor
                        )
                    }
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor,
                        modifier = Modifier.weight(1f)
                    )
                    if (answered && isCorrect) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Corretta",
                            tint = contentColor
                        )
                    } else if (answered && isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Errata",
                            tint = contentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExplanationCard(text: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun CompletionContent(
    lesson: Lesson,
    ui: LessonUiState,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Lezione completata",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = lesson.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (ui.xpEarned > 0) "+${ui.xpEarned} XP" else "Ripasso completato",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (ui.alreadyCompleted) {
                        "Avevi già completato questa lezione: nessun nuovo XP."
                    } else {
                        "La sessione è stata aggiunta ai tuoi progressi."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Torna alle lezioni")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
