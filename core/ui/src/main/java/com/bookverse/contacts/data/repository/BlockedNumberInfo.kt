package com.bookverse.contacts.data.repository

import com.bookverse.contacts.data.model.Contact

data class BlockedNumberInfo(
    val id: Long,
    val displayNumber: String,
    val normalizedNumber: String?,
    val contact: Contact?,
)
