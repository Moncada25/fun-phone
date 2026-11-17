package com.bookverse.contacts.data.repository

import android.content.Context
import com.bookverse.contacts.data.db.AppDatabase
import com.bookverse.contacts.data.model.RecentOperation
import kotlinx.coroutines.flow.Flow

class RecentOperationsRepository(context: Context) {

    private val database by lazy { AppDatabase.getInstance(context) }
    private val recentOperationDao get() = database.recentOperationDao()

    fun getLocalOperations(): Flow<List<RecentOperation>> = recentOperationDao.getLocalOperations()

    suspend fun addRecentOperation(operation: RecentOperation) =
        recentOperationDao.insertOperation(operation)

    suspend fun deleteRecentOperations(contactIds: List<Long>) =
        recentOperationDao.deleteOperationsForContact(contactIds)

    suspend fun deleteRecentOperationsByIds(operationIds: List<Long>) =
        recentOperationDao.deleteOperationsByIds(operationIds)

    suspend fun updateRecentOperation(operation: RecentOperation) =
        recentOperationDao.updateOperation(operation)
}
