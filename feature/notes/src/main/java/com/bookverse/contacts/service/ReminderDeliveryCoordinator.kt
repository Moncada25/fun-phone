package com.bookverse.contacts.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bookverse.contacts.feature.notes.R
import com.bookverse.contacts.navigation.AppEntryPoints
import com.bookverse.contacts.data.db.AppDatabase
import com.bookverse.contacts.data.notes.nextOccurrence
import com.bookverse.contacts.data.notes.repeats
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.core.ui.R as CoreUiR

class ReminderDeliveryCoordinator(
    private val context: Context,
) {
    suspend fun deliver(noteId: Long, notificationType: String) {
        NotificationService.createNotificationChannels(context)
        val noteDao = AppDatabase.getInstance(context).noteDao()
        val note = noteDao.getNoteById(noteId)
        val reminderData = noteDao.getReminderDataForNote(noteId)

        if (
            !shouldDeliverStoredReminder(
                noteExists = note != null,
                isReminder = note?.isReminder == true,
                isDeleted = note?.isDeleted == true,
                hasReminderData = reminderData != null,
            )
        ) {
            NotificationService.cancelNotification(context, noteId)
            return
        }

        note ?: return
        reminderData ?: return
        val currentTime = System.currentTimeMillis()

        when (
            resolveReminderDeliveryAction(
                notificationType = notificationType,
                currentTime = currentTime,
                expirationDate = reminderData.nextOccurrence(currentTime) ?: reminderData.date,
            )
        ) {
            ReminderDeliveryAction.CANCEL -> NotificationService.cancelNotification(context, noteId)
            ReminderDeliveryAction.SHOW_PERIODIC -> {
                showPeriodicNotification(noteId, note.title, reminderData.message)
                resolveNextPeriodicNotificationTime(
                    currentTime = currentTime,
                    intervalSeconds = reminderData.interval,
                    expirationDate = reminderData.date,
                    anchorTime = reminderData.creationDate,
                )?.let { nextNotificationTime ->
                    NotificationService.scheduleNextNotification(
                        context,
                        note,
                        reminderData,
                        nextNotificationTime,
                    )
                }
            }
            ReminderDeliveryAction.SHOW_ALARM -> {
                showAlarmNotification(noteId, note.title, reminderData.message)
                if (reminderData.repeats) {
                    NotificationService.scheduleEventOccurrence(
                        context,
                        note,
                        reminderData,
                        currentTime,
                    )
                }
            }
            ReminderDeliveryAction.SHOW_ADVANCE -> {
                val localizedResources = LocaleController.localizedContext(context).resources
                val message = localizedResources.getQuantityString(
                    R.plurals.reminder_advance_notification,
                    reminderData.advanceNotificationMinutes,
                    reminderData.message,
                    reminderData.advanceNotificationMinutes,
                )
                showAdvanceNotification(noteId, note.title, message)
            }
            ReminderDeliveryAction.IGNORE -> Unit
        }
    }

    private fun showPeriodicNotification(noteId: Long, title: String, message: String) {
        val notification = NotificationCompat.Builder(
            context,
            NotificationService.PERIODIC_CHANNEL_ID,
        )
            .setSmallIcon(CoreUiR.drawable.ic_notification_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(createContentIntent())
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        postNotification(
            channelId = NotificationService.PERIODIC_CHANNEL_ID,
            notificationId = NotificationService.NOTIFICATION_ID_PREFIX + noteId.toInt(),
            notification = notification,
        )
    }

    @SuppressLint("FullScreenIntentPolicy")
    private fun showAlarmNotification(noteId: Long, title: String, message: String) {
        val contentIntent = createContentIntent()
        val notification = NotificationCompat.Builder(
            context,
            NotificationService.ALARM_CHANNEL_ID,
        )
            .setSmallIcon(CoreUiR.drawable.ic_notification_reminder)
            .setContentTitle("🚨 $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 250, 500, 250, 500))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(contentIntent, true)
            .build()

        postNotification(
            channelId = NotificationService.ALARM_CHANNEL_ID,
            notificationId = NotificationService.NOTIFICATION_ID_PREFIX + noteId.toInt(),
            notification = notification,
        )
    }

    private fun showAdvanceNotification(noteId: Long, title: String, message: String) {
        val notification = NotificationCompat.Builder(
            context,
            NotificationService.ADVANCE_CHANNEL_ID,
        )
            .setSmallIcon(CoreUiR.drawable.ic_notification_reminder)
            .setContentTitle("⏰ $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(createContentIntent())
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 150, 150, 150))
            .build()

        postNotification(
            channelId = NotificationService.ADVANCE_CHANNEL_ID,
            notificationId = (NotificationService.NOTIFICATION_ID_PREFIX + 500 + noteId).toInt(),
            notification = notification,
        )
    }

    private fun postNotification(
        channelId: String,
        notificationId: Int,
        notification: android.app.Notification,
    ) {
        if (!NotificationService.isReminderChannelEnabled(context, channelId)) {
            DiagnosticLog.warning(DiagnosticEvent.REMINDER_NOTIFICATION_SKIPPED)
            return
        }
        notificationManager().notify(notificationId, notification)
    }

    private fun createContentIntent(): PendingIntent {
        val intent = AppEntryPoints.mainActivityIntent(context).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_notes_list", true)
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationManager(): NotificationManager =
        ContextCompat.getSystemService(context, NotificationManager::class.java) as NotificationManager

    companion object {
        internal const val KEY_NOTE_ID = "noteId"
        internal const val KEY_NOTIFICATION_TYPE = "notificationType"
    }
}
