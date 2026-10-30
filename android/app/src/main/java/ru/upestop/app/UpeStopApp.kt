package ru.upestop.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import ru.upestop.app.data.*
import ru.upestop.app.data.db.AppDatabase
import ru.upestop.app.timer.*

class UpeStopApp : Application() {
    val container by lazy { AppContainer(this) }
}
class AppContainer(private val context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val errors = MutableStateFlow<String?>(null)
    @Volatile var foreground = false
    val algorithm = AlgorithmRepository(context)
    val database = Room.databaseBuilder(context, AppDatabase::class.java, "upe-stop.db").build()
    val settings = SettingsRepository(context)
    val progress = ProgressRepository(database)
    val trainer = TrainerRepository(database.stopDao())
    val timers = TimerRepository(database.stopDao(), algorithm, settings, TimerScheduler(context), TimerNotifications(context), progress) { foreground }
    fun launch(block: suspend () -> Unit) = scope.launch {
        try { block() } catch (error: Exception) {
            if (error is CancellationException) throw error
            errors.value = context.getString(R.string.storage_error, error.localizedMessage.orEmpty())
        }
    }
}
