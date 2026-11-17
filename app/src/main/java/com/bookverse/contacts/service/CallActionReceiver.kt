package com.bookverse.contacts.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.domain.call.AudioRoute

class CallActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ANSWER = "com.bookverse.contacts.action.ANSWER_CALL"
        const val ACTION_REJECT = "com.bookverse.contacts.action.REJECT_CALL"
        const val ACTION_END = "com.bookverse.contacts.action.END_CALL"
        const val ACTION_TOGGLE_MUTE = "com.bookverse.contacts.action.TOGGLE_MUTE"
        const val ACTION_TOGGLE_SPEAKER = "com.bookverse.contacts.action.TOGGLE_SPEAKER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val callId = intent.getStringExtra("call_id")
        val action = intent.action

        DiagnosticLog.debug(DiagnosticEvent.CALL_ACTION_RECEIVED)

        when (action) {
            ACTION_ANSWER -> {
                DiagnosticLog.debug(DiagnosticEvent.CALL_ANSWER_REQUESTED)
                callId?.let { CallController.markAnsweredFromNotification(it) }
                IncomingCallService.cancelNotification(context)
                IncomingCallService.stop(context)
                CallController.answer(callId)
            }
            ACTION_REJECT -> {
                DiagnosticLog.debug(DiagnosticEvent.CALL_REJECT_REQUESTED)
                CallController.reject(callId)
                IncomingCallService.stop(context)
                IncomingCallService.cancelNotification(context)
            }
            ACTION_END -> {
                DiagnosticLog.debug(DiagnosticEvent.CALL_END_REQUESTED)
                CallController.end(callId)
                CallNotificationManager.stopOngoing(context)
            }
            ACTION_TOGGLE_MUTE -> {
                DiagnosticLog.debug(DiagnosticEvent.CALL_MUTE_REQUESTED)
                val isMuted = intent.getBooleanExtra("is_muted", false)
                val newMuteState = toggledMuteState(isMuted)
                CallController.mute(newMuteState)
                DefaultCallRepository.setMuted(newMuteState)
                refreshOngoingNotification(context, callId, intent)
            }
            ACTION_TOGGLE_SPEAKER -> {
                DiagnosticLog.debug(DiagnosticEvent.CALL_SPEAKER_REQUESTED)
                val route = toggledSpeakerRoute(
                    isSpeakerOn = intent.getBooleanExtra("is_speaker_on", false),
                )
                CallController.setAudioRoute(route)
                DefaultCallRepository.setAudioRoute(route)
                refreshOngoingNotification(context, callId, intent)
            }
        }
    }

    private fun refreshOngoingNotification(
        context: Context,
        callId: String?,
        intent: Intent,
    ) {
        val number = intent.getStringExtra("number").orEmpty()
        if (callId != null && number.isNotEmpty()) {
            CallNotificationManager.showOngoing(context, callId, number)
        }
    }
}

internal fun toggledMuteState(isMuted: Boolean): Boolean = !isMuted

internal fun toggledSpeakerRoute(isSpeakerOn: Boolean): AudioRoute =
    if (isSpeakerOn) AudioRoute.EARPIECE else AudioRoute.SPEAKER
