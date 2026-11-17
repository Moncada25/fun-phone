package com.bookverse.contacts.settings

import com.bookverse.contacts.data.model.AppSettingsBackup
import com.bookverse.contacts.data.model.ToolsOrderConfig
import com.bookverse.contacts.ui.viewmodel.AccentBarStyle
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceBackupTest {
    @Test
    fun everyStyleAndNewSeedSurvivesExportAndImport() {
        AccentBarStyle.entries.forEach { style ->
            listOf("sage", "petrol", "terracotta", "mauve", "sand", "graphite").forEach { seed ->
                val original = backup().copy(accentBarStyle = style.name, accentColorName = seed)
                assertEquals(original, AppSettingsBackup.fromJson(original.toJson()))
            }
        }
    }

    @Test
    fun customSeedAndDarkVariantArePreserved() {
        val original = backup().copy(accentColorName = "custom", customAccentColor = "12345678", darkModeStyle = "DIM")
        assertEquals(original, AppSettingsBackup.fromJson(original.toJson()))
    }

    @Test
    fun oldBackupsKeepTheirExplicitStyleAndDefaultMissingDarkVariant() {
        val json = JsonParser.parseString(backup().copy(accentBarStyle = "MINIMAL").toJson()).asJsonObject
        json.remove("darkModeStyle")
        json.remove("customAccentColor")
        val imported = AppSettingsBackup.fromJson(json.toString())
        assertEquals("AMOLED", imported?.darkModeStyle)
        assertEquals("MINIMAL", imported?.accentBarStyle)
    }

    private fun backup() = AppSettingsBackup(
        accentColorName = "sage", fontType = "manrope", animationSpeed = "NORMAL",
        vibrationDuration = 50, vibrationPattern = "simple", themeMode = "LIGHT",
        transitionType = "SLIDE", defaultDialerConsent = false, passwordManagerEnabled = true,
        sensitiveSectionsLockEnabled = false, defaultContactAccount = "local", favoritesColumns = 2,
        favoritesSortOrder = "ALPHABETICAL", favoritesCustomOrder = emptyList(),
        accentBarStyle = "MODERN", appLanguage = "system", expenseCurrency = "COP",
        toolsOrder = ToolsOrderConfig.default(), startupScreen = "contacts",
    )
}
