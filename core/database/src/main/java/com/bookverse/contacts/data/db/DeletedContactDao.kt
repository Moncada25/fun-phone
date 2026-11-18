package com.bookverse.contacts.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeletedContactDao {

    @Query("SELECT * FROM deleted_contacts ORDER BY deletedAt DESC, id DESC")
    fun observeAll(): Flow<List<DeletedContactEntity>>

    @Query("SELECT * FROM deleted_contacts")
    suspend fun getAll(): List<DeletedContactEntity>

    @Query("SELECT * FROM deleted_contacts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DeletedContactEntity?

    @Query("SELECT * FROM deleted_contacts WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<DeletedContactEntity>

    @Insert
    suspend fun insert(contact: DeletedContactEntity): Long

    @Query("DELETE FROM deleted_contacts WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
