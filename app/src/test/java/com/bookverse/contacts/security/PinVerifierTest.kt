package com.bookverse.contacts.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinVerifierTest {
    @Test
    fun verifiesIndependentPbkdf2VectorAndRejectsWrongPin() {
        val encoded = "pbkdf2-sha256:600000:AAAAAAAAAAAAAAAAAAAAAA==:wVDWV9+AFAJG20zLqGKu3Kop6PjRKttVKwf1bXxJnvM="
        assertTrue(PinVerifier.verify("1234", encoded))
        assertFalse(PinVerifier.verify("4321", encoded))
        assertFalse(PinVerifier.verify("1234", encoded.replace("600000", "1")))
        assertFalse(PinVerifier.verify("1234", "damaged"))
    }

    @Test
    fun usesIndependentRandomSaltAndAcceptsLegacyWithoutChangingThePin() {
        val first = PinVerifier.create("1234")
        assertNotEquals(first, PinVerifier.create("1234"))
        assertTrue(PinVerifier.verify("1234", first))
        assertTrue(PinVerifier.verify("1234", "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4"))
        assertFalse(PinVerifier.verify("12345", first))
    }
}
