package com.bookverse.contacts.domain.holidays

import com.bookverse.contacts.data.model.Contact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class BirthdayCalendarPolicyTest {
    private val zoneId = ZoneId.of("America/Bogota")

    @Test
    fun projectsBirthdayIntoRequestedYearAndMonth() {
        val contact = contact("ada", "Ada", LocalDate.of(1990, 8, 28))

        val entries = BirthdayCalendarPolicy.entriesForMonth(
            contacts = listOf(contact),
            month = YearMonth.of(2028, 8),
            zoneId = zoneId,
        )

        assertEquals(LocalDate.of(2028, 8, 28), entries.single().date)
        assertEquals("Ada", entries.single().contactName)
    }

    @Test
    fun keepsLeapBirthdayOnExactDayOnly() {
        val contact = contact("leap", "Leap", LocalDate.of(2000, 2, 29))

        val regularYear = BirthdayCalendarPolicy.entriesForMonth(
            listOf(contact),
            YearMonth.of(2027, 2),
            zoneId,
        )
        val leapYear = BirthdayCalendarPolicy.entriesForMonth(
            listOf(contact),
            YearMonth.of(2028, 2),
            zoneId,
        )

        assertTrue(regularYear.isEmpty())
        assertEquals(LocalDate.of(2028, 2, 29), leapYear.single().date)
    }

    private fun contact(id: String, name: String, birthday: LocalDate): Contact = Contact(
        id = id,
        name = name,
        phones = emptyList(),
        birthday = birthday.atStartOfDay(zoneId).toInstant().toEpochMilli(),
    )
}
