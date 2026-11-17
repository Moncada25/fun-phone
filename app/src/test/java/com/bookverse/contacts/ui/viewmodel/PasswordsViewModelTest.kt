package com.bookverse.contacts.ui.viewmodel

import com.bookverse.contacts.data.password.PasswordCredential
import com.bookverse.contacts.data.password.PasswordFolder
import com.bookverse.contacts.data.password.PasswordStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PasswordsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun shouldForwardSearchAndExposeCreatedOperation() = runTest(dispatcher) {
        val store = FakePasswordStore()
        val viewModel = PasswordsViewModel(store).also { it.unlock() }
        val credential = credential(id = 0)

        viewModel.setSearch("bank")
        viewModel.upsert(credential)
        advanceUntilIdle()

        assertEquals("bank", viewModel.search.value)
        assertEquals("bank", store.searchQuery)
        assertEquals(credential, store.upserted)
        assertEquals(PasswordOp.CREATED, viewModel.operation.value)

        viewModel.clearOperation()
        assertNull(viewModel.operation.value)
        viewModel.lock()
    }

    @Test
    fun shouldExposeUpdatedOperationAndForwardDeleteAndGet() = runTest(dispatcher) {
        val credential = credential(id = 42)
        val store = FakePasswordStore(stored = credential)
        val viewModel = PasswordsViewModel(store).also { it.unlock() }

        viewModel.upsert(credential)
        viewModel.delete(credential.id)
        advanceUntilIdle()

        assertEquals(PasswordOp.UPDATED, viewModel.operation.value)
        assertEquals(credential.id, store.deletedId)
        assertEquals(credential, viewModel.get(credential.id))
        viewModel.lock()
    }

    @Test
    fun shouldForwardRecycleBinAndFolderOperations() = runTest(dispatcher) {
        val store = FakePasswordStore()
        val viewModel = PasswordsViewModel(store).also { it.unlock() }
        val folder = PasswordFolder(id = 7, name = "Work")

        viewModel.setLastOpenedFolder(folder.id)
        viewModel.saveFolder(folder)
        viewModel.moveToFolder(42, folder.id)
        viewModel.restore(42)
        viewModel.permanentlyDelete(42)
        viewModel.deleteFolder(folder)
        advanceUntilIdle()

        assertEquals(folder.id, viewModel.lastOpenedFolderId)
        assertEquals(folder, store.updatedFolder)
        assertEquals(42L to folder.id, store.moved)
        assertEquals(42L, store.restoredId)
        assertEquals(42L, store.permanentlyDeletedId)
        assertEquals(folder, store.deletedFolder)
        assertEquals(folder.id, store.clearedFolderId)
        viewModel.lock()
    }

    @Test
    fun lockedVaultDoesNotCollectOrExposeCredentials() = runTest(dispatcher) {
        val store = FakePasswordStore(stored = credential(42))
        store.rows.value = listOf(credential(42))
        val viewModel = PasswordsViewModel(store)
        advanceUntilIdle()
        assertEquals(emptyList<PasswordCredential>(), viewModel.credentials.value)
        assertNull(viewModel.get(42))
        org.junit.Assert.assertTrue(runCatching { viewModel.getAllForBackup() }.isFailure)
        org.junit.Assert.assertTrue(runCatching { viewModel.getFoldersForBackup() }.isFailure)
        viewModel.unlock()
        advanceUntilIdle()
        assertEquals(1, viewModel.credentials.value.size)
        assertEquals(store.rows.value, viewModel.getAllForBackup())
        viewModel.lock()
        assertEquals(emptyList<PasswordCredential>(), viewModel.credentials.value)
        store.rows.value = listOf(credential(43))
        advanceUntilIdle()
        assertEquals(emptyList<PasswordCredential>(), viewModel.credentials.value)
        assertNull(viewModel.get(42))
    }

    @Test
    fun corruptRowsExposeRecoverableFailureWithoutLeakingPartialCredentials() = runTest(dispatcher) {
        val viewModel = PasswordsViewModel(FakePasswordStore(corrupt = true))
        viewModel.unlock()
        advanceUntilIdle()
        assertEquals(true, viewModel.loadFailed.value)
        assertEquals(emptyList<PasswordCredential>(), viewModel.credentials.value)
        viewModel.lock()
        assertEquals(false, viewModel.loadFailed.value)
    }

    private fun credential(id: Long) = PasswordCredential(
        id = id,
        name = "Bank",
        username = "user",
        password = "secret",
        note = null,
        updatedAt = 2,
        createdAt = 1,
    )

    private class FakePasswordStore(
        private val stored: PasswordCredential? = null,
        private val corrupt: Boolean = false,
    ) : PasswordStore {
        val rows = MutableStateFlow<List<PasswordCredential>>(emptyList())
        override val credentials: Flow<List<PasswordCredential>> = if (corrupt) flow { error("Invalid synthetic ciphertext") } else rows
        override val deletedCredentials = MutableStateFlow<List<PasswordCredential>>(emptyList())
        override val folders = MutableStateFlow<List<PasswordFolder>>(emptyList())
        var searchQuery: String? = null
        var upserted: PasswordCredential? = null
        var deletedId: Long? = null
        var restoredId: Long? = null
        var permanentlyDeletedId: Long? = null
        var updatedFolder: PasswordFolder? = null
        var deletedFolder: PasswordFolder? = null
        var clearedFolderId: Long? = null
        var moved: Pair<Long, Long?>? = null

        override fun setSearch(query: String) {
            searchQuery = query
        }

        override suspend fun get(id: Long): PasswordCredential? = stored?.takeIf { it.id == id }

        override suspend fun getAllForBackup(): List<PasswordCredential> = rows.value

        override suspend fun getFoldersForBackup(): List<PasswordFolder> = folders.value

        override suspend fun upsert(credential: PasswordCredential) {
            upserted = credential
        }

        override suspend fun markAsDeleted(id: Long) {
            deletedId = id
        }

        override suspend fun restore(id: Long) {
            restoredId = id
        }

        override suspend fun permanentlyDelete(id: Long) {
            permanentlyDeletedId = id
        }

        override suspend fun insertFolder(folder: PasswordFolder): Long = 1L

        override suspend fun updateFolder(folder: PasswordFolder) {
            updatedFolder = folder
        }

        override suspend fun deleteFolder(folder: PasswordFolder) {
            deletedFolder = folder
        }

        override suspend fun clearFolderAssignments(folderId: Long) {
            clearedFolderId = folderId
        }

        override suspend fun moveToFolder(id: Long, folderId: Long?) {
            moved = id to folderId
        }
    }
}
