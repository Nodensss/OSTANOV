package ru.upestop.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.upestop.app.R
import ru.upestop.app.data.model.Phase
import ru.upestop.app.domain.TimeFormat
import ru.upestop.app.domain.TimerMath
import ru.upestop.app.domain.TimerState
import ru.upestop.app.ui.theme.UpeColors
import ru.upestop.app.ui.theme.UpeText
import java.time.ZoneId

class TimerActions(
    val onStart: () -> Unit,
    val onPause: () -> Unit,
    val onReset: () -> Unit,
    val onExpired: () -> Unit,
)

/** Таймер этапа: кольцо прогресса, обратный отсчёт, запуск/пауза/сброс (логика — components/ui/Timer.tsx). */
@Composable
fun TimerPanel(
    phase: Phase,
    state: TimerState,
    testMode: Boolean,
    actions: TimerActions,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val spec = phase.timer ?: return
    val now = rememberNow()
    val duration = TimerMath.effectiveDuration(state, spec.minutes, testMode)
    val remaining = TimerMath.remaining(state, duration, now)
    val progress = if (duration > 0) TimerMath.elapsed(state, duration, now).toFloat() / duration else 0f
    val expired = TimerMath.isExpired(state, duration, now)
    val notifications = rememberNotificationAccess()
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(expired) { if (expired) actions.onExpired() }

    val pulse = if (state.finished) finishedPulse() else 1f
    val accent = if (state.finished) UpeColors.Danger else UpeColors.Cool

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.08f), RectangleShape)
            .border(1.dp, if (state.finished) UpeColors.Danger.copy(alpha = pulse) else accent.copy(alpha = 0.4f), RectangleShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionLabel(spec.label, color = accent)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            TimerRing(
                progress = progress,
                text = TimerMath.formatRemaining(remaining),
                finished = state.finished,
                size = if (compact) 76 else 96,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.finished) {
                    Text(
                        stringResource(R.string.timer_finished, state.finishedAt?.let { TimeFormat.time(it, ZoneId.systemDefault()) }.orEmpty()),
                        color = UpeColors.Danger,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
                    val (label, icon, onClick) = when {
                        state.running -> Triple(R.string.timer_pause, Icons.Filled.Pause, actions.onPause)
                        state.paused -> Triple(R.string.timer_resume, Icons.Filled.PlayArrow, actions.onStart)
                        else -> Triple(R.string.timer_start, Icons.Filled.PlayArrow, actions.onStart)
                    }
                    Button(
                        onClick = onClick,
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = UpeColors.Cool, contentColor = UpeColors.Bg),
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(label), modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.SemiBold)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.timer_plan, spec.minutes),
                        style = UpeText.mono.copy(fontSize = 12.sp),
                        color = UpeColors.TextMute,
                        modifier = Modifier.weight(1f),
                    )
                    if (!state.idle) {
                        IconButton(onClick = { if (state.running || state.finished) confirmReset = true else actions.onReset() }) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = stringResource(R.string.timer_reset), tint = UpeColors.TextDim)
                        }
                    }
                }
            }
        }
        if (testMode) {
            Text(stringResource(R.string.timer_test_mode), style = UpeText.mono.copy(fontSize = 11.sp), color = UpeColors.Danger)
        }
        if (!notifications.enabled && !state.finished) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(UpeColors.Warn.copy(alpha = 0.08f), RectangleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.NotificationsOff, contentDescription = null, tint = UpeColors.Warn, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.timer_notifications_off), color = UpeColors.TextDim, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = notifications.request) { Text(stringResource(R.string.action_allow)) }
            }
        }
    }

    if (confirmReset) {
        ConfirmDialog(
            title = stringResource(R.string.timer_reset_title),
            text = stringResource(R.string.timer_reset_text, phase.id),
            confirmLabel = stringResource(R.string.timer_reset),
            onConfirm = {
                confirmReset = false
                actions.onReset()
            },
            onDismiss = { confirmReset = false },
        )
    }
}

/** Пульсация рамки завершённого таймера. Анимация создаётся только для завершённого таймера. */
@Composable
private fun finishedPulse(): Float {
    val pulse by rememberInfiniteTransition(label = "finished").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "finishedPulse",
    )
    return pulse
}

@Composable
private fun TimerRing(progress: Float, text: String, finished: Boolean, size: Int) {
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size.dp)) {
            val stroke = 6.dp.toPx()
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(UpeColors.Surface2, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            drawArc(
                if (finished) UpeColors.Danger else UpeColors.Cool,
                -90f,
                360f * progress.coerceIn(0f, 1f),
                false,
                topLeft,
                arcSize,
                style = Stroke(stroke),
            )
        }
        Text(
            text = text,
            style = UpeText.mono.copy(fontSize = if (text.length > 5) 14.sp else 18.sp, fontWeight = FontWeight.SemiBold),
            color = if (finished) UpeColors.Danger else UpeColors.Text,
        )
    }
}
