package com.bookverse.contacts.domain.holidays

import com.bookverse.contacts.data.model.CalendarEvent

data class CalendarSource(
    val id: Long,
    val name: String,
    val accountName: String,
    val accountType: String,
    val isPrimary: Boolean = false,
)

interface CalendarEventSource {
    suspend fun getCalendarEvents(
        calendarIds: Set<Long>,
        startDate: Long,
        endDate: Long,
    ): List<CalendarEvent>

    suspend fun getCalendars(): List<CalendarSource>
}
