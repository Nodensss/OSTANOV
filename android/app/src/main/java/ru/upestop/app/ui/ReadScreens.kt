package ru.upestop.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ru.upestop.app.R
import ru.upestop.app.data.model.*
import ru.upestop.app.domain.*
import ru.upestop.app.ui.components.*
import ru.upestop.app.ui.theme.*

@Composable
fun HomeScreen(data: StopAlgorithm, stored: StoredState, now: Long, navigate: (String) -> Unit) {
    val progress = Progress(data, stored.checks)
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Label(data.subtitle, Cool); Text(data.title, style = MaterialTheme.typography.headlineMedium) }
        item { Panel { Text(stringResource(R.string.metrics, data.phases.size, progress.total, data.criticalRules.size, data.trainerQuestions.size)) } }
        item { Panel {
            Label(stringResource(R.string.current_stop), Cool); Text(stringResource(R.string.progress, progress.count, progress.total)); ProgressBlock(progress)
            progress.currentPhase?.let { Text(stringResource(R.string.phase_title, it.id, it.title)) }
            RunningTimers(data, stored, now) { navigate("checklist?phase=$it") }
            Button(onClick = { navigate(if (progress.firstIndex < 0) "report" else "step") }, Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(stringResource(if (progress.firstIndex < 0) R.string.stop_done else if (progress.count == 0) R.string.start_stop else R.string.continue_stop))
            }
        } }
        items(listOf("checklist?phase=-1" to R.string.checklist, "step" to R.string.step, "reference" to R.string.reference, "rules" to R.string.rules, "trainer" to R.string.trainer)) { (route, label) -> Action(stringResource(label)) { navigate(route) } }
        item { Text(stringResource(R.string.disclaimer), color = Dim); Action(stringResource(R.string.settings)) { navigate("settings") } }
    }
}
@Composable
fun ReferenceScreen(data: StopAlgorithm) {
    var query by rememberSaveable { mutableStateOf("") }
    val phases = remember(data, query) { PositionSearch.filter(data.phases, query) }
    Column {
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(16.dp), label = { Text(stringResource(R.string.search_label)) },
            placeholder = { Text(stringResource(R.string.search_hint)) }, trailingIcon = { TextButton(onClick = { query = "" }) { Text(stringResource(R.string.clear)) } })
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (phases.isEmpty()) item { Text(stringResource(R.string.not_found, query)) }
            items(phases, key = { it.id }) { phase ->
                var open by rememberSaveable(phase.id, PositionSearch.normalize(query)) { mutableStateOf(PositionSearch.normalize(query).isNotEmpty() || data.phases.indexOf(phase) < 2) }
                Panel {
                    Column(Modifier.fillMaxWidth().clickable { open = !open }.heightIn(min = 48.dp)) { PhaseHeader(phase) }
                    if (open) {
                        FlowRowCompat(phase.positions, query)
                        Warnings(phase); phase.scheme?.let { SchemeBlock(it) }
                        phase.checks.forEach { check ->
                            Panel(color = if (PositionSearch.matches(check.text, query) || PositionSearch.matches(check.param.orEmpty(), query)) Cool else Border) {
                                Label(check.id); Text(check.text); Param(check)
                            }
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowCompat(positions: List<String>, query: String) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        positions.forEach { position ->
            val match = PositionSearch.matches(position, query)
            Text(position, fontFamily = FontFamily.Monospace, color = if (match) Bg else Dim,
                modifier = Modifier.background(if (match) Cool else Bg).border(1.dp, if (match) Cool else Border).padding(8.dp))
        }
    }
}
@Composable
fun RulesScreen(data: StopAlgorithm) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Label(stringResource(R.string.constraints), Danger) }
        items(data.criticalRules, key = { it.id }) { rule -> Panel(color = Danger) {
            Label(rule.id, Danger); Text(rule.title, style = MaterialTheme.typography.titleLarge); Text(rule.rule)
            if (rule.consequence.isNotBlank()) Panel { Text(rule.consequence) }
        } }
    }
}
