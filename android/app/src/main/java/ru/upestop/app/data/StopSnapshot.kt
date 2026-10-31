package ru.upestop.app.data

import ru.upestop.app.data.model.StopAlgorithm
import ru.upestop.app.domain.FlatCheck
import ru.upestop.app.domain.Progress
import ru.upestop.app.domain.TimerState
import ru.upestop.app.domain.flatChecks

/** Всё состояние текущей остановки разом: данные алгоритма, отметки, таймеры, настройки. */
data class StopSnapshot(
    val algorithm: StopAlgorithm,
    val checked: Map<String, Long>,
    val timers: Map<Int, TimerState>,
    val settings: AppSettings,
) {
    val flat: List<FlatCheck> by lazy { algorithm.flatChecks() }
    val total: Int get() = flat.size
    val done: Int by lazy { Progress.completed(algorithm, checked) }
    val allDone: Boolean get() = total > 0 && done == total

    fun timer(phaseId: Int): TimerState = timers[phaseId] ?: TimerState(phaseId)
}
