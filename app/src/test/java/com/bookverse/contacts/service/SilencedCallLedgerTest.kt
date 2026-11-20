package com.bookverse.contacts.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SilencedCallLedgerTest {
    @Test
    fun aMarkedNumberIsSilencedOnce() {
        val ledger = SilencedCallLedger(ttlMillis = 1_000L)

        ledger.mark("3105550101", nowMillis = 0L)

        assertTrue(ledger.consume("3105550101", nowMillis = 10L))
        assertFalse(ledger.consume("3105550101", nowMillis = 20L))
    }

    @Test
    fun theSameSubscriberMatchesWithOrWithoutCountryCode() {
        val ledger = SilencedCallLedger(ttlMillis = 1_000L)

        ledger.mark("+57 310 555 0101", nowMillis = 0L)

        assertTrue(ledger.consume("3105550101", nowMillis = 10L))
    }

    @Test
    fun otherNumbersAreNotSilenced() {
        val ledger = SilencedCallLedger(ttlMillis = 1_000L)

        ledger.mark("3105550101", nowMillis = 0L)

        assertFalse(ledger.consume("3105550199", nowMillis = 10L))
        assertTrue(ledger.consume("3105550101", nowMillis = 20L))
    }

    @Test
    fun expiredVerdictsAreIgnored() {
        val ledger = SilencedCallLedger(ttlMillis = 1_000L)

        ledger.mark("3105550101", nowMillis = 0L)

        assertFalse(ledger.consume("3105550101", nowMillis = 1_000L))
    }
}
