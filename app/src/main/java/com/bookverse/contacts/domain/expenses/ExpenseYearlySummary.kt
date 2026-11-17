package com.bookverse.contacts.domain.expenses

import com.bookverse.contacts.data.expenses.ExpenseTransaction
import java.time.YearMonth

data class MonthSummary(
    val month: YearMonth,
    val totals: MonthlyTotals,
)

data class YearlySummary(
    val year: Int,
    val months: List<MonthSummary>,
    val totals: MonthlyTotals,
    val categories: List<CategoryShare>,
    val transactions: List<ExpenseTransaction>,
) {
    val isEmpty: Boolean get() = transactions.isEmpty()
}

internal fun buildYearlySummary(
    transactions: List<ExpenseTransaction>,
    year: Int,
): YearlySummary {
    val recurring = transactions.filter { it.isFixed }
    val regularByMonth = transactions
        .filter { !it.isFixed && it.date.year == year }
        .groupBy { it.date.monthValue }
    val monthlyTransactions = (1..12).map { monthValue ->
        val month = YearMonth.of(year, monthValue)
        val recurringCopies = recurring.map { it.copy(date = month.atDay(1)) }
        month to (regularByMonth[monthValue].orEmpty() + recurringCopies)
    }
    val yearTransactions = monthlyTransactions.flatMap { it.second }
    return YearlySummary(
        year = year,
        months = monthlyTransactions.map { (month, items) ->
            MonthSummary(month = month, totals = calculateMonthlyTotals(items))
        },
        totals = calculateMonthlyTotals(yearTransactions),
        categories = buildCategoryDistribution(yearTransactions),
        transactions = yearTransactions,
    )
}
