package com.bookverse.contacts.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactAccountTest {
    @Test
    fun shouldIdentifyLocalAccount_whenStoredSentinelIsRecreated() {
        val restored = ContactAccount(
            name = ContactAccount.LOCAL_ACCOUNT.name,
            type = ContactAccount.LOCAL_ACCOUNT.type,
            displayName = "Device",
        )

        assertTrue(restored.isLocal)
    }

    @Test
    fun shouldNotIdentifySyncedAccount_asLocal() {
        val synced = ContactAccount(
            name = "person@example.com",
            type = "com.google",
        )

        assertFalse(synced.isLocal)
    }
}
