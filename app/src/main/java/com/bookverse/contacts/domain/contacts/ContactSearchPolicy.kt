package com.bookverse.contacts.domain.contacts

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.utils.initialInfo

object ContactSearchPolicy {
    fun filterAndSort(
        contacts: List<Contact>,
        query: String,
        allowedContactIds: Set<String>? = null,
    ): List<Contact> {
        val normalizedQuery = query.trim()
        val alphabeticalComparator = Comparator<Contact> { first, second ->
            val firstInfo = first.initialInfo()
            val secondInfo = second.initialInfo()
            firstInfo.category.compareTo(secondInfo.category)
                .takeIf { it != 0 }
                ?: firstInfo.sortKey.compareTo(secondInfo.sortKey, ignoreCase = true)
                    .takeIf { it != 0 }
                ?: first.name.compareTo(second.name, ignoreCase = true)
        }

        val matchingContacts = if (normalizedQuery.isEmpty()) {
            contacts.sortedWith(alphabeticalComparator)
        } else {
            contacts
                .filter { contact ->
                    contact.name.contains(normalizedQuery, ignoreCase = true) ||
                        contact.phones.any {
                            it.number.contains(normalizedQuery, ignoreCase = true)
                        }
                }
                .sortedWith(
                    compareBy<Contact> { searchRank(it, normalizedQuery) }
                        .then(alphabeticalComparator),
                )
        }

        return allowedContactIds?.let { ids ->
            matchingContacts.filter { it.id in ids }
        } ?: matchingContacts
    }

    private fun searchRank(contact: Contact, query: String): Int = when {
        contact.name.equals(query, ignoreCase = true) -> 0
        contact.name.startsWith(query, ignoreCase = true) -> 1
        contact.name.split(" ").any { it.contains(query, ignoreCase = true) } -> 2
        else -> 3
    }
}
