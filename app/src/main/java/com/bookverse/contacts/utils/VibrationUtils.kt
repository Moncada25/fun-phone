package com.bookverse.contacts.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object VibrationUtils {

    fun vibrate(context: Context, vibrationDuration: Int, vibrationPattern: String) {
        if (vibrationPattern == "off") return

        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(VibratorManager::class.java) as VibratorManager).defaultVibrator
            } else {
                context.getSystemService(Vibrator::class.java)
            }

            if (vibrator != null) {
                val effect = when (vibrationPattern) {
                    "simple" -> VibrationEffect.createOneShot(
                        vibrationDuration.toLong(),
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )

                    "double" -> VibrationEffect.createWaveform(
                        longArrayOf(
                            0,
                            vibrationDuration.toLong(),
                            40,
                            vibrationDuration.toLong()
                        ), -1
                    )

                    "triple" -> VibrationEffect.createWaveform(
                        longArrayOf(
                            0,
                            vibrationDuration.toLong(),
                            40,
                            vibrationDuration.toLong(),
                            40,
                            vibrationDuration.toLong()
                        ), -1
                    )

                    else -> VibrationEffect.createOneShot(
                        vibrationDuration.toLong(),
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                }
                vibrator.vibrate(effect)
            }
        } catch (_: Exception) {
        }
    }
}
