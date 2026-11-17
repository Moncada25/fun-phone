package com.bookverse.contacts.utils

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.DuplicateGroup
import com.bookverse.contacts.data.model.MatchType
import java.text.Normalizer

class DuplicateDetector {

    fun findDuplicates(contacts: List<Contact>): List<DuplicateGroup> {
        if (contacts.size < 2) return emptyList()

        val allGroups = mutableListOf<DuplicateGroup>()
        val processedPairs = mutableSetOf<Set<String>>()

        allGroups.addAll(findPhoneMatches(contacts, processedPairs))
        allGroups.addAll(findExactNameMatches(contacts, processedPairs))
        allGroups.addAll(findNameAndPhoneMatches(contacts, processedPairs))
        allGroups.addAll(findSimilarNameMatches(contacts, processedPairs))

        return mergeAndSortGroups(allGroups)
    }

    private fun findPhoneMatches(
        contacts: List<Contact>,
        processedPairs: MutableSet<Set<String>>,
    ): List<DuplicateGroup> {
        val phoneMap = mutableMapOf<String, MutableList<Contact>>()

        contacts.forEach { contact ->
            contact.phones.forEach { phone ->
                val normalized = normalizePhoneNumber(phone.number)
                if (normalized.isNotEmpty()) {
                    phoneMap.getOrPut(normalized) { mutableListOf() }.add(contact)
                }
            }
        }

        return phoneMap.values
            .filter { it.size > 1 }
            .mapNotNull { groupContacts ->
                val uniqueContacts = groupContacts.distinctBy { it.id }
                if (uniqueContacts.size > 1) {
                    val pairKey = uniqueContacts.map { it.id }.toSet()
                    if (processedPairs.add(pairKey)) {
                        DuplicateGroup(
                            contacts = uniqueContacts,
                            matchType = MatchType.PHONE_MATCH,
                            confidence = 0.95f,
                        )
                    } else null
                } else null
            }
    }

    private fun findExactNameMatches(
        contacts: List<Contact>,
        processedPairs: MutableSet<Set<String>>,
    ): List<DuplicateGroup> {
        val nameMap = mutableMapOf<String, MutableList<Contact>>()

        contacts.forEach { contact ->
            val normalized = normalizeName(contact.name)
            if (normalized.isNotBlank()) {
                nameMap.getOrPut(normalized) { mutableListOf() }.add(contact)
            }
        }

        return nameMap.values
            .filter { it.size > 1 }
            .mapNotNull { groupContacts ->
                val uniqueContacts = groupContacts.distinctBy { it.id }
                if (uniqueContacts.size > 1) {
                    val pairKey = uniqueContacts.map { it.id }.toSet()
                    if (processedPairs.add(pairKey)) {
                        DuplicateGroup(
                            contacts = uniqueContacts,
                            matchType = MatchType.EXACT_NAME,
                            confidence = 0.9f,
                        )
                    } else null
                } else null
            }
    }

    private fun findNameAndPhoneMatches(
        contacts: List<Contact>,
        processedPairs: MutableSet<Set<String>>,
    ): List<DuplicateGroup> {
        val lastNamePhoneMap = mutableMapOf<Pair<String, String>, MutableList<Contact>>()

        contacts.forEach { contact ->
            val lastName = extractLastName(contact.name)
            if (lastName.isNotBlank()) {
                contact.phones.forEach { phone ->
                    val normalized = normalizePhoneNumber(phone.number)
                    if (normalized.isNotEmpty()) {
                        val key = lastName to normalized
                        lastNamePhoneMap.getOrPut(key) { mutableListOf() }.add(contact)
                    }
                }
            }
        }

        return lastNamePhoneMap.values
            .filter { it.size > 1 }
            .mapNotNull { groupContacts ->
                val uniqueContacts = groupContacts.distinctBy { it.id }
                if (uniqueContacts.size > 1) {
                    val pairKey = uniqueContacts.map { it.id }.toSet()
                    if (processedPairs.add(pairKey)) {
                        DuplicateGroup(
                            contacts = uniqueContacts,
                            matchType = MatchType.NAME_AND_PHONE,
                            confidence = 0.85f,
                        )
                    } else null
                } else null
            }
    }

