package ru.upestop.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.map
import ru.upestop.app.data.db.*

class ProgressRepository(private val database: AppDatabase) {
    private val dao = database.stopDao()
    val checks = dao.checks().map { list -> list.map { it.domain() } }
    suspend fun set(id: String, checked: Boolean, now: Long) = dao.check(CheckEntity(id, checked, if (checked) now else null))
    suspend fun clearStop() = database.withTransaction { dao.clearChecks(); dao.clearTimers() }
}
