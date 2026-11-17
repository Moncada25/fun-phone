package com.bookverse.contacts.data.repository

import android.content.Context
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import contacts.core.Contacts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileRepository(private val context: Context) {
    private val contactPhotoStore = ContactPhotoStore(context)

    suspend fun getProfileContact(): Contact? = withContext(Dispatchers.IO) {
        val providerContact = Contacts(context).profile().query().find().contact
        val profileContact = providerContact?.toAppContact(
            unknownName = context.getString(com.bookverse.contacts.R.string.unknown),
            resolvedPhotoUri = contactPhotoStore.resolveProfilePhoto(providerContact.photoUri),
        )

        profileContact ?: return@withContext null
        DiagnosticLog.debug(DiagnosticEvent.CONTACT_PROFILE_LOADED)

        profileContact
    }

    suspend fun deleteProfile(): Boolean = withContext(Dispatchers.IO) {
        val deleteResult = Contacts(context).profile().delete().contact().commit()
        if (deleteResult.isSuccessful) {
            contactPhotoStore.removeProfilePhoto()
        }
        deleteResult.isSuccessful
    }
}
