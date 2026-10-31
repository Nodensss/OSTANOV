package ru.upestop.app.domain

import java.util.Locale

/**
 * Состояние таймера этапа. Логика повторяет components/ui/Timer.tsx веб-версии:
 * [startedAt] — момент последнего запуска (null на паузе), [elapsedMs] — время до последней паузы.
 * [durationMs] фиксируется при первом запуске, чтобы смена тестового режима не влияла на идущий таймер.
 */
data class TimerState(
    val phaseId: Int,
    val startedAt: Long? = null,
    val elapsedMs: Long = 0,
    val durationMs: Long? = null,
    val finished: Boolean = false,
    val firstStartedAt: Long? = null,
    val finishedAt: Long? = null,
) {
    val running: Boolean get() = startedAt != null && !finished
    val paused: Boolean get() = startedAt == null && !finished && elapsedMs > 0
    val idle: Boolean get() = startedAt == null && !finished && elapsedMs == 0L
}

object TimerMath {
    const val MINUTE_MS = 60_000L

    /** В тестовом режиме (только debug-сборка) минута таймера длится секунду. */
    const val TEST_MODE_MINUTE_MS = 1_000L

    fun durationMs(minutes: Int, testMode: Boolean): Long =
        minutes * if (testMode) TEST_MODE_MINUTE_MS else MINUTE_MS

    /** Длительность, зафиксированная при запуске, иначе — по текущим настройкам. */
    fun effectiveDuration(state: TimerState, minutes: Int, testMode: Boolean): Long =
        state.durationMs ?: durationMs(minutes, testMode)

    fun elapsed(state: TimerState, durationMs: Long, now: Long): Long {
        val sinceStart = state.startedAt?.takeIf { !state.finished }?.let { (now - it).coerceAtLeast(0) } ?: 0
        return (state.elapsedMs + sinceStart).coerceIn(0, durationMs)
    }

    fun remaining(state: TimerState, durationMs: Long, now: Long): Long =
        durationMs - elapsed(state, durationMs, now)

    /** Момент окончания идущего таймера; null, если таймер не идёт. */
    fun endsAt(state: TimerState, durationMs: Long): Long? {
        val startedAt = state.startedAt ?: return null
        if (state.finished) return null
        return startedAt + (durationMs - state.elapsedMs).coerceAtLeast(0)
    }

    fun isExpired(state: TimerState, durationMs: Long, now: Long): Boolean =
        state.running && remaining(state, durationMs, now) == 0L

    /** Запуск или продолжение. Завершённый таймер сначала нужно сбросить. */
    fun start(state: TimerState, durationMs: Long, now: Long): TimerState {
        if (state.finished || state.running) return state
        return state.copy(
            startedAt = now,
            durationMs = state.durationMs ?: durationMs,
            firstStartedAt = state.firstStartedAt ?: now,
        )
    }

    fun pause(state: TimerState, now: Long): TimerState {
        val startedAt = state.startedAt ?: return state
        if (state.finished) return state
        val elapsed = state.elapsedMs + (now - startedAt).coerceAtLeast(0)
        return state.copy(startedAt = null, elapsedMs = elapsed.coerceAtMost(state.durationMs ?: Long.MAX_VALUE))
    }

    fun reset(state: TimerState): TimerState = TimerState(state.phaseId)

    fun finish(state: TimerState, durationMs: Long, now: Long): TimerState =
        state.copy(
            startedAt = null,
            elapsedMs = durationMs,
            durationMs = state.durationMs ?: durationMs,
            finished = true,
            finishedAt = now,
        )

    /** Оставшееся время: MM:SS до часа, H:MM:SS от часа. Секунды округляются вверх, как в вебе. */
    fun formatRemaining(ms: Long): String {
        val totalSeconds = (ms.coerceAtLeast(0) + 999) / 1000
        val hours = totalSeconds / 3600
        val minutes = totalSeconds % 3600 / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
        }
    }

    /** Прошедшее время для журнала: всегда H:MM:SS. */
    fun formatElapsed(ms: Long): String {
        val totalSeconds = ms.coerceAtLeast(0) / 1000
        return String.format(
            Locale.ROOT,
            "%d:%02d:%02d",
            totalSeconds / 3600,
            totalSeconds % 3600 / 60,
            totalSeconds % 60,
        )
    }
}
