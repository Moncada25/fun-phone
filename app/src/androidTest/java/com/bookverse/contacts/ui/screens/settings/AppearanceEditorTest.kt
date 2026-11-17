package com.bookverse.contacts.ui.screens.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.R
import com.bookverse.contacts.settings.AppearanceSettings
import com.bookverse.contacts.settings.ListDensity
import com.bookverse.contacts.ui.viewmodel.AccentBarStyle
import com.bookverse.contacts.ui.viewmodel.DarkModeStyle
import com.bookverse.contacts.ui.viewmodel.ThemeMode
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppearanceEditorTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun changesRemainLocalUntilApply() {
        val applied = AtomicReference<AppearanceSettings>()
        showEditor(applied, AtomicBoolean())
        selectRetro()
        assertNull(applied.get())
        compose.onNodeWithText(text(R.string.apply)).performClick()
        assertEquals(AccentBarStyle.RETRO, applied.get().style)
        assertEquals(initial.font, applied.get().font)
        assertEquals(initial.accent, applied.get().accent)
    }

    @Test
    fun cancelDoesNotApplyThePreview() {
        val applied = AtomicReference<AppearanceSettings>()
        val dismissed = AtomicBoolean()
        showEditor(applied, dismissed)
        selectRetro()
        compose.onNodeWithText(text(R.string.cancel)).performClick()
        assertTrue(dismissed.get())
        assertNull(applied.get())
    }

    private fun showEditor(applied: AtomicReference<AppearanceSettings>, dismissed: AtomicBoolean) {
        compose.setContent {
            MaterialTheme {
                AppearanceEditorSheet(
                    initial, AppearanceSection.STYLE, mapOf("sage" to initial.accent),
                    saving = false, error = false, onApply = applied::set, onDismiss = { dismissed.set(true) },
                )
            }
        }
    }

    private fun selectRetro() {
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .performScrollToNode(hasText(text(R.string.accent_usage_retro)))
        compose.onNodeWithText(text(R.string.accent_usage_retro)).performClick()
    }

    private fun text(resource: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(resource)

    private val initial = AppearanceSettings(
        AccentBarStyle.MODERN, Color(0xFF748873), "sage", "manrope", ThemeMode.LIGHT,
        DarkModeStyle.AMOLED, 1f, ListDensity.NORMAL,
    )
}
