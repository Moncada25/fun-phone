package com.bookverse.contacts

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.feature.expenses.R as ExpensesR
import com.bookverse.contacts.feature.settings.R as SettingsR
import com.bookverse.contacts.ui.navigation.DynamicShortcuts
import com.bookverse.contacts.ui.navigation.HomeShortcutDestination
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WidgetLaunchRecreationTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val onboarding = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
    private val originalOnboarding = onboarding.all

    @Before
    fun completeOnboarding() {
        onboarding.edit(commit = true) { putBoolean("completed", true) }
    }

    @After
    fun restoreOnboarding() {
        onboarding.edit(commit = true) {
            for (key in listOf("completed", "limited_mode")) {
                val original = originalOnboarding[key] as? Boolean
                if (original == null) remove(key) else putBoolean(key, original)
            }
        }
    }

    @Test
    fun widgetOrReminderOpensNotesOnce() {
        verifyNotesLaunch(Intent(context, MainActivity::class.java).putExtra("open_notes_list", true))
    }

    @Test
    fun notesShortcutOpensNotesOnce() {
        verifyNotesLaunch(DynamicShortcuts.launchIntent(context, HomeShortcutDestination.NOTES))
    }

    @Test
    fun favoritesWidgetOpensFavoritesOnce() {
        verifyTabLaunch("open_favorites", R.string.tab_favorites)
    }

    @Test
    fun recentCallsWidgetOpensHistoryOnce() {
        verifyTabLaunch("open_recents", R.string.tab_history)
    }

    @Test
    fun expensesWidgetOpensExpensesOnce() {
        val intent = DynamicShortcuts.launchIntent(context, HomeShortcutDestination.EXPENSES)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            awaitTitle(ExpensesR.string.expense_manager_title)
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            val contactsTab = hasText(context.getString(R.string.tab_contacts)) and isSelectable()
            compose.onNode(contactsTab).performClick()
            scenario.recreate()
            awaitTab(R.string.tab_contacts)
        }
    }

    private fun verifyTabLaunch(extra: String, title: Int) {
        val intent = Intent(context, MainActivity::class.java).putExtra(extra, true)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            awaitTab(title)
            compose.onNode(hasText(context.getString(R.string.tab_contacts)) and isSelectable()).performClick()
            awaitTab(R.string.tab_contacts)
            scenario.recreate()
            awaitTab(R.string.tab_contacts)
        }
    }

    private fun awaitTab(resourceId: Int) {
        val selectedTab = hasText(context.getString(resourceId)) and isSelected()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(selectedTab).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
        compose.onNode(selectedTab).assertIsDisplayed()
    }

    @Test
    fun unhandledNotesRequestSurvivesRecreationDuringOnboarding() {
        onboarding.edit(commit = true) { putBoolean("completed", false) }
        val intent = Intent(context, MainActivity::class.java).putExtra("open_notes_list", true)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            awaitTitle(SettingsR.string.onboarding_welcome_title)
            onboarding.edit(commit = true) { putBoolean("completed", true) }
            scenario.recreate()
            awaitTitle(R.string.notes_title)
        }
    }

    private fun verifyNotesLaunch(intent: Intent) {
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            awaitTitle(R.string.notes_title)
            compose.onNodeWithContentDescription(context.getString(R.string.more_options)).performClick()
            compose.onNodeWithText(context.getString(R.string.note_folders_title)).performClick()
            awaitTitle(R.string.note_folders_title)

            scenario.recreate()
            awaitTitle(R.string.note_folders_title)
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            awaitTitle(R.string.notes_title)
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            compose.waitForIdle()
            compose.onNodeWithText(context.getString(R.string.notes_title)).assertDoesNotExist()

            scenario.onActivity { it.startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)) }
            awaitTitle(R.string.notes_title)
        }
    }

    private fun awaitTitle(resourceId: Int) {
        val title = context.getString(resourceId)
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
        compose.onNodeWithText(title).assertIsDisplayed()
    }
}
