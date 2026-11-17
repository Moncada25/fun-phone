package com.bookverse.contacts.service

import android.telecom.Call
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FunPhoneInCallServicePolicyTest {
    @Test
    fun shouldMarkMissedCall_whenTrackedIncomingNeverConnected() {
        assertTrue(
            isMissedCall(
                isIncomingCall = true,
                previousState = Call.STATE_RINGING,
                connectTimeMillis = 0L,
            ),
        )
    }

    @Test
    fun shouldNotMarkMissedCall_whenIncomingCallConnected() {
        assertFalse(
            isMissedCall(
                isIncomingCall = true,
                previousState = Call.STATE_ACTIVE,
                connectTimeMillis = 1_000L,
            ),
        )
    }

    @Test
    fun shouldNotMarkMissedCall_whenOutgoingCallDisconnects() {
        assertFalse(
            isMissedCall(
                isIncomingCall = false,
                previousState = Call.STATE_CONNECTING,
                connectTimeMillis = 0L,
            ),
        )
    }
}
