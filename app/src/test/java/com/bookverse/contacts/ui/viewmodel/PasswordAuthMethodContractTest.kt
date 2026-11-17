package com.bookverse.contacts.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PasswordAuthMethodContractTest {
    @Test
    fun shouldExposeOnlyPinAndBiometricAuthenticationMethods() {
        assertEquals(
            setOf(PasswordAuthMethod.BIOMETRIC, PasswordAuthMethod.PIN),
            PasswordAuthMethod.entries.toSet(),
        )
    }

    @Test
    fun shouldRejectUnsupportedAuthenticationNames() {
        assertEquals(PasswordAuthMethod.BIOMETRIC, PasswordAuthMethod.fromName("BIOMETRIC"))
        assertEquals(PasswordAuthMethod.PIN, PasswordAuthMethod.fromName("PIN"))
        assertNull(PasswordAuthMethod.fromName("PASSPHRASE"))
        assertNull(PasswordAuthMethod.fromName("PASSWORD"))
        assertNull(PasswordAuthMethod.fromName(null))
    }
}
