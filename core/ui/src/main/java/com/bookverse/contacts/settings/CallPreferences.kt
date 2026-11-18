package com.bookverse.contacts.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bookverse.contacts.domain.call.CallScreeningSettings
import com.bookverse.contacts.domain.call.SimPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object CallPreferences {
    private val simAccountKey = stringPreferencesKey("call_sim_account")
    private val blockListedKey = booleanPreferencesKey("call_screening_blocked_list")
    private val blockHiddenKey = booleanPreferencesKey("call_screening_hidden_numbers")
    private val blockNotInContactsKey = booleanPreferencesKey("call_screening_not_in_contacts")

    fun simPreference(context: Context): Flow<SimPreference> =
        AppDataStore.getDataStore(context.applicationContext).data
            .map { prefs -> SimPreference.fromStoredValue(prefs[simAccountKey]) }
            .distinctUntilChanged()

    suspend fun currentSimPreference(context: Context): SimPreference = simPreference(context).first()

    suspend fun setSimPreference(context: Context, preference: SimPreference) {
        AppDataStore.getDataStore(context.applicationContext).edit { prefs ->
            val stored = preference.toStoredValue()
            if (stored == null) prefs.remove(simAccountKey) else prefs[simAccountKey] = stored
        }
    }

    fun screeningSettings(context: Context): Flow<CallScreeningSettings> =
        AppDataStore.getDataStore(context.applicationContext).data
            .map(::screeningSettingsFrom)
            .distinctUntilChanged()

    suspend fun currentScreeningSettings(context: Context): CallScreeningSettings =
        screeningSettings(context).first()

    suspend fun setBlockListedNumbers(context: Context, enabled: Boolean) =
        setFlag(context, blockListedKey, enabled)

    suspend fun setBlockHiddenNumbers(context: Context, enabled: Boolean) =
        setFlag(context, blockHiddenKey, enabled)

    suspend fun setBlockNumbersNotInContacts(context: Context, enabled: Boolean) =
        setFlag(context, blockNotInContactsKey, enabled)

    private fun screeningSettingsFrom(prefs: Preferences) = CallScreeningSettings(
        blockListedNumbers = prefs[blockListedKey] ?: false,
        blockHiddenNumbers = prefs[blockHiddenKey] ?: false,
        blockNumbersNotInContacts = prefs[blockNotInContactsKey] ?: false,
    )

    private suspend fun setFlag(context: Context, key: Preferences.Key<Boolean>, enabled: Boolean) {
        AppDataStore.getDataStore(context.applicationContext).edit { prefs -> prefs[key] = enabled }
    }
}
