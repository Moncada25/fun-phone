package com.bookverse.contacts

import com.bookverse.contacts.domain.call.CallState
import com.bookverse.contacts.domain.call.UiCall
import org.junit.Assert.assertEquals
import org.junit.Test

class CallRestoreTargetTest {
    @Test
    fun shouldRestoreIncomingSurfaceForRingingCall() {
        assertEquals(CallRestoreTarget.INCOMING, resolveCallRestoreTarget(call(CallState.RINGING)))
    }

    @Test
    fun shouldRestoreInCallSurfaceForEveryOngoingState() {
        listOf(CallState.DIALING, CallState.ACTIVE, CallState.ON_HOLD).forEach { state ->
            assertEquals(CallRestoreTarget.IN_CALL, resolveCallRestoreTarget(call(state)))
        }
    }

    @Test
    fun shouldNotRestoreSurfaceWithoutAnActiveCall() {
        assertEquals(CallRestoreTarget.NONE, resolveCallRestoreTarget(null))
        assertEquals(CallRestoreTarget.NONE, resolveCallRestoreTarget(call(CallState.DISCONNECTED)))
    }

    private fun call(state: CallState) = UiCall(
        id = "call-239",
        number = "+573001112233",
        state = state,
    )
}
