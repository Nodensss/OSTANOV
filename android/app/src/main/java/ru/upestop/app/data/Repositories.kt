package ru.upestop.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import ru.upestop.app.data.db.CheckDao
import ru.upestop.app.data.db.CheckStateEntity
import ru.upestop.app.data.db.TimerDao
import ru.upestop.app.data.db.TimerStateEntity
import ru.upestop.app.data.db.TrainerAttemptDao
import ru.upestop.app.data.db.TrainerAttemptEntity
import ru.upestop.app.domain.TimerState
import ru.upestop.app.domain.TrainerAnswer
import ru.upestop.app.domain.TrainerAttempt

class ProgressRepository(private val dao: CheckDao) {

    /** Отмеченные операции: ID операции → время отметки. */
    val checked: Flow<Map<String, Long>> = dao.observeAll().map { rows ->
        rows.mapNotNull { row -> row.checkedAt?.takeIf { row.checked }?.let { row.checkId to it } }.toMap()
    }

    suspend fun setChecked(checkId: String, checked: Boolean, now: Long) =
        dao.upsert(CheckStateEntity(checkId, checked, if (checked) now else null))

    suspend fun clear() = dao.deleteAll()
}

class TimerRepository(private val dao: TimerDao) {

    val timers: Flow<Map<Int, TimerState>> = dao.observeAll().map { rows ->
        rows.associate { it.phaseId to it.toDomain() }
    }

    suspend fun get(phaseId: Int): TimerState = dao.get(phaseId)?.toDomain() ?: TimerState(phaseId)

    suspend fun getAll(): List<TimerState> = dao.getAll().map { it.toDomain() }

    /** Сброшенный таймер не хранится: строка удаляется. */
    suspend fun save(state: TimerState) {
        if (state == TimerState(state.phaseId)) dao.delete(state.phaseId) else dao.upsert(state.toEntity())
    }

    suspend fun clear() = dao.deleteAll()

    private fun TimerStateEntity.toDomain() =
        TimerState(phaseId, startedAt, elapsedMs, durationMs, finished, firstStartedAt, finishedAt)

    private fun TimerState.toEntity() =
        TimerStateEntity(phaseId, startedAt, elapsedMs, durationMs, finished, firstStartedAt, finishedAt)
}

class TrainerRepository(private val dao: TrainerAttemptDao) {
    private val answersSerializer = ListSerializer(TrainerAnswer.serializer())
    private val json = Json { ignoreUnknownKeys = true }

    val attempts: Flow<List<TrainerAttempt>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun attempt(id: String): Flow<TrainerAttempt?> = dao.observe(id).map { it?.toDomain() }

    suspend fun save(attempt: TrainerAttempt) = dao.upsert(
        TrainerAttemptEntity(
            id = attempt.id,
            startedAt = attempt.startedAt,
            finishedAt = attempt.finishedAt,
            setKey = attempt.setKey,
            setLabel = attempt.setLabel,
            total = attempt.total,
            score = attempt.score,
            answersJson = json.encodeToString(answersSerializer, attempt.answers),
        ),
    )

    suspend fun clear() = dao.deleteAll()

    private fun TrainerAttemptEntity.toDomain() = TrainerAttempt(
        id = id,
        startedAt = startedAt,
        finishedAt = finishedAt,
        setKey = setKey,
        setLabel = setLabel,
        total = total,
        score = score,
        answers = runCatching { json.decodeFromString(answersSerializer, answersJson) }.getOrDefault(emptyList()),
    )
}
