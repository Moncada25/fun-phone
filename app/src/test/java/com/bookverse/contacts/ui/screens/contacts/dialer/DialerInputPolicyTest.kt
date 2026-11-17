package com.bookverse.contacts.ui.screens.contacts.dialer

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import org.junit.Assert.assertEquals
import org.junit.Test

class DialerInputPolicyTest {
    @Test
    fun pasteRemovesFormattingAndKeepsTheLogicalCursor() {
        assertEquals(
            DialerEditValue(text = "+15550199", selectionStart = 7),
            sanitizePastedDialerText(
                rawText = "+1 (555) 01-99 ext",
                rawCursor = 11,
            ),
        )
    }

    @Test
    fun internationalNumbersKeepTheirPrefixDuringEditingAndCalling() {
        assertEquals("+1555010099", sanitizeDialerText(" +1 (555) 010-099"))
        assertEquals("+1555010099", sanitizeDialerText("++1 (555)+010-099"))
        assertEquals(
            DialerEditValue("+15550100997", 12),
            insertDialerText(DialerEditValue("+1555010099", 11), "7"),
        )
        assertEquals(
            DialerActionResolution.Execute("+1555010099"),
            resolveDialerAction("+1555010099", null, "+57"),
        )
        assertEquals(
            DialerActionResolution.RestoreLastDialed("+1555010099"),
            resolveDialerAction("", "+1555010099", "+57"),
        )
    }

    @Test
    fun insertReplacesTheSelectedRangeAndRejectsOverflow() {
        assertEquals(
            DialerEditValue(text = "12*#56", selectionStart = 4),
            insertDialerText(
                current = DialerEditValue("123456", selectionStart = 2, selectionEnd = 4),
                insertedText = "*#",
            ),
        )
        assertEquals(
            DialerEditValue("1234", selectionStart = 4),
            insertDialerText(
                current = DialerEditValue("1234", selectionStart = 4),
                insertedText = "5",
                maxLength = 4,
            ),
        )
    }

    @Test
    fun deleteHandlesSelectionCursorAndLongPressClear() {
        assertEquals(
            DialerEditValue("145", selectionStart = 1),
            deleteDialerText(DialerEditValue("12345", 1, 3)),
        )
        assertEquals(
            DialerEditValue("145", selectionStart = 1),
            deleteDialerText(DialerEditValue("1245", 2)),
        )
        assertEquals(
            DialerEditValue("", selectionStart = 0),
            deleteDialerText(DialerEditValue("145", 3), deleteAll = true),
        )
    }

    @Test
    fun actionRestoresLastNumberBeforeExecutingAndPrefixesOrdinaryCalls() {
        assertEquals(
            DialerActionResolution.RestoreLastDialed("5550100"),
            resolveDialerAction("", "555-0100", "+57"),
        )
        assertEquals(
            DialerActionResolution.Execute("+575550100"),
            resolveDialerAction("555-0100", "ignored", "+57"),
        )
        assertEquals(
            DialerActionResolution.Execute("*123#"),
            resolveDialerAction("*123#", null, "+57"),
        )
        assertEquals(
            DialerActionResolution.Empty,
            resolveDialerAction("", null, "+57"),
        )
    }

    @Test
    fun callingCodeIsShownOnlyWhenItWillBeDialed() {
        assertEquals("+57", dialerCallingCodePrefix("", "+57"))
        assertEquals("+57", dialerCallingCodePrefix("310 555 0101", "+57"))
        assertEquals(null, dialerCallingCodePrefix("+13105550101", "+57"))
        assertEquals(null, dialerCallingCodePrefix("*123#", "+57"))
        assertEquals(null, dialerCallingCodePrefix("#31#", "+57"))
        assertEquals(null, dialerCallingCodePrefix("3105550101", null))
        assertEquals(null, dialerCallingCodePrefix("3105550101", " "))
        assertEquals("+573105550101", buildDialerTarget("310 555 0101", "+57"))
        assertEquals("+13105550101", buildDialerTarget("+13105550101", "+57"))
    }

    @Test
    fun shortNumbersAreDialedWithoutCallingCode() {
        assertEquals(null, dialerCallingCodePrefix("123", "+57"))
        assertEquals(null, dialerCallingCodePrefix("555 010", "+57"))
        assertEquals("+57", dialerCallingCodePrefix("555 0101", "+57"))
        assertEquals("123", buildDialerTarget("123", "+57"))
        assertEquals("+575550101", buildDialerTarget("555 0101", "+57"))
    }

    @Test
    fun exactContactMatchAcceptsFormattingAndCountryCodeDifferences() {
        val codes = setOf("57", "1", "44")
        val maria = dialerContact("Maria", "+57 310 555 0101")
        val bruno = dialerContact("Bruno", "(320) 555-0199")
        val contacts = listOf(maria, bruno)

        assertEquals(maria, findExactDialerContact("3105550101", contacts, codes))
        assertEquals(maria, findExactDialerContact("+573105550101", contacts, codes))
        assertEquals(bruno, findExactDialerContact("+13205550199", contacts, codes))
        assertEquals(bruno, findExactDialerContact("3205550199", contacts, codes))
    }

    @Test
    fun partialNumbersAndServiceCodesNeverMatchAContact() {
        val codes = setOf("57", "1")
        val contacts = listOf(
            dialerContact("Maria", "+57 310 555 0101"),
            dialerContact("Service", "123"),
        )

        assertEquals(null, findExactDialerContact("310555", contacts, codes))
        assertEquals(null, findExactDialerContact("5550101", contacts, codes))
        assertEquals(null, findExactDialerContact("+993105550101", contacts, codes))
        assertEquals(null, findExactDialerContact("*123#", contacts, codes))
        assertEquals(null, findExactDialerContact("", contacts, codes))
        assertEquals("Service", findExactDialerContact("123", contacts, codes)?.name)
    }

    private fun dialerContact(name: String, number: String) = Contact(
        id = name,
        name = name,
        phones = listOf(ContactPhone(number = number, type = null)),
    )
}
