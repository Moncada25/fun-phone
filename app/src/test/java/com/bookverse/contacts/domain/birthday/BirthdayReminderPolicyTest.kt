package com.bookverse.contacts.domain.birthday

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BirthdayReminderPolicyTest {
    @Test
    fun shouldReturnTwoOneAndZeroDayReminders_whenBirthdaysMatchWindow() {
        val zone = ZoneId.of("America/Bogota")
        val now = ZonedDateTime.of(2026, 8, 20, 9, 0, 0, 0, zone).toInstant()
        val contacts = listOf(
            contact("today", "Today", LocalDate.of(1990, 8, 20), zone),
            contact("tomorrow", "Tomorrow", LocalDate.of(1991, 8, 21), zone),
            contact("two-days", "Two days", LocalDate.of(1992, 8, 22), zone),
            contact("later", "Later", LocalDate.of(1993, 8, 23), zone),
        )

        val reminders = BirthdayReminderPolicy.remindersFor(contacts, now, zone)

        assertEquals(listOf(0, 1, 2), reminders.map { it.daysUntilBirthday })
        assertEquals(listOf("today", "tomorrow", "two-days"), reminders.map { it.contact.id })
    }

    @Test
    fun shouldHandleYearRollover_whenWindowCrossesNewYear() {
        val zone = ZoneId.of("America/Bogota")
        val now = ZonedDateTime.of(2026, 12, 30, 9, 0, 0, 0, zone).toInstant()
        val contacts = listOf(
            contact("new-year", "New Year", LocalDate.of(1990, 1, 1), zone),
        )

        val reminders = BirthdayReminderPolicy.remindersFor(contacts, now, zone)

        assertEquals(2, reminders.single().daysUntilBirthday)
        assertEquals(LocalDate.of(2027, 1, 1), reminders.single().birthdayDate)
    }

    @Test
    fun shouldUseFebruaryTwentyEight_whenLeapBirthdayFallsInNonLeapYear() {
        val zone = ZoneId.of("America/Bogota")
        val now = ZonedDateTime.of(2027, 2, 26, 9, 0, 0, 0, zone).toInstant()
        val contact = contact("leap", "Leap", LocalDate.of(2000, 2, 29), zone)

        val reminder = BirthdayReminderPolicy.remindersFor(listOf(contact), now, zone).single()

        assertEquals(2, reminder.daysUntilBirthday)
        assertEquals(LocalDate.of(2027, 2, 28), reminder.birthdayDate)
    }

    @Test
    fun shouldKeepFebruaryTwentyNine_whenYearIsLeap() {
        val zone = ZoneId.of("America/Bogota")
        val now = ZonedDateTime.of(2028, 2, 27, 9, 0, 0, 0, zone).toInstant()
        val contact = contact("leap", "Leap", LocalDate.of(2000, 2, 29), zone)

        val reminder = BirthdayReminderPolicy.remindersFor(listOf(contact), now, zone).single()

        assertEquals(2, reminder.daysUntilBirthday)
        assertEquals(LocalDate.of(2028, 2, 29), reminder.birthdayDate)
    }

    @Test
    fun shouldDeduplicateContactsById() {
        val zone = ZoneId.of("UTC")
        val now = Instant.parse("2026-08-20T09:00:00Z")
        val birthday = LocalDate.of(1990, 8, 20)
        val duplicate = contact("same", "Same", birthday, zone)

        val reminders = BirthdayReminderPolicy.remindersFor(listOf(duplicate, duplicate), now, zone)

        assertEquals(1, reminders.size)
    }

    @Test
    fun shouldCalculateNextNineAmAcrossDstStart() {
        val zone = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(2026, 3, 7, 10, 0, 0, 0, zone).toInstant()

        val next = BirthdayReminderPolicy.nextNineAm(now, zone)

        assertEquals(
            ZonedDateTime.of(2026, 3, 8, 9, 0, 0, 0, zone).toInstant(),
            next,
        )
    }

    @Test
    fun shouldCalculateNextNineAmAcrossDstEnd() {
        val zone = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(2026, 10, 31, 10, 0, 0, 0, zone).toInstant()

        val next = BirthdayReminderPolicy.nextNineAm(now, zone)

        assertEquals(
            ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, zone).toInstant(),
            next,
        )
    }

    @Test
    fun shouldScheduleTodayBeforeNineAndTomorrowAtOrAfterNine() {
        val zone = ZoneId.of("Europe/Berlin")
        val beforeNine = ZonedDateTime.of(2026, 8, 20, 8, 59, 59, 0, zone).toInstant()
        val atNine = ZonedDateTime.of(2026, 8, 20, 9, 0, 0, 0, zone).toInstant()

        assertEquals(
            ZonedDateTime.of(2026, 8, 20, 9, 0, 0, 0, zone).toInstant(),
            BirthdayReminderPolicy.nextNineAm(beforeNine, zone),
        )
        assertEquals(
            ZonedDateTime.of(2026, 8, 21, 9, 0, 0, 0, zone).toInstant(),
            BirthdayReminderPolicy.nextNineAm(atNine, zone),
        )
        assertFalse(BirthdayReminderPolicy.shouldCatchUpToday(beforeNine, zone))
        assertTrue(BirthdayReminderPolicy.shouldCatchUpToday(atNine, zone))
    }

    @Test
    fun shouldInterpretBirthdayInRequestedLocalZone() {
        val zone = ZoneId.of("Asia/Tokyo")
        val now = ZonedDateTime.of(2026, 8, 20, 9, 0, 0, 0, zone).toInstant()
        val birthday = contact("tokyo", "Tokyo", LocalDate.of(1990, 8, 20), zone)

        val reminders = BirthdayReminderPolicy.remindersFor(listOf(birthday), now, zone)

        assertEquals(0, reminders.single().daysUntilBirthday)
    }

    private fun contact(
        id: String,
        name: String,
        birthday: LocalDate,
        zoneId: ZoneId,
    ): BirthdayContact = BirthdayContact(
        id = id,
        name = name,
        birthdayMillis = birthday.atStartOfDay(zoneId).toInstant().toEpochMilli(),
    )
}
