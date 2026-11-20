package com.bookverse.contacts

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallExitPolicyTest {
    @Test
    fun callOpenedWhileAppInBackground_isFromOutside() {
        assertTrue(isCallOpenedFromOutside(openInCall = true, appWasVisible = false))
    }

    @Test
    fun callOpenedWhileAppVisible_isNotFromOutside() {
        assertFalse(isCallOpenedFromOutside(openInCall = true, appWasVisible = true))
    }

    @Test
    fun intentWithoutInCallRequest_isNotFromOutside() {
        assertFalse(isCallOpenedFromOutside(openInCall = false, appWasVisible = false))
    }

    @Test
    fun shouldLeave_whenOutsideCallEndsWhileVisible() {
        assertTrue(shouldLeaveAfterCallEnds(leaveAfterCall = true, hasOngoingCall = false, activityStarted = true))
    }

    @Test
    fun shouldStay_whileAnotherCallIsOngoing() {
        assertFalse(shouldLeaveAfterCallEnds(leaveAfterCall = true, hasOngoingCall = true, activityStarted = true))
    }

    @Test
    fun shouldStay_whenCallEndsWhileAppIsInBackground() {
        assertFalse(shouldLeaveAfterCallEnds(leaveAfterCall = true, hasOngoingCall = false, activityStarted = false))
    }

    @Test
    fun shouldStay_whenCallStartedInsideTheApp() {
        assertFalse(shouldLeaveAfterCallEnds(leaveAfterCall = false, hasOngoingCall = false, activityStarted = true))
    }
}
