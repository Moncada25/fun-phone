package com.bookverse.contacts.domain.call

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallConferencePolicyTest {
    private fun call(
        id: String,
        state: CallState,
        canMerge: Boolean = false,
        parentId: String? = null,
        isConference: Boolean = false,
    ) = UiCall(
        id = id,
        number = "redacted-$id",
        state = state,
        canMerge = canMerge,
        parentId = parentId,
        isConference = isConference,
    )

    @Test
    fun mergeNeedsAnActiveCallAHeldCallAndTelecomSupport() {
        val active = call("active", CallState.ACTIVE, canMerge = true)
        val held = call("held", CallState.ON_HOLD)

        assertTrue(CallConferencePolicy.canMerge(listOf(held, active)))
        assertFalse(CallConferencePolicy.canMerge(listOf(active)))
        assertFalse(CallConferencePolicy.canMerge(listOf(active, call("ringing", CallState.RINGING))))
        assertFalse(
            CallConferencePolicy.canMerge(
                listOf(active.copy(canMerge = false), held),
            ),
        )
    }

    @Test
    fun mergeCandidatesPreferTheActiveCall() {
        val active = call("active", CallState.ACTIVE, canMerge = true)
        val held = call("held", CallState.ON_HOLD, canMerge = true)

        assertEquals(
            listOf("active", "held"),
            CallConferencePolicy.mergeCandidates(listOf(held, active)).map(UiCall::id),
        )
        assertEquals(emptyList<UiCall>(), CallConferencePolicy.mergeCandidates(listOf(active)))
    }

    @Test
    fun conferenceBecomesTheForegroundCallAndHidesItsParticipants() {
        val first = call("first", CallState.ACTIVE, parentId = "conference")
        val second = call("second", CallState.ACTIVE, parentId = "conference")
        val conference = call("conference", CallState.ACTIVE, isConference = true)
        val calls = listOf(first, second, conference)

        assertEquals(conference, CallConferencePolicy.foregroundCall(calls))
        assertEquals(listOf(conference), CallConferencePolicy.topLevelCalls(calls))
        assertEquals(listOf(first, second), CallConferencePolicy.participants(conference, calls))
        assertFalse(CallConferencePolicy.canMerge(calls))
    }

    @Test
    fun participantsThatAlreadyLeftAreNotCounted() {
        val conference = call("conference", CallState.ACTIVE, isConference = true)
        val remaining = call("remaining", CallState.ACTIVE, parentId = "conference")
        val gone = call("gone", CallState.DISCONNECTED, parentId = "conference")

        assertEquals(
            listOf(remaining),
            CallConferencePolicy.participants(conference, listOf(remaining, gone, conference)),
        )
    }

    @Test
    fun foregroundCallKeepsInsertionOrderForOrdinaryCalls() {
        val held = call("held", CallState.ON_HOLD)
        val active = call("active", CallState.ACTIVE)

        assertEquals(held, CallConferencePolicy.foregroundCall(listOf(held, active)))
        assertNull(CallConferencePolicy.foregroundCall(listOf(call("ended", CallState.DISCONNECTED))))
    }

    @Test
    fun controlsTargetTheConferenceInsteadOfASingleParticipant() {
        val conference = call("conference", CallState.ACTIVE, isConference = true)
        val ordinary = call("ordinary", CallState.ACTIVE)

        assertEquals("conference", CallConferencePolicy.controlTargetId("first", conference))
        assertEquals("first", CallConferencePolicy.controlTargetId("first", ordinary))
        assertNull(CallConferencePolicy.controlTargetId(null, null))
    }
}
