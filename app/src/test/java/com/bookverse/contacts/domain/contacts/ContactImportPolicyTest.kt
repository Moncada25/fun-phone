package com.bookverse.contacts.domain.contacts

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactAccount
import com.bookverse.contacts.data.model.ContactPhone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactImportPolicyTest {
    private val account = ContactAccount("device", "local")

    @Test
    fun rejectsContactWithoutUsablePhoneNumber() {
        val contact = contact(listOf(" ", ""))

        assertNull(ContactImportPolicy.prepare(contact, account))
    }

    @Test
    fun clearsForeignIdAndNormalizesPhoneList() {
        val prepared = ContactImportPolicy.prepare(
            contact(listOf(" 555 0101 ", "555 0101", "")),
            account,
        )!!

        assertEquals("", prepared.id)
        assertEquals("Imported", prepared.name)
        assertEquals(listOf("555 0101"), prepared.phones.map { it.number })
        assertEquals(listOf(account), prepared.accounts)
    }

    private fun contact(numbers: List<String>) = Contact(
        id = "foreign-id",
        name = " Imported ",
        phones = numbers.map { ContactPhone(it, type = null) },
    )
}
