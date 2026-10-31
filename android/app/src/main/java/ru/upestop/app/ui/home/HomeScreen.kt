package ru.upestop.app.ui.home

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.upestop.app.AppContainer
import ru.upestop.app.R
import ru.upestop.app.data.StopSnapshot
import ru.upestop.app.data.model.Phase
import ru.upestop.app.domain.Progress
import ru.upestop.app.domain.TimerMath
import ru.upestop.app.ui.components.LoadingScreen
import ru.upestop.app.ui.components.Panel
import ru.upestop.app.ui.components.ProgressLine
import ru.upestop.app.ui.components.SectionLabel
import ru.upestop.app.ui.components.UpeTopBar
import ru.upestop.app.ui.components.appViewModel
import ru.upestop.app.ui.components.rememberNow
import ru.upestop.app.ui.theme.UpeColors
import ru.upestop.app.ui.theme.UpeText

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    val snapshot: StateFlow<StopSnapshot?> =
        container.stopSnapshot.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onTimerExpired(phaseId: Int) {
        viewModelScope.launch { container.timerController.finishIfExpired(phaseId) }
    }
}

class HomeNavigation(
    val onOpenChecklist: () -> Unit,
    val onOpenPhase: (Int) -> Unit,
    val onOpenStep: () -> Unit,
    val onOpenReference: () -> Unit,
    val onOpenRules: () -> Unit,
    val onOpenTrainer: () -> Unit,
    val onOpenJournal: () -> Unit,
    val onOpenSettings: () -> Unit,
)

