package com.bookverse.contacts.utils

import com.bookverse.contacts.data.model.Contact
import java.text.Normalizer
import java.util.Locale

private val combiningMarkRegex = "\\p{M}+".toRegex()

data class ContactInitialInfo(
    val category: Int,
    val sortKey: String,
    val headerLabel: String,
    val avatarLabel: String
)

data class ContactGroup(
    val label: String,
    val contacts: List<Contact>
)

fun String.toContactInitialInfo(): ContactInitialInfo {
    val trimmed = trim()
    if (trimmed.isEmpty()) {
        return ContactInitialInfo(
            category = 3,
            sortKey = "~",
            headerLabel = "#",
            avatarLabel = "#"
        )
    }

    val firstCodePoint = trimmed.codePointAt(0)
    val label = String(Character.toChars(firstCodePoint))

    return when {
        Character.isLetter(firstCodePoint) -> {
            val upper = label.uppercase(Locale.ROOT)
            val decomposed = Normalizer.normalize(upper, Normalizer.Form.NFD)
            val stripped = combiningMarkRegex.replace(decomposed, "").ifBlank { upper }
            val baseCodePoint = stripped.codePointAt(0)
            val base = String(Character.toChars(baseCodePoint))
            ContactInitialInfo(
                category = 2,
                sortKey = base,
                headerLabel = base,
                avatarLabel = base
            )
        }

        Character.isDigit(firstCodePoint) -> ContactInitialInfo(
            category = 1,
            sortKey = label,
            headerLabel = "#",
            avatarLabel = label
        )

        else -> ContactInitialInfo(
            category = 0,
            sortKey = label,
            headerLabel = label,
            avatarLabel = label
        )
    }
}

fun Contact.initialInfo(): ContactInitialInfo = name.toContactInitialInfo()

fun Contact.avatarLabel(): String = initialInfo().avatarLabel

fun Contact.headerLabel(): String = initialInfo().headerLabel

fun groupContactsAlphabetically(
    contacts: List<Contact>,
    labelOf: (Contact) -> String = Contact::headerLabel,
): List<ContactGroup> {
    if (contacts.isEmpty()) return emptyList()

    val groups = mutableListOf<ContactGroup>()
    var currentLabel: String? = null
    var currentContacts = mutableListOf<Contact>()

    contacts.forEach { contact ->
        val label = labelOf(contact)
        if (label != currentLabel) {
            if (currentLabel != null) {
                groups.add(ContactGroup(currentLabel, currentContacts.toList()))
            }
            currentLabel = label
            currentContacts = mutableListOf(contact)
        } else {
            currentContacts.add(contact)
        }
    }

    if (currentLabel != null) {
        groups.add(ContactGroup(currentLabel, currentContacts.toList()))
    }

    return groups
}
