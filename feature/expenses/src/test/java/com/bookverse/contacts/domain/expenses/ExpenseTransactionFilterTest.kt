package com.bookverse.contacts.domain.expenses

import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.data.expenses.ExpenseType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseTransactionFilterTest {
    private val lunch = transaction(1, ExpenseType.EXPENSE, 12.5, LocalDate.of(2026, 1, 15), 10, "Alimentación", "Almuerzo en el café")
    private val salary = transaction(2, ExpenseType.INCOME, 2000.0, LocalDate.of(2026, 2, 1), 20, "Salary", null)
    private val taxi = transaction(3, ExpenseType.EXPENSE, 30.0, LocalDate.of(2025, 11, 30), 30, "Transport", "Taxi to AIRPORT")
    private val rent = transaction(4, ExpenseType.EXPENSE, 800.0, LocalDate.of(2024, 5, 20), 40, "Home", "Rent", isFixed = true)
    private val all = listOf(lunch, salary, taxi, rent)

    @Test
    fun emptyFilterMatchesEverything() {
        val filter = ExpenseTransactionFilter()

        assertFalse(filter.isActive)
        assertEquals(0, filter.activeCount)
        assertTrue(all.all(filter::matches))
        assertSame(all, filterTransactions(all, filter))
    }

    @Test
    fun blankQueryIsNotAnActiveFilter() {
        val filter = ExpenseTransactionFilter(query = "   ")

        assertFalse(filter.isActive)
        assertEquals(all, filterTransactions(all, filter))
    }

    @Test
    fun queryMatchesNoteIgnoringCaseAndDiacritics() {
        assertEquals(listOf(lunch), filterTransactions(all, ExpenseTransactionFilter(query = "CAFE")))
        assertEquals(listOf(taxi), filterTransactions(all, ExpenseTransactionFilter(query = "airport")))
        assertEquals(listOf(taxi), filterTransactions(all, ExpenseTransactionFilter(query = "áirpört")))
    }

    @Test
    fun queryMatchesCategoryNameIgnoringCaseAndDiacritics() {
        assertEquals(listOf(lunch), filterTransactions(all, ExpenseTransactionFilter(query = "alimentacion")))
        assertEquals(listOf(salary), filterTransactions(all, ExpenseTransactionFilter(query = "  SALARY ")))
    }

    @Test
    fun everyQueryWordMustMatch() {
        assertEquals(listOf(lunch), filterTransactions(all, ExpenseTransactionFilter(query = "cafe aliment")))
        assertTrue(filterTransactions(all, ExpenseTransactionFilter(query = "cafe taxi")).isEmpty())
        assertTrue(filterTransactions(all, ExpenseTransactionFilter(query = "missing")).isEmpty())
    }

    @Test
    fun typeFilterSelectsIncomeOrExpense() {
        assertEquals(listOf(salary), filterTransactions(all, ExpenseTransactionFilter(type = ExpenseType.INCOME)))
        assertEquals(
            listOf(lunch, taxi, rent),
            filterTransactions(all, ExpenseTransactionFilter(type = ExpenseType.EXPENSE)),
        )
    }

    @Test
    fun categoryFilterAcceptsSeveralCategories() {
        assertEquals(listOf(lunch), filterTransactions(all, ExpenseTransactionFilter(categoryIds = setOf(10))))
        assertEquals(
            listOf(lunch, taxi),
            filterTransactions(all, ExpenseTransactionFilter(categoryIds = setOf(10, 30))),
        )
        assertTrue(filterTransactions(all, ExpenseTransactionFilter(categoryIds = setOf(99))).isEmpty())
    }

    @Test
    fun categoryFilterExcludesTransactionsWithoutCategory() {
        val orphan = lunch.copy(id = 9, categoryId = null)

        assertFalse(ExpenseTransactionFilter(categoryIds = setOf(10)).matches(orphan))
    }

    @Test
    fun dateBoundsAreInclusive() {
        val exact = ExpenseTransactionFilter(
            dateFrom = LocalDate.of(2026, 1, 15),
            dateTo = LocalDate.of(2026, 1, 15),
        )

        assertTrue(exact.matches(lunch))
        assertFalse(exact.copy(dateFrom = LocalDate.of(2026, 1, 16), dateTo = null).matches(lunch))
        assertFalse(exact.copy(dateFrom = null, dateTo = LocalDate.of(2026, 1, 14)).matches(lunch))
    }

    @Test
    fun openEndedDateRanges() {
        val regular = listOf(lunch, salary, taxi)

        assertEquals(
            listOf(lunch, salary),
            filterTransactions(regular, ExpenseTransactionFilter(dateFrom = LocalDate.of(2026, 1, 1))),
        )
        assertEquals(
            listOf(taxi),
            filterTransactions(regular, ExpenseTransactionFilter(dateTo = LocalDate.of(2025, 12, 31))),
        )
    }

    @Test
    fun recurringTransactionsMatchRangesContainingAFirstOfMonth() {
        assertTrue(ExpenseTransactionFilter(dateFrom = LocalDate.of(2026, 3, 1)).matches(rent))
        assertTrue(ExpenseTransactionFilter(dateTo = LocalDate.of(2020, 1, 1)).matches(rent))
        assertTrue(
            ExpenseTransactionFilter(
                dateFrom = LocalDate.of(2026, 3, 1),
                dateTo = LocalDate.of(2026, 3, 1),
            ).matches(rent)
        )
        assertTrue(
            ExpenseTransactionFilter(
                dateFrom = LocalDate.of(2026, 3, 20),
                dateTo = LocalDate.of(2026, 4, 1),
            ).matches(rent)
        )
        assertFalse(
            ExpenseTransactionFilter(
                dateFrom = LocalDate.of(2026, 3, 2),
                dateTo = LocalDate.of(2026, 3, 31),
            ).matches(rent)
        )
    }

    @Test
    fun amountBoundsAreInclusive() {
        assertTrue(ExpenseTransactionFilter(minAmount = 12.5, maxAmount = 12.5).matches(lunch))
        assertFalse(ExpenseTransactionFilter(minAmount = 12.51).matches(lunch))
        assertFalse(ExpenseTransactionFilter(maxAmount = 12.49).matches(lunch))
        assertEquals(
            listOf(lunch, taxi),
            filterTransactions(all, ExpenseTransactionFilter(minAmount = 12.5, maxAmount = 30.0)),
        )
        assertEquals(listOf(salary), filterTransactions(all, ExpenseTransactionFilter(minAmount = 1000.0)))
        assertEquals(listOf(lunch), filterTransactions(all, ExpenseTransactionFilter(maxAmount = 20.0)))
    }

    @Test
    fun invertedRangesMatchNothing() {
        assertTrue(
            filterTransactions(all, ExpenseTransactionFilter(minAmount = 50.0, maxAmount = 10.0)).isEmpty()
        )
        assertTrue(
            filterTransactions(
                listOf(lunch, salary, taxi),
                ExpenseTransactionFilter(
                    dateFrom = LocalDate.of(2026, 2, 1),
                    dateTo = LocalDate.of(2026, 1, 1),
                ),
            ).isEmpty()
        )
    }

    @Test
    fun filtersCombineWithAnd() {
        val filter = ExpenseTransactionFilter(
            query = "taxi",
            categoryIds = setOf(10, 30),
            type = ExpenseType.EXPENSE,
            dateFrom = LocalDate.of(2025, 11, 1),
            dateTo = LocalDate.of(2025, 11, 30),
            minAmount = 30.0,
            maxAmount = 30.0,
        )

        assertEquals(5, filter.activeCount)
        assertEquals(listOf(taxi), filterTransactions(all, filter))
        assertTrue(filterTransactions(all, filter.copy(type = ExpenseType.INCOME)).isEmpty())
        assertTrue(filterTransactions(all, filter.copy(query = "lunch")).isEmpty())
        assertTrue(filterTransactions(all, filter.copy(categoryIds = setOf(10))).isEmpty())
        assertTrue(filterTransactions(all, filter.copy(dateTo = LocalDate.of(2025, 11, 29))).isEmpty())
        assertTrue(filterTransactions(all, filter.copy(minAmount = 30.01, maxAmount = null)).isEmpty())
    }

    @Test
    fun activeCountCountsEachFilterGroupOnce() {
        assertEquals(1, ExpenseTransactionFilter(dateFrom = LocalDate.of(2026, 1, 1)).activeCount)
        assertEquals(
            1,
            ExpenseTransactionFilter(
                dateFrom = LocalDate.of(2026, 1, 1),
                dateTo = LocalDate.of(2026, 2, 1),
            ).activeCount,
        )
        assertEquals(1, ExpenseTransactionFilter(minAmount = 1.0, maxAmount = 2.0).activeCount)
        assertEquals(2, ExpenseTransactionFilter(query = "a", type = ExpenseType.INCOME).activeCount)
    }

    private fun transaction(
        id: Long,
        type: ExpenseType,
        amount: Double,
        date: LocalDate,
        categoryId: Long,
        categoryName: String,
        note: String?,
        isFixed: Boolean = false,
    ) = ExpenseTransaction(
        id = id,
        type = type,
        amount = amount,
        categoryId = categoryId,
        categoryName = categoryName,
        categoryColor = "#FF7043",
        date = date,
        note = note,
        createdAt = 0,
        updatedAt = 0,
        isFixed = isFixed,
    )
}
