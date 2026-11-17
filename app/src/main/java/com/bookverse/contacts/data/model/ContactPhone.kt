package com.bookverse.contacts.data.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import contacts.core.entities.PhoneEntity
import kotlinx.parcelize.Parcelize

@Immutable
@Parcelize
data class ContactPhone(
    val number: String,
    val type: PhoneEntity.Type?,
    val countryCode: String? = null
) : Parcelable