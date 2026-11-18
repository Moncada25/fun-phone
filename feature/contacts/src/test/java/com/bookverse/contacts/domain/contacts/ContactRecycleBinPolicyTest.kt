package com.bookverse.contacts.domain.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class ContactRecycleBinPolicyTest {
    private val day = TimeUnit.DAYS.toMillis(1)
    private val deletedAt = 1_700_000_000_000L

    @Test
    fun keepsContactsUntilThirtyDaysHavePassed() {
        assertEquals(30, ContactRecycleBinPolicy.RETENTION_DAYS)
        assertFalse(ContactRecycleBinPolicy.isExpired(deletedAt, deletedAt))
        assertFalse(ContactRecycleBinPolicy.isExpired(deletedAt, deletedAt + 30 * day - 1))
        assertTrue(ContactRecycleBinPolicy.isExpired(deletedAt, deletedAt + 30 * day))
        assertTrue(ContactRecycleBinPolicy.isExpired(deletedAt, deletedAt + 400 * day))
    }

    @Test
    fun doesNotExpireWhenClockMovedBackwards() {
        assertFalse(ContactRecycleBinPolicy.isExpired(deletedAt, deletedAt - 5 * day))
        assertEquals(30, ContactRecycleBinPolicy.daysRemaining(deletedAt, deletedAt - 5 * day))
    }

    @Test
    fun daysRemainingRoundsUpAndNeverGoesNegative() {
        assertEquals(30, ContactRecycleBinPolicy.daysRemaining(deletedAt, deletedAt))
        assertEquals(30, ContactRecycleBinPolicy.daysRemaining(deletedAt, deletedAt + 1))
        assertEquals(29, ContactRecycleBinPolicy.daysRemaining(deletedAt, deletedAt + day))
        assertEquals(1, ContactRecycleBinPolicy.daysRemaining(deletedAt, deletedAt + 30 * day - 1))
        assertEquals(0, ContactRecycleBinPolicy.daysRemaining(deletedAt, deletedAt + 30 * day))
        assertEquals(0, ContactRecycleBinPolicy.daysRemaining(deletedAt, deletedAt + 90 * day))
    }
}
