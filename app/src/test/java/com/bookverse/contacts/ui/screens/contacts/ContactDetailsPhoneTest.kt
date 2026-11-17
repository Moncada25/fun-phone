package com.bookverse.contacts.ui.screens.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactDetailsPhoneTest {
    @Test
    fun formattingIsRemovedWithoutDroppingCountryPrefix() {
        assertEquals("+573105550101", normalizeContactDetailsPhone("+57 (310) 555-0101"))
    }

    @Test
    fun blankInputHasNoIdentity() {
        assertNull(normalizeContactDetailsPhone(null))
        assertNull(normalizeContactDetailsPhone("  "))
    }
}
