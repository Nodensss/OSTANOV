package ru.upestop.app.domain

// Epoch milliseconds are persisted so timers survive process death and reboot.
data class TimerState(val phaseId: Int, val startedAt: Long? = null, val elapsedMs: Long = 0,
    val finished: Boolean = false, val firstStartedAt: Long? = null, val finishedAt: Long? = null) {
    val running: Boolean get() = startedAt != null && !finished
}

object TimerMath {
    fun duration(minutes: Int, testMode: Boolean = false): Long = minutes.toLong() * if (testMode) 1_000L else 60_000L
    fun elapsed(state: TimerState, duration: Long, now: Long): Long =
        (state.elapsedMs + if (state.running) (now - state.startedAt!!).coerceAtLeast(0) else 0).coerceIn(0, duration)
    fun remaining(state: TimerState, duration: Long, now: Long): Long = duration - elapsed(state, duration, now)
    fun start(state: TimerState, now: Long): TimerState {
        require(!state.finished) { "Завершённый таймер нужно сбросить" }
        return if (state.running) state else state.copy(startedAt = now, firstStartedAt = state.firstStartedAt ?: now)
    }
    fun pause(state: TimerState, duration: Long, now: Long): TimerState =
        state.copy(elapsedMs = elapsed(state, duration, now), startedAt = null)
    fun reset(state: TimerState) = TimerState(state.phaseId)
    fun finish(state: TimerState, duration: Long, now: Long) = state.copy(finished = true, elapsedMs = duration, startedAt = null, finishedAt = now)
    fun format(ms: Long): String {
        val seconds = (ms.coerceAtLeast(0) + 999) / 1000
        return if (seconds >= 3600) "%d:%02d:%02d".format(java.util.Locale.ROOT, seconds / 3600, seconds / 60 % 60, seconds % 60)
        else "%02d:%02d".format(java.util.Locale.ROOT, seconds / 60, seconds % 60)
    }
}
