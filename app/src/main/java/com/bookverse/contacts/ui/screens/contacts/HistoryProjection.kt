package com.bookverse.contacts.ui.screens.contacts

import com.bookverse.contacts.data.model.RecentOperation
import com.bookverse.contacts.data.model.RecentType

internal data class HistoryProjection(
    val allRecents: List<RecentOperation>,
    val filteredRecents: List<RecentOperation>,
    val groupedOperations: Map<String, List<RecentOperation>>,
)

internal fun buildHistoryProjection(
    localOperations: List<RecentOperation>,
    callLogs: List<RecentOperation>,
    selectedFilter: RecentType?,
    searchQuery: String = "",
    formatDate: (Long) -> String,
): HistoryProjection {
    val allRecents = (localOperations + callLogs).sortedByDescending { it.date }
    val typeFiltered = selectedFilter?.let { filter ->
        allRecents.filter { it.type == filter }
    } ?: allRecents
    val query = searchQuery.trim()
    val filteredRecents = if (query.isEmpty()) {
        typeFiltered
    } else {
        typeFiltered.filter { it.matchesHistoryQuery(query) }
    }
    val groupedOperations = filteredRecents
        .groupBy { operation -> formatDate(operation.date) }
        .toList()
        .sortedByDescending { (_, operations) ->
            operations.maxOfOrNull(RecentOperation::date) ?: 0L
        }
        .associateTo(LinkedHashMap()) { (date, operations) ->
            val latestByContact = operations
                .groupBy(::recentContactIdentity)
                .mapNotNull { (_, contactOperations) ->
                    contactOperations.maxByOrNull(RecentOperation::date)
                }
                .sortedByDescending(RecentOperation::date)
            date to latestByContact
        }
    return HistoryProjection(allRecents, filteredRecents, groupedOperations)
}

private fun RecentOperation.matchesHistoryQuery(query: String): Boolean {
    if (contact.name.contains(query, ignoreCase = true)) return true
    if (target.contains(query, ignoreCase = true)) return true
    if (contact.phones.any { it.number.contains(query, ignoreCase = true) }) return true

    val digits = query.filter(Char::isDigit)
    return digits.isNotEmpty() && (
        target.filter(Char::isDigit).contains(digits) ||
            contact.phones.any { it.number.filter(Char::isDigit).contains(digits) }
        )
}

private fun recentContactIdentity(operation: RecentOperation): String {
    val contact = operation.contact
    return contact.id.ifEmpty {
        "unknown_${contact.phones.firstOrNull()?.number.orEmpty()}"
    }
}
