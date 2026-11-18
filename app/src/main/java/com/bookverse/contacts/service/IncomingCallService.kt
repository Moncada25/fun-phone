package com.bookverse.contacts.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.ServiceCompat
import androidx.core.graphics.drawable.IconCompat
import com.bookverse.contacts.R
import com.bookverse.contacts.data.repository.ContactRepository
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.domain.call.CallState
import com.bookverse.contacts.ui.activities.IncomingCallActivity
import com.bookverse.contacts.utils.ContactUtils
import com.bookverse.contacts.utils.ContactUtils.asCircular
import com.bookverse.contacts.utils.GradientUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class IncomingCallService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 5002
        private const val ACTION_START_CALL = "com.bookverse.contacts.action.START_INCOMING_CALL"
        private val startGate = OngoingCallStartGate()

        fun start(
            context: Context,
            callId: String,
            number: String,
            displayName: String? = null,
            avatarBytes: ByteArray? = null
        ): Boolean {
            if (callId.isBlank() || !isRinging(callId)) return false

            val startToken = startGate.onStartRequested()
            val intent = Intent(context, IncomingCallService::class.java).apply {
                action = ACTION_START_CALL
                putExtra("call_id", callId)
                putExtra("number", number)
                putExtra("start_token", startToken)
                displayName?.let { putExtra("display_name", it) }
                avatarBytes?.let { putExtra("avatar_bytes", it) }
            }
            try {
                context.startForegroundService(intent)
                return true
            } catch (exception: Exception) {
                startGate.onStartFailed(startToken)
                val exceptionName = exception.javaClass.name
                if (exceptionName == "android.app.ForegroundServiceStartNotAllowedException") {
                    DiagnosticLog.warning(DiagnosticEvent.CALL_SURFACE_START_BLOCKED)
                    return false
                }
                DiagnosticLog.error(DiagnosticEvent.CALL_SURFACE_START_FAILED)
                return false
            }
        }

        fun stop(context: Context) {
            if (!startGate.shouldStopImmediately()) return
            context.stopService(Intent(context, IncomingCallService::class.java))
        }

        fun cancelNotification(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm?.cancel(NOTIFICATION_ID)
        }

        private fun isRinging(callId: String): Boolean =
            DefaultCallRepository.calls.value.any { it.id == callId && it.state == CallState.RINGING }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val foregroundLock = Any()
    private var foregroundCallId: String? = null
    private var isDestroyed = false

    override fun onCreate() {
        super.onCreate()
        CallNotificationChannels.ensureCreated(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val callId = intent?.getStringExtra("call_id")
        val startToken = intent?.getLongExtra("start_token", 0L) ?: 0L
        if (intent?.action != ACTION_START_CALL || callId.isNullOrBlank()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val number = intent.getStringExtra("number").orEmpty()
        val prefilledName = intent.getStringExtra("display_name")
        val prefilledAvatarBytes = intent.getByteArrayExtra("avatar_bytes")
        val prefilledAvatar = prefilledAvatarBytes?.let { bytes ->
            runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
        }

        val notification = createIncomingCallNotification(
            callId = callId,
            number = number,
            displayName = prefilledName ?: number,
            avatarBitmap = prefilledAvatar,
        )
        if (!promoteToForeground(callId, notification)) {
            startGate.onStartFailed(startToken)
            stopSelf(startId)
            return START_NOT_STICKY
        }
        if (startGate.onForegroundStarted(startToken) || !isRinging(callId)) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (prefilledAvatar == null || prefilledName == null) {
            serviceScope.launch {
                val contact = runCatching {
                    val repository = ContactRepository(applicationContext)
                    repository.findContactByPhoneNumber(repository.getContacts(), number)
                }.getOrNull()
                val avatarBitmap = contact?.photoUri?.let {
                    ContactUtils.uriToBitmap(it, applicationContext)
                }
                val updated = createIncomingCallNotification(
                    callId = callId,
                    number = number,
                    displayName = contact?.name ?: number,
                    avatarBitmap = avatarBitmap,
                )
                updateForegroundNotification(callId, updated)
                DiagnosticLog.debug(DiagnosticEvent.CALL_NOTIFICATION_CONTACT_LOADED)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        synchronized(foregroundLock) {
            isDestroyed = true
            foregroundCallId = null
            runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        }
        startGate.onDestroyed()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun promoteToForeground(callId: String, notification: Notification): Boolean =
        synchronized(foregroundLock) {
            if (isDestroyed) return@synchronized false
            try {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL,
                )
                foregroundCallId = callId
                true
            } catch (_: IllegalStateException) {
                DiagnosticLog.warning(DiagnosticEvent.CALL_SURFACE_START_BLOCKED)
                false
            } catch (_: SecurityException) {
                DiagnosticLog.error(DiagnosticEvent.CALL_NOTIFICATION_POST_FAILED)
                false
            }
        }

    private fun updateForegroundNotification(callId: String, notification: Notification) {
        synchronized(foregroundLock) {
            if (isDestroyed || foregroundCallId != callId || !isRinging(callId)) return
            runCatching {
                getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
            }
        }
    }

    @SuppressLint("FullScreenIntentPolicy")
    private fun createIncomingCallNotification(
        callId: String,
        number: String,
        displayName: String = number,
        avatarBitmap: Bitmap? = null
    ): Notification {
        val fullScreenIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra(IncomingCallActivity.EXTRA_CALL_ID, callId)
            putExtra(IncomingCallActivity.EXTRA_PHONE_NUMBER, number)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val answerIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_ANSWER
            putExtra("call_id", callId)
        }
        val answerPendingIntent = PendingIntent.getBroadcast(
            this, 1, answerIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rejectIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_REJECT
            putExtra("call_id", callId)
        }
        val rejectPendingIntent = PendingIntent.getBroadcast(
            this, 2, rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isDarkTheme = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        val person = Person.Builder()
            .setName(displayName)
            .apply {
                avatarBitmap?.let { setIcon(IconCompat.createWithBitmap(it.asCircular())) }
            }
            .build()

        val gradientColors = GradientUtils.getBackgroundGradientColors(isDarkTheme)

        return NotificationCompat.Builder(this, CallNotificationChannels.INCOMING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_received_call)
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setColorized(true)
            .setColor(gradientColors.first())
            .setContentTitle(displayName)
            .setContentText(getString(R.string.recent_type_call_received))
            .setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    person,
                    rejectPendingIntent,
                    answerPendingIntent
                )
            )
            .build()
    }
}
