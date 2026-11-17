package com.bookverse.contacts.domain.contacts.vcard

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

object VCardCharsets {
    private const val BUFFER_SIZE = 8 * 1024

    val legacyFallback: Charset = runCatching { Charset.forName("windows-1252") }
        .getOrDefault(Charsets.ISO_8859_1)

    fun detect(input: InputStream): Charset {
        val buffer = ByteArray(BUFFER_SIZE)
        var pendingContinuationBytes = 0
        var isFirstChunk = true
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            if (isFirstChunk) {
                isFirstChunk = false
                byteOrderMarkCharset(buffer, read)?.let { return it }
            }
            for (index in 0 until read) {
                val value = buffer[index].toInt() and 0xFF
                if (pendingContinuationBytes > 0) {
                    if (value and 0xC0 != 0x80) return legacyFallback
                    pendingContinuationBytes--
                } else {
                    pendingContinuationBytes = when (value) {
                        in 0x00..0x7F -> 0
                        in 0xC2..0xDF -> 1
                        in 0xE0..0xEF -> 2
                        in 0xF0..0xF4 -> 3
                        else -> return legacyFallback
                    }
                }
            }
        }
        return if (pendingContinuationBytes == 0) Charsets.UTF_8 else legacyFallback
    }

    fun decode(bytes: ByteArray, declaredCharset: String?): String {
        val declared = declaredCharset
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let { name -> runCatching { Charset.forName(name) }.getOrNull() }
        if (declared != null) return String(bytes, declared)
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (_: CharacterCodingException) {
            String(bytes, legacyFallback)
        }
    }

    private fun byteOrderMarkCharset(buffer: ByteArray, length: Int): Charset? {
        fun byteAt(index: Int): Int = if (index < length) buffer[index].toInt() and 0xFF else -1
        return when {
            byteAt(0) == 0xEF && byteAt(1) == 0xBB && byteAt(2) == 0xBF -> Charsets.UTF_8
            byteAt(0) == 0xFE && byteAt(1) == 0xFF -> Charsets.UTF_16BE
            byteAt(0) == 0xFF && byteAt(1) == 0xFE -> Charsets.UTF_16LE
            else -> null
        }
    }
}
