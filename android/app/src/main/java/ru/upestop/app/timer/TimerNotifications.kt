package ru.upestop.app.timer

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.media.*
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ru.upestop.app.*
import ru.upestop.app.R
import ru.upestop.app.data.Settings
import ru.upestop.app.data.model.Phase
import ru.upestop.app.domain.*

class TimerNotifications(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    fun allowed(): Boolean = (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) && NotificationManagerCompat.from(context).areNotificationsEnabled()
    private fun content(id: Int) = PendingIntent.getActivity(context, id,
        Intent(context, MainActivity::class.java).putExtra(TimerScheduler.EXTRA_PHASE, id)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    @android.annotation.SuppressLint("MissingPermission") // allowed() checks POST_NOTIFICATIONS immediately before notify; revocation is caught.
    private fun post(id: Int, notification: Notification) {
        if (allowed()) {
            // The permission is checked above; handle revocation while posting too.
            try { manager.notify(id, notification) } catch (_: SecurityException) { }
        }
    }
    fun running(phase: Phase, state: TimerState, duration: Long, now: Long) {
        manager.createNotificationChannel(NotificationChannel("running", context.getString(R.string.channel_running), NotificationManager.IMPORTANCE_LOW).apply { setSound(null, null) })
        val pause = PendingIntent.getBroadcast(context, phase.id,
            Intent(context, TimerReceiver::class.java).setAction(TimerScheduler.ACTION_PAUSE).putExtra(TimerScheduler.EXTRA_PHASE, phase.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        post(phase.id, NotificationCompat.Builder(context, "running")
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(context.getString(R.string.phase_title, phase.id, phase.timer!!.label))
            .setContentIntent(content(phase.id)).setOngoing(true).setOnlyAlertOnce(true)
            .setUsesChronometer(true).setChronometerCountDown(true).setWhen(now + TimerMath.remaining(state, duration, now))
            .addAction(android.R.drawable.ic_media_pause, context.getString(R.string.pause), pause).build())
    }
    fun finished(phase: Phase, settings: Settings, foreground: Boolean) {
        val sound = settings.sound && !foreground
        val vibration = settings.vibration && !foreground
        val channel = "finished_${sound}_${vibration}"
        manager.createNotificationChannel(NotificationChannel(channel, context.getString(R.string.channel_finished), NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(vibration)
            setSound(if (sound) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) else null,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        })
        post(phase.id, NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(context.getString(R.string.time_up))
            .setContentText(context.getString(R.string.timer_expired, phase.id, phase.timer!!.label, phase.timer.minutes))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.timer_expired, phase.id, phase.timer.label, phase.timer.minutes)))
            .setContentIntent(content(phase.id)).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH).build())
        if (foreground) {
            if (settings.sound) {
                val tone = ToneGenerator(AudioManager.STREAM_ALARM, 80)
                tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 900)
                Handler(Looper.getMainLooper()).postDelayed({ tone.release() }, 1000)
            }
            if (settings.vibration) {
                val vibrator = if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java).defaultVibrator else {
                    @Suppress("DEPRECATION") // VibratorManager was introduced in API 31.
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }
    fun cancel(id: Int) = manager.cancel(id)
}
