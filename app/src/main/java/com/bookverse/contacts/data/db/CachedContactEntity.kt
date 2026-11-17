package com.bookverse.contacts.data.db

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactAccount
import com.bookverse.contacts.data.model.ContactPhone

@Entity(tableName = "cached_contacts")
data class CachedContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phones: List<ContactPhone>,
    val accounts: List<ContactAccount>? = null,
    val birthday: Long? = null,
    val photoUri: Uri? = null,
    val remotePhotoUri: Uri? = null,
    val email: String? = null,
    val company: String? = null,
    val isFavorite: Boolean = false,
    val socialMedia: List<String> = emptyList(),
    val notes: String? = null,
    val isBlocked: Boolean = false,
    val ringtone: Uri? = null,
    val isProfile: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toContact(): Contact = Contact(
        id = id,
        name = name,
        phones = phones,
        accounts = accounts,
        birthday = birthday,
        photoUri = photoUri,
        remotePhotoUri = remotePhotoUri,
        email = email,
        company = company,
        isFavorite = isFavorite,
        socialMedia = socialMedia,
        notes = notes,
        isBlocked = isBlocked,
        ringtone = ringtone
    )

    companion object {
        fun from(contact: Contact, isProfile: Boolean): CachedContactEntity = CachedContactEntity(
            id = contact.id,
            name = contact.name,
            phones = contact.phones,
            accounts = contact.accounts,
            birthday = contact.birthday,
            photoUri = contact.photoUri,
            remotePhotoUri = contact.remotePhotoUri,
            email = contact.email,
            company = contact.company,
            isFavorite = contact.isFavorite,
            socialMedia = contact.socialMedia,
            notes = contact.notes,
            isBlocked = contact.isBlocked,
            ringtone = contact.ringtone,
            isProfile = isProfile
        )
    }
}
