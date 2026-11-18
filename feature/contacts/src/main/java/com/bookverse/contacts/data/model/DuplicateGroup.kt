package com.bookverse.contacts.data.model

import androidx.compose.runtime.Immutable

enum class MatchType {
    EXACT_NAME,
    SIMILAR_NAME,
    PHONE_MATCH,
    NAME_AND_PHONE,
}

@Immutable
data class DuplicateGroup(
    val contacts: List<Contact>,
    val matchType: MatchType,
    val confidence: Float,
)
