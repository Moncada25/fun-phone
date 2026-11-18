package com.bookverse.contacts.domain.expenses

import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.data.expenses.ExpenseType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseDistributionTest {
    @Test
    fun distributionIncludesIncomeAndExpenseShares() {
        val shares = buildCategoryDistribution(
            listOf(
                transaction(
                    id = 1L,
                    type = ExpenseType.INCOME,
                    amount = 100.0,
                    categoryId = 10L,
                    categoryName = "Bonus"
                ),
                transaction(
                    id = 2L,
                    type = ExpenseType.EXPENSE,
                    amount = 25.0,
                    categoryId = 20L,
                    categoryName = "Food"
                )
            )
        )

        assertEquals(2, shares.size)
        assertTrue(shares.any { it.type == ExpenseType.INCOME && it.name == "Bonus" })
        assertTrue(shares.any { it.type == ExpenseType.EXPENSE && it.name == "Food" })
        assertEquals(0.8f, shares.first { it.type == ExpenseType.INCOME }.percentage, 0.001f)
        assertEquals(0.2f, shares.first { it.type == ExpenseType.EXPENSE }.percentage, 0.001f)
    }

    private fun transaction(
        id: Long,
        type: ExpenseType,
        amount: Double,
        categoryId: Long,
        categoryName: String
    ) = ExpenseTransaction(
        id = id,
        type = type,
        amount = amount,
        categoryId = categoryId,
        categoryName = categoryName,
        categoryColor = "#FF7043",
        date = LocalDate.of(2026, 9, 12),
        note = null
    )
}
