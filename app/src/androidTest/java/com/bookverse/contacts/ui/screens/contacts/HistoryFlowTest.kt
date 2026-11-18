package com.bookverse.contacts.ui.screens.contacts

import com.bookverse.contacts.ui.screens.history.HistoryScreen
import com.bookverse.contacts.utils.PhoneActions
import android.os.Build
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import com.bookverse.contacts.App
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import com.bookverse.contacts.data.model.RecentType
import com.bookverse.contacts.testing.ComposeTestContent
import com.bookverse.contacts.testing.ComposeTestHostActivity
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.ui.viewmodel.NavigationViewModel
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import com.bookverse.contacts.feature.dialer.R as DialerR

class HistoryFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestHostActivity>()

    @Test
    fun searchThenDeleteRemovesOnlySelectedLocalHistory() {
        assumeTrue("Synthetic history mutation runs only on an emulator", isEmulator())
        val suffix = System.currentTimeMillis().toString().takeLast(8)
        val selectedName = "FunPhoneHistory Alpha $suffix"
        val retainedName = "FunPhoneHistory Beta $suffix"
        val app = composeRule.activity.application as App
        val contactsViewModel = ViewModelProvider(
            composeRule.activity,
            app.container.contactsViewModelFactory,
        )[ContactsViewModel::class.java]
        val settingsViewModel = AppSettingsViewModel()
        val navigationViewModel = NavigationViewModel(SavedStateHandle())

        addHistory(contactsViewModel, selectedName, "5550101001")
        addHistory(contactsViewModel, retainedName, "5550101002")
        composeRule.waitUntil(timeoutMillis = 10_000) {
            contactsViewModel.localOperations.value.any { it.contact.name == selectedName } &&
                contactsViewModel.localOperations.value.any { it.contact.name == retainedName }
        }

        try {
            composeRule.runOnUiThread {
                ComposeTestContent.show {
                    MaterialTheme {
                        HistoryScreen(
                            contactsViewModel = contactsViewModel,
                            onOpenSecondary = { navigationViewModel.openSecondary(it) },
                            phoneActions = PhoneActions,
                            onDialerPhoneNumberChange = {},
                            isLimitedMode = true,
                            listState = rememberLazyListState(),
                        )
                    }
                }
            }

            composeRule
                .onNode(
                    hasText(composeRule.activity.getString(DialerR.string.search_history)) or
                        hasContentDescription(composeRule.activity.getString(DialerR.string.search_history))
                )
                .performClick()
            composeRule
                .onAllNodes(hasSetTextAction())[0]
                .performTextInput("Alpha $suffix")
            composeRule.onNodeWithText(selectedName).assertIsDisplayed()
            composeRule.onNodeWithText(retainedName).assertDoesNotExist()

            composeRule
                .onNodeWithText(selectedName)
                .performTouchInput { longClick() }
            composeRule
                .onNodeWithContentDescription(composeRule.activity.getString(R.string.delete))
                .performClick()
            composeRule
                .onNodeWithText(composeRule.activity.getString(R.string.delete))
                .performClick()

            composeRule.waitUntil(timeoutMillis = 10_000) {
                contactsViewModel.localOperations.value.none { it.contact.name == selectedName }
            }
            composeRule.onNodeWithText(selectedName).assertDoesNotExist()
        } finally {
            cleanupHistory(contactsViewModel, suffix)
        }
    }

    private fun addHistory(
        contactsViewModel: ContactsViewModel,
        name: String,
        phone: String,
    ) {
        contactsViewModel.addRecentOperation(
            contact = Contact(
                id = "synthetic-$phone",
                name = name,
                phones = listOf(ContactPhone(phone, type = null)),
            ),
            type = RecentType.WHATSAPP_OPEN,
            target = phone,
        )
    }

    private fun cleanupHistory(contactsViewModel: ContactsViewModel, suffix: String) {
        val operations = contactsViewModel.localOperations.value
            .filter { it.contact.name.endsWith(suffix) }
        val ids = operations.map { it.id }
        if (operations.isNotEmpty()) {
            contactsViewModel.deleteRecentOperations(operations)
            composeRule.waitUntil(timeoutMillis = 10_000) {
                contactsViewModel.localOperations.value.none { it.id in ids }
            }
        }
    }

    private fun isEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.contains("emulator") ||
            Build.MODEL.contains("sdk_gphone")
    }
}
