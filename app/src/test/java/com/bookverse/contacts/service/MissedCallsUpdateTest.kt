package com.bookverse.contacts.service

import org.junit.Assert.assertEquals
import org.junit.Test

class MissedCallsUpdateTest {
    @Test
    fun clears_whenTelecomCountDropsToZero() {
        assertEquals(
            MissedCallsUpdate.Clear,
            resolveMissedCallsUpdate(count = 0, hasAnyNotification = true, callLogNumber = null),
        )
    }

    @Test
    fun keeps_whenFunPhoneAlreadyShowsMissedCalls() {
        assertEquals(
            MissedCallsUpdate.Keep,
            resolveMissedCallsUpdate(count = 3, hasAnyNotification = true, callLogNumber = "3001234567"),
        )
    }

    @Test
    fun showsNumber_whenSingleMissedCallHasNoNotification() {
        assertEquals(
            MissedCallsUpdate.ShowNumber("3001234567"),
            resolveMissedCallsUpdate(count = 1, hasAnyNotification = false, callLogNumber = "3001234567"),
        )
    }

    @Test
    fun showsCount_whenSeveralMissedCallsHaveNoNotification() {
        assertEquals(
            MissedCallsUpdate.ShowCount(3),
            resolveMissedCallsUpdate(count = 3, hasAnyNotification = false, callLogNumber = "3001234567"),
        )
    }

    @Test
    fun showsCount_whenCallLogNumberIsUnavailable() {
        assertEquals(
            MissedCallsUpdate.ShowCount(1),
            resolveMissedCallsUpdate(count = 1, hasAnyNotification = false, callLogNumber = ""),
        )
    }
}