@Composable
fun HomeScreen(
    navigation: HomeNavigation,
    viewModel: HomeViewModel = appViewModel { container, _ -> HomeViewModel(container) },
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val state = snapshot ?: return LoadingScreen()
    val algorithm = state.algorithm

    Scaffold(
        containerColor = UpeColors.Bg,
        topBar = {
            UpeTopBar(title = stringResource(R.string.app_name)) {
                IconButton(onClick = navigation.onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
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
            Panel {
                SectionLabel(algorithm.subtitle)
                Text(algorithm.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Metric(algorithm.phases.size, stringResource(R.string.metric_phases), Modifier.weight(1f))
                    Metric(state.total, stringResource(R.string.metric_checks), Modifier.weight(1f))
                    Metric(algorithm.criticalRules.size, stringResource(R.string.metric_rules), Modifier.weight(1f))
                    Metric(algorithm.trainerQuestions.size, stringResource(R.string.metric_questions), Modifier.weight(1f))
                }
            }

            CurrentStopPanel(state, navigation, onTimerExpired = viewModel::onTimerExpired)

            SectionLabel(stringResource(R.string.home_modes))
            ModeTile(Icons.Filled.Checklist, R.string.mode_checklist, R.string.mode_checklist_note, navigation.onOpenChecklist)
            ModeTile(Icons.AutoMirrored.Filled.DirectionsWalk, R.string.mode_step, R.string.mode_step_note, navigation.onOpenStep)
            ModeTile(Icons.AutoMirrored.Filled.MenuBook, R.string.mode_reference, R.string.mode_reference_note, navigation.onOpenReference)
            ModeTile(Icons.Filled.GppMaybe, R.string.mode_rules, R.string.mode_rules_note, navigation.onOpenRules)
            ModeTile(Icons.Filled.Quiz, R.string.mode_trainer, R.string.mode_trainer_note, navigation.onOpenTrainer)

            Text(stringResource(R.string.disclaimer), color = UpeColors.TextMute, fontSize = 13.sp, lineHeight = 18.sp)
            TextButton(onClick = navigation.onOpenSettings) { Text(stringResource(R.string.home_settings_link)) }
        }
    }
}

@Composable
private fun CurrentStopPanel(state: StopSnapshot, navigation: HomeNavigation, onTimerExpired: (Int) -> Unit) {
    val currentPhase = Progress.firstUnchecked(state.flat, state.checked)?.phase
    val runningTimers = state.algorithm.phases.filter { it.timer != null && state.timer(it.id).running }

    Panel(border = UpeColors.Cool.copy(alpha = 0.4f)) {
        SectionLabel(stringResource(R.string.home_current_stop), color = UpeColors.Cool)
        Text(
            stringResource(R.string.home_progress, state.done, state.total, Progress.percent(state.done, state.total)),
            style = UpeText.mono,
            color = UpeColors.Text,
        )
        ProgressLine(if (state.total == 0) 0f else state.done.toFloat() / state.total)
        Text(
            text = when {
                state.allDone -> stringResource(R.string.home_all_done)
                state.done == 0 -> stringResource(R.string.home_not_started)
                else -> stringResource(R.string.home_current_phase, currentPhase?.id ?: 0, currentPhase?.title.orEmpty())
            },
            color = if (state.allDone) UpeColors.Safe else UpeColors.TextDim,
        )
        runningTimers.forEach { phase ->
            RunningTimerRow(phase, state, onClick = { navigation.onOpenPhase(phase.id) }, onExpired = onTimerExpired)
        }
        Button(
            onClick = if (state.allDone) navigation.onOpenJournal else navigation.onOpenStep,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(2.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.allDone) UpeColors.Safe else UpeColors.Cool,
                contentColor = UpeColors.Bg,
            ),
        ) {
            Text(
                stringResource(
                    when {
                        state.allDone -> R.string.home_action_finished
                        state.done == 0 -> R.string.home_action_start
                        else -> R.string.home_action_continue
                    },
                ),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun RunningTimerRow(phase: Phase, state: StopSnapshot, onClick: () -> Unit, onExpired: (Int) -> Unit) {
    val spec = phase.timer ?: return
    val timer = state.timer(phase.id)
    val now = rememberNow()
    val duration = TimerMath.effectiveDuration(timer, spec.minutes, state.settings.timerTestMode)
    val expired = TimerMath.isExpired(timer, duration, now)
    LaunchedEffect(expired) { if (expired) onExpired(phase.id) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(UpeColors.Bg, RectangleShape)
            .border(1.dp, UpeColors.Border, RectangleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Filled.Timer, contentDescription = null, tint = UpeColors.Cool, modifier = Modifier.size(18.dp))
        Text(
            stringResource(R.string.notification_timer_title, phase.id, spec.label),
            color = UpeColors.TextDim,
            fontSize = 13.sp, lineHeight = 18.sp,
            modifier = Modifier.weight(1f),
            maxLines = 2,
        )
        Text(
            TimerMath.formatRemaining(TimerMath.remaining(timer, duration, now)),
            style = UpeText.mono.copy(fontWeight = FontWeight.SemiBold),
            color = UpeColors.Text,
        )
    }
}

@Composable
private fun Metric(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(UpeColors.Bg.copy(alpha = 0.5f), RectangleShape)
            .border(1.dp, UpeColors.Border, RectangleShape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value.toString(), style = UpeText.mono.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold), color = UpeColors.Cool)
        Text(label, style = UpeText.label.copy(fontSize = 9.sp, letterSpacing = 0.8.sp), color = UpeColors.TextMute, maxLines = 1)
    }
}

@Composable
private fun ModeTile(icon: ImageVector, title: Int, note: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(UpeColors.Surface, RectangleShape)
            .border(1.dp, UpeColors.Border, RectangleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = UpeColors.Cool,
            modifier = Modifier
                .border(1.dp, UpeColors.Cool.copy(alpha = 0.5f), RectangleShape)
                .background(UpeColors.Cool.copy(alpha = 0.1f), RectangleShape)
                .padding(10.dp)
                .size(22.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(title), fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = UpeColors.Text)
            Text(stringResource(note), color = UpeColors.TextDim, fontSize = 13.sp, lineHeight = 18.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = UpeColors.TextMute)
    }
}
