package com.bookverse.contacts.ui.screens.dialer

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.feature.dialer.R
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import com.bookverse.contacts.core.ui.R as CoreUiR

class DialerModernLayoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val testContent = SwappableComposeContent { composeRule.setContent(it) }

    @Test
    fun sheetActionsMeetMinimumTouchTarget() {
        showSheet(dialedNumber = "5550199")

        val minimumPixels = with(composeRule.density) { 48.dp.toPx() }
        val actions = composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        assertEquals(4, actions.size)
        actions.forEach { node ->
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
    fun primaryActionsDispatchCallbacks() {
        val callClicked = AtomicBoolean(false)
        val whatsAppClicked = AtomicBoolean(false)

        showSheet(
            dialedNumber = "5550199",
            onCall = { callClicked.set(true) },
            onWhatsApp = { whatsAppClicked.set(true) },
        )

        composeRule
            .onNodeWithText(composeRule.activity.getString(CoreUiR.string.call))
            .assertIsDisplayed()
            .performClick()
        composeRule
            .onNodeWithContentDescription(composeRule.activity.getString(CoreUiR.string.whatsapp_action))
            .assertIsDisplayed()
            .performClick()

        assertTrue(callClicked.get())
        assertTrue(whatsAppClicked.get())
    }

    @Test
    fun numberActionsAreOnlyEnabledWithNumber() {
        val addContactLabel = composeRule.activity.getString(R.string.add_contact)
        val deleteLabel = composeRule.activity.getString(CoreUiR.string.delete)

        showSheet()
        composeRule.onNodeWithContentDescription(addContactLabel).assertIsNotEnabled()
        composeRule.onNodeWithContentDescription(deleteLabel).assertIsNotEnabled()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.enter_a_number))
            .assertIsDisplayed()

        showSheet(dialedNumber = "5550199")
        composeRule.onNodeWithContentDescription(addContactLabel).assertIsEnabled()
        composeRule.onNodeWithContentDescription(deleteLabel).assertIsEnabled()
    }

    @Test
    fun deleteDispatchesTapAndLongPress() {
        val deleteCount = AtomicInteger(0)
        val longDeleteCount = AtomicInteger(0)
        val deleteLabel = composeRule.activity.getString(CoreUiR.string.delete)

        showSheet(
            dialedNumber = "5550199",
            onDelete = { deleteCount.incrementAndGet() },
            onLongDelete = { longDeleteCount.incrementAndGet() },
        )

        composeRule.onNodeWithContentDescription(deleteLabel).performClick()
        composeRule.onNodeWithContentDescription(deleteLabel).performTouchInput { longClick() }

        assertEquals(1, deleteCount.get())
        assertEquals(1, longDeleteCount.get())
    }

    @Test
    fun addContactCardDispatchesCallbackForUnknownNumber() {
        val addContactClicked = AtomicBoolean(false)

        testContent.show {
            MaterialTheme {
                DialerModernResults(
                    modifier = Modifier.fillMaxSize(),
                    topInset = 0.dp,
                    bottomInset = 0.dp,
                    dialedNumber = "5550199",
                    suggestionEntries = emptyList(),
                    filteredContacts = emptyList(),
                    unknownTitle = "Unknown",
                    accentColor = Color(0xFF2196F3),
                    textColor = MaterialTheme.colorScheme.onBackground,
                    loadContactPhoto = { null },
                    onContactCall = {},
                    onAddContact = { addContactClicked.set(true) },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.add_contact))
            .assertIsDisplayed()
            .performClick()

        assertTrue(addContactClicked.get())
    }

    @Test
    fun numberContextShowsCallingCodeAndMatchedContact() {
        showSheet(dialedNumber = "3105550101", numberContext = sampleContext)

        composeRule.onNodeWithTag(DIALER_CALLING_CODE_TAG)
            .assertIsDisplayed()
            .assertContentDescriptionEquals(
                composeRule.activity.getString(
                    R.string.dialer_calling_code_description,
                    "+57",
                    "Colombia",
                ),
            )
        composeRule.onNodeWithTag(DIALER_MATCHED_CONTACT_TAG)
            .assertIsDisplayed()
            .assertContentDescriptionEquals(
                composeRule.activity.getString(
                    R.string.dialer_matched_contact_description,
                    "Maria Fernanda",
                ),
            )
        assertEquals(4, composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
    }

    @Test
    fun numberContextIsAbsentWithoutCountryOrMatch() {
        showSheet(dialedNumber = "3105550101")

        composeRule.onNodeWithTag(DIALER_NUMBER_CONTEXT_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(DIALER_CALLING_CODE_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(DIALER_MATCHED_CONTACT_TAG).assertDoesNotExist()
    }

    @Test
    fun numberContextGrowsSheetUpwardWithoutMovingKeysOrEdges() {
        val callLabel = composeRule.activity.getString(CoreUiR.string.call)
        val deleteLabel = composeRule.activity.getString(CoreUiR.string.delete)
        val sheetHeight = AtomicReference(0.dp)

        showSheet(dialedNumber = "3105550101", onHeightChanged = sheetHeight::set)
        val plainHeight = sheetHeight.get()
        val plainCall = composeRule.onNodeWithText(callLabel).fetchSemanticsNode().boundsInRoot
        val plainDelete = composeRule.onNodeWithContentDescription(deleteLabel)
            .fetchSemanticsNode().boundsInRoot

        showSheet(
            dialedNumber = "3105550101",
            numberContext = sampleContext,
            onHeightChanged = sheetHeight::set,
        )
        val contextHeight = sheetHeight.get()
        val contextCall = composeRule.onNodeWithText(callLabel).fetchSemanticsNode().boundsInRoot
        val contextDelete = composeRule.onNodeWithContentDescription(deleteLabel)
            .fetchSemanticsNode().boundsInRoot
        val contextRow = composeRule.onNodeWithTag(DIALER_NUMBER_CONTEXT_TAG)
            .fetchSemanticsNode().boundsInRoot
        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot

        assertTrue("Sheet did not grow: $plainHeight -> $contextHeight", contextHeight > plainHeight)
        assertTrue("Context row is too tall: ${contextHeight - plainHeight}", contextHeight - plainHeight <= 48.dp)
        assertEquals(plainCall, contextCall)
        assertEquals(plainDelete, contextDelete)
        assertTrue("Context row overlaps the number row", contextRow.bottom <= contextDelete.top + 1f)
        assertTrue("Context row leaves the sheet", contextRow.left >= root.left && contextRow.right <= root.right)
    }

    @Test
    fun numberContextStaysUsableAtDoubleFontScale() {
        showSheet(dialedNumber = "3105550101", numberContext = sampleContext, fontScale = 2f)

        composeRule.onNodeWithTag(DIALER_CALLING_CODE_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(DIALER_MATCHED_CONTACT_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(CoreUiR.string.call)).assertIsDisplayed()

        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val code = composeRule.onNodeWithTag(DIALER_CALLING_CODE_TAG).fetchSemanticsNode().boundsInRoot
        val contact = composeRule.onNodeWithTag(DIALER_MATCHED_CONTACT_TAG)
            .fetchSemanticsNode().boundsInRoot
        assertTrue("Calling code is clipped: $code", code.left >= root.left && code.right <= root.right)
        assertTrue("Contact name is clipped: $contact", contact.left >= root.left && contact.right <= root.right)
        assertTrue("Calling code and contact overlap", code.right <= contact.left)

        val minimumPixels = with(composeRule.density) { 48.dp.toPx() }
        composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
            assertTrue(
                "Dialer action is smaller than 48dp: ${node.boundsInRoot}",
                node.boundsInRoot.width >= minimumPixels && node.boundsInRoot.height >= minimumPixels,
            )
        }
    }

    private val sampleContext = DialerNumberContext(
        callingCode = "+57",
        countryFlag = "\uD83C\uDDE8\uD83C\uDDF4",
        countryName = "Colombia",
        contactName = "Maria Fernanda",
    )

    private fun showSheet(
        dialedNumber: String = "",
        onCall: () -> Unit = {},
        onWhatsApp: () -> Unit = {},
        onDelete: () -> Unit = {},
        onLongDelete: () -> Unit = {},
        numberContext: DialerNumberContext = DialerNumberContext(),
        fontScale: Float = 1f,
        onHeightChanged: (Dp) -> Unit = {},
    ) {
        testContent.show {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale),
            ) {
                MaterialTheme {
                    DialerModernKeypadSheet(
                        modifier = Modifier.fillMaxSize(),
                        accentColor = Color(0xFF2196F3),
                        fontType = "default",
                        dtmfTonesEnabled = "disabled",
                        vibrationDuration = 0,
                        vibrationPattern = "default",
                        phoneFieldValue = TextFieldValue(
                            text = dialedNumber,
                            selection = TextRange(dialedNumber.length),
                        ),
                        dialedNumber = dialedNumber,
                        onValueChange = {},
                        registerEditText = {},
                        onAddContact = {},
                        onCall = onCall,
                        onWhatsApp = onWhatsApp,
                        onDelete = onDelete,
                        onLongDelete = onLongDelete,
                        onDigit = {},
                        onOneLongPress = {},
                        onNumberLongPress = {},
                        numberContext = numberContext,
                        onHeightChanged = onHeightChanged,
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }
}
