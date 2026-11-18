package com.bookverse.contacts.domain.contacts

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactSearchPolicyTest {
    @Test
    fun exactAndPrefixNameMatchesRankAheadOfContainsAndPhoneMatches() {
        val contacts = listOf(
            contact("4", "Zed", "555123"),
            contact("3", "Maya Ana", "300"),
            contact("2", "Anabelle", "200"),
            contact("1", "Ana", "100"),
        )

        val result = ContactSearchPolicy.filterAndSort(contacts, "ana")

        assertEquals(listOf("1", "2", "3"), result.map { it.id })
    }

    @Test
    fun phoneSearchAndTagFilterCanBeCombined() {
        val contacts = listOf(
            contact("1", "Ana", "+57 310 555 0101"),
            contact("2", "Ben", "+57 310 555 0102"),
        )

        val result = ContactSearchPolicy.filterAndSort(
            contacts = contacts,
            query = "310",
            allowedContactIds = setOf("2"),
        )

        assertEquals(listOf("2"), result.map { it.id })
    }

    private fun contact(id: String, name: String, number: String) = Contact(
        id = id,
        name = name,
        phones = listOf(ContactPhone(number, type = null)),
    )
}
