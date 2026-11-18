package com.bookverse.contacts.data.qr

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QRHistoryDao {
    @Query("SELECT * FROM qr_history ORDER BY timestamp DESC")
    fun getAllQRHistory(): Flow<List<QRHistoryEntity>>

    @Insert
    suspend fun insertQRHistory(qrHistory: QRHistoryEntity)

    @Query("DELETE FROM qr_history WHERE id = :id")
    suspend fun deleteQRHistory(id: Long)

    @Query("DELETE FROM qr_history")
    suspend fun clearAllHistory()
}
