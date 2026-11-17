package com.bookverse.contacts.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.os.Build

class CallAlert(context: Context) {
    private val appContext = context.applicationContext

    private fun ringtoneUri(): Uri =
        android.media.RingtoneManager.getActualDefaultRingtoneUri(
            appContext, android.media.RingtoneManager.TYPE_RINGTONE
        )
            ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)

    private fun ringAudioAttrs(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    fun start(tag: String) {
        val key = normalizeTag(tag)
        val shouldStart = synchronized(lock) {
            val added = activeTags.add(key)
            added && activeTags.size == 1
        }
        if (!shouldStart) return

        val am = appContext.getSystemService(AudioManager::class.java)
        am?.mode = AudioManager.MODE_RINGTONE
        val ringerMode = am?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
        val ringVol = am?.getStreamVolume(AudioManager.STREAM_RING) ?: 0

        if (ringerMode == AudioManager.RINGER_MODE_NORMAL && ringVol > 0) {
            if (ringtone == null) {
                ringtone =
                    android.media.RingtoneManager.getRingtone(appContext, ringtoneUri()).apply {
                        audioAttributes = ringAudioAttrs()
                    }
            }
            ringtone?.play()
        }

        if (ringerMode != AudioManager.RINGER_MODE_SILENT) {
            if (vibrator == null) {
                vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    appContext.getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator
                } else {
                    appContext.getSystemService(android.os.Vibrator::class.java)
                }
            }
            val vib = vibrator
            val pattern = longArrayOf(0, 1200, 450, 1200, 450)
            val effect = android.os.VibrationEffect.createWaveform(pattern, 0)
            if (Build.VERSION.SDK_INT >= 33) {
                val vAttrs = android.os.VibrationAttributes.createForUsage(
                    android.os.VibrationAttributes.USAGE_RINGTONE
                )
                vib?.vibrate(effect, vAttrs)
            } else {
                @Suppress("DEPRECATION")
                vib?.vibrate(effect, ringAudioAttrs())
            }
        }
    }

    fun stop(tag: String? = null) {
        val key = tag?.let(::normalizeTag)
        val shouldStop = synchronized(lock) {
            if (key == null) {
                val wasActive = activeTags.isNotEmpty()
                activeTags.clear()
                wasActive
            } else {
                activeTags.remove(key)
                activeTags.isEmpty()
            }
        }
        if (!shouldStop) return

        ringtone?.stop()
        vibrator?.cancel()
        ringtone = null
        vibrator = null
        val am = appContext.getSystemService(AudioManager::class.java)
        if (am?.mode == AudioManager.MODE_RINGTONE) {
            am.mode = AudioManager.MODE_NORMAL
        }
    }

    private fun normalizeTag(raw: String): String = raw.ifBlank { "default" }

    companion object {
        private val lock = Any()
        private val activeTags = mutableSetOf<String>()
        private var ringtone: android.media.Ringtone? = null
        private var vibrator: android.os.Vibrator? = null

        fun activeTagsSnapshot(): Set<String> = synchronized(lock) { activeTags.toSet() }
    }
}
