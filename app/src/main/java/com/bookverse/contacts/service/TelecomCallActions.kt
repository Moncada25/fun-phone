package com.bookverse.contacts.service

import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.VideoProfile
import androidx.annotation.RequiresApi
import com.bookverse.contacts.domain.call.AudioRoute
import com.bookverse.contacts.domain.call.CallConferencePolicy
import com.bookverse.contacts.domain.call.CallState
import com.bookverse.contacts.domain.call.RejectMessagePolicy
import com.bookverse.contacts.domain.call.UiCall

internal interface TelecomCallActions {
    fun answerAudioOnly()
    fun rejectDeclined()
    fun rejectWithMessage(message: String)
    fun disconnect()
    fun hold()
    fun unhold()
    fun playDtmf(digit: Char)
    fun stopDtmf()
    fun canMergeConference(): Boolean
    fun mergeConference()
    fun conferenceWithFirstAvailable(): Boolean
}

internal class AndroidTelecomCallActions(
    private val call: Call,
) : TelecomCallActions {
    override fun answerAudioOnly() {
        call.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun rejectDeclined() {
        call.reject(Call.REJECT_REASON_DECLINED)
    }

    override fun rejectWithMessage(message: String) {
        call.reject(true, message)
    }

    override fun disconnect() {
        call.disconnect()
    }

    override fun hold() {
        call.hold()
    }

    override fun unhold() {
        call.unhold()
    }

    override fun playDtmf(digit: Char) {
        call.playDtmfTone(digit)
    }

    override fun stopDtmf() {
        call.stopDtmfTone()
    }

    override fun canMergeConference(): Boolean =
        call.details.can(Call.Details.CAPABILITY_MERGE_CONFERENCE)

    override fun mergeConference() {
        call.mergeConference()
    }

    override fun conferenceWithFirstAvailable(): Boolean {
        val other = call.conferenceableCalls.firstOrNull() ?: return false
        call.conference(other)
        return true
    }
}

internal fun answerTelecomCall(actions: TelecomCallActions?) {
    actions?.answerAudioOnly()
}

internal fun rejectTelecomCall(
    actions: TelecomCallActions?,
    supportsDeclinedReason: Boolean,
) {
    if (supportsDeclinedReason) {
        actions?.rejectDeclined()
    } else {
        actions?.disconnect()
    }
}

internal fun rejectTelecomCallWithMessage(
    actions: TelecomCallActions?,
    message: String?,
    callSupportsTextResponse: Boolean,
    supportsDeclinedReason: Boolean,
): Boolean {
    val sanitized = RejectMessagePolicy.sanitize(message)
    if (actions == null) return false
    if (sanitized == null || !callSupportsTextResponse) {
        rejectTelecomCall(actions, supportsDeclinedReason)
        return false
    }
    actions.rejectWithMessage(sanitized)
    return true
}

internal fun mergeTelecomCalls(
    calls: List<UiCall>,
    actionsForId: (String) -> TelecomCallActions?,
): Boolean {
    CallConferencePolicy.mergeCandidates(calls).forEach { candidate ->
        val actions = actionsForId(candidate.id) ?: return@forEach
        if (actions.canMergeConference()) {
            actions.mergeConference()
            return true
        }
        if (actions.conferenceWithFirstAvailable()) return true
    }
    return false
}

internal fun endTelecomCall(actions: TelecomCallActions?) {
    actions?.disconnect()
}

internal fun holdTelecomCall(actions: TelecomCallActions?) {
    actions?.hold()
}

internal fun unholdTelecomCall(actions: TelecomCallActions?) {
    actions?.unhold()
}

internal fun startTelecomDtmf(actions: TelecomCallActions?, digit: Char) {
    actions?.playDtmf(digit)
}

internal fun stopTelecomDtmf(actions: TelecomCallActions?) {
    actions?.stopDtmf()
}

internal fun swapTelecomCalls(
    calls: List<UiCall>,
    actionsForId: (String) -> TelecomCallActions?,
) {
    if (calls.size < 2) return
    calls.filter { it.state == CallState.ACTIVE }
        .forEach { call -> actionsForId(call.id)?.hold() }
    calls.filter { it.state == CallState.ON_HOLD }
        .forEach { call -> actionsForId(call.id)?.unhold() }
}

internal fun platformAudioRoute(route: AudioRoute): Int = when (route) {
    AudioRoute.SPEAKER -> CallAudioState.ROUTE_SPEAKER
    AudioRoute.BLUETOOTH -> CallAudioState.ROUTE_BLUETOOTH
    AudioRoute.WIRED -> CallAudioState.ROUTE_WIRED_HEADSET
    AudioRoute.EARPIECE -> CallAudioState.ROUTE_EARPIECE
}

internal fun audioRouteFromPlatform(route: Int?): AudioRoute = when (route) {
    CallAudioState.ROUTE_SPEAKER -> AudioRoute.SPEAKER
    CallAudioState.ROUTE_BLUETOOTH -> AudioRoute.BLUETOOTH
    CallAudioState.ROUTE_WIRED_HEADSET -> AudioRoute.WIRED
    else -> AudioRoute.EARPIECE
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal fun platformEndpointType(route: AudioRoute): Int = when (route) {
    AudioRoute.SPEAKER -> CallEndpoint.TYPE_SPEAKER
    AudioRoute.BLUETOOTH -> CallEndpoint.TYPE_BLUETOOTH
    AudioRoute.WIRED -> CallEndpoint.TYPE_WIRED_HEADSET
    AudioRoute.EARPIECE -> CallEndpoint.TYPE_EARPIECE
}
