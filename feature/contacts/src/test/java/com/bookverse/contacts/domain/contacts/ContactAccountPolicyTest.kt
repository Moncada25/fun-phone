package com.bookverse.contacts.domain.contacts

import com.bookverse.contacts.data.model.ContactAccount
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactAccountPolicyTest {
    private val google = ContactAccount(name = "user@gmail.com", type = "com.google")
    private val davAddressBook = ContactAccount(
        name = "Contacts (user) #1",
        type = "at.bitfire.davdroid.address_book",
    )
    private val signal = ContactAccount(name = "Signal", type = "org.thoughtcrime.securesms")
    private val adapters = listOf(
        ContactSyncAdapter(accountType = "com.google", supportsUploading = true),
        ContactSyncAdapter(accountType = "at.bitfire.davdroid.address_book", supportsUploading = true),
        ContactSyncAdapter(accountType = "org.thoughtcrime.securesms", supportsUploading = false),
    )

    @Test
    fun startsWithLocalAccountAndKeepsSystemAccounts() {
        val accounts = ContactAccountPolicy.writableAccounts(
            systemAccounts = listOf(google),
            providerAccounts = emptyList(),
            contactSyncAdapters = adapters,
        )

        assertEquals(listOf(ContactAccount.LOCAL_ACCOUNT, google), accounts)
    }

    @Test
    fun addsProviderAccountsThatAccountManagerDoesNotExpose() {
        val accounts = ContactAccountPolicy.writableAccounts(
            systemAccounts = emptyList(),
            providerAccounts = listOf(davAddressBook),
            contactSyncAdapters = adapters,
        )

        assertEquals(listOf(ContactAccount.LOCAL_ACCOUNT, davAddressBook), accounts)
    }

    @Test
    fun skipsProviderAccountsWhoseSyncAdapterCannotUpload() {
        val accounts = ContactAccountPolicy.writableAccounts(
            systemAccounts = emptyList(),
            providerAccounts = listOf(signal, davAddressBook),
            contactSyncAdapters = adapters,
        )

        assertEquals(listOf(ContactAccount.LOCAL_ACCOUNT, davAddressBook), accounts)
    }

    @Test
    fun skipsProviderAccountsWithoutAKnownContactsSyncAdapter() {
        val unknown = ContactAccount(name = "Orphan", type = "com.example.removed")

        val accounts = ContactAccountPolicy.writableAccounts(
            systemAccounts = emptyList(),
            providerAccounts = listOf(unknown),
            contactSyncAdapters = adapters,
        )

        assertEquals(listOf(ContactAccount.LOCAL_ACCOUNT), accounts)
    }

    @Test
    fun skipsBlacklistedAndLocalAccountsAndRemovesDuplicates() {
        val whatsapp = ContactAccount(name = "WhatsApp", type = ContactAccount.WHATSAPP)
        val accounts = ContactAccountPolicy.writableAccounts(
            systemAccounts = listOf(google, whatsapp),
            providerAccounts = listOf(
                google,
                ContactAccount.LOCAL_ACCOUNT,
                whatsapp,
                davAddressBook,
                davAddressBook,
            ),
            contactSyncAdapters = adapters + ContactSyncAdapter(ContactAccount.WHATSAPP, true),
        )

        assertEquals(listOf(ContactAccount.LOCAL_ACCOUNT, google, davAddressBook), accounts)
    }

    @Test
    fun resolvesPreferredAccountOnlyWhenItIsWritable() {
        val writable = listOf(ContactAccount.LOCAL_ACCOUNT, davAddressBook)

        assertEquals(
            davAddressBook,
            ContactAccountPolicy.resolveWritable(listOf(signal, davAddressBook), writable),
        )
        assertEquals(
            ContactAccount.LOCAL_ACCOUNT,
            ContactAccountPolicy.resolveWritable(listOf(google), writable),
        )
        assertEquals(
            ContactAccount.LOCAL_ACCOUNT,
            ContactAccountPolicy.resolveWritable(emptyList(), writable),
        )
    }
}
