package com.bookverse.contacts.domain.expenses

import android.net.Uri
import com.bookverse.contacts.data.expenses.ExpenseCsvHeaders
import com.bookverse.contacts.data.expenses.ExpenseExportSummary
import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.data.expenses.ExpensesStore
import kotlinx.coroutines.flow.Flow

class ManageExpenseTransactionsUseCase(
    private val repository: ExpensesStore
) {
    val transactions: Flow<List<ExpenseTransaction>> = repository.transactions

    suspend fun save(transaction: ExpenseTransaction): Long = repository.upsertTransaction(transaction)

    suspend fun delete(id: Long) = repository.deleteTransaction(id)

    suspend fun get(id: Long): ExpenseTransaction? = repository.getTransaction(id)

    suspend fun exportCsv(
        uri: Uri,
        headers: ExpenseCsvHeaders,
        currencyCode: String,
    ): ExpenseExportSummary = repository.exportTransactionsCsv(uri, headers, currencyCode)
}
