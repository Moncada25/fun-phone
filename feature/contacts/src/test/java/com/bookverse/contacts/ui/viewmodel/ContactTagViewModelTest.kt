package com.bookverse.contacts.ui.viewmodel

import com.bookverse.contacts.data.contacts.ContactTagStore
import com.bookverse.contacts.data.model.ContactTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class ContactTagViewModelTest {
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
    fun shouldLoadAssignmentsAndCreateTrimmedTag() = runTest(dispatcher) {
        val family = tag(id = 1, name = "Family")
        val store = FakeContactTagStore(
            tagsByContact = mutableMapOf("contact-1" to mutableListOf(family)),
        )
        val viewModel = ContactTagViewModel(store)

        viewModel.createTag("  Work  ", "#123456")
        advanceUntilIdle()

        assertEquals(listOf(family), viewModel.contactTags.value["contact-1"])
        assertEquals("Work", store.createdTag?.name)
        assertEquals("#123456", store.createdTag?.colorHex)
    }

    @Test
    fun shouldRefreshAssignmentAndFilterMatchingContacts() = runTest(dispatcher) {
        val work = tag(id = 2, name = "Work")
        val store = FakeContactTagStore(
            knownTags = mapOf(work.id to work),
            contactIdsByTag = mapOf(work.id to listOf("contact-2")),
        )
        val viewModel = ContactTagViewModel(store)

        viewModel.assignTagToContact("contact-2", work.id)
        viewModel.toggleFilterTag(work.id)
        advanceUntilIdle()

        assertEquals(listOf(work), viewModel.contactTags.value["contact-2"])
        assertEquals(setOf(work.id), viewModel.selectedFilterTags.value)
        assertEquals(setOf("contact-2"), viewModel.filteredContactIds.value)

        viewModel.clearFilterTags()
        assertNull(viewModel.filteredContactIds.value)
    }

    private fun tag(id: Long, name: String) = ContactTag(id = id, name = name)

    private class FakeContactTagStore(
        private val knownTags: Map<Long, ContactTag> = emptyMap(),
        private val tagsByContact: MutableMap<String, MutableList<ContactTag>> = mutableMapOf(),
        private val contactIdsByTag: Map<Long, List<String>> = emptyMap(),
    ) : ContactTagStore {
        override val allTags = MutableStateFlow(knownTags.values.toList())
        var createdTag: ContactTag? = null

        override suspend fun getAllTagsByContact(): Map<String, List<ContactTag>> =
            tagsByContact.mapValues { it.value.toList() }

        override suspend fun getTagsForContact(contactId: String): List<ContactTag> =
            tagsByContact[contactId]?.toList().orEmpty()

        override suspend fun getContactIdsForTag(tagId: Long): List<String> =
            contactIdsByTag[tagId].orEmpty()

        override suspend fun getContactIdsForTags(tagIds: List<Long>): List<String> =
            tagIds.flatMap { contactIdsByTag[it].orEmpty() }

        override suspend fun createTag(tag: ContactTag): Boolean {
            createdTag = tag
            return true
        }

        override suspend fun updateTag(tag: ContactTag): Boolean = true

        override suspend fun deleteTag(tag: ContactTag) = Unit

        override suspend fun assignTag(contactId: String, tagId: Long) {
            knownTags[tagId]?.let { tag ->
                tagsByContact.getOrPut(contactId) { mutableListOf() }.add(tag)
            }
        }

        override suspend fun removeTagFromContact(contactId: String, tagId: Long) {
            tagsByContact[contactId]?.removeAll { it.id == tagId }
        }
    }
}
