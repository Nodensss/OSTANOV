package ru.upestop.app.ui.trainer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.upestop.app.AppContainer
import ru.upestop.app.R
import ru.upestop.app.data.model.StopAlgorithm
import ru.upestop.app.data.model.TrainerQuestion
import ru.upestop.app.domain.QuestionSetKey
import ru.upestop.app.domain.QuestionSetOption
import ru.upestop.app.domain.TrainerAnswer
import ru.upestop.app.domain.TrainerAttempt
import ru.upestop.app.domain.TrainerEngine
import ru.upestop.app.domain.phase
import ru.upestop.app.ui.components.LoadingScreen
import ru.upestop.app.ui.components.OutlineAction
import ru.upestop.app.ui.components.Panel
import ru.upestop.app.ui.components.ProgressLine
import ru.upestop.app.ui.components.SectionLabel
import ru.upestop.app.ui.components.UpeTopBar
import ru.upestop.app.ui.components.appViewModel
import ru.upestop.app.ui.theme.UpeColors
import ru.upestop.app.ui.theme.UpeText
import java.util.UUID

/** Идущая попытка: вопросы в перемешанном порядке и ответы. */
data class TrainerSession(val attempt: TrainerAttempt, val questions: List<TrainerQuestion>, val index: Int) {
    val current: TrainerQuestion? get() = questions.getOrNull(index)
    val currentAnswer: TrainerAnswer? get() = current?.let { q -> attempt.answers.firstOrNull { it.questionId == q.id } }
}

data class TrainerUi(val algorithm: StopAlgorithm, val options: List<QuestionSetOption>, val session: TrainerSession?)

class TrainerViewModel(private val container: AppContainer) : ViewModel() {
    private val session = MutableStateFlow<TrainerSession?>(null)

