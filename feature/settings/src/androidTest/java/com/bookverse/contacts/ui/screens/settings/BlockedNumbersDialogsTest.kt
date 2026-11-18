package com.bookverse.contacts.ui.screens.settings

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.bookverse.contacts.data.repository.BlockedNumberInfo
import com.bookverse.contacts.feature.settings.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference
import com.bookverse.contacts.core.ui.R as CoreUiR

class BlockedNumbersDialogsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val testContent = SwappableComposeContent { composeRule.setContent(it) }

    @Test
    fun emptyListExplainsThatNoNumbersAreBlocked() {
        showDialog(blockedNumbers = emptyList())

        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.blocked_numbers_empty_title))
            .assertIsDisplayed()
    }

    @Test
    fun confirmingStandaloneNumberReturnsItsNormalizedIdentity() {
        val unblockedNumber = AtomicReference<String>()
        showDialog(
            blockedNumbers = listOf(
                BlockedNumberInfo(
                    id = 1L,
                    displayNumber = "(555) 010-2000",
                    normalizedNumber = "+15550102000",
                    contact = null,
                ),
            ),
            onUnblockNumber = unblockedNumber::set,
        )

        val unblockLabel = composeRule.activity.getString(CoreUiR.string.contact_unblock_action)
        composeRule.onNodeWithText(unblockLabel).performClick()
        composeRule
            .onNodeWithText(
                composeRule.activity.getString(
                    R.string.blocked_numbers_unblock_confirmation_title,
                ),
            )
            .assertIsDisplayed()
        composeRule.onNodeWithText(unblockLabel).performClick()

        assertEquals("+15550102000", unblockedNumber.get())
    }

    @Test
    fun addingNumberReturnsItsSanitizedValue() {
        val blockedNumber = AtomicReference<String>()
        showDialog(blockedNumbers = emptyList(), onBlockNumber = blockedNumber::set)

        composeRule.onNodeWithTag(BLOCKED_NUMBER_ADD_BUTTON_TAG).performClick()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.blocked_numbers_add_title))
            .assertIsDisplayed()
        composeRule.onNodeWithTag(BLOCKED_NUMBER_INPUT_TAG).performTextInput("+57 (310) 555-0101")
        clickConfirm()

        assertEquals("+573105550101", blockedNumber.get())
        composeRule.onNodeWithTag(BLOCKED_NUMBER_INPUT_TAG).assertDoesNotExist()
    }

    @Test
    fun addingInvalidOrDuplicateNumberShowsAnErrorWithoutBlocking() {
        val blockedNumber = AtomicReference<String>()
        showDialog(
            blockedNumbers = listOf(
                BlockedNumberInfo(
                    id = 1L,
                    displayNumber = "(310) 555-0101",
                    normalizedNumber = "+573105550101",
                    contact = null,
                ),
            ),
            onBlockNumber = blockedNumber::set,
        )

        composeRule.onNodeWithTag(BLOCKED_NUMBER_ADD_BUTTON_TAG).performClick()
        clickConfirm()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.blocked_numbers_add_error_empty))
            .assertIsDisplayed()

        composeRule.onNodeWithTag(BLOCKED_NUMBER_INPUT_TAG).performTextInput("call me")
        clickConfirm()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.blocked_numbers_add_error_invalid))
            .assertIsDisplayed()

        composeRule.onNodeWithTag(BLOCKED_NUMBER_INPUT_TAG).performTextClearance()
        composeRule.onNodeWithTag(BLOCKED_NUMBER_INPUT_TAG).performTextInput("310 555 0101")
        clickConfirm()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.blocked_numbers_add_error_duplicate))
            .assertIsDisplayed()

        assertNull(blockedNumber.get())
    }

    private fun clickConfirm() {
        composeRule
            .onNodeWithTag(BLOCKED_NUMBER_CONFIRM_TAG)
            .performSemanticsAction(SemanticsActions.OnClick)
    }

    private fun showDialog(
        blockedNumbers: List<BlockedNumberInfo>,
        onUnblockNumber: (String) -> Unit = {},
        onBlockNumber: (String) -> Unit = {},
    ) {
        testContent.show {
            MaterialTheme {
                BlockedNumbersSheet(
                    blockedNumbers = blockedNumbers,
                    accentColor = Color(0xFF2196F3),
                    textColor = MaterialTheme.colorScheme.onSurface,
                    secondaryTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    onDismiss = {},
                    onUnblockContact = {},
                    onUnblockNumber = onUnblockNumber,
                    onBlockNumber = onBlockNumber,
                )
            }
        }
    }
}
