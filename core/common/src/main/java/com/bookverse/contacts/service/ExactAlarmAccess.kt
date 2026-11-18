package com.bookverse.contacts.service

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog

object ExactAlarmAccess {
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true

        return try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.REMINDER_EXACT_ALARM_PERMISSION_READ_FAILED)
            false
        }
    }

    fun createAlarmPermissionIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null

        return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = "package:${context.packageName}".toUri()
        }
    }
}
