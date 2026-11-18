package com.bookverse.contacts.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactAccount
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.domain.contacts.ContactImportPolicy
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object JsonUtils {
    private val gson = GsonBuilder().registerTypeAdapter(Uri::class.java, UriAdapter()).create()

    @Suppress("UNUSED_PARAMETER")
    private suspend fun buildContactsJson(
        contacts: List<Contact>,
        profileId: String,
        includePhotos: Boolean
    ): String = withContext(Dispatchers.IO) {
        val contactsToExport = mutableListOf<Map<String, Any?>>()
        for (contact in contacts) {
            val contactMap = mutableMapOf<String, Any?>()
            contactMap["name"] = contact.name
            contactMap["phones"] = contact.phones
            contactMap["birthday"] = contact.birthday
            contactMap["email"] = contact.email
            contactMap["company"] = contact.company
            contactMap["isFavorite"] = contact.isFavorite
            contactMap["socialMedia"] = contact.socialMedia
            // JSON and QR exports are portable text files. A content:// URI only works inside the
            // originating device, so photos are shared separately through Android Sharesheet.
            contactMap["remotePhotoUri"] = null
            contactsToExport.add(contactMap)
        }
        gson.toJson(contactsToExport)
    }

    suspend fun shareContactsAsJson(
        context: Context,
        contacts: List<Contact>,
        shareJsonLauncher: ActivityResultLauncher<Intent>,
        profileId: String,
        includePhotos: Boolean
    ) {
        val jsonFile = withContext(Dispatchers.IO) {
            val jsonString = buildContactsJson(contacts, profileId, includePhotos)
            File(context.cacheDir, "contacts_${UUID.randomUUID()}.json").also { file ->
                FileOutputStream(file).use { it.write(jsonString.toByteArray()) }
            }
        }
        withContext(Dispatchers.Main) {
            val uri =
                FileProvider.getUriForFile(context, context.packageName + ".provider", jsonFile)
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            shareJsonLauncher.launch(
                Intent.createChooser(
                    shareIntent,
                    context.getString(R.string.json_share_chooser)
                )
            )
        }
    }

    suspend fun importContactsFromJson(
        jsonUri: Uri,
        context: Context,
        contactsViewModel: ContactsViewModel,
        account: ContactAccount
    ): Int = withContext(Dispatchers.IO) {
        try {
            val jsonString =
                context.contentResolver.openInputStream(jsonUri)?.bufferedReader()?.readText()
            if (jsonString.isNullOrBlank()) return@withContext 0
            val contactListType = object : TypeToken<List<Contact>>() {}.type
            val contactsToImport: List<Contact> = gson.fromJson(jsonString, contactListType)
            var contactsImported = 0
            for (contact in contactsToImport) {
                val importableContact = ContactImportPolicy.prepare(contact, account) ?: continue
                // Legacy remote-photo fields are intentionally ignored. Imports never fetch URLs.
                val newContact = importableContact.copy(photoUri = null, remotePhotoUri = null)
                contactsViewModel.createContact(
                    contact = newContact,
                    account = account,
                    favorite = newContact.isFavorite
                )
                contactsImported++
            }
            contactsImported
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.CONTACT_JSON_IMPORT_FAILED)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.json_import_error),
                    Toast.LENGTH_LONG
                ).show()
            }
            0
        }
    }

    suspend fun saveContactsAsJson(
        context: Context,
        contacts: List<Contact>,
        uri: Uri,
        profileId: String,
        includePhotos: Boolean
    ) = withContext(Dispatchers.IO) {
        try {
            val jsonString = buildContactsJson(contacts, profileId, includePhotos)
            context.contentResolver.openOutputStream(uri)
                ?.use { it.write(jsonString.toByteArray()) }
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.json_saved_success),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.json_saved_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
