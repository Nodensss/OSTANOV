package ru.upestop.app

import org.junit.Assert.*
import org.junit.Test
import ru.upestop.app.data.model.*
import ru.upestop.app.domain.AlgorithmValidator

class AlgorithmValidatorTest {
    private val check = CheckItem("1.1", "Тестовая операция", CheckType.CHECK)
    private val phase = Phase(1, "Раздел", "Тест", "", checks = listOf(check), positions = emptyList())
    private val data = StopAlgorithm("Тест", "", emptyList(), listOf(phase), emptyList(), emptyList())
    @Test fun invalidInputsExplainProblems() {
        val broken = data.copy(phases = listOf(phase.copy(checks = listOf(check, check.copy(text = "", type = CheckType.PARAM)), timer = TimerSpec(0, "Тест")), phase),
            trainerQuestions = listOf(TrainerQuestion("q", 99, "Вопрос", listOf("Ответ"), 2, "")))
        val errors = AlgorithmValidator.validate(broken).joinToString()
        listOf("checks: дублирующийся", "phases: дублирующийся", "correct", "phaseId", "timer.minutes", "пустой text", "param отсутствует").forEach { assertTrue("Missing $it in $errors", errors.contains(it)) }
    }
}
