package ru.upestop.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.upestop.app.R
import ru.upestop.app.data.db.AttemptEntity
import ru.upestop.app.data.model.*
import ru.upestop.app.domain.*
import ru.upestop.app.ui.components.*
import ru.upestop.app.ui.theme.*

@Composable
private fun setLabel(set: QuestionSet): String = when (set.key) {
    "all" -> stringResource(R.string.all_questions, set.questions.size)
    "rules" -> stringResource(R.string.rules_set, set.questions.size)
    "retry" -> stringResource(R.string.retry_errors)
    else -> stringResource(R.string.phase_set, set.phase!!.id, set.phase.title, set.questions.size)
}
@Composable
fun TrainerScreen(data: StopAlgorithm, stored: StoredState, vm: AppViewModel, busy: Boolean, navigate: (String) -> Unit) {
    var setKey by rememberSaveable { mutableStateOf("all") }
    var attemptId by rememberSaveable { mutableStateOf("") }
    var questionIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    val sets = remember(data) { TrainerEngine.sets(data) }
    val attempt = stored.attempts.firstOrNull { it.id == attemptId }
    val labels = sets.associate { it.key to setLabel(it) }
    val selected = sets.firstOrNull { it.key == setKey } ?: sets.firstOrNull()
    val answers = attempt?.let { vm.app.trainer.answers(it) }.orEmpty()
    val questions = questionIds.mapNotNull { id -> data.trainerQuestions.find { it.id == id } }
    val retryLabel = stringResource(R.string.retry_errors)
    fun start(set: QuestionSet, label: String) = vm.startAttempt(set, label) { id, ids -> attemptId = id; questionIds = ids; index = 0 }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (attemptId.isEmpty() || attempt == null) {
            items(sets, key = { it.key }) { set ->
                OutlinedButton(shape = MaterialTheme.shapes.small, onClick = { setKey = set.key }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), border = BorderStroke(1.dp, if (set.key == selected?.key) Cool else Border)) {
                    Text(labels.getValue(set.key))
                }
            }
            item { Button(shape = MaterialTheme.shapes.small, onClick = { selected?.let { start(it, labels.getValue(it.key)) } }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = !busy && selected != null) { Text(stringResource(R.string.start)) } }
            item { Action(stringResource(R.string.history)) { navigate("history") } }
        } else if (attempt.finishedAt != null) {
            val score = TrainerEngine.score(answers)
            val errors = TrainerEngine.errors(questions, answers)
            item { Text(stringResource(R.string.score, score, attempt.total, TrainerEngine.percent(score, attempt.total)), style = MaterialTheme.typography.headlineSmall) }
            if (errors.isNotEmpty()) item { Label(stringResource(R.string.errors), Danger) }
            items(errors, key = { it.id }) { question -> AnswerDetail(question, answers.first { it.questionId == question.id }) }
            item { Action(stringResource(R.string.retry_all), !busy) { start(QuestionSet(attempt.setKey, selected?.phase, questions), attempt.setLabel) } }
            if (errors.isNotEmpty()) item { Action(stringResource(R.string.retry_errors), !busy) { start(QuestionSet("retry", null, errors), retryLabel) } }
            item { Action(stringResource(R.string.history)) { navigate("history") } }
        } else {
            val question = questions.getOrNull(index)
            if (question != null) {
                val answer = answers.find { it.questionId == question.id }
                item {
                    Text(stringResource(R.string.question_count, index + 1, questions.size))
                    LinearProgressIndicator(progress = { answers.size.toFloat() / questions.size }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    val phase = data.phases.firstOrNull { it.id == question.phaseId }
                    Label(if (phase == null) stringResource(R.string.rules) else stringResource(R.string.phase_title, phase.id, phase.title), Cool)
                    Text(question.question, style = MaterialTheme.typography.headlineSmall)
                }
                items(question.options.indices.toList()) { option ->
                    val color = when {
                        answer == null -> Border
                        option == question.correct -> Safe
                        option == answer.selectedIndex -> Danger
                        else -> Mute
                    }
                    Panel(Modifier.clickable(enabled = answer == null && !busy) { vm.answer(attempt.id, question, option) }.heightIn(min = 56.dp), color) {
                        Text(question.options[option], color = if (answer != null && option != question.correct && option != answer.selectedIndex) Mute else Ink)
                    }
                }
                if (answer != null) {
                    item { Panel { Text(question.explanation) } }
                    item { Action(stringResource(if (index == questions.lastIndex) R.string.finish else R.string.further), !busy) {
                        if (index == questions.lastIndex) vm.finish(attempt.id) {} else index++
                    } }
                }
            }
        }
    }
}
@Composable
fun AnswerDetail(question: TrainerQuestion, answer: Answer) {
    Panel(color = if (answer.correct) Safe else Danger) {
        Text(stringResource(R.string.attempt_answer, if (answer.correct) "✓" else "✗", question.question))
        Text(stringResource(R.string.selected_answer, question.options.getOrNull(answer.selectedIndex).orEmpty()))
        Text(stringResource(R.string.correct_answer, question.options[question.correct]), color = Safe)
        Text(question.explanation, color = Dim)
    }
}
@Composable
fun HistoryScreen(stored: StoredState, vm: AppViewModel, open: (String) -> Unit) {
    var clear by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Action(stringResource(R.string.clear_history), stored.attempts.isNotEmpty()) { clear = true } }
        if (stored.attempts.isEmpty()) item { Text(stringResource(R.string.history_empty), color = Dim) }
        items(stored.attempts, key = { it.id }) { attempt ->
            Panel(Modifier.clickable { open(attempt.id) }) {
                Text(TimeFormat.format(attempt.startedAt, pattern = "dd.MM.yyyy HH:mm")); Text(attempt.setLabel)
                val score = attempt.score ?: TrainerEngine.score(vm.app.trainer.answers(attempt))
                Text(stringResource(R.string.score_short, score, attempt.total, TrainerEngine.percent(score, attempt.total)))
                if (attempt.finishedAt == null) Label(stringResource(R.string.attempt_unfinished), Amber)
            }
        }
    }
    if (clear) AlertDialog(onDismissRequest = { clear = false }, title = { Text(stringResource(R.string.clear_history_question)) },
        confirmButton = { TextButton(shape = MaterialTheme.shapes.small, onClick = { vm.clearHistory(); clear = false }) { Text(stringResource(R.string.clear)) } },
        dismissButton = { TextButton(shape = MaterialTheme.shapes.small, onClick = { clear = false }) { Text(stringResource(R.string.cancel)) } })
}
@Composable
fun AttemptScreen(data: StopAlgorithm, stored: StoredState, id: String, vm: AppViewModel) {
    val attempt = stored.attempts.find { it.id == id }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (attempt != null) {
            item { Text(attempt.setLabel); Text(TimeFormat.format(attempt.startedAt, pattern = "dd.MM.yyyy HH:mm")); if (attempt.finishedAt == null) Text(stringResource(R.string.attempt_unfinished)) }
            items(vm.app.trainer.answers(attempt), key = { it.questionId }) { answer ->
                data.trainerQuestions.find { it.id == answer.questionId }?.let { AnswerDetail(it, answer) }
            }
        }
    }
}
