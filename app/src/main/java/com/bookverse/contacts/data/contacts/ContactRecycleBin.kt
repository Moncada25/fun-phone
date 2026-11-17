package com.bookverse.contacts.data.contacts

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.DeletedContact
import kotlinx.coroutines.flow.Flow

enum class ContactRestoreResult {
    RESTORED,
    RESTORED_WITHOUT_PHOTO,
    FAILED,
}

interface ContactRecycleBin {
    fun observeDeletedContacts(): Flow<List<DeletedContact>>

    suspend fun deleteToBin(contacts: List<Contact>): Boolean

    suspend fun restore(id: Long): ContactRestoreResult

    suspend fun deleteForever(ids: List<Long>): Boolean

    suspend fun purgeExpired(now: Long): Int
}
