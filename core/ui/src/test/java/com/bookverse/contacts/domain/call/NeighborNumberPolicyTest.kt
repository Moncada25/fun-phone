package com.bookverse.contacts.domain.call

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NeighborNumberPolicyTest {
    private val own = listOf("+57 310 555 0101")

    @Test
    fun aNumberThatDiffersOnlyInTheLastFourDigitsResemblesTheOwnNumber() {
        assertTrue(NeighborNumberPolicy.resemblesOwnNumber("3105559876", own))
        assertTrue(NeighborNumberPolicy.resemblesOwnNumber("+573105559876", own))
    }

    @Test
    fun theOwnNumberCallingItselfIsAlsoSuspicious() {
        assertTrue(NeighborNumberPolicy.resemblesOwnNumber("3105550101", own))
    }

    @Test
    fun aDifferentBlockOrCarrierPrefixDoesNotMatch() {
        assertFalse(NeighborNumberPolicy.resemblesOwnNumber("3105569876", own))
        assertFalse(NeighborNumberPolicy.resemblesOwnNumber("3205559876", own))
    }

    @Test
    fun anotherCountryWithTheSameLocalDigitsDoesNotMatch() {
        assertFalse(NeighborNumberPolicy.resemblesOwnNumber("+1 310 555 9876", own))
    }

    @Test
    fun shortNumbersAndServiceCodesNeverMatch() {
        assertFalse(NeighborNumberPolicy.resemblesOwnNumber("5559876", own))
        assertFalse(NeighborNumberPolicy.resemblesOwnNumber("123", listOf("123")))
        assertFalse(NeighborNumberPolicy.resemblesOwnNumber("3105559876", listOf("5550101")))
        assertFalse(NeighborNumberPolicy.resemblesOwnNumber(null, own))
    }

    @Test
    fun anyOfSeveralOwnNumbersCanMatch() {
        val numbers = listOf("+57 310 555 0101", "+57 601 444 2020")

        assertTrue(NeighborNumberPolicy.resemblesOwnNumber("6014449999", numbers))
        assertFalse(NeighborNumberPolicy.resemblesOwnNumber("6014459999", numbers))
    }

    @Test
    fun trunkPrefixZeroIsIgnored() {
        val ukNumber = listOf("07700 900123")

        assertTrue(NeighborNumberPolicy.resemblesOwnNumber("+44 7700 909999", ukNumber))
    }

    @Test
    fun withoutOwnNumbersNothingMatches() {
        assertFalse(NeighborNumberPolicy.resemblesOwnNumber("3105559876", emptyList()))
    }
}
