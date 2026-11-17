package com.bookverse.contacts.data.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bookverse.contacts.data.db.AppDatabase
import com.bookverse.contacts.data.notes.repeats
import com.bookverse.contacts.data.repository.ContactCacheRepository
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.service.NotificationService
import com.bookverse.contacts.service.birthday.BirthdayReminderNotifier
import com.bookverse.contacts.service.birthday.BirthdayReminderScheduler
import kotlin.coroutines.cancellation.CancellationException

internal fun shouldRescheduleReminderAfterBoot(
    isReminder: Boolean,
    isDeleted: Boolean,
    reminderDate: Long?,
    currentTime: Long,
    repeats: Boolean = false,
): Boolean =
    isReminder &&
        !isDeleted &&
        reminderDate != null &&
        (repeats || reminderDate > currentTime)

class ReminderBootWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getInstance(applicationContext)
            val noteDao = db.noteDao()
            val notes = noteDao.getAllNotesSync().filter { it.isReminder }
            val currentTime = System.currentTimeMillis()

            for (note in notes) {
                val reminderData = noteDao.getReminderDataForNote(note.id)
                if (
                    shouldRescheduleReminderAfterBoot(
                        isReminder = note.isReminder,
                        isDeleted = note.isDeleted,
                        reminderDate = reminderData?.date,
                        currentTime = currentTime,
                        repeats = reminderData?.repeats == true,
                    )
                ) {
                    reminderData ?: continue
                    NotificationService.scheduleNotification(applicationContext, note, reminderData)
                }
            }
            BirthdayReminderNotifier(applicationContext).createChannel()
            val birthdayScheduler = BirthdayReminderScheduler(applicationContext)
            if (
                ContactCacheRepository(applicationContext)
                    .getCachedContacts()
                    .any { it.birthday != null }
            ) {
                birthdayScheduler.scheduleNext(catchUpToday = true)
            } else {
                birthdayScheduler.cancel()
            }
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.REMINDER_RESCHEDULE_FAILED)
            Result.retry()
        }
    }

    companion object {
        internal const val UNIQUE_WORK = "reminder_boot_reschedule"

        fun enqueueReschedule(context: Context): Operation {
            val workRequest = OneTimeWorkRequestBuilder<ReminderBootWorker>()
                .build()

            return WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}
