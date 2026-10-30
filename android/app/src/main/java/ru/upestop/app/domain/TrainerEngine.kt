package ru.upestop.app.domain

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt
import ru.upestop.app.data.model.*

@Serializable
data class Answer(val questionId: String, val selectedIndex: Int, val correct: Boolean)
data class QuestionSet(val key: String, val phase: Phase?, val questions: List<TrainerQuestion>)
object TrainerEngine {
    fun sets(data: StopAlgorithm): List<QuestionSet> = buildList {
        if (data.trainerQuestions.isNotEmpty()) add(QuestionSet("all", null, data.trainerQuestions))
        data.phases.forEach { phase ->
            val questions = data.trainerQuestions.filter { it.phaseId == phase.id }
            if (questions.isNotEmpty()) add(QuestionSet("phase:${phase.id}", phase, questions))
        }
        val rules = data.trainerQuestions.filter { it.phaseId == null }
        if (rules.isNotEmpty()) add(QuestionSet("rules", null, rules))
    }
    fun answer(question: TrainerQuestion, index: Int): Answer {
        require(index in question.options.indices)
        return Answer(question.id, index, index == question.correct)
    }
    fun score(answers: List<Answer>) = answers.count { it.correct }
    fun percent(score: Int, total: Int) = if (total == 0) 0 else (score * 100.0 / total).roundToInt()
    fun errors(questions: List<TrainerQuestion>, answers: List<Answer>): List<TrainerQuestion> =
        questions.filter { question -> answers.any { it.questionId == question.id && !it.correct } }
}
