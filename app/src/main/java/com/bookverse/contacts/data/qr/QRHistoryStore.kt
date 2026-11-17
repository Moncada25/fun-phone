package com.bookverse.contacts.data.qr

import kotlinx.coroutines.flow.Flow

interface QRHistoryStore {
    val history: Flow<List<QRHistoryEntity>>
    suspend fun insert(entry: QRHistoryEntity)
    suspend fun delete(id: Long)
    suspend fun clear()
}
