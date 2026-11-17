package com.bookverse.contacts.domain.birthday

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Year
import java.time.ZoneId

data class BirthdayContact(
    val id: String,
    val name: String,
    val birthdayMillis: Long,
)

data class BirthdayReminder(
    val contact: BirthdayContact,
    val daysUntilBirthday: Int,
    val birthdayDate: LocalDate,
)

object BirthdayReminderPolicy {
    val reminderOffsets: Set<Int> = setOf(2, 1, 0)

    fun remindersFor(
        contacts: List<BirthdayContact>,
        now: Instant,
        zoneId: ZoneId,
    ): List<BirthdayReminder> {
        val today = now.atZone(zoneId).toLocalDate()
        return contacts
            .asSequence()
            .filter { it.id.isNotBlank() }
            .distinctBy { it.id }
            .mapNotNull { contact -> reminderFor(contact, today, zoneId) }
            .sortedWith(compareBy<BirthdayReminder> { it.daysUntilBirthday }.thenBy { it.contact.name })
            .toList()
    }

    fun nextNineAm(
        now: Instant,
        zoneId: ZoneId,
        allowToday: Boolean = true,
    ): Instant {
        val localNow = now.atZone(zoneId)
        val todayAtNine = localNow.toLocalDate().atTime(LocalTime.of(9, 0)).atZone(zoneId)
        return when {
            allowToday && localNow.isBefore(todayAtNine) -> todayAtNine.toInstant()
            else -> localNow.toLocalDate().plusDays(1).atTime(LocalTime.of(9, 0)).atZone(zoneId).toInstant()
        }
    }

    fun shouldCatchUpToday(now: Instant, zoneId: ZoneId): Boolean {
        val localNow = now.atZone(zoneId)
        val nineAm = localNow.toLocalDate().atTime(9, 0).atZone(zoneId)
        return !localNow.isBefore(nineAm)
    }

    private fun reminderFor(
        contact: BirthdayContact,
        today: LocalDate,
        zoneId: ZoneId,
    ): BirthdayReminder? {
        val birthDate = Instant.ofEpochMilli(contact.birthdayMillis).atZone(zoneId).toLocalDate()
        return reminderOffsets
            .asSequence()
            .map { daysUntil -> daysUntil to today.plusDays(daysUntil.toLong()) }
            .firstOrNull { (_, targetDate) -> anniversaryInYear(birthDate, targetDate.year) == targetDate }
            ?.let { (daysUntil, birthdayDate) ->
                BirthdayReminder(
                    contact = contact,
                    daysUntilBirthday = daysUntil,
                    birthdayDate = birthdayDate,
                )
            }
    }

    private fun anniversaryInYear(birthDate: LocalDate, year: Int): LocalDate =
        if (birthDate.monthValue == 2 && birthDate.dayOfMonth == 29 && !Year.isLeap(year.toLong())) {
            LocalDate.of(year, 2, 28)
        } else {
            LocalDate.of(year, birthDate.monthValue, birthDate.dayOfMonth)
        }
}
