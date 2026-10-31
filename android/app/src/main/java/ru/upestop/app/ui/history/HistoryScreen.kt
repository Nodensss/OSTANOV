package ru.upestop.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.upestop.app.AppContainer
import ru.upestop.app.R
import ru.upestop.app.data.model.StopAlgorithm
import ru.upestop.app.domain.TimeFormat
import ru.upestop.app.domain.TrainerAttempt
import ru.upestop.app.domain.TrainerEngine
import ru.upestop.app.ui.components.ConfirmDialog
import ru.upestop.app.ui.components.LoadingScreen
import ru.upestop.app.ui.components.Panel
import ru.upestop.app.ui.components.SectionLabel
import ru.upestop.app.ui.components.UpeTopBar
import ru.upestop.app.ui.components.appViewModel
import ru.upestop.app.ui.theme.UpeColors
import ru.upestop.app.ui.theme.UpeText
import ru.upestop.app.ui.trainer.AnswerReview
import java.time.ZoneId

class HistoryViewModel(private val container: AppContainer) : ViewModel() {
    val attempts: StateFlow<List<TrainerAttempt>?> =
        container.trainerRepository.attempts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun clear() {
        viewModelScope.launch { container.trainerRepository.clear() }
    }
}

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenAttempt: (String) -> Unit,
    viewModel: HistoryViewModel = appViewModel { container, _ -> HistoryViewModel(container) },
) {
    val attemptsState by viewModel.attempts.collectAsStateWithLifecycle()
    val attempts = attemptsState ?: return LoadingScreen()
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = UpeColors.Bg,
        topBar = {
            UpeTopBar(title = stringResource(R.string.history_title), onBack = onBack) {
                if (attempts.isNotEmpty()) {
                    IconButton(onClick = { confirmClear = true }) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = stringResource(R.string.history_clear))
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (attempts.isEmpty()) {
                item(key = "empty") { Text(stringResource(R.string.history_empty), color = UpeColors.TextDim) }
            }
            items(attempts, key = { it.id }) { attempt -> AttemptRow(attempt) { onOpenAttempt(attempt.id) } }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = stringResource(R.string.history_clear_title),
            text = stringResource(R.string.history_clear_text),
            confirmLabel = stringResource(R.string.history_clear_confirm),
            onConfirm = {
                confirmClear = false
                viewModel.clear()
            },
            onDismiss = { confirmClear = false },
        )
    }
}

@Composable
private fun AttemptRow(attempt: TrainerAttempt, onClick: () -> Unit) {
    val score = attempt.score
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(UpeColors.Surface, RectangleShape)
            .border(1.dp, UpeColors.Border, RectangleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(TimeFormat.dateTime(attempt.startedAt, ZoneId.systemDefault()), style = UpeText.mono.copy(fontSize = 12.sp), color = UpeColors.TextMute)
            Text(attempt.setLabel, color = UpeColors.Text)
        }
        if (attempt.finished && score != null) {
            Text(
                stringResource(R.string.history_score, score, attempt.total, TrainerEngine.percent(score, attempt.total)),
                style = UpeText.mono.copy(fontWeight = FontWeight.SemiBold),
                color = if (score == attempt.total) UpeColors.Safe else UpeColors.Cool,
            )
        } else {
            Text(stringResource(R.string.history_unfinished), color = UpeColors.Warn, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

class AttemptViewModel(container: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val id: String = handle[ARG_ID] ?: ""

    val state: StateFlow<Pair<StopAlgorithm, TrainerAttempt?>?> =
        combine(container.algorithmRepository.algorithm, container.trainerRepository.attempt(id)) { algorithm, attempt ->
            algorithm to attempt
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        const val ARG_ID = "id"
    }
}

@Composable
fun AttemptScreen(
    onBack: () -> Unit,
    viewModel: AttemptViewModel = appViewModel { container, handle -> AttemptViewModel(container, handle) },
) {
    val loaded by viewModel.state.collectAsStateWithLifecycle()
    val (algorithm, attempt) = loaded ?: return LoadingScreen()

    Scaffold(
        containerColor = UpeColors.Bg,
        topBar = { UpeTopBar(title = stringResource(R.string.attempt_title), onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (attempt == null) {
                item { Text(stringResource(R.string.history_empty), color = UpeColors.TextDim) }
                return@LazyColumn
            }
            item(key = "summary") {
                Panel {
                    SectionLabel(TimeFormat.dateTime(attempt.startedAt, ZoneId.systemDefault()))
                    Text(attempt.setLabel, color = UpeColors.Text, fontWeight = FontWeight.SemiBold)
                    val score = attempt.score
                    Text(
                        if (attempt.finished && score != null) {
                            stringResource(R.string.history_score, score, attempt.total, TrainerEngine.percent(score, attempt.total))
                        } else {
                            stringResource(R.string.history_unfinished) + " · " +
                                stringResource(R.string.attempt_answered, attempt.answers.size, attempt.total)
                        },
                        style = UpeText.mono,
                        color = UpeColors.TextDim,
                    )
                }
            }
            items(attempt.answers, key = { it.questionId }) { answer ->
                val question = algorithm.trainerQuestions.firstOrNull { it.id == answer.questionId }
                if (question == null) {
                    Text(stringResource(R.string.attempt_question_missing, answer.questionId), color = UpeColors.TextMute)
                } else {
                    AnswerReview(algorithm, question, answer)
                }
            }
        }
    }
}
