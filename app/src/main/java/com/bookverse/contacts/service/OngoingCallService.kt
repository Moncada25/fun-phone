package com.bookverse.contacts.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.bookverse.contacts.MainActivity
import com.bookverse.contacts.R
import com.bookverse.contacts.data.repository.ContactRepository
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.domain.call.AudioRoute
import com.bookverse.contacts.utils.ContactUtils
import com.bookverse.contacts.utils.ContactUtils.asCircular
import com.bookverse.contacts.utils.GradientUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OngoingCallService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 5003
        private val startGate = OngoingCallStartGate()

        fun start(context: Context, callId: String, number: String): Boolean {
            val startToken = startGate.onStartRequested()
            val intent = Intent(context, OngoingCallService::class.java).apply {
                putExtra("call_id", callId)
                putExtra("number", number)
                putExtra("start_token", startToken)
            }
            return try {
                context.startForegroundService(intent)
                true
            } catch (_: Exception) {
                startGate.onStartFailed(startToken)
                DiagnosticLog.error(DiagnosticEvent.CALL_SURFACE_START_FAILED)
                false
            }
        }

        fun stop(context: Context) {
            if (!startGate.shouldStopImmediately()) return
            val intent = Intent(context, OngoingCallService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        CallNotificationChannels.ensureCreated(this)
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val foregroundLock = Any()
    private var foregroundCallId: String? = null
    private var isDestroyed = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val callId = intent?.getStringExtra("call_id") ?: ""
        val number = intent?.getStringExtra("number") ?: ""
        val startToken = intent?.getLongExtra("start_token", 0L) ?: 0L

        if (callId.isNotEmpty()) {
            val notification = createOngoingCallNotification(callId, number)
            if (!promoteToForeground(callId, notification)) {
                startGate.onStartFailed(startToken)
                stopSelf(startId)
                return START_NOT_STICKY
            }
            if (startGate.onForegroundStarted(startToken)) {
                stopSelf(startId)
                return START_NOT_STICKY
            }
            serviceScope.launch {
                try {
                    refreshContactNotification(callId, number)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    DiagnosticLog.error(DiagnosticEvent.CALL_NOTIFICATION_REFRESH_FAILED)
                }
            }
        } else {
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        synchronized(foregroundLock) {
            isDestroyed = true
            foregroundCallId = null
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        startGate.onDestroyed()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun promoteToForeground(callId: String, notification: Notification): Boolean =
        synchronized(foregroundLock) {
            if (isDestroyed) return@synchronized false
            postForegroundNotification(callId, notification)
        }

    private fun updateForegroundNotification(callId: String, notification: Notification): Boolean =
        synchronized(foregroundLock) {
            if (isDestroyed || !shouldUpdateOngoingCallNotification(foregroundCallId, callId)) {
                return@synchronized false
            }
            postForegroundNotification(callId, notification)
        }

    private fun postForegroundNotification(callId: String, notification: Notification): Boolean {
        return try {
            startForeground(NOTIFICATION_ID, notification)
            foregroundCallId = callId
            true
        } catch (_: Exception) {
            foregroundCallId = null
            DiagnosticLog.error(DiagnosticEvent.CALL_NOTIFICATION_POST_FAILED)
            false
        }
    }

    private suspend fun refreshContactNotification(callId: String, number: String) {
        val contact = withContext(Dispatchers.IO) {
            val repository = ContactRepository(applicationContext)
            val contacts = repository.getContacts()
            repository.findContactByPhoneNumber(contacts, number)
        }
        val avatarBitmap = contact?.photoUri?.let {
            ContactUtils.uriToBitmap(it, applicationContext)
        }
        val updated = createOngoingCallNotification(
            callId = callId,
            number = number,
            requestedDisplayName = contact?.name ?: number,
            avatarBitmap = avatarBitmap,
        )
        if (!updateForegroundNotification(callId, updated)) {
            DiagnosticLog.warning(DiagnosticEvent.CALL_NOTIFICATION_LATE_UPDATE_IGNORED)
            return
        }
        DiagnosticLog.debug(DiagnosticEvent.CALL_NOTIFICATION_CONTACT_LOADED)
    }

    private fun createOngoingCallNotification(
        callId: String,
        number: String,
        requestedDisplayName: String = number,
        avatarBitmap: Bitmap? = null
    ): Notification {
        val displayName = requestedDisplayName.ifBlank {
            DefaultCallRepository.calls.value.firstOrNull { it.id == callId }?.displayName
                ?: getString(R.string.unknown)
        }
        val contentIntent = ongoingCallContentIntent(this, callId, number)
        val contentPending = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val endIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_END
            putExtra("call_id", callId)
        }
        val endPending = PendingIntent.getBroadcast(
            this, 1, endIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val currentCall = DefaultCallRepository.activeCall.value
        val isMuted = currentCall?.isMuted ?: false

        val toggleMuteIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_TOGGLE_MUTE
            putExtra("call_id", callId)
            putExtra("number", number)
            putExtra("is_muted", isMuted)
        }
        val toggleMutePending = PendingIntent.getBroadcast(
            this,
            2,
            toggleMuteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isSpeakerOn = currentCall?.audioRoute == AudioRoute.SPEAKER
        val toggleSpeakerIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_TOGGLE_SPEAKER
            putExtra("call_id", callId)
            putExtra("number", number)
            putExtra("is_speaker_on", isSpeakerOn)
        }
        val toggleSpeakerPending = PendingIntent.getBroadcast(
            this,
            3,
            toggleSpeakerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
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

        val muteIcon = if (isMuted) {
            android.R.drawable.ic_lock_silent_mode_off
        } else {
            android.R.drawable.stat_notify_call_mute
        }
        val muteText = if (isMuted) getString(R.string.mic_on) else getString(R.string.mic_off)
        val speakerText = getString(if (isSpeakerOn) R.string.earpiece else R.string.speaker)

        return NotificationCompat.Builder(this, CallNotificationChannels.ONGOING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_received_call)
            .setContentTitle(displayName)
            .setContentText(getString(R.string.call_in_progress))
            .setCategory(Notification.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColorized(true)
            .setColor(gradientColors.first())
            .setContentIntent(contentPending)
            .setStyle(NotificationCompat.CallStyle.forOngoingCall(person, endPending))
            .addAction(muteIcon, muteText, toggleMutePending)
            .addAction(
                android.R.drawable.ic_lock_silent_mode_off,
                speakerText,
                toggleSpeakerPending,
            )
            .build()
    }
}

/** Ensures every startForegroundService request reaches startForeground before it can be stopped. */
internal class OngoingCallStartGate {
    private var lastStartToken = 0L
    private var stopThroughToken = 0L
    private val pendingStartTokens = mutableSetOf<Long>()
    private var foregroundStarted = false

    @Synchronized
    fun onStartRequested(): Long {
        val token = ++lastStartToken
        pendingStartTokens += token
        return token
    }

    @Synchronized
    fun shouldStopImmediately(): Boolean {
        stopThroughToken = lastStartToken
        return foregroundStarted || pendingStartTokens.isEmpty()
    }

    @Synchronized
    fun onForegroundStarted(startToken: Long): Boolean {
        pendingStartTokens -= startToken
        foregroundStarted = true
        return startToken <= stopThroughToken
    }

    @Synchronized
    fun onStartFailed(startToken: Long) {
        pendingStartTokens -= startToken
    }

    @Synchronized
    fun onDestroyed() {
        pendingStartTokens.clear()
        foregroundStarted = false
        stopThroughToken = lastStartToken
    }
}

internal fun shouldUpdateOngoingCallNotification(
    foregroundCallId: String?,
    updateCallId: String,
): Boolean = foregroundCallId != null && foregroundCallId == updateCallId

internal fun ongoingCallContentIntent(
    context: Context,
    callId: String,
    number: String,
): Intent = Intent(context, MainActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    putExtra("open_incall", true)
    putExtra("incall_call_id", callId)
    putExtra("incall_number", number)
}
