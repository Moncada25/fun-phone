package com.bookverse.contacts.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bookverse.contacts.data.model.ContactUsageStats
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactUsageStatsDao {
    @Query("SELECT * FROM contact_usage_stats ORDER BY callCount DESC, lastCallTimestamp DESC LIMIT :limit")
    fun getTopUsedContacts(limit: Int = 10): Flow<List<ContactUsageStats>>

    @Query("SELECT * FROM contact_usage_stats WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getStatsForNumber(phoneNumber: String): ContactUsageStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStats(stats: ContactUsageStats)

    @Query("""
        UPDATE contact_usage_stats
        SET callCount = callCount + 1,
            lastCallTimestamp = :timestamp,
            totalCallDuration = totalCallDuration + :duration
        WHERE phoneNumber = :phoneNumber
    """)
    suspend fun incrementCallCount(phoneNumber: String, timestamp: Long, duration: Long): Int

    @Query("DELETE FROM contact_usage_stats WHERE phoneNumber = :phoneNumber")
    suspend fun deleteStats(phoneNumber: String)
}
