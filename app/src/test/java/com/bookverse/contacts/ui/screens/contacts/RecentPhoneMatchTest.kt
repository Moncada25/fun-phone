package com.bookverse.contacts.ui.screens.contacts

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentPhoneMatchTest {
    @Test
    fun formattedCountryCodeAndLocalNumberMatch() {
        assertTrue(recentNumbersMatch("+57 310-555-0101", "3105550101"))
    }

    @Test
    fun differentNumbersAndBlankInputsDoNotMatch() {
        assertFalse(recentNumbersMatch("3105550101", "3105559999"))
        assertFalse(recentNumbersMatch("", "3105550101"))
        assertFalse(recentNumbersMatch(null, null))
    }
}
