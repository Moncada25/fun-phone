package com.bookverse.contacts.domain.contacts

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import java.util.LinkedHashSet

object ContactInputPolicy {
    fun sanitize(contact: Contact): Contact {
        val sanitizedPhones = sanitizePhones(contact.phones)
        val sanitizedSocial = contact.socialMedia.mapNotNull(::sanitizeField)
        return contact.copy(
            name = contact.name.trim().ifEmpty { contact.name },
            phones = sanitizedPhones,
            email = sanitizeField(contact.email),
            company = sanitizeField(contact.company),
            notes = sanitizeField(contact.notes),
            socialMedia = sanitizedSocial,
        )
    }

    fun findMatch(candidates: List<Contact>, template: Contact): Contact? {
        if (candidates.isEmpty()) return null
        val targetNumbers = template.phones
            .mapNotNull { normalizePhoneForLookup(it.number) }
            .toSet()
        if (targetNumbers.isNotEmpty()) {
            candidates.firstOrNull { candidate ->
                candidate.phones.any {
                    normalizePhoneForLookup(it.number) in targetNumbers
                }
            }?.let { return it }
        }

        val trimmedName = template.name.trim()
        if (trimmedName.isEmpty()) return null
        return candidates.firstOrNull { candidate ->
            candidate.name.trim().equals(trimmedName, ignoreCase = true) &&
                candidate.phones.size == template.phones.size
        } ?: candidates.firstOrNull { candidate ->
            candidate.name.trim().equals(trimmedName, ignoreCase = true)
        }
    }

    fun findUniquePhoneMatch(candidates: List<Contact>, template: Contact): Contact? {
        val targetNumbers = template.phones
            .mapNotNull { normalizePhoneForLookup(it.number) }
            .toSet()
        if (targetNumbers.isEmpty()) return null
        return candidates.filter { candidate ->
            candidate.phones.any { normalizePhoneForLookup(it.number) in targetNumbers }
        }.singleOrNull()
    }

    fun normalizePhoneForLookup(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return buildString(raw.length) {
            raw.forEachIndexed { index, character ->
                when {
                    character.isDigit() -> append(character)
                    character == '+' && index == 0 -> append(character)
                }
            }
        }.takeIf { it.isNotBlank() }
    }

    private fun sanitizePhones(phones: List<ContactPhone>): List<ContactPhone> {
        val seen = LinkedHashSet<String>()
        return buildList {
            phones.forEach { phone ->
                val cleaned = sanitizePhone(phone) ?: return@forEach
                if (seen.add(cleaned.number)) add(cleaned)
            }
        }
    }

    private fun sanitizePhone(phone: ContactPhone): ContactPhone? {
        val raw = phone.number.trim()
        if (raw.isEmpty()) return null
        val normalizedCode = phone.countryCode
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let { code -> if (code.startsWith("+")) code else "+${code.trimStart('+')}" }
        val cleanedNumber = raw.replace("[^+\\d]".toRegex(), "")
        if (cleanedNumber.isEmpty()) return null
        val rebuiltNumber = when {
            cleanedNumber.startsWith("+") -> cleanedNumber
            normalizedCode != null -> {
                val digits = cleanedNumber.removePrefix(normalizedCode.removePrefix("+"))
                normalizedCode + digits
            }
            else -> cleanedNumber
        }
        return phone.copy(
            number = rebuiltNumber,
            countryCode = normalizedCode ?: phone.countryCode,
        )
    }

    private fun sanitizeField(value: String?): String? =
        value?.trim()?.takeIf(String::isNotEmpty)
}
