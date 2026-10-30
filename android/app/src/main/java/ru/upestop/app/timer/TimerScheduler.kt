package ru.upestop.app.timer

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import ru.upestop.app.domain.*

class TimerScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    fun exactAllowed() = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()
    private fun pending(id: Int) = PendingIntent.getBroadcast(context, id,
        Intent(context, TimerReceiver::class.java).setAction(ACTION_FINISH).putExtra(EXTRA_PHASE, id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun schedule(state: TimerState, duration: Long, now: Long) {
        val at = now + TimerMath.remaining(state, duration, now)
        val intent = pending(state.phaseId)
        try {
            if (exactAllowed()) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        } catch (_: SecurityException) {
            // Permission may be revoked between canScheduleExactAlarms() and setExact.
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
    }
    fun cancel(id: Int) = alarms.cancel(pending(id))
    companion object {
        const val ACTION_FINISH = "ru.upestop.app.FINISH_TIMER"
        const val ACTION_PAUSE = "ru.upestop.app.PAUSE_TIMER"
        const val EXTRA_PHASE = "phase_id"
    }
}
