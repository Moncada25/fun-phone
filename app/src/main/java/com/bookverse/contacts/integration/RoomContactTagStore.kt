package com.bookverse.contacts.integration

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.bookverse.contacts.data.contacts.ContactTagStore
import com.bookverse.contacts.data.db.AppDatabase
import com.bookverse.contacts.data.model.ContactTag
import com.bookverse.contacts.data.model.ContactTagCrossRef
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog

class RoomContactTagStore(context: Context) : ContactTagStore {
    private val dao = AppDatabase.getInstance(context.applicationContext).contactTagDao()

    override val allTags = dao.observeAllTags()

    override suspend fun getAllTagsByContact(): Map<String, List<ContactTag>> {
        val tagsById = dao.getAllTags().associateBy { it.id }
        return dao.getAllCrossRefs()
            .mapNotNull { crossRef ->
                tagsById[crossRef.tagId]?.let { crossRef.contactId to it }
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second },
            )
    }

    override suspend fun getTagsForContact(contactId: String): List<ContactTag> =
        dao.getTagsForContact(contactId)

    override suspend fun getContactIdsForTag(tagId: Long): List<String> =
        dao.getContactIdsForTag(tagId)

    override suspend fun getContactIdsForTags(tagIds: List<Long>): List<String> =
        dao.getContactIdsForTags(tagIds)

    override suspend fun createTag(tag: ContactTag): Boolean = try {
        dao.insertTag(tag)
        true
    } catch (_: SQLiteConstraintException) {
        DiagnosticLog.warning(DiagnosticEvent.CONTACT_TAG_CREATE_CONFLICT)
        false
    }

    override suspend fun updateTag(tag: ContactTag): Boolean = try {
        dao.updateTag(tag)
        true
    } catch (_: SQLiteConstraintException) {
        DiagnosticLog.warning(DiagnosticEvent.CONTACT_TAG_UPDATE_CONFLICT)
        false
    }

    override suspend fun deleteTag(tag: ContactTag) = dao.deleteTag(tag)

    override suspend fun assignTag(contactId: String, tagId: Long) {
        dao.assignTag(ContactTagCrossRef(contactId = contactId, tagId = tagId))
    }

    override suspend fun removeTagFromContact(contactId: String, tagId: Long) =
        dao.removeTagFromContact(contactId, tagId)
}
