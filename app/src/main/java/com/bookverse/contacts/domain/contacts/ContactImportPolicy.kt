package com.bookverse.contacts.domain.contacts

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactAccount

object ContactImportPolicy {
    fun prepare(contact: Contact, account: ContactAccount): Contact? {
        val phones = contact.phones
            .map { it.copy(number = it.number.trim()) }
            .filter { it.number.isNotEmpty() }
            .distinctBy { it.number }
        if (phones.isEmpty()) return null

        return contact.copy(
            id = "",
            name = contact.name.trim(),
            phones = phones,
            accounts = listOf(account),
        )
    }
}
