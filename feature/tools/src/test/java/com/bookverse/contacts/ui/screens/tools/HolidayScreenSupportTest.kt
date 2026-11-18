package com.bookverse.contacts.ui.screens.tools

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.domain.holidays.BirthdayCalendarPolicy
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class HolidayScreenSupportTest {
    private val zoneId = ZoneId.of("America/Bogota")

    @Test
    fun birthdayAgendaIncludesTheProfileBirthday() {
        val profile = Contact(
            id = "profile",
            name = "Santiago Moncada",
            phones = emptyList(),
            birthday = LocalDate.of(1992, 9, 3)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli(),
        )

        val entries = BirthdayCalendarPolicy.entriesForMonth(
            contacts = birthdayAgendaContacts(emptyList(), profile),
            month = YearMonth.of(2026, 9),
            zoneId = zoneId,
        )

        assertEquals("Santiago Moncada", entries.single().contactName)
        assertEquals(LocalDate.of(2026, 9, 3), entries.single().date)
    }
}