    val ui: StateFlow<TrainerUi?> = combine(container.algorithmRepository.algorithm, session) { algorithm, session ->
        TrainerUi(algorithm, TrainerEngine.options(algorithm), session)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Попытка сохраняется в БД сразу при старте: незавершённая попадёт в историю как «Не завершена». */
    fun start(key: QuestionSetKey, label: String, retryIds: List<String> = emptyList()) {
        val algorithm = ui.value?.algorithm ?: return
        val questions = TrainerEngine.questions(algorithm, key, retryIds).shuffled()
        if (questions.isEmpty()) return
        val attempt = TrainerAttempt(
            id = UUID.randomUUID().toString(),
            startedAt = System.currentTimeMillis(),
            finishedAt = null,
            setKey = key.raw,
            setLabel = label,
            total = questions.size,
            score = null,
            answers = emptyList(),
        )
        session.value = TrainerSession(attempt, questions, 0)
        save(attempt)
    }

    fun answer(selectedIndex: Int) {
        val current = session.value ?: return
        val question = current.current ?: return
        if (current.currentAnswer != null) return
        val attempt = current.attempt.copy(answers = current.attempt.answers + TrainerEngine.answer(question, selectedIndex))
        session.value = current.copy(attempt = attempt)
        save(attempt)
    }

    fun next() {
        val current = session.value ?: return
        if (current.currentAnswer == null) return
        if (current.index < current.questions.lastIndex) {
            session.update { it?.copy(index = current.index + 1) }
            return
        }
        val answers = current.attempt.answers
        val finished = current.attempt.copy(finishedAt = System.currentTimeMillis(), score = TrainerEngine.score(answers))
        session.value = current.copy(attempt = finished)
        save(finished)
    }

    fun restart() {
        session.value = null
    }

    private fun save(attempt: TrainerAttempt) {
        viewModelScope.launch { container.trainerRepository.save(attempt) }
    }
}

@Composable
fun TrainerScreen(
    onOpenHistory: () -> Unit,
    viewModel: TrainerViewModel = appViewModel { container, _ -> TrainerViewModel(container) },
) {
    val uiState by viewModel.ui.collectAsStateWithLifecycle()
    val ui = uiState ?: return LoadingScreen()
    val session = ui.session
    var selected by rememberSaveable { mutableStateOf(QuestionSetKey.All.raw) }
    val labels = ui.options.associate { it.key.raw to questionSetLabel(it) }

    Scaffold(
        containerColor = UpeColors.Bg,
        topBar = {
            UpeTopBar(title = stringResource(R.string.trainer_title)) {
                IconButton(onClick = onOpenHistory) {
                    Icon(Icons.Filled.History, contentDescription = stringResource(R.string.trainer_history))
                }
            }
        },
        bottomBar = {
            // Кнопка старта закреплена внизу: список наборов длинный и не должен её прятать.
            if (session == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(UpeColors.Surface)
                        .padding(12.dp),
                ) {
                    Button(
                        onClick = { QuestionSetKey.parse(selected)?.let { viewModel.start(it, labels[selected].orEmpty()) } },
                        enabled = selected in labels,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = UpeColors.Cool, contentColor = UpeColors.Bg),
                    ) { Text(stringResource(R.string.trainer_start), fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                session == null -> SetupContent(ui.options, labels, selected, onSelect = { selected = it }, onOpenHistory)
                session.attempt.finished -> ResultContent(ui.algorithm, session, viewModel, onOpenHistory)
                else -> QuestionContent(ui.algorithm, session, viewModel)
            }
        }
    }
}

@Composable
fun questionSetLabel(option: QuestionSetOption): String = when (val key = option.key) {
    QuestionSetKey.All -> stringResource(R.string.trainer_set_all, option.count)
    is QuestionSetKey.ForPhase -> stringResource(R.string.trainer_set_phase, key.phaseId, option.phase?.title.orEmpty(), option.count)
    QuestionSetKey.Rules -> stringResource(R.string.trainer_set_rules, option.count)
    QuestionSetKey.Retry -> stringResource(R.string.trainer_set_retry, option.count)
}

@Composable
private fun SetupContent(
    options: List<QuestionSetOption>,
    labels: Map<String, String>,
    selected: String,
    onSelect: (String) -> Unit,
    onOpenHistory: () -> Unit,
) {
    OutlineAction(stringResource(R.string.trainer_history), onClick = onOpenHistory, modifier = Modifier.fillMaxWidth())
    Panel {
        SectionLabel(stringResource(R.string.trainer_set_label))
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .selectable(selected = selected == option.key.raw, role = Role.RadioButton) { onSelect(option.key.raw) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selected == option.key.raw,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = UpeColors.Cool, unselectedColor = UpeColors.TextMute),
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(labels.getValue(option.key.raw), color = UpeColors.Text)
            }
        }
    }
}

