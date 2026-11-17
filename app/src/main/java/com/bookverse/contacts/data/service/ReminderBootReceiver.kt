package com.bookverse.contacts.data.service

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bookverse.contacts.service.awaitWorkEnqueue

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED ||
            intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        ) {
            val pendingResult = goAsync()
            awaitWorkEnqueue(
                context = context,
                pendingResult = pendingResult,
            ) {
                ReminderBootWorker.enqueueReschedule(context.applicationContext)
            }
        }
    }
}
