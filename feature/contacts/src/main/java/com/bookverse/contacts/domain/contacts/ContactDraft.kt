package com.bookverse.contacts.domain.contacts

import com.bookverse.contacts.data.model.Contact

data class ContactDraft(
    val contact: Contact,
    val additionalEmails: List<String> = emptyList(),
)
