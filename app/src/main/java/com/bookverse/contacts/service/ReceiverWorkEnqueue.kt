package com.bookverse.contacts.service

import android.content.BroadcastReceiver
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.work.Operation
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog

/** Keeps a receiver alive until WorkManager has persisted and scheduled its operation. */
internal fun awaitWorkEnqueue(
    context: Context,
    pendingResult: BroadcastReceiver.PendingResult?,
    enqueue: () -> Operation?,
) {
    try {
        val operation = enqueue()
        if (operation == null) {
            pendingResult?.finish()
            return
        }

        if (pendingResult == null) return

        operation.result.addListener(
            { pendingResult.finish() },
            ContextCompat.getMainExecutor(context),
        )
    } catch (_: Exception) {
        DiagnosticLog.error(DiagnosticEvent.REMINDER_ENQUEUE_FAILED)
        pendingResult?.finish()
    }
}
