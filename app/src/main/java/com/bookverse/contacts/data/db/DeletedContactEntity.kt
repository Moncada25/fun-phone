package com.bookverse.contacts.data.db

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactAccount
import com.bookverse.contacts.data.model.ContactPhone

@Entity(tableName = "deleted_contacts")
data class DeletedContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phones: List<ContactPhone>,
    val accounts: List<ContactAccount>? = null,
    val birthday: Long? = null,
    val email: String? = null,
    val additionalEmails: List<String> = emptyList(),
    val company: String? = null,
    val isFavorite: Boolean = false,
    val socialMedia: List<String> = emptyList(),
    val notes: String? = null,
    val ringtone: Uri? = null,
    val photoFileName: String? = null,
    val deletedAt: Long,
) {
    fun toContact(photoUri: Uri? = null): Contact = Contact(
        id = "",
        name = name,
        phones = phones,
        accounts = accounts,
        birthday = birthday,
        photoUri = photoUri,
        email = email,
        company = company,
        isFavorite = isFavorite,
        socialMedia = socialMedia,
        notes = notes,
        ringtone = ringtone,
    )

    companion object {
        fun from(
            contact: Contact,
            additionalEmails: List<String>,
            photoFileName: String?,
            deletedAt: Long,
        ): DeletedContactEntity = DeletedContactEntity(
            name = contact.name,
            phones = contact.phones,
            accounts = contact.accounts,
            birthday = contact.birthday,
            email = contact.email,
            additionalEmails = additionalEmails,
            company = contact.company,
            isFavorite = contact.isFavorite,
            socialMedia = contact.socialMedia,
            notes = contact.notes,
            ringtone = contact.ringtone,
            photoFileName = photoFileName,
            deletedAt = deletedAt,
        )
    }
}
