package com.bookverse.contacts.domain.expenses

import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.data.expenses.ExpenseType
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseYearlySummaryTest {
    @Test
    fun emptyYearHasTwelveZeroMonthsAndIsEmpty() {
        val summary = buildYearlySummary(emptyList(), 2026)

        assertTrue(summary.isEmpty)
        assertEquals(2026, summary.year)
        assertEquals((1..12).map { YearMonth.of(2026, it) }, summary.months.map { it.month })
        assertTrue(summary.months.all { it.totals.income == 0.0 && it.totals.expenses == 0.0 })
        assertEquals(0.0, summary.totals.balance, 0.0)
        assertTrue(summary.categories.isEmpty())
    }

    @Test
    fun totalsAreGroupedPerMonth() {
        val summary = buildYearlySummary(
            listOf(
                transaction(1, ExpenseType.INCOME, 1000.0, LocalDate.of(2026, 1, 31), 1, "Salary"),
                transaction(2, ExpenseType.EXPENSE, 200.0, LocalDate.of(2026, 1, 1), 2, "Food"),
                transaction(3, ExpenseType.EXPENSE, 50.0, LocalDate.of(2026, 12, 31), 2, "Food"),
            ),
            2026,
        )

        assertFalse(summary.isEmpty)
        assertEquals(1000.0, summary.months[0].totals.income, 0.0)
        assertEquals(200.0, summary.months[0].totals.expenses, 0.0)
        assertEquals(800.0, summary.months[0].totals.balance, 0.0)
        assertEquals(50.0, summary.months[11].totals.expenses, 0.0)
        assertEquals(-50.0, summary.months[11].totals.balance, 0.0)
        assertEquals(0.0, summary.months[5].totals.income, 0.0)
        assertEquals(1000.0, summary.totals.income, 0.0)
        assertEquals(250.0, summary.totals.expenses, 0.0)
        assertEquals(750.0, summary.totals.balance, 0.0)
    }

    @Test
    fun otherYearsAreExcluded() {
        val summary = buildYearlySummary(
            listOf(
                transaction(1, ExpenseType.EXPENSE, 10.0, LocalDate.of(2025, 12, 31), 2, "Food"),
                transaction(2, ExpenseType.EXPENSE, 20.0, LocalDate.of(2027, 1, 1), 2, "Food"),
            ),
            2026,
        )

        assertTrue(summary.isEmpty)
        assertEquals(0.0, summary.totals.expenses, 0.0)
    }

    @Test
    fun recurringTransactionsCountInEveryMonth() {
        val summary = buildYearlySummary(
            listOf(
                transaction(1, ExpenseType.EXPENSE, 100.0, LocalDate.of(2024, 6, 10), 3, "Rent", isFixed = true),
                transaction(2, ExpenseType.INCOME, 30.0, LocalDate.of(2026, 2, 2), 1, "Bonus"),
            ),
            2026,
        )

        assertTrue(summary.months.all { it.totals.expenses == 100.0 })
        assertEquals(1200.0, summary.totals.expenses, 0.0)
        assertEquals(30.0, summary.totals.income, 0.0)
        assertEquals(13, summary.transactions.size)
        assertEquals(
            (1..12).map { LocalDate.of(2026, it, 1) },
            summary.transactions.filter { it.isFixed }.map { it.date },
        )
    }

    @Test
    fun categoriesAreTotalledAcrossTheYear() {
        val summary = buildYearlySummary(
            listOf(
                transaction(1, ExpenseType.EXPENSE, 60.0, LocalDate.of(2026, 1, 5), 2, "Food"),
                transaction(2, ExpenseType.EXPENSE, 40.0, LocalDate.of(2026, 7, 5), 2, "Food"),
                transaction(3, ExpenseType.EXPENSE, 300.0, LocalDate.of(2026, 3, 5), 3, "Rent"),
                transaction(4, ExpenseType.INCOME, 600.0, LocalDate.of(2026, 3, 5), 1, "Salary"),
            ),
            2026,
        )

        assertEquals(listOf("Salary", "Rent", "Food"), summary.categories.map { it.name })
        assertEquals(100.0, summary.categories.first { it.name == "Food" }.amount, 0.0)
        assertEquals(0.1f, summary.categories.first { it.name == "Food" }.percentage, 0.0001f)
        assertEquals(0.6f, summary.categories.first { it.name == "Salary" }.percentage, 0.0001f)
        assertEquals(ExpenseType.INCOME, summary.categories.first().type)
    }

    private fun transaction(
        id: Long,
        type: ExpenseType,
        amount: Double,
        date: LocalDate,
        categoryId: Long,
        categoryName: String,
        isFixed: Boolean = false,
    ) = ExpenseTransaction(
        id = id,
        type = type,
        amount = amount,
        categoryId = categoryId,
        categoryName = categoryName,
        categoryColor = "#FF7043",
        date = date,
        note = null,
        createdAt = 0,
        updatedAt = 0,
        isFixed = isFixed,
    )
}
