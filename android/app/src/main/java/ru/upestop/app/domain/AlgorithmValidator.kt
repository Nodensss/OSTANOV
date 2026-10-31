package ru.upestop.app.domain

import ru.upestop.app.data.model.CheckType
import ru.upestop.app.data.model.StopAlgorithm

/** Ошибки данных. Тексты для показа формируются в UI из строковых ресурсов. */
sealed interface ValidationError {
    data object NoPhases : ValidationError
    data class DuplicatePhaseId(val phaseId: Int) : ValidationError
    data class DuplicateCheckId(val checkId: String) : ValidationError
    data class EmptyCheckText(val checkId: String) : ValidationError
    data class ParamWithoutValue(val checkId: String) : ValidationError
    data class NonPositiveTimer(val phaseId: Int) : ValidationError
    data class DuplicateRuleId(val ruleId: String) : ValidationError
    data class DuplicateQuestionId(val questionId: String) : ValidationError
    data class QuestionWithoutOptions(val questionId: String) : ValidationError
    data class CorrectOutOfRange(val questionId: String) : ValidationError
    data class UnknownQuestionPhase(val questionId: String, val phaseId: Int) : ValidationError
}

object AlgorithmValidator {

    fun validate(algorithm: StopAlgorithm): List<ValidationError> = buildList {
        if (algorithm.phases.isEmpty()) add(ValidationError.NoPhases)
        duplicates(algorithm.phases.map { it.id }).forEach { add(ValidationError.DuplicatePhaseId(it)) }

        val checks = algorithm.phases.flatMap { it.checks }
        duplicates(checks.map { it.id }).forEach { add(ValidationError.DuplicateCheckId(it)) }
        checks.filter { it.text.isBlank() }.forEach { add(ValidationError.EmptyCheckText(it.id)) }
        checks
            .filter { it.type == CheckType.PARAM && it.param.isNullOrBlank() }
            .forEach { add(ValidationError.ParamWithoutValue(it.id)) }

        algorithm.phases
            .filter { phase -> phase.timer != null && phase.timer.minutes <= 0 }
            .forEach { add(ValidationError.NonPositiveTimer(it.id)) }

        duplicates(algorithm.criticalRules.map { it.id }).forEach { add(ValidationError.DuplicateRuleId(it)) }

        val phaseIds = algorithm.phases.map { it.id }.toSet()
        duplicates(algorithm.trainerQuestions.map { it.id }).forEach { add(ValidationError.DuplicateQuestionId(it)) }
        algorithm.trainerQuestions.forEach { question ->
            when {
                question.options.isEmpty() -> add(ValidationError.QuestionWithoutOptions(question.id))
                question.correct !in question.options.indices -> add(ValidationError.CorrectOutOfRange(question.id))
            }
            if (question.phaseId != null && question.phaseId !in phaseIds) {
                add(ValidationError.UnknownQuestionPhase(question.id, question.phaseId))
            }
        }
    }

    private fun <T> duplicates(values: List<T>): List<T> =
        values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.toList()
}
