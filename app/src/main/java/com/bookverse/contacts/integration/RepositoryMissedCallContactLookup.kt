package com.bookverse.contacts.integration

import com.bookverse.contacts.data.repository.ContactRepository
import com.bookverse.contacts.service.MissedCallContactLookup

class RepositoryMissedCallContactLookup(
    private val repository: ContactRepository,
) : MissedCallContactLookup {
    override suspend fun findName(number: String): String? {
        val contacts = repository.getContacts()
        return repository.findContactByPhoneNumber(contacts, number)?.name
    }
}
