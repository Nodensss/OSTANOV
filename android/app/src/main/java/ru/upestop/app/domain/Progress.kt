package ru.upestop.app.domain

import ru.upestop.app.data.model.Phase
import ru.upestop.app.data.model.StopAlgorithm

/**
 * Подсчёт прогресса. [checked] — отмеченные операции: ID операции → время отметки.
 * ID, которых нет в алгоритме, нигде не учитываются.
 */
object Progress {

    fun completed(algorithm: StopAlgorithm, checked: Map<String, Long>): Int =
        algorithm.phases.sumOf { phaseCompleted(it, checked) }

    fun phaseCompleted(phase: Phase, checked: Map<String, Long>): Int =
        phase.checks.count { it.id in checked }

    fun isPhaseDone(phase: Phase, checked: Map<String, Long>): Boolean =
        phase.checks.isNotEmpty() && phase.checks.all { it.id in checked }

    fun percent(done: Int, total: Int): Int = if (total <= 0) 0 else (done * 100 + total / 2) / total

    fun firstUnchecked(flat: List<FlatCheck>, checked: Map<String, Long>): FlatCheck? =
        flat.firstOrNull { it.check.id !in checked }

    /** Этап первой неотмеченной операции; null, если отмечено всё. */
    fun currentPhase(algorithm: StopAlgorithm, checked: Map<String, Long>): Phase? =
        firstUnchecked(algorithm.flatChecks(), checked)?.phase

    /** Следующая неотмеченная после [afterIndex]; если таких нет — первая неотмеченная во всём списке. */
    fun nextUnchecked(flat: List<FlatCheck>, checked: Map<String, Long>, afterIndex: Int): FlatCheck? =
        flat.drop(afterIndex + 1).firstOrNull { it.check.id !in checked }
            ?: flat.firstOrNull { it.check.id !in checked }

    /** Время первой и последней отметки среди операций алгоритма. */
    fun markRange(algorithm: StopAlgorithm, checked: Map<String, Long>): Pair<Long, Long>? {
        val times = algorithm.phases.flatMap { it.checks }.mapNotNull { checked[it.id] }
        if (times.isEmpty()) return null
        return times.min() to times.max()
    }
}
