package com.bookverse.contacts.domain.contacts.vcard

import java.util.Locale

object VCardWriter {
    private const val LINE_BREAK = "\r\n"
    private const val MAX_LINE_OCTETS = 75

    fun write(contacts: List<VCardContact>): String = buildString {
        writeTo(this, contacts)
    }

    fun writeTo(output: Appendable, contacts: List<VCardContact>) {
        contacts.forEach { contact -> writeContact(output, contact) }
    }

    fun writeContact(output: Appendable, contact: VCardContact) {
        output.line("BEGIN:VCARD")
        output.line("VERSION:3.0")
        output.line(
            "N:" + listOf(
                contact.familyName,
                contact.givenName,
                contact.additionalName,
                contact.prefix,
                contact.suffix,
            ).joinToString(";") { escapeText(it.trim()) },
        )
        output.line("FN:" + escapeText(contact.displayName))
        contact.phones.forEach { phone ->
            val number = singleLine(phone.number)
            if (number.isEmpty()) return@forEach
            val types = phone.types
                .map { it.trim().uppercase(Locale.ROOT) }
                .filter { type -> type.isNotEmpty() && type.all(::isParameterCharacter) }
            val parameter = if (types.isEmpty()) "" else ";TYPE=" + types.joinToString(",")
            output.line("TEL$parameter:$number")
        }
        contact.emails.forEach { email ->
            val address = singleLine(email)
            if (address.isNotEmpty()) output.line("EMAIL;TYPE=INTERNET:$address")
        }
        contact.organization?.trim()?.takeIf(String::isNotEmpty)?.let { organization ->
            output.line("ORG:" + escapeText(organization))
        }
        contact.note?.trim()?.takeIf(String::isNotEmpty)?.let { note ->
            output.line("NOTE:" + escapeText(note))
        }
        contact.birthday?.let { birthday -> output.line("BDAY:" + formatDate(birthday)) }
        contact.urls.forEach { url ->
            val value = singleLine(url).replace("\\", "\\\\")
            if (value.isNotEmpty()) output.line("URL:$value")
        }
        output.line("END:VCARD")
    }

    private fun formatDate(date: VCardDate): String {
        val monthAndDay = String.format(Locale.ROOT, "%02d-%02d", date.month, date.day)
        return if (date.year == null) {
            "--$monthAndDay"
        } else {
            String.format(Locale.ROOT, "%04d-%s", date.year, monthAndDay)
        }
    }

    private fun isParameterCharacter(character: Char): Boolean =
        character in 'A'..'Z' || character in '0'..'9' || character == '-'

    private fun singleLine(value: String): String =
        value.replace("\r", "").replace("\n", " ").trim()

    private fun escapeText(value: String): String = buildString(value.length) {
        var index = 0
        while (index < value.length) {
            val character = value[index]
            when (character) {
                '\\' -> append("\\\\")
                ',' -> append("\\,")
                ';' -> append("\\;")
                '\n' -> append("\\n")
                '\r' -> {
                    append("\\n")
                    if (value.getOrNull(index + 1) == '\n') index++
                }

                else -> append(character)
            }
            index++
        }
    }

    private fun Appendable.line(content: String) {
        var octets = 0
        var index = 0
        while (index < content.length) {
            val codePoint = content.codePointAt(index)
            val size = utf8Size(codePoint)
            if (octets + size > MAX_LINE_OCTETS) {
                append(LINE_BREAK).append(' ')
                octets = 1
            }
            appendCodePoint(codePoint)
            octets += size
            index += Character.charCount(codePoint)
        }
        append(LINE_BREAK)
    }

    private fun Appendable.appendCodePoint(codePoint: Int) {
        if (Character.isBmpCodePoint(codePoint)) {
            append(codePoint.toChar())
        } else {
            append(Character.highSurrogate(codePoint))
            append(Character.lowSurrogate(codePoint))
        }
    }

    private fun utf8Size(codePoint: Int): Int = when {
        codePoint < 0x80 -> 1
        codePoint < 0x800 -> 2
        codePoint < 0x10000 -> 3
        else -> 4
    }
}
