package com.bookverse.contacts.utils

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog

object WakeUpHelper {
    fun wakeUp(context: Context, activity: Activity? = null) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager

            @Suppress("DEPRECATION")
            val levelAndFlags = PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE

            @Suppress("DEPRECATION")
            val wl = pm.newWakeLock(levelAndFlags, "funphone:incoming_call_wakeup")
            wl.acquire(3_000L)
        } catch (_: Throwable) {
            DiagnosticLog.warning(DiagnosticEvent.WAKE_LOCK_ACQUIRE_FAILED)
        }

        if (activity != null) {
            try {
                val km = activity.getSystemService(KeyguardManager::class.java)
                km?.requestDismissKeyguard(activity, null)
            } catch (_: Throwable) {
                DiagnosticLog.warning(DiagnosticEvent.KEYGUARD_DISMISS_FAILED)
            }
        }
    }
}
