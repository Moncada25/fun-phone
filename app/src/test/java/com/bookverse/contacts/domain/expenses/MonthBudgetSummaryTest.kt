package com.bookverse.contacts.domain.expenses

import com.bookverse.contacts.data.expenses.ExpenseBudget
import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.data.expenses.ExpenseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthBudgetSummaryTest {
    private val month = YearMonth.of(2026, 11)

    @Test
    fun monthIncludesItsOwnEntriesAndFixedCopies() {
        val transactions = listOf(
            transaction(1, 40.0, LocalDate.of(2026, 11, 3)),
            transaction(2, 99.0, LocalDate.of(2026, 10, 30)),
            transaction(3, 10.0, LocalDate.of(2026, 1, 1), isFixed = true),
        )
        val result = transactionsForMonth(transactions, month)
        assertEquals(listOf(3L, 1L), result.map { it.id })
        assertEquals(month.atDay(1), result.first().date)
    }

    @Test
    fun summaryAddsUpBudgetedCategoriesOnly() {
        val transactions = listOf(
            transaction(1, 60.0, LocalDate.of(2026, 11, 3), categoryId = 1),
            transaction(2, 30.0, LocalDate.of(2026, 11, 4), categoryId = 2),
            transaction(3, 500.0, LocalDate.of(2026, 11, 5), categoryId = 9),
            transaction(4, 1000.0, LocalDate.of(2026, 11, 1), type = ExpenseType.INCOME, categoryId = 1),
        )
        val budgets = listOf(budget(1, 100.0) to null, budget(2, 100.0) to null, budget(3, 0.0) to null)
        val summary = buildMonthBudgetSummary(transactions, budgets)
        assertEquals(590.0, summary.totals.expenses, 0.0)
        assertEquals(1000.0, summary.totals.income, 0.0)
        assertEquals(200.0, summary.budgetLimit, 0.0)
        assertEquals(90.0, summary.budgetSpent, 0.0)
        assertEquals(0.45f, summary.progress, 0.0001f)
        assertEquals(BudgetState.OK, summary.state)
    }

    @Test
    fun stateWarnsAndExceeds() {
        val budgets = listOf(budget(1, 100.0) to null)
        val warning = buildMonthBudgetSummary(listOf(transaction(1, 80.0, LocalDate.of(2026, 11, 2), categoryId = 1)), budgets)
        val exceeded = buildMonthBudgetSummary(listOf(transaction(1, 120.0, LocalDate.of(2026, 11, 2), categoryId = 1)), budgets)
        assertEquals(BudgetState.WARNING, warning.state)
        assertEquals(BudgetState.EXCEEDED, exceeded.state)
        assertEquals(1f, exceeded.progress, 0f)
        assertFalse(buildMonthBudgetSummary(emptyList(), emptyList()).hasBudget)
    }

    private fun budget(categoryId: Long, limit: Double) = ExpenseBudget(id = categoryId, categoryId = categoryId, limit = limit)

    private fun transaction(
        id: Long,
        amount: Double,
        date: LocalDate,
        type: ExpenseType = ExpenseType.EXPENSE,
        categoryId: Long = 1,
        isFixed: Boolean = false,
    ) = ExpenseTransaction(
        id = id,
        type = type,
        amount = amount,
        categoryId = categoryId,
        categoryName = "Food",
        categoryColor = "#FF7043",
        date = date,
        note = null,
        createdAt = id,
        updatedAt = 0,
        isFixed = isFixed,
    )
}