    private fun findSimilarNameMatches(
        contacts: List<Contact>,
        processedPairs: MutableSet<Set<String>>,
    ): List<DuplicateGroup> {
        val groups = mutableListOf<DuplicateGroup>()
        val nameContacts = contacts.filter { it.name.isNotBlank() }

        val firstLetterGroups = nameContacts.groupBy { contact ->
            normalizeName(contact.name).firstOrNull()?.lowercaseChar() ?: ' '
        }

        firstLetterGroups.values.forEach { group ->
            if (group.size < 2) return@forEach

            for (i in group.indices) {
                for (j in i + 1 until group.size) {
                    val contact1 = group[i]
                    val contact2 = group[j]

                    val name1 = normalizeName(contact1.name)
                    val name2 = normalizeName(contact2.name)

                    if (name1.isBlank() || name2.isBlank()) continue

                    val maxDistance = if (name1.length <= 4 || name2.length <= 4) 1 else 2
                    val distance = levenshteinDistance(name1, name2)

                    if (distance <= maxDistance) {
                        val pairKey = setOf(contact1.id, contact2.id)
                        if (processedPairs.add(pairKey)) {
                            groups.add(
                                DuplicateGroup(
                                    contacts = listOf(contact1, contact2),
                                    matchType = MatchType.SIMILAR_NAME,
                                    confidence = 0.7f,
                                ),
                            )
                        }
                    }
                }
            }
        }

        return groups
    }

    companion object {
        internal fun normalizePhoneNumber(number: String): String {
            val digits = number.filter { it.isDigit() }
            return when {
                digits.length >= 10 -> digits.takeLast(10)
                digits.length >= 7 -> digits.takeLast(7)
                else -> digits
            }
        }

        internal fun normalizeName(name: String): String {
            val normalized = Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            return normalized
                .replace("\\s+".toRegex(), " ")
                .lowercase()
        }
    }

    private fun extractLastName(name: String): String {
        val normalized = normalizeName(name)
        val parts = normalized.split(" ").filter { it.isNotBlank() }
        return parts.lastOrNull() ?: ""
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        val costs = IntArray(s2.length + 1) { it }

        for (i in 1..s1.length) {
            var lastValue = i
            for (j in 1..s2.length) {
                val newValue = if (s1[i - 1] == s2[j - 1]) {
                    costs[j - 1]
                } else {
                    minOf(
                        costs[j - 1],
                        costs[j],
                        lastValue,
                    ) + 1
                }
                costs[j - 1] = lastValue
                lastValue = newValue
            }
            costs[s2.length] = lastValue
        }

        return costs[s2.length]
    }

    private fun mergeAndSortGroups(groups: List<DuplicateGroup>): List<DuplicateGroup> {
        val mergedGroups = mutableListOf<DuplicateGroup>()
        val contactIdToGroup = mutableMapOf<String, MutableSet<Contact>>()

        groups.sortedByDescending { it.confidence }.forEach { group ->
            val contactIds = group.contacts.map { it.id }.toSet()
            val existingGroups = contactIds.mapNotNull { contactIdToGroup[it] }.toSet()

            if (existingGroups.isEmpty()) {
                val newGroup = mutableSetOf<Contact>().apply { addAll(group.contacts) }
                group.contacts.forEach { contact ->
                    contactIdToGroup[contact.id] = newGroup
                }
                mergedGroups.add(group)
            } else if (existingGroups.size == 1) {
                val existingGroup = existingGroups.first()
                val addedNew = existingGroup.addAll(group.contacts)
                if (addedNew) {
                    group.contacts.forEach { contact ->
                        contactIdToGroup[contact.id] = existingGroup
                    }
                    val index = mergedGroups.indexOfFirst { it.contacts.any { c -> existingGroup.contains(c) } }
                    if (index >= 0) {
                        mergedGroups[index] = mergedGroups[index].copy(
                            contacts = existingGroup.toList(),
                            confidence = maxOf(mergedGroups[index].confidence, group.confidence),
                        )
                    }
                }
            } else {
                val mergedSet = mutableSetOf<Contact>()
                existingGroups.forEach { mergedSet.addAll(it) }
                mergedSet.addAll(group.contacts)

                mergedSet.forEach { contact ->
                    contactIdToGroup[contact.id] = mergedSet
                }

                mergedGroups.removeAll { it.contacts.any { c -> mergedSet.contains(c) } }
                mergedGroups.add(
                    group.copy(
                        contacts = mergedSet.toList(),
                        confidence = maxOf(
                            group.confidence,
                            existingGroups.mapNotNull { set ->
                                groups.find { it.contacts.any { c -> set.contains(c) } }?.confidence
                            }.maxOrNull() ?: group.confidence,
                        ),
                    ),
                )
            }
        }

        return mergedGroups.sortedByDescending { it.confidence }
    }
}
