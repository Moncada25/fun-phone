package com.bookverse.contacts.ui.screens.contacts.dialer

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactUsageStats
import com.bookverse.contacts.data.model.RecentOperation
import com.bookverse.contacts.data.model.historyKey

private fun normalizePhoneNumber(phone: String): String {
    val cleaned = phone.replace(Regex("[^0-9]"), "")
    return when {
        cleaned.length >= 10 -> cleaned.takeLast(10)
        else -> cleaned
    }
}

private fun extractPhoneNumber(operation: RecentOperation): String {
    return operation.target.ifBlank {
        operation.contact.phones.firstOrNull()?.number.orEmpty()
    }
}

internal fun buildSmartSuggestions(
    localRecents: List<RecentOperation>,
    callLogRecents: List<RecentOperation>,
    topUsedContacts: List<ContactUsageStats>
): List<RecentOperation> {
    val now = System.currentTimeMillis()
    val oneDayMs = 24 * 60 * 60 * 1000L

    val allRecents = (callLogRecents + localRecents)
        .groupBy { normalizePhoneNumber(extractPhoneNumber(it)) }
        .mapValues { (_, operations) -> operations.maxByOrNull { it.date }!! }
        .values
        .toList()

    val recentThree = allRecents
        .sortedByDescending { it.date }
        .take(3)

    val usageMap = topUsedContacts
        .groupBy { normalizePhoneNumber(it.phoneNumber) }
        .mapValues { (_, stats) ->
            ContactUsageStats(
                phoneNumber = stats.first().phoneNumber,
                callCount = stats.sumOf { it.callCount },
                totalCallDuration = stats.sumOf { it.totalCallDuration }
            )
        }

    val scoredOperations = allRecents.mapNotNull { operation ->
        val phoneNumber = extractPhoneNumber(operation)
        if (phoneNumber.isBlank()) return@mapNotNull null

        val normalizedPhone = normalizePhoneNumber(phoneNumber)
        val usageStats = usageMap[normalizedPhone]

        val daysSinceLastCall = (now - operation.date) / oneDayMs.toDouble()
        val recencyScore = when {
            daysSinceLastCall < 1 -> 100.0
            daysSinceLastCall < 3 -> 50.0
            daysSinceLastCall < 7 -> 25.0
            else -> 10.0 / (daysSinceLastCall / 7.0)
        }

        val frequencyScore = (usageStats?.callCount ?: 0) * 10.0

        val durationScore = (usageStats?.totalCallDuration ?: 0) / 60000.0

        val finalScore = recencyScore + frequencyScore + durationScore

        SuggestionScore(operation, finalScore)
    }

    val recentKeys = recentThree.map { it.historyKey }.toSet()
    val topScored = scoredOperations
        .filter { it.operation.historyKey !in recentKeys }
        .sortedByDescending { it.score }
        .take(7)
        .map { it.operation }

    return (recentThree + topScored).take(10)
}

internal fun normalizePhoneForDialerSearch(value: String): String {
    val cleaned = value.replace(Regex("[^0-9+]"), "")
    return if (cleaned.startsWith("+")) {
        cleaned.substring(1)
    } else {
        cleaned
    }
}

internal fun buildDialerContactSearchIndex(
    contacts: List<Contact>,
): List<DialerContactSearchIndex> = contacts.map { contact ->
    DialerContactSearchIndex(
        contact = contact,
        normalizedPhones = contact.phones.map { normalizePhoneForDialerSearch(it.number) },
        primaryPhone = contact.phones.firstOrNull()?.number.orEmpty(),
    )
}

internal fun filterDialerContacts(
    searchIndex: List<DialerContactSearchIndex>,
    rawQuery: String,
    limit: Int = MAX_DIALER_FILTER_RESULTS,
): List<DialerContactSearchIndex> {
    val queryDigits = rawQuery.filter(Char::isDigit)
    if (queryDigits.isEmpty() || limit <= 0) return emptyList()
    return searchIndex
        .asSequence()
        .filter { entry ->
            entry.normalizedPhones.any { phone -> phone.contains(queryDigits) }
        }
        .take(limit)
        .toList()
}
