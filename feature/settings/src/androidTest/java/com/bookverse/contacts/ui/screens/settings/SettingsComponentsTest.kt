package com.bookverse.contacts.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class SettingsComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sectionStartsCollapsedAndRestoresItsExpandedState() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            MaterialTheme {
                SettingsSectionCard(
                    section = testSection(),
                )
            }
        }

        composeRule.onNodeWithText("Theme option").assertDoesNotExist()
        composeRule.onNodeWithText("Appearance section").performClick()
        composeRule.onNodeWithText("Theme option").assertIsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("Theme option").assertIsDisplayed()
    }

    @Test
    fun itemDisplaysDescriptionAndCurrentValue() {
        composeRule.setContent {
            MaterialTheme {
                SettingsItemRow(
                    item = testSection().items.single(),
                )
            }
        }

        composeRule.onNodeWithText("Theme option").assertIsDisplayed()
        composeRule.onNodeWithText("Controls the visual theme").assertIsDisplayed()
        composeRule.onNodeWithText("System · AMOLED").assertIsDisplayed()
    }

    @Test
    fun toggleRowChangesStateOnlyOncePerClick() {
        val changes = AtomicInteger(0)
        composeRule.setContent {
            MaterialTheme {
                SettingsToggleItem(
                    item = SettingsItemModel(
                        key = "toggle",
                        icon = Icons.Default.Settings,
                        title = "Toggle setting",
                        description = "A setting controlled by a switch",
                        onClick = {},
                        toggle = SettingsToggleModel(
                            checked = false,
                            onCheckedChange = { changes.incrementAndGet() },
                        ),
                    ),
                )
            }
        }

        composeRule.onNodeWithText("Toggle setting").performClick()

        assertEquals(1, changes.get())
    }

    private fun testSection() = SettingsSectionModel(
        key = "appearance",
        title = "Appearance section",
        description = "Visual settings",
        items = listOf(
            SettingsItemModel(
                key = "theme",
                icon = Icons.Default.Settings,
                title = "Theme option",
                description = "Controls the visual theme",
                currentValue = "System · AMOLED",
                onClick = {},
            ),
        ),
    )
}
