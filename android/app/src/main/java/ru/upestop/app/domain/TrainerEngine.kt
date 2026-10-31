package ru.upestop.app.domain

import kotlinx.serialization.Serializable
import ru.upestop.app.data.model.Phase
import ru.upestop.app.data.model.StopAlgorithm
import ru.upestop.app.data.model.TrainerQuestion

/** Набор вопросов тренажёра. [raw] хранится в БД (колонка set_key). */
sealed interface QuestionSetKey {
    val raw: String

    data object All : QuestionSetKey {
        override val raw = "all"
    }

    data class ForPhase(val phaseId: Int) : QuestionSetKey {
        override val raw get() = "phase:$phaseId"
    }

    /** Вопросы без этапа (phaseId = null) — по критическим правилам. */
    data object Rules : QuestionSetKey {
        override val raw = "rules"
    }

    data object Retry : QuestionSetKey {
        override val raw = "retry"
    }

    companion object {
        fun parse(raw: String): QuestionSetKey? = when {
            raw == All.raw -> All
            raw == Rules.raw -> Rules
            raw == Retry.raw -> Retry
            raw.startsWith("phase:") -> raw.removePrefix("phase:").toIntOrNull()?.let(::ForPhase)
            else -> null
        }
    }
}

data class QuestionSetOption(val key: QuestionSetKey, val phase: Phase?, val count: Int)

@Serializable
data class TrainerAnswer(val questionId: String, val selectedIndex: Int, val correct: Boolean)

data class TrainerAttempt(
    val id: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val setKey: String,
    val setLabel: String,
    val total: Int,
    val score: Int?,
    val answers: List<TrainerAnswer>,
) {
    val finished: Boolean get() = finishedAt != null
}

object TrainerEngine {

    /** Наборы для выбора: все вопросы, этапы с вопросами (без пустых наборов) и вопросы без этапа. */
    fun options(algorithm: StopAlgorithm): List<QuestionSetOption> = buildList {
        val questions = algorithm.trainerQuestions
        if (questions.isNotEmpty()) add(QuestionSetOption(QuestionSetKey.All, null, questions.size))
        algorithm.phases.forEach { phase ->
            val count = questions.count { it.phaseId == phase.id }
            if (count > 0) add(QuestionSetOption(QuestionSetKey.ForPhase(phase.id), phase, count))
        }
        val withoutPhase = questions.count { it.phaseId == null }
        if (withoutPhase > 0) add(QuestionSetOption(QuestionSetKey.Rules, null, withoutPhase))
    }

    fun questions(
        algorithm: StopAlgorithm,
        key: QuestionSetKey,
        retryIds: Collection<String> = emptyList(),
    ): List<TrainerQuestion> {
        val all = algorithm.trainerQuestions
        return when (key) {
            QuestionSetKey.All -> all
            is QuestionSetKey.ForPhase -> all.filter { it.phaseId == key.phaseId }
            QuestionSetKey.Rules -> all.filter { it.phaseId == null }
            QuestionSetKey.Retry -> all.filter { it.id in retryIds }
        }
    }

    fun answer(question: TrainerQuestion, selectedIndex: Int): TrainerAnswer =
        TrainerAnswer(question.id, selectedIndex, selectedIndex == question.correct)

    fun score(answers: List<TrainerAnswer>): Int = answers.count { it.correct }

    fun percent(score: Int, total: Int): Int = Progress.percent(score, total)

    fun wrongQuestionIds(answers: List<TrainerAnswer>): List<String> =
        answers.filterNot { it.correct }.map { it.questionId }
}
