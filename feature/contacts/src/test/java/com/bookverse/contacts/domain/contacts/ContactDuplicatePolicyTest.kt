package com.bookverse.contacts.domain.contacts

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactDuplicatePolicyTest {

    @Test
    fun matchesSameNormalizedNameWithAtLeastOneSharedPhone() {
        val existing = contact("José  Núñez", "+57 300 123 4567", "6015550100")

        assertTrue(
            ContactDuplicatePolicy.isDuplicate(contact("jose nunez", "(300) 123-4567"), existing),
        )
        assertTrue(
            ContactDuplicatePolicy.isDuplicate(
                contact("JOSÉ NÚÑEZ", "3009998877", "601 555 0100"),
                existing,
            ),
        )
    }

    @Test
    fun keepsContactsThatShareOnlyTheNameOrOnlyThePhone() {
        val existing = contact("José Núñez", "3001234567")

        assertFalse(ContactDuplicatePolicy.isDuplicate(contact("José Núñez", "3007654321"), existing))
        assertFalse(ContactDuplicatePolicy.isDuplicate(contact("Josefa Núñez", "3001234567"), existing))
    }

    @Test
    fun matchesNamelessContactOnlyWhenPhoneSetIsIdentical() {
        val existing = contact("Oficina", "3001234567", "6015550100")

        assertTrue(
            ContactDuplicatePolicy.isDuplicate(contact("", "601 555 0100", "+573001234567"), existing),
        )
        assertFalse(ContactDuplicatePolicy.isDuplicate(contact("", "3001234567"), existing))
        assertTrue(
            ContactDuplicatePolicy.isDuplicate(
                contact("+57 300 123 4567", "3001234567"),
                contact("Mamá", "+573001234567"),
            ),
        )
    }

    @Test
    fun neverTreatsContactsWithoutPhonesAsDuplicates() {
        assertFalse(ContactDuplicatePolicy.isDuplicate(contact("Ana"), contact("Ana")))
    }

    @Test
    fun planSkipsDuplicatesOfExistingAndOfEarlierIncomingContacts() {
        val existing = listOf(contact("Ana", "3000000001"))
        val incoming = listOf(
            contact("Ana", "3000000001"),
            contact("Beto", "3000000002"),
            contact("beto", "+57 300 000 0002"),
            contact("Carla", "3000000003"),
            contact("Ana", "3000000009"),
        )

        val plan = ContactDuplicatePolicy.plan(existing, incoming) { it }

        assertEquals(listOf("Beto", "Carla", "Ana"), plan.unique.map { it.name })
        assertEquals(2, plan.duplicates)
    }

    private fun contact(name: String, vararg numbers: String) = Contact(
        id = "",
        name = name,
        phones = numbers.map { ContactPhone(it, type = null) },
    )
}
