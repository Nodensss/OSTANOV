package ru.upestop.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.upestop.app.data.model.CheckItem
import ru.upestop.app.data.model.CheckType
import ru.upestop.app.data.model.CriticalRule
import ru.upestop.app.data.model.TimerSpec
import ru.upestop.app.data.model.TrainerQuestion

class AlgorithmValidatorTest {
    private val valid = TestData.sample()

    @Test
    fun sampleIsValid() {
        assertEquals(emptyList<ValidationError>(), AlgorithmValidator.validate(valid))
    }

    @Test
    fun noPhases() {
        assertEquals(listOf(ValidationError.NoPhases), AlgorithmValidator.validate(valid.copy(phases = emptyList(), trainerQuestions = emptyList())))
    }

    @Test
    fun duplicatePhaseAndCheckIds() {
        val phases = valid.phases + valid.phases[0]
        val errors = AlgorithmValidator.validate(valid.copy(phases = phases))
        assertTrue(ValidationError.DuplicatePhaseId(1) in errors)
        assertTrue(ValidationError.DuplicateCheckId("1.1") in errors)
        assertTrue(ValidationError.DuplicateCheckId("1.2") in errors)
    }

    @Test
    fun brokenChecks() {
        val phase = valid.phases[0].copy(
            checks = listOf(CheckItem("1.1", " "), CheckItem("1.2", "Параметр", CheckType.PARAM, null)),
        )
        val errors = AlgorithmValidator.validate(valid.copy(phases = listOf(phase) + valid.phases.drop(1)))
        assertTrue(ValidationError.EmptyCheckText("1.1") in errors)
        assertTrue(ValidationError.ParamWithoutValue("1.2") in errors)
    }

    @Test
    fun nonPositiveTimer() {
        val phase = valid.phases[1].copy(timer = TimerSpec(0, "Таймер"))
        val errors = AlgorithmValidator.validate(valid.copy(phases = listOf(valid.phases[0], phase, valid.phases[2])))
        assertEquals(listOf(ValidationError.NonPositiveTimer(2)), errors)
    }

    @Test
    fun brokenQuestions() {
        val questions = listOf(
            TrainerQuestion("a", 1, "Вне диапазона", listOf("x", "y"), 2),
            TrainerQuestion("b", 99, "Нет этапа", listOf("x"), 0),
            TrainerQuestion("c", null, "Без вариантов", emptyList(), 0),
            TrainerQuestion("c", null, "Дубль", listOf("x"), 0),
        )
        val errors = AlgorithmValidator.validate(valid.copy(trainerQuestions = questions))
        assertTrue(ValidationError.CorrectOutOfRange("a") in errors)
        assertTrue(ValidationError.UnknownQuestionPhase("b", 99) in errors)
        assertTrue(ValidationError.QuestionWithoutOptions("c") in errors)
        assertTrue(ValidationError.DuplicateQuestionId("c") in errors)
    }

    @Test
    fun duplicateRuleIds() {
        val rule = CriticalRule("cr-1", "Правило", "Текст")
        val errors = AlgorithmValidator.validate(valid.copy(criticalRules = listOf(rule, rule)))
        assertEquals(listOf(ValidationError.DuplicateRuleId("cr-1")), errors)
    }
}
