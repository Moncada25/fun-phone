package com.bookverse.contacts.domain.call

object CallConferencePolicy {
    private val liveStates = setOf(
        CallState.ACTIVE,
        CallState.DIALING,
        CallState.RINGING,
        CallState.ON_HOLD,
    )

    fun topLevelCalls(calls: List<UiCall>): List<UiCall> = calls.filter { it.parentId == null }

    fun foregroundCall(calls: List<UiCall>): UiCall? =
        topLevelCalls(calls).firstOrNull { it.state in liveStates }

    fun canMerge(calls: List<UiCall>): Boolean {
        val topLevel = topLevelCalls(calls)
        val active = topLevel.filter { it.state == CallState.ACTIVE }
        val held = topLevel.filter { it.state == CallState.ON_HOLD }
        return active.isNotEmpty() &&
            held.isNotEmpty() &&
            (active + held).any { it.canMerge }
    }

    fun mergeCandidates(calls: List<UiCall>): List<UiCall> {
        if (!canMerge(calls)) return emptyList()
        val topLevel = topLevelCalls(calls)
        return topLevel.filter { it.state == CallState.ACTIVE && it.canMerge } +
            topLevel.filter { it.state == CallState.ON_HOLD && it.canMerge }
    }

    fun participants(conference: UiCall, calls: List<UiCall>): List<UiCall> =
        calls.filter { it.parentId == conference.id && it.state != CallState.DISCONNECTED }

    fun controlTargetId(screenCallId: String?, foreground: UiCall?): String? =
        if (foreground?.isConference == true) foreground.id else screenCallId
}
