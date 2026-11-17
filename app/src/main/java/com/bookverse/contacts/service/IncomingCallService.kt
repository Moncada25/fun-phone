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
import com.bookverse.contacts.MainActivity
import com.bookverse.contacts.utils.ContactUtils
import com.bookverse.contacts.utils.ContactUtils.asCircular
import com.bookverse.contacts.utils.GradientUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IncomingCallService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 5002
        private const val ACTION_ANSWER = "com.bookverse.contacts.action.ANSWER_CALL"
        private const val ACTION_REJECT = "com.bookverse.contacts.action.REJECT_CALL"
        private const val ACTION_START_CALL = "com.bookverse.contacts.action.START_INCOMING_CALL"

        fun start(
            context: Context,
            callId: String,
            number: String,
            displayName: String? = null,
            avatarBytes: ByteArray? = null
        ): Boolean {
            val isRingingCall = DefaultCallRepository.calls.value.any {
                it.id == callId && it.state == CallState.RINGING
            }
            if (callId.isBlank() || !isRingingCall) return false

            val intent = Intent(context, IncomingCallService::class.java).apply {
                action = ACTION_START_CALL
                putExtra("call_id", callId)
                putExtra("number", number)
                displayName?.let { putExtra("display_name", it) }
                avatarBytes?.let { putExtra("avatar_bytes", it) }
            }
            try {
                context.startForegroundService(intent)
                return true
            } catch (exception: Exception) {
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
            val intent = Intent(context, IncomingCallService::class.java)
            context.stopService(intent)
        }

        fun cancelNotification(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm?.cancel(NOTIFICATION_ID)
        }
    }

    private var callAlert: CallAlert? = null
    private var currentCallId: String? = null
    @Volatile
    private var isStopping = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        CallNotificationChannels.ensureCreated(this)
        callAlert = CallAlert(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val callId = intent?.getStringExtra("call_id")
        if (intent?.action != ACTION_START_CALL || callId.isNullOrBlank()) {
            // Only validated call requests use startForegroundService.
            stopSelf(startId)
            return START_NOT_STICKY
        }

        isStopping = false
        val number = intent.getStringExtra("number").orEmpty()
        val prefilledName = intent.getStringExtra("display_name")
        val prefilledAvatarBytes = intent.getByteArrayExtra("avatar_bytes")
        val prefilledAvatar = prefilledAvatarBytes?.let { bytes ->
            runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
        }

        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as? android.app.KeyguardManager
        val isLocked = keyguardManager?.isDeviceLocked == true
        val notification = createIncomingCallNotification(
            callId = callId,
            number = number,
            screenOnly = isLocked,
            displayName = prefilledName ?: number,
            avatarBitmap = prefilledAvatar,
        )
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL,
            )
        } catch (_: IllegalStateException) {
            DiagnosticLog.warning(DiagnosticEvent.CALL_SURFACE_START_BLOCKED)
            stopSelf(startId)
            return START_NOT_STICKY
        } catch (_: SecurityException) {
            DiagnosticLog.error(DiagnosticEvent.CALL_NOTIFICATION_POST_FAILED)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        currentCallId = callId
        if (prefilledAvatar == null || prefilledName == null) {
            serviceScope.launch {
                if (isStopping) return@launch
                val contact = withContext(Dispatchers.IO) {
                    val repository = ContactRepository(applicationContext)
                    val contacts = repository.getContacts()
                    repository.findContactByPhoneNumber(contacts, number)
                }
                val avatarBitmap = contact?.photoUri?.let {
                    ContactUtils.uriToBitmap(it, applicationContext)
                }
                val updated = createIncomingCallNotification(
                    callId = callId,
                    number = number,
                    screenOnly = isLocked,
                    displayName = contact?.name ?: number,
                    avatarBitmap = avatarBitmap,
                )
                if (!isStopping) {
                    val nm = getSystemService(NotificationManager::class.java)
                    nm.notify(NOTIFICATION_ID, updated)
                }
                DiagnosticLog.debug(DiagnosticEvent.CALL_NOTIFICATION_CONTACT_LOADED)
            }
        }
        if (isLocked) {
            try {
                com.bookverse.contacts.utils.WakeUpHelper.wakeUp(applicationContext)
            } catch (_: Throwable) {
            }
        }
        callAlert?.start(callId)
        return START_NOT_STICKY
    }

    @SuppressLint("ObsoleteSdkInt")
    override fun onDestroy() {
        isStopping = true
        currentCallId?.let { callAlert?.stop(it) }
        currentCallId = null
        serviceScope.cancel()
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Throwable) {
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("FullScreenIntentPolicy")
    private fun createIncomingCallNotification(
        callId: String,
        number: String,
        screenOnly: Boolean,
        displayName: String = number,
        avatarBitmap: Bitmap? = null
    ): Notification {
        val fullScreenIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra("call_id", callId)
            putExtra("phone_number", number)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val answerIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = ACTION_ANSWER
            putExtra("call_id", callId)
        }
        val answerPendingIntent = PendingIntent.getBroadcast(
            this, 1, answerIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rejectIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = ACTION_REJECT
            putExtra("call_id", callId)
        }
        val rejectPendingIntent = PendingIntent.getBroadcast(
            this, 2, rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_incoming", true)
            putExtra("incoming_call_id", callId)
        }
        val mainAppPendingIntent = PendingIntent.getActivity(
            this,
            1,
            mainAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isDarkTheme = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        val person = if (avatarBitmap != null) {
            Person.Builder()
                .setName(displayName)
                .setIcon(IconCompat.createWithBitmap(avatarBitmap.asCircular()))
                .build()
        } else {
            Person.Builder()
                .setName(displayName)
                .build()
        }

        val gradientColors = GradientUtils.getBackgroundGradientColors(isDarkTheme)

        val channelId = if (screenOnly) {
            CallNotificationChannels.INCOMING_SILENT_CHANNEL_ID
        } else {
            CallNotificationChannels.INCOMING_CHANNEL_ID
        }
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_received_call)
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_CALL)
            .setVisibility(
                if (screenOnly) NotificationCompat.VISIBILITY_SECRET else NotificationCompat.VISIBILITY_PUBLIC
            )
            .setContentIntent(if (screenOnly) fullScreenPendingIntent else mainAppPendingIntent)

        if (screenOnly) {
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        if (screenOnly) {
            builder
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setContentTitle(displayName)
                .setContentText(getString(R.string.recent_type_call_received))
        } else {
            builder
                .setPriority(NotificationCompat.PRIORITY_MAX)
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
        }

        return builder.build()
    }
}
