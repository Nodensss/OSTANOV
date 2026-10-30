package ru.upestop.app.domain

import ru.upestop.app.data.model.*

data class CheckState(val checkId: String, val checked: Boolean, val checkedAt: Long? = null)
data class Operation(val phase: Phase, val check: CheckItem)
class Progress(val algorithm: StopAlgorithm, checks: List<CheckState>) {
    val operations = algorithm.phases.flatMap { phase -> phase.checks.map { Operation(phase, it) } }
    private val knownIds = operations.map { it.check.id }.toSet()
    val checked = checks.filter { it.checked && it.checkId in knownIds }.associateBy { it.checkId }
    val count get() = checked.size
    val total get() = operations.size
    val firstIndex get() = operations.indexOfFirst { it.check.id !in checked }
    val currentPhase get() = operations.getOrNull(firstIndex)?.phase
    fun phaseDone(phase: Phase) = phase.checks.isNotEmpty() && phase.checks.all { it.id in checked }
    fun nextUnchecked(after: Int): Int = ((after + 1 until operations.size) + (0..after.coerceAtMost(operations.lastIndex)))
        .firstOrNull { operations[it].check.id !in checked } ?: -1
}
