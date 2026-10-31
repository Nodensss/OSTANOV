package ru.upestop.app.ui.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
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
import ru.upestop.app.data.model.CheckItem
import ru.upestop.app.data.model.CheckType
import ru.upestop.app.data.model.Phase
import ru.upestop.app.domain.Progress
import ru.upestop.app.domain.TimeFormat
import ru.upestop.app.domain.sections
import ru.upestop.app.ui.components.KeepScreenOn
import ru.upestop.app.ui.components.LoadingScreen
import ru.upestop.app.ui.components.ParamChip
import ru.upestop.app.ui.components.PhaseCard
import ru.upestop.app.ui.components.ProgressLine
import ru.upestop.app.ui.components.SchemeBlock
import ru.upestop.app.ui.components.SectionLabel
import ru.upestop.app.ui.components.TimerActions
import ru.upestop.app.ui.components.TimerPanel
import ru.upestop.app.ui.components.UpeTopBar
import ru.upestop.app.ui.components.WarningBlock
import ru.upestop.app.ui.components.appViewModel
import ru.upestop.app.ui.theme.UpeColors
import ru.upestop.app.ui.theme.UpeText
import java.time.ZoneId

class ChecklistViewModel(private val container: AppContainer) : ViewModel() {
    val snapshot: StateFlow<StopSnapshot?> =
        container.stopSnapshot.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setChecked(checkId: String, checked: Boolean) {
        viewModelScope.launch { container.progressRepository.setChecked(checkId, checked, System.currentTimeMillis()) }
    }

    fun resetStop() {
        viewModelScope.launch { container.resetStop() }
    }

    fun timerActions(phaseId: Int): TimerActions = timerActions(container, phaseId)
}

/** Действия таймера этапа через TimerController (общие для чек-листа и пошагового режима). */
fun ViewModel.timerActions(container: AppContainer, phaseId: Int) = TimerActions(
    onStart = { viewModelScope.launch { container.timerController.start(phaseId) } },
    onPause = { viewModelScope.launch { container.timerController.pause(phaseId) } },
    onReset = { viewModelScope.launch { container.timerController.reset(phaseId) } },
    onExpired = { viewModelScope.launch { container.timerController.finishIfExpired(phaseId) } },
)

