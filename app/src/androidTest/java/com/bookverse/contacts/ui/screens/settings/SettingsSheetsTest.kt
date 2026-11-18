package com.bookverse.contacts.ui.screens.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.R as Material3R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.R
import com.bookverse.contacts.ui.theme.DarkModeStyle
import com.bookverse.contacts.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class SettingsSheetsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun simpleSelectionAppliesImmediatelyAndDismisses() {
        val selected = AtomicReference<String>()
        val dismissed = AtomicBoolean(false)
        composeRule.setContent {
            MaterialTheme {
                SettingsSelectionSheet(
                    title = "Selection title",
                    options = listOf("First", "Second"),
                    selectedOption = "First",
                    onSelected = selected::set,
                    onDismiss = { dismissed.set(true) },
                    optionKey = { it },
                    optionLabel = { it },
                )
            }
        }

        composeRule.onNodeWithText("Second").performClick()

        assertEquals("Second", selected.get())
        assertTrue(dismissed.get())
    }

    @Test
    fun accentColorSelectionEmitsTheChosenSeedKey() {
        val selected = AtomicReference<String>()
        val colors = linkedMapOf(
            "blue" to Color(0xFF1E88E5),
            "orange" to Color(0xFFFB8C00),
        )
        composeRule.setContent {
            MaterialTheme {
                SettingsColorSheet(
                    selectedColor = colors.getValue("blue"),
                    availableColors = colors,
                    onColorSelected = selected::set,
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.orange)).performClick()

        assertEquals("orange", selected.get())
    }

    @Test
    fun longSelectionListScrollsInsideTheExpandedSheet() {
        val selected = AtomicReference<String>()
        val dismissed = AtomicBoolean(false)
        composeRule.setContent {
            MaterialTheme {
                SettingsSelectionSheet(
                    title = "Long selection",
                    options = List(40) { index -> "Option $index" },
                    selectedOption = null,
                    onSelected = selected::set,
                    onDismiss = { dismissed.set(true) },
                    optionKey = { it },
                    optionLabel = { it },
                )
            }
        }

        composeRule.onNode(hasScrollAction()).performTouchInput {
            swipeDown(durationMillis = 600)
        }
        assertTrue(!dismissed.get())

        composeRule.onNode(hasScrollAction()).performScrollToIndex(39)
        composeRule.onNodeWithText("Option 39").performClick()

        assertEquals("Option 39", selected.get())
    }

    @Test
    fun rapidBoundaryFlingsDoNotDismissTheExpandedSheet() {
        val dismissed = AtomicBoolean(false)
        composeRule.setContent {
            MaterialTheme {
                SettingsSelectionSheet(
                    title = "Long selection",
                    options = List(40) { index -> "Option $index" },
                    selectedOption = null,
                    onSelected = {},
                    onDismiss = { dismissed.set(true) },
                    optionKey = { it },
                    optionLabel = { it },
                )
            }
        }

        val list = composeRule.onNode(hasScrollAction())
        list.performScrollToIndex(39)
        list.performTouchInput { swipeUp(durationMillis = 50) }
        list.performScrollToIndex(0)
        list.performTouchInput { swipeDown(durationMillis = 50) }

        composeRule.onNodeWithText("Long selection").assertIsDisplayed()
        assertTrue(!dismissed.get())
    }

    @Test
    fun draggingTheHandleStillDismissesTheSheet() {
        val dismissed = AtomicBoolean(false)
        composeRule.setContent {
            MaterialTheme {
                SettingsSelectionSheet(
                    title = "Selection title",
                    options = listOf("First"),
                    selectedOption = "First",
                    onSelected = {},
                    onDismiss = { dismissed.set(true) },
                    optionKey = { it },
                    optionLabel = { it },
                )
            }
        }

        composeRule.onNodeWithContentDescription(dragHandleDescription()).performTouchInput {
            swipe(
                start = center,
                end = Offset(x = center.x, y = center.y + 800f),
                durationMillis = 100,
            )
        }

        composeRule.waitUntil(timeoutMillis = 3_000) { dismissed.get() }
    }

    @Test
    fun backRemainsAValidDismissAction() {
        val dismissedByBack = AtomicBoolean(false)
        composeRule.setContent {
            MaterialTheme {
                SettingsSelectionSheet(
                    title = "Back dismissal",
                    options = listOf("First"),
                    selectedOption = "First",
                    onSelected = {},
                    onDismiss = { dismissedByBack.set(true) },
                    optionKey = { it },
                    optionLabel = { it },
                )
            }
        }

        pressBack()
        composeRule.waitUntil(timeoutMillis = 3_000) { dismissedByBack.get() }
    }

    @Test
    fun scrimRemainsAValidDismissAction() {
        val dismissedByScrim = AtomicBoolean(false)
        composeRule.setContent {
            MaterialTheme {
                SettingsSelectionSheet(
                    title = "Scrim dismissal",
                    options = listOf("First"),
                    selectedOption = "First",
                    onSelected = {},
                    onDismiss = { dismissedByScrim.set(true) },
                    optionKey = { it },
                    optionLabel = { it },
                )
            }
        }

        composeRule.onNode(
            isRoot() and hasAnyDescendant(hasText("Scrim dismissal")),
            useUnmergedTree = true,
        ).performTouchInput {
            click(Offset(x = center.x, y = 4f))
        }
        composeRule.waitUntil(timeoutMillis = 3_000) { dismissedByScrim.get() }
    }

    @Test
    fun settingsSheetUsesNativeDragHandle() {
        composeRule.setContent {
            MaterialTheme {
                SettingsSelectionSheet(
                    title = "Selection title",
                    options = listOf("First"),
                    selectedOption = "First",
                    onSelected = {},
                    onDismiss = {},
                    optionKey = { it },
                    optionLabel = { it },
                )
            }
        }

        composeRule
            .onNodeWithContentDescription(dragHandleDescription())
            .assertIsDisplayed()
    }

    @Test
    fun settingsSheetDoesNotRenderApplyCancelOrCloseActions() {
        composeRule.setContent {
            MaterialTheme {
                SettingsSelectionSheet(
                    title = "Selection title",
                    options = listOf("First"),
                    selectedOption = "First",
                    onSelected = {},
                    onDismiss = {},
                    optionKey = { it },
                    optionLabel = { it },
                )
            }
        }

        composeRule.onAllNodesWithText(text(R.string.apply)).assertCountEquals(0)
        composeRule.onAllNodesWithText(text(R.string.cancel)).assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription(text(R.string.close)).assertCountEquals(0)
    }

    @Test
    fun themeSelectionsApplyLiveWithoutClosingTheSheet() {
        val selectedMode = AtomicReference(ThemeMode.SYSTEM)
        val selectedStyle = AtomicReference(DarkModeStyle.AMOLED)
        composeRule.setContent {
            MaterialTheme {
                var mode by remember { mutableStateOf(ThemeMode.SYSTEM) }
                var style by remember { mutableStateOf(DarkModeStyle.AMOLED) }
                SettingsThemeSheet(
                    selectedMode = mode,
                    selectedDarkStyle = style,
                    themeModes = ThemeMode.entries,
                    darkModeStyles = DarkModeStyle.entries,
                    onThemeModeSelected = {
                        mode = it
                        selectedMode.set(it)
                    },
                    onDarkModeStyleSelected = {
                        style = it
                        selectedStyle.set(it)
                    },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.theme_dark)).performClick()
        composeRule.onNodeWithText(text(R.string.dark_style_dark_gray)).performClick()

        assertEquals(ThemeMode.DARK, selectedMode.get())
        assertEquals(DarkModeStyle.DARK_GRAY, selectedStyle.get())
        composeRule.onNodeWithText(text(R.string.theme)).assertIsDisplayed()
    }

    @Test
    fun speedDialContactPickerReturnsToNumbersWithinTheSameSheet() {
        composeRule.setContent {
            MaterialTheme {
                SpeedDialConfigSheet(
                    onDismiss = {},
                    contacts = emptyList(),
                    speedDialContacts = emptyMap(),
                    accentColor = Color(0xFF6750A4),
                    onAssignmentChanged = { _, _ -> },
                    loadContactPhoto = { null },
                )
            }
        }

        composeRule.onNodeWithText("0").performClick()
        composeRule.onNodeWithText(text(R.string.speed_dial_assign_contact)).assertIsDisplayed()

        pressBack()

        composeRule.onNodeWithText(text(R.string.speed_dial_configure)).assertIsDisplayed()
    }

    private fun text(resourceId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId)

    private fun dragHandleDescription(): String =
        InstrumentationRegistry.getInstrumentation().targetContext
            .getString(Material3R.string.m3c_bottom_sheet_drag_handle_description)
}
