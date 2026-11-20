package com.bookverse.contacts.ui.screens.settings

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.bookverse.contacts.domain.call.CallScreeningOption
import com.bookverse.contacts.domain.call.CallScreeningSettings
import com.bookverse.contacts.domain.call.SimAccount
import com.bookverse.contacts.domain.call.SimPreference
import com.bookverse.contacts.feature.settings.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class CallSettingsSheetsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val testContent = SwappableComposeContent { composeRule.setContent(it) }

    @Test
    fun spamRulesStartOffAndDispatchEachToggleSeparately() {
        val toggled = AtomicReference<Pair<CallScreeningOption, Boolean>>()
        showScreeningSheet(
            settings = CallScreeningSettings(),
            onToggle = { rule, enabled -> toggled.set(rule to enabled) },
        )

        composeRule.onNodeWithTag(SPAM_RULE_BLOCKED_LIST_TAG).assertIsOff()
        composeRule.onNodeWithTag(SPAM_RULE_HIDDEN_TAG).assertIsOff()
        composeRule.onNodeWithTag(SPAM_RULE_NOT_IN_CONTACTS_TAG).assertIsOff()

        composeRule.onNodeWithTag(SPAM_RULE_HIDDEN_TAG).performClick()
        assertEquals(CallScreeningOption.BLOCK_HIDDEN to true, toggled.get())
        composeRule.onNodeWithTag(SPAM_RULE_BLOCKED_LIST_TAG).performClick()
        assertEquals(CallScreeningOption.BLOCK_LISTED to true, toggled.get())
        composeRule.onNodeWithTag(SPAM_RULE_NOT_IN_CONTACTS_TAG).performScrollTo().performClick()
        assertEquals(CallScreeningOption.BLOCK_NOT_IN_CONTACTS to true, toggled.get())
        composeRule.onNodeWithTag(SPAM_RULE_SPOOFED_TAG).performScrollTo().assertIsOff().performClick()
        assertEquals(CallScreeningOption.BLOCK_FAILED_VERIFICATION to true, toggled.get())
        composeRule.onNodeWithTag(SPAM_RULE_SILENCE_UNKNOWN_TAG).assertDoesNotExist()
    }

    @Test
    fun neighborRuleNeedsANumberInTheProfile() {
        val toggled = AtomicReference<Pair<CallScreeningOption, Boolean>>()
        showScreeningSheet(
            settings = CallScreeningSettings(),
            hasProfileNumber = false,
            onToggle = { rule, enabled -> toggled.set(rule to enabled) },
        )

        composeRule.onNodeWithTag(SPAM_RULE_NEIGHBOR_TAG).performScrollTo().assertIsOff().assertIsNotEnabled()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.spam_filter_neighbor_needs_profile))
            .assertExists()
        composeRule.onNodeWithTag(SPAM_RULE_NEIGHBOR_TAG).performClick()
        assertEquals(null, toggled.get())
    }

    @Test
    fun neighborRuleCanStillBeTurnedOffAfterTheProfileNumberIsRemoved() {
        val toggled = AtomicReference<Pair<CallScreeningOption, Boolean>>()
        showScreeningSheet(
            settings = CallScreeningSettings(blockNeighborNumbers = true),
            hasProfileNumber = false,
            onToggle = { rule, enabled -> toggled.set(rule to enabled) },
        )

        composeRule.onNodeWithTag(SPAM_RULE_NEIGHBOR_TAG).performScrollTo().assertIsOn().assertIsEnabled()
        composeRule.onNodeWithTag(SPAM_RULE_NEIGHBOR_TAG).performClick()
        assertEquals(CallScreeningOption.BLOCK_NEIGHBOR_NUMBERS to false, toggled.get())
    }

    @Test
    fun neighborRuleCanBeToggledWithAProfileNumber() {
        val toggled = AtomicReference<Pair<CallScreeningOption, Boolean>>()
        showScreeningSheet(
            settings = CallScreeningSettings(),
            hasProfileNumber = true,
            onToggle = { rule, enabled -> toggled.set(rule to enabled) },
        )

        composeRule.onNodeWithTag(SPAM_RULE_NEIGHBOR_TAG).performScrollTo().assertIsOff().assertIsEnabled()
        composeRule.onNodeWithTag(SPAM_RULE_NEIGHBOR_TAG).performClick()
        assertEquals(CallScreeningOption.BLOCK_NEIGHBOR_NUMBERS to true, toggled.get())
    }

    @Test
    fun unknownCallerOptionsAppearOnlyWithTheirRule() {
        val toggled = AtomicReference<Pair<CallScreeningOption, Boolean>>()
        showScreeningSheet(
            settings = CallScreeningSettings(
                blockNumbersNotInContacts = true,
                allowRecentlyDialed = true,
            ),
            onToggle = { rule, enabled -> toggled.set(rule to enabled) },
        )

        composeRule.onNodeWithTag(SPAM_RULE_SILENCE_UNKNOWN_TAG).performScrollTo().assertIsOff()
        composeRule.onNodeWithTag(SPAM_RULE_ALLOW_RECENT_TAG).performScrollTo().assertIsOn()
        composeRule.onNodeWithTag(SPAM_RULE_ALLOW_REPEATED_TAG).performScrollTo().assertIsOff()

        composeRule.onNodeWithTag(SPAM_RULE_SILENCE_UNKNOWN_TAG).performScrollTo().performClick()
        assertEquals(CallScreeningOption.SILENCE_NOT_IN_CONTACTS to true, toggled.get())
        composeRule.onNodeWithTag(SPAM_RULE_ALLOW_RECENT_TAG).performScrollTo().performClick()
        assertEquals(CallScreeningOption.ALLOW_RECENTLY_DIALED to false, toggled.get())
        composeRule.onNodeWithTag(SPAM_RULE_ALLOW_REPEATED_TAG).performScrollTo().performClick()
        assertEquals(CallScreeningOption.ALLOW_REPEATED_CALLS to true, toggled.get())
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
        testContent.show {
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
        hasProfileNumber: Boolean = true,
        onToggle: (CallScreeningOption, Boolean) -> Unit = { _, _ -> },
        onRequestRole: () -> Unit = {},
    ) {
        testContent.show {
            MaterialTheme {
                CallScreeningSheet(
                    settings = settings,
                    roleActive = roleActive,
                    roleAvailable = true,
                    hasProfileNumber = hasProfileNumber,
                    onOptionChanged = onToggle,
                    onRequestRole = onRequestRole,
                    onDismiss = {},
                )
            }
        }
        composeRule.waitForIdle()
    }
}
