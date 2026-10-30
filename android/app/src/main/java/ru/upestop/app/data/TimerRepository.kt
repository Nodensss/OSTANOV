package ru.upestop.app.data

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.upestop.app.data.db.*
import ru.upestop.app.domain.*
import ru.upestop.app.timer.*

class TimerRepository(private val dao: StopDao, private val algorithm: AlgorithmRepository,
    private val settings: SettingsRepository, val scheduler: TimerScheduler, val notifications: TimerNotifications,
    private val progress: ProgressRepository, private val foreground: () -> Boolean) {
    val timers = dao.timers().map { list -> list.map { it.domain() } }
    // All alarm transitions, resets and duration-mode changes share this lock.
    private val mutex = Mutex()
    suspend fun action(id: Int, action: String, now: Long = System.currentTimeMillis()) = mutex.withLock {
        val phase = algorithm.load().phases.first { it.id == id && it.timer != null }
        val prefs = settings.settings.first()
        val duration = TimerMath.duration(phase.timer!!.minutes, prefs.testMode)
        val state = timers.first().find { it.phaseId == id } ?: TimerState(id)
        if (action != "reset" && state.running && TimerMath.remaining(state, duration, now) == 0L) {
            complete(phase, state, duration, now, prefs)
        } else {
            val next = when (action) {
                "start" -> TimerMath.start(state, now)
                "pause" -> TimerMath.pause(state, duration, now)
                "reset" -> TimerMath.reset(state)
                else -> state
            }
            dao.timer(TimerEntity.from(next))
            scheduler.cancel(id); notifications.cancel(id)
            if (next.running) { scheduler.schedule(next, duration, now); notifications.running(phase, next, duration, now) }
        }
    }
    private suspend fun complete(phase: ru.upestop.app.data.model.Phase, state: TimerState, duration: Long, now: Long, prefs: Settings) {
        // Use the deadline as completion time when delivery was delayed by reboot or Doze.
        val endAt = state.startedAt?.let { it + (duration - state.elapsedMs).coerceAtLeast(0) } ?: now
        dao.timer(TimerEntity.from(TimerMath.finish(state, duration, endAt)))
        scheduler.cancel(phase.id); notifications.cancel(phase.id)
        notifications.finished(phase, prefs, foreground())
    }
    suspend fun restore(now: Long = System.currentTimeMillis()) = mutex.withLock {
        val data = algorithm.load()
        val prefs = settings.settings.first()
        timers.first().filter { it.running }.forEach { state ->
            val phase = data.phases.firstOrNull { it.id == state.phaseId && it.timer != null }
            if (phase == null) {
                scheduler.cancel(state.phaseId); notifications.cancel(state.phaseId)
                dao.timer(TimerEntity.from(TimerMath.reset(state)))
                return@forEach
            }
            val duration = TimerMath.duration(phase.timer!!.minutes, prefs.testMode)
            if (TimerMath.remaining(state, duration, now) == 0L) complete(phase, state, duration, now, prefs)
            else { scheduler.schedule(state, duration, now); notifications.running(phase, state, duration, now) }
        }
    }
    suspend fun resetStop() = mutex.withLock {
        val ids = (timers.first().map { it.phaseId } + algorithm.load().phases.filter { it.timer != null }.map { it.id }).distinct()
        progress.clearStop()
        ids.forEach { scheduler.cancel(it); notifications.cancel(it) }
    }
    suspend fun setTestMode(value: Boolean) = mutex.withLock {
        check(timers.first().none { it.running })
        settings.set(Setting.TEST_MODE, value)
    }
}
