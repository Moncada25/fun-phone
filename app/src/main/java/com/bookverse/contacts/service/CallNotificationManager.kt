package com.bookverse.contacts.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.bookverse.contacts.App
import com.bookverse.contacts.MainActivity
import com.bookverse.contacts.R
import com.bookverse.contacts.settings.LocaleController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger

internal suspend fun resolveMissedCallDisplayName(
    number: String,
    timeoutMillis: Long,
    lookup: suspend () -> String?,
): String =
    withTimeoutOrNull(timeoutMillis) {
        runCatching { lookup() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    } ?: number

object CallNotificationManager {
    private const val MISSED_CHANNEL_ID = "missed_call_channel"
    private val missedCounter = AtomicInteger(6000)
    private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun ensureCallChannels(context: Context) {
        CallNotificationChannels.ensureCreated(context.applicationContext)
    }

    internal fun availability(context: Context): CallNotificationAvailability =
        CallNotificationChannels.readAvailability(context.applicationContext)

    fun showIncoming(
        context: Context,
        callId: String,
        number: String,
        displayName: String? = null,
        avatarBytes: ByteArray? = null
    ): Boolean =
        availability(context).canShowIncoming &&
            IncomingCallService.start(context, callId, number, displayName, avatarBytes)

    fun showOngoing(context: Context, callId: String, number: String): Boolean =
        availability(context).canShowOngoing &&
            OngoingCallService.start(context, callId, number)

    fun stopOngoing(context: Context) {
        OngoingCallService.stop(context.applicationContext)
    }

    fun openCallNotificationSettings(context: Context) {
        val availability = availability(context)
        val intent = if (
            availability.appNotificationsEnabled &&
            (!availability.incomingChannelEnabled || !availability.ongoingChannelEnabled)
        ) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(
                    Settings.EXTRA_CHANNEL_ID,
                    if (!availability.incomingChannelEnabled) {
                        CallNotificationChannels.INCOMING_CHANNEL_ID
                    } else {
                        CallNotificationChannels.ONGOING_CHANNEL_ID
                    },
                )
            }
        } else {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    @RequiresPermission("android.permission.POST_NOTIFICATIONS")
    fun showMissed(context: Context, number: String) {
        val appContext = context.applicationContext
        val notificationId = missedCounter.incrementAndGet()
        postMissedNotification(appContext, number, number, notificationId)
        notificationScope.launch {
            val lookup = (appContext as App).container.missedCallContactLookup
            val contactName = resolveMissedCallDisplayName(
                number = number,
                timeoutMillis = CONTACT_LOOKUP_TIMEOUT_MS,
            ) {
                lookup.findName(number)
            }
            if (contactName == number) return@launch
            postMissedNotification(appContext, number, contactName, notificationId)
        }
    }

    @RequiresPermission("android.permission.POST_NOTIFICATIONS")
    private fun postMissedNotification(
        context: Context,
        number: String,
        contactName: String,
        notificationId: Int,
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val localizedContext = LocaleController.localizedContext(context)
        val ch = NotificationChannel(
            MISSED_CHANNEL_ID,
            localizedContext.getString(R.string.recent_type_call_missed),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        nm.createNotificationChannel(ch)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_recents", true)
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = localizedContext.getString(R.string.recent_type_call_missed)
        val builder = NotificationCompat.Builder(context, MISSED_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_received_call)
            .setContentTitle(title)
            .setContentText(contactName)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setColor(context.getColor(R.color.accent_red))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setCategory(Notification.CATEGORY_MISSED_CALL)
        } else {
            builder.setCategory(Notification.CATEGORY_CALL)
        }
        val notif = builder.build()
        nm.notify(notificationId, notif)
    }

    private const val CONTACT_LOOKUP_TIMEOUT_MS = 1_500L
}
