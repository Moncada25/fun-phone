package com.bookverse.contacts.ui.viewmodel

import android.net.Uri
import com.bookverse.contacts.data.expenses.ExpenseBudget
import com.bookverse.contacts.data.expenses.ExpenseCategory
import com.bookverse.contacts.data.expenses.ExpenseCsvHeaders
import com.bookverse.contacts.data.expenses.ExpenseExportSummary
import com.bookverse.contacts.data.expenses.ExpenseImportSummary
import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.data.expenses.ExpenseType
import com.bookverse.contacts.data.expenses.ExpensesStore
import com.bookverse.contacts.domain.expenses.ExpenseTransactionFilter
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun shouldInitializeDefaultsAndMoveSelectedMonth() = runTest(dispatcher) {
        val store = FakeExpensesStore()
        val viewModel = ExpensesViewModel(store)
        val initialMonth = viewModel.selectedMonth.value

        viewModel.incrementMonth(2)
        viewModel.decrementMonth(1)
        advanceUntilIdle()

        assertTrue(store.defaultsSeeded)
        assertEquals(initialMonth.plusMonths(1), viewModel.selectedMonth.value)
    }

    @Test
    fun shouldBuildAndSaveTransactionThroughStore() = runTest(dispatcher) {
        val store = FakeExpensesStore()
        val viewModel = ExpensesViewModel(store)
        val category = ExpenseCategory(
            id = 7,
            name = "Food",
            colorHex = "#FF7043",
        )
        val date = LocalDate.of(2026, 7, 24)

        viewModel.saveTransaction(
            id = 9,
            type = ExpenseType.EXPENSE,
            amount = 25.5,
            category = category,
            date = date,
            note = "Lunch",
            isFixed = true,
        )
        advanceUntilIdle()

        assertEquals(
            ExpenseTransaction(
                id = 9,
                type = ExpenseType.EXPENSE,
                amount = 25.5,
                categoryId = category.id,
                categoryName = category.name,
                categoryColor = category.colorHex,
                date = date,
                note = "Lunch",
                isFixed = true,
            ).copy(
                createdAt = store.savedTransaction?.createdAt ?: 0,
                updatedAt = store.savedTransaction?.updatedAt ?: 0,
            ),
            store.savedTransaction,
        )
    }

    @Test
    fun shouldKeepAndClearTransactionFilter() = runTest(dispatcher) {
        val viewModel = ExpensesViewModel(FakeExpensesStore())
        val filter = ExpenseTransactionFilter(
            query = "rent",
            categoryIds = setOf(3L),
            type = ExpenseType.EXPENSE,
            dateFrom = LocalDate.of(2026, 1, 1),
            maxAmount = 900.0,
        )

        assertFalse(viewModel.transactionFilter.value.isActive)

        viewModel.setTransactionFilter(filter)
        assertEquals(filter, viewModel.transactionFilter.value)

        viewModel.clearTransactionFilter()
        assertEquals(ExpenseTransactionFilter(), viewModel.transactionFilter.value)
    }

    @Test
    fun shouldIgnoreCsvExportWhenNothingIsQueued() = runTest(dispatcher) {
        val store = FakeExpensesStore()
        val viewModel = ExpensesViewModel(store)

        viewModel.runPendingCsvExport(
            headers = ExpenseCsvHeaders("d", "t", "a", "c", "g", "n", "r"),
            currencyCode = "USD",
        )
        advanceUntilIdle()

        assertEquals(0, store.csvExports)
        assertNull(viewModel.pendingCsvExport.value)
        assertNull(viewModel.csvExportResult.value)
    }

    private class FakeExpensesStore : ExpensesStore {
        override val categories = MutableStateFlow<List<ExpenseCategory>>(emptyList())
        override val transactions = MutableStateFlow<List<ExpenseTransaction>>(emptyList())
        override val budgetsWithCategories =
            MutableStateFlow<List<Pair<ExpenseBudget, ExpenseCategory?>>>(emptyList())
        var defaultsSeeded = false
        var savedTransaction: ExpenseTransaction? = null
        var csvExports = 0

        override suspend fun seedDefaultCategoriesIfNeeded() {
            defaultsSeeded = true
        }

        override suspend fun upsertTransaction(transaction: ExpenseTransaction): Long {
            savedTransaction = transaction
            return transaction.id
        }

        override suspend fun deleteTransaction(id: Long) = Unit

        override suspend fun upsertCategory(category: ExpenseCategory): Long = category.id

        override suspend fun deleteCategory(categoryId: Long): Boolean = true

        override suspend fun upsertBudget(budget: ExpenseBudget): Long = budget.id

        override suspend fun deleteBudget(budgetId: Long) = Unit

        override suspend fun getTransaction(id: Long): ExpenseTransaction? = null

        override suspend fun getCategory(categoryId: Long): ExpenseCategory? = null

        override suspend fun exportFixedTransactions(
            transactions: List<ExpenseTransaction>,
            uri: Uri,
        ) = ExpenseExportSummary(transactions.count { it.isFixed })

        override suspend fun importFixedTransactions(uri: Uri) = ExpenseImportSummary(0)

        override suspend fun exportTransactionsCsv(
            uri: Uri,
            headers: ExpenseCsvHeaders,
            currencyCode: String,
        ): ExpenseExportSummary {
            csvExports++
            return ExpenseExportSummary(0)
        }
    }
}
