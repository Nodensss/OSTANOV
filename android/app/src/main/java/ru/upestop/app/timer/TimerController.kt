package ru.upestop.app.timer

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.upestop.app.data.AlgorithmRepository
import ru.upestop.app.data.SettingsRepository
import ru.upestop.app.data.TimerRepository
import ru.upestop.app.data.model.Phase
import ru.upestop.app.domain.TimerMath
import ru.upestop.app.domain.TimerState
import ru.upestop.app.domain.phase

/**
 * Единая точка изменения таймеров: состояние в БД, будильник и уведомления
 * всегда меняются вместе (из интерфейса, из уведомления, по будильнику, после перезагрузки).
 */
class TimerController(
    private val algorithmRepository: AlgorithmRepository,
    private val timers: TimerRepository,
    private val settings: SettingsRepository,
    private val scheduler: TimerAlarmScheduler,
    private val notifications: TimerNotifications,
) {
    private val mutex = Mutex()

    suspend fun start(phaseId: Int) = mutate(phaseId) { phase, state, now ->
        val minutes = phase.timer?.minutes ?: return@mutate state
        val testMode = settings.settings.first().timerTestMode
        TimerMath.start(state, TimerMath.effectiveDuration(state, minutes, testMode), now)
    }

    suspend fun pause(phaseId: Int) = mutate(phaseId) { _, state, now -> TimerMath.pause(state, now) }

    suspend fun reset(phaseId: Int) = mutate(phaseId) { _, state, _ -> TimerMath.reset(state) }

    /** Завершает таймер, если его время вышло. Вызывается будильником, интерфейсом и при старте. */
    suspend fun finishIfExpired(phaseId: Int) = mutate(phaseId) { phase, state, now ->
        val duration = durationOf(phase, state)
        if (TimerMath.isExpired(state, duration, now)) TimerMath.finish(state, duration, now) else state
    }

    /** После запуска приложения и перезагрузки: завершить истёкшие, остальным вернуть будильник и уведомление. */
    suspend fun reconcile() {
        timers.getAll().filter { it.running }.forEach { finishIfExpired(it.phaseId) }
    }

    suspend fun resetAll() = mutex.withLock {
        timers.getAll().forEach { state ->
            scheduler.cancel(state.phaseId)
            notifications.cancelRunning(state.phaseId)
            notifications.cancelFinished(state.phaseId)
        }
        timers.clear()
    }

    private suspend fun mutate(phaseId: Int, transform: suspend (Phase, TimerState, Long) -> TimerState) =
        mutex.withLock {
            val phase = algorithmRepository.require()?.phase(phaseId)?.takeIf { it.timer != null }
                ?: return@withLock
            val before = timers.get(phaseId)
            val after = transform(phase, before, System.currentTimeMillis())
            if (after != before) timers.save(after)
            sync(phase, before, after)
        }

    private fun sync(phase: Phase, before: TimerState, after: TimerState) {
        val endsAt = TimerMath.endsAt(after, durationOf(phase, after))
        if (endsAt != null) {
            scheduler.schedule(phase.id, endsAt)
            notifications.showRunning(phase, endsAt)
        } else {
            scheduler.cancel(phase.id)
            notifications.cancelRunning(phase.id)
        }
        when {
            after.finished && !before.finished -> notifications.showFinished(phase)
            !after.finished && before.finished -> notifications.cancelFinished(phase.id)
        }
    }

    private fun durationOf(phase: Phase, state: TimerState): Long =
        state.durationMs ?: TimerMath.durationMs(phase.timer?.minutes ?: 0, testMode = false)
}
