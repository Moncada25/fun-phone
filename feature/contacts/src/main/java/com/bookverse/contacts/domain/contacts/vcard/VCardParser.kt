package com.bookverse.contacts.domain.contacts.vcard

import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.Locale

object VCardParser {
    private const val BYTE_ORDER_MARK = "\uFEFF"
    private const val QUOTED_PRINTABLE = "QUOTED-PRINTABLE"
    private const val LEAP_YEAR = 2000
    private val encodingNames = setOf(QUOTED_PRINTABLE, "BASE64", "B", "8BIT", "7BIT")
    private val fullDatePattern = Regex("""^(\d{4})-?(\d{2})-?(\d{2})$""")
    private val yearlessDatePattern = Regex("""^--(\d{2})-?(\d{2})$""")

    fun parse(text: String): VCardParseResult = parse(text.lineSequence())

    fun parse(lines: Sequence<String>): VCardParseResult {
        val contacts = mutableListOf<VCardContact>()
        var failed = 0
        var card: CardBuilder? = null
        for (line in logicalLines(lines)) {
            val property = parseProperty(line) ?: continue
            val isCardMarker = property.rawValue.trim().equals("VCARD", ignoreCase = true)
            when {
                property.name == "BEGIN" && isCardMarker -> {
                    if (card != null) failed++
                    card = CardBuilder()
                }

                property.name == "END" && isCardMarker -> {
                    val open = card
                    if (open != null) {
                        val contact = open.build()
                        if (contact != null) contacts += contact else failed++
                    }
                    card = null
                }

                else -> card?.add(property)
            }
        }
        if (card != null) failed++
        return VCardParseResult(contacts = contacts, failed = failed)
    }

    private fun logicalLines(lines: Sequence<String>): Sequence<String> = sequence {
        val cursor = LineCursor(lines.iterator())
        var isFirstLine = true
        while (true) {
            var physical = cursor.next() ?: break
            if (isFirstLine) {
                physical = physical.removePrefix(BYTE_ORDER_MARK)
                isFirstLine = false
            }
            val logical = StringBuilder(physical)
            while (true) {
                val continuation = cursor.peek() ?: break
                if (continuation.isEmpty() || (continuation[0] != ' ' && continuation[0] != '\t')) break
                logical.append(continuation, 1, continuation.length)
                cursor.next()
            }
            if (declaresQuotedPrintable(logical)) {
                while (logical.endsWith("=")) {
                    val continuation = cursor.next() ?: break
                    logical.setLength(logical.length - 1)
                    logical.append(continuation)
                }
            }
            if (logical.isNotBlank()) yield(logical.toString())
        }
    }

    private fun declaresQuotedPrintable(line: CharSequence): Boolean {
        val colon = indexOfUnquoted(line, ':')
        if (colon <= 0) return false
        return line.subSequence(0, colon).toString().uppercase(Locale.ROOT).contains(QUOTED_PRINTABLE)
    }

    private fun parseProperty(line: String): Property? {
        val colon = indexOfUnquoted(line, ':')
        if (colon <= 0) return null
        val head = splitUnquoted(line.substring(0, colon), ';')
        val name = head.first().substringAfterLast('.').trim().uppercase(Locale.ROOT)
        if (name.isEmpty()) return null

        val types = linkedSetOf<String>()
        var encoding: String? = null
        var charset: String? = null
        for (parameter in head.drop(1)) {
            val separator = parameter.indexOf('=')
            val key = if (separator >= 0) {
                parameter.substring(0, separator).trim().uppercase(Locale.ROOT)
            } else {
                ""
            }
            val values = parameter.substring(separator + 1)
                .replace("\"", "")
                .split(',')
                .map(String::trim)
                .filter(String::isNotEmpty)
            when (key) {
                "ENCODING" -> encoding = values.firstOrNull()?.uppercase(Locale.ROOT)
                "CHARSET" -> charset = values.firstOrNull()
                "TYPE" -> values.mapTo(types) { it.uppercase(Locale.ROOT) }
                "" -> values.forEach { value ->
                    val upper = value.uppercase(Locale.ROOT)
                    if (upper in encodingNames) encoding = upper else types += upper
                }
            }
        }
        return Property(
            name = name,
            types = types,
            encoding = encoding,
            charset = charset,
            rawValue = line.substring(colon + 1),
        )
    }

    private fun indexOfUnquoted(text: CharSequence, target: Char): Int {
        var quoted = false
        for (index in text.indices) {
            val character = text[index]
            when {
                character == '"' -> quoted = !quoted
                character == target && !quoted -> return index
            }
        }
        return -1
    }

