package com.bookverse.contacts.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class DeletedContact(
    val id: Long,
    val contact: Contact,
    val deletedAt: Long,
)
