package com.bookverse.contacts.domain.call

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SipAddressPolicyTest {
    @Test
    fun userAtHostIsASipAddress() {
        assertEquals("alice@sip.example.com", SipAddressPolicy.addressOrNull(" alice@sip.example.com "))
        assertEquals("1001@pbx.local", SipAddressPolicy.addressOrNull("1001@pbx.local"))
    }

    @Test
    fun sipSchemePrefixesAreStrippedCaseInsensitively() {
        assertEquals("alice@sip.example.com", SipAddressPolicy.addressOrNull("sip:alice@sip.example.com"))
        assertEquals("alice@sip.example.com", SipAddressPolicy.addressOrNull("SIPS:alice@sip.example.com"))
        assertEquals("1001", SipAddressPolicy.addressOrNull("sip:1001"))
    }

    @Test
    fun phoneNumbersAndServiceCodesAreNotSipAddresses() {
        for (value in listOf("+57 300 123 4567", "3001234567", "*123#", "*#06#", "")) {
            assertFalse(value, SipAddressPolicy.isSipAddress(value))
        }
    }

    @Test
    fun malformedAddressesAreRejected() {
        for (value in listOf("@host", "alice@", "a@b@c", "sip:", "sip: ", "alice smith@host")) {
            assertNull(value, SipAddressPolicy.addressOrNull(value))
        }
    }

    @Test
    fun dialTargetRoundTripsThroughThePolicy() {
        val target = SipAddressPolicy.dialTarget("alice@sip.example.com")
        assertEquals("sip:alice@sip.example.com", target)
        assertTrue(SipAddressPolicy.isSipAddress(target))
        assertEquals("alice@sip.example.com", SipAddressPolicy.addressOrNull(target))
    }
}
