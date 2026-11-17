package com.bookverse.contacts.domain.contacts.vcard

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import com.bookverse.contacts.domain.contacts.ContactDraft
import com.bookverse.contacts.domain.contacts.ContactNamePolicy
import contacts.core.entities.PhoneEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object VCardContactMapper {
    private val phoneTypeTokens: Map<PhoneEntity.Type, Set<String>> = mapOf(
        PhoneEntity.Type.MOBILE to setOf("CELL"),
        PhoneEntity.Type.WORK to setOf("WORK"),
        PhoneEntity.Type.HOME to setOf("HOME"),
        PhoneEntity.Type.MAIN to setOf("MAIN"),
        PhoneEntity.Type.FAX_WORK to setOf("WORK", "FAX"),
        PhoneEntity.Type.FAX_HOME to setOf("HOME", "FAX"),
        PhoneEntity.Type.PAGER to setOf("PAGER"),
        PhoneEntity.Type.OTHER to setOf("VOICE"),
        PhoneEntity.Type.CALLBACK to setOf("X-CALLBACK"),
        PhoneEntity.Type.CAR to setOf("CAR"),
        PhoneEntity.Type.COMPANY_MAIN to setOf("X-COMPANY-MAIN"),
        PhoneEntity.Type.ISDN to setOf("ISDN"),
        PhoneEntity.Type.OTHER_FAX to setOf("FAX"),
        PhoneEntity.Type.RADIO to setOf("X-RADIO"),
        PhoneEntity.Type.TELEX to setOf("TLX"),
        PhoneEntity.Type.TTY_TDD to setOf("X-TTY-TDD"),
        PhoneEntity.Type.WORK_MOBILE to setOf("WORK", "CELL"),
        PhoneEntity.Type.WORK_PAGER to setOf("WORK", "PAGER"),
        PhoneEntity.Type.ASSISTANT to setOf("X-ASSISTANT"),
        PhoneEntity.Type.MMS to setOf("MSG"),
    )

    fun toVCard(contact: Contact, zone: ZoneId = ZoneId.systemDefault()): VCardContact {
        val hasRealName = ContactNamePolicy.hasRealName(contact)
        val nameParts = ContactNamePolicy.split(if (hasRealName) contact.name else "")
        return VCardContact(
            formattedName = contact.name.trim(),
            familyName = nameParts.familyName,
            givenName = nameParts.givenName,
            phones = contact.phones.map { phone ->
                VCardPhone(
                    number = phone.number,
                    types = phone.type?.let(phoneTypeTokens::get).orEmpty(),
                )
            },
            emails = listOfNotNull(contact.email?.trim()?.takeIf(String::isNotEmpty)),
            organization = contact.company?.trim()?.takeIf(String::isNotEmpty),
            note = contact.notes?.trim()?.takeIf(String::isNotEmpty),
            birthday = contact.birthday?.let { timestamp ->
                val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
                VCardDate(year = date.year, month = date.monthValue, day = date.dayOfMonth)
            },
            urls = contact.socialMedia.map(String::trim).filter(String::isNotEmpty),
        )
    }

    fun toDraft(
        card: VCardContact,
        zone: ZoneId = ZoneId.systemDefault(),
        currentYear: Int = LocalDate.now(zone).year,
    ): ContactDraft {
        val phones = card.phones.map { phone ->
            ContactPhone(number = phone.number.trim(), type = phoneType(phone.types))
        }
        val hasStructuredName = card.structuredName.isNotEmpty()
        val displayName = card.displayName
        val name = if (
            hasStructuredName ||
            ContactNamePolicy.hasRealName(displayName, phones.map { it.number })
        ) {
            displayName
        } else {
            ""
        }
        val emails = card.emails.map(String::trim).filter(String::isNotEmpty).distinct()
        return ContactDraft(
            contact = Contact(
                id = "",
                name = name,
                phones = phones,
                birthday = card.birthday?.let { birthday -> toTimestamp(birthday, zone, currentYear) },
                email = emails.firstOrNull(),
                company = card.organization?.trim()?.takeIf(String::isNotEmpty),
                notes = card.note?.trim()?.takeIf(String::isNotEmpty),
                socialMedia = card.urls.map(String::trim).filter(String::isNotEmpty).distinct(),
            ),
            additionalEmails = emails.drop(1),
        )
    }

    internal fun phoneType(types: Set<String>): PhoneEntity.Type {
        fun has(vararg tokens: String): Boolean = tokens.any(types::contains)
        val work = has("WORK")
        val home = has("HOME")
        return when {
            has("FAX") && work -> PhoneEntity.Type.FAX_WORK
            has("FAX") && home -> PhoneEntity.Type.FAX_HOME
            has("FAX") -> PhoneEntity.Type.OTHER_FAX
            has("PAGER") && work -> PhoneEntity.Type.WORK_PAGER
            has("PAGER") -> PhoneEntity.Type.PAGER
            has("CELL", "MOBILE", "IPHONE") && work -> PhoneEntity.Type.WORK_MOBILE
            has("CELL", "MOBILE", "IPHONE") -> PhoneEntity.Type.MOBILE
            has("X-CALLBACK", "CALLBACK") -> PhoneEntity.Type.CALLBACK
            has("CAR") -> PhoneEntity.Type.CAR
            has("X-COMPANY-MAIN", "COMPANY-MAIN") -> PhoneEntity.Type.COMPANY_MAIN
            has("ISDN") -> PhoneEntity.Type.ISDN
            has("X-RADIO", "RADIO") -> PhoneEntity.Type.RADIO
            has("TLX", "TELEX") -> PhoneEntity.Type.TELEX
            has("X-TTY-TDD", "TTY-TDD", "TEXTPHONE") -> PhoneEntity.Type.TTY_TDD
            has("X-ASSISTANT", "ASSISTANT") -> PhoneEntity.Type.ASSISTANT
            has("MSG", "MMS") -> PhoneEntity.Type.MMS
            has("MAIN") -> PhoneEntity.Type.MAIN
            work -> PhoneEntity.Type.WORK
            home -> PhoneEntity.Type.HOME
            has("VOICE", "OTHER") -> PhoneEntity.Type.OTHER
            else -> PhoneEntity.Type.MOBILE
        }
    }

    private fun toTimestamp(date: VCardDate, zone: ZoneId, currentYear: Int): Long? {
        val localDate = if (date.year != null) {
            runCatching { LocalDate.of(date.year, date.month, date.day) }.getOrNull()
        } else {
            (currentYear downTo currentYear - 7).firstNotNullOfOrNull { year ->
                runCatching { LocalDate.of(year, date.month, date.day) }.getOrNull()
            }
        }
        return localDate?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()
    }
}
