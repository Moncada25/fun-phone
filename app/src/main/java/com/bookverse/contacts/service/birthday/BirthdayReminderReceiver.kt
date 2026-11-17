package com.bookverse.contacts.service.birthday

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BirthdayReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BirthdayReminderScheduler.ACTION_DELIVER) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                BirthdayReminderCoordinator(appContext).deliver()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                /* noop */
            } finally {
                BirthdayReminderScheduler(appContext).scheduleNext()
                pendingResult.finish()
            }
        }
    }
}
