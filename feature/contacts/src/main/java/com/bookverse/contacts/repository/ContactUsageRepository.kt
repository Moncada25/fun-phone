package com.bookverse.contacts.repository

import android.content.Context
import com.bookverse.contacts.data.db.AppDatabase
import com.bookverse.contacts.data.model.ContactUsageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ContactUsageRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).contactUsageStatsDao()

    fun getTopUsedContacts(limit: Int = 10): Flow<List<ContactUsageStats>> =
        dao.getTopUsedContacts(limit)

    suspend fun recordCall(phoneNumber: String, duration: Long = 0) = withContext(Dispatchers.IO) {
        val cleanNumber = phoneNumber.trim()
        if (cleanNumber.isEmpty()) return@withContext

        val timestamp = System.currentTimeMillis()
        val existing = dao.getStatsForNumber(cleanNumber)

        if (existing != null) {
            dao.incrementCallCount(cleanNumber, timestamp, duration)
        } else {
            dao.upsertStats(
                ContactUsageStats(
                    phoneNumber = cleanNumber,
                    callCount = 1,
                    lastCallTimestamp = timestamp,
                    totalCallDuration = duration
                )
            )
        }
    }
}
