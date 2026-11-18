package com.bookverse.contacts.service

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import com.bookverse.contacts.data.notes.Note
import com.bookverse.contacts.data.notes.ReminderData
import com.bookverse.contacts.data.notes.ReminderType
import com.bookverse.contacts.data.notes.nextOccurrence
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.feature.notes.R
import com.bookverse.contacts.settings.LocaleController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal enum class ReminderDeliveryAction {
    CANCEL,
    SHOW_PERIODIC,
    SHOW_ALARM,
    SHOW_ADVANCE,
    IGNORE,
}

internal fun resolveReminderDeliveryAction(
    notificationType: String,
    currentTime: Long,
    expirationDate: Long,
): ReminderDeliveryAction {
    return when (notificationType) {
        "periodic" -> {
            if (currentTime >= expirationDate) {
                ReminderDeliveryAction.CANCEL
            } else {
                ReminderDeliveryAction.SHOW_PERIODIC
            }
        }
        "alarm" -> ReminderDeliveryAction.SHOW_ALARM
        "advance" -> {
            if (currentTime >= expirationDate) {
                ReminderDeliveryAction.CANCEL
            } else {
                ReminderDeliveryAction.SHOW_ADVANCE
            }
        }
        else -> ReminderDeliveryAction.IGNORE
    }
}

internal fun shouldDeliverStoredReminder(
    noteExists: Boolean,
    isReminder: Boolean,
    isDeleted: Boolean,
    hasReminderData: Boolean,
): Boolean = noteExists && isReminder && !isDeleted && hasReminderData

internal fun resolveNextPeriodicNotificationTime(
    currentTime: Long,
    intervalSeconds: Int,
    expirationDate: Long,
    anchorTime: Long = currentTime,
): Long? {
    if (intervalSeconds <= 0 || currentTime >= expirationDate) return null

    val intervalMillis = intervalSeconds.toLong() * 1_000L
    val elapsedSinceAnchor = (currentTime - anchorTime).coerceAtLeast(0L)
    val completedIntervals = elapsedSinceAnchor / intervalMillis
    val nextNotificationTime = try {
        Math.addExact(
            anchorTime,
            Math.multiplyExact(completedIntervals + 1L, intervalMillis),
        )
    } catch (_: ArithmeticException) {
        return null
    }
    return nextNotificationTime.takeIf {
        it > currentTime && it <= expirationDate
    }
}

internal fun resolveAdvanceNotificationTime(
    occurrence: Long,
    advanceMinutes: Int,
    currentTime: Long,
): Long? {
    if (advanceMinutes <= 0) return null
    val advanceTime = occurrence - advanceMinutes * 60_000L
    return advanceTime.takeIf { it > currentTime }
}

internal enum class AlarmSchedulingMode {
    EXACT,
    UNAVAILABLE,
}

internal fun resolveAlarmSchedulingMode(
    requiresExactAlarmPermission: Boolean,
    canScheduleExactAlarms: Boolean,
): AlarmSchedulingMode =
    if (!requiresExactAlarmPermission || canScheduleExactAlarms) {
        AlarmSchedulingMode.EXACT
    } else {
        AlarmSchedulingMode.UNAVAILABLE
    }

internal data class ReminderChannelAvailability(
    val canPost: Boolean,
    val isAudible: Boolean,
)

internal fun resolveReminderChannelAvailability(
    appNotificationsEnabled: Boolean,
    notificationPermissionGranted: Boolean,
    channelExists: Boolean,
    channelEnabled: Boolean,
    channelHasSound: Boolean,
): ReminderChannelAvailability {
    val canPost = appNotificationsEnabled &&
        notificationPermissionGranted &&
        channelExists &&
        channelEnabled
    return ReminderChannelAvailability(
        canPost = canPost,
        isAudible = canPost && channelHasSound,
    )
}

