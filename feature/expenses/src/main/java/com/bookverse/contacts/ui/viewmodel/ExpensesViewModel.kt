package com.bookverse.contacts.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookverse.contacts.data.expenses.ExpenseBudget
import com.bookverse.contacts.data.expenses.ExpenseCategory
import com.bookverse.contacts.data.expenses.ExpenseCsvHeaders
import com.bookverse.contacts.data.expenses.ExpenseExportSummary
import com.bookverse.contacts.data.expenses.ExpenseImportSummary
import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.data.expenses.ExpenseType
import com.bookverse.contacts.data.expenses.ExpensesStore
import com.bookverse.contacts.domain.expenses.ExpenseTransactionFilter
import com.bookverse.contacts.domain.expenses.ManageExpenseBudgetsUseCase
import com.bookverse.contacts.domain.expenses.ManageExpenseCategoriesUseCase
import com.bookverse.contacts.domain.expenses.ManageExpenseTransactionsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class ExpensesViewModel(
    private val store: ExpensesStore,
) : ViewModel() {
    private val transactionsUseCase = ManageExpenseTransactionsUseCase(store)
    private val categoriesUseCase = ManageExpenseCategoriesUseCase(store)
    private val budgetsUseCase = ManageExpenseBudgetsUseCase(store)

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    val categories: StateFlow<List<ExpenseCategory>> =
        categoriesUseCase.categories.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val transactions: StateFlow<List<ExpenseTransaction>> =
        transactionsUseCase.transactions.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val budgets: StateFlow<List<Pair<ExpenseBudget, ExpenseCategory?>>> =
        budgetsUseCase.budgets.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    private val _transactionFilter = MutableStateFlow(ExpenseTransactionFilter())
    val transactionFilter: StateFlow<ExpenseTransactionFilter> = _transactionFilter.asStateFlow()

    private val _pendingCsvExport = MutableStateFlow<Uri?>(null)
    val pendingCsvExport: StateFlow<Uri?> = _pendingCsvExport.asStateFlow()

    private val _csvExportResult = MutableStateFlow<Result<ExpenseExportSummary>?>(null)
    val csvExportResult: StateFlow<Result<ExpenseExportSummary>?> = _csvExportResult.asStateFlow()

    init {
        viewModelScope.launch {
            categoriesUseCase.ensureDefaults()
        }
    }

    fun incrementMonth(offset: Long) {
        _selectedMonth.value = _selectedMonth.value.plusMonths(offset)
    }

    fun decrementMonth(offset: Long) {
        _selectedMonth.value = _selectedMonth.value.minusMonths(offset)
    }

    fun saveTransaction(
        id: Long = 0,
        type: ExpenseType,
        amount: Double,
        category: ExpenseCategory?,
        date: LocalDate,
        note: String?,
        isFixed: Boolean
    ) {
        viewModelScope.launch {
            val transaction = ExpenseTransaction(
                id = id,
                type = type,
                amount = amount,
                categoryId = category?.id,
                categoryName = category?.name ?: "",
                categoryColor = category?.colorHex ?: "#607D8B",
                date = date,
                note = note,
                isFixed = isFixed
            )
            transactionsUseCase.save(transaction)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionsUseCase.delete(id)
        }
    }

    fun saveCategory(
        id: Long = 0,
        name: String,
        colorHex: String,
        isDefault: Boolean = false,
        type: ExpenseType = ExpenseType.EXPENSE,
        nameResKey: String? = null
    ) {
        viewModelScope.launch {
            val category = ExpenseCategory(
                id = id,
                name = name,
                colorHex = colorHex,
                isDefault = isDefault,
                type = type,
                nameResKey = nameResKey
            )
            categoriesUseCase.save(category)
        }
    }

    fun deleteCategory(id: Long, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = categoriesUseCase.delete(id)
            onResult(success)
        }
    }

    fun saveBudget(
        id: Long = 0,
        categoryId: Long,
        limit: Double
    ) {
        viewModelScope.launch {
            val budget = ExpenseBudget(
                id = id,
                categoryId = categoryId,
                limit = limit
            )
            budgetsUseCase.save(budget)
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch { budgetsUseCase.delete(id) }
    }

    fun exportFixedTransactions(uri: Uri): kotlinx.coroutines.Deferred<Result<ExpenseExportSummary>> {
        return viewModelScope.async {
            runCatching {
                store.exportFixedTransactions(
                    transactions = transactions.value,
                    uri = uri,
                )
            }
        }
    }

    fun importFixedTransactions(uri: Uri): kotlinx.coroutines.Deferred<Result<ExpenseImportSummary>> {
        return viewModelScope.async {
            runCatching { store.importFixedTransactions(uri) }
        }
    }

    fun setTransactionFilter(filter: ExpenseTransactionFilter) {
        _transactionFilter.value = filter
    }

    fun clearTransactionFilter() {
        _transactionFilter.value = ExpenseTransactionFilter()
    }

    fun queueCsvExport(uri: Uri) {
        _pendingCsvExport.value = uri
    }

    fun runPendingCsvExport(headers: ExpenseCsvHeaders, currencyCode: String) {
        val uri = _pendingCsvExport.value ?: return
        _pendingCsvExport.value = null
        viewModelScope.launch {
            _csvExportResult.value = try {
                Result.success(transactionsUseCase.exportCsv(uri, headers, currencyCode))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            }
        }
    }

    fun consumeCsvExportResult() {
        _csvExportResult.value = null
    }
}
