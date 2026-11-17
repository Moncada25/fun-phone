package com.bookverse.contacts.data.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bookverse.contacts.data.repository.ContactCacheRepository
import com.bookverse.contacts.data.repository.ContactRepository
import com.bookverse.contacts.data.repository.ProfileRepository
import com.bookverse.contacts.utils.PermissionUtils
import java.util.concurrent.TimeUnit

class ContactSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val contactRepository = ContactRepository(appContext)
    private val profileRepository = ProfileRepository(appContext)
    private val contactCacheRepository = ContactCacheRepository(appContext)

    override suspend fun doWork(): Result {
        if (!PermissionUtils.hasContactsPermission(applicationContext)) {
            return Result.failure()
        }

        return runCatching {
            val contacts = contactRepository.getContacts()
            val profile = profileRepository.getProfileContact()
            contactCacheRepository.replaceCachedContacts(contacts, profile)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }

    companion object {
        private const val UNIQUE_ONETIME = "contact_sync_onetime"
        private const val UNIQUE_PERIODIC = "contact_sync_periodic"

        fun enqueueImmediate(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<ContactSyncWorker>()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONETIME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        fun ensurePeriodicSync(context: Context) {
            val periodicRequest = PeriodicWorkRequestBuilder<ContactSyncWorker>(
                6,
                TimeUnit.HOURS
            )
                .setInitialDelay(30, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }
}