@Composable
private fun QuestionContent(algorithm: StopAlgorithm, session: TrainerSession, viewModel: TrainerViewModel) {
    val question = session.current ?: return
    val answer = session.currentAnswer
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.trainer_question_counter, session.index + 1, session.questions.size),
            style = UpeText.mono,
            color = UpeColors.TextDim,
        )
        ProgressLine(session.attempt.answers.size.toFloat() / session.questions.size)
    }
    SectionLabel(questionTag(algorithm, question), color = UpeColors.Cool)
    Text(question.question, style = MaterialTheme.typography.titleLarge, color = UpeColors.Text)

    question.options.forEachIndexed { index, option ->
        val isCorrect = index == question.correct
        val isSelected = answer?.selectedIndex == index
        val (border, background, text) = when {
            answer == null -> Triple(UpeColors.Border, UpeColors.Surface, UpeColors.Text)
            isCorrect -> Triple(UpeColors.Safe, UpeColors.Safe.copy(alpha = 0.12f), UpeColors.Text)
            isSelected -> Triple(UpeColors.Danger, UpeColors.Danger.copy(alpha = 0.12f), UpeColors.Text)
            else -> Triple(UpeColors.Border, UpeColors.Bg, UpeColors.TextMute)
        }
        Text(
            text = option,
            color = text,
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .background(background, RectangleShape)
                .border(1.dp, border, RectangleShape)
                .clickable(enabled = answer == null, role = Role.Button) { viewModel.answer(index) }
                .padding(16.dp),
        )
    }

    if (answer != null) {
        Panel(border = if (answer.correct) UpeColors.Safe else UpeColors.Danger) {
            Text(
                stringResource(if (answer.correct) R.string.trainer_correct else R.string.trainer_wrong),
                color = if (answer.correct) UpeColors.Safe else UpeColors.Danger,
                fontWeight = FontWeight.SemiBold,
            )
            if (question.explanation.isNotBlank()) Text(question.explanation, color = UpeColors.TextDim)
        }
        Button(
            onClick = viewModel::next,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(2.dp),
            colors = ButtonDefaults.buttonColors(containerColor = UpeColors.Cool, contentColor = UpeColors.Bg),
        ) {
            val last = session.index == session.questions.lastIndex
            Text(stringResource(if (last) R.string.trainer_finish else R.string.trainer_next), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun ResultContent(algorithm: StopAlgorithm, session: TrainerSession, viewModel: TrainerViewModel, onOpenHistory: () -> Unit) {
    val answers = session.attempt.answers
    val score = TrainerEngine.score(answers)
    val total = session.questions.size
    val wrongIds = TrainerEngine.wrongQuestionIds(answers)
    val retryLabel = stringResource(R.string.trainer_set_retry, wrongIds.size)

    Panel {
        SectionLabel(stringResource(R.string.trainer_result_overline))
        Text(
            stringResource(R.string.trainer_result, score, total, TrainerEngine.percent(score, total)),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (wrongIds.isEmpty()) UpeColors.Safe else UpeColors.Text,
        )
    }
    if (wrongIds.isNotEmpty()) {
        Button(
            onClick = { viewModel.start(QuestionSetKey.Retry, retryLabel, wrongIds) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(2.dp),
            colors = ButtonDefaults.buttonColors(containerColor = UpeColors.Cool, contentColor = UpeColors.Bg),
        ) { Text(stringResource(R.string.trainer_retry_mistakes), fontWeight = FontWeight.SemiBold) }
    }
    OutlineAction(stringResource(R.string.trainer_restart), onClick = viewModel::restart, modifier = Modifier.fillMaxWidth())
    OutlineAction(stringResource(R.string.trainer_history), onClick = onOpenHistory, modifier = Modifier.fillMaxWidth())

    SectionLabel(stringResource(R.string.trainer_mistakes))
    if (wrongIds.isEmpty()) {
        Text(stringResource(R.string.trainer_no_mistakes), color = UpeColors.Safe)
    }
    session.questions.filter { it.id in wrongIds }.forEach { question ->
        AnswerReview(algorithm, question, answers.firstOrNull { it.questionId == question.id })
    }
}

/** Разбор ответа: вопрос, выбранный и правильный ответы, пояснение. Используется и в истории. */
@Composable
fun AnswerReview(algorithm: StopAlgorithm, question: TrainerQuestion, answer: TrainerAnswer?) {
    val correct = answer?.correct == true
    Panel(border = if (correct) UpeColors.Safe.copy(alpha = 0.5f) else UpeColors.Danger.copy(alpha = 0.5f), spacing = 6) {
        SectionLabel(questionTag(algorithm, question))
        Text(question.question, color = UpeColors.Text, fontWeight = FontWeight.Medium)
        val selected = answer?.let { question.options.getOrNull(it.selectedIndex) }
        AnswerLine(
            text = selected?.let { stringResource(R.string.trainer_your_answer, it) } ?: stringResource(R.string.attempt_not_answered),
            color = if (correct) UpeColors.Safe else UpeColors.Danger,
        )
        if (!correct) {
            AnswerLine(stringResource(R.string.trainer_correct_answer, question.options.getOrNull(question.correct).orEmpty()), UpeColors.Safe)
        }
        if (question.explanation.isNotBlank()) Text(question.explanation, color = UpeColors.TextDim, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun AnswerLine(text: String, color: Color) {
    Text(text, color = color, fontSize = 14.sp, lineHeight = 20.sp)
}

@Composable
fun questionTag(algorithm: StopAlgorithm, question: TrainerQuestion): String {
    val phaseId = question.phaseId ?: return stringResource(R.string.trainer_rules_tag)
    return stringResource(R.string.trainer_phase_tag, phaseId, algorithm.phase(phaseId)?.title.orEmpty())
}
