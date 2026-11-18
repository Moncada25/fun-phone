package com.bookverse.contacts.settings

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bookverse.contacts.ui.theme.AccentBarStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class ModernAppearanceMigrationTest {
    private val style = stringPreferencesKey("accent_bar_style")

    @Test
    fun newInstallUsesModern() {
        val preferences = mutablePreferencesOf()
        ModernAppearanceMigration.migrate(preferences)
        assertEquals("MODERN", preferences[style])
        assertEquals(AccentBarStyle.MODERN, AccentBarStyle.fromName(null))
    }

    @Test
    fun upgradeAdoptsModernOnceAndPreservesEveryOtherPreference() {
        val color = stringPreferencesKey("accent_color")
        val font = stringPreferencesKey("font_type")
        val animation = stringPreferencesKey("animation_speed")
        val preferences = mutablePreferencesOf(
            style to "RETRO", color to "custom", font to "manrope", animation to "OFF",
        )
        ModernAppearanceMigration.migrate(preferences)
        assertEquals("MODERN", preferences[style])
        assertEquals("custom", preferences[color])
        assertEquals("manrope", preferences[font])
        assertEquals("OFF", preferences[animation])

        preferences[style] = "MINIMAL"
        ModernAppearanceMigration.migrate(preferences)
        assertEquals("MINIMAL", preferences[style])
    }

    @Test
    fun explicitImportBeforeFirstLaunchPreventsAutomaticOverrideForEveryStyle() {
        AccentBarStyle.entries.forEach { choice ->
            val imported = mutablePreferencesOf(style to choice.name)
            ModernAppearanceMigration.markApplied(imported)
            ModernAppearanceMigration.migrate(imported)
            assertEquals(choice.name, imported[style])
        }
    }
}
