package ru.upestop.app

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import ru.upestop.app.data.AlgorithmLoad
import ru.upestop.app.data.AlgorithmRepository
import ru.upestop.app.data.ProgressRepository
import ru.upestop.app.data.SettingsRepository
import ru.upestop.app.data.StopSnapshot
import ru.upestop.app.data.TimerRepository
import ru.upestop.app.data.TrainerRepository
import ru.upestop.app.data.db.AppDatabase
import ru.upestop.app.timer.TimerAlarmScheduler
import ru.upestop.app.timer.TimerController
import ru.upestop.app.timer.TimerNotifications

class UpeStopApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.start()
    }
}

/** Ручной DI: все зависимости приложения создаются здесь один раз. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.create(appContext)

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val algorithmRepository = AlgorithmRepository(appContext.assets)
    val progressRepository = ProgressRepository(database.checkDao())
    val timerRepository = TimerRepository(database.timerDao())
    val trainerRepository = TrainerRepository(database.trainerAttemptDao())
    val settingsRepository = SettingsRepository(appContext)
    val timerNotifications = TimerNotifications(appContext)
    val alarmScheduler = TimerAlarmScheduler(appContext)
    val timerController = TimerController(
        algorithmRepository,
        timerRepository,
        settingsRepository,
        alarmScheduler,
        timerNotifications,
    )

    val stopSnapshot: Flow<StopSnapshot> = combine(
        algorithmRepository.algorithm,
        progressRepository.checked,
        timerRepository.timers,
        settingsRepository.settings,
        ::StopSnapshot,
    )

    fun start() {
        timerNotifications.createChannels()
        appScope.launch {
            if (algorithmRepository.load() is AlgorithmLoad.Ready) timerController.reconcile()
        }
    }

    /** Новая остановка: снять все отметки и сбросить таймеры. История тренажёра не трогается. */
    suspend fun resetStop() {
        progressRepository.clear()
        timerController.resetAll()
    }
}
