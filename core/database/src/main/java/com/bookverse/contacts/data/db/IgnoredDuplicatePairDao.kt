package com.bookverse.contacts.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bookverse.contacts.data.model.IgnoredDuplicatePair

@Dao
interface IgnoredDuplicatePairDao {

    @Query("SELECT * FROM ignored_duplicate_pairs")
    suspend fun getAll(): List<IgnoredDuplicatePair>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(pair: IgnoredDuplicatePair)

    @Query(
        """
        DELETE FROM ignored_duplicate_pairs
        WHERE (contactId1 = :id1 AND contactId2 = :id2)
           OR (contactId1 = :id2 AND contactId2 = :id1)
        """,
    )
    suspend fun remove(id1: String, id2: String)

    @Query("DELETE FROM ignored_duplicate_pairs")
    suspend fun clearAll()
}