    private fun splitUnquoted(text: String, separator: Char): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        for (character in text) {
            when {
                character == '"' -> {
                    quoted = !quoted
                    current.append(character)
                }

                character == separator && !quoted -> {
                    parts += current.toString()
                    current.setLength(0)
                }

                else -> current.append(character)
            }
        }
        parts += current.toString()
        return parts
    }

    private fun splitEscaped(text: String, separator: Char): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var index = 0
        while (index < text.length) {
            val character = text[index]
            when {
                character == '\\' && index + 1 < text.length -> {
                    current.append(character).append(text[index + 1])
                    index += 2
                }

                character == separator -> {
                    parts += current.toString()
                    current.setLength(0)
                    index++
                }

                else -> {
                    current.append(character)
                    index++
                }
            }
        }
        parts += current.toString()
        return parts
    }

    private fun unescape(text: String, legacy: Boolean): String {
        if ('\\' !in text) return text
        return buildString(text.length) {
            var index = 0
            while (index < text.length) {
                val character = text[index]
                val next = text.getOrNull(index + 1)
                when {
                    character != '\\' || next == null -> {
                        append(character)
                        index++
                    }

                    legacy && next != ';' && next != '\\' -> {
                        append(character)
                        index++
                    }

                    next == 'n' || next == 'N' -> {
                        append('\n')
                        index += 2
                    }

                    else -> {
                        append(next)
                        index += 2
                    }
                }
            }
        }
    }

    private fun decodeQuotedPrintable(text: String, charsetName: String?): String {
        val bytes = ByteArrayOutputStream(text.length)
        var index = 0
        while (index < text.length) {
            val character = text[index]
            val high = text.getOrNull(index + 1)?.digitToIntOrNull(16)
            val low = text.getOrNull(index + 2)?.digitToIntOrNull(16)
            when {
                character == '=' && high != null && low != null -> {
                    bytes.write(high * 16 + low)
                    index += 3
                }

                character == '=' && index == text.lastIndex -> index++

                else -> {
                    bytes.write(character.toString().toByteArray(Charsets.UTF_8))
                    index++
                }
            }
        }
        return VCardCharsets.decode(bytes.toByteArray(), charsetName)
    }

    private fun parseDate(text: String): VCardDate? {
        val value = text.trim().substringBefore('T')
        val date = fullDatePattern.matchEntire(value)?.let { match ->
            VCardDate(
                year = match.groupValues[1].toInt(),
                month = match.groupValues[2].toInt(),
                day = match.groupValues[3].toInt(),
            )
        } ?: yearlessDatePattern.matchEntire(value)?.let { match ->
            VCardDate(
                year = null,
                month = match.groupValues[1].toInt(),
                day = match.groupValues[2].toInt(),
            )
        } ?: return null
        val isValid = runCatching {
            LocalDate.of(date.year ?: LEAP_YEAR, date.month, date.day)
        }.isSuccess
        return date.takeIf { isValid }
    }

    private fun parsePhoneNumber(text: String): String {
        val value = text.trim()
        return if (value.startsWith("tel:", ignoreCase = true)) {
            value.substring(4).substringBefore(';').trim()
        } else {
            value
        }
    }

    private class Property(
        val name: String,
        val types: Set<String>,
        val encoding: String?,
        val charset: String?,
        val rawValue: String,
    )

    private class LineCursor(private val iterator: Iterator<String>) {
        private var buffered: String? = null
        private var hasBuffered = false

        fun peek(): String? {
            if (!hasBuffered) {
                buffered = if (iterator.hasNext()) iterator.next() else null
                hasBuffered = true
            }
            return buffered
        }

        fun next(): String? {
            val value = peek()
            buffered = null
            hasBuffered = false
            return value
        }
    }

    private class CardBuilder {
        private val properties = mutableListOf<Property>()

        fun add(property: Property) {
            properties += property
        }

        fun build(): VCardContact? {
            val legacy = properties
                .firstOrNull { it.name == "VERSION" }
                ?.rawValue
                ?.trim()
                ?.startsWith("2") == true

            fun Property.decode(value: String): String = if (encoding == QUOTED_PRINTABLE) {
                decodeQuotedPrintable(value, charset).replace("\r\n", "\n")
            } else {
                value
            }

            fun Property.text(): String = unescape(decode(rawValue), legacy)

            fun Property.components(): List<String> =
                splitEscaped(rawValue, ';').map { part -> unescape(decode(part), legacy).trim() }

            var formattedName = ""
            var nameParts: List<String> = emptyList()
            val phones = mutableListOf<VCardPhone>()
            val emails = linkedSetOf<String>()
            var organization: String? = null
            val notes = mutableListOf<String>()
            var birthday: VCardDate? = null
            val urls = linkedSetOf<String>()

            for (property in properties) {
                when (property.name) {
                    "FN" -> if (formattedName.isEmpty()) formattedName = property.text().trim()
                    "N" -> if (nameParts.isEmpty()) nameParts = property.components()
                    "TEL" -> {
                        val number = parsePhoneNumber(property.text())
                        if (number.isNotEmpty()) phones += VCardPhone(number, property.types)
                    }

                    "EMAIL" -> {
                        val address = property.text().trim().removePrefix("mailto:")
                        if (address.isNotEmpty()) emails += address
                    }

                    "ORG" -> if (organization == null) {
                        organization = property.components().firstOrNull(String::isNotEmpty)
                    }

                    "NOTE" -> {
                        val note = property.text().trim()
                        if (note.isNotEmpty()) notes += note
                    }

                    "BDAY" -> if (birthday == null) birthday = parseDate(property.text())
                    "URL" -> {
                        val url = property.text().trim()
                        if (url.isNotEmpty()) urls += url
                    }
                }
            }

            val contact = VCardContact(
                formattedName = formattedName,
                familyName = nameParts.getOrNull(0).orEmpty(),
                givenName = nameParts.getOrNull(1).orEmpty(),
                additionalName = nameParts.getOrNull(2).orEmpty(),
                prefix = nameParts.getOrNull(3).orEmpty(),
                suffix = nameParts.getOrNull(4).orEmpty(),
                phones = phones,
                emails = emails.toList(),
                organization = organization,
                note = notes.joinToString("\n").ifEmpty { null },
                birthday = birthday,
                urls = urls.toList(),
            )
            val hasContent = contact.displayName.isNotEmpty() ||
                contact.phones.isNotEmpty() ||
                contact.emails.isNotEmpty()
            return contact.takeIf { hasContent }
        }
    }
}
