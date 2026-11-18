package com.bookverse.contacts.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.core.ui.R
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SelectBottomSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectionAppliesAndDismissesImmediately() {
        val selected = AtomicReference<String>()
        val selectionCount = AtomicInteger(0)
        val dismissed = AtomicBoolean(false)
        showSheet(
            onSelect = {
                selected.set(it)
                selectionCount.incrementAndGet()
            },
            onDismiss = { dismissed.set(true) },
        )

        composeRule.onNodeWithText("Option 1").performClick()

        assertEquals("Option 1", selected.get())
        assertEquals(1, selectionCount.get())
        assertTrue(dismissed.get())
    }

    @Test
    fun sheetDoesNotRenderApplyOrCancelActions() {
        showSheet()

        composeRule.onAllNodesWithText(text(R.string.apply)).assertCountEquals(0)
        composeRule.onAllNodesWithText(text(R.string.cancel)).assertCountEquals(0)
    }

    @Test
    fun rapidBoundaryFlingsStayInsideTheLongOptionList() {
        val dismissed = AtomicBoolean(false)
        showSheet(
            options = List(40) { index -> "Option $index" },
            onDismiss = { dismissed.set(true) },
        )

        val list = composeRule.onNodeWithTag(SELECT_BOTTOM_SHEET_LIST_TAG)
        list.performScrollToIndex(39)
        list.performTouchInput { swipeUp(durationMillis = 50) }
        list.performScrollToIndex(0)
        list.performTouchInput { swipeDown(durationMillis = 50) }

        composeRule.onNodeWithText("Choose an option").assertIsDisplayed()
        assertTrue(!dismissed.get())
    }

    private fun showSheet(
        options: List<String> = listOf("Option 0", "Option 1"),
        onSelect: (String) -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialTheme {
                SelectBottomSheet(
                    onDismiss = onDismiss,
                    title = "Choose an option",
                    options = options,
                    selectedOption = options.firstOrNull(),
                    onSelect = onSelect,
                    optionKey = { it },
                    optionLabel = { it },
                )
            }
        }
    }

    private fun text(resourceId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId)
}
