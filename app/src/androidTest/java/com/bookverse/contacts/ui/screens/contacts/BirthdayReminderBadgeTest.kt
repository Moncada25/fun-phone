package com.bookverse.contacts.ui.screens.contacts

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.R
import org.junit.Rule
import org.junit.Test

class BirthdayReminderBadgeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsActiveState_whenAccessIsReady() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent {
            MaterialTheme {
                BirthdayReminderBadgeContent(
                    ready = true,
                    accentColor = Color.Blue,
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.birthday_reminders_active)).assertIsDisplayed()
    }

    @Test
    fun showsRecoveryState_whenAccessIsMissing() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent {
            MaterialTheme {
                BirthdayReminderBadgeContent(
                    ready = false,
                    accentColor = Color.Blue,
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.birthday_reminders_enable)).assertIsDisplayed()
    }
}
