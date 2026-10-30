package ru.upestop.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ru.upestop.app.R
import ru.upestop.app.data.model.*
import ru.upestop.app.domain.*
import ru.upestop.app.ui.*
import ru.upestop.app.ui.theme.*

@Composable
fun TimerPanel(phase: Phase, stored: StoredState, vm: AppViewModel, now: Long, busy: Boolean, allowed: Boolean, allow: () -> Unit) {
    val spec = phase.timer ?: return
    val state = stored.timers.find { it.phaseId == phase.id } ?: TimerState(phase.id)
    val duration = TimerMath.duration(spec.minutes, stored.settings.testMode)
    val remaining = TimerMath.remaining(state, duration, now)
    var reset by remember { mutableStateOf(false) }
    val alpha = if (state.finished) {
        val transition = rememberInfiniteTransition(label = "finished")
        val value by transition.animateFloat(.4f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "border")
        value
    } else 1f
    Panel(color = if (state.finished) Danger.copy(alpha = alpha) else Cool) {
        Text(spec.label, color = Cool)
        if (stored.settings.testMode) Label(stringResource(R.string.timer_test_active), Danger)
        val fontScale = LocalDensity.current.fontScale
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Box(Modifier.size(minOf(maxWidth, (132 * fontScale).dp)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { (duration - remaining).toFloat() / duration }, modifier = Modifier.fillMaxSize(), color = if (state.finished) Danger else Cool, trackColor = Surface2, strokeWidth = 5.dp)
            Text(TimerMath.format(remaining), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleLarge, color = if (state.finished) Danger else Ink)
        }
        }
        Text(stringResource(R.string.timer_plan, spec.minutes))
        if (state.finished) { Text(stringResource(R.string.time_up), color = Danger); state.finishedAt?.let { Text(stringResource(R.string.timer_finished_at, TimeFormat.format(it))) } }
        Action(stringResource(if (state.running) R.string.pause else if (state.firstStartedAt != null) R.string.resume else R.string.launch), !busy && !state.finished) { vm.timer(phase.id, if (state.running) "pause" else "start") }
        Action(stringResource(R.string.reset), !busy) {
            if (state.firstStartedAt != null || state.finished) reset = true else vm.timer(phase.id, "reset")
        }
        if (!allowed) { Text(stringResource(R.string.notifications_off), color = Amber); Action(stringResource(R.string.allow), onClick = allow) }
    }
    if (reset) Confirm(stringResource(R.string.reset_timer_question), { reset = false }, { vm.timer(phase.id, "reset"); reset = false })
}
@Composable
fun RunningTimers(data: StopAlgorithm, stored: StoredState, now: Long, onPhase: (Int) -> Unit) {
    stored.timers.filter { it.running }.forEach { timer ->
        val phase = data.phases.firstOrNull { it.id == timer.phaseId } ?: return@forEach
        val spec = phase.timer ?: return@forEach
        TextButton(shape = MaterialTheme.shapes.small, onClick = { onPhase(phase.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.timer_link, phase.id, TimerMath.format(TimerMath.remaining(timer, TimerMath.duration(spec.minutes, stored.settings.testMode), now))), fontFamily = FontFamily.Monospace)
        }
    }
}
