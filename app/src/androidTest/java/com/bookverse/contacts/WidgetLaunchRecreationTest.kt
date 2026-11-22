package com.bookverse.contacts

import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.feature.expenses.R as ExpensesR
import com.bookverse.contacts.feature.settings.R as SettingsR
import com.bookverse.contacts.ui.navigation.DynamicShortcuts
import com.bookverse.contacts.ui.navigation.HomeShortcutDestination
import org.junit.After
import org.junit.Before
import org.junit.Test

class WidgetLaunchRecreationTest {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

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
            selectContacts()
            scenario.recreate()
            awaitTab(R.string.tab_contacts)
        }
    }

    private fun verifyTabLaunch(extra: String, title: Int) {
        val intent = Intent(context, MainActivity::class.java).putExtra(extra, true)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            awaitTab(title)
            selectContacts()
            awaitTab(R.string.tab_contacts)
            scenario.recreate()
            awaitTab(R.string.tab_contacts)
        }
    }

    private fun selectContacts() {
        awaitObject(By.text(context.getString(R.string.tab_contacts))).click()
    }

    private fun awaitTab(resourceId: Int) {
        val label = context.getString(resourceId)
        val selectedTab = By.selected(true).hasDescendant(By.text(label))
        check(device.wait(Until.hasObject(selectedTab), UI_TIMEOUT_MS)) { "Expected selected tab: $label" }
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
            awaitObject(By.desc(context.getString(R.string.more_options))).click()
            awaitObject(By.text(context.getString(R.string.note_folders_title))).click()
            awaitTitle(R.string.note_folders_title)

            scenario.recreate()
            awaitTitle(R.string.note_folders_title)
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            awaitTitle(R.string.notes_title)
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            check(device.wait(Until.gone(By.text(context.getString(R.string.notes_title))), UI_TIMEOUT_MS))

            scenario.onActivity { it.startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)) }
            awaitTitle(R.string.notes_title)
        }
    }

    private fun awaitTitle(resourceId: Int) {
        awaitObject(By.text(context.getString(resourceId)))
    }

    private fun awaitObject(selector: BySelector) =
        checkNotNull(device.wait(Until.findObject(selector), UI_TIMEOUT_MS)) { "Expected visible element: $selector" }

    private companion object {
        const val UI_TIMEOUT_MS = 30_000L
    }
}
