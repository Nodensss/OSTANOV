package ru.upestop.app.ui.step

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.upestop.app.AppContainer
import ru.upestop.app.R
import ru.upestop.app.data.StopSnapshot
import ru.upestop.app.data.model.CheckType
import ru.upestop.app.data.model.WarningItem
import ru.upestop.app.domain.Progress
import ru.upestop.app.domain.TimeFormat
import ru.upestop.app.ui.checklist.timerActions
import ru.upestop.app.ui.components.CriticalBadge
import ru.upestop.app.ui.components.KeepScreenOn
import ru.upestop.app.ui.components.LoadingScreen
import ru.upestop.app.ui.components.OutlineAction
import ru.upestop.app.ui.components.ParamChip
import ru.upestop.app.ui.components.Panel
import ru.upestop.app.ui.components.ProgressLine
import ru.upestop.app.ui.components.SchemeBlock
import ru.upestop.app.ui.components.SectionLabel
import ru.upestop.app.ui.components.TimerPanel
import ru.upestop.app.ui.components.UpeTopBar
import ru.upestop.app.ui.components.WarningBlock
import ru.upestop.app.ui.components.appViewModel
import ru.upestop.app.ui.theme.UpeColors
import ru.upestop.app.ui.theme.UpeText
import java.time.ZoneId

/**
 * Пошаговый режим: курсор по сквозному списку операций. Позиция курсора хранится
 * в SavedStateHandle и переживает поворот экрана.
 */
class StepViewModel(private val container: AppContainer, private val handle: SavedStateHandle) : ViewModel() {
    val snapshot: StateFlow<StopSnapshot?> =
        container.stopSnapshot.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val cursor: StateFlow<Int> = handle.getStateFlow(CURSOR, -1)

    private val _phaseCompleted = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val phaseCompleted: SharedFlow<Int> = _phaseCompleted

    init {
        viewModelScope.launch {
            val state = snapshot.filterNotNull().first()
            if (cursor.value < 0) {
                val requested = handle.get<String>(ARG_CHECK)
                handle[CURSOR] = state.flat.indexOfFirst { it.check.id == requested }.takeIf { it >= 0 }
                    ?: Progress.firstUnchecked(state.flat, state.checked)?.index
                    ?: 0
            }
        }
    }

    fun done() {
        val state = snapshot.value ?: return
        val item = state.flat.getOrNull(cursor.value) ?: return
        val now = System.currentTimeMillis()
        val checked = state.checked + (item.check.id to now)
        viewModelScope.launch { container.progressRepository.setChecked(item.check.id, true, now) }
        if (Progress.isPhaseDone(item.phase, checked) && !Progress.isPhaseDone(item.phase, state.checked)) {
            _phaseCompleted.tryEmit(item.phase.id)
        }
        Progress.nextUnchecked(state.flat, checked, item.index)?.let { handle[CURSOR] = it.index }
    }

    fun uncheck() {
        val item = snapshot.value?.flat?.getOrNull(cursor.value) ?: return
        viewModelScope.launch { container.progressRepository.setChecked(item.check.id, false, System.currentTimeMillis()) }
    }

    fun move(delta: Int) {
        val size = snapshot.value?.flat?.size ?: return
        handle[CURSOR] = (cursor.value + delta).coerceIn(0, (size - 1).coerceAtLeast(0))
    }

    fun timerActions(phaseId: Int) = timerActions(container, phaseId)

    companion object {
        const val ARG_CHECK = "check"
        private const val CURSOR = "cursor"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepScreen(
    onBack: () -> Unit,
    onOpenChecklist: () -> Unit,
    onOpenJournal: () -> Unit,
    onHome: () -> Unit,
    viewModel: StepViewModel = appViewModel { container, handle -> StepViewModel(container, handle) },
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val cursor by viewModel.cursor.collectAsStateWithLifecycle()
    val state = snapshot ?: return LoadingScreen()
    val item = state.flat.getOrNull(cursor) ?: return LoadingScreen()
    KeepScreenOn(state.settings.keepScreenOn)

    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.phaseCompleted.collect { snackbar.showSnackbar(context.getString(R.string.step_phase_done, it)) }
    }
    var reviewing by rememberSaveable { mutableStateOf(false) }
    var schemeOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = UpeColors.Bg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            UpeTopBar(title = stringResource(R.string.step_title), onBack = onBack) {
                IconButton(onClick = onOpenChecklist) {
                    Icon(Icons.Filled.Checklist, contentDescription = stringResource(R.string.step_open_checklist))
                }
            }
        },
        bottomBar = {
            if (!state.allDone || reviewing) StepControls(state, cursor, viewModel)
        },
    ) { padding ->
        if (state.allDone && !reviewing) {
            AllDone(state, Modifier.padding(padding), onOpenJournal, onHome, onReview = { reviewing = true })
            return@Scaffold
        }
        val phase = item.phase
        val checkedAt = state.checked[item.check.id]
        val firstOfPhase = phase.checks.firstOrNull()?.id == item.check.id
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel(stringResource(R.string.step_header, phase.id, state.algorithm.phases.size, phase.section))
                Text(phase.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                phase.condition?.let { Text(it, style = UpeText.mono.copy(fontSize = 13.sp, lineHeight = 18.sp), color = UpeColors.Amber) }
                if (phase.critical) CriticalBadge()
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row {
                    Text(
                        stringResource(R.string.step_counter, item.index + 1, state.total),
                        style = UpeText.mono,
                        color = UpeColors.TextDim,
                        modifier = Modifier.weight(1f),
                    )
                    Text(stringResource(R.string.phase_counter, state.done, state.total), style = UpeText.mono, color = UpeColors.TextMute)
                }
                ProgressLine(if (state.total == 0) 0f else state.done.toFloat() / state.total)
            }

            if (phase.warnings.isNotEmpty()) {
                key(item.check.id) { WarningsBlock(phase.warnings, initiallyOpen = firstOfPhase) }
            }
            if (phase.timer != null) {
                TimerPanel(phase, state.timer(phase.id), state.settings.timerTestMode, viewModel.timerActions(phase.id), compact = true)
            }
            if (phase.scheme != null) {
                OutlineAction(stringResource(R.string.step_scheme), onClick = { schemeOpen = true }, color = UpeColors.Cool)
            }

            Panel(
                border = if (checkedAt != null) UpeColors.Safe.copy(alpha = 0.6f) else UpeColors.Cool.copy(alpha = 0.6f),
                contentPadding = PaddingValues(20.dp),
                spacing = 14,
            ) {
                Text(item.check.id, style = UpeText.mono.copy(fontSize = 13.sp, lineHeight = 18.sp), color = UpeColors.TextMute)
                Text(item.check.text, fontSize = 22.sp, lineHeight = 30.sp, color = UpeColors.Text, fontWeight = FontWeight.Medium)
                if (item.check.type == CheckType.PARAM && !item.check.param.isNullOrBlank()) {
                    ParamChip(item.check.param, large = true)
                }
                checkedAt?.let {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = UpeColors.Safe)
                        Text(
                            stringResource(R.string.step_done_at, TimeFormat.time(it, ZoneId.systemDefault())),
                            color = UpeColors.Safe,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }

    val scheme = item.phase.scheme
    if (schemeOpen && scheme != null) {
        ModalBottomSheet(onDismissRequest = { schemeOpen = false }, containerColor = UpeColors.Surface) {
            SchemeBlock(scheme, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 32.dp))
        }
    }
}

