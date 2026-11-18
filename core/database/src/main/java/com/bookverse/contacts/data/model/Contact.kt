package com.bookverse.contacts.data.model

import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import kotlinx.parcelize.Parcelize

@Immutable
@Parcelize
data class Contact(
    val id: String,
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

    @ColumnInfo(defaultValue = "0")
    val isBlocked: Boolean = false,

    val ringtone: Uri? = null
) : Parcelable {
    constructor(
        name: String,
        phones: List<ContactPhone>
    ) : this(
        id = "",
        name = name,
        phones = phones
    )
}
