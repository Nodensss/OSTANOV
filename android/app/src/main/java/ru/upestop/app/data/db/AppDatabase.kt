package ru.upestop.app.data.db

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "check_state")
data class CheckStateEntity(
    @PrimaryKey @ColumnInfo(name = "check_id") val checkId: String,
    @ColumnInfo(name = "checked") val checked: Boolean,
    @ColumnInfo(name = "checked_at") val checkedAt: Long?,
)

@Entity(tableName = "timer_state")
data class TimerStateEntity(
    @PrimaryKey @ColumnInfo(name = "phase_id") val phaseId: Int,
    @ColumnInfo(name = "started_at") val startedAt: Long?,
    @ColumnInfo(name = "elapsed_ms") val elapsedMs: Long,
    @ColumnInfo(name = "duration_ms") val durationMs: Long?,
    @ColumnInfo(name = "finished") val finished: Boolean,
    @ColumnInfo(name = "first_started_at") val firstStartedAt: Long?,
    @ColumnInfo(name = "finished_at") val finishedAt: Long?,
)

@Entity(tableName = "trainer_attempt")
data class TrainerAttemptEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "finished_at") val finishedAt: Long?,
    @ColumnInfo(name = "set_key") val setKey: String,
    @ColumnInfo(name = "set_label") val setLabel: String,
    @ColumnInfo(name = "total") val total: Int,
    @ColumnInfo(name = "score") val score: Int?,
    @ColumnInfo(name = "answers_json") val answersJson: String,
)

@Dao
interface CheckDao {
    @Query("SELECT * FROM check_state")
    fun observeAll(): Flow<List<CheckStateEntity>>

    @Upsert
    suspend fun upsert(entity: CheckStateEntity)

    @Query("DELETE FROM check_state")
    suspend fun deleteAll()
}

@Dao
interface TimerDao {
    @Query("SELECT * FROM timer_state")
    fun observeAll(): Flow<List<TimerStateEntity>>

    @Query("SELECT * FROM timer_state")
    suspend fun getAll(): List<TimerStateEntity>

    @Query("SELECT * FROM timer_state WHERE phase_id = :phaseId")
    suspend fun get(phaseId: Int): TimerStateEntity?

    @Upsert
    suspend fun upsert(entity: TimerStateEntity)

    @Query("DELETE FROM timer_state WHERE phase_id = :phaseId")
    suspend fun delete(phaseId: Int)

    @Query("DELETE FROM timer_state")
    suspend fun deleteAll()
}

@Dao
interface TrainerAttemptDao {
    @Query("SELECT * FROM trainer_attempt ORDER BY started_at DESC")
    fun observeAll(): Flow<List<TrainerAttemptEntity>>

    @Query("SELECT * FROM trainer_attempt WHERE id = :id")
    fun observe(id: String): Flow<TrainerAttemptEntity?>

    @Upsert
    suspend fun upsert(entity: TrainerAttemptEntity)

    @Query("DELETE FROM trainer_attempt")
    suspend fun deleteAll()
}

@Database(
    entities = [CheckStateEntity::class, TimerStateEntity::class, TrainerAttemptEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun checkDao(): CheckDao
    abstract fun timerDao(): TimerDao
    abstract fun trainerAttemptDao(): TrainerAttemptDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "upe-stop.db").build()
    }
}
