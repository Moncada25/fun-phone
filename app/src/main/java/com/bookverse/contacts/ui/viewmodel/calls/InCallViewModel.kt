package com.bookverse.contacts.ui.viewmodel.calls

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.domain.call.AudioEndpoint
import com.bookverse.contacts.domain.call.AudioRoute
import com.bookverse.contacts.domain.call.CallConferencePolicy
import com.bookverse.contacts.domain.call.CallState
import com.bookverse.contacts.domain.call.UiCall
import com.bookverse.contacts.service.CallController
import com.bookverse.contacts.service.CallRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

data class InCallUiState(
    val callEnded: Boolean = false,
    val isAudioToggling: Boolean = false
)

class InCallViewModel(
    private val callId: String?,
    private val callRepository: CallRepository,
) : ViewModel() {
    private val _activeCall = MutableStateFlow<UiCall?>(null)
    val activeCall: StateFlow<UiCall?> = _activeCall.asStateFlow()

    private val _durationSec = MutableStateFlow(0)
    val durationSec: StateFlow<Int> = _durationSec.asStateFlow()

    private val _uiState = MutableStateFlow(InCallUiState())
    val uiState: StateFlow<InCallUiState> = _uiState.asStateFlow()

    private val _allCalls = MutableStateFlow<List<UiCall>>(emptyList())
    val allCalls: StateFlow<List<UiCall>> = _allCalls.asStateFlow()

    private var timerJob: Job? = null
    private var hadCallBefore = false
    private val audioToggleMutex = Mutex()
    private val _isAudioToggling = MutableStateFlow(false)
    val isAudioToggling: StateFlow<Boolean> = _isAudioToggling.asStateFlow()

    init {
        viewModelScope.launch {
            callRepository.activeCall.collect { currentCall ->
                DiagnosticLog.debug(DiagnosticEvent.CALL_STATE_UPDATED)
                _activeCall.value = currentCall
                handleCallUpdate(currentCall)
            }
        }
        viewModelScope.launch {
            callRepository.calls.collect { calls ->
                _allCalls.value = calls
            }
        }
    }

    private fun handleCallUpdate(currentCall: UiCall?) {
        val callPresentInRepo = if (callId != null) {
            callRepository.calls.value.any { it.id == callId }
        } else {
            currentCall != null
        }
        // Only track the call this screen was opened for: emissions for other ids
        // (multi-call hold/swap, transient repository states while reopening from
        // the banner) must not mark this screen as seen or ended.
        val isTargetCall = currentCall != null &&
            currentCall.state != CallState.DISCONNECTED &&
            (callId == null || currentCall.id == callId)
        val ended = when {
            !callPresentInRepo && hadCallBefore -> true
            currentCall?.state == CallState.DISCONNECTED &&
                (callId == null || currentCall.id == callId) -> true
            else -> false
        }

        if (isTargetCall) {
            hadCallBefore = true
        } else if (ended) {
            hadCallBefore = false
            timerJob?.cancel()
            timerJob = null
            _durationSec.value = 0
            _uiState.value = _uiState.value.copy(callEnded = true)
            return
        }

        _uiState.value = _uiState.value.copy(callEnded = ended)
        startOrRestartTimer(currentCall)
    }

    private fun startOrRestartTimer(currentCall: UiCall?) {
        if (currentCall?.state != CallState.ACTIVE) {
            timerJob?.cancel()
            timerJob = null
            _durationSec.value = 0
            return
        }
        val realConnect = currentCall.connectTimeMillis?.takeIf { it > 0 }
        if (timerJob?.isActive == true) {
            if (realConnect != null) {
                val anchoredSeconds = ((System.currentTimeMillis() - realConnect) / 1000L).toInt()
                if (anchoredSeconds > _durationSec.value) {
                    _durationSec.value = anchoredSeconds
                }
            }
            return
        }
        val baseTime = realConnect ?: System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val call = _activeCall.value
                if (call?.state != CallState.ACTIVE) break
                val seconds = ((System.currentTimeMillis() - baseTime) / 1000L).toInt()
                _durationSec.value = seconds
                delay(1000.milliseconds)
            }
        }
    }

    private fun controlTargetId(): String? =
        CallConferencePolicy.controlTargetId(callId, _activeCall.value)

    fun end() {
        CallController.end(controlTargetId())
        _uiState.value = _uiState.value.copy(callEnded = true)
    }

    fun toggleMute() {
        viewModelScope.launch {
            audioToggleMutex.withLock {
                _isAudioToggling.value = true
                try {
                    val newMuted = !(_activeCall.value?.isMuted ?: false)
                    CallController.mute(newMuted)
                } finally {
                    _isAudioToggling.value = false
                }
            }
        }
    }

    private val _showAudioRouteDialog = MutableStateFlow(false)
    val showAudioRouteDialog: StateFlow<Boolean> = _showAudioRouteDialog.asStateFlow()

    fun showAudioRouteDialog() {
        _showAudioRouteDialog.value = true
    }

    fun hideAudioRouteDialog() {
        _showAudioRouteDialog.value = false
    }

    fun toggleSpeaker(context: Context) {
        viewModelScope.launch {
            audioToggleMutex.withLock {
                _isAudioToggling.value = true
                try {
                    val endpoints = CallController.getAvailableAudioEndpoints()
                    val hasBluetooth = endpoints.any { it.route == AudioRoute.BLUETOOTH }
                    val hasWired = endpoints.any { it.route == AudioRoute.WIRED }
                    val multiple = endpoints.size > 2
                    var potentialExtra = false
                    if (!hasBluetooth && !hasWired && !multiple) {
                        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                        val outs = am?.getDevices(AudioManager.GET_DEVICES_OUTPUTS).orEmpty()
                        potentialExtra = outs.any { d ->
                            when (d.type) {
                                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                                AudioDeviceInfo.TYPE_USB_HEADSET -> true
                                else -> false
                            }
                        }
                    }
                    if (hasBluetooth || hasWired || multiple || potentialExtra) {
                        _showAudioRouteDialog.value = true
                    } else {
                        val currentRoute = _activeCall.value?.audioRoute ?: AudioRoute.EARPIECE
                        val newRoute = when (currentRoute) {
                            AudioRoute.EARPIECE -> AudioRoute.SPEAKER
                            AudioRoute.SPEAKER -> AudioRoute.EARPIECE
                            else -> AudioRoute.EARPIECE
                        }
                        CallController.setAudioRoute(newRoute)
                    }
                } finally {
                    _isAudioToggling.value = false
                }
            }
        }
    }

    fun setAudioEndpoint(endpoint: AudioEndpoint) {
        CallController.setAudioEndpoint(endpoint)
        _showAudioRouteDialog.value = false
    }

    fun getAvailableAudioEndpoints(): List<AudioEndpoint> {
        return CallController.getAvailableAudioEndpoints()
    }

    fun toggleHold() {
        val onHold = _activeCall.value?.isOnHold == true
        val targetId = controlTargetId()
        if (onHold) CallController.unhold(targetId) else CallController.hold(targetId)
    }

    fun startDtmf(digit: Char) {
        CallController.startDtmf(controlTargetId(), digit)
    }

    fun stopDtmf() {
        CallController.stopDtmf(controlTargetId())
    }

    fun swap() {
        CallController.swapCalls()
    }

    fun merge() {
        CallController.mergeCalls()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        fun provideFactory(
            callId: String?,
            callRepository: CallRepository,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return InCallViewModel(callId, callRepository) as T
                }
            }
        }
    }
}
