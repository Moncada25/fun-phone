package com.bookverse.contacts.domain.expenses

import com.bookverse.contacts.data.expenses.ExpenseCategory
import com.bookverse.contacts.data.expenses.ExpensesStore
import kotlinx.coroutines.flow.Flow

class ManageExpenseCategoriesUseCase(
    private val repository: ExpensesStore
) {
    val categories: Flow<List<ExpenseCategory>> = repository.categories

    suspend fun ensureDefaults() = repository.seedDefaultCategoriesIfNeeded()

    suspend fun save(category: ExpenseCategory): Long = repository.upsertCategory(category)

    suspend fun delete(categoryId: Long): Boolean = repository.deleteCategory(categoryId)

    suspend fun get(categoryId: Long): ExpenseCategory? = repository.getCategory(categoryId)
}
