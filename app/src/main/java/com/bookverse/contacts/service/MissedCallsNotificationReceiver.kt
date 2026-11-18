package com.bookverse.contacts.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import androidx.core.content.IntentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MissedCallsNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TelecomManager.ACTION_SHOW_MISSED_CALLS_NOTIFICATION -> {
                val count = intent.getIntExtra(TelecomManager.EXTRA_NOTIFICATION_COUNT, 0)
                val callLogUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    IntentCompat.getParcelableExtra(intent, TelecomManager.EXTRA_CALL_LOG_URI, Uri::class.java)
                } else {
                    null
                }
                val appContext = context.applicationContext
                val pending = goAsync()
                receiverScope.launch {
                    try {
                        if (count > 0) delay(IN_CALL_SERVICE_GRACE_MS)
                        CallNotificationManager.onTelecomMissedCalls(appContext, count, callLogUri)
                    } finally {
                        pending.finish()
                    }
                }
            }

            ACTION_MISSED_NOTIFICATION_DISMISSED ->
                CallNotificationManager.onMissedNotificationDismissed(context)
        }
    }

    companion object {
        const val ACTION_MISSED_NOTIFICATION_DISMISSED =
            "com.bookverse.contacts.action.MISSED_CALL_NOTIFICATION_DISMISSED"
        private const val IN_CALL_SERVICE_GRACE_MS = 1_500L
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
