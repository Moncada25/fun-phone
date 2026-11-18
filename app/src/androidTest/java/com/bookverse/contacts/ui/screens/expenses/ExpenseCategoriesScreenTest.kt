package com.bookverse.contacts.ui.screens.expenses

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.expenses.ExpenseCategory
import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.data.expenses.ExpenseType
import com.bookverse.contacts.domain.expenses.ExpenseCurrency
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import com.bookverse.contacts.feature.expenses.R as ExpensesR

class ExpenseCategoriesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun deletingUsedCategoryShowsLinkedTransactionsInsteadOfBlockedDialog() {
        val editedTransaction = AtomicReference<ExpenseTransaction>()
        showScreen(onEditTransaction = editedTransaction::set)

        composeRule.onNodeWithContentDescription(text(R.string.more_options)).performClick()
        composeRule.onNodeWithText(text(R.string.delete)).performClick()

        composeRule.onNodeWithText(
            text(ExpensesR.string.expense_category_linked_transactions_title, CATEGORY.name),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(TRANSACTION.note!!).assertIsDisplayed()
        composeRule.onNodeWithText(
            text(R.string.expense_category_delete_blocked_title),
        ).assertDoesNotExist()

        composeRule.onNodeWithContentDescription(text(R.string.more_options)).performClick()
        composeRule.onNodeWithText(text(R.string.edit)).performClick()

        assertEquals(TRANSACTION, editedTransaction.get())
    }

    @Test
    fun deletingLinkedTransactionRequiresConfirmationAndInvokesDelete() {
        val deletedTransaction = AtomicReference<ExpenseTransaction>()
        showScreen(onDeleteTransaction = deletedTransaction::set)
        composeRule.onNodeWithContentDescription(text(R.string.more_options)).performClick()
        composeRule.onNodeWithText(text(R.string.delete)).performClick()

        composeRule.onNodeWithContentDescription(text(R.string.more_options)).performClick()
        composeRule.onNodeWithText(text(R.string.delete)).performClick()
        composeRule.onNodeWithText(text(ExpensesR.string.expense_delete_transaction)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.delete)).performClick()

        assertEquals(TRANSACTION, deletedTransaction.get())
    }

    private fun showScreen(
        onEditTransaction: (ExpenseTransaction) -> Unit = {},
        onDeleteTransaction: (ExpenseTransaction) -> Unit = {},
    ) {
        val appSettingsViewModel = AppSettingsViewModel()
        composeRule.setContent {
            var linkedCategoryId by remember { mutableStateOf<Long?>(null) }
            MaterialTheme {
                ExpenseCategoriesScreen(
                    categories = listOf(CATEGORY),
                    transactions = listOf(TRANSACTION),
                    accentColor = Color(0xFFFF8A00),
                    currency = ExpenseCurrency("USD", "$", "US Dollar", 2),
                    categoryUsage = mapOf(CATEGORY.id to 1),
                    linkedCategoryId = linkedCategoryId,
                    onLinkedCategoryChange = { linkedCategoryId = it },
                    onSaveCategory = {},
                    onDeleteCategory = { _, _ -> },
                    onEditTransaction = onEditTransaction,
                    onDeleteTransaction = onDeleteTransaction,
                    appSettingsViewModel = appSettingsViewModel,
                )
            }
        }
    }

    private fun text(resourceId: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId, *args)

    private companion object {
        val CATEGORY = ExpenseCategory(
            id = 7L,
            name = "Food",
            colorHex = "#FF7043",
        )
        val TRANSACTION = ExpenseTransaction(
            id = 11L,
            type = ExpenseType.EXPENSE,
            amount = 18.5,
            categoryId = CATEGORY.id,
            categoryName = CATEGORY.name,
            categoryColor = CATEGORY.colorHex,
            date = LocalDate.of(2026, 8, 27),
            note = "Lunch",
        )
    }
}
