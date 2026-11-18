package com.bookverse.contacts.ui.screens.calls

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.domain.call.SimAccount
import com.bookverse.contacts.testing.ComposeTestContent
import com.bookverse.contacts.testing.ComposeTestHostActivity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class CallSheetsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestHostActivity>()

    private val simOne = SimAccount(id = "telephony#1", label = "Personal", detail = "+57 310 555 0101")
    private val simTwo = SimAccount(id = "telephony#2", label = "Work")

    @Test
    fun simSheetListsEverySimAndReturnsTheChosenOne() {
        val selection = AtomicReference<Pair<SimAccount, Boolean>>()
        showSimSheet { account, remember -> selection.set(account to remember) }

        composeRule.onNodeWithText("Personal").assertIsDisplayed()
        composeRule.onNodeWithText("+57 310 555 0101").assertIsDisplayed()
        composeRule.onNodeWithText("3205550199").assertIsDisplayed()
        composeRule.onNodeWithTag(simSelectionOptionTag(simTwo.id)).performClick()

        assertEquals(simTwo to false, selection.get())
    }

    @Test
    fun simSheetReportsTheRememberChoice() {
        val selection = AtomicReference<Pair<SimAccount, Boolean>>()
        showSimSheet { account, remember -> selection.set(account to remember) }

        composeRule.onNodeWithTag(SIM_SELECTION_REMEMBER_TAG).performClick()
        composeRule.onNodeWithTag(simSelectionOptionTag(simOne.id)).performClick()

        assertEquals(simOne to true, selection.get())
    }

    @Test
    fun rejectSheetSendsAQuickReply() {
        val sent = AtomicReference<String>()
        showRejectSheet(sent::set)

        composeRule.onNodeWithTag(rejectQuickReplyTag(1)).assertIsDisplayed().performClick()

        assertEquals(
            composeRule.activity.getString(R.string.reject_message_quick_call_back),
            sent.get(),
        )
    }

    @Test
    fun rejectSheetSendsOnlyNonBlankCustomText() {
        val sent = AtomicReference<String>()
        showRejectSheet(sent::set)

        composeRule.onNodeWithTag(REJECT_MESSAGE_SEND_TAG).assertIsNotEnabled()
        composeRule.onNodeWithTag(REJECT_MESSAGE_INPUT_TAG).performTextInput("  Back in   ten minutes ")
        composeRule.onNodeWithTag(REJECT_MESSAGE_SEND_TAG).assertIsEnabled().performClick()

        assertEquals("Back in ten minutes", sent.get())
    }

    private fun showSimSheet(onSelected: (SimAccount, Boolean) -> Unit) {
        composeRule.runOnUiThread {
            ComposeTestContent.show {
                MaterialTheme {
                    SimSelectionSheet(
                        number = "3205550199",
                        accounts = listOf(simOne, simTwo),
                        onSelected = onSelected,
                        onDismiss = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun showRejectSheet(onSend: (String) -> Unit) {
        composeRule.runOnUiThread {
            ComposeTestContent.show {
                MaterialTheme {
                    RejectWithMessageSheet(onSend = onSend, onDismiss = {})
                }
            }
        }
        composeRule.waitForIdle()
    }
}
