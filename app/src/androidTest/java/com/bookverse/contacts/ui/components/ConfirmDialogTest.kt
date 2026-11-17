package com.bookverse.contacts.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.R
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class ConfirmDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun destructiveConfirmationUsesTheDeleteActionAndCanBeConfirmed() {
        val confirmed = AtomicBoolean(false)
        val dismissed = AtomicBoolean(false)
        showDialog(
            onConfirm = { confirmed.set(true) },
            onDismiss = { dismissed.set(true) },
        )

        composeRule
            .onNodeWithText(text(R.string.delete))
            .assertIsDisplayed()
            .performClick()

        assertTrue(confirmed.get())
        assertTrue(dismissed.get())
    }

    @Test
    fun nonDestructiveConfirmationUsesAccentAction() {
        val confirmed = AtomicBoolean(false)
        showDialog(
            confirmButtonLabel = text(R.string.restore),
            isDestructive = false,
            onConfirm = { confirmed.set(true) },
        )

        composeRule
            .onNodeWithText(text(R.string.restore))
            .assertIsDisplayed()
            .performClick()

        assertTrue(confirmed.get())
    }

    @Test
    fun dismissActionsAreNotRendered() {
        showDialog(onConfirm = {})

        composeRule.onAllNodesWithContentDescription(text(R.string.close)).assertCountEquals(0)
        composeRule.onAllNodesWithText(text(R.string.cancel)).assertCountEquals(0)
    }

    private fun showDialog(
        confirmButtonLabel: String? = null,
        isDestructive: Boolean = true,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit = {},
    ) {
        val appSettingsViewModel = AppSettingsViewModel()
        composeRule.setContent {
            MaterialTheme {
                ConfirmDialog(
                    onDismiss = onDismiss,
                    onConfirm = onConfirm,
                    title = "Decision title",
                    message = "Decision message",
                    confirmButtonLabel = confirmButtonLabel,
                    isDestructive = isDestructive,
                    appSettingsViewModel = appSettingsViewModel,
                )
            }
        }
    }

    private fun text(resourceId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId)
}
