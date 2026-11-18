package com.bookverse.contacts.domain.contacts

enum class ContactSortOrder {
    FIRST_NAME,
    LAST_NAME,
    RECENTLY_ADDED;

    companion object {
        fun fromName(name: String?): ContactSortOrder =
            entries.firstOrNull { it.name == name } ?: FIRST_NAME
    }
}
