package com.bookverse.contacts.ui.screens.contacts

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.lifecycle.ViewModelProvider
import com.bookverse.contacts.App
import com.bookverse.contacts.R
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactAccount
import com.bookverse.contacts.testing.ComposeTestContent
import com.bookverse.contacts.testing.ComposeTestHostActivity
import com.bookverse.contacts.ui.components.LocalBackdropAllowed
import com.bookverse.contacts.ui.theme.ContactsAppTheme
import com.bookverse.contacts.ui.theme.AccentBarStyle
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.ui.theme.ThemeMode
import org.junit.Rule
import org.junit.Test

class ContactDetailsAccountsTest {
    @get:Rule val compose = createAndroidComposeRule<ComposeTestHostActivity>()
    private var accounts by mutableStateOf<List<ContactAccount>?>(null)
    private val local = ContactAccount.LOCAL_ACCOUNT
    private val external = ContactAccount("QA account", "com.example.synthetic")
    private val excluded = ContactAccount("WhatsApp", ContactAccount.WHATSAPP)
    private val list get() = compose.onNode(hasScrollToIndexAction())

    @Test
    fun nullEmptyAndExcludedAccountsOpenWithoutAnEmptySelector() {
        showDetail()
        for (variant in listOf(null, emptyList(), listOf(excluded))) {
            compose.runOnIdle { accounts = variant }
            assertDetailOpens()
            list.performScrollToNode(hasText(text(R.string.delete)))
            list.performTouchInput { swipeUp() }
            compose.onNodeWithText(text(R.string.contact_info)).assertDoesNotExist()
            compose.onNodeWithText(text(R.string.multiple_account_title)).assertDoesNotExist()
        }
    }

    @Test
    fun singleLocalAndExternalAccountsShowTheirOrigin() {
        showDetail()
        for (account in listOf(local, external)) {
            compose.runOnIdle { accounts = listOf(account) }
            assertDetailOpens()
            list.performScrollToNode(hasText(text(R.string.contact_info)))
            val label = if (account.isLocal) text(R.string.device) else account.displayName
            compose.onNodeWithText(label).assertIsDisplayed()
            compose.onNodeWithText(text(R.string.multiple_account_title)).assertDoesNotExist()
        }
    }

    @Test
    fun multipleAccountsShareTheFilteredCountAndCloseWhenAccountsDisappear() {
        accounts = listOf(local, external, local, excluded)
        showDetail()
        assertDetailOpens()
        val label = compose.activity.resources.getQuantityString(R.plurals.multiple_account, 2, 2)
        list.performScrollToNode(hasText(label))
        compose.onNodeWithText(label).performClick()
        compose.onNodeWithText(text(R.string.multiple_account_title)).assertIsDisplayed()
        compose.onAllNodesWithText(text(R.string.device)).assertCountEquals(1)
        compose.onNodeWithText(external.displayName).assertIsDisplayed()
        compose.onNodeWithText(excluded.displayName).assertDoesNotExist()
        compose.runOnIdle { accounts = null }
        compose.onNodeWithText(text(R.string.multiple_account_title)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.contact_info)).assertDoesNotExist()
        assertDetailOpens()
    }

    private fun assertDetailOpens() {
        list.performScrollToIndex(0)
        compose.onNodeWithText(text(R.string.contact_details_title)).assertIsDisplayed()
        list.performScrollToNode(hasText("QA synthetic contact"))
        compose.onNodeWithText("QA synthetic contact").assertIsDisplayed()
    }

    private fun showDetail() {
        val app = compose.activity.application as App
        // Initialization observes only the local cache; no provider refresh or writes are requested.
        val contacts = ViewModelProvider(compose.activity, app.container.contactsViewModelFactory)[ContactsViewModel::class.java]
        val settings = AppSettingsViewModel()
        compose.runOnUiThread {
            ComposeTestContent.show {
                ContactsAppTheme(themeMode = ThemeMode.LIGHT, accentBarStyle = AccentBarStyle.MODERN) {
                    CompositionLocalProvider(LocalBackdropAllowed provides false) {
                        ContactDetailsScreen(
                            contact = Contact("qa-account-regression", "QA synthetic contact", emptyList(), accounts = accounts),
                            onBackClick = {}, onEditContact = {},
                            appSettingsViewModel = settings, contactsViewModel = contacts,
                        )
                    }
                }
            }
        }
    }

    private fun text(resource: Int) = compose.activity.getString(resource)
}
