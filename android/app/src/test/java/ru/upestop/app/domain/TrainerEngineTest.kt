package ru.upestop.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainerEngineTest {
    private val algorithm = TestData.sample()

    @Test
    fun optionsSkipPhasesWithoutQuestions() {
        val options = TrainerEngine.options(algorithm)
        assertEquals(
            listOf(QuestionSetKey.All, QuestionSetKey.ForPhase(1), QuestionSetKey.ForPhase(3), QuestionSetKey.Rules),
            options.map { it.key },
        )
        assertEquals(listOf(4, 2, 1, 1), options.map { it.count })
        assertEquals(algorithm.phases[0], options[1].phase)
    }

    @Test
    fun repositoryOptionsHaveNoEmptySets() {
        val repository = TestData.repositoryAlgorithm
        val options = TrainerEngine.options(repository)
        assertTrue(options.all { it.count > 0 })
        assertEquals(repository.trainerQuestions.size, options.first { it.key == QuestionSetKey.All }.count)
        val phasesWithoutQuestions = repository.phases.map { it.id }.filter { id -> repository.trainerQuestions.none { it.phaseId == id } }
        assertTrue(options.none { (it.key as? QuestionSetKey.ForPhase)?.phaseId in phasesWithoutQuestions })
    }

    @Test
    fun questionSets() {
        assertEquals(4, TrainerEngine.questions(algorithm, QuestionSetKey.All).size)
        assertEquals(listOf("q1", "q2"), TrainerEngine.questions(algorithm, QuestionSetKey.ForPhase(1)).map { it.id })
        assertEquals(listOf("q4"), TrainerEngine.questions(algorithm, QuestionSetKey.Rules).map { it.id })
        assertEquals(listOf("q2", "q3"), TrainerEngine.questions(algorithm, QuestionSetKey.Retry, listOf("q3", "q2")).map { it.id })
    }

    @Test
    fun scoringAndMistakes() {
        val questions = algorithm.trainerQuestions
        val answers = listOf(
            TrainerEngine.answer(questions[0], 0),
            TrainerEngine.answer(questions[1], 0),
            TrainerEngine.answer(questions[2], 0),
        )
        assertEquals(listOf(true, false, true), answers.map { it.correct })
        assertEquals(2, TrainerEngine.score(answers))
        assertEquals(67, TrainerEngine.percent(2, 3))
        assertEquals(listOf("q2"), TrainerEngine.wrongQuestionIds(answers))
    }

    @Test
    fun setKeysRoundTrip() {
        listOf(QuestionSetKey.All, QuestionSetKey.ForPhase(14), QuestionSetKey.Rules, QuestionSetKey.Retry).forEach {
            assertEquals(it, QuestionSetKey.parse(it.raw))
        }
        assertNull(QuestionSetKey.parse("phase:x"))
        assertFalse(QuestionSetKey.parse("unknown") != null)
    }
}
