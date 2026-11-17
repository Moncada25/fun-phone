package com.bookverse.contacts.ui.screens.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bookverse.contacts.R
import com.bookverse.contacts.domain.call.CallScreeningSettings
import com.bookverse.contacts.domain.call.SimAccount
import com.bookverse.contacts.domain.call.SimPreference
import com.bookverse.contacts.testing.ComposeTestContent
import com.bookverse.contacts.testing.ComposeTestHostActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class CallSettingsSheetsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestHostActivity>()

    @Test
    fun spamRulesStartOffAndDispatchEachToggleSeparately() {
        val toggled = AtomicReference<Pair<String, Boolean>>()
        showScreeningSheet(
            settings = CallScreeningSettings(),
            onToggle = { rule, enabled -> toggled.set(rule to enabled) },
        )

        composeRule.onNodeWithTag(SPAM_RULE_BLOCKED_LIST_TAG).assertIsOff()
        composeRule.onNodeWithTag(SPAM_RULE_HIDDEN_TAG).assertIsOff()
        composeRule.onNodeWithTag(SPAM_RULE_NOT_IN_CONTACTS_TAG).assertIsOff()

        composeRule.onNodeWithTag(SPAM_RULE_HIDDEN_TAG).performClick()
        assertEquals("hidden" to true, toggled.get())
        composeRule.onNodeWithTag(SPAM_RULE_BLOCKED_LIST_TAG).performClick()
        assertEquals("listed" to true, toggled.get())
        composeRule.onNodeWithTag(SPAM_RULE_NOT_IN_CONTACTS_TAG).performClick()
        assertEquals("contacts" to true, toggled.get())
    }

    @Test
    fun enabledSpamRulesAreShownAsOn() {
        showScreeningSheet(
            settings = CallScreeningSettings(blockHiddenNumbers = true),
            roleActive = true,
        )

        composeRule.onNodeWithTag(SPAM_RULE_HIDDEN_TAG).assertIsOn()
        composeRule.onNodeWithTag(SPAM_RULE_BLOCKED_LIST_TAG).assertIsOff()
        composeRule.onNodeWithTag(SPAM_ROLE_REQUEST_TAG).assertDoesNotExist()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.call_screening_active))
            .assertIsDisplayed()
    }

    @Test
    fun missingScreeningRoleOffersTheRoleRequest() {
        val requested = AtomicBoolean(false)
        showScreeningSheet(
            settings = CallScreeningSettings(),
            roleActive = false,
            onRequestRole = { requested.set(true) },
        )

        composeRule.onNodeWithTag(SPAM_ROLE_REQUEST_TAG).assertIsDisplayed().performClick()

        assertTrue(requested.get())
    }

    @Test
    fun simPreferenceSheetOffersAskEveryTimeAndEverySim() {
        val selected = AtomicReference<SimPreference>()
        composeRule.runOnUiThread {
            ComposeTestContent.show {
                MaterialTheme {
                    CallSimPreferenceSheet(
                        accounts = listOf(
                            SimAccount(id = "telephony#1", label = "Personal"),
                            SimAccount(id = "telephony#2", label = "Work"),
                        ),
                        selected = SimPreference.AskEveryTime,
                        onSelected = selected::set,
                        onDismiss = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.call_sim_ask_every_time))
            .assertIsDisplayed()
        composeRule.onNodeWithText("Personal").assertIsDisplayed()
        composeRule.onNodeWithText("Work").assertIsDisplayed().performClick()

        assertEquals(SimPreference.Account("telephony#2"), selected.get())
    }

    private fun showScreeningSheet(
        settings: CallScreeningSettings,
        roleActive: Boolean = true,
        onToggle: (String, Boolean) -> Unit = { _, _ -> },
        onRequestRole: () -> Unit = {},
    ) {
        composeRule.runOnUiThread {
            ComposeTestContent.show {
                MaterialTheme {
                    CallScreeningSheet(
                        settings = settings,
                        roleActive = roleActive,
                        roleAvailable = true,
                        onBlockListedChanged = { onToggle("listed", it) },
                        onBlockHiddenChanged = { onToggle("hidden", it) },
                        onBlockNotInContactsChanged = { onToggle("contacts", it) },
                        onRequestRole = onRequestRole,
                        onDismiss = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }
}
