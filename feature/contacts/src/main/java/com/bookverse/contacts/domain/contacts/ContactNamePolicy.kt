package com.bookverse.contacts.domain.contacts

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.utils.DuplicateDetector

data class ContactNameParts(
    val givenName: String,
    val familyName: String,
)

object ContactNamePolicy {
    private val whitespace = "\\s+".toRegex()

    fun hasRealName(contact: Contact): Boolean = hasRealName(contact.name, contact.phones.map { it.number })

    fun hasRealName(name: String, phoneNumbers: List<String>): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        if (trimmed.any(Char::isLetter)) return true
        val nameDigits = DuplicateDetector.normalizePhoneNumber(trimmed)
        if (nameDigits.isEmpty()) return true
        return phoneNumbers.none { number ->
            DuplicateDetector.normalizePhoneNumber(number) == nameDigits
        }
    }

    fun split(name: String): ContactNameParts {
        val tokens = name.trim().split(whitespace).filter(String::isNotEmpty)
        return when (tokens.size) {
            0 -> ContactNameParts(givenName = "", familyName = "")
            1 -> ContactNameParts(givenName = tokens.single(), familyName = "")
            else -> ContactNameParts(
                givenName = tokens.dropLast(1).joinToString(" "),
                familyName = tokens.last(),
            )
        }
    }

    fun lastName(name: String): String {
        val parts = split(name)
        return parts.familyName.ifEmpty { parts.givenName }
    }
}
