package com.bookverse.contacts.data.model

import android.provider.CalendarContract

data class CalendarAttendee(
    val name: String?,
    val email: String?,
    val status: Int,
    val relationship: Int
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: email?.takeIf { it.isNotBlank() }
            ?: ""

    fun isOrganizer(): Boolean {
        return relationship == CalendarContract.Attendees.RELATIONSHIP_ORGANIZER
    }
}
