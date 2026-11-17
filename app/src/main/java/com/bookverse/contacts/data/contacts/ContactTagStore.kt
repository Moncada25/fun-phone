package com.bookverse.contacts.data.contacts

import com.bookverse.contacts.data.model.ContactTag
import kotlinx.coroutines.flow.Flow

interface ContactTagStore {
    val allTags: Flow<List<ContactTag>>

    suspend fun getAllTagsByContact(): Map<String, List<ContactTag>>
    suspend fun getTagsForContact(contactId: String): List<ContactTag>
    suspend fun getContactIdsForTag(tagId: Long): List<String>
    suspend fun getContactIdsForTags(tagIds: List<Long>): List<String>
    suspend fun createTag(tag: ContactTag): Boolean
    suspend fun updateTag(tag: ContactTag): Boolean
    suspend fun deleteTag(tag: ContactTag)
    suspend fun assignTag(contactId: String, tagId: Long)
    suspend fun removeTagFromContact(contactId: String, tagId: Long)
}
