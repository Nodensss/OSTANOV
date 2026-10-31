package ru.upestop.app.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch
import ru.upestop.app.AppContainer
import ru.upestop.app.UpeStopApp

/** Будильник окончания таймера и действие «Пауза» из уведомления. */
class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val phaseId = intent.getIntExtra(EXTRA_PHASE_ID, -1)
        if (phaseId < 0) return
        val action = intent.action
        if (action != ACTION_FINISHED && action != ACTION_PAUSE) return
        runAsync(context) { container ->
            if (action == ACTION_FINISHED) {
                container.timerController.finishIfExpired(phaseId)
            } else {
                container.timerController.pause(phaseId)
            }
        }
    }

    companion object {
        const val ACTION_FINISHED = "ru.upestop.app.action.TIMER_FINISHED"
        const val ACTION_PAUSE = "ru.upestop.app.action.TIMER_PAUSE"
        const val EXTRA_PHASE_ID = "phase_id"
    }
}

/** После перезагрузки и обновления приложения будильники нужно поставить заново. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        runAsync(context) { container -> container.timerController.reconcile() }
    }
}

private fun BroadcastReceiver.runAsync(context: Context, block: suspend (AppContainer) -> Unit) {
    val container = (context.applicationContext as UpeStopApp).container
    val pending = goAsync()
    container.appScope.launch {
        try {
            block(container)
        } finally {
            pending.finish()
        }
    }
}
