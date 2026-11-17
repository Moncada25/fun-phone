package com.bookverse.contacts.domain.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class BackupArchiveTest {
    private val iterations = 10_000
    private val passphrase = "correct horse battery".toCharArray()

    private fun archive(entries: Map<String, ByteArray>, secret: CharArray = passphrase): ByteArray {
        val output = ByteArrayOutputStream()
        BackupArchive.write(output, secret, iterations) { zip ->
            entries.forEach { (name, content) -> BackupArchive.putEntry(zip, name, content) }
        }
        return output.toByteArray()
    }

    private fun readAll(bytes: ByteArray, secret: CharArray = passphrase): Map<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        BackupArchive.read(ByteArrayInputStream(bytes), secret) { name, content ->
            result[name] = content.readBytes()
        }
        return result
    }

    @Test
    fun roundTripRestoresEveryEntryIncludingLargeBinaryOnes() {
        val large = ByteArray(3 * 1024 * 1024 + 17) { (it * 31).toByte() }
        val entries = linkedMapOf(
            "manifest.json" to """{"format":1}""".toByteArray(),
            "passwords.json" to """[{"password":"s3cr3t-ñ-🔐"}]""".toByteArray(),
            "notes/images/a.png" to large,
            "empty.json" to ByteArray(0),
        )
        val restored = readAll(archive(entries))
        assertEquals(entries.keys.toList(), restored.keys.toList())
        entries.forEach { (name, content) -> assertArrayEquals(name, content, restored[name]) }
    }

    @Test
    fun fileNeverContainsPlaintextSecrets() {
        val marker = "hunter2-very-secret-password"
        val bytes = archive(mapOf("passwords.json" to marker.repeat(50).toByteArray()))
        val text = String(bytes, Charsets.ISO_8859_1)
        assertFalse(text.contains(marker))
        assertFalse(text.contains("passwords.json"))
        assertTrue(text.startsWith("FPBACKUP"))
    }

    @Test
    fun wrongPassphraseIsRejectedWithoutYieldingEntries() {
        val bytes = archive(mapOf("passwords.json" to "secret".toByteArray()))
        var delivered = 0
        assertThrows(BackupPassphraseException::class.java) {
            BackupArchive.read(ByteArrayInputStream(bytes), "wrong horse battery".toCharArray()) { _, _ ->
                delivered++
            }
        }
        assertEquals(0, delivered)
    }

    @Test
    fun tamperedCiphertextOrHeaderIsRejected() {
        val bytes = archive(mapOf("a.json" to ByteArray(4096) { 7 }))
        val flippedBody = bytes.copyOf().also { it[it.size - 40] = (it[it.size - 40] + 1).toByte() }
        assertThrows(BackupPassphraseException::class.java) { readAll(flippedBody) }
        val flippedSalt = bytes.copyOf().also { it[20] = (it[20] + 1).toByte() }
        assertThrows(BackupPassphraseException::class.java) { readAll(flippedSalt) }
        val truncated = bytes.copyOf(bytes.size - 30)
        assertThrows(BackupPassphraseException::class.java) { readAll(truncated) }
    }

    @Test
    fun foreignOrUnsupportedFilesAreReportedAsFormatErrors() {
        assertThrows(BackupFormatException::class.java) { readAll("""{"type":"passwords"}""".toByteArray()) }
        assertThrows(BackupFormatException::class.java) { readAll(ByteArray(0)) }
        val wrongVersion = archive(mapOf("a" to byteArrayOf(1))).also { it[8] = 9 }
        assertThrows(BackupFormatException::class.java) { readAll(wrongVersion) }
    }

    @Test
    fun eachBackupUsesAFreshSaltSoOutputsDiffer() {
        val entries = mapOf("a.json" to "same".toByteArray())
        assertFalse(archive(entries).contentEquals(archive(entries)))
    }

    @Test
    fun shortPassphrasesAreRefused() {
        assertFalse(BackupArchive.isPassphraseAcceptable("1234567".toCharArray()))
        assertTrue(BackupArchive.isPassphraseAcceptable("12345678".toCharArray()))
        assertThrows(IllegalArgumentException::class.java) {
            archive(mapOf("a" to byteArrayOf(1)), "short".toCharArray())
        }
    }
}
