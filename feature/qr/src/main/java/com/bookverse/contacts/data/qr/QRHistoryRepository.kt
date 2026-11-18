package com.bookverse.contacts.data.qr

import kotlinx.coroutines.flow.Flow

class QRHistoryRepository(private val qrHistoryDao: QRHistoryDao) : QRHistoryStore {
    override val history: Flow<List<QRHistoryEntity>> = qrHistoryDao.getAllQRHistory()

    override suspend fun insert(entry: QRHistoryEntity) {
        qrHistoryDao.insertQRHistory(entry)
    }

    override suspend fun delete(id: Long) {
        qrHistoryDao.deleteQRHistory(id)
    }

    override suspend fun clear() {
        qrHistoryDao.clearAllHistory()
    }
}