@Composable
fun ChecklistScreen(
    focusPhase: Int?,
    onFocusHandled: () -> Unit,
    onOpenStep: (String?) -> Unit,
    onOpenJournal: () -> Unit,
    viewModel: ChecklistViewModel = appViewModel { container, _ -> ChecklistViewModel(container) },
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val state = snapshot ?: return LoadingScreen()
    val algorithm = state.algorithm
    KeepScreenOn(state.settings.keepScreenOn)

    // Индексы элементов списка: заголовок раздела, затем его этапы.
    val itemIndex = remember(algorithm) {
        buildMap {
            var index = 0
            algorithm.sections().forEach { section ->
                index++
                algorithm.phases.filter { it.section == section }.forEach { put(it.id, index++) }
            }
        }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var expanded by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    var initialized by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    fun focus(phaseId: Int) {
        if (phaseId !in expanded) expanded = expanded + phaseId
        itemIndex[phaseId]?.let { scope.launch { listState.animateScrollToItem(it) } }
    }

    LaunchedEffect(Unit) {
        if (!initialized) {
            initialized = true
            Progress.currentPhase(algorithm, state.checked)?.let { expanded = listOf(it.id) }
        }
    }
    LaunchedEffect(focusPhase) {
        focusPhase?.let {
            focus(it)
            onFocusHandled()
        }
    }

    Scaffold(
        containerColor = UpeColors.Bg,
        topBar = {
            UpeTopBar(title = stringResource(R.string.checklist_title)) {
                IconButton(onClick = { onOpenStep(null) }) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = stringResource(R.string.mode_step))
                }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_journal)) },
                        onClick = {
                            menuOpen = false
                            onOpenJournal()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_reset), color = UpeColors.Danger) },
                        onClick = {
                            menuOpen = false
                            confirmReset = true
                        },
                    )
                }
            }
        },
        floatingActionButton = {
            if (!state.allDone) {
                ExtendedFloatingActionButton(
                    onClick = { Progress.firstUnchecked(state.flat, state.checked)?.let { focus(it.phase.id) } },
                    icon = { Icon(Icons.Filled.MyLocation, contentDescription = null) },
                    text = { Text(stringResource(R.string.checklist_to_current)) },
                    containerColor = UpeColors.Cool,
                    contentColor = UpeColors.Bg,
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(UpeColors.Surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.checklist_progress, state.done, state.total),
                    style = UpeText.mono,
                    color = UpeColors.Text,
                )
                ProgressLine(if (state.total == 0) 0f else state.done.toFloat() / state.total)
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                algorithm.sections().forEach { section ->
                    item(key = "section:$section") {
                        SectionLabel(section, modifier = Modifier.padding(top = 8.dp))
                    }
                    algorithm.phases.filter { it.section == section }.forEach { phase ->
                        item(key = "phase:${phase.id}") {
                            ChecklistPhase(
                                phase = phase,
                                state = state,
                                expanded = phase.id in expanded,
                                onToggleExpanded = {
                                    expanded = if (phase.id in expanded) expanded - phase.id else expanded + phase.id
                                },
                                onCheck = viewModel::setChecked,
                                timerActions = viewModel.timerActions(phase.id),
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = UpeColors.Surface,
            title = { Text(stringResource(R.string.reset_title)) },
            text = { Text(stringResource(R.string.reset_text), color = UpeColors.TextDim) },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        confirmReset = false
                        onOpenJournal()
                    }) { Text(stringResource(R.string.reset_save_journal)) }
                    TextButton(onClick = {
                        confirmReset = false
                        viewModel.resetStop()
                        expanded = emptyList()
                        algorithm.phases.firstOrNull()?.let { focus(it.id) }
                    }) { Text(stringResource(R.string.reset_confirm), color = UpeColors.Danger) }
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun ChecklistPhase(
    phase: Phase,
    state: StopSnapshot,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onCheck: (String, Boolean) -> Unit,
    timerActions: TimerActions,
) {
    val done = Progress.isPhaseDone(phase, state.checked)
    PhaseCard(
        phase = phase,
        expanded = expanded,
        onToggle = onToggleExpanded,
        done = done,
        trailing = stringResource(R.string.phase_counter, Progress.phaseCompleted(phase, state.checked), phase.checks.size),
    ) {
        if (phase.timer != null) {
            TimerPanel(phase, state.timer(phase.id), state.settings.timerTestMode, timerActions)
        }
        phase.warnings.forEach { WarningBlock(it) }
        phase.scheme?.let { SchemeBlock(it) }
        phase.checks.forEach { check ->
            CheckRow(check, state.checked[check.id], state.settings.haptics, onToggle = { onCheck(check.id, it) })
        }
    }
}

@Composable
fun CheckRow(check: CheckItem, checkedAt: Long?, haptics: Boolean, onToggle: (Boolean) -> Unit) {
    val checked = checkedAt != null
    val haptic = LocalHapticFeedback.current
    val stateText = stringResource(if (checked) R.string.check_done_state else R.string.check_not_done_state)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(UpeColors.Surface2, RectangleShape)
            .border(1.dp, if (checked) UpeColors.Border else UpeColors.Border.copy(alpha = 0.9f), RectangleShape)
            .toggleable(value = checked, role = Role.Checkbox) { next ->
                if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle(next)
            }
            .semantics { stateDescription = stateText }
            .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.padding(12.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = UpeColors.Cool,
                uncheckedColor = UpeColors.TextMute,
                checkmarkColor = UpeColors.Bg,
            ),
        )
        Column(Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(check.id, style = UpeText.mono.copy(fontSize = 11.sp), color = UpeColors.TextMute, modifier = Modifier.weight(1f))
                checkedAt?.let {
                    Text(TimeFormat.time(it, ZoneId.systemDefault()), style = UpeText.mono.copy(fontSize = 12.sp), color = UpeColors.Safe)
                }
            }
            Text(
                text = check.text,
                color = if (checked) UpeColors.TextMute else UpeColors.Text,
                textDecoration = if (checked) TextDecoration.LineThrough else null,
                fontSize = 15.sp,
                lineHeight = 21.sp,
            )
            if (check.type == CheckType.PARAM && !check.param.isNullOrBlank()) ParamChip(check.param)
        }
    }
}
