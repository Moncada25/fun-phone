package com.bookverse.contacts.ui.screens.contacts.dialer

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedDialResolverTest {
    @Test
    fun resolvesConfiguredContactPhone() {
        assertEquals(
            SpeedDialResolution.Dial("5550101"),
            resolveSpeedDial(
                key = 2,
                assignments = mapOf(2 to "contact-1"),
                contacts = listOf(contact("contact-1", "5550101")),
            ),
        )
    }

    @Test
    fun distinguishesMissingAssignmentFromUnavailableContact() {
        assertEquals(
            SpeedDialResolution.NotConfigured,
            resolveSpeedDial(2, emptyMap(), emptyList()),
        )
        assertEquals(
            SpeedDialResolution.ContactUnavailable,
            resolveSpeedDial(2, mapOf(2 to "missing"), emptyList()),
        )
    }

    private fun contact(id: String, number: String) = Contact(
        id = id,
        name = "Synthetic",
        phones = listOf(ContactPhone(number, type = null)),
    )
}
