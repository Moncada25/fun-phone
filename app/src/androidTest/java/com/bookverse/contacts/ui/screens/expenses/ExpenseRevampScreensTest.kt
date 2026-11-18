package com.bookverse.contacts.ui.screens.expenses

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.expenses.ExpenseBudget
import com.bookverse.contacts.data.expenses.ExpenseCategory
import com.bookverse.contacts.data.expenses.ExpenseCsvHeaders
import com.bookverse.contacts.data.expenses.ExpenseExportSummary
import com.bookverse.contacts.data.expenses.ExpenseImportSummary
import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.data.expenses.ExpenseType
import com.bookverse.contacts.data.expenses.ExpensesStore
import com.bookverse.contacts.domain.expenses.BudgetState
import com.bookverse.contacts.domain.expenses.BudgetStatus
import com.bookverse.contacts.domain.expenses.ExpenseCurrency
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.ExpensesViewModel
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ExpenseRevampScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun recurringTransactionOverflowEditsSelectedTransaction() {
        val editedTransaction = AtomicReference<ExpenseTransaction>()
        val added = AtomicBoolean(false)
        val appSettingsViewModel = AppSettingsViewModel()
        composeRule.setContent {
            MaterialTheme {
                FixedTransactionsScreen(
                    transactions = listOf(TRANSACTION.copy(isFixed = true)),
                    accentColor = ACCENT_COLOR,
                    currency = CURRENCY,
                    onAddTransaction = { added.set(true) },
                    onEditTransaction = editedTransaction::set,
                    onDeleteTransaction = {},
                    onExport = {},
                    onImport = {},
                    appSettingsViewModel = appSettingsViewModel,
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.add)).performClick()
        composeRule.onAllNodesWithContentDescription(text(R.string.more_options))[1].performClick()
        composeRule.onNodeWithText(text(R.string.edit)).performClick()

        assertTrue(added.get())
        assertEquals(TRANSACTION.copy(isFixed = true), editedTransaction.get())
    }

    @Test
    fun budgetOverflowRequiresConfirmationBeforeDelete() {
        val deletedBudget = AtomicReference<ExpenseBudget>()
        val budget = ExpenseBudget(id = 3L, categoryId = CATEGORY.id, limit = 250.0)
        val appSettingsViewModel = AppSettingsViewModel()
        composeRule.setContent {
            MaterialTheme {
                ExpenseBudgetsScreen(
                    budgets = listOf(budget to CATEGORY),
                    statuses = listOf(
                        BudgetStatus(
                            budget = budget,
                            category = CATEGORY,
                            spent = 80.0,
                            progress = 0.32f,
                            state = BudgetState.OK,
                        ),
                    ),
                    categories = listOf(CATEGORY),
                    accentColor = ACCENT_COLOR,
                    currency = CURRENCY,
                    onSaveBudget = {},
                    onDeleteBudget = deletedBudget::set,
                    appSettingsViewModel = appSettingsViewModel,
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.expense_budget_all_categories_used))
            .assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription(text(R.string.more_options))[0].performClick()
        composeRule.onNodeWithText(text(R.string.delete)).performClick()
        composeRule.onNodeWithText(text(R.string.expense_budget_delete_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.delete)).performClick()

        assertEquals(budget, deletedBudget.get())
    }

    @Test
    fun editingTransactionRequiresConfirmationBeforeDelete() {
        val deleted = AtomicBoolean(false)
        val appSettingsViewModel = AppSettingsViewModel()
        composeRule.setContent {
            MaterialTheme {
                AddTransactionScreen(
                    transaction = TRANSACTION,
                    categories = listOf(CATEGORY),
                    accentColor = ACCENT_COLOR,
                    currency = CURRENCY,
                    onSave = { _, _, _, _, _, _ -> },
                    onDelete = { deleted.set(true) },
                    onCancel = {},
                    appSettingsViewModel = appSettingsViewModel,
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.expense_delete_transaction)).performClick()
        composeRule.onNodeWithText(text(R.string.expense_delete_transaction_confirm))
            .assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.delete)).performClick()

        assertTrue(deleted.get())
    }

    @Test
    fun transactionFormActionsRemainReachableAtCompactWidthAndLargeText() {
        val appSettingsViewModel = AppSettingsViewModel()
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                MaterialTheme {
                    Box(
                        modifier = Modifier
                            .width(320.dp)
                            .height(640.dp),
                    ) {
                        AddTransactionScreen(
                            transaction = TRANSACTION,
                            categories = listOf(CATEGORY),
                            accentColor = ACCENT_COLOR,
                            currency = CURRENCY,
                            onSave = { _, _, _, _, _, _ -> },
                            onDelete = {},
                            onCancel = {},
                            appSettingsViewModel = appSettingsViewModel,
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText(text(R.string.save)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.expense_delete_transaction))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun categoryDialogOffersExplicitCancelAction() {
        val appSettingsViewModel = AppSettingsViewModel()
        composeRule.setContent {
            MaterialTheme {
                ExpenseCategoriesScreen(
                    categories = listOf(CATEGORY),
                    transactions = emptyList(),
                    accentColor = ACCENT_COLOR,
                    currency = CURRENCY,
                    categoryUsage = emptyMap(),
                    linkedCategoryId = null,
                    onLinkedCategoryChange = {},
                    onSaveCategory = {},
                    onDeleteCategory = { _, _ -> },
                    onEditTransaction = {},
                    onDeleteTransaction = {},
                    appSettingsViewModel = appSettingsViewModel,
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.expense_category_create)).performClick()
        composeRule.onNodeWithText(text(R.string.cancel)).assertIsDisplayed().performClick()
        composeRule.onAllNodesWithText(text(R.string.expense_category_create)).assertCountEquals(1)
    }

    @Test
    fun backingOutOfExpenseSubsectionReturnsHomeBeforeLeavingManager() {
        val exited = AtomicBoolean(false)
        val expensesViewModel = ExpensesViewModel(InMemoryExpensesStore())
        val appSettingsViewModel = AppSettingsViewModel()
        composeRule.setContent {
            MaterialTheme {
                ExpenseManagerScreen(
                    onBackClick = { exited.set(true) },
                    expensesViewModel = expensesViewModel,
                    appSettingsViewModel = appSettingsViewModel,
                )
            }
        }

        composeRule.onNodeWithContentDescription(text(R.string.more_options)).performClick()
        composeRule.onNodeWithText(text(R.string.expense_nav_categories)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.back)).performClick()

        composeRule.onNodeWithText(text(R.string.expense_home_summary_title, CURRENCY.code))
            .assertIsDisplayed()
        assertTrue(!exited.get())

        composeRule.onNodeWithContentDescription(text(R.string.back)).performClick()

        assertTrue(exited.get())
    }

    private class InMemoryExpensesStore : ExpensesStore {
        override val categories = MutableStateFlow(emptyList<ExpenseCategory>())
        override val transactions = MutableStateFlow(emptyList<ExpenseTransaction>())
        override val budgetsWithCategories = MutableStateFlow(emptyList<Pair<ExpenseBudget, ExpenseCategory?>>())

        override suspend fun seedDefaultCategoriesIfNeeded() = Unit

        override suspend fun upsertTransaction(transaction: ExpenseTransaction): Long = transaction.id

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
        ): ExpenseExportSummary = ExpenseExportSummary(transactions.size)

        override suspend fun importFixedTransactions(uri: Uri): ExpenseImportSummary =
            ExpenseImportSummary(0)

        override suspend fun exportTransactionsCsv(
            uri: Uri,
            headers: ExpenseCsvHeaders,
            currencyCode: String,
        ) = ExpenseExportSummary(0)
    }

    private fun text(resourceId: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId, *args)

    private companion object {
        val ACCENT_COLOR = Color(0xFF00A7B8)
        val CURRENCY = ExpenseCurrency("USD", "$", "US Dollar", 2)
        val CATEGORY = ExpenseCategory(
            id = 7L,
            name = "Food",
            colorHex = "#FF7043",
            type = ExpenseType.EXPENSE,
        )
        val TRANSACTION = ExpenseTransaction(
            id = 11L,
            type = ExpenseType.EXPENSE,
            amount = 18.5,
            categoryId = CATEGORY.id,
            categoryName = CATEGORY.name,
            categoryColor = CATEGORY.colorHex,
            date = LocalDate.of(2026, 9, 1),
            note = "Lunch",
        )
    }
}