class NotificationService {
    companion object {
        const val PERIODIC_CHANNEL_ID = "periodic_reminder_channel"
        const val ALARM_CHANNEL_ID = "alarm_reminder_channel"
        const val ADVANCE_CHANNEL_ID = "advance_notification_channel"
        const val NOTIFICATION_ID_PREFIX = 1000
        const val REQUEST_CODE_PREFIX = 2000
        const val ADVANCE_REQUEST_CODE_PREFIX = 3000

        fun canScheduleExactAlarms(context: Context): Boolean = ExactAlarmAccess.canScheduleExactAlarms(context)

        fun openAlarmPermissionSettings(context: Context) {
            createAlarmPermissionIntent(context)?.let { intent ->
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }

        fun createAlarmPermissionIntent(context: Context): Intent? = ExactAlarmAccess.createAlarmPermissionIntent(context)

        fun createNotificationSettingsIntent(context: Context): Intent =
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                data = "package:${context.packageName}".toUri()
            }

        fun isReminderChannelEnabled(context: Context, channelId: String): Boolean {
            return reminderChannelAvailability(context, channelId).canPost
        }

        fun isReminderChannelAudible(context: Context, channelId: String): Boolean {
            val availability = reminderChannelAvailability(context, channelId)
            if (availability.canPost && !availability.isAudible) {
                DiagnosticLog.warning(DiagnosticEvent.REMINDER_CHANNEL_SOUND_DISABLED)
            }
            return availability.isAudible
        }

        private fun reminderChannelAvailability(
            context: Context,
            channelId: String,
        ): ReminderChannelAvailability {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = notificationManager.getNotificationChannel(channelId)
            return resolveReminderChannelAvailability(
                appNotificationsEnabled = notificationManager.areNotificationsEnabled(),
                notificationPermissionGranted =
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED,
                channelExists = channel != null,
                channelEnabled = channel?.importance != NotificationManager.IMPORTANCE_NONE,
                channelHasSound = channel?.sound != null,
            )
        }

        fun areReminderNotificationsEnabled(context: Context): Boolean {
            createNotificationChannels(context)
            return listOf(PERIODIC_CHANNEL_ID, ALARM_CHANNEL_ID, ADVANCE_CHANNEL_ID)
                .all { channelId -> isReminderChannelEnabled(context, channelId) }
        }

        fun areReminderChannelsAudible(context: Context): Boolean {
            createNotificationChannels(context)
            return listOf(PERIODIC_CHANNEL_ID, ALARM_CHANNEL_ID, ADVANCE_CHANNEL_ID)
                .all { channelId -> isReminderChannelAudible(context, channelId) }
        }

        @SuppressLint("ScheduleExactAlarm")
        fun scheduleNotification(context: Context, note: Note, reminderData: ReminderData) {
            cancelNotification(context, note.id)
            createNotificationChannels(context)

            val currentTime = System.currentTimeMillis()
            val expirationDate = reminderData.date

            when (reminderData.type) {
                ReminderType.PERIODIC -> {
                    if (currentTime >= expirationDate) return
                    resolveNextPeriodicNotificationTime(
                        currentTime = currentTime,
                        intervalSeconds = reminderData.interval,
                        expirationDate = expirationDate,
                        anchorTime = reminderData.creationDate,
                    )?.let { nextNotificationTime ->
                        scheduleNextNotification(context, note, reminderData, nextNotificationTime)
                    }
                }
                ReminderType.SINGLE_EVENT -> {
                    scheduleEventOccurrence(context, note, reminderData, currentTime)
                }
            }
        }

        fun scheduleEventOccurrence(
            context: Context,
            note: Note,
            reminderData: ReminderData,
            afterMillis: Long,
        ): Long? {
            val occurrence = reminderData.nextOccurrence(afterMillis) ?: return null
            resolveAdvanceNotificationTime(
                occurrence = occurrence,
                advanceMinutes = reminderData.advanceNotificationMinutes,
                currentTime = afterMillis,
            )?.let { advanceTime ->
                scheduleAdvanceNotification(context, note, reminderData, advanceTime)
            }
            scheduleAlarmNotification(context, note, reminderData, occurrence)
            return occurrence
        }

        @SuppressLint("ScheduleExactAlarm")
        fun scheduleNextNotification(
            context: Context,
            note: Note,
            reminderData: ReminderData,
            notificationTime: Long
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val notificationIntent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("noteId", note.id)
                putExtra("noteTitle", note.title)
                putExtra("noteMessage", reminderData.message)
                putExtra("expirationDate", reminderData.date)
                putExtra("interval", reminderData.interval)
                putExtra("reminderType", reminderData.type.name)
                putExtra("notificationType", "periodic")
            }

            val requestCode = generateRequestCode(note.id)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleAlarm(context, alarmManager, notificationTime, pendingIntent)
        }

        @SuppressLint("ScheduleExactAlarm")
        fun scheduleAlarmNotification(
            context: Context,
            note: Note,
            reminderData: ReminderData,
            notificationTime: Long
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val notificationIntent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("noteId", note.id)
                putExtra("noteTitle", note.title)
                putExtra("noteMessage", reminderData.message)
                putExtra("expirationDate", reminderData.date)
                putExtra("reminderType", reminderData.type.name)
                putExtra("notificationType", "alarm")
            }

