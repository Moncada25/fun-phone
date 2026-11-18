package com.bookverse.contacts.domain.contacts

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.utils.ContactGroup
import com.bookverse.contacts.utils.groupContactsAlphabetically
import com.bookverse.contacts.utils.toContactInitialInfo
import java.text.CollationKey
import java.text.Collator
import java.util.Locale

object ContactSortPolicy {
    const val UNNAMED_SECTION_LABEL = "#"
    private const val NAMED_BY_LETTER = 0
    private const val NAMED_BY_SYMBOL = 1
    private const val UNNAMED = 2

    fun sort(
        contacts: List<Contact>,
        order: ContactSortOrder,
        locale: Locale = Locale.getDefault(),
    ): List<Contact> = when (order) {
        ContactSortOrder.RECENTLY_ADDED -> contacts.sortedWith(
            compareByDescending<Contact> { it.id.toLongOrNull() != null }
                .thenByDescending { it.id.toLongOrNull() ?: Long.MIN_VALUE },
        )

        ContactSortOrder.FIRST_NAME, ContactSortOrder.LAST_NAME -> {
            val collator = Collator.getInstance(locale).apply {
                strength = Collator.SECONDARY
                decomposition = Collator.CANONICAL_DECOMPOSITION
            }
            contacts
                .map { contact ->
                    val key = sortKey(contact, order)
                    SortEntry(
                        contact = contact,
                        category = category(contact, key),
                        primary = collator.getCollationKey(key),
                        secondary = collator.getCollationKey(contact.name.trim()),
                    )
                }
                .sortedWith(
                    compareBy<SortEntry> { it.category }
                        .thenBy { it.primary }
                        .thenBy { it.secondary },
                )
                .map(SortEntry::contact)
        }
    }

    fun sections(sortedContacts: List<Contact>, order: ContactSortOrder): List<ContactGroup> {
        if (order == ContactSortOrder.RECENTLY_ADDED) return emptyList()
        return groupContactsAlphabetically(sortedContacts) { contact -> sectionLabel(contact, order) }
    }

    fun sectionLabel(contact: Contact, order: ContactSortOrder): String {
        val key = sortKey(contact, order)
        return if (category(contact, key) == NAMED_BY_LETTER) {
            key.toContactInitialInfo().headerLabel
        } else {
            UNNAMED_SECTION_LABEL
        }
    }

    private fun sortKey(contact: Contact, order: ContactSortOrder): String = when (order) {
        ContactSortOrder.LAST_NAME -> ContactNamePolicy.lastName(contact.name)
        else -> contact.name.trim()
    }

    private fun category(contact: Contact, key: String): Int = when {
        !ContactNamePolicy.hasRealName(contact) -> UNNAMED
        key.isNotEmpty() && Character.isLetter(key.codePointAt(0)) -> NAMED_BY_LETTER
        else -> NAMED_BY_SYMBOL
    }

    private class SortEntry(
        val contact: Contact,
        val category: Int,
        val primary: CollationKey,
        val secondary: CollationKey,
    )
}
