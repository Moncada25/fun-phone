package com.bookverse.contacts.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Versioned PIN verification; this never changes the vault's encryption keys or ciphertext. */
object PinVerifier {
    private const val ITERATIONS = 600_000
    private const val PREFIX = "pbkdf2-sha256"

    fun create(pin: String): String {
        require(pin.matches(Regex("[0-9]{4}")))
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val encoder = Base64.getEncoder()
        return "$PREFIX:$ITERATIONS:${encoder.encodeToString(salt)}:${encoder.encodeToString(derive(pin, salt))}"
    }

    fun isLegacy(encoded: String): Boolean = encoded.matches(Regex("[a-fA-F0-9]{64}"))

    fun verify(pin: String, encoded: String): Boolean {
        if (!pin.matches(Regex("[0-9]{4}"))) return false
        if (isLegacy(encoded)) {
            val hash = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray(Charsets.UTF_8))
            val expected = encoded.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            return MessageDigest.isEqual(hash, expected)
        }
        return runCatching {
            val parts = encoded.split(':')
            if (parts.size != 4 || parts[0] != PREFIX || parts[1] != ITERATIONS.toString()) return false
            val salt = Base64.getDecoder().decode(parts[2])
            val expected = Base64.getDecoder().decode(parts[3])
            salt.size == 16 && expected.size == 32 && MessageDigest.isEqual(derive(pin, salt), expected)
        }.getOrDefault(false)
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
