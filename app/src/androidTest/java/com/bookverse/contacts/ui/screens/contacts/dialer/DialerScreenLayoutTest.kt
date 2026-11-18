package com.bookverse.contacts.ui.screens.contacts.dialer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.R
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import com.bookverse.contacts.testing.ComposeTestContent
import com.bookverse.contacts.testing.ComposeTestHostActivity
import com.bookverse.contacts.ui.theme.AccentBarStyle
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DialerScreenLayoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestHostActivity>()

    @Test
    fun primaryActionsAreDisplayedAndMeetMinimumTouchTarget() {
        showLayout()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.call))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.recent_type_whatsapp))
            .assertIsDisplayed()

        val minimumPixels = with(composeRule.density) { 48.dp.toPx() }
        composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
            assertTrue(
                "Dialer action is narrower than 48dp: ${node.boundsInRoot}",
                node.boundsInRoot.width >= minimumPixels,
            )
            assertTrue(
                "Dialer action is shorter than 48dp: ${node.boundsInRoot}",
                node.boundsInRoot.height >= minimumPixels,
            )
        }
    }

    @Test
    fun contactResultDispatchesExactPhoneNumber() {
        val calledNumber = AtomicReference<String>()
        val contact = Contact(
            id = "dialer-test-contact",
            name = "Ada Lovelace",
            phones = listOf(ContactPhone(number = "5550100", type = null)),
        )

        showLayout(
            dialedNumber = "5550100",
            filteredContacts = listOf(
                DialerContactSearchIndex(
                    contact = contact,
                    normalizedPhones = listOf("5550100"),
                    primaryPhone = "5550100",
                ),
            ),
            onContactCall = calledNumber::set,
        )

        composeRule.onNodeWithText("Ada Lovelace").assertIsDisplayed()
        composeRule
            .onAllNodes(
                hasContentDescription(composeRule.activity.getString(R.string.call)),
            )[0]
            .performClick()

        assertEquals("5550100", calledNumber.get())
    }

    @Test
    fun addContactCardDispatchesCallbackForUnknownNumber() {
        val addContactClicked = AtomicBoolean(false)

        showLayout(
            dialedNumber = "5550199",
            onAddContact = { addContactClicked.set(true) },
        )

        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.add_contact))
            .assertIsDisplayed()
            .performClick()

        assertTrue(addContactClicked.get())
    }

    @Test
    fun addContactButtonIsAlwaysVisibleButOnlyEnabledWithNumber() {
        val addContactLabel = composeRule.activity.getString(R.string.add_contact)

        showLayout()
        composeRule
            .onNodeWithContentDescription(addContactLabel)
            .assertIsDisplayed()
            .assertIsNotEnabled()

        showLayout(dialedNumber = "5550199")
        composeRule
            .onNodeWithContentDescription(addContactLabel)
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun deleteButtonUsesDisabledStateWithoutNumber() {
        val deleteLabel = composeRule.activity.getString(R.string.delete)

        showLayout()
        composeRule
            .onNodeWithContentDescription(deleteLabel)
            .assertIsDisplayed()
            .assertIsNotEnabled()

        showLayout(dialedNumber = "5550199")
        composeRule
            .onNodeWithContentDescription(deleteLabel)
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun numberContextShowsCallingCodeAndContactInMinimalAndRetro() {
        val context = DialerNumberContext(
            callingCode = "+57",
            countryFlag = "\uD83C\uDDE8\uD83C\uDDF4",
            countryName = "Colombia",
            contactName = "Maria Fernanda",
        )
        val deleteLabel = composeRule.activity.getString(R.string.delete)

        listOf(AccentBarStyle.MINIMAL, AccentBarStyle.RETRO).forEach { style ->
            showLayout(dialedNumber = "3105550101", numberContext = context, accentBarStyle = style)

            composeRule.onNodeWithTag(DIALER_CALLING_CODE_TAG).assertIsDisplayed()
            composeRule.onNodeWithTag(DIALER_MATCHED_CONTACT_TAG).assertIsDisplayed()
            val contextRow = composeRule.onNodeWithTag(DIALER_NUMBER_CONTEXT_TAG)
                .fetchSemanticsNode().boundsInRoot
            val delete = composeRule.onNodeWithContentDescription(deleteLabel)
                .fetchSemanticsNode().boundsInRoot
            assertTrue("Context row overlaps the number row in $style", contextRow.bottom <= delete.top + 1f)
        }
    }

    @Test
    fun numberContextDoesNotMoveTheKeypadActions() {
        val callLabel = composeRule.activity.getString(R.string.call)

        showLayout(dialedNumber = "3105550101")
        composeRule.onNodeWithTag(DIALER_NUMBER_CONTEXT_TAG).assertDoesNotExist()
        val plainCall = composeRule.onNodeWithText(callLabel).fetchSemanticsNode().boundsInRoot

        showLayout(
            dialedNumber = "3105550101",
            numberContext = DialerNumberContext(contactName = "Maria Fernanda"),
        )
        composeRule.onNodeWithTag(DIALER_CALLING_CODE_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(DIALER_MATCHED_CONTACT_TAG).assertIsDisplayed()
        val contextCall = composeRule.onNodeWithText(callLabel).fetchSemanticsNode().boundsInRoot

        assertEquals(plainCall, contextCall)
    }

    private fun showLayout(
        dialedNumber: String = "",
        filteredContacts: List<DialerContactSearchIndex> = emptyList(),
        onContactCall: (String) -> Unit = {},
        onAddContact: () -> Unit = {},
        numberContext: DialerNumberContext = DialerNumberContext(),
        accentBarStyle: AccentBarStyle = AccentBarStyle.MINIMAL,
    ) {
        composeRule.runOnUiThread {
            ComposeTestContent.show {
                MaterialTheme {
                    DialerScreenLayout(
                        modifier = Modifier.fillMaxSize(),
                        accentColor = Color(0xFF2196F3),
                        textColor = MaterialTheme.colorScheme.onBackground,
                        accentBarStyle = accentBarStyle,
                        fontType = "default",
                        dtmfTonesEnabled = "disabled",
                        vibrationDuration = 0,
                        vibrationPattern = "default",
                        phoneFieldValue = TextFieldValue(
                            text = dialedNumber,
                            selection = TextRange(dialedNumber.length),
                        ),
                        dialedNumber = dialedNumber,
                        suggestionEntries = emptyList(),
                        filteredContacts = filteredContacts,
                        unknownTitle = "Unknown",
                        onValueChange = {},
                        registerEditText = {},
                        onContactCall = onContactCall,
                        onAddContact = onAddContact,
                        onCall = {},
                        onWhatsApp = {},
                        onDelete = {},
                        onLongDelete = {},
                        onDigit = {},
                        onOneLongPress = {},
                        onNumberLongPress = {},
                        loadContactPhoto = { null },
                        numberContext = numberContext,
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }
}
