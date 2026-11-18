package com.bookverse.contacts.domain.contacts

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ContactSortPolicyTest {
    private val spanish = Locale.forLanguageTag("es-CO")

    @Test
    fun firstNameOrderIsLocaleAwareAndIgnoresCaseAndAccents() {
        val contacts = listOf(
            contact("1", "Zoe"),
            contact("2", "álvaro"),
            contact("3", "Oscar"),
            contact("4", "Ñu"),
            contact("5", "Alberto"),
            contact("6", "Nora"),
            contact("7", "ana"),
            contact("8", "Émile"),
            contact("9", "Eduardo"),
        )

        val sorted = ContactSortPolicy.sort(contacts, ContactSortOrder.FIRST_NAME, spanish)

        assertEquals(
            listOf("Alberto", "álvaro", "ana", "Eduardo", "Émile", "Nora", "Ñu", "Oscar", "Zoe"),
            sorted.map { it.name },
        )
    }

    @Test
    fun lastNameOrderUsesLastTokenThenFullName() {
        val contacts = listOf(
            contact("1", "Carlos Zapata"),
            contact("2", "Beatriz  Álvarez"),
            contact("3", "Madonna"),
            contact("4", "Ana María Gómez"),
            contact("5", "Zulma Gómez"),
            contact("6", "andrés álvarez"),
        )

        val sorted = ContactSortPolicy.sort(contacts, ContactSortOrder.LAST_NAME, spanish)

        assertEquals(
            listOf(
                "andrés álvarez",
                "Beatriz  Álvarez",
                "Ana María Gómez",
                "Zulma Gómez",
                "Madonna",
                "Carlos Zapata",
            ),
            sorted.map { it.name },
        )
    }

    @Test
    fun contactsWithoutNameGoLastAfterSymbolNames() {
        val contacts = listOf(
            contact("1", "+57 300 111 2233", "+573001112233"),
            contact("2", "", "3000000000"),
            contact("3", "123 Taxi"),
            contact("4", "Bea"),
            contact("5", "3005556677", "300 555 6677"),
            contact("6", "Abel"),
        )

        val byFirstName = ContactSortPolicy.sort(contacts, ContactSortOrder.FIRST_NAME, spanish)
        val byLastName = ContactSortPolicy.sort(contacts, ContactSortOrder.LAST_NAME, spanish)

        assertEquals(listOf("6", "4", "3"), byFirstName.take(3).map { it.id })
        assertEquals(setOf("1", "2", "5"), byFirstName.drop(3).map { it.id }.toSet())
        assertEquals(listOf("6", "4", "3"), byLastName.take(3).map { it.id })
        assertEquals(setOf("1", "2", "5"), byLastName.drop(3).map { it.id }.toSet())
    }

    @Test
    fun sortIsStableForEquivalentNames() {
        val contacts = listOf(
            contact("9", "Ana"),
            contact("3", "ANA"),
            contact("7", "ana"),
            contact("1", "Ana"),
        )

        val sorted = ContactSortPolicy.sort(contacts, ContactSortOrder.FIRST_NAME, Locale.ENGLISH)

        assertEquals(listOf("9", "3", "7", "1"), sorted.map { it.id })
    }

    @Test
    fun recentlyAddedSortsByNumericIdDescendingKeepingUnknownIdsLast() {
        val contacts = listOf(
            contact("9", "Old"),
            contact("120", "Newest"),
            contact("pending", "No numeric id"),
            contact("15", "Middle"),
        )

        val sorted = ContactSortPolicy.sort(contacts, ContactSortOrder.RECENTLY_ADDED, spanish)

        assertEquals(listOf("120", "15", "9", "pending"), sorted.map { it.id })
    }

    @Test
    fun sectionsFollowTheChosenOrder() {
        val contacts = listOf(
            contact("1", "Ana Zapata"),
            contact("2", "Álvaro Gómez"),
            contact("3", "Bruno Álvarez"),
            contact("4", "99 Flores"),
            contact("5", "3001234567", "3001234567"),
        )

        val firstName = ContactSortPolicy.sort(contacts, ContactSortOrder.FIRST_NAME, spanish)
        val lastName = ContactSortPolicy.sort(contacts, ContactSortOrder.LAST_NAME, spanish)
        val recent = ContactSortPolicy.sort(contacts, ContactSortOrder.RECENTLY_ADDED, spanish)

        assertEquals(
            listOf("A" to listOf("2", "1"), "B" to listOf("3"), "#" to listOf("4", "5")),
            ContactSortPolicy.sections(firstName, ContactSortOrder.FIRST_NAME)
                .map { group -> group.label to group.contacts.map { it.id } },
        )
        assertEquals(
            listOf("A" to listOf("3"), "F" to listOf("4"), "G" to listOf("2"), "Z" to listOf("1"), "#" to listOf("5")),
            ContactSortPolicy.sections(lastName, ContactSortOrder.LAST_NAME)
                .map { group -> group.label to group.contacts.map { it.id } },
        )
        assertTrue(ContactSortPolicy.sections(recent, ContactSortOrder.RECENTLY_ADDED).isEmpty())
    }

    @Test
    fun unknownStoredValueFallsBackToFirstName() {
        assertEquals(ContactSortOrder.FIRST_NAME, ContactSortOrder.fromName(null))
        assertEquals(ContactSortOrder.FIRST_NAME, ContactSortOrder.fromName("BY_COMPANY"))
        assertEquals(ContactSortOrder.LAST_NAME, ContactSortOrder.fromName("LAST_NAME"))
        assertEquals(ContactSortOrder.RECENTLY_ADDED, ContactSortOrder.fromName("RECENTLY_ADDED"))
    }

    @Test
    fun namePolicyDetectsPlaceholderNamesAndSplitsLastToken() {
        assertFalse(ContactNamePolicy.hasRealName("", listOf("300")))
        assertFalse(ContactNamePolicy.hasRealName("+57 (300) 111-2233", listOf("+573001112233")))
        assertTrue(ContactNamePolicy.hasRealName("911", listOf("3001112233")))
        assertTrue(ContactNamePolicy.hasRealName("❤️", listOf("300")))
        assertEquals(
            ContactNameParts(givenName = "Juan de la", familyName = "Cruz"),
            ContactNamePolicy.split("  Juan de  la Cruz "),
        )
        assertEquals("Madonna", ContactNamePolicy.lastName("Madonna"))
        assertEquals("", ContactNamePolicy.lastName("   "))
    }

    private fun contact(id: String, name: String, phone: String = "555$id") = Contact(
        id = id,
        name = name,
        phones = listOf(ContactPhone(phone, type = null)),
    )
}
