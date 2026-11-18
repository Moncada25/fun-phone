package com.bookverse.contacts.domain.contacts

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockedNumberMatchPolicyTest {
    @Test
    fun matchesFormattedAndCountryCodeVariantsUsingStableSuffixes() {
        assertTrue(
            BlockedNumberMatchPolicy.matches(
                firstRaw = "+57 310 555 0101",
                firstNormalized = "+573105550101",
                secondRaw = "3105550101",
                secondNormalized = "3105550101",
            ),
        )
    }

    @Test
    fun doesNotSuffixMatchShortNumbers() {
        assertFalse(
            BlockedNumberMatchPolicy.matches(
                firstRaw = "123456",
                firstNormalized = "123456",
                secondRaw = "99123456",
                secondNormalized = "99123456",
            ),
        )
    }

    @Test
    fun blankNumbersNeverMatch() {
        assertFalse(BlockedNumberMatchPolicy.matches(null, null, " ", null))
    }
}
