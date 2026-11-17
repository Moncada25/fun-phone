package com.bookverse.contacts.service.birthday

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bookverse.contacts.R
import com.bookverse.contacts.domain.birthday.BirthdayReminder
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.widget.ContactDetailActivity

class BirthdayReminderNotifier(context: Context) {
    private val appContext = context.applicationContext

    @SuppressLint("MissingPermission")
    fun notify(reminders: List<BirthdayReminder>): Boolean {
        if (reminders.isEmpty() || !canPublish(appContext)) return false
        val notificationManager = NotificationManagerCompat.from(appContext)

        return try {
            reminders.forEach { reminder ->
                notificationManager.notify(
                    notificationId(reminder.contact.id),
                    buildNotification(reminder),
                )
            }

            if (reminders.size > 1) {
                notificationManager.notify(SUMMARY_NOTIFICATION_ID, buildSummary(reminders))
            }
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun createChannel() = createChannel(appContext)

    private fun buildNotification(reminder: BirthdayReminder): android.app.Notification {
        val localizedContext = LocaleController.localizedContext(appContext)
        val (titleId, textId) = when (reminder.daysUntilBirthday) {
            0 -> R.string.birthday_notification_today_title to R.string.birthday_notification_today_text
            1 -> R.string.birthday_notification_tomorrow_title to R.string.birthday_notification_tomorrow_text
            else -> R.string.birthday_notification_in_two_days_title to R.string.birthday_notification_in_two_days_text
        }
        return NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle(localizedContext.getString(titleId, reminder.contact.name))
            .setContentText(localizedContext.getString(textId))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setGroup(GROUP_KEY)
            .setContentIntent(contactPendingIntent(reminder))
            .setAutoCancel(true)
            .build()
    }

    private fun buildSummary(reminders: List<BirthdayReminder>): android.app.Notification {
        val localizedContext = LocaleController.localizedContext(appContext)
        val title = localizedContext.resources.getQuantityString(
            R.plurals.birthday_notification_summary_title,
            reminders.size,
            reminders.size,
        )
        return NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle(title)
            .setContentText(localizedContext.getString(R.string.birthday_notification_summary_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
    }

    private fun contactPendingIntent(reminder: BirthdayReminder): PendingIntent {
        val intent = Intent(appContext, ContactDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ContactDetailActivity.EXTRA_CONTACT_ID, reminder.contact.id)
            putExtra(ContactDetailActivity.EXTRA_CONTACT_NAME, reminder.contact.name)
        }
        return PendingIntent.getActivity(
            appContext,
            notificationId(reminder.contact.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationId(contactId: String): Int =
        NOTIFICATION_ID_PREFIX + (contactId.hashCode() and 0x000f_ffff)

    companion object {
        const val CHANNEL_ID = "birthday_reminders"
        private const val GROUP_KEY = "birthday_reminders_group"
        private const val NOTIFICATION_ID_PREFIX = 4_000_000
        private const val SUMMARY_NOTIFICATION_ID = 5_100_000

        fun canPublish(context: Context): Boolean {
            val appContext = context.applicationContext
            createChannel(appContext)
            if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) return false
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
            val notificationManager =
                appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return notificationManager.getNotificationChannel(CHANNEL_ID)?.importance !=
                NotificationManager.IMPORTANCE_NONE
        }

        fun createChannel(context: Context) {
            val appContext = context.applicationContext
            val localizedContext = LocaleController.localizedContext(appContext)
            val channel = NotificationChannel(
                CHANNEL_ID,
                localizedContext.getString(R.string.birthday_reminders_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = localizedContext.getString(R.string.birthday_reminders_channel_description)
                enableVibration(true)
            }
            val notificationManager =
                appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
