package com.bookverse.contacts.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.testing.ComposeTestContent
import com.bookverse.contacts.testing.ComposeTestHostActivity
import com.bookverse.contacts.ui.navigation.allowsBackdrop
import com.bookverse.contacts.ui.screens.expenses.ExpenseManagerScreen
import com.bookverse.contacts.ui.screens.notes.NoteListScreen
import com.bookverse.contacts.ui.screens.passwords.PasswordBackupActions
import com.bookverse.contacts.ui.screens.passwords.PasswordListScreen
import com.bookverse.contacts.ui.theme.ContactsAppTheme
import com.bookverse.contacts.ui.theme.AccentBarStyle
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.ExpensesViewModel
import com.bookverse.contacts.ui.viewmodel.NotesViewModel
import com.bookverse.contacts.ui.viewmodel.PasswordsViewModel
import com.bookverse.contacts.ui.theme.ThemeMode
import com.bookverse.contacts.utils.SecondaryScreen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.abs

class SecondaryGlassScreensTest {
    @get:Rule val compose = createAndroidComposeRule<ComposeTestHostActivity>()
    private val models = ViewModelStore()

    @After fun clearModels() { compose.runOnIdle { models.clear() } }
    @Test fun notesScrollBehindTheirGlassHeader() = verify(SecondaryScreen.NOTES, R.string.notes_title)
    @Test fun passwordsScrollBehindTheirGlassHeader() = verify(SecondaryScreen.PASSWORD_LIST, R.string.passwords_title)
    @Test fun expensesScrollBehindTheirGlassHeader() = verify(SecondaryScreen.EXPENSES, R.string.expense_manager_title)

    private fun verify(screen: SecondaryScreen, title: Int) {
        val settings = AppSettingsViewModel(GlassSettingsStore())
        val notes = NotesViewModel(compose.activity.application, GlassNotesRepository())
        val passwords = PasswordsViewModel(GlassPasswordStore())
        val expenses = ExpensesViewModel(GlassExpensesStore())
        compose.runOnUiThread {
            models.put("settings", settings); models.put("notes", notes); models.put("passwords", passwords); models.put("expenses", expenses)
            if (screen == SecondaryScreen.PASSWORD_LIST) passwords.unlock()
            ComposeTestContent.show {
                ContactsAppTheme(themeMode = ThemeMode.SYSTEM, accentBarStyle = AccentBarStyle.MODERN) {
                    CompositionLocalProvider(LocalBackdropAllowed provides screen.allowsBackdrop()) {
                        Box(Modifier.fillMaxSize().testTag("screen")) {
                            when (screen) {
                                SecondaryScreen.NOTES -> NoteListScreen({}, { _, _, _ -> }, { _, _ -> }, { _, _ -> }, {}, {}, {}, notesViewModel = notes, appSettingsViewModel = settings)
                                SecondaryScreen.PASSWORD_LIST -> PasswordListScreen({}, {}, {}, {}, {}, passwords, settings, PasswordBackupActions({}, {}))
                                SecondaryScreen.EXPENSES -> ExpenseManagerScreen(onBackClick = {}, expensesViewModel = expenses, appSettingsViewModel = settings)
                                else -> error("Unsupported fixture")
                            }
                        }
                    }
                }
            }
        }
        compose.waitUntil(5_000) {
            when (screen) {
                SecondaryScreen.NOTES -> notes.notes.value.size == 60
                SecondaryScreen.PASSWORD_LIST -> passwords.credentials.value.size == 60
                else -> expenses.transactions.value.size == 60
            }
        }
        val root = compose.onNodeWithTag("screen")
        val before = root.captureToImage().toPixelMap()
        val titleBounds = compose.onNodeWithText(compose.activity.getString(title)).fetchSemanticsNode().boundsInRoot
        val list = compose.onNode(hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
        list.performScrollToIndex(if (screen == SecondaryScreen.EXPENSES) 4 else 12)
        list.performTouchInput { swipeUp() }
        assertEquals("The header must stay fixed while its backdrop changes", titleBounds,
            compose.onNodeWithText(compose.activity.getString(title)).fetchSemanticsNode().boundsInRoot)
        val afterImage = root.captureToImage()
        val after = afterImage.toPixelMap()
        var changed = 0
        for (y in 0 until titleBounds.bottom.toInt().coerceAtMost(after.height) step 2) {
            for (x in 0 until after.width step 2) {
                val a = before[x, y]; val b = after[x, y]
                if (maxOf(abs(a.red - b.red), abs(a.green - b.green), abs(a.blue - b.blue)) > 0.015f) changed++
            }
        }
        assertTrue("$screen must blur the real content moving under its header (changed=$changed)", changed > 100)
        if (InstrumentationRegistry.getArguments().getString("captureGlass") == "true") {
            val dir = compose.activity.getExternalFilesDir("glass-qa")!!
            File(dir, "${screen.name}.png").outputStream().use { afterImage.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
