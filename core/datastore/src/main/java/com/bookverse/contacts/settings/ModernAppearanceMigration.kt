package com.bookverse.contacts.settings

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** Adopt Modern once, while leaving every other preference and subsequent choice untouched. */
object ModernAppearanceMigration {
    private val appliedKey = booleanPreferencesKey("modern_appearance_migrated")
    private val styleKey = stringPreferencesKey("accent_bar_style")

    fun migrate(preferences: MutablePreferences) {
        if (preferences[appliedKey] != true) {
            preferences[styleKey] = "MODERN"
            markApplied(preferences)
        }
    }

    fun markApplied(preferences: MutablePreferences) {
        preferences[appliedKey] = true
    }
}
