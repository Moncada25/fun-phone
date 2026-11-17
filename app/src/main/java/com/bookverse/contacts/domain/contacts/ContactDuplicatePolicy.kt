package com.bookverse.contacts.domain.contacts

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.utils.DuplicateDetector

data class ContactDuplicatePlan<T>(
    val unique: List<T>,
    val duplicates: Int,
)

object ContactDuplicatePolicy {
    fun <T> plan(
        existing: List<Contact>,
        incoming: List<T>,
        contactOf: (T) -> Contact,
    ): ContactDuplicatePlan<T> {
        val index = Index()
        existing.forEach(index::add)
        val unique = mutableListOf<T>()
        var duplicates = 0
        for (candidate in incoming) {
            val contact = contactOf(candidate)
            if (index.contains(contact)) {
                duplicates++
            } else {
                index.add(contact)
                unique += candidate
            }
        }
        return ContactDuplicatePlan(unique = unique, duplicates = duplicates)
    }

    fun isDuplicate(candidate: Contact, existing: Contact): Boolean =
        Index().apply { add(existing) }.contains(candidate)

    private fun nameKey(contact: Contact): String = if (ContactNamePolicy.hasRealName(contact)) {
        DuplicateDetector.normalizeName(contact.name)
    } else {
        ""
    }

    private fun phoneKeys(contact: Contact): Set<String> = contact.phones
        .map { DuplicateDetector.normalizePhoneNumber(it.number) }
        .filter(String::isNotEmpty)
        .toSet()

    private class Index {
        private val phonesByName = mutableMapOf<String, MutableSet<String>>()
        private val phoneSets = mutableSetOf<Set<String>>()

        fun add(contact: Contact) {
            val phones = phoneKeys(contact)
            if (phones.isEmpty()) return
            phoneSets += phones
            val name = nameKey(contact)
            if (name.isNotEmpty()) phonesByName.getOrPut(name) { mutableSetOf() } += phones
        }

        fun contains(contact: Contact): Boolean {
            val phones = phoneKeys(contact)
            if (phones.isEmpty()) return false
            val name = nameKey(contact)
            return if (name.isEmpty()) {
                phones in phoneSets
            } else {
                phonesByName[name]?.any(phones::contains) == true
            }
        }
    }
}
