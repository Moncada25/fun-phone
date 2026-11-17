package com.bookverse.contacts.ui.screens.contacts.dialer

import androidx.datastore.preferences.core.stringPreferencesKey
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.RecentOperation

internal val LAST_DIALED_NUMBER_KEY = stringPreferencesKey("last_dialed_number")
internal const val MAX_DIALED_NUMBERS = 20
internal const val MAX_DIALER_FILTER_RESULTS = 25

internal data class NumberExistenceResult(
    val contactExists: Boolean,
    val profileExists: Boolean
)

internal data class DialerContactSearchIndex(
    val contact: Contact,
    val normalizedPhones: List<String>,
    val primaryPhone: String
)

internal data class DialerSuggestionEntry(
    val key: String,
    val contact: Contact,
    val number: String
)

internal data class SuggestionScore(
    val operation: RecentOperation,
    val score: Double
)
