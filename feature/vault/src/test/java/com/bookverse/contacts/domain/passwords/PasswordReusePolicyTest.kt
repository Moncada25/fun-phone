package com.bookverse.contacts.domain.passwords

import com.bookverse.contacts.data.password.PasswordCredential
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordReusePolicyTest {
    private fun credential(id: Long, password: String, deleted: Boolean = false) = PasswordCredential(
        id = id,
        name = "account-$id",
        username = "user-$id",
        password = password,
        note = null,
        updatedAt = 0L,
        createdAt = 0L,
        isDeleted = deleted,
    )

    @Test
    fun flagsEveryEntrySharingAPassword() {
        val credentials = listOf(
            credential(1, "shared"),
            credential(2, "unique"),
            credential(3, "shared"),
            credential(4, "other"),
            credential(5, "other"),
            credential(6, "other"),
        )
        assertEquals(setOf(1L, 3L, 4L, 5L, 6L), PasswordReusePolicy.reusedIds(credentials))
    }

    @Test
    fun comparisonIsExactAndIgnoresEmptyOrDeletedEntries() {
        val credentials = listOf(
            credential(1, "Secret"),
            credential(2, "secret"),
            credential(3, "secret "),
            credential(4, ""),
            credential(5, ""),
            credential(6, "gone", deleted = true),
            credential(7, "gone"),
        )
        assertTrue(PasswordReusePolicy.reusedIds(credentials).isEmpty())
    }

    @Test
    fun draftPasswordIsComparedAgainstOtherEntriesOnly() {
        val credentials = listOf(credential(1, "alpha"), credential(2, "beta"), credential(3, "old", deleted = true))
        assertTrue(PasswordReusePolicy.isReused("alpha", ownId = null, credentials))
        assertFalse(PasswordReusePolicy.isReused("alpha", ownId = 1L, credentials))
        assertTrue(PasswordReusePolicy.isReused("beta", ownId = 1L, credentials))
        assertFalse(PasswordReusePolicy.isReused("old", ownId = null, credentials))
        assertFalse(PasswordReusePolicy.isReused("", ownId = null, credentials + credential(4, "")))
    }
}
