package ru.upestop.app

import org.junit.Assert.*
import org.junit.Test
import ru.upestop.app.domain.*

class TrainerEngineTest {
    @Test fun setsAndRetry() {
        val data = realAlgorithm()
        val sets = TrainerEngine.sets(data)
        assertEquals(data.trainerQuestions, sets.first().questions)
        data.phases.forEach { phase ->
            assertEquals(data.trainerQuestions.any { it.phaseId == phase.id }, sets.any { it.key == "phase:${phase.id}" })
        }
        assertEquals(data.trainerQuestions.filter { it.phaseId == null }, sets.first { it.key == "rules" }.questions)
        val questions = data.trainerQuestions.take(2)
        val answers = listOf(TrainerEngine.answer(questions[0], questions[0].correct), TrainerEngine.answer(questions[1], (questions[1].correct + 1) % questions[1].options.size))
        assertEquals(1, TrainerEngine.score(answers)); assertEquals(50, TrainerEngine.percent(1, 2))
        assertEquals(listOf(questions[1]), TrainerEngine.errors(questions, answers))
        assertEquals(0, TrainerEngine.percent(0, 0))
        assertEquals(13, TrainerEngine.percent(1, 8))
    }
}
