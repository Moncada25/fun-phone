package com.bookverse.contacts.data.repository

import android.content.Context
import com.bookverse.contacts.utils.PreparedContactPhoto
import contacts.core.Contacts
import contacts.core.entities.ExistingContactEntity
import contacts.core.util.PhotoData
import contacts.core.util.removePhotoDirect
import contacts.core.util.setPhotoDirect

internal class ContactPhotoWriter(context: Context) {
    private val photoStore = ContactPhotoStore(context)

    fun write(
        contact: ExistingContactEntity,
        api: Contacts,
        preparedPhoto: PreparedContactPhoto,
    ): Boolean {
        val providerSaved = preparedPhoto.providerVariants.any { variant ->
            contact.setPhotoDirect(api, PhotoData.from(variant.bytes))
        }
        val localSaved = if (contact.isProfile) {
            photoStore.saveProfilePhoto(preparedPhoto.localVariant)
        } else {
            photoStore.saveContactPhoto(contact.lookupKey, preparedPhoto.localVariant)
        }
        if (!localSaved && providerSaved) removeLocalPhoto(contact)
        return providerSaved && localSaved
    }

    fun remove(contact: ExistingContactEntity, api: Contacts): Boolean {
        if (!contact.removePhotoDirect(api)) return false
        return removeLocalPhoto(contact)
    }

    private fun removeLocalPhoto(contact: ExistingContactEntity): Boolean =
        if (contact.isProfile) {
            photoStore.removeProfilePhoto()
        } else {
            photoStore.removeContactPhoto(contact.lookupKey)
        }
}
