package com.bookverse.contacts.ui.screens.contacts.dialer

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DialerContactFilterTest {
    @Test
    fun formattedPhonesMatchDigitsAndKeepContactOrder() {
        val first = contact("first", "+57 310-555-0101")
        val second = contact("second", "(310) 555-0199")
        val unrelated = contact("other", "604-000-0000")

        val matches = filterDialerContacts(
            searchIndex = buildDialerContactSearchIndex(listOf(first, second, unrelated)),
            rawQuery = "310 555 01",
        )

        assertEquals(listOf(first, second), matches.map { it.contact })
        assertEquals("+57 310-555-0101", matches.first().primaryPhone)
    }

    @Test
    fun blankQueryAndNonPositiveLimitReturnNoResults() {
        val index = buildDialerContactSearchIndex(listOf(contact("first", "5550101")))

        assertTrue(filterDialerContacts(index, " *# ").isEmpty())
        assertTrue(filterDialerContacts(index, "555", limit = 0).isEmpty())
    }

    @Test
    fun resultLimitIsEnforcedBeforeLaterMatches() {
        val contacts = (1..5).map { index -> contact("$index", "555-01$index") }

        val matches = filterDialerContacts(
            searchIndex = buildDialerContactSearchIndex(contacts),
            rawQuery = "555",
            limit = 2,
        )

        assertEquals(listOf("1", "2"), matches.map { it.contact.id })
    }

    private fun contact(id: String, phone: String) = Contact(
        id = id,
        name = id,
        phones = listOf(ContactPhone(number = phone, type = null)),
    )
}
