package com.bookverse.contacts.ui.viewmodel

import com.bookverse.contacts.data.model.AppSettingsBackup
import com.bookverse.contacts.data.model.ToolsOrderConfig
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoritesCardShapeTest {
    @Test
    fun shouldResolveRoundShapeFromStoredValue() {
        assertEquals(FavoritesCardShape.ROUND, FavoritesCardShape.fromName("ROUND"))
    }

    @Test
    fun shouldFallbackToSquareForMissingOrUnknownStoredValues() {
        assertEquals(FavoritesCardShape.SQUARE, FavoritesCardShape.fromName(null))
        assertEquals(FavoritesCardShape.SQUARE, FavoritesCardShape.fromName("UNKNOWN"))
    }

    @Test
    fun shouldDefaultOldBackupsToSquareShape() {
        val oldBackupJson = JsonParser.parseString(
            AppSettingsBackup(
                accentColorName = "blue",
                fontType = "default",
                animationSpeed = "NORMAL",
                vibrationDuration = 50,
                vibrationPattern = "simple",
                themeMode = "SYSTEM",
                transitionType = "SLIDE",
                defaultDialerConsent = false,
                passwordManagerEnabled = true,
                sensitiveSectionsLockEnabled = false,
                defaultContactAccount = "local",
                favoritesColumns = 2,
                favoritesSortOrder = "ALPHABETICAL",
                favoritesCustomOrder = emptyList(),
                accentBarStyle = "MINIMAL",
                appLanguage = "system",
                expenseCurrency = "COP",
                toolsOrder = ToolsOrderConfig.default(),
                startupScreen = "contacts",
            ).toJson()
        ).asJsonObject.apply {
            remove("favoritesCardShape")
        }.toString()

        val backup = AppSettingsBackup.fromJson(oldBackupJson)

        assertEquals("SQUARE", backup?.favoritesCardShape)
    }
}
