package com.bookverse.contacts.ui.viewmodel

import com.bookverse.contacts.data.contacts.ContactMergeGateway
import com.bookverse.contacts.data.contacts.IgnoredDuplicateStore
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import com.bookverse.contacts.data.model.DuplicateGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class DuplicateContactsViewModelTest {
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
    fun shouldExcludeGroupsContainingAnIgnoredPair() = runTest(dispatcher) {
        val store = FakeIgnoredDuplicateStore(
            ignoredPairs = setOf(setOf("1", "2")),
        )
        val viewModel = DuplicateContactsViewModel(store, FakeContactMergeGateway(), dispatcher)

        viewModel.scanForDuplicates(duplicateContacts())
        advanceUntilIdle()

        assertTrue(viewModel.scanComplete.value)
        assertFalse(viewModel.isScanning.value)
        assertTrue(viewModel.duplicateGroups.value.isEmpty())
    }

    @Test
    fun shouldPersistEveryContactIdBeforeRemovingIgnoredGroup() = runTest(dispatcher) {
        val store = FakeIgnoredDuplicateStore()
        val viewModel = DuplicateContactsViewModel(store, FakeContactMergeGateway(), dispatcher)
        viewModel.scanForDuplicates(duplicateContacts())
        advanceUntilIdle()
        val group = viewModel.duplicateGroups.value.single()

        viewModel.ignoreDuplicateGroup(group)
        advanceUntilIdle()

        assertEquals(listOf("1", "2"), store.ignoredContactIds)
        assertTrue(viewModel.duplicateGroups.value.isEmpty())
    }

    @Test
    fun shouldDelegateMergeAndInvokeCallbackOnlyOnSuccess() = runTest(dispatcher) {
        val merger = FakeContactMergeGateway(mergeResult = true)
        val viewModel = DuplicateContactsViewModel(
            FakeIgnoredDuplicateStore(),
            merger,
            dispatcher,
        )
        viewModel.scanForDuplicates(duplicateContacts())
        advanceUntilIdle()
        val group = viewModel.duplicateGroups.value.single()
        var callbackInvoked = false

        viewModel.mergeContacts(group, primaryContactId = "2") {
            callbackInvoked = true
        }
        advanceUntilIdle()

        assertEquals(group, merger.mergedGroup)
        assertEquals("2", merger.primaryContactId)
        assertTrue(callbackInvoked)
        assertFalse(viewModel.mergeInProgress.value)
        assertTrue(viewModel.duplicateGroups.value.isEmpty())
    }

    private fun duplicateContacts() = listOf(
        contact(id = "1", number = "5551234567"),
        contact(id = "2", number = "5551234567"),
    )

    private fun contact(id: String, number: String) = Contact(
        id = id,
        name = "Contact $id",
        phones = listOf(ContactPhone(number = number, type = null)),
    )

    private class FakeIgnoredDuplicateStore(
        private val ignoredPairs: Set<Set<String>> = emptySet(),
        private val ignoreResult: Boolean = true,
    ) : IgnoredDuplicateStore {
        var ignoredContactIds: List<String>? = null

        override suspend fun loadPairs(): Set<Set<String>> = ignoredPairs

        override suspend fun ignorePairs(contactIds: List<String>): Boolean {
            ignoredContactIds = contactIds
            return ignoreResult
        }
    }

    private class FakeContactMergeGateway(
        private val mergeResult: Boolean = false,
    ) : ContactMergeGateway {
        var mergedGroup: DuplicateGroup? = null
        var primaryContactId: String? = null

        override suspend fun merge(
            group: DuplicateGroup,
            primaryContactId: String,
        ): Boolean {
            mergedGroup = group
            this.primaryContactId = primaryContactId
            return mergeResult
        }
    }
}
