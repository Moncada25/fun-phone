package com.bookverse.contacts.domain.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockedNumberInputPolicyTest {
    @Test
    fun formattedNumbersAreStoredAsDigitsWithAnOptionalPlus() {
        assertEquals(
            BlockedNumberInputResult.Valid("+573105550101"),
            BlockedNumberInputPolicy.validate(" +57 (310) 555-0101 ", emptyList()),
        )
        assertEquals(
            BlockedNumberInputResult.Valid("3105550101"),
            BlockedNumberInputPolicy.validate("310.555.0101", emptyList()),
        )
    }

    @Test
    fun emptyAndMalformedInputIsRejected() {
        assertEquals(BlockedNumberInputResult.Empty, BlockedNumberInputPolicy.validate("   ", emptyList()))
        assertEquals(
            BlockedNumberInputResult.InvalidCharacters,
            BlockedNumberInputPolicy.validate("310-CALL-ME", emptyList()),
        )
        assertEquals(
            BlockedNumberInputResult.InvalidCharacters,
            BlockedNumberInputPolicy.validate("310+5550101", emptyList()),
        )
        assertEquals(
            BlockedNumberInputResult.InvalidCharacters,
            BlockedNumberInputPolicy.validate("*123#", emptyList()),
        )
    }

    @Test
    fun lengthLimitsCountDigitsOnly() {
        assertEquals(BlockedNumberInputResult.TooShort, BlockedNumberInputPolicy.validate("+ 1 2", emptyList()))
        assertEquals(BlockedNumberInputResult.Valid("123"), BlockedNumberInputPolicy.validate("1-2-3", emptyList()))
        assertEquals(
            BlockedNumberInputResult.TooLong,
            BlockedNumberInputPolicy.validate("1".repeat(BlockedNumberInputPolicy.MAX_DIGITS + 1), emptyList()),
        )
        assertEquals(
            BlockedNumberInputPolicy.MAX_INPUT_LENGTH,
            BlockedNumberInputPolicy.limitInput("9".repeat(80)).length,
        )
    }

    @Test
    fun duplicatesAreDetectedAcrossFormatsAndCountryCodes() {
        val existing = listOf("+57 310 555 0101", null, "")

        assertEquals(
            BlockedNumberInputResult.Duplicate,
            BlockedNumberInputPolicy.validate("3105550101", existing),
        )
        assertEquals(
            BlockedNumberInputResult.Duplicate,
            BlockedNumberInputPolicy.validate("+573105550101", existing),
        )
        assertEquals(
            BlockedNumberInputResult.Valid("3205550101"),
            BlockedNumberInputPolicy.validate("3205550101", existing),
        )
    }

    @Test
    fun sameSubscriberIgnoresCountryCodeAndTrunkPrefixOnly() {
        assertTrue(BlockedNumberMatchPolicy.isSameSubscriber("+44 7911 123456", "07911 123456"))
        assertTrue(BlockedNumberMatchPolicy.isSameSubscriber("3105550101", "+573105550101"))
        assertTrue(BlockedNumberMatchPolicy.isSameSubscriber("555-0101", "5550101"))
        assertFalse(BlockedNumberMatchPolicy.isSameSubscriber("3105550101", "3205550101"))
        assertFalse(BlockedNumberMatchPolicy.isSameSubscriber("0101", "3105550101"))
        assertFalse(BlockedNumberMatchPolicy.isSameSubscriber(null, "3105550101"))
        assertFalse(BlockedNumberMatchPolicy.isSameSubscriber("", ""))
    }
}
