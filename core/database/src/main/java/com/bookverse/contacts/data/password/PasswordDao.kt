package com.bookverse.contacts.data.password

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {
    @Query("SELECT * FROM passwords WHERE isDeleted = 0 ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<PasswordEntity>>

    @Query("SELECT * FROM passwords WHERE isDeleted = 0 ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllSync(): List<PasswordEntity>

    @Query("SELECT * FROM passwords WHERE isDeleted = 1 ORDER BY name COLLATE NOCASE ASC")
    fun observeDeleted(): Flow<List<PasswordEntity>>

    @Query("SELECT * FROM passwords WHERE id=:id")
    suspend fun getById(id: Long): PasswordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PasswordEntity): Long

    @Update
    suspend fun update(entity: PasswordEntity)

    @Delete
    suspend fun delete(entity: PasswordEntity)

    @Query("SELECT * FROM passwords WHERE isDeleted = 0 AND name LIKE :q ORDER BY name COLLATE NOCASE ASC")
    fun search(q: String): Flow<List<PasswordEntity>>

    @Query("UPDATE passwords SET isDeleted = 1 WHERE id = :id")
    suspend fun markAsDeleted(id: Long)

    @Query("UPDATE passwords SET isDeleted = 0 WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("UPDATE passwords SET folderId = :folderId WHERE id = :id")
    suspend fun moveToFolder(id: Long, folderId: Long?)

    @Query("UPDATE passwords SET folderId = NULL WHERE folderId = :folderId")
    suspend fun clearFolderAssignments(folderId: Long)

    @Query("SELECT * FROM password_folders ORDER BY name COLLATE NOCASE")
    fun observeFolders(): Flow<List<PasswordFolder>>

    @Query("SELECT * FROM password_folders ORDER BY name COLLATE NOCASE")
    suspend fun getFoldersSync(): List<PasswordFolder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: PasswordFolder): Long

    @Update
    suspend fun updateFolder(folder: PasswordFolder)

    @Delete
    suspend fun deleteFolder(folder: PasswordFolder)
}
