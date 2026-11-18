package com.bookverse.contacts.ui.screens.expenses

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.expenses.ExpenseCategory
import com.bookverse.contacts.data.expenses.ExpenseType
import com.bookverse.contacts.domain.expenses.ExpenseCurrency
import com.bookverse.contacts.ui.components.SELECT_BOTTOM_SHEET_LIST_TAG
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import org.junit.Rule
import org.junit.Test
import com.bookverse.contacts.feature.expenses.R as ExpensesR

class ExpenseCategoryBottomSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun transactionCategoryPickerUsesTheSharedBottomSheetAndAppliesSelection() {
        val categories = List(40) { index ->
            ExpenseCategory(
                id = index.toLong(),
                name = "Category $index",
                colorHex = "#FF7043",
                type = ExpenseType.EXPENSE,
            )
        }
        val appSettingsViewModel = AppSettingsViewModel()
        composeRule.setContent {
            MaterialTheme {
                AddTransactionScreen(
                    transaction = null,
                    categories = categories,
                    accentColor = Color(0xFFFF8A00),
                    currency = ExpenseCurrency("USD", "$", "US Dollar", 2),
                    onSave = { _, _, _, _, _, _ -> },
                    onDelete = null,
                    onCancel = {},
                    appSettingsViewModel = appSettingsViewModel,
                )
            }
        }

        composeRule.onNodeWithText("Category 0").performClick()

        composeRule.onNodeWithText(text(ExpensesR.string.expense_category_picker_title)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(dragHandleDescription()).assertIsDisplayed()
        composeRule.onNodeWithTag(SELECT_BOTTOM_SHEET_LIST_TAG).performScrollToIndex(39)
        composeRule.onNodeWithText("Category 39").performClick()

        composeRule.onNodeWithText("Category 39").assertIsDisplayed()
    }

    private fun text(resourceId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId)

    private fun dragHandleDescription(): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(
            androidx.compose.material3.R.string.m3c_bottom_sheet_drag_handle_description,
        )
}
