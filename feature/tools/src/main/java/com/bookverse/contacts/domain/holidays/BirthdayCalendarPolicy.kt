package com.bookverse.contacts.domain.holidays

import com.bookverse.contacts.data.model.Contact
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class BirthdayAgendaEntry(
    val contactId: String,
    val contactName: String,
    val date: LocalDate,
)

object BirthdayCalendarPolicy {
    fun entriesForMonth(
        contacts: List<Contact>,
        month: YearMonth,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<BirthdayAgendaEntry> = contacts.mapNotNull { contact ->
        val birthday = contact.birthday ?: return@mapNotNull null
        val sourceDate = Instant.ofEpochMilli(birthday).atZone(zoneId).toLocalDate()
        val date = runCatching {
            month.atDay(sourceDate.dayOfMonth)
        }.getOrNull()?.takeIf { sourceDate.monthValue == month.monthValue }
            ?: return@mapNotNull null

        BirthdayAgendaEntry(
            contactId = contact.id,
            contactName = contact.name,
            date = date,
        )
    }.distinctBy { it.contactId to it.date }
        .sortedWith(compareBy(BirthdayAgendaEntry::date, BirthdayAgendaEntry::contactName))
}
