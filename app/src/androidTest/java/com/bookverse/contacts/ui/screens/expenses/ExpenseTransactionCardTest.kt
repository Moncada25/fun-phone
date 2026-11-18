package com.bookverse.contacts.ui.screens.expenses

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.data.expenses.ExpenseType
import com.bookverse.contacts.domain.expenses.ExpenseCurrency
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import com.bookverse.contacts.feature.expenses.R as ExpensesR

class ExpenseTransactionCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun recurringTransactionShowsItsBadge() {
        composeRule.setContent {
            MaterialTheme {
                ExpenseTransactionCard(
                    transaction = ExpenseTransaction(
                        id = 1L,
                        type = ExpenseType.EXPENSE,
                        amount = 25.0,
                        categoryId = null,
                        categoryName = "Subscription",
                        categoryColor = "#FF7043",
                        date = LocalDate.of(2026, 9, 1),
                        note = null,
                        isFixed = true,
                    ),
                    accentColor = Color(0xFFFF8A00),
                    currency = ExpenseCurrency("USD", "$", "US Dollar", 2),
                    onEdit = {},
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(
                ExpensesR.string.expense_transaction_recurring_badge,
            ),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(
                ExpensesR.string.expense_transaction_recurring_badge,
            ),
        ).assertDoesNotExist()
    }
}
