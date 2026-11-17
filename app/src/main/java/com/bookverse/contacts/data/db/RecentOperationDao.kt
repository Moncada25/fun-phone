package com.bookverse.contacts.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bookverse.contacts.data.model.RecentOperation
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentOperationDao {
    @Query("SELECT * FROM recent_operations ORDER BY date DESC")
    fun getLocalOperations(): Flow<List<RecentOperation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: RecentOperation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperations(operations: List<RecentOperation>)

    @Query("DELETE FROM recent_operations")
    suspend fun clearAll()

    @Query("DELETE FROM recent_operations WHERE contact_id IN(:contactIds)")
    suspend fun deleteOperationsForContact(contactIds: List<Long>)

    @Query("DELETE FROM recent_operations WHERE id IN (:operationIds)")
    suspend fun deleteOperationsByIds(operationIds: List<Long>)

    @Query("DELETE FROM recent_operations WHERE contact_id = '' AND contact_phones LIKE '%' || :phoneNumber || '%'")
    suspend fun deleteOperationsForUnknownContact(phoneNumber: String)

    @Update
    suspend fun updateOperation(operation: RecentOperation)
}
