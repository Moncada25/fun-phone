package com.bookverse.contacts.data.password

import kotlinx.coroutines.flow.Flow

interface PasswordStore {
    val credentials: Flow<List<PasswordCredential>>
    val allCredentials: Flow<List<PasswordCredential>> get() = credentials
    val deletedCredentials: Flow<List<PasswordCredential>>
    val folders: Flow<List<PasswordFolder>>

    fun setSearch(query: String)
    suspend fun get(id: Long): PasswordCredential?
    suspend fun getAllForBackup(): List<PasswordCredential>
    suspend fun getFoldersForBackup(): List<PasswordFolder>
    suspend fun upsert(credential: PasswordCredential)
    suspend fun markAsDeleted(id: Long)
    /** Keeps the pre-folders contract while routing deletion through the recycle bin. */
    suspend fun delete(id: Long) = markAsDeleted(id)
    suspend fun restore(id: Long)
    suspend fun permanentlyDelete(id: Long)
    suspend fun insertFolder(folder: PasswordFolder): Long
    suspend fun updateFolder(folder: PasswordFolder)
    suspend fun deleteFolder(folder: PasswordFolder)
    suspend fun clearFolderAssignments(folderId: Long)
    suspend fun moveToFolder(id: Long, folderId: Long?)
}
