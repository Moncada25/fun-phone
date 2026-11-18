package com.bookverse.contacts.ui.screens.notes

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.notes.ChecklistItem
import com.bookverse.contacts.ui.theme.AccentBarStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class ChecklistItemEditSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun editingTextUpdatesTheItemImmediately() {
        val updatedText = AtomicReference<String>()
        showSheet(
            onTextChange = { updatedText.set(it) },
        )

        composeRule
            .onNode(hasSetTextAction())
            .performTextReplacement("Updated checklist item")

        assertEquals("Updated checklist item", updatedText.get())
    }

    @Test
    fun togglingCheckUpdatesTheItemAndActionLabelImmediately() {
        val checked = AtomicBoolean(false)
        showSheet(
            onToggleChecked = { checked.set(true) },
            checked = false,
        )

        composeRule
            .onNodeWithText(text(R.string.checklist_mark_complete))
            .performClick()
        composeRule.onNodeWithText(text(R.string.checklist_mark_incomplete)).assertIsDisplayed()

        assertTrue(checked.get())
    }

    @Test
    fun copyingUsesTheCurrentItemContent() {
        val copiedText = AtomicReference<String>()
        showSheet(
            onCopy = { copiedText.set(it) },
        )

        composeRule
            .onNode(hasSetTextAction())
            .performTextReplacement("Updated checklist item")
        composeRule
            .onNodeWithText(text(R.string.checklist_copy_item))
            .performClick()

        assertEquals("Updated checklist item", copiedText.get())
    }

    @Test
    fun closeButtonIsNotRendered() {
        showSheet()

        composeRule.onAllNodesWithContentDescription(text(R.string.close)).assertCountEquals(0)
    }

    private fun showSheet(
        onTextChange: (String) -> Unit = {},
        onToggleChecked: () -> Unit = {},
        onCopy: (String) -> Unit = {},
        onDismiss: () -> Unit = {},
        checked: Boolean = false,
    ) {
        val initialItem = ChecklistItem(
            id = 1L,
            noteId = 1L,
            text = "Current checklist item",
            isChecked = checked,
        )
        composeRule.setContent {
            MaterialTheme {
                var item by remember { mutableStateOf(initialItem) }
                ChecklistItemEditSheet(
                    item = item,
                    accentColor = Color(0xFF6750A4),
                    accentBarStyle = AccentBarStyle.MINIMAL,
                    onTextChange = { text ->
                        item = item.copy(text = text)
                        onTextChange(text)
                    },
                    onToggleChecked = {
                        item = item.copy(isChecked = !item.isChecked)
                        onToggleChecked()
                    },
                    onCopy = { onCopy(item.text) },
                    onDismiss = onDismiss,
                )
            }
        }
    }

    private fun text(resourceId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId)
}
