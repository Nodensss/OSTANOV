package ru.upestop.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import ru.upestop.app.R
import ru.upestop.app.data.model.*
import ru.upestop.app.domain.*
import ru.upestop.app.ui.components.*
import ru.upestop.app.ui.theme.*

private data class ChecklistEntry(val key: String, val section: String? = null, val phase: Phase? = null, val kind: String, val check: CheckItem? = null)
@Composable
fun ChecklistScreen(data: StopAlgorithm, stored: StoredState, vm: AppViewModel, now: Long, busy: Boolean, target: Int, allowed: Boolean, allow: () -> Unit) {
    val progress = Progress(data, stored.checks)
    var expanded by rememberSaveable { mutableStateOf(listOfNotNull(if (target > 0) target else progress.currentPhase?.id)) }
    var scrollKey by rememberSaveable { mutableStateOf(if (target > 0) "phase:$target" else "") }
    val list = rememberLazyListState()
    val entries = buildList {
        data.phases.groupBy { it.section }.forEach { (section, phases) ->
            add(ChecklistEntry("section:$section", section = section, kind = "section"))
            phases.forEach { phase ->
                add(ChecklistEntry("phase:${phase.id}", phase = phase, kind = "header"))
                if (phase.id in expanded) {
                    add(ChecklistEntry("details:${phase.id}", phase = phase, kind = "details"))
                    phase.checks.forEach { add(ChecklistEntry("check:${it.id}", phase = phase, kind = "check", check = it)) }
                }
            }
        }
    }
    LaunchedEffect(scrollKey, entries.size) {
        if (scrollKey.isNotBlank()) {
            val index = entries.indexOfFirst { it.key == scrollKey }
            if (index >= 0) { list.scrollToItem(index); scrollKey = "" }
        }
    }
    fun toPhase(id: Int) { if (id !in expanded) expanded = expanded + id; scrollKey = "phase:$id" }
    Box(Modifier.fillMaxSize()) {
        Column {
            Column(Modifier.fillMaxWidth().background(Bg).padding(16.dp)) {
                ProgressBlock(progress); RunningTimers(data, stored, now, ::toPhase)
            }
            LazyColumn(state = list, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries, key = { it.key }) { item ->
                    when (item.kind) {
                        "section" -> Label(item.section.orEmpty(), Cool)
                        "header" -> {
                            val phase = item.phase!!
                            Panel(Modifier.clickable { expanded = if (phase.id in expanded) expanded - phase.id else expanded + phase.id }) {
                                Text(phase.id.toString().padStart(2, '0'), color = if (progress.phaseDone(phase)) Safe else Cool,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 32.sp)
                                PhaseHeader(phase)
                                Label(stringResource(R.string.checks_count, phase.checks.count { it.id in progress.checked }, phase.checks.size), if (progress.phaseDone(phase)) Safe else Dim)
                            }
                        }
                        "details" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val phase = item.phase!!
                            if (phase.timer != null) TimerPanel(phase, stored, vm, now, busy, allowed, allow)
                            Warnings(phase); phase.scheme?.let { SchemeBlock(it) }
                        }
                        "check" -> {
                            val check = item.check!!
                            val mark = progress.checked[check.id]
                            val haptic = LocalHapticFeedback.current
                            val stateLabel = stringResource(if (mark != null) R.string.done else R.string.not_done)
                            Row(Modifier.fillMaxWidth().background(Surface2).border(1.dp, Border)
                                .toggleable(mark != null, enabled = !busy, role = Role.Checkbox) {
                                    vm.check(check.id, it)
                                    if (stored.settings.vibration) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }.semantics(mergeDescendants = true) { stateDescription = stateLabel }
                                .heightIn(min = 48.dp).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Checkbox(checked = mark != null, onCheckedChange = null)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Label(check.id)
                                        mark?.checkedAt?.let { Label(TimeFormat.format(it), Safe) }
                                    }
                                    Text(check.text, color = if (mark != null) Mute else Ink,
                                        textDecoration = if (mark != null) TextDecoration.LineThrough else null)
                                    Param(check)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (progress.firstIndex >= 0) ExtendedFloatingActionButton(shape = MaterialTheme.shapes.small, onClick = {
            progress.currentPhase?.let { phase ->
                if (phase.id !in expanded) expanded = expanded + phase.id
                scrollKey = "check:${progress.operations[progress.firstIndex].check.id}"
            }
        }, modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd).padding(16.dp)) { Text(stringResource(R.string.current_step)) }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepScreen(data: StopAlgorithm, stored: StoredState, vm: AppViewModel, now: Long, busy: Boolean, allowed: Boolean, allow: () -> Unit, navigate: (String) -> Unit) {
    val progress = Progress(data, stored.checks)
    var cursor by rememberSaveable { mutableIntStateOf(progress.firstIndex.coerceAtLeast(0)) }
    val operation = progress.operations.getOrNull(cursor)
    var scheme by rememberSaveable { mutableStateOf(false) }
    var phaseComplete by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    if (progress.firstIndex < 0 || operation == null) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.all_done), style = MaterialTheme.typography.headlineMedium)
            val times = progress.checked.values.mapNotNull { it.checkedAt }
            if (times.isNotEmpty()) Text(stringResource(R.string.report_range, TimeFormat.format(times.min()), TimeFormat.format(times.max())))
            Action(stringResource(R.string.report)) { navigate("report") }; Action(stringResource(R.string.to_home)) { navigate("home") }
        }
        return
    }
    val phase = operation.phase
    val check = operation.check
    val checked = progress.checked[check.id]
    var warningsOpen by rememberSaveable(check.id) { mutableStateOf(phase.checks.first().id == check.id) }
    val finishedLabel = stringResource(R.string.phase_finished, phase.id)
    val haptic = LocalHapticFeedback.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        phaseComplete?.let { Panel(color = Safe) { Text(it) } }
        Label(stringResource(R.string.phase_heading, phase.id, data.phases.size, phase.section), Cool)
        PhaseHeader(phase)
        Text(stringResource(R.string.step_count, cursor + 1, progress.total)); ProgressBlock(progress)
        if (!phase.warnings.isNullOrEmpty()) { Action(stringResource(R.string.warnings)) { warningsOpen = !warningsOpen }; if (warningsOpen) Warnings(phase) }
        if (phase.timer != null) TimerPanel(phase, stored, vm, now, busy, allowed, allow)
        if (phase.scheme != null) Action(stringResource(R.string.scheme)) { scheme = true }
        Panel { Label(check.id, Cool); Text(check.text, fontSize = 24.sp, lineHeight = 34.sp); Param(check) }
        if (checked != null) {
            Text(stringResource(R.string.done_at, checked.checkedAt?.let { TimeFormat.format(it) }.orEmpty()), color = Safe)
            Action(stringResource(R.string.uncheck), !busy) { vm.check(check.id, false) }
        } else Button(shape = MaterialTheme.shapes.small, onClick = {
            val nextProgress = Progress(data, stored.checks.filter { it.checkId != check.id } + CheckState(check.id, true, now))
            vm.check(check.id, true) {
                if (stored.settings.vibration) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val next = nextProgress.nextUnchecked(cursor)
                if (nextProgress.phaseDone(phase)) {
                    val completedCursor = cursor
                    phaseComplete = finishedLabel
                    scope.launch {
                        delay(2500)
                        phaseComplete = null
                        if (next >= 0 && cursor == completedCursor) cursor = next
                    }
                } else if (next >= 0) cursor = next
            }
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp), enabled = !busy) { Text(stringResource(R.string.done), fontSize = 22.sp) }
        Action(stringResource(R.string.previous), cursor > 0 && !busy) { cursor-- }
        Action(stringResource(R.string.next), cursor < progress.total - 1 && !busy) { cursor++ }
    }
    if (scheme && phase.scheme != null) ModalBottomSheet(onDismissRequest = { scheme = false }) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) { SchemeBlock(phase.scheme); Action(stringResource(R.string.close)) { scheme = false } }
    }
}
@Composable
fun TimersScreen(data: StopAlgorithm, stored: StoredState, vm: AppViewModel, now: Long, busy: Boolean, allowed: Boolean, allow: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(data.phases.filter { it.timer != null }, key = { it.id }) { phase ->
            Text(stringResource(R.string.phase_title, phase.id, phase.title)); TimerPanel(phase, stored, vm, now, busy, allowed, allow)
        }
    }
}
