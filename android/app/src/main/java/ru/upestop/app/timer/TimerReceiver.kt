package ru.upestop.app.timer

import android.content.*
import ru.upestop.app.UpeStopApp

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = (context.applicationContext as UpeStopApp).container
        app.launch {
            try {
                if (intent.action == TimerScheduler.ACTION_PAUSE)
                    app.timers.action(intent.getIntExtra(TimerScheduler.EXTRA_PHASE, -1), "pause")
                else app.timers.restore()
            } finally { pending.finish() }
        }
    }
}
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED") return
        val pending = goAsync()
        val app = (context.applicationContext as UpeStopApp).container
        app.launch { try { app.timers.restore() } finally { pending.finish() } }
    }
}