@Composable
private fun WarningsBlock(warnings: List<WarningItem>, initiallyOpen: Boolean) {
    var open by rememberSaveable { mutableStateOf(initiallyOpen) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .background(UpeColors.Warn.copy(alpha = 0.08f), RectangleShape)
                .border(1.dp, UpeColors.Warn.copy(alpha = 0.55f), RectangleShape)
                .clickable(role = Role.Button) { open = !open }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = UpeColors.Warn, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.step_warnings, warnings.size), color = UpeColors.Warn, modifier = Modifier.weight(1f))
            Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = UpeColors.Warn)
        }
        if (open) warnings.forEach { WarningBlock(it) }
    }
}

@Composable
private fun StepControls(state: StopSnapshot, cursor: Int, viewModel: StepViewModel) {
    val item = state.flat.getOrNull(cursor) ?: return
    val checked = item.check.id in state.checked
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(UpeColors.Surface)
            .navigationBarsPadding()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (checked) {
            Button(
                onClick = viewModel::uncheck,
                modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = UpeColors.Surface2, contentColor = UpeColors.Text),
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                Text(stringResource(R.string.step_uncheck), modifier = Modifier.padding(start = 8.dp), fontSize = 18.sp)
            }
        } else {
            Button(
                onClick = {
                    if (state.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.done()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = UpeColors.Cool, contentColor = UpeColors.Bg),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(28.dp))
                Text(stringResource(R.string.step_done), modifier = Modifier.padding(start = 8.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StepNavButton(
                text = stringResource(R.string.step_prev),
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                enabled = cursor > 0,
                onClick = { viewModel.move(-1) },
                modifier = Modifier.weight(1f),
            )
            StepNavButton(
                text = stringResource(R.string.step_next),
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                enabled = cursor < state.flat.lastIndex,
                onClick = { viewModel.move(1) },
                modifier = Modifier.weight(1f),
                iconFirst = false,
            )
        }
    }
}

@Composable
private fun StepNavButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconFirst: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(2.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = UpeColors.Bg,
            contentColor = UpeColors.Text,
            disabledContainerColor = UpeColors.Bg,
            disabledContentColor = UpeColors.TextMute,
        ),
        border = BorderStroke(1.dp, UpeColors.Border),
    ) {
        if (iconFirst) Icon(icon, contentDescription = null)
        Text(text, fontSize = 16.sp)
        if (!iconFirst) Icon(icon, contentDescription = null)
    }
}

@Composable
private fun AllDone(state: StopSnapshot, modifier: Modifier, onOpenJournal: () -> Unit, onHome: () -> Unit, onReview: () -> Unit) {
    val range = Progress.markRange(state.algorithm, state.checked)
    val zone = ZoneId.systemDefault()
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Panel(border = UpeColors.Safe.copy(alpha = 0.6f)) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = UpeColors.Safe, modifier = Modifier.size(40.dp))
            Text(stringResource(R.string.step_all_done_title), style = MaterialTheme.typography.headlineSmall, color = UpeColors.Safe)
            range?.let { (first, last) ->
                Text(
                    stringResource(R.string.step_all_done_times, TimeFormat.dateTime(first, zone), TimeFormat.dateTime(last, zone)),
                    style = UpeText.mono,
                    color = UpeColors.TextDim,
                )
            }
        }
        Button(
            onClick = onOpenJournal,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(2.dp),
            colors = ButtonDefaults.buttonColors(containerColor = UpeColors.Cool, contentColor = UpeColors.Bg),
        ) { Text(stringResource(R.string.menu_journal), fontWeight = FontWeight.SemiBold) }
        OutlineAction(stringResource(R.string.step_home), onClick = onHome, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = onReview) {
            Icon(Icons.Filled.Checklist, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.step_review), modifier = Modifier.padding(start = 6.dp))
        }
    }
}
