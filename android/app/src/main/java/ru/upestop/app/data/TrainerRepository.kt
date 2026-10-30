package ru.upestop.app.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import java.util.UUID
import ru.upestop.app.data.db.*
import ru.upestop.app.data.model.*
import ru.upestop.app.domain.*

class TrainerRepository(private val dao: StopDao) {
    val attempts = dao.attempts()
    private val mutex = Mutex()
    suspend fun start(set: QuestionSet, label: String, now: Long): AttemptEntity = mutex.withLock {
        require(set.questions.isNotEmpty())
        AttemptEntity(UUID.randomUUID().toString(), now, setKey = set.key, setLabel = label, total = set.questions.size).also { dao.attempt(it) }
    }
    fun answers(attempt: AttemptEntity): List<Answer> = AlgorithmJson.decodeFromString(attempt.answersJson)
    suspend fun answer(id: String, question: TrainerQuestion, index: Int) = mutex.withLock {
        val attempt = attempts.first().first { it.id == id }
        val answers = answers(attempt)
        if (attempt.finishedAt == null && answers.none { it.questionId == question.id })
            dao.attempt(attempt.copy(answersJson = AlgorithmJson.encodeToString(answers + TrainerEngine.answer(question, index))))
    }
    suspend fun finish(id: String, now: Long) = mutex.withLock {
        val attempt = attempts.first().first { it.id == id }
        val answers = answers(attempt)
        require(answers.size == attempt.total)
        dao.attempt(attempt.copy(finishedAt = now, score = TrainerEngine.score(answers)))
    }
    suspend fun clear() = mutex.withLock { dao.clearAttempts() }
}
