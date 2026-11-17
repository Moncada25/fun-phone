package com.bookverse.contacts.integration

import android.content.ContentValues
import android.content.Context
import android.provider.ContactsContract
import com.bookverse.contacts.data.contacts.ContactMergeGateway
import com.bookverse.contacts.data.model.DuplicateGroup
import contacts.core.Contacts

class AndroidContactMergeGateway(context: Context) : ContactMergeGateway {
    private val appContext = context.applicationContext

    override suspend fun merge(
        group: DuplicateGroup,
        primaryContactId: String,
    ): Boolean = try {
        val contactIds = group.contacts.map { it.id.toLong() }.toSet()
        val libraryContacts = Contacts(appContext)
            .query()
            .find()
            .filter { it.id in contactIds }

        if (libraryContacts.isEmpty()) {
            return false
        }

        val primaryId = primaryContactId.toLongOrNull() ?: libraryContacts.first().id
        val sorted = libraryContacts.sortedByDescending { it.id == primaryId }
        val result = Contacts(appContext)
            .aggregationExceptions()
            .link()
            .contacts(*sorted.toTypedArray())
            .commit()

        if (result.isSuccessful) {
            setNameAsSuperPrimary(primaryId)
        }
        result.isSuccessful
    } catch (_: Exception) {
        false
    }

    private fun setNameAsSuperPrimary(primaryContactId: Long) {
        try {
            val resolver = appContext.contentResolver
            val rawContactIds = mutableListOf<Long>()
            resolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts._ID),
                "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                arrayOf(primaryContactId.toString()),
                null,
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    rawContactIds.add(cursor.getLong(0))
                }
            }

            if (rawContactIds.isEmpty()) return

            val clearValues = ContentValues().apply {
                put(ContactsContract.Data.IS_SUPER_PRIMARY, 0)
            }
            for (rawId in rawContactIds) {
                resolver.update(
                    ContactsContract.Data.CONTENT_URI,
                    clearValues,
                    "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(
                        rawId.toString(),
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                    ),
                )
            }

            val values = ContentValues().apply {
                put(ContactsContract.Data.IS_PRIMARY, 1)
                put(ContactsContract.Data.IS_SUPER_PRIMARY, 1)
            }
            resolver.update(
                ContactsContract.Data.CONTENT_URI,
                values,
                "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(
                    rawContactIds.first().toString(),
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                ),
            )
        } catch (_: Exception) {
            // Linking succeeded; keep the merge result even if name preference could not be applied.
        }
    }
}
