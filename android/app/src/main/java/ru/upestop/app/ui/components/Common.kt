package ru.upestop.app.ui.components

import android.content.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.upestop.app.R
import ru.upestop.app.data.model.*
import ru.upestop.app.domain.*
import ru.upestop.app.ui.theme.*

@Composable
fun Panel(modifier: Modifier = Modifier, color: Color = Border, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().border(1.dp, color).background(if (color == Border) Surface else color.copy(alpha = .08f)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}
@Composable
fun Label(text: String, color: Color = Dim) { Text(text, color = color, fontFamily = FontFamily.Monospace, fontSize = 12.sp, letterSpacing = 1.sp) }
@Composable
fun Action(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(onClick, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = enabled) { Text(text) }
}
@Composable
fun ProgressBlock(progress: Progress) {
    Text(stringResource(R.string.steps_progress, progress.count, progress.total))
    LinearProgressIndicator(progress = { if (progress.total == 0) 0f else progress.count.toFloat() / progress.total }, modifier = Modifier.fillMaxWidth())
}
@Composable
fun PhaseHeader(phase: Phase) {
    Label(stringResource(R.string.phase_section, phase.id, phase.section), Cool)
    Text(phase.title, style = MaterialTheme.typography.titleLarge)
    if (phase.subtitle.isNotBlank()) Text(phase.subtitle, color = Dim)
    phase.condition?.let { Text(it, color = Amber) }
    if (phase.critical) Label(stringResource(R.string.critical), Danger)
}
@Composable
fun Warnings(phase: Phase) {
    phase.warnings.orEmpty().forEach { warning -> Panel(color = Warn) { Label("⚠ ${warning.title}", Warn); Text(warning.text) } }
}
@Composable
fun SchemeBlock(scheme: Scheme) {
    Panel { Text(scheme.title); Text(scheme.lines.joinToString("\n"), fontFamily = FontFamily.Monospace,
        modifier = Modifier.horizontalScroll(rememberScrollState()), softWrap = false) }
}
@Composable
fun Param(check: CheckItem) { check.param?.let { Text(it, color = Amber, fontFamily = FontFamily.Monospace,
    modifier = Modifier.border(1.dp, Amber.copy(alpha = .4f)).background(Amber.copy(alpha = .08f)).padding(8.dp)) } }
@Composable
fun Confirm(title: String, onDismiss: () -> Unit, onConfirm: () -> Unit, extra: (@Composable () -> Unit)? = null) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.reset)) } },
        dismissButton = { Column { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }; extra?.invoke() } })
}
fun shareReport(context: Context, report: String) {
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, report), context.getString(R.string.share)))
}
fun buildReport(context: Context, algorithm: StopAlgorithm, checks: List<CheckState>, timers: List<TimerState>, testMode: Boolean): String {
    // Explicit resource IDs keep translation and resource-shrinker behavior deterministic.
    val resources = mapOf("report_header" to R.string.report_header, "report_generated" to R.string.report_generated,
        "report_progress" to R.string.report_progress, "report_range" to R.string.report_range, "report_timers" to R.string.report_timers,
        "report_timer" to R.string.report_timer, "report_timer_done" to R.string.report_timer_done,
        "report_timer_running" to R.string.report_timer_running, "report_timer_paused" to R.string.report_timer_paused,
        "report_timer_idle" to R.string.report_timer_idle, "report_phase" to R.string.report_phase, "report_critical" to R.string.report_critical)
    return StopReportBuilder { key, args -> context.getString(resources.getValue(key), *args.toTypedArray()) }
        .build(algorithm, checks, timers, System.currentTimeMillis(), java.time.ZoneId.systemDefault(), testMode)
}
