package com.bookverse.contacts.ui.viewmodel

import com.bookverse.contacts.data.contacts.ContactRecycleBin
import com.bookverse.contacts.data.contacts.ContactRestoreResult
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import com.bookverse.contacts.data.model.DeletedContact
import com.bookverse.contacts.domain.contacts.ContactRecycleBinPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class ContactRecycleBinViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val now = 1_800_000_000_000L

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun purgeRemovesOnlyExpiredContacts() = runTest(dispatcher) {
        val bin = FakeRecycleBin(
            entry(1, "Fresh", deletedAt = now - TimeUnit.DAYS.toMillis(29)),
            entry(2, "Expired", deletedAt = now - TimeUnit.DAYS.toMillis(30)),
        )
        val viewModel = ContactRecycleBinViewModel(bin, dispatcher) { now }

        viewModel.purgeExpired()
        advanceUntilIdle()

        assertEquals(listOf("Fresh"), viewModel.deletedContacts.value.map { it.contact.name })
    }

    @Test
    fun restoreRemovesEntryAndReportsPhotoOutcome() = runTest(dispatcher) {
        val bin = FakeRecycleBin(entry(1, "Ana"), entry(2, "Beto"))
        bin.restoreResults[2] = ContactRestoreResult.RESTORED_WITHOUT_PHOTO
        val viewModel = ContactRecycleBinViewModel(bin, dispatcher) { now }
        val events = mutableListOf<ContactRecycleBinEvent>()
        val collector = launch { viewModel.events.collect(events::add) }
        advanceUntilIdle()

        viewModel.restore(viewModel.deletedContacts.value.first { it.id == 1L })
        advanceUntilIdle()
        viewModel.restore(viewModel.deletedContacts.value.first { it.id == 2L })
        advanceUntilIdle()

        assertEquals(
            listOf(
                ContactRecycleBinEvent.Restored("Ana", photoRestored = true),
                ContactRecycleBinEvent.Restored("Beto", photoRestored = false),
            ),
            events,
        )
        assertTrue(viewModel.deletedContacts.value.isEmpty())
        assertFalse(viewModel.isWorking.value)
        collector.cancel()
    }

    @Test
    fun failedRestoreKeepsEntryInBin() = runTest(dispatcher) {
        val bin = FakeRecycleBin(entry(1, "Ana"))
        bin.restoreResults[1] = ContactRestoreResult.FAILED
        val viewModel = ContactRecycleBinViewModel(bin, dispatcher) { now }
        val events = mutableListOf<ContactRecycleBinEvent>()
        val collector = launch { viewModel.events.collect(events::add) }
        advanceUntilIdle()

        viewModel.restore(viewModel.deletedContacts.value.single())
        advanceUntilIdle()

        assertEquals(listOf(ContactRecycleBinEvent.RestoreFailed("Ana")), events)
        assertEquals(1, viewModel.deletedContacts.value.size)
        collector.cancel()
    }

    @Test
    fun deleteForeverAndEmptyBinRemoveEntries() = runTest(dispatcher) {
        val bin = FakeRecycleBin(entry(1, "Ana"), entry(2, "Beto"), entry(3, "Carla"))
        val viewModel = ContactRecycleBinViewModel(bin, dispatcher) { now }
        val events = mutableListOf<ContactRecycleBinEvent>()
        val collector = launch { viewModel.events.collect(events::add) }
        advanceUntilIdle()

        viewModel.deleteForever(viewModel.deletedContacts.value.first { it.id == 2L })
        advanceUntilIdle()
        assertEquals(listOf(1L, 3L), viewModel.deletedContacts.value.map { it.id })

        viewModel.emptyBin()
        advanceUntilIdle()

        assertTrue(viewModel.deletedContacts.value.isEmpty())
        assertEquals(
            listOf(ContactRecycleBinEvent.DeletedForever("Beto"), ContactRecycleBinEvent.Emptied),
            events,
        )
        collector.cancel()
    }

    private fun entry(id: Long, name: String, deletedAt: Long = now) = DeletedContact(
        id = id,
        contact = Contact(id = "", name = name, phones = listOf(ContactPhone("555$id", type = null))),
        deletedAt = deletedAt,
    )

    private class FakeRecycleBin(vararg entries: DeletedContact) : ContactRecycleBin {
        private val state = MutableStateFlow(entries.toList())
        val restoreResults = mutableMapOf<Long, ContactRestoreResult>()

        override fun observeDeletedContacts(): Flow<List<DeletedContact>> = state

        override suspend fun deleteToBin(contacts: List<Contact>): Boolean = true

        override suspend fun restore(id: Long): ContactRestoreResult {
            val result = restoreResults[id] ?: ContactRestoreResult.RESTORED
            if (result != ContactRestoreResult.FAILED) {
                state.update { entries -> entries.filterNot { it.id == id } }
            }
            return result
        }

        override suspend fun deleteForever(ids: List<Long>): Boolean {
            state.update { entries -> entries.filterNot { it.id in ids } }
            return true
        }

        override suspend fun purgeExpired(now: Long): Int {
            val before = state.value.size
            state.update { entries ->
                entries.filterNot { ContactRecycleBinPolicy.isExpired(it.deletedAt, now) }
            }
            return before - state.value.size
        }
    }
}
