package com.bookverse.contacts.domain.passwords

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TotpTest {
    private val times = longArrayOf(59L, 1111111109L, 1111111111L, 1234567890L, 2000000000L, 20000000000L)

    private fun base32(bytes: ByteArray): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val out = StringBuilder()
        var buffer = 0
        var bits = 0
        for (byte in bytes) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bits += 8
            while (bits >= 5) {
                bits -= 5
                out.append(alphabet[(buffer shr bits) and 0x1F])
            }
        }
        if (bits > 0) out.append(alphabet[(buffer shl (5 - bits)) and 0x1F])
        return out.toString()
    }

    private fun seed(length: Int): ByteArray =
        "1234567890".repeat(7).substring(0, length).toByteArray(Charsets.US_ASCII)

    private fun assertVectors(algorithm: TotpAlgorithm, seedLength: Int, expected: List<String>) {
        val config = TotpConfig(secret = base32(seed(seedLength)), digits = 8, algorithm = algorithm)
        expected.forEachIndexed { index, code ->
            assertEquals("t=${times[index]}", code, Totp.code(config, times[index]))
        }
    }

    @Test
    fun rfc6238Sha1Vectors() = assertVectors(
        TotpAlgorithm.SHA1,
        20,
        listOf("94287082", "07081804", "14050471", "89005924", "69279037", "65353130"),
    )

    @Test
    fun rfc6238Sha256Vectors() = assertVectors(
        TotpAlgorithm.SHA256,
        32,
        listOf("46119246", "68084774", "67062674", "91819424", "90698825", "77737706"),
    )

    @Test
    fun rfc6238Sha512Vectors() = assertVectors(
        TotpAlgorithm.SHA512,
        64,
        listOf("90693936", "25091201", "99943326", "93441116", "38618901", "47863826"),
    )

    @Test
    fun sixDigitCodeIsTheTruncatedEightDigitCode() {
        val config = TotpConfig(secret = base32(seed(20)))
        assertEquals("287082", Totp.code(config, 59L))
        assertEquals("081804", Totp.code(config, 1111111109L))
    }

    @Test
    fun remainingSecondsCountsDownWithinThePeriod() {
        val config = TotpConfig(secret = base32(seed(20)))
        assertEquals(30, Totp.remainingSeconds(config, 0L))
        assertEquals(1, Totp.remainingSeconds(config, 59L))
        assertEquals(30, Totp.remainingSeconds(config, 60L))
        assertEquals(15, Totp.remainingSeconds(config.copy(periodSeconds = 60), 45L))
    }

    @Test
    fun base32DecodingIgnoresCaseSpacesDashesAndPadding() {
        assertArrayEquals("foobar".toByteArray(), Totp.decodeBase32("MZXW6YTBOI======"))
        assertArrayEquals("foobar".toByteArray(), Totp.decodeBase32("mzxw 6ytb-oi"))
        assertNull(Totp.decodeBase32("MZXW1"))
        assertNull(Totp.decodeBase32("   "))
    }

    @Test
    fun parsesOtpauthUriWithAllParameters() {
        val config = Totp.parse(
            "otpauth://totp/ACME%20Co:john.doe%40example.com?secret=JBSW Y3DP-EHPK3PXP" +
                "&issuer=ACME%20Co&algorithm=SHA256&digits=8&period=60",
        )
        assertNotNull(config)
        config!!
        assertEquals("JBSWY3DPEHPK3PXP", config.secret)
        assertEquals(8, config.digits)
        assertEquals(60, config.periodSeconds)
        assertEquals(TotpAlgorithm.SHA256, config.algorithm)
        assertEquals("ACME Co", config.issuer)
        assertEquals("john.doe@example.com", config.account)
    }

    @Test
    fun parsesUriDefaultsAndLabelIssuerFallback() {
        val config = Totp.parse("OTPAUTH://TOTP/Example:alice?secret=jbswy3dpehpk3pxp")!!
        assertEquals(TotpConfig("JBSWY3DPEHPK3PXP", 6, 30, TotpAlgorithm.SHA1, "Example", "alice"), config)
        assertEquals("alice", Totp.parse("otpauth://totp/alice?secret=JBSWY3DPEHPK3PXP")!!.account)
        assertNull(Totp.parse("otpauth://totp/alice?secret=JBSWY3DPEHPK3PXP")!!.issuer)
    }

    @Test
    fun parsesRawSecret() {
        assertEquals(TotpConfig("JBSWY3DPEHPK3PXP"), Totp.parse("  jbsw y3dp ehpk 3pxp "))
    }

    @Test
    fun rejectsUnsupportedOrMalformedInput() {
        assertNull(Totp.parse(null))
        assertNull(Totp.parse(""))
        assertNull(Totp.parse("not base32 !"))
        assertNull(Totp.parse("otpauth://hotp/alice?secret=JBSWY3DPEHPK3PXP&counter=1"))
        assertNull(Totp.parse("otpauth://totp/alice"))
        assertNull(Totp.parse("otpauth://totp/alice?issuer=x"))
        assertNull(Totp.parse("otpauth://totp/alice?secret=JBSWY3DPEHPK3PXP&digits=5"))
        assertNull(Totp.parse("otpauth://totp/alice?secret=JBSWY3DPEHPK3PXP&digits=abc"))
        assertNull(Totp.parse("otpauth://totp/alice?secret=JBSWY3DPEHPK3PXP&period=0"))
        assertNull(Totp.parse("otpauth://totp/alice?secret=JBSWY3DPEHPK3PXP&algorithm=MD5"))
    }

    @Test
    fun uriRoundTripPreservesConfiguration() {
        val config = TotpConfig(
            secret = "JBSWY3DPEHPK3PXP",
            digits = 7,
            periodSeconds = 45,
            algorithm = TotpAlgorithm.SHA512,
            issuer = "Café & Co",
            account = "ana maría@example.com",
        )
        assertEquals(config, Totp.parse(Totp.toUri(config)))
        val bare = TotpConfig(secret = "JBSWY3DPEHPK3PXP")
        assertEquals(bare, Totp.parse(Totp.toUri(bare)))
    }
}
