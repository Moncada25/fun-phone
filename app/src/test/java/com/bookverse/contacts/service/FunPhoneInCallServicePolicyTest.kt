package com.bookverse.contacts.service

import android.telecom.Call
import android.telecom.DisconnectCause
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

    @Test
    fun shouldMarkMissedCall_whenTelecomReportsMissed() {
        assertTrue(
            isMissedCall(
                isIncomingCall = true,
                previousState = Call.STATE_RINGING,
                connectTimeMillis = 0L,
                disconnectCode = DisconnectCause.MISSED,
            ),
        )
    }

    @Test
    fun shouldNotMarkMissedCall_whenUserRejectedRingingCall() {
        assertFalse(
            isMissedCall(
                isIncomingCall = true,
                previousState = Call.STATE_RINGING,
                connectTimeMillis = 0L,
                disconnectCode = DisconnectCause.REJECTED,
            ),
        )
    }

    @Test
    fun shouldNotMarkMissedCall_whenAnsweredOnAnotherDevice() {
        assertFalse(
            isMissedCall(
                isIncomingCall = true,
                previousState = Call.STATE_RINGING,
                connectTimeMillis = 0L,
                disconnectCode = DisconnectCause.ANSWERED_ELSEWHERE,
            ),
        )
    }

    @Test
    fun shouldMarkMissedCall_whenRemoteHangsUpWhileRinging() {
        assertTrue(
            isMissedCall(
                isIncomingCall = true,
                previousState = Call.STATE_RINGING,
                connectTimeMillis = 0L,
                disconnectCode = DisconnectCause.REMOTE,
            ),
        )
    }

    @Test
    fun shouldLaunchIncomingActivity_whenDeviceLocked() {
        assertTrue(
            shouldLaunchIncomingCallActivity(
                immersive = false,
                deviceLocked = true,
                appInForeground = false,
                notificationShown = true,
            ),
        )
    }

    @Test
    fun shouldRelyOnNotification_whenUnlockedInBackground() {
        assertFalse(
            shouldLaunchIncomingCallActivity(
                immersive = false,
                deviceLocked = false,
                appInForeground = false,
                notificationShown = true,
            ),
        )
    }

    @Test
    fun shouldLaunchIncomingActivity_whenNotificationUnavailableInBackground() {
        assertTrue(
            shouldLaunchIncomingCallActivity(
                immersive = false,
                deviceLocked = false,
                appInForeground = false,
                notificationShown = false,
            ),
        )
    }

    @Test
    fun shouldKeepInAppSurface_whenAppInForeground() {
        assertFalse(
            shouldLaunchIncomingCallActivity(
                immersive = false,
                deviceLocked = false,
                appInForeground = true,
                notificationShown = false,
            ),
        )
    }
}
