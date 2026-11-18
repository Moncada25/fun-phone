package com.bookverse.contacts.domain.contacts.vcard

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import contacts.core.entities.PhoneEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class VCardCodecTest {
    private val zone = ZoneId.of("America/Bogota")

    @Test
    fun parsesVersion3CardWithFoldedLinesEscapesAndGroups() {
        val text = listOf(
            "BEGIN:VCARD",
            "VERSION:3.0",
            "N:Pérez Gómez;María José;;Dra.;",
            "FN:Dra. María José Pérez Gómez",
            "item1.TEL;TYPE=CELL,VOICE:+57 300 123 4567",
            "TEL;TYPE=WORK;TYPE=FAX:6015550101",
            "EMAIL;TYPE=INTERNET;TYPE=HOME:maria@example.com",
            "EMAIL:mj.perez@example.org",
            "ORG:Acme\\, Inc.;Research",
            "NOTE:Line one\\nLine two\\; with semicolon and a very long tail that is fol",
            " ded across two physical lines",
            "BDAY:1990-05-17",
            "URL:https://example.com/maria",
            "END:VCARD",
        ).joinToString("\r\n")

        val result = VCardParser.parse(text)

        assertEquals(0, result.failed)
        val card = result.contacts.single()
        assertEquals("Dra. María José Pérez Gómez", card.displayName)
        assertEquals("Pérez Gómez", card.familyName)
        assertEquals("María José", card.givenName)
        assertEquals("Dra.", card.prefix)
        assertEquals(
            listOf(
                VCardPhone("+57 300 123 4567", setOf("CELL", "VOICE")),
                VCardPhone("6015550101", setOf("WORK", "FAX")),
            ),
            card.phones,
        )
        assertEquals(listOf("maria@example.com", "mj.perez@example.org"), card.emails)
        assertEquals("Acme, Inc.", card.organization)
        assertEquals(
            "Line one\nLine two; with semicolon and a very long tail that is folded across two physical lines",
            card.note,
        )
        assertEquals(VCardDate(1990, 5, 17), card.birthday)
        assertEquals(listOf("https://example.com/maria"), card.urls)
    }

    @Test
    fun parsesVersion21QuotedPrintableWithCharsetAndSoftLineBreaks() {
        val text = listOf(
            "BEGIN:VCARD",
            "VERSION:2.1",
            "N;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE:=4E=C3=BA=C3=B1=65=7A;=4A=6F=73=C3=A9;;;",
            "FN;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE:=4A=6F=73=C3=A9=20=4E=C3=BA=C3=B1=65=7A",
            "TEL;CELL;PREF:3001234567",
            "TEL;WORK;VOICE:6015550199",
            "NOTE;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8:Primera l=C3=ADnea=0D=0ASegunda =",
            "l=C3=ADnea",
            "ORG;CHARSET=ISO-8859-1;ENCODING=QUOTED-PRINTABLE:Caf=E9 Central",
            "BDAY:19851224",
            "END:VCARD",
        ).joinToString("\r\n")

        val card = VCardParser.parse(text).contacts.single()

        assertEquals("José Núñez", card.displayName)
        assertEquals("Núñez", card.familyName)
        assertEquals("José", card.givenName)
        assertEquals(
            listOf(
                VCardPhone("3001234567", setOf("CELL", "PREF")),
                VCardPhone("6015550199", setOf("WORK", "VOICE")),
            ),
            card.phones,
        )
        assertEquals("Primera línea\nSegunda línea", card.note)
        assertEquals("Café Central", card.organization)
        assertEquals(VCardDate(1985, 12, 24), card.birthday)
    }

    @Test
    fun keepsBackslashesLiteralInVersion21ExceptEscapedSemicolons() {
        val text = listOf(
            "BEGIN:VCARD",
            "VERSION:2.1",
            "N:Smith\\;Jones;Ann",
            "NOTE:C:\\new\\folder",
            "TEL:5550100",
            "END:VCARD",
        ).joinToString("\n")

        val card = VCardParser.parse(text).contacts.single()

        assertEquals("Smith;Jones", card.familyName)
        assertEquals("Ann Smith;Jones", card.displayName)
        assertEquals("C:\\new\\folder", card.note)
    }

    @Test
    fun parsesVersion4TelUrisQuotedTypesAndYearlessBirthday() {
        val text = listOf(
            "BEGIN:VCARD",
            "VERSION:4.0",
            "FN:Ada Lovelace",
            "TEL;VALUE=uri;PREF=1;TYPE=\"cell,voice\":tel:+44-20-7946-0001;ext=12",
            "tel;type=home:+44 20 7946 0002",
            "EMAIL:mailto:ada@example.com",
            "BDAY:--1210",
            "END:VCARD",
        ).joinToString("\n")

        val card = VCardParser.parse(text).contacts.single()

        assertEquals(
            listOf(
                VCardPhone("+44-20-7946-0001", setOf("CELL", "VOICE")),
                VCardPhone("+44 20 7946 0002", setOf("HOME")),
            ),
            card.phones,
        )
        assertEquals(listOf("ada@example.com"), card.emails)
        assertEquals(VCardDate(null, 12, 10), card.birthday)
    }

    @Test
    fun countsUnterminatedAndEmptyCardsAsFailedAndIgnoresNoise() {
        val text = listOf(
            "garbage before any card",
            "BEGIN:VCARD",
            "VERSION:3.0",
            "FN:First",
            "TEL:111",
            "END:VCARD",
            "",
            "BEGIN:VCARD",
            "VERSION:3.0",
            "PHOTO;ENCODING=b;TYPE=JPEG:AAAA",
            "END:VCARD",
            "BEGIN:VCARD",
            "FN:Never closed",
            "BEGIN:VCARD",
            "FN:Second",
            "TEL:222",
            "END:VCARD",
            "BEGIN:VCARD",
            "FN:Truncated at end of file",
        ).joinToString("\r\n")

        val result = VCardParser.parse(text)

        assertEquals(listOf("First", "Second"), result.contacts.map { it.displayName })
        assertEquals(3, result.failed)
    }

    @Test
    fun rejectsImpossibleDatesAndUnknownDateFormats() {
        fun birthdayOf(value: String): VCardDate? = VCardParser.parse(
            "BEGIN:VCARD\nFN:X\nBDAY:$value\nEND:VCARD",
        ).contacts.single().birthday

        assertNull(birthdayOf("1990-02-30"))
        assertNull(birthdayOf("sometime in May"))
        assertEquals(VCardDate(2000, 2, 29), birthdayOf("2000-02-29T00:00:00Z"))
        assertEquals(VCardDate(null, 2, 29), birthdayOf("--02-29"))
    }

    @Test
    fun returnsNothingForTextWithoutCards() {
        val result = VCardParser.parse("[{\"name\":\"Json contact\"}]")

        assertTrue(result.contacts.isEmpty())
        assertEquals(0, result.failed)
    }

    @Test
    fun writesVersion3WithCrLfEscapingAndTypeParameters() {
        val text = VCardWriter.write(
            listOf(
                VCardContact(
                    formattedName = "O'Neil, Sam; Jr.",
                    familyName = "O'Neil",
                    givenName = "Sam",
                    phones = listOf(
                        VCardPhone("+1 555 0100", setOf("WORK", "CELL")),
                        VCardPhone("5550101"),
                    ),
                    emails = listOf("sam@example.com"),
                    organization = "Night & Day; Ltd",
                    note = "First\nSecond\\Third",
                    birthday = VCardDate(null, 3, 9),
                    urls = listOf("https://example.com/?a=1,2;b"),
                ),
            ),
        )

        assertEquals(
            listOf(
                "BEGIN:VCARD",
                "VERSION:3.0",
                "N:O'Neil;Sam;;;",
                "FN:O'Neil\\, Sam\\; Jr.",
                "TEL;TYPE=WORK,CELL:+1 555 0100",
                "TEL:5550101",
                "EMAIL;TYPE=INTERNET:sam@example.com",
                "ORG:Night & Day\\; Ltd",
                "NOTE:First\\nSecond\\\\Third",
                "BDAY:--03-09",
                "URL:https://example.com/?a=1,2;b",
                "END:VCARD",
                "",
            ).joinToString("\r\n"),
            text,
        )
    }

    @Test
    fun foldsLongLinesAtSeventyFiveOctetsWithoutSplittingCharacters() {
        val note = "ñandú 😀 ".repeat(40).trim()
        val text = VCardWriter.write(listOf(VCardContact(formattedName = "Fold", note = note)))

        val physicalLines = text.split("\r\n")
        assertTrue(physicalLines.all { it.toByteArray(Charsets.UTF_8).size <= 75 })
        assertTrue(physicalLines.count { it.startsWith(" ") } > 3)
        assertFalse(text.contains('\uFFFD'))
        assertEquals(note, VCardParser.parse(text).contacts.single().note)
    }

    @Test
    fun roundTripsContactsThroughWriterAndParser() {
        val birthday = LocalDate.of(1988, 2, 29).atStartOfDay(zone).toInstant().toEpochMilli()
        val contacts = listOf(
            Contact(
                id = "12",
                name = "Zoë Öztürk-Müller",
                phones = listOf(
                    ContactPhone("+573001234567", PhoneEntity.Type.MOBILE),
                    ContactPhone("6015550101", PhoneEntity.Type.FAX_WORK),
                    ContactPhone("123;456", PhoneEntity.Type.HOME),
                ),
                birthday = birthday,
                email = "zoe@example.com",
                company = "Müller, Söhne & Co; KG",
                socialMedia = listOf("https://instagram.com/zoe", "https://example.com/a,b"),
                notes = "Multi\nline: note; with, punctuation \\ and emoji 🎉",
            ),
            Contact(
                id = "13",
                name = "Madonna",
                phones = listOf(ContactPhone("5550001", PhoneEntity.Type.OTHER)),
            ),
            Contact(
                id = "14",
                name = "+57 300 765 4321",
                phones = listOf(ContactPhone("+573007654321", PhoneEntity.Type.WORK_MOBILE)),
            ),
        )

        val text = VCardWriter.write(contacts.map { VCardContactMapper.toVCard(it, zone) })
        val parsed = VCardParser.parse(text)
        val restored = parsed.contacts.map { VCardContactMapper.toDraft(it, zone, currentYear = 2026) }

        assertEquals(0, parsed.failed)
        assertEquals(
            contacts.map { it.copy(id = "") }.let { expected ->
                expected.dropLast(1) + expected.last().copy(name = "")
            },
            restored.map { it.contact },
        )
        assertTrue(restored.all { it.additionalEmails.isEmpty() })
        assertEquals("Öztürk-Müller", parsed.contacts.first().familyName)
        assertEquals("Zoë", parsed.contacts.first().givenName)
    }

    @Test
    fun mapsPhoneTypesAndKeepsExtraEmailsWhenImporting() {
        val draft = VCardContactMapper.toDraft(
            VCardContact(
                familyName = "Curie",
                givenName = "Marie",
                phones = listOf(
                    VCardPhone("1", setOf("CELL")),
                    VCardPhone("2", setOf("WORK", "CELL")),
                    VCardPhone("3", setOf("HOME", "FAX")),
                    VCardPhone("4", setOf("WORK", "VOICE")),
                    VCardPhone("5", setOf("VOICE")),
                    VCardPhone("6", setOf("PREF")),
                    VCardPhone("7", emptySet()),
                    VCardPhone("8", setOf("PAGER")),
                    VCardPhone("9", setOf("MAIN")),
                ),
                emails = listOf(" marie@example.com ", "curie@example.org", "marie@example.com"),
                birthday = VCardDate(null, 2, 29),
            ),
            zone,
            currentYear = 2026,
        )

        assertEquals("Marie Curie", draft.contact.name)
        assertEquals(
            listOf(
                PhoneEntity.Type.MOBILE,
                PhoneEntity.Type.WORK_MOBILE,
                PhoneEntity.Type.FAX_HOME,
                PhoneEntity.Type.WORK,
                PhoneEntity.Type.OTHER,
                PhoneEntity.Type.MOBILE,
                PhoneEntity.Type.MOBILE,
                PhoneEntity.Type.PAGER,
                PhoneEntity.Type.MAIN,
            ),
            draft.contact.phones.map { it.type },
        )
        assertEquals("marie@example.com", draft.contact.email)
        assertEquals(listOf("curie@example.org"), draft.additionalEmails)
        assertEquals(
            LocalDate.of(2024, 2, 29).atStartOfDay(zone).toInstant().toEpochMilli(),
            draft.contact.birthday,
        )
    }

    @Test
    fun usesOrganizationAsNameWhenCardHasNoPersonName() {
        val draft = VCardContactMapper.toDraft(
            VCardContact(organization = "Pizzería Napoli", phones = listOf(VCardPhone("5550100"))),
            zone,
            currentYear = 2026,
        )

        assertEquals("Pizzería Napoli", draft.contact.name)
        assertEquals("Pizzería Napoli", draft.contact.company)
    }

    @Test
    fun detectsCharsetFromByteOrderMarkAndUtf8Validity() {
        val utf8 = "BEGIN:VCARD\r\nFN:José\r\nEND:VCARD".toByteArray(Charsets.UTF_8)
        val latin1 = "BEGIN:VCARD\r\nFN:José\r\nEND:VCARD".toByteArray(Charsets.ISO_8859_1)
        val utf16 = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) +
            "BEGIN:VCARD\r\nFN:José\r\nEND:VCARD".toByteArray(Charsets.UTF_16LE)

        assertEquals(Charsets.UTF_8, VCardCharsets.detect(utf8.inputStream()))
        assertEquals(VCardCharsets.legacyFallback, VCardCharsets.detect(latin1.inputStream()))
        assertEquals(Charsets.UTF_16LE, VCardCharsets.detect(utf16.inputStream()))
        assertEquals(
            "José",
            VCardParser.parse(String(utf16, Charsets.UTF_16LE)).contacts.single().displayName,
        )
        assertEquals(
            "José",
            VCardParser.parse(String(latin1, VCardCharsets.legacyFallback)).contacts.single().displayName,
        )
    }

    @Test
    fun streamsCardsFromLineSequence() {
        val lines = sequence {
            repeat(500) { index ->
                yield("BEGIN:VCARD")
                yield("VERSION:3.0")
                yield("FN:Contact $index")
                yield("TEL;TYPE=CELL:300${index.toString().padStart(7, '0')}")
                yield("END:VCARD")
            }
        }

        val result = VCardParser.parse(lines)

        assertEquals(500, result.contacts.size)
        assertEquals("Contact 499", result.contacts.last().displayName)
        assertEquals(0, result.failed)
    }
}
