package com.bookverse.contacts.ui.screens.settings

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeedDialSettingsPolicyTest {
    @Test
    fun shouldSortAllContactsWhenSearchIsEmpty() {
        val contacts = listOf(
            contact(id = "2", name = "Zoe"),
            contact(id = "1", name = "Ana"),
        )

        assertEquals(listOf("Ana", "Zoe"), filterSpeedDialContacts(contacts, "").map(Contact::name))
    }

    @Test
    fun shouldFilterContactNamesIgnoringCase() {
        val contacts = listOf(
            contact(id = "1", name = "Ángela"),
            contact(id = "2", name = "Santiago"),
            contact(id = "3", name = "Andrea"),
        )

        assertEquals(
            listOf("Andrea", "Santiago"),
            filterSpeedDialContacts(contacts, "AN").map(Contact::name),
        )
    }

    @Test
    fun shouldResolveOnlyTheExactAssignedContact() {
        val assigned = contact(id = "contact-2", name = "Bruno")
        val contacts = listOf(contact(id = "contact-1", name = "Ana"), assigned)

        assertEquals(assigned, resolveSpeedDialContact(contacts, "contact-2"))
        assertNull(resolveSpeedDialContact(contacts, "missing"))
        assertNull(resolveSpeedDialContact(contacts, null))
    }

    private fun contact(id: String, name: String) = Contact(
        id = id,
        name = name,
        phones = listOf(ContactPhone(number = "5550101", type = null)),
    )
}
