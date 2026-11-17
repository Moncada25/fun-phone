package com.bookverse.contacts.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.R as Material3R
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.R
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class OptionsDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun itemOptionsUseSettingsSheetShellAndButtonSemantics() {
        composeRule.setContent {
            MaterialTheme {
                OptionsDialog(
                    title = "More options",
                    options = listOf(
                        OptionsDialogItem(
                            label = "Settings",
                            icon = Icons.Default.Settings,
                            onClick = {},
                        ),
                    ),
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("More options").assertIsDisplayed()
        composeRule.onNodeWithText("Settings")
            .assert(hasClickAction())
        composeRule.onNodeWithContentDescription(
            InstrumentationRegistry.getInstrumentation().targetContext
                .getString(Material3R.string.m3c_bottom_sheet_drag_handle_description)
        ).assertIsDisplayed()
    }

    @Test
    fun clickingAnOptionDismissesBeforeRunningItsAction() {
        val dismissed = AtomicBoolean(false)
        val clicked = AtomicBoolean(false)
        composeRule.setContent {
            MaterialTheme {
                OptionsDialog(
                    title = "More options",
                    options = listOf(
                        OptionsDialogItem(
                            label = "Run action",
                            icon = Icons.Default.Settings,
                            onClick = {
                                assertTrue(dismissed.get())
                                clicked.set(true)
                            },
                        ),
                    ),
                    onDismiss = { dismissed.set(true) },
                )
            }
        }

        composeRule.onNodeWithText("Run action").performClick()

        assertTrue(clicked.get())
    }

    @Test
    fun disabledInfoRowCannotBeActivatedAndSelectedStateIsAnnounced() {
        val selectedDescription = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.cd_selected)
        composeRule.setContent {
            MaterialTheme {
                OptionsDialog(
                    title = "More options",
                    options = listOf(
                        OptionsDialogItem(
                            label = "Selected action",
                            icon = Icons.Default.Settings,
                            selected = true,
                            onClick = {},
                        ),
                        OptionsDialogItem(
                            label = "Metadata",
                            icon = null,
                            description = "Updated today",
                            enabled = false,
                            onClick = {},
                        ),
                        OptionsDialogItem(
                            label = "Delete",
                            icon = Icons.Default.Delete,
                            destructive = true,
                            onClick = {},
                        ),
                    ),
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription(selectedDescription).assertIsDisplayed()
        composeRule.onNodeWithText("Metadata").assertIsNotEnabled()
        composeRule.onNodeWithText("Delete")
            .assert(hasClickAction())
    }

    @Test
    fun longOptionListsScrollInsideTheSheet() {
        composeRule.setContent {
            MaterialTheme {
                OptionsDialog(
                    title = "Long options",
                    options = List(40) { index ->
                        OptionsDialogItem(
                            label = "Option $index",
                            icon = Icons.Default.Settings,
                            onClick = {},
                        )
                    },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNode(hasScrollAction()).performScrollToIndex(39)
        composeRule.onNodeWithText("Option 39").assertIsDisplayed()
        composeRule.onAllNodesWithText("Option 39").assertCountEquals(1)
    }
}
