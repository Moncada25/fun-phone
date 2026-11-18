package com.bookverse.contacts.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.repository.ContactRepository
import com.bookverse.contacts.data.repository.ContactPhotoWriter
import com.bookverse.contacts.data.repository.InCallBackgroundChange
import com.bookverse.contacts.data.repository.InCallBackgroundUpdateResult
import com.bookverse.contacts.data.repository.IncomingCallBackgroundChange
import com.bookverse.contacts.data.repository.IncomingCallBackgroundUpdateResult
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.utils.PreparedContactPhoto
import contacts.core.Contacts
import contacts.core.entities.ExistingContactEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ContactMediaCoordinator(
    private val application: Application,
    private val contactRepository: ContactRepository,
    private val scope: CoroutineScope,
    private val onBackgroundWarning: (Int) -> Unit,
) {
    private val photoCache = LruCache<String, Bitmap>(50)
    private val contactPhotoWriter = ContactPhotoWriter(application)

    fun uriToBitmap(uri: Uri): Bitmap? =
        com.bookverse.contacts.utils.ContactUtils.uriToBitmap(uri, application)

    suspend fun loadContactPhoto(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        val key = uri.toString()
        photoCache.get(key)?.let { return@withContext it }
        val bitmap = uriToBitmap(uri)
        DiagnosticLog.debug(DiagnosticEvent.CONTACT_PHOTO_LOAD_COMPLETED)
        if (bitmap != null) photoCache.put(key, bitmap)
        bitmap
    }

    suspend fun getIncomingCallBackgroundUri(contactId: String): Uri? =
        contactRepository.getIncomingCallBackgroundUri(contactId)

    suspend fun getInCallBackgroundUri(contactId: String): Uri? =
        contactRepository.getInCallBackgroundUri(contactId)

    fun updateIncomingCallBackground(
        contactId: String,
        change: IncomingCallBackgroundChange,
        onResult: (Boolean) -> Unit,
    ) {
        if (change == IncomingCallBackgroundChange.Unchanged) {
            onResult(true)
            return
        }
        scope.launch(Dispatchers.IO) {
            val result = contactRepository.updateIncomingCallBackground(contactId, change)
            withContext(Dispatchers.Main) {
                if (result != IncomingCallBackgroundUpdateResult.Success) {
                    onBackgroundWarning(result.warningMessageResource())
                }
                onResult(result == IncomingCallBackgroundUpdateResult.Success)
            }
        }
    }

    fun updateInCallBackground(
        contactId: String,
        change: InCallBackgroundChange,
        onResult: (Boolean) -> Unit,
    ) {
        if (change == InCallBackgroundChange.Unchanged) {
            onResult(true)
            return
        }
        scope.launch(Dispatchers.IO) {
            val result = contactRepository.updateInCallBackground(contactId, change)
            withContext(Dispatchers.Main) {
                if (result != InCallBackgroundUpdateResult.Success) {
                    onBackgroundWarning(result.warningMessageResource())
                }
                onResult(result == InCallBackgroundUpdateResult.Success)
            }
        }
    }

    fun evictContactPhotos() {
        photoCache.evictAll()
    }

    fun writeContactPhoto(
        contact: ExistingContactEntity,
        api: Contacts,
        photo: PreparedContactPhoto,
    ): Boolean = contactPhotoWriter.write(contact, api, photo).also { evictContactPhotos() }

    fun removeContactPhoto(contact: ExistingContactEntity, api: Contacts): Boolean =
        contactPhotoWriter.remove(contact, api).also { evictContactPhotos() }
}

private fun IncomingCallBackgroundUpdateResult.warningMessageResource(): Int = when (this) {
    IncomingCallBackgroundUpdateResult.ImageTooLarge ->
        R.string.incoming_call_background_too_large

    IncomingCallBackgroundUpdateResult.InvalidImage ->
        R.string.incoming_call_background_invalid

    else -> R.string.incoming_call_background_save_warning
}

private fun InCallBackgroundUpdateResult.warningMessageResource(): Int = when (this) {
    InCallBackgroundUpdateResult.ImageTooLarge ->
        R.string.in_call_background_too_large

    InCallBackgroundUpdateResult.InvalidImage ->
        R.string.in_call_background_invalid

    else -> R.string.in_call_background_save_warning
}
