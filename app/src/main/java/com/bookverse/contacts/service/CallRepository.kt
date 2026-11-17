package com.bookverse.contacts.service

import android.telecom.Call
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.domain.call.AudioEndpoint
import com.bookverse.contacts.domain.call.AudioRoute
import com.bookverse.contacts.domain.call.CallConferencePolicy
import com.bookverse.contacts.domain.call.CallState
import com.bookverse.contacts.domain.call.UiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface CallRepository {
    val calls: StateFlow<List<UiCall>>
    val activeCall: StateFlow<UiCall?>
    fun upsert(callId: String, uiCall: UiCall, platformCall: Call? = null)
    fun remove(callId: String)
    fun getPlatformCall(callId: String): Call?
    fun setMuted(muted: Boolean)
    fun setAudioRoute(route: AudioRoute)
    fun setAudioEndpoint(endpoint: AudioEndpoint)
}

object DefaultCallRepository : CallRepository {
    private val _calls = MutableStateFlow<List<UiCall>>(emptyList())
    private val _activeCall = MutableStateFlow<UiCall?>(null)
    private val platformCalls = mutableMapOf<String, Call>()

    override val calls: StateFlow<List<UiCall>> = _calls.asStateFlow()
    override val activeCall: StateFlow<UiCall?> = _activeCall.asStateFlow()

    override fun upsert(callId: String, uiCall: UiCall, platformCall: Call?) {
        DiagnosticLog.debug(DiagnosticEvent.CALL_REPOSITORY_UPDATED)
        if (platformCall != null) platformCalls[callId] = platformCall

        val updated = _calls.value.toMutableList().apply {
            val idx = indexOfFirst { it.id == callId }
            if (idx >= 0) {
                val existing = this[idx]
                val mergedAudio = if (uiCall.audioEndpoint == null && existing.audioEndpoint != null && uiCall.audioRoute == existing.audioRoute) {
                    uiCall.copy(audioEndpoint = existing.audioEndpoint)
                } else uiCall
                val merged = mergedAudio.copy(
                    displayName = mergedAudio.displayName ?: existing.displayName,
                    photoUri = mergedAudio.photoUri ?: existing.photoUri,
                    avatarBytes = mergedAudio.avatarBytes ?: existing.avatarBytes
                )
                set(idx, merged)
            } else add(uiCall)
        }
        _calls.value = updated

        _activeCall.value = CallConferencePolicy.foregroundCall(updated)
        DiagnosticLog.debug(DiagnosticEvent.CALL_STATE_UPDATED)
    }

    override fun remove(callId: String) {
        DiagnosticLog.debug(DiagnosticEvent.CALL_REPOSITORY_REMOVED)
        platformCalls.remove(callId)
        val updated = _calls.value.filterNot { it.id == callId }
        _calls.value = updated

        _activeCall.value = CallConferencePolicy.foregroundCall(updated)
        DiagnosticLog.debug(DiagnosticEvent.CALL_STATE_UPDATED)
    }

    override fun getPlatformCall(callId: String): Call? {
        return platformCalls[callId]
    }

    override fun setMuted(muted: Boolean) {
        val currentCalls = _calls.value.toMutableList()
        var updated = false
        for (i in currentCalls.indices) {
            if (currentCalls[i].state == CallState.ACTIVE || currentCalls[i].state == CallState.ON_HOLD) {
                currentCalls[i] = currentCalls[i].copy(isMuted = muted)
                updated = true
            }
        }
        if (updated) {
            _calls.value = currentCalls
            _activeCall.value = CallConferencePolicy.foregroundCall(currentCalls)
        }
        DiagnosticLog.debug(DiagnosticEvent.CALL_REPOSITORY_AUDIO_UPDATED)
    }

    override fun setAudioRoute(route: AudioRoute) {
        val list = _calls.value.toMutableList()
        var changed = false
        for (i in list.indices) if (list[i].state == CallState.ACTIVE || list[i].state == CallState.ON_HOLD || list[i].state == CallState.DIALING || list[i].state == CallState.RINGING) { list[i] = list[i].copy(audioRoute = route, audioEndpoint = if (list[i].audioEndpoint?.route == route) list[i].audioEndpoint else null); changed = true }
        if (changed) {
            _calls.value = list
            _activeCall.value = CallConferencePolicy.foregroundCall(list)
        }
    }

    override fun setAudioEndpoint(endpoint: AudioEndpoint) {
        val list = _calls.value.toMutableList()
        var changed = false
        for (i in list.indices) if (list[i].state == CallState.ACTIVE || list[i].state == CallState.DIALING || list[i].state == CallState.RINGING || list[i].state == CallState.ON_HOLD) { list[i] = list[i].copy(audioRoute = endpoint.route, audioEndpoint = endpoint); changed = true }
        if (changed) {
            _calls.value = list
            _activeCall.value = CallConferencePolicy.foregroundCall(list)
        }
    }

    fun getAllCalls(): List<UiCall> {
        return _calls.value
    }
}
