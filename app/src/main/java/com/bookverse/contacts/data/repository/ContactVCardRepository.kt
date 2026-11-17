package com.bookverse.contacts.data.repository

import android.content.Context
import android.net.Uri
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactAccount
import com.bookverse.contacts.domain.contacts.vcard.VCardCharsets
import com.bookverse.contacts.domain.contacts.vcard.VCardContactMapper
import com.bookverse.contacts.domain.contacts.vcard.VCardImportPlanner
import com.bookverse.contacts.domain.contacts.vcard.VCardImportSummary
import com.bookverse.contacts.domain.contacts.vcard.VCardParser
import com.bookverse.contacts.domain.contacts.vcard.VCardWriter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.OutputStream

class ContactVCardRepository(
    context: Context,
    private val contactRepository: ContactRepository,
) {
    private val contentResolver = context.applicationContext.contentResolver

    suspend fun containsVCard(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(SNIFF_BYTES)
                val read = input.read(buffer).coerceAtLeast(0)
                String(buffer, 0, read, Charsets.ISO_8859_1)
                    .replace("\u0000", "")
                    .contains(CARD_MARKER, ignoreCase = true)
            } ?: false
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            false
        }
    }

    suspend fun importContacts(
        uri: Uri,
        existing: List<Contact>,
        account: ContactAccount,
    ): VCardImportSummary? = withContext(Dispatchers.IO) {
        try {
            val charset = contentResolver.openInputStream(uri)?.use(VCardCharsets::detect)
                ?: return@withContext null
            val parsed = contentResolver.openInputStream(uri)?.use { input ->
                input.reader(charset).buffered().useLines { lines -> VCardParser.parse(lines) }
            } ?: return@withContext null
            if (parsed.contacts.isEmpty() && parsed.failed == 0) return@withContext null

            val plan = VCardImportPlanner.plan(parsed, existing, account)
            var imported = 0
            var failed = plan.failed
            for (draft in plan.toImport) {
                ensureActive()
                val inserted = try {
                    contactRepository.insertContact(
                        contact = draft.contact,
                        account = account,
                        favorite = null,
                        additionalEmails = draft.additionalEmails,
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    false
                }
                if (inserted) imported++ else failed++
            }
            VCardImportSummary(
                imported = imported,
                skippedDuplicates = plan.skippedDuplicates,
                failed = failed,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }

    suspend fun exportContacts(uri: Uri, contacts: List<Contact>): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val output = openForWriting(uri) ?: return@withContext false
                output.bufferedWriter(Charsets.UTF_8).use { writer ->
                    contacts.forEach { contact ->
                        ensureActive()
                        VCardWriter.writeContact(writer, VCardContactMapper.toVCard(contact))
                    }
                }
                true
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                false
            }
        }

    private fun openForWriting(uri: Uri): OutputStream? = try {
        contentResolver.openOutputStream(uri, "wt")
    } catch (_: Exception) {
        contentResolver.openOutputStream(uri)
    }

    private companion object {
        const val SNIFF_BYTES = 4 * 1024
        const val CARD_MARKER = "BEGIN:VCARD"
    }
}
