package com.bookverse.contacts.domain.contacts.vcard

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactAccount
import com.bookverse.contacts.domain.contacts.ContactDraft
import com.bookverse.contacts.domain.contacts.ContactDuplicatePolicy
import com.bookverse.contacts.domain.contacts.ContactImportPolicy
import java.time.LocalDate
import java.time.ZoneId

data class VCardImportPlan(
    val toImport: List<ContactDraft>,
    val skippedDuplicates: Int,
    val failed: Int,
)

data class VCardImportSummary(
    val imported: Int,
    val skippedDuplicates: Int,
    val failed: Int,
) {
    val total: Int get() = imported + skippedDuplicates + failed
}

object VCardImportPlanner {
    fun plan(
        parsed: VCardParseResult,
        existing: List<Contact>,
        account: ContactAccount,
        zone: ZoneId = ZoneId.systemDefault(),
        currentYear: Int = LocalDate.now(zone).year,
    ): VCardImportPlan {
        var failed = parsed.failed
        val importable = parsed.contacts.mapNotNull { card ->
            val draft = VCardContactMapper.toDraft(card, zone, currentYear)
            val prepared = ContactImportPolicy.prepare(draft.contact, account)
            if (prepared == null) {
                failed++
                null
            } else {
                draft.copy(contact = prepared)
            }
        }
        val plan = ContactDuplicatePolicy.plan(existing, importable, ContactDraft::contact)
        return VCardImportPlan(
            toImport = plan.unique,
            skippedDuplicates = plan.duplicates,
            failed = failed,
        )
    }
}
