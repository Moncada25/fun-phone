package com.bookverse.contacts.domain.expenses

import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.data.expenses.ExpenseType
import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale

data class ExpenseTransactionFilter(
    val query: String = "",
    val categoryIds: Set<Long> = emptySet(),
    val type: ExpenseType? = null,
    val dateFrom: LocalDate? = null,
    val dateTo: LocalDate? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
) {
    val activeCount: Int
        get() = listOf(
            query.isNotBlank(),
            categoryIds.isNotEmpty(),
            type != null,
            dateFrom != null || dateTo != null,
            minAmount != null || maxAmount != null,
        ).count { it }

    val isActive: Boolean get() = activeCount > 0

    fun matches(transaction: ExpenseTransaction): Boolean {
        if (type != null && transaction.type != type) return false
        if (categoryIds.isNotEmpty() && transaction.categoryId !in categoryIds) return false
        if (minAmount != null && transaction.amount < minAmount) return false
        if (maxAmount != null && transaction.amount > maxAmount) return false
        if (!matchesDate(transaction)) return false
        return matchesQuery(transaction)
    }

    private fun matchesDate(transaction: ExpenseTransaction): Boolean {
        if (transaction.isFixed) return recursWithin(dateFrom, dateTo)
        if (dateFrom != null && transaction.date.isBefore(dateFrom)) return false
        if (dateTo != null && transaction.date.isAfter(dateTo)) return false
        return true
    }

    private fun matchesQuery(transaction: ExpenseTransaction): Boolean {
        val tokens = normalizeForSearch(query).split(' ').filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return true
        val haystack = normalizeForSearch(transaction.categoryName + " " + transaction.note.orEmpty())
        return tokens.all { haystack.contains(it) }
    }
}

private val combiningMarks = Regex("\\p{Mn}+")
private val whitespaceRuns = Regex("\\s+")

internal fun normalizeForSearch(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(combiningMarks, "")
        .lowercase(Locale.ROOT)
        .replace(whitespaceRuns, " ")
        .trim()

internal fun recursWithin(from: LocalDate?, to: LocalDate?): Boolean {
    if (from == null || to == null) return true
    val firstOccurrence = if (from.dayOfMonth == 1) from else from.withDayOfMonth(1).plusMonths(1)
    return !firstOccurrence.isAfter(to)
}

internal fun filterTransactions(
    transactions: List<ExpenseTransaction>,
    filter: ExpenseTransactionFilter,
): List<ExpenseTransaction> =
    if (filter.isActive) transactions.filter(filter::matches) else transactions
