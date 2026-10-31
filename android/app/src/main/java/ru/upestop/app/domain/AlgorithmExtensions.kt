package ru.upestop.app.domain

import ru.upestop.app.data.model.CheckItem
import ru.upestop.app.data.model.Phase
import ru.upestop.app.data.model.StopAlgorithm

/** Операция в сквозном списке всех операций алгоритма (порядок этапов сохраняется). */
data class FlatCheck(val index: Int, val phase: Phase, val check: CheckItem)

fun StopAlgorithm.flatChecks(): List<FlatCheck> =
    phases
        .flatMap { phase -> phase.checks.map { check -> phase to check } }
        .mapIndexed { index, (phase, check) -> FlatCheck(index, phase, check) }

/** Разделы в порядке первого появления, как в веб-версии. */
fun StopAlgorithm.sections(): List<String> = phases.map { it.section }.distinct()

fun StopAlgorithm.phase(id: Int): Phase? = phases.firstOrNull { it.id == id }

val StopAlgorithm.totalChecks: Int
    get() = phases.sumOf { it.checks.size }
