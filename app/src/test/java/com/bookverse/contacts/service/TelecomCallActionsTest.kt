package com.bookverse.contacts.service

import android.annotation.SuppressLint
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import com.bookverse.contacts.domain.call.AudioRoute
import com.bookverse.contacts.domain.call.CallState
import com.bookverse.contacts.domain.call.UiCall
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelecomCallActionsTest {
    @Test
    fun answerUsesAudioOnlyPlatformAction() {
        val actions = RecordingCallActions()

        answerTelecomCall(actions)

        assertEquals(listOf("answerAudioOnly"), actions.events)
    }

    @Test
    fun rejectUsesDeclinedReasonOnAndroid11AndNewer() {
        val actions = RecordingCallActions()

        rejectTelecomCall(actions, supportsDeclinedReason = true)

        assertEquals(listOf("rejectDeclined"), actions.events)
    }

    @Test
    fun rejectFallsBackToDisconnectOnLegacyAndroid() {
        val actions = RecordingCallActions()

        rejectTelecomCall(actions, supportsDeclinedReason = false)

        assertEquals(listOf("disconnect"), actions.events)
    }

    @Test
    fun endHoldAndUnholdUseMatchingPlatformActions() {
        val actions = RecordingCallActions()

        endTelecomCall(actions)
        holdTelecomCall(actions)
        unholdTelecomCall(actions)

        assertEquals(listOf("disconnect", "hold", "unhold"), actions.events)
    }

    @Test
    fun dtmfStartsAndStopsThroughMatchingActions() {
        val actions = RecordingCallActions()

        startTelecomDtmf(actions, '#')
        stopTelecomDtmf(actions)

        assertEquals(listOf("play:#", "stop"), actions.events)
    }

    @Test
    fun swapHoldsActiveAndUnholdsHeldCallsOnly() {
        val activeActions = RecordingCallActions()
        val heldActions = RecordingCallActions()
        val ringingActions = RecordingCallActions()
        val actions = mapOf(
            "active" to activeActions,
            "held" to heldActions,
            "ringing" to ringingActions,
        )

        swapTelecomCalls(
            calls = listOf(
                call("active", CallState.ACTIVE),
                call("held", CallState.ON_HOLD),
                call("ringing", CallState.RINGING),
            ),
            actionsForId = actions::get,
        )

        assertEquals(listOf("hold"), activeActions.events)
        assertEquals(listOf("unhold"), heldActions.events)
        assertEquals(emptyList<String>(), ringingActions.events)
    }

    @Test
    fun swapDoesNothingWithFewerThanTwoCalls() {
        val actions = RecordingCallActions()

        swapTelecomCalls(
            calls = listOf(call("active", CallState.ACTIVE)),
            actionsForId = { actions },
        )

        assertEquals(emptyList<String>(), actions.events)
    }

    @Test
    @SuppressLint("NewApi")
    fun audioRoutesMapToLegacyAndEndpointApis() {
        val expectedLegacy = mapOf(
            AudioRoute.EARPIECE to CallAudioState.ROUTE_EARPIECE,
            AudioRoute.SPEAKER to CallAudioState.ROUTE_SPEAKER,
            AudioRoute.BLUETOOTH to CallAudioState.ROUTE_BLUETOOTH,
            AudioRoute.WIRED to CallAudioState.ROUTE_WIRED_HEADSET,
        )
        val expectedEndpoints = mapOf(
            AudioRoute.EARPIECE to CallEndpoint.TYPE_EARPIECE,
            AudioRoute.SPEAKER to CallEndpoint.TYPE_SPEAKER,
            AudioRoute.BLUETOOTH to CallEndpoint.TYPE_BLUETOOTH,
            AudioRoute.WIRED to CallEndpoint.TYPE_WIRED_HEADSET,
        )

        expectedLegacy.forEach { (route, platformRoute) ->
            assertEquals(platformRoute, platformAudioRoute(route))
            assertEquals(route, audioRouteFromPlatform(platformRoute))
        }
        expectedEndpoints.forEach { (route, endpointType) ->
            assertEquals(endpointType, platformEndpointType(route))
        }
        assertEquals(AudioRoute.EARPIECE, audioRouteFromPlatform(null))
    }

    @Test
    fun rejectWithMessageSendsTheSanitizedText() {
        val actions = RecordingCallActions()

        val sent = rejectTelecomCallWithMessage(
            actions = actions,
            message = "  Call you   later ",
            callSupportsTextResponse = true,
            supportsDeclinedReason = true,
        )

        assertTrue(sent)
        assertEquals(listOf("rejectWithMessage:Call you later"), actions.events)
    }

    @Test
    fun rejectWithMessageFallsBackToAPlainRejectWhenTextCannotBeSent() {
        val unsupported = RecordingCallActions()
        val blank = RecordingCallActions()

        val unsupportedSent = rejectTelecomCallWithMessage(
            actions = unsupported,
            message = "Call you later",
            callSupportsTextResponse = false,
            supportsDeclinedReason = true,
        )
        val blankSent = rejectTelecomCallWithMessage(
            actions = blank,
            message = "   ",
            callSupportsTextResponse = true,
            supportsDeclinedReason = false,
        )

        assertFalse(unsupportedSent)
        assertFalse(blankSent)
        assertEquals(listOf("rejectDeclined"), unsupported.events)
        assertEquals(listOf("disconnect"), blank.events)
        assertFalse(rejectTelecomCallWithMessage(null, "text", true, true))
    }

    @Test
    fun mergeUsesTheNativeMergeCapabilityWhenTelecomOffersIt() {
        val active = RecordingCallActions(supportsMergeConference = true)
        val held = RecordingCallActions(hasConferenceableCall = true)

        val merged = mergeTelecomCalls(
            calls = listOf(
                call("held", CallState.ON_HOLD, canMerge = true),
                call("active", CallState.ACTIVE, canMerge = true),
            ),
            actionsForId = mapOf("active" to active, "held" to held)::get,
        )

        assertTrue(merged)
        assertEquals(listOf("mergeConference"), active.events)
        assertEquals(emptyList<String>(), held.events)
    }

    @Test
    fun mergeConferencesWithTheOtherCallWhenThereIsNoNativeMerge() {
        val active = RecordingCallActions(hasConferenceableCall = true)

        val merged = mergeTelecomCalls(
            calls = listOf(
                call("active", CallState.ACTIVE, canMerge = true),
                call("held", CallState.ON_HOLD),
            ),
            actionsForId = mapOf("active" to active)::get,
        )

        assertTrue(merged)
        assertEquals(listOf("conference"), active.events)
    }

    @Test
    fun mergeDoesNothingWhenCallsCannotBeConferenced() {
        val active = RecordingCallActions(hasConferenceableCall = true)

        val merged = mergeTelecomCalls(
            calls = listOf(
                call("active", CallState.ACTIVE),
                call("held", CallState.ON_HOLD),
            ),
            actionsForId = { active },
        )

        assertFalse(merged)
        assertEquals(emptyList<String>(), active.events)
    }

    private fun call(id: String, state: CallState, canMerge: Boolean = false) = UiCall(
        id = id,
        number = "redacted",
        state = state,
        canMerge = canMerge,
    )

    private class RecordingCallActions(
        val supportsMergeConference: Boolean = false,
        val hasConferenceableCall: Boolean = false,
    ) : TelecomCallActions {
        val events = mutableListOf<String>()

        override fun answerAudioOnly() {
            events += "answerAudioOnly"
        }

        override fun rejectDeclined() {
            events += "rejectDeclined"
        }

        override fun rejectWithMessage(message: String) {
            events += "rejectWithMessage:$message"
        }

        override fun disconnect() {
            events += "disconnect"
        }

        override fun hold() {
            events += "hold"
        }

        override fun unhold() {
            events += "unhold"
        }

        override fun playDtmf(digit: Char) {
            events += "play:$digit"
        }

        override fun stopDtmf() {
            events += "stop"
        }

        override fun canMergeConference(): Boolean = supportsMergeConference

        override fun mergeConference() {
            events += "mergeConference"
        }

        override fun conferenceWithFirstAvailable(): Boolean {
            if (hasConferenceableCall) events += "conference"
            return hasConferenceableCall
        }
    }
}
