package com.bookverse.contacts.security.autofill

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bookverse.contacts.settings.AppDataStore
import kotlinx.coroutines.flow.first

internal object VaultGate {
    private val enabledKey = booleanPreferencesKey("password_manager_enabled")
    private val methodKey = stringPreferencesKey("password_auth_method")

    suspend fun isConfigured(context: Context): Boolean {
        val preferences = AppDataStore.getDataStore(context.applicationContext).data.first()
        val method = preferences[methodKey]
        return (preferences[enabledKey] ?: true) && (method == "PIN" || method == "BIOMETRIC")
    }
}
