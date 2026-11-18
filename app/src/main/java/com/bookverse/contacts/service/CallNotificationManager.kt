package com.bookverse.contacts.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.CallLog
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.annotation.SuppressLint
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.bookverse.contacts.App
import com.bookverse.contacts.MainActivity
import com.bookverse.contacts.R
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.utils.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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
        postMissedWithContactLookup(context.applicationContext, number, alert = true)
    }

    internal fun onTelecomMissedCalls(context: Context, count: Int, callLogUri: Uri?) {
        val appContext = context.applicationContext
        val hasAnyNotification = activeMissedTags(appContext).isNotEmpty()
        val update = resolveMissedCallsUpdate(
            count = count,
            hasAnyNotification = hasAnyNotification,
            callLogNumber = if (count == 1 && !hasAnyNotification) {
                callLogUri?.let { readCallLogNumber(appContext, it) }
            } else {
                null
            },
        )
        DiagnosticLog.debug(DiagnosticEvent.CALL_MISSED_NOTIFICATION_REQUESTED)
        if (update == MissedCallsUpdate.Clear) {
            cancelMissedNotifications(appContext)
            return
        }
        if (!PermissionUtils.hasNotificationPermission(appContext)) return
        try {
            when (update) {
                is MissedCallsUpdate.ShowNumber ->
                    postMissedWithContactLookup(appContext, update.number, alert = false)
                is MissedCallsUpdate.ShowCount ->
                    postMissedNotification(
                        context = appContext,
                        number = null,
                        contactName = null,
                        count = update.count,
                        alert = false,
                    )
                else -> Unit
            }
        } catch (_: SecurityException) {
            DiagnosticLog.error(DiagnosticEvent.CALL_MISSED_NOTIFICATION_FAILED)
        }
    }

    private fun readCallLogNumber(context: Context, callLogUri: Uri): String? {
        val authority = callLogUri.encodedAuthority?.substringAfter('@') ?: return null
        val uri = callLogUri.buildUpon().encodedAuthority(authority).build()
        return runCatching {
            context.contentResolver.query(uri, arrayOf(CallLog.Calls.NUMBER), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    @SuppressLint("MissingPermission")
    fun clearMissedCalls(context: Context) {
        val appContext = context.applicationContext
        cancelMissedNotifications(appContext)
        if (!PermissionUtils.isDefaultDialer(appContext)) return
        try {
            appContext.getSystemService(TelecomManager::class.java)?.cancelMissedCallsNotification()
            DiagnosticLog.debug(DiagnosticEvent.CALL_MISSED_NOTIFICATIONS_CLEARED)
        } catch (_: SecurityException) {
            DiagnosticLog.warning(DiagnosticEvent.CALL_MISSED_NOTIFICATIONS_CLEAR_FAILED)
        } catch (_: UnsupportedOperationException) {
            DiagnosticLog.warning(DiagnosticEvent.CALL_MISSED_NOTIFICATIONS_CLEAR_FAILED)
        }
    }

    internal fun onMissedNotificationDismissed(context: Context) {
        if (activeMissedTags(context.applicationContext).isEmpty()) {
            clearMissedCalls(context)
        }
    }

    private fun cancelMissedNotifications(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        activeMissedTags(context).forEach { tag -> nm.cancel(tag, MISSED_NOTIFICATION_ID) }
    }

    private fun activeMissedTags(context: Context): Set<String> {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return emptySet()
        return runCatching {
            nm.activeNotifications
                .filter { it.id == MISSED_NOTIFICATION_ID && it.tag?.startsWith(MISSED_TAG_PREFIX) == true }
                .mapNotNull { it.tag }
                .toSet()
        }.getOrDefault(emptySet())
    }

    private fun postMissedWithContactLookup(context: Context, number: String, alert: Boolean) {
        if (!PermissionUtils.hasNotificationPermission(context)) return
        postMissedNotification(context, number, number, count = 1, alert = alert)
        notificationScope.launch {
            val lookup = (context as App).container.missedCallContactLookup
            val contactName = resolveMissedCallDisplayName(
                number = number,
                timeoutMillis = CONTACT_LOOKUP_TIMEOUT_MS,
            ) {
                lookup.findName(number)
            }
            if (contactName == number) return@launch
            if (missedTag(number) !in activeMissedTags(context)) return@launch
            try {
                postMissedNotification(context, number, contactName, count = 1, alert = false)
            } catch (_: SecurityException) {
                DiagnosticLog.error(DiagnosticEvent.CALL_MISSED_NOTIFICATION_FAILED)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun postMissedNotification(
        context: Context,
        number: String?,
        contactName: String?,
        count: Int,
        alert: Boolean,
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val localizedContext = LocaleController.localizedContext(context)
        val ch = NotificationChannel(
            MISSED_CHANNEL_ID,
            localizedContext.getString(R.string.recent_type_call_missed),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        nm.createNotificationChannel(ch)
        val tag = number?.let(::missedTag) ?: MISSED_SUMMARY_TAG
        val requestSeed = tag.hashCode()
        val contentIntent = missedCallActivityIntent(context, number = null, action = null)
        val pending = PendingIntent.getActivity(
            context,
            requestSeed,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val deleteIntent = PendingIntent.getBroadcast(
            context,
            requestSeed,
            Intent(context, MissedCallsNotificationReceiver::class.java)
                .setAction(MissedCallsNotificationReceiver.ACTION_MISSED_NOTIFICATION_DISMISSED),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = localizedContext.getString(R.string.recent_type_call_missed)
        val text = contactName
            ?: localizedContext.resources.getQuantityString(R.plurals.missed_calls_count, count, count)
        val builder = NotificationCompat.Builder(context, MISSED_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_missed_call)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setOnlyAlertOnce(!alert)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setDeleteIntent(deleteIntent)
            .setColor(context.getColor(R.color.accent_red))
        if (number != null && number.isNotBlank()) {
            builder.addAction(
                0,
                localizedContext.getString(R.string.missed_call_action_call_back),
                PendingIntent.getActivity(
                    context,
                    requestSeed + 1,
                    missedCallActivityIntent(context, number, MISSED_ACTION_CALL),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            builder.addAction(
                0,
                localizedContext.getString(R.string.missed_call_action_message),
                PendingIntent.getActivity(
                    context,
                    requestSeed + 2,
                    missedCallActivityIntent(context, number, MISSED_ACTION_MESSAGE),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setCategory(Notification.CATEGORY_MISSED_CALL)
        } else {
            builder.setCategory(Notification.CATEGORY_CALL)
        }
        nm.notify(tag, MISSED_NOTIFICATION_ID, builder.build())
    }

    private fun missedCallActivityIntent(context: Context, number: String?, action: String?): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_recents", action == null)
            putExtra(EXTRA_CLEAR_MISSED_CALLS, true)
            if (number != null && action != null) {
                putExtra(EXTRA_MISSED_CALL_ACTION, action)
                putExtra(EXTRA_MISSED_CALL_NUMBER, number)
            }
        }

    internal fun missedTag(number: String): String =
        MISSED_TAG_PREFIX + (PhoneNumberUtils.normalizeNumber(number)?.ifBlank { null } ?: number)

    const val EXTRA_CLEAR_MISSED_CALLS = "clear_missed_calls"
    const val EXTRA_MISSED_CALL_ACTION = "missed_call_action"
    const val EXTRA_MISSED_CALL_NUMBER = "missed_call_number"
    const val MISSED_ACTION_CALL = "call"
    const val MISSED_ACTION_MESSAGE = "message"
    private const val MISSED_NOTIFICATION_ID = 6000
    private const val MISSED_TAG_PREFIX = "missed:"
    private const val MISSED_SUMMARY_TAG = "missed:summary"
    private const val CONTACT_LOOKUP_TIMEOUT_MS = 1_500L
}

internal sealed interface MissedCallsUpdate {
    data object Clear : MissedCallsUpdate
    data object Keep : MissedCallsUpdate
    data class ShowNumber(val number: String) : MissedCallsUpdate
    data class ShowCount(val count: Int) : MissedCallsUpdate
}

internal fun resolveMissedCallsUpdate(
    count: Int,
    hasAnyNotification: Boolean,
    callLogNumber: String?,
): MissedCallsUpdate = when {
    count <= 0 -> MissedCallsUpdate.Clear
    hasAnyNotification -> MissedCallsUpdate.Keep
    count == 1 && !callLogNumber.isNullOrBlank() -> MissedCallsUpdate.ShowNumber(callLogNumber)
    else -> MissedCallsUpdate.ShowCount(count)
}
