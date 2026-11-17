package com.bookverse.contacts.domain.contacts

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactInputPolicyTest {
    @Test
    fun sanitizeTrimsFieldsNormalizesCountryCodeAndRemovesDuplicatePhones() {
        val sanitized = ContactInputPolicy.sanitize(
            contact(
                id = "1",
                name = " Ana ",
                numbers = listOf("310 555-0101", "3105550101", " "),
                countryCode = "57",
            ).copy(email = " ana@example.com ", notes = " "),
        )

        assertEquals("Ana", sanitized.name)
        assertEquals(listOf("+573105550101"), sanitized.phones.map { it.number })
        assertEquals("+57", sanitized.phones.single().countryCode)
        assertEquals("ana@example.com", sanitized.email)
        assertNull(sanitized.notes)
    }

    @Test
    fun matchingPrefersPhoneIdentityBeforeNameFallback() {
        val phoneMatch = contact("phone", "Different", listOf("+573105550101"))
        val nameMatch = contact("name", "Ana", listOf("999"))

        assertEquals(
            phoneMatch,
            ContactInputPolicy.findMatch(
                listOf(nameMatch, phoneMatch),
                contact("template", "Ana", listOf("+57 310 555 0101")),
            ),
        )
    }

    @Test
    fun uniquePhoneMatchFindsReaggregatedContact() {
        val reaggregated = contact("114", "Testing", listOf("+57 301 555 0101"))
        val other = contact("7", "Testing", listOf("+573105550199"))

        assertEquals(
            reaggregated,
            ContactInputPolicy.findUniquePhoneMatch(
                listOf(other, reaggregated),
                contact("113", "Testing", listOf("+573015550101")),
            ),
        )
    }

    @Test
    fun uniquePhoneMatchRejectsAmbiguousOrMissingMatches() {
        val first = contact("1", "Ana", listOf("+573015550101"))
        val second = contact("2", "Ana trabajo", listOf("+573015550101"))
        val template = contact("9", "Ana", listOf("+573015550101"))

        assertNull(ContactInputPolicy.findUniquePhoneMatch(listOf(first, second), template))
        assertNull(ContactInputPolicy.findUniquePhoneMatch(emptyList(), template))
        assertNull(
            ContactInputPolicy.findUniquePhoneMatch(listOf(first), contact("9", "Ana", emptyList())),
        )
    }

    private fun contact(
        id: String,
        name: String,
        numbers: List<String>,
        countryCode: String? = null,
    ) = Contact(
        id = id,
        name = name,
        phones = numbers.map { ContactPhone(it, type = null, countryCode = countryCode) },
    )
}
