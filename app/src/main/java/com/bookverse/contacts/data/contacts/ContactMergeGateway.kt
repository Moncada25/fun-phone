package com.bookverse.contacts.data.contacts

import com.bookverse.contacts.data.model.DuplicateGroup

interface ContactMergeGateway {
    suspend fun merge(group: DuplicateGroup, primaryContactId: String): Boolean
}
