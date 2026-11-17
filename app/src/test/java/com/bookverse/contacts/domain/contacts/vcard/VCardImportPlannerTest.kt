package com.bookverse.contacts.domain.contacts.vcard

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactAccount
import com.bookverse.contacts.data.model.ContactPhone
import contacts.core.entities.PhoneEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class VCardImportPlannerTest {
    private val account = ContactAccount("device", "local")
    private val zone = ZoneId.of("UTC")

    @Test
    fun reportsImportableDuplicateAndFailedCardsSeparately() {
        val existing = listOf(
            Contact(
                id = "1",
                name = "Ana María López",
                phones = listOf(ContactPhone("+57 300 111 2233", PhoneEntity.Type.MOBILE)),
            ),
        )
        val text = listOf(
            card("Ana Maria Lopez", "3001112233"),
            card("Bruno Díaz", "3004445566"),
            card("Bruno Díaz", "+57 (300) 444-5566"),
            "BEGIN:VCARD\nVERSION:3.0\nFN:No Phone\nEMAIL:nophone@example.com\nEND:VCARD",
            "BEGIN:VCARD\nVERSION:3.0\nFN:Broken",
        ).joinToString("\n")

        val plan = VCardImportPlanner.plan(
            parsed = VCardParser.parse(text),
            existing = existing,
            account = account,
            zone = zone,
            currentYear = 2026,
        )

        assertEquals(listOf("Bruno Díaz"), plan.toImport.map { it.contact.name })
        assertEquals(listOf(account), plan.toImport.single().contact.accounts)
        assertEquals(2, plan.skippedDuplicates)
        assertEquals(2, plan.failed)
    }

    @Test
    fun summaryTotalAddsEveryOutcome() {
        val summary = VCardImportSummary(imported = 4, skippedDuplicates = 3, failed = 2)

        assertEquals(9, summary.total)
    }

    private fun card(name: String, phone: String): String = listOf(
        "BEGIN:VCARD",
        "VERSION:3.0",
        "FN:$name",
        "TEL;TYPE=CELL:$phone",
        "END:VCARD",
    ).joinToString("\n")
}
