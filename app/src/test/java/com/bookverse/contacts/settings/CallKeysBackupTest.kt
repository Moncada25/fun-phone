package com.bookverse.contacts.settings

import com.bookverse.contacts.data.model.AppSettingsBackup
import com.bookverse.contacts.data.model.ToolsOrderConfig
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallKeysBackupTest {
    @Test
    fun volumeKeyCallActionsSurviveExportAndImport() {
        val original = backup().copy(answerWithVolumeUp = true, declineWithVolumeDown = true)

        assertEquals(original, AppSettingsBackup.fromJson(original.toJson()))
    }

    @Test
    fun backupsWithoutDeclineKeepItDisabled() {
        val json = JsonParser.parseString(backup().copy(answerWithVolumeUp = true).toJson()).asJsonObject
        json.remove("declineWithVolumeDown")

        val imported = AppSettingsBackup.fromJson(json.toString())

        assertTrue(imported?.answerWithVolumeUp == true)
        assertFalse(imported?.declineWithVolumeDown ?: true)
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
