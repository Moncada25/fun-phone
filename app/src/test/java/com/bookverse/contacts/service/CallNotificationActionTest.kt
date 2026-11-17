package com.bookverse.contacts.service

import com.bookverse.contacts.domain.call.AudioRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallNotificationActionTest {
    @Test
    fun shouldToggleMuteInBothDirections() {
        assertTrue(toggledMuteState(isMuted = false))
        assertFalse(toggledMuteState(isMuted = true))
    }

    @Test
    fun shouldToggleBetweenSpeakerAndEarpiece() {
        assertEquals(AudioRoute.SPEAKER, toggledSpeakerRoute(isSpeakerOn = false))
        assertEquals(AudioRoute.EARPIECE, toggledSpeakerRoute(isSpeakerOn = true))
    }

    @Test
    fun shouldUpdateOngoingNotification_whenSameCallOwnsForegroundService() {
        assertTrue(shouldUpdateOngoingCallNotification("call-239", "call-239"))
    }

    @Test
    fun shouldSkipLateOngoingNotificationUpdate_whenForegroundServiceAlreadyStopped() {
        assertFalse(shouldUpdateOngoingCallNotification(null, "call-239"))
    }

    @Test
    fun shouldSkipOngoingNotificationUpdate_whenAnotherCallOwnsForegroundService() {
        assertFalse(shouldUpdateOngoingCallNotification("new-call", "call-239"))
    }

    @Test
    fun shouldDeferStop_whenForegroundStartIsPending() {
        val gate = OngoingCallStartGate()

        val startToken = gate.onStartRequested()

        assertFalse(gate.shouldStopImmediately())
        assertTrue(gate.onForegroundStarted(startToken))
    }

    @Test
    fun shouldStopImmediately_whenServiceAlreadyReachedForeground() {
        val gate = OngoingCallStartGate()
        val startToken = gate.onStartRequested()
        assertFalse(gate.onForegroundStarted(startToken))

        assertTrue(gate.shouldStopImmediately())
    }

    @Test
    fun shouldClearDeferredStop_whenNewStartSupersedesIt() {
        val gate = OngoingCallStartGate()
        gate.onStartRequested()
        assertFalse(gate.shouldStopImmediately())

        val newerStartToken = gate.onStartRequested()

        assertFalse(gate.onForegroundStarted(newerStartToken))
    }

    @Test
    fun shouldStopEveryQueuedStartRequested_beforeStop() {
        val gate = OngoingCallStartGate()
        val firstStartToken = gate.onStartRequested()
        val secondStartToken = gate.onStartRequested()
        assertFalse(gate.shouldStopImmediately())

        assertTrue(gate.onForegroundStarted(firstStartToken))
        assertTrue(gate.onForegroundStarted(secondStartToken))
    }
}
