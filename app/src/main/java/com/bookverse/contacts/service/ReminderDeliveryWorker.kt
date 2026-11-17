package com.bookverse.contacts.service

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import kotlin.coroutines.cancellation.CancellationException

class ReminderDeliveryWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val noteId = inputData.getLong(ReminderDeliveryCoordinator.KEY_NOTE_ID, 0L)
        val notificationType =
            inputData.getString(ReminderDeliveryCoordinator.KEY_NOTIFICATION_TYPE)
                ?: return Result.success()
        if (noteId <= 0L) return Result.success()

        return try {
            ReminderDeliveryCoordinator(applicationContext).deliver(noteId, notificationType)
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.REMINDER_DELIVERY_FAILED)
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_PREFIX = "reminder_delivery"

        internal fun uniqueWorkName(noteId: Long, notificationType: String): String =
            "${UNIQUE_WORK_PREFIX}_${noteId}_$notificationType"

        fun enqueue(context: Context, intent: Intent): Operation? {
            val noteId = intent.getLongExtra(ReminderDeliveryCoordinator.KEY_NOTE_ID, 0L)
            val notificationType =
                intent.getStringExtra(ReminderDeliveryCoordinator.KEY_NOTIFICATION_TYPE)
                    ?: return null
            if (noteId <= 0L) return null

            val request = OneTimeWorkRequestBuilder<ReminderDeliveryWorker>()
                .setInputData(
                    workDataOf(
                        ReminderDeliveryCoordinator.KEY_NOTE_ID to noteId,
                        ReminderDeliveryCoordinator.KEY_NOTIFICATION_TYPE to notificationType,
                    ),
                )
                .build()

            return WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                uniqueWorkName(noteId, notificationType),
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
