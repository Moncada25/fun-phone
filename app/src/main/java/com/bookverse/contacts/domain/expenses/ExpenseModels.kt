package com.bookverse.contacts.domain.expenses

import com.bookverse.contacts.data.expenses.ExpenseBudget
import com.bookverse.contacts.data.expenses.ExpenseCategory
import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.data.expenses.ExpenseType
import java.time.YearMonth

data class MonthlyTotals(
    val income: Double,
    val expenses: Double
) {
    val balance: Double get() = income - expenses
}

data class CategoryShare(
    val categoryId: Long?,
    val name: String,
    val colorHex: String,
    val amount: Double,
    val percentage: Float,
    val type: ExpenseType = ExpenseType.EXPENSE
)

enum class BudgetState {
    OK,
    WARNING,
    EXCEEDED
}

data class BudgetStatus(
    val budget: ExpenseBudget,
    val category: ExpenseCategory?,
    val spent: Double,
    val progress: Float,
    val state: BudgetState
)

data class ExpenseOverview(
    val month: YearMonth,
    val totals: MonthlyTotals,
    val distribution: List<CategoryShare>,
    val recentTransactions: List<ExpenseTransaction>,
    val budgets: List<BudgetStatus>
)
