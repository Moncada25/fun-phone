package com.bookverse.contacts.service

import com.bookverse.contacts.domain.call.AudioRoute
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AudioRouteRequestPolicyTest {

    @Before
    fun setUp() {
        CallController.onNewCallStarted()
        CallController.resetLastReportedMutedForTest()
    }

    @After
    fun tearDown() {
        CallController.onNewCallStarted()
        CallController.resetLastReportedMutedForTest()
    }

    @Test
    fun withoutRequest_everySystemRouteIsFollowed() {
        AudioRoute.entries.forEach { reported ->
            assertEquals(
                AudioRouteReaction.FOLLOW_SYSTEM,
                reactToReportedAudioRoute(requested = null, reported = reported),
            )
        }
    }

    @Test
    fun matchingReport_settlesRequest() {
        AudioRoute.entries.forEach { route ->
            assertEquals(
                AudioRouteReaction.SETTLE,
                reactToReportedAudioRoute(requested = route, reported = route),
            )
        }
    }

    @Test
    fun mismatchingReport_retriesRequest() {
        assertEquals(
            AudioRouteReaction.RETRY_ONCE,
            reactToReportedAudioRoute(requested = AudioRoute.SPEAKER, reported = AudioRoute.EARPIECE),
        )
    }

    @Test
    fun newCall_hasNoRequestedRoute() {
        assertNull(CallController.getRequestedAudioRouteForTest())
    }

    @Test
    fun carBluetoothRoute_isNotOverriddenWithoutUserRequest() {
        CallController.onSystemAudioStateChanged(AudioRoute.BLUETOOTH, isMuted = false)
        CallController.onSystemAudioStateChanged(AudioRoute.EARPIECE, isMuted = false)
        CallController.onSystemAudioStateChanged(AudioRoute.BLUETOOTH, isMuted = false)

        assertNull(CallController.getRequestedAudioRouteForTest())
    }

    @Test
    fun confirmedUserRoute_yieldsToLaterSystemChanges() {
        CallController.setAudioRoute(AudioRoute.SPEAKER)
        assertEquals(AudioRoute.SPEAKER, CallController.getRequestedAudioRouteForTest())

        CallController.onSystemAudioStateChanged(AudioRoute.SPEAKER, isMuted = false)
        assertNull(CallController.getRequestedAudioRouteForTest())

        CallController.onSystemAudioStateChanged(AudioRoute.BLUETOOTH, isMuted = false)
        assertNull(CallController.getRequestedAudioRouteForTest())
    }

    @Test
    fun unconfirmedUserRoute_isRetriedOnlyOnce() {
        CallController.setAudioRoute(AudioRoute.SPEAKER)

        CallController.onSystemAudioStateChanged(AudioRoute.BLUETOOTH, isMuted = false)

        assertNull(CallController.getRequestedAudioRouteForTest())
    }

    @Test
    fun muteChange_keepsPendingUserRoute() {
        CallController.setAudioRoute(AudioRoute.SPEAKER)

        CallController.onSystemAudioStateChanged(AudioRoute.EARPIECE, isMuted = true)

        assertEquals(AudioRoute.SPEAKER, CallController.getRequestedAudioRouteForTest())
    }

    @Test
    fun newCall_clearsPendingUserRoute() {
        CallController.setAudioRoute(AudioRoute.SPEAKER)

        CallController.onNewCallStarted()

        assertNull(CallController.getRequestedAudioRouteForTest())
    }
}
