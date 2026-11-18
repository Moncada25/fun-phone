package com.bookverse.contacts.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bookverse.contacts.data.model.ContactTag
import com.bookverse.contacts.data.model.ContactTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactTagDao {

    @Query("SELECT * FROM contact_tags ORDER BY name COLLATE NOCASE")
    fun observeAllTags(): Flow<List<ContactTag>>

    @Query("SELECT * FROM contact_tags ORDER BY name COLLATE NOCASE")
    suspend fun getAllTags(): List<ContactTag>

    @Query("""
        SELECT ct.* FROM contact_tags ct
        INNER JOIN contact_tag_cross_ref ref ON ct.id = ref.tagId
        WHERE ref.contactId = :contactId
        ORDER BY ct.name COLLATE NOCASE
    """)
    fun observeTagsForContact(contactId: String): Flow<List<ContactTag>>

    @Query("""
        SELECT ct.* FROM contact_tags ct
        INNER JOIN contact_tag_cross_ref ref ON ct.id = ref.tagId
        WHERE ref.contactId = :contactId
        ORDER BY ct.name COLLATE NOCASE
    """)
    suspend fun getTagsForContact(contactId: String): List<ContactTag>

    @Query("SELECT contactId FROM contact_tag_cross_ref WHERE tagId = :tagId")
    suspend fun getContactIdsForTag(tagId: Long): List<String>

    @Query("SELECT contactId FROM contact_tag_cross_ref WHERE tagId IN (:tagIds)")
    suspend fun getContactIdsForTags(tagIds: List<Long>): List<String>

    @Query("SELECT * FROM contact_tag_cross_ref")
    suspend fun getAllCrossRefs(): List<ContactTagCrossRef>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTag(tag: ContactTag): Long

    @Update
    suspend fun updateTag(tag: ContactTag)

    @Delete
    suspend fun deleteTag(tag: ContactTag)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun assignTag(crossRef: ContactTagCrossRef)

    @Query("DELETE FROM contact_tag_cross_ref WHERE contactId = :contactId AND tagId = :tagId")
    suspend fun removeTagFromContact(contactId: String, tagId: Long)

    @Query("DELETE FROM contact_tag_cross_ref WHERE contactId = :contactId")
    suspend fun removeAllTagsForContact(contactId: String)
}
