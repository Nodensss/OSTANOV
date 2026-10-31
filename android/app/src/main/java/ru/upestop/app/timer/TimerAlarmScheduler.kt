package ru.upestop.app.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/** Будильник на момент окончания таймера этапа: срабатывает и при закрытом приложении. */
class TimerAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun schedule(phaseId: Int, triggerAtMillis: Long) {
        val intent = pendingIntent(phaseId)
        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, intent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, intent)
            }
        } catch (_: SecurityException) {
            // Разрешение на точные будильники отозвали между проверкой и вызовом — ставим неточный.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, intent)
        }
    }

    fun cancel(phaseId: Int) = alarmManager.cancel(pendingIntent(phaseId))

    private fun pendingIntent(phaseId: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        phaseId,
        Intent(context, TimerAlarmReceiver::class.java)
            .setAction(TimerAlarmReceiver.ACTION_FINISHED)
            .putExtra(TimerAlarmReceiver.EXTRA_PHASE_ID, phaseId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
