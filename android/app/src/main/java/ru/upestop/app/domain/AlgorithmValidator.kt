package ru.upestop.app.domain

import ru.upestop.app.data.model.*

object AlgorithmValidator {
    fun validate(data: StopAlgorithm): List<String> = buildList {
        fun duplicates(ids: List<Any>, field: String) {
            ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.forEach { add("$field: дублирующийся ID $it") }
        }
        if (data.title.isBlank()) add("title: пустой заголовок")
        if (data.phases.isEmpty()) add("phases: нет этапов")
        duplicates(data.phases.map { it.id }, "phases")
        duplicates(data.phases.flatMap { it.checks }.map { it.id }, "checks")
        duplicates(data.trainerQuestions.map { it.id }, "trainerQuestions")
        duplicates(data.criticalRules.map { it.id }, "criticalRules")
        data.phases.forEach { phase ->
            if (phase.id <= 0) add("phase ${phase.id}: ID должен быть положительным")
            if (phase.checks.isEmpty()) add("phase ${phase.id}: нет операций")
            if (phase.timer != null && phase.timer.minutes <= 0) add("phase ${phase.id}: timer.minutes должен быть > 0")
            phase.checks.forEach { check ->
                if (check.text.isBlank()) add("check ${check.id}: пустой text")
                if (check.id.isBlank()) add("check: пустой ID")
                if (check.type == CheckType.PARAM && check.param.isNullOrBlank()) add("check ${check.id}: param отсутствует")
            }
        }
        val phaseIds = data.phases.map { it.id }.toSet()
        data.trainerQuestions.forEach { question ->
            if (question.correct !in question.options.indices) add("question ${question.id}: correct вне options")
            if (question.phaseId != null && question.phaseId !in phaseIds) add("question ${question.id}: неизвестный phaseId ${question.phaseId}")
            if (question.question.isBlank()) add("question ${question.id}: пустой вопрос")
        }
    }
}
