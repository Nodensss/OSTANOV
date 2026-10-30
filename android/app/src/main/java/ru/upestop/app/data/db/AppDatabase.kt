package ru.upestop.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.upestop.app.domain.*

@Entity(tableName = "check_state")
data class CheckEntity(@PrimaryKey @ColumnInfo(name = "check_id") val checkId: String,
    val checked: Boolean, @ColumnInfo(name = "checked_at") val checkedAt: Long?) {
    fun domain() = CheckState(checkId, checked, checkedAt)
}
@Entity(tableName = "timer_state")
data class TimerEntity(@PrimaryKey @ColumnInfo(name = "phase_id") val phaseId: Int,
    @ColumnInfo(name = "started_at") val startedAt: Long?, @ColumnInfo(name = "elapsed_ms") val elapsedMs: Long,
    val finished: Boolean, @ColumnInfo(name = "first_started_at") val firstStartedAt: Long?,
    @ColumnInfo(name = "finished_at") val finishedAt: Long?) {
    fun domain() = TimerState(phaseId, startedAt, elapsedMs, finished, firstStartedAt, finishedAt)
    companion object { fun from(s: TimerState) = TimerEntity(s.phaseId, s.startedAt, s.elapsedMs, s.finished, s.firstStartedAt, s.finishedAt) }
}
@Entity(tableName = "trainer_attempt")
data class AttemptEntity(@PrimaryKey val id: String, @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "finished_at") val finishedAt: Long? = null,
    @ColumnInfo(name = "set_key") val setKey: String, @ColumnInfo(name = "set_label") val setLabel: String,
    val total: Int, val score: Int? = null, @ColumnInfo(name = "answers_json") val answersJson: String = "[]")

@Dao
interface StopDao {
    @Query("SELECT * FROM check_state") fun checks(): Flow<List<CheckEntity>>
    @Upsert suspend fun check(value: CheckEntity)
    @Query("DELETE FROM check_state") suspend fun clearChecks()
    @Query("SELECT * FROM timer_state") fun timers(): Flow<List<TimerEntity>>
    @Upsert suspend fun timer(value: TimerEntity)
    @Query("DELETE FROM timer_state") suspend fun clearTimers()
    @Query("SELECT * FROM trainer_attempt ORDER BY started_at DESC") fun attempts(): Flow<List<AttemptEntity>>
    @Upsert suspend fun attempt(value: AttemptEntity)
    @Query("DELETE FROM trainer_attempt") suspend fun clearAttempts()
}
@Database(entities = [CheckEntity::class, TimerEntity::class, AttemptEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() { abstract fun stopDao(): StopDao }