            val requestCode = generateRequestCode(note.id)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleAlarm(context, alarmManager, notificationTime, pendingIntent)
        }

        @SuppressLint("ScheduleExactAlarm")
        fun scheduleAdvanceNotification(
            context: Context,
            note: Note,
            reminderData: ReminderData,
            notificationTime: Long
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val notificationIntent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("noteId", note.id)
                putExtra("noteTitle", note.title)
                putExtra("noteMessage", reminderData.message)
                putExtra("expirationDate", reminderData.date)
                putExtra("reminderType", reminderData.type.name)
                putExtra("notificationType", "advance")
                putExtra("advanceMinutes", reminderData.advanceNotificationMinutes)
            }

            val requestCode = generateAdvanceRequestCode(note.id)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleAlarm(context, alarmManager, notificationTime, pendingIntent)
        }

        fun generateRequestCode(noteId: Long): Int {
            return (REQUEST_CODE_PREFIX + noteId).toInt()
        }

        fun generateAdvanceRequestCode(noteId: Long): Int {
            return (ADVANCE_REQUEST_CODE_PREFIX + noteId).toInt()
        }

        fun cancelNotification(context: Context, noteId: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.cancel(NOTIFICATION_ID_PREFIX + noteId.toInt())
            notificationManager.cancel((NOTIFICATION_ID_PREFIX + 500 + noteId).toInt())

            val cancelIntent = Intent(context, ReminderReceiver::class.java)
            val requestCode = generateRequestCode(noteId)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val advanceRequestCode = generateAdvanceRequestCode(noteId)
            val advancePendingIntent = PendingIntent.getBroadcast(
                context,
                advanceRequestCode,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                alarmManager.cancel(advancePendingIntent)
                advancePendingIntent.cancel()
            } catch (_: Exception) {
                DiagnosticLog.warning(DiagnosticEvent.REMINDER_ALARM_CANCEL_FAILED)
            }
        }

        fun createNotificationChannels(context: Context) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val localizedContext = LocaleController.localizedContext(context)

            val periodicChannel = NotificationChannel(
                PERIODIC_CHANNEL_ID,
                localizedContext.getString(R.string.periodic_reminders),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = localizedContext.getString(R.string.periodic_reminders_description)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                enableLights(true)
            }

            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                localizedContext.getString(R.string.alarm_reminders),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = localizedContext.getString(R.string.alarm_reminders_description)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
                enableLights(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
            }

            val advanceChannel = NotificationChannel(
                ADVANCE_CHANNEL_ID,
                localizedContext.getString(R.string.advance_notifications),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = localizedContext.getString(R.string.advance_notifications_description)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150, 150, 150)
                enableLights(true)
            }

            notificationManager.createNotificationChannel(periodicChannel)
            notificationManager.createNotificationChannel(alarmChannel)
            notificationManager.createNotificationChannel(advanceChannel)
        }

        @SuppressLint("ScheduleExactAlarm")
        private fun scheduleAlarm(
            context: Context,
            alarmManager: AlarmManager,
            notificationTime: Long,
            pendingIntent: PendingIntent,
        ) {
            val mode = resolveAlarmSchedulingMode(
                requiresExactAlarmPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                canScheduleExactAlarms = canScheduleExactAlarms(context),
            )
            if (mode != AlarmSchedulingMode.EXACT) {
                DiagnosticLog.error(DiagnosticEvent.REMINDER_ALARM_SCHEDULE_FAILED)
                return
            }

            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    notificationTime,
                    pendingIntent,
                )
            } catch (_: Exception) {
                DiagnosticLog.error(DiagnosticEvent.REMINDER_ALARM_SCHEDULE_FAILED)
            }
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getLongExtra(ReminderDeliveryCoordinator.KEY_NOTE_ID, 0L)
        val notificationType =
            intent.getStringExtra(ReminderDeliveryCoordinator.KEY_NOTIFICATION_TYPE)
        if (noteId <= 0L || notificationType == null) return

        val pendingResult = goAsync()
        val applicationContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ReminderDeliveryCoordinator(applicationContext).deliver(noteId, notificationType)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                DiagnosticLog.error(DiagnosticEvent.REMINDER_DELIVERY_FAILED)
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
