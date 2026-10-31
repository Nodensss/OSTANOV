package ru.upestop.app.timer

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ru.upestop.app.MainActivity
import ru.upestop.app.R
import ru.upestop.app.data.model.Phase
import ru.upestop.app.domain.TimeFormat
import java.time.ZoneId

/**
 * Уведомления таймеров: постоянное с обратным отсчётом, пока таймер идёт,
 * и громкое (звук будильника + вибрация) по окончании.
 */
class TimerNotifications(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        val running = NotificationChannelCompat.Builder(CHANNEL_RUNNING, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(context.getString(R.string.channel_timers_running))
            .setDescription(context.getString(R.string.channel_timers_running_desc))
            .setShowBadge(false)
            .build()
        val finished = NotificationChannelCompat.Builder(CHANNEL_FINISHED, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.channel_timers_finished))
            .setDescription(context.getString(R.string.channel_timers_finished_desc))
            .setVibrationEnabled(true)
            .setVibrationPattern(longArrayOf(0, 700, 300, 700, 300, 700))
            .setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
        manager.createNotificationChannelsCompat(listOf(running, finished))
    }

    fun canNotify(): Boolean {
        if (!manager.areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission") // Разрешение проверяется в canNotify() перед показом.
    fun showRunning(phase: Phase, endsAt: Long) {
        if (!canNotify()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_RUNNING)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(context.getString(R.string.notification_timer_title, phase.id, phase.timer?.label.orEmpty()))
            .setContentText(
                context.getString(R.string.timer_running_until, TimeFormat.time(endsAt, ZoneId.systemDefault())),
            )
            .setWhen(endsAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(openPhase(phase.id))
            .addAction(R.drawable.ic_stat_pause, context.getString(R.string.timer_pause), pauseIntent(phase.id))
            .build()
        manager.notify(runningId(phase.id), notification)
    }

    @SuppressLint("MissingPermission") // Разрешение проверяется в canNotify() перед показом.
    fun showFinished(phase: Phase) {
        if (!canNotify()) return
        val text = context.getString(
            R.string.notification_timer_finished,
            phase.id,
            phase.timer?.label.orEmpty(),
            phase.timer?.minutes ?: 0,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_FINISHED)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(context.getString(R.string.notification_timer_finished_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openPhase(phase.id))
            .build()
        manager.notify(finishedId(phase.id), notification)
    }

    fun cancelRunning(phaseId: Int) = manager.cancel(runningId(phaseId))

    fun cancelFinished(phaseId: Int) = manager.cancel(finishedId(phaseId))

    private fun openPhase(phaseId: Int): PendingIntent = PendingIntent.getActivity(
        context,
        phaseId,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_OPEN_PHASE, phaseId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun pauseIntent(phaseId: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        phaseId,
        Intent(context, TimerAlarmReceiver::class.java)
            .setAction(TimerAlarmReceiver.ACTION_PAUSE)
            .putExtra(TimerAlarmReceiver.EXTRA_PHASE_ID, phaseId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val CHANNEL_RUNNING = "timers_running"
        const val CHANNEL_FINISHED = "timers_finished"

        private fun runningId(phaseId: Int) = 1_000 + phaseId
        private fun finishedId(phaseId: Int) = 2_000 + phaseId
    }
}
