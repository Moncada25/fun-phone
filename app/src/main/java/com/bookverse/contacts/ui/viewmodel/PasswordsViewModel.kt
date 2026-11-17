package com.bookverse.contacts.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookverse.contacts.data.password.PASSWORD_FOLDER_FILTER_ALL
import com.bookverse.contacts.data.password.PasswordCredential
import com.bookverse.contacts.data.password.PasswordFolder
import com.bookverse.contacts.data.password.PasswordStore
import com.bookverse.contacts.domain.passwords.PasswordReusePolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class PasswordOp { CREATED, UPDATED }

class PasswordsViewModel(
    private val store: PasswordStore,
) : ViewModel() {
    private var lastOpenedFolderInternal: Long = PASSWORD_FOLDER_FILTER_ALL

    val lastOpenedFolderId: Long
        get() = lastOpenedFolderInternal

    fun setLastOpenedFolder(folderId: Long) {
        lastOpenedFolderInternal = folderId
    }

    private val _credentials = MutableStateFlow<List<PasswordCredential>>(emptyList())
    val credentials = _credentials.asStateFlow()
    private val _deletedCredentials = MutableStateFlow<List<PasswordCredential>>(emptyList())
    val deletedCredentials = _deletedCredentials.asStateFlow()
    private val _folders = MutableStateFlow<List<PasswordFolder>>(emptyList())
    val folders = _folders.asStateFlow()
    private val _reusedPasswordIds = MutableStateFlow<Set<Long>>(emptySet())
    val reusedPasswordIds = _reusedPasswordIds.asStateFlow()
    private var unfilteredCredentials: List<PasswordCredential> = emptyList()
    private var sessionJob: Job? = null
    private var isUnlocked = false
    private val _loadFailed = MutableStateFlow(false)
    val loadFailed = _loadFailed.asStateFlow()

    fun unlock() {
        if (sessionJob?.isActive == true) return
        isUnlocked = true
        _loadFailed.value = false
        sessionJob = viewModelScope.launch {
            try {
                coroutineScope {
                    launch { store.credentials.collect { _credentials.value = it } }
                    launch { store.deletedCredentials.collect { _deletedCredentials.value = it } }
                    launch { store.folders.collect { _folders.value = it } }
                    launch {
                        store.allCredentials.collect { all ->
                            unfilteredCredentials = all
                            _reusedPasswordIds.value = PasswordReusePolicy.reusedIds(all)
                        }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _credentials.value = emptyList()
                _deletedCredentials.value = emptyList()
                _folders.value = emptyList()
                clearReuseState()
                _loadFailed.value = true
            }
        }
    }

    fun lock() {
        isUnlocked = false
        _loadFailed.value = false
        sessionJob?.cancel()
        sessionJob = null
        _credentials.value = emptyList()
        _deletedCredentials.value = emptyList()
        _folders.value = emptyList()
        clearReuseState()
        _search.value = ""
        store.setSearch("")
        _operation.value = null
    }

    private fun clearReuseState() {
        unfilteredCredentials = emptyList()
        _reusedPasswordIds.value = emptySet()
    }

    fun isPasswordReused(password: String, ownId: Long?): Boolean =
        isUnlocked && PasswordReusePolicy.isReused(password, ownId, unfilteredCredentials)

    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()
    private val _operation = MutableStateFlow<PasswordOp?>(null)
    val operation: StateFlow<PasswordOp?> = _operation.asStateFlow()

    fun setSearch(q: String) {
        _search.value = q
        store.setSearch(q)
    }

    fun upsert(cred: PasswordCredential) {
        viewModelScope.launch {
            if (!isUnlocked) return@launch
            val isCreate = cred.id == 0L
            store.upsert(cred)
            _operation.value = if (isCreate) PasswordOp.CREATED else PasswordOp.UPDATED
        }
    }

    fun clearOperation() {
        _operation.value = null
    }

    fun delete(id: Long) {
        viewModelScope.launch { if (isUnlocked) store.markAsDeleted(id) }
    }

    fun restore(id: Long) {
        viewModelScope.launch { if (isUnlocked) store.restore(id) }
    }

    fun permanentlyDelete(id: Long) {
        viewModelScope.launch { if (isUnlocked) store.permanentlyDelete(id) }
    }

    fun saveFolder(folder: PasswordFolder) = viewModelScope.launch {
        if (!isUnlocked) return@launch
        val prepared = if (folder.id == 0L) {
            folder.copy(createdAt = folder.createdAt.takeIf { it != 0L } ?: System.currentTimeMillis())
        } else {
            folder
        }
        if (prepared.id == 0L) store.insertFolder(prepared) else store.updateFolder(prepared)
    }

    fun deleteFolder(folder: PasswordFolder) = viewModelScope.launch {
        if (!isUnlocked) return@launch
        store.deleteFolder(folder)
        store.clearFolderAssignments(folder.id)
    }

    fun moveToFolder(id: Long, folderId: Long?) = viewModelScope.launch {
        if (!isUnlocked) return@launch
        store.moveToFolder(id, folderId)
    }

    suspend fun get(id: Long) = if (isUnlocked) store.get(id) else null

    suspend fun getAllForBackup(): List<PasswordCredential> {
        check(isUnlocked)
        return store.getAllForBackup()
    }

    suspend fun getFoldersForBackup(): List<PasswordFolder> {
        check(isUnlocked)
        return store.getFoldersForBackup()
    }

    suspend fun importFolder(folder: PasswordFolder): Long {
        check(isUnlocked)
        return store.insertFolder(folder)
    }

    suspend fun importCredential(credential: PasswordCredential) {
        check(isUnlocked)
        store.upsert(credential)
    }
}
