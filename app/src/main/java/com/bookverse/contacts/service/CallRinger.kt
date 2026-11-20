package com.bookverse.contacts.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog

internal data class RingerOutputs(
    val playRingtone: Boolean,
    val vibrate: Boolean,
    val playWaitingTone: Boolean,
)

internal fun resolveRingerOutputs(
    ringerMode: Int,
    ringVolume: Int,
    callWaiting: Boolean,
    silenced: Boolean = false,
): RingerOutputs = if (silenced) {
    RingerOutputs(playRingtone = false, vibrate = false, playWaitingTone = false)
} else if (callWaiting) {
    RingerOutputs(playRingtone = false, vibrate = false, playWaitingTone = true)
} else {
    RingerOutputs(
        playRingtone = ringerMode == AudioManager.RINGER_MODE_NORMAL && ringVolume > 0,
        vibrate = ringerMode != AudioManager.RINGER_MODE_SILENT,
        playWaitingTone = false,
    )
}

internal class RingingCallTracker {
    private val ringingIds = linkedSetOf<String>()
    private var silenced = false

    val isRinging: Boolean
        get() = ringingIds.isNotEmpty()

    fun onRinging(callId: String): Boolean {
        val wasIdle = ringingIds.isEmpty()
        val added = ringingIds.add(callId)
        if (wasIdle) silenced = false
        return added && wasIdle
    }

    fun onStopped(callId: String): Boolean = ringingIds.remove(callId) && ringingIds.isEmpty()

    fun silence(): Boolean {
        if (ringingIds.isEmpty() || silenced) return false
        silenced = true
        return true
    }

    fun clear(): Boolean {
        val wasActive = ringingIds.isNotEmpty()
        ringingIds.clear()
        silenced = false
        return wasActive
    }
}

object CallRinger {
    private val tracker = RingingCallTracker()
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var toneGenerator: ToneGenerator? = null

    @Synchronized
    fun start(context: Context, callId: String, callWaiting: Boolean, silenced: Boolean = false) {
        if (!tracker.onRinging(callId)) return
        if (silenced) tracker.silence()
        val appContext = context.applicationContext
        val audioManager = appContext.getSystemService(AudioManager::class.java)
        val outputs = resolveRingerOutputs(
            ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL,
            ringVolume = audioManager?.getStreamVolume(AudioManager.STREAM_RING) ?: 0,
            callWaiting = callWaiting,
            silenced = silenced,
        )
        if (outputs.playRingtone) playRingtone(appContext)
        if (outputs.vibrate) vibrate(appContext)
        if (outputs.playWaitingTone) playWaitingTone()
        DiagnosticLog.debug(DiagnosticEvent.CALL_RINGER_STARTED)
    }

    @Synchronized
    fun stop(callId: String) {
        if (tracker.onStopped(callId)) {
            release()
            DiagnosticLog.debug(DiagnosticEvent.CALL_RINGER_STOPPED)
        }
    }

    @Synchronized
    fun silence() {
        if (tracker.silence()) {
            release()
            DiagnosticLog.debug(DiagnosticEvent.CALL_RINGER_SILENCED)
        }
    }

    @Synchronized
    fun stopAll() {
        tracker.clear()
        release()
    }

    @Synchronized
    fun isRinging(): Boolean = tracker.isRinging

    private fun ringtoneAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    private fun playRingtone(context: Context) {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        runCatching {
            RingtoneManager.getRingtone(context, uri)?.apply {
                audioAttributes = ringtoneAttributes()
                isLooping = true
                play()
            }
        }.onSuccess { ringtone = it }
            .onFailure { DiagnosticLog.error(DiagnosticEvent.CALL_RINGER_FAILED) }
    }

    private fun vibrate(context: Context) {
        val target = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        } ?: return
        val effect = VibrationEffect.createWaveform(RING_VIBRATION_PATTERN, 0)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                target.vibrate(
                    effect,
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_RINGTONE),
                )
            } else {
                @Suppress("DEPRECATION")
                target.vibrate(effect, ringtoneAttributes())
            }
        }.onSuccess { vibrator = target }
            .onFailure { DiagnosticLog.error(DiagnosticEvent.CALL_RINGER_FAILED) }
    }

    private fun playWaitingTone() {
        runCatching {
            ToneGenerator(AudioManager.STREAM_VOICE_CALL, WAITING_TONE_VOLUME).apply {
                startTone(ToneGenerator.TONE_SUP_CALL_WAITING)
            }
        }.onSuccess { toneGenerator = it }
            .onFailure { DiagnosticLog.error(DiagnosticEvent.CALL_RINGER_FAILED) }
    }

    private fun release() {
        runCatching { ringtone?.stop() }
        ringtone = null
        runCatching { vibrator?.cancel() }
        vibrator = null
        runCatching {
            toneGenerator?.stopTone()
            toneGenerator?.release()
        }
        toneGenerator = null
    }

    private val RING_VIBRATION_PATTERN = longArrayOf(0, 1200, 450, 1200, 450)
    private const val WAITING_TONE_VOLUME = 80
}
