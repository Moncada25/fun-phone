package com.bookverse.contacts.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bookverse.contacts.domain.call.CallScreeningOption
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
    private val blockFailedVerificationKey = booleanPreferencesKey("call_screening_failed_verification")
    private val blockNeighborNumbersKey = booleanPreferencesKey("call_screening_neighbor_numbers")
    private val silenceNotInContactsKey = booleanPreferencesKey("call_screening_silence_not_in_contacts")
    private val allowRecentlyDialedKey = booleanPreferencesKey("call_screening_allow_recently_dialed")
    private val allowRepeatedCallsKey = booleanPreferencesKey("call_screening_allow_repeated_calls")

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

    suspend fun setScreeningOption(context: Context, option: CallScreeningOption, enabled: Boolean) =
        setFlag(context, keyFor(option), enabled)

    private fun keyFor(option: CallScreeningOption): Preferences.Key<Boolean> = when (option) {
        CallScreeningOption.BLOCK_LISTED -> blockListedKey
        CallScreeningOption.BLOCK_HIDDEN -> blockHiddenKey
        CallScreeningOption.BLOCK_FAILED_VERIFICATION -> blockFailedVerificationKey
        CallScreeningOption.BLOCK_NEIGHBOR_NUMBERS -> blockNeighborNumbersKey
        CallScreeningOption.BLOCK_NOT_IN_CONTACTS -> blockNotInContactsKey
        CallScreeningOption.SILENCE_NOT_IN_CONTACTS -> silenceNotInContactsKey
        CallScreeningOption.ALLOW_RECENTLY_DIALED -> allowRecentlyDialedKey
        CallScreeningOption.ALLOW_REPEATED_CALLS -> allowRepeatedCallsKey
    }

    private fun screeningSettingsFrom(prefs: Preferences) = CallScreeningSettings(
        blockListedNumbers = prefs[blockListedKey] ?: false,
        blockHiddenNumbers = prefs[blockHiddenKey] ?: false,
        blockNumbersNotInContacts = prefs[blockNotInContactsKey] ?: false,
        blockFailedVerification = prefs[blockFailedVerificationKey] ?: false,
        blockNeighborNumbers = prefs[blockNeighborNumbersKey] ?: false,
        silenceNumbersNotInContacts = prefs[silenceNotInContactsKey] ?: false,
        allowRecentlyDialed = prefs[allowRecentlyDialedKey] ?: false,
        allowRepeatedCalls = prefs[allowRepeatedCallsKey] ?: false,
    )

    private suspend fun setFlag(context: Context, key: Preferences.Key<Boolean>, enabled: Boolean) {
        AppDataStore.getDataStore(context.applicationContext).edit { prefs -> prefs[key] = enabled }
    }
}
