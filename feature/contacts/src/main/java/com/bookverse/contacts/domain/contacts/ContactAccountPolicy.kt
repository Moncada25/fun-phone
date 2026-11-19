package com.bookverse.contacts.domain.contacts

import com.bookverse.contacts.data.model.ContactAccount

data class ContactSyncAdapter(
    val accountType: String,
    val supportsUploading: Boolean,
)

object ContactAccountPolicy {
    fun writableAccounts(
        systemAccounts: List<ContactAccount>,
        providerAccounts: List<ContactAccount>,
        contactSyncAdapters: List<ContactSyncAdapter>,
    ): List<ContactAccount> {
        val uploadableTypes = contactSyncAdapters
            .filter(ContactSyncAdapter::supportsUploading)
            .map(ContactSyncAdapter::accountType)
            .toSet()
        val discovered = providerAccounts
            .filter { it.type in uploadableTypes }
            .sortedBy { it.displayName.lowercase() }
        return (listOf(ContactAccount.LOCAL_ACCOUNT) + systemAccounts + discovered)
            .filterNot { it.isBlacklisted() }
            .distinctBy { it.name to it.type }
    }

    fun resolveWritable(
        preferred: List<ContactAccount>,
        writable: List<ContactAccount>,
    ): ContactAccount {
        val writableKeys = writable.map { it.name to it.type }.toSet()
        return preferred
            .filterNot { it.isLocal || it.isBlacklisted() }
            .firstOrNull { (it.name to it.type) in writableKeys }
            ?: ContactAccount.LOCAL_ACCOUNT
    }

    private fun ContactAccount.isBlacklisted(): Boolean =
        name in ContactAccount.ACCOUNTS_BLACKLIST
}
