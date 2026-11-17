package com.bookverse.contacts.data.expenses

import android.net.Uri
import kotlinx.coroutines.flow.Flow

data class ExpenseExportSummary(val count: Int)

data class ExpenseImportSummary(val count: Int)

interface ExpensesStore {
    val categories: Flow<List<ExpenseCategory>>
    val transactions: Flow<List<ExpenseTransaction>>
    val budgetsWithCategories: Flow<List<Pair<ExpenseBudget, ExpenseCategory?>>>

    suspend fun seedDefaultCategoriesIfNeeded()
    suspend fun upsertTransaction(transaction: ExpenseTransaction): Long
    suspend fun deleteTransaction(id: Long)
    suspend fun upsertCategory(category: ExpenseCategory): Long
    suspend fun deleteCategory(categoryId: Long): Boolean
    suspend fun upsertBudget(budget: ExpenseBudget): Long
    suspend fun deleteBudget(budgetId: Long)
    suspend fun getTransaction(id: Long): ExpenseTransaction?
    suspend fun getCategory(categoryId: Long): ExpenseCategory?
    suspend fun exportFixedTransactions(
        transactions: List<ExpenseTransaction>,
        uri: Uri,
    ): ExpenseExportSummary
    suspend fun importFixedTransactions(uri: Uri): ExpenseImportSummary
    suspend fun exportTransactionsCsv(
        uri: Uri,
        headers: ExpenseCsvHeaders,
        currencyCode: String,
    ): ExpenseExportSummary
}
