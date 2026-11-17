package com.bookverse.contacts.domain.expenses

import com.bookverse.contacts.data.expenses.ExpenseBudget
import com.bookverse.contacts.data.expenses.ExpenseCategory
import com.bookverse.contacts.data.expenses.ExpensesStore
import kotlinx.coroutines.flow.Flow

class ManageExpenseBudgetsUseCase(
    private val repository: ExpensesStore
) {
    val budgets: Flow<List<Pair<ExpenseBudget, ExpenseCategory?>>> =
        repository.budgetsWithCategories

    suspend fun save(budget: ExpenseBudget): Long = repository.upsertBudget(budget)

    suspend fun delete(budgetId: Long) = repository.deleteBudget(budgetId)
}
