package com.bookverse.contacts.service.birthday

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.bookverse.contacts.service.ExactAlarmAccess
import com.bookverse.contacts.utils.PermissionUtils

enum class BirthdayReminderAccessIssue {
    NOTIFICATION_PERMISSION,
    EXACT_ALARM_PERMISSION,
    NOTIFICATION_SETTINGS,
}

object BirthdayReminderAccess {
    fun issue(context: Context, channelId: String): BirthdayReminderAccessIssue? {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val channelEnabled = notificationManager.getNotificationChannel(channelId)?.importance !=
            NotificationManager.IMPORTANCE_NONE
        return resolve(
            notificationPermissionGranted = PermissionUtils.hasNotificationPermission(context),
            notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            exactAlarmsEnabled = ExactAlarmAccess.canScheduleExactAlarms(context),
            channelEnabled = channelEnabled,
        )
    }

    fun resolve(
        notificationPermissionGranted: Boolean,
        notificationsEnabled: Boolean,
        exactAlarmsEnabled: Boolean,
        channelEnabled: Boolean,
    ): BirthdayReminderAccessIssue? {
        return when {
            !notificationPermissionGranted -> BirthdayReminderAccessIssue.NOTIFICATION_PERMISSION
            !exactAlarmsEnabled -> BirthdayReminderAccessIssue.EXACT_ALARM_PERMISSION
            !notificationsEnabled || !channelEnabled ->
                BirthdayReminderAccessIssue.NOTIFICATION_SETTINGS
            else -> null
        }
    }
}
