package com.bookverse.contacts.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedContactDao {

    @Query("SELECT * FROM cached_contacts WHERE isProfile = 0 ORDER BY name COLLATE NOCASE")
    suspend fun getContacts(): List<CachedContactEntity>

    @Query("SELECT * FROM cached_contacts WHERE isProfile = 0 ORDER BY name COLLATE NOCASE")
    fun observeContacts(): Flow<List<CachedContactEntity>>

    @Query("SELECT * FROM cached_contacts WHERE isProfile = 1 LIMIT 1")
    suspend fun getProfile(): CachedContactEntity?

    @Query("SELECT * FROM cached_contacts WHERE isProfile = 1 LIMIT 1")
    fun observeProfile(): Flow<CachedContactEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<CachedContactEntity>)

    @Query("DELETE FROM cached_contacts")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(contacts: List<CachedContactEntity>, profile: CachedContactEntity?) {
        clearAll()
        val data = if (profile != null) contacts + profile else contacts
        if (data.isNotEmpty()) {
            insertAll(data)
        }
    }
}
