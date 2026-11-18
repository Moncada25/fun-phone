package com.bookverse.contacts.ui.screens.expenses

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.data.expenses.ExpenseType
import com.bookverse.contacts.domain.expenses.CategoryShare
import com.bookverse.contacts.domain.expenses.ExpenseCurrency
import com.bookverse.contacts.domain.expenses.ExpenseOverview
import com.bookverse.contacts.domain.expenses.MonthlyTotals
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ExpenseHomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun summaryShowsCurrencyAndMonthlyTotals() {
        showScreen()

        composeRule.onNodeWithText(text(R.string.expense_home_summary_title, CURRENCY.code))
            .assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.expense_summary_income)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.expense_summary_expense)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.expense_summary_balance)).assertIsDisplayed()
    }

    @Test
    fun monthControlsInvokeTheProvidedCallbacks() {
        val previousClicks = AtomicInteger()
        val nextClicks = AtomicInteger()
        showScreen(
            onPrevMonth = { previousClicks.incrementAndGet() },
            onNextMonth = { nextClicks.incrementAndGet() }
        )

        composeRule.onNodeWithContentDescription(text(R.string.cd_previous_month)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.cd_next_month)).performClick()

        assertEquals(1, previousClicks.get())
        assertEquals(1, nextClicks.get())
    }

    @Test
    fun categoryDistributionRemainsInteractive() {
        showScreen(
            transactions = listOf(TRANSACTION.copy(categoryName = "Lunch")),
            distribution = listOf(
                CategoryShare(
                    categoryId = 1L,
                    name = CATEGORY_NAME,
                    colorHex = "#FF7043",
                    amount = 18.5,
                    percentage = 1f
                )
            )
        )

        composeRule.onNodeWithText(CATEGORY_NAME).performClick()
        composeRule.onNodeWithText(text(R.string.expense_breakdown_title)).assertIsDisplayed()
    }

    @Test
    fun emptyHomeShowsEmptyTransactionsAndChartStates() {
        showScreen(transactions = emptyList(), distribution = emptyList())

        composeRule.onNodeWithText(text(R.string.expense_chart_empty)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.expense_transactions_empty)).assertIsDisplayed()
    }

    @Test
    fun transactionOverflowEditsTheSelectedTransaction() {
        val editedTransaction = AtomicReference<ExpenseTransaction>()
        showScreen(onEditTransaction = editedTransaction::set)

        composeRule.onNodeWithContentDescription(text(R.string.more_options)).performClick()
        composeRule.onNodeWithText(text(R.string.edit)).performClick()

        assertEquals(TRANSACTION, editedTransaction.get())
    }

    @Test
    fun transactionOverflowConfirmsAndDeletesTheSelectedTransaction() {
        val deletedTransaction = AtomicReference<ExpenseTransaction>()
        showScreen(onDeleteTransaction = deletedTransaction::set)

        composeRule.onNodeWithContentDescription(text(R.string.more_options)).performClick()
        composeRule.onNodeWithText(text(R.string.delete)).performClick()
        composeRule.onNodeWithText(text(R.string.expense_delete_transaction)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.delete)).performClick()

        assertEquals(TRANSACTION, deletedTransaction.get())
    }

    @Test
    fun recurringTransactionKeepsItsAccessibleBadge() {
        showScreen(transactions = listOf(TRANSACTION.copy(isFixed = true)))

        composeRule.onNodeWithContentDescription(
            text(R.string.expense_transaction_recurring_badge)
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            text(R.string.expense_transaction_recurring_badge)
        ).assertDoesNotExist()
    }

    private fun showScreen(
        transactions: List<ExpenseTransaction> = listOf(TRANSACTION),
        distribution: List<CategoryShare> = listOf(
            CategoryShare(
                categoryId = TRANSACTION.categoryId,
                name = CATEGORY_NAME,
                colorHex = TRANSACTION.categoryColor,
                amount = TRANSACTION.amount,
                percentage = 1f
            )
        ),
        onPrevMonth: () -> Unit = {},
        onNextMonth: () -> Unit = {},
        onEditTransaction: (ExpenseTransaction) -> Unit = {},
        onDeleteTransaction: (ExpenseTransaction) -> Unit = {}
    ) {
        val appSettingsViewModel = AppSettingsViewModel()
        composeRule.setContent {
            MaterialTheme {
                HomeContent(
                    transactions = transactions,
                    distribution = distribution,
                    onPrevMonth = onPrevMonth,
                    onNextMonth = onNextMonth,
                    onEditTransaction = onEditTransaction,
                    onDeleteTransaction = onDeleteTransaction,
                    appSettingsViewModel = appSettingsViewModel
                )
            }
        }
    }

    @Composable
    private fun HomeContent(
        transactions: List<ExpenseTransaction>,
        distribution: List<CategoryShare>,
        onPrevMonth: () -> Unit,
        onNextMonth: () -> Unit,
        onEditTransaction: (ExpenseTransaction) -> Unit,
        onDeleteTransaction: (ExpenseTransaction) -> Unit,
        appSettingsViewModel: AppSettingsViewModel
    ) {
        ExpenseHomeScreen(
            overview = ExpenseOverview(
                month = MONTH,
                totals = MonthlyTotals(income = 100.0, expenses = 18.5),
                distribution = distribution,
                recentTransactions = transactions,
                budgets = emptyList()
            ),
            transactions = transactions,
            accentColor = ACCENT_COLOR,
            currency = CURRENCY,
            month = MONTH,
            onPrevMonth = onPrevMonth,
            onNextMonth = onNextMonth,
            onEditTransaction = onEditTransaction,
            onDeleteTransaction = onDeleteTransaction,
            appSettingsViewModel = appSettingsViewModel
        )
    }

    private fun text(resourceId: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId, *args)

    private companion object {
        val ACCENT_COLOR = Color(0xFF00A7B8)
        val CURRENCY = ExpenseCurrency("USD", "$", "US Dollar", 2)
        val MONTH = YearMonth.of(2026, 9)
        const val CATEGORY_NAME = "Food"
        val TRANSACTION = ExpenseTransaction(
            id = 11L,
            type = ExpenseType.EXPENSE,
            amount = 18.5,
            categoryId = 1L,
            categoryName = CATEGORY_NAME,
            categoryColor = "#FF7043",
            date = LocalDate.of(2026, 9, 1),
            note = "Lunch"
        )
    }
}
