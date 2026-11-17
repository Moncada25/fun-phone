package com.bookverse.contacts.integration

import android.graphics.Bitmap
import android.net.Uri
import com.bookverse.contacts.domain.call.CallContactLookup
import com.bookverse.contacts.domain.call.CallContactPresentation
import com.bookverse.contacts.domain.call.CallPhonePresentation
import com.bookverse.contacts.domain.call.CallPhoneType
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import contacts.core.entities.PhoneEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

class ContactsCallContactLookup(
    private val contactsViewModel: ContactsViewModel,
) : CallContactLookup {
    override suspend fun findByPhoneNumber(phoneNumber: String): CallContactPresentation? {
        val contact = contactsViewModel.findContactByPhoneNumber(phoneNumber)
            ?: withTimeoutOrNull(CONTACT_LOAD_TIMEOUT_MILLIS.milliseconds) {
                contactsViewModel.contacts.mapNotNull {
                    contactsViewModel.findContactByPhoneNumber(phoneNumber)
                }.first()
            }
            ?: return null
        return CallContactPresentation(
            name = contact.name,
            photoUri = contact.photoUri,
            incomingCallBackgroundUri = contactsViewModel.getIncomingCallBackgroundUri(contact.id),
            inCallBackgroundUri = contactsViewModel.getInCallBackgroundUri(contact.id),
            phones = contact.phones.map { phone ->
                CallPhonePresentation(
                    number = phone.number,
                    type = phone.type.toCallPhoneType(),
                )
            },
        )
    }

    override suspend fun loadPhoto(uri: Uri): Bitmap? = contactsViewModel.loadContactPhoto(uri)

    private companion object {
        const val CONTACT_LOAD_TIMEOUT_MILLIS = 3_000L
    }
}

internal fun PhoneEntity.Type?.toCallPhoneType(): CallPhoneType = when (this) {
    PhoneEntity.Type.MOBILE -> CallPhoneType.MOBILE
    PhoneEntity.Type.WORK -> CallPhoneType.WORK
    PhoneEntity.Type.HOME -> CallPhoneType.HOME
    PhoneEntity.Type.MAIN -> CallPhoneType.MAIN
    else -> CallPhoneType.OTHER
}
