package com.bookverse.contacts.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.security.GeneralSecurityException

@RunWith(AndroidJUnit4::class)
class CryptoManagerTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun androidKeystoreRoundTripUsesRandomizedAuthenticatedEncryption() {
        val plaintext = "synthetic-vault-value".toByteArray()

        val firstCiphertext = requireNotNull(CryptoManager.encrypt(context, plaintext))
        val secondCiphertext = requireNotNull(CryptoManager.encrypt(context, plaintext))

        assertFalse(firstCiphertext.contentEquals(secondCiphertext))
        assertArrayEquals(plaintext, CryptoManager.decrypt(context, firstCiphertext))
        assertArrayEquals(plaintext, CryptoManager.decrypt(context, secondCiphertext))
    }

    @Test
    fun nullAndTruncatedInputsDoNotProducePlaintext() {
        assertNull(CryptoManager.encrypt(context, null))
        assertNull(CryptoManager.decrypt(context, null))
        assertNull(CryptoManager.decrypt(context, byteArrayOf(1, 2, 3)))
    }

    @Test
    fun tamperedCiphertextFailsAuthentication() {
        val ciphertext = requireNotNull(
            CryptoManager.encrypt(context, "synthetic-vault-value".toByteArray()),
        )
        ciphertext[ciphertext.lastIndex] = (ciphertext.last() + 1).toByte()

        assertThrows(GeneralSecurityException::class.java) {
            CryptoManager.decrypt(context, ciphertext)
        }
    }
}
