package com.bookverse.contacts.domain.backup

import com.google.crypto.tink.subtle.AesGcmHkdfStreaming
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class BackupFormatException(message: String) : IOException(message)

class BackupPassphraseException : IOException("Wrong passphrase or damaged backup")

object BackupArchive {
    const val MIN_PASSPHRASE_LENGTH = 8
    const val DEFAULT_ITERATIONS = 600_000
    const val FILE_EXTENSION = "funphonebackup"
    const val MIME_TYPE = "application/octet-stream"

    private val MAGIC = "FPBACKUP".toByteArray(Charsets.US_ASCII)
    private const val FORMAT_VERSION = 1
    private const val SALT_BYTES = 16
    private const val KEY_BYTES = 32
    private const val SEGMENT_BYTES = 1 shl 20
    private const val HEADER_BYTES = 8 + 1 + 4 + SALT_BYTES
    private val ITERATION_RANGE = 10_000..5_000_000
    private val secureRandom = SecureRandom()

    fun isPassphraseAcceptable(passphrase: CharArray): Boolean =
        passphrase.size >= MIN_PASSPHRASE_LENGTH

    fun write(
        output: OutputStream,
        passphrase: CharArray,
        iterations: Int = DEFAULT_ITERATIONS,
        writeEntries: (ZipOutputStream) -> Unit,
    ) {
        open(output, passphrase, iterations).use(writeEntries)
    }

    fun open(
        output: OutputStream,
        passphrase: CharArray,
        iterations: Int = DEFAULT_ITERATIONS,
    ): ZipOutputStream {
        require(isPassphraseAcceptable(passphrase)) { "Passphrase too short" }
        val salt = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)
        val header = header(iterations, salt)
        output.write(header)
        return ZipOutputStream(streaming(passphrase, salt, iterations).newEncryptingStream(output, header))
    }

    fun read(
        input: InputStream,
        passphrase: CharArray,
        readEntry: (name: String, content: InputStream) -> Unit,
    ) {
        val header = ByteArray(HEADER_BYTES)
        try {
            DataInputStream(input).readFully(header)
        } catch (_: EOFException) {
            throw BackupFormatException("Not a Fun Phone backup")
        }
        if (!header.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw BackupFormatException("Not a Fun Phone backup")
        }
        if (header[MAGIC.size].toInt() != FORMAT_VERSION) {
            throw BackupFormatException("Unsupported backup version")
        }
        val iterations = ByteBuffer.wrap(header, MAGIC.size + 1, 4).int
        if (iterations !in ITERATION_RANGE) throw BackupFormatException("Invalid backup header")
        val salt = header.copyOfRange(HEADER_BYTES - SALT_BYTES, HEADER_BYTES)
        val decrypted = try {
            streaming(passphrase, salt, iterations).newDecryptingStream(input, header)
        } catch (_: GeneralSecurityException) {
            throw BackupPassphraseException()
        }
        try {
            ZipInputStream(decrypted).use { zip ->
                var entries = 0
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory) {
                        readEntry(entry.name, NonClosingInputStream(zip))
                        entries++
                    }
                    zip.closeEntry()
                }
                if (entries == 0) throw BackupPassphraseException()
            }
        } catch (error: BackupPassphraseException) {
            throw error
        } catch (error: BackupFormatException) {
            throw error
        } catch (error: BackupContentException) {
            throw error
        } catch (_: IOException) {
            throw BackupPassphraseException()
        }
    }

    fun putEntry(zip: ZipOutputStream, name: String, content: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content)
        zip.closeEntry()
    }

    private fun header(iterations: Int, salt: ByteArray): ByteArray {
        val bytes = java.io.ByteArrayOutputStream(HEADER_BYTES)
        DataOutputStream(bytes).apply {
            write(MAGIC)
            writeByte(FORMAT_VERSION)
            writeInt(iterations)
            write(salt)
        }
        return bytes.toByteArray()
    }

    private fun streaming(passphrase: CharArray, salt: ByteArray, iterations: Int): AesGcmHkdfStreaming {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BYTES * 8)
        val key = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
        return AesGcmHkdfStreaming(key, "HmacSha256", KEY_BYTES, SEGMENT_BYTES, 0)
    }

    private class NonClosingInputStream(private val delegate: InputStream) : InputStream() {
        override fun read(): Int = delegate.read()
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            delegate.read(buffer, offset, length)
        override fun close() = Unit
    }
}

class BackupContentException(message: String, cause: Throwable? = null) : IOException(message, cause)
