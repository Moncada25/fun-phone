package com.bookverse.contacts.data.model

data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String?,
    val location: String?,
    val startDate: Long,
    val endDate: Long,
    val calendarId: String,
    val calendarName: String,
    val accountName: String,
    val allDay: Boolean,
    val attendees: List<CalendarAttendee> = emptyList(),
    val recurrenceRule: String? = null,
    val duration: String? = null,
)
