package com.bookverse.contacts.ui.screens.tools

import com.bookverse.contacts.domain.holidays.BirthdayAgendaEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HolidayAgendaPolicyTest {
    @Test
    fun sectionsShowTodayAndFutureBeforeEarlierItems() {
        val today = LocalDate.of(2026, 8, 20)
        val items = listOf(
            birthday(day = 28),
            birthday(day = 1),
            birthday(day = 20),
        )

        val sections = HolidayAgendaPolicy.sections(items, today)

        assertEquals(listOf(20, 28), sections.upcoming.map { it.date.dayOfMonth })
        assertEquals(listOf(1), sections.past.map { it.date.dayOfMonth })
    }

    private fun birthday(day: Int): HolidayAgendaItem.Birthday {
        val date = LocalDate.of(2026, 8, day)
        return HolidayAgendaItem.Birthday(
            value = BirthdayAgendaEntry(
                contactId = day.toString(),
                contactName = "Contact $day",
                date = date,
            ),
            date = date,
        )
    }
}
