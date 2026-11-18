package com.bookverse.contacts.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.bookverse.contacts.MainActivity
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.settings.LocaleController
import java.util.Locale

class PomodoroService : Service() {

    private var toneGenerator: ToneGenerator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private var tickRunnable: Runnable? = null
    private var isWorkSession = false

    companion object {
        const val CHANNEL_ID = "pomodoro_timer_channel"
        const val NOTIFICATION_ID = 4000

        const val ACTION_START_WORK = "ACTION_START_WORK"
        const val ACTION_START_BREAK = "ACTION_START_BREAK"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"

        const val EXTRA_TIME_LEFT = "EXTRA_TIME_LEFT"
        const val EXTRA_TOTAL_TIME = "EXTRA_TOTAL_TIME"

        fun startWorkSession(context: Context, timeLeft: Int, totalTime: Int) {
            val intent = Intent(context, PomodoroService::class.java).apply {
                action = ACTION_START_WORK
                putExtra(EXTRA_TIME_LEFT, timeLeft)
                putExtra(EXTRA_TOTAL_TIME, totalTime)
            }
            context.startForegroundService(intent)
        }

        fun startBreakSession(context: Context, timeLeft: Int, totalTime: Int) {
            val intent = Intent(context, PomodoroService::class.java).apply {
                action = ACTION_START_BREAK
                putExtra(EXTRA_TIME_LEFT, timeLeft)
                putExtra(EXTRA_TOTAL_TIME, totalTime)
            }
            context.startForegroundService(intent)
        }

        fun pauseTimer(context: Context) {
            val intent = Intent(context, PomodoroService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun stopTimer(context: Context) {
            val intent = Intent(context, PomodoroService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_WORK -> {
                val timeLeft = intent.getIntExtra(EXTRA_TIME_LEFT, 0)
                isWorkSession = true
                startForeground(NOTIFICATION_ID, createNotification(timeLeft, true))
                playStartSound()
                startTickingSound()
            }

            ACTION_START_BREAK -> {
                val timeLeft = intent.getIntExtra(EXTRA_TIME_LEFT, 0)
                isWorkSession = false
                startForeground(NOTIFICATION_ID, createNotification(timeLeft, false))
                playCompleteSound()
                stopTickingSound()
            }

            ACTION_PAUSE -> {
                stopTickingSound()
            }

            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun playStartSound() {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, notification)
            ringtone?.play()
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.POMODORO_START_SOUND_FAILED)
        }
    }

    private fun playCompleteSound() {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, notification)
            ringtone?.play()
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.POMODORO_END_SOUND_FAILED)
        }
    }

    private fun startTickingSound() {
        stopTickingSound()

        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 15)

            tickRunnable = object : Runnable {
                override fun run() {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 50)
                    handler.postDelayed(this, 1000)
                }
            }
            handler.post(tickRunnable!!)
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.POMODORO_TIMER_START_FAILED)
        }
    }

    private fun stopTickingSound() {
        tickRunnable?.let { handler.removeCallbacks(it) }
        tickRunnable = null
        toneGenerator?.release()
        toneGenerator = null
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "FunPhone:PomodoroWakeLock"
        )
        wakeLock?.acquire(60 * 60 * 1000L)
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        val localizedContext = LocaleController.localizedContext(this)
        val channel = NotificationChannel(
            CHANNEL_ID,
            localizedContext.getString(R.string.pomodoro_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = localizedContext.getString(R.string.pomodoro_notification_channel_desc)
            setSound(null, null)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(timeLeft: Int, isWork: Boolean): Notification {
        val minutes = timeLeft / 60
        val seconds = timeLeft % 60
        val timeText = String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_POMODORO", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isWork) {
            getString(R.string.pomodoro_work_session)
        } else {
            getString(R.string.pomodoro_break_session)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(timeText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTickingSound()
        releaseWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
