package com.bookverse.contacts.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.bookverse.contacts.R
import com.bookverse.contacts.domain.call.CallState
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.utils.PermissionUtils

internal data class CallNotificationAvailability(
    val permissionGranted: Boolean,
    val appNotificationsEnabled: Boolean,
    val incomingChannelEnabled: Boolean,
    val ongoingChannelEnabled: Boolean,
) {
    val canShowIncoming: Boolean
        get() = permissionGranted && appNotificationsEnabled && incomingChannelEnabled

    val canShowOngoing: Boolean
        get() = permissionGranted && appNotificationsEnabled && ongoingChannelEnabled

    val isReady: Boolean
        get() = canShowIncoming && canShowOngoing
}

internal fun resolveCallNotificationAvailability(
    permissionGranted: Boolean,
    appNotificationsEnabled: Boolean,
    incomingChannelImportance: Int?,
    ongoingChannelImportance: Int?,
): CallNotificationAvailability = CallNotificationAvailability(
    permissionGranted = permissionGranted,
    appNotificationsEnabled = appNotificationsEnabled,
    incomingChannelEnabled = incomingChannelImportance != NotificationManager.IMPORTANCE_NONE,
    ongoingChannelEnabled = ongoingChannelImportance != NotificationManager.IMPORTANCE_NONE,
)

internal enum class CallSurface {
    IN_APP_INCOMING,
    IN_APP_ONGOING,
    INCOMING_NOTIFICATION,
    ONGOING_NOTIFICATION,
    NONE,
}

internal fun resolveCallSurface(
    state: CallState,
    appInForeground: Boolean,
    notificationAvailability: CallNotificationAvailability,
): CallSurface = when (state) {
    CallState.RINGING -> when {
        appInForeground -> CallSurface.IN_APP_INCOMING
        notificationAvailability.canShowIncoming -> CallSurface.INCOMING_NOTIFICATION
        else -> CallSurface.NONE
    }

    CallState.DIALING, CallState.ACTIVE, CallState.ON_HOLD -> when {
        appInForeground -> CallSurface.IN_APP_ONGOING
        notificationAvailability.canShowOngoing -> CallSurface.ONGOING_NOTIFICATION
        else -> CallSurface.NONE
    }

    CallState.DISCONNECTED -> CallSurface.NONE
}

internal object CallNotificationChannels {
    const val INCOMING_CHANNEL_ID = "incoming_call_channel"
    const val INCOMING_SILENT_CHANNEL_ID = "incoming_call_channel_silent"
    const val ONGOING_CHANNEL_ID = "ongoing_call_channel"

    fun ensureCreated(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val localizedContext = LocaleController.localizedContext(context)

        notificationManager.createNotificationChannel(
            NotificationChannel(
                INCOMING_CHANNEL_ID,
                localizedContext.getString(R.string.incoming_calls_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = localizedContext.getString(R.string.incoming_calls_channel_description)
                enableVibration(false)
                enableLights(true)
                setSound(null, null)
            },
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(
                INCOMING_SILENT_CHANNEL_ID,
                localizedContext.getString(R.string.incoming_calls_silent_channel_name),
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = localizedContext.getString(R.string.incoming_calls_silent_channel_description)
                enableVibration(false)
                setShowBadge(false)
                setSound(null, null)
            },
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(
                ONGOING_CHANNEL_ID,
                localizedContext.getString(R.string.ongoing_calls_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = localizedContext.getString(R.string.ongoing_calls_channel_description)
                setSound(null, null)
            },
        )
    }

    fun readAvailability(context: Context): CallNotificationAvailability {
        ensureCreated(context)
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val incomingImportance = notificationManager
            .getNotificationChannel(INCOMING_CHANNEL_ID)
            ?.importance
        val ongoingImportance = notificationManager
            .getNotificationChannel(ONGOING_CHANNEL_ID)
            ?.importance
        return resolveCallNotificationAvailability(
            permissionGranted = PermissionUtils.hasNotificationPermission(context),
            appNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            incomingChannelImportance = incomingImportance,
            ongoingChannelImportance = ongoingImportance,
        )
    }
}
