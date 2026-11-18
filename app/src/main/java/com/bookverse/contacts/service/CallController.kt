package com.bookverse.contacts.service

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.InCallService
import androidx.core.content.ContextCompat
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.domain.call.AudioEndpoint
import com.bookverse.contacts.domain.call.AudioRoute
import java.lang.ref.WeakReference

object CallController {
    private var serviceRef: WeakReference<InCallService>? = null
    private val cachedBluetoothNames = mutableSetOf<String>()
    private var preferredRoute: AudioRoute = AudioRoute.EARPIECE
    private var preferredEndpoint: AudioEndpoint? = null
    private var lastReportedMuted: Boolean = false

    fun attach(svc: InCallService) {
        serviceRef = WeakReference(svc)
        DiagnosticLog.debug(DiagnosticEvent.CALL_SERVICE_ATTACHED)
        resetAudioPreferences()
    }

    fun detach() {
        DiagnosticLog.debug(DiagnosticEvent.CALL_SERVICE_DETACHED)
        serviceRef?.clear()
        serviceRef = null
        cachedBluetoothNames.clear()
        resetAudioPreferences()
    }

    private fun getCallById(callId: String?): Call? {
        val id = callId ?: DefaultCallRepository.activeCall.value?.id ?: return null
        DefaultCallRepository.getPlatformCall(id)?.let { return it }
        val svc = serviceRef?.get() ?: return null
        @Suppress("DEPRECATION")
        val activeCalls = svc.calls.orEmpty()
        return activeCalls.firstOrNull { System.identityHashCode(it).toString() == id }
    }

    private fun getCallActionsById(callId: String?): TelecomCallActions? =
        getCallById(callId)?.let(::AndroidTelecomCallActions)

    private fun stopRingingFor(callId: String?) {
        val id = callId ?: DefaultCallRepository.activeCall.value?.id ?: return
        CallRinger.stop(id)
    }

    fun answer(callId: String? = null) {
        val call = getCallById(callId)
        DiagnosticLog.debug(DiagnosticEvent.CALL_ANSWER_REQUESTED)
        stopRingingFor(callId)
        answerTelecomCall(call?.let(::AndroidTelecomCallActions))
    }

    fun markAnsweredFromNotification(callId: String) {
        val svc = serviceRef?.get() as? FunPhoneInCallService
        svc?.markCallAnsweredFromNotification(callId)
        DiagnosticLog.debug(DiagnosticEvent.CALL_ANSWERED_FROM_NOTIFICATION)
    }

    fun reject(callId: String? = null) {
        val call = getCallById(callId)
        DiagnosticLog.debug(DiagnosticEvent.CALL_REJECT_REQUESTED)
        stopRingingFor(callId)
        rejectTelecomCall(
            actions = call?.let(::AndroidTelecomCallActions),
            supportsDeclinedReason = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
        )
    }

    fun rejectWithMessage(callId: String?, message: String): Boolean {
        val call = getCallById(callId)
        DiagnosticLog.debug(DiagnosticEvent.CALL_REJECT_WITH_MESSAGE_REQUESTED)
        stopRingingFor(callId)
        return rejectTelecomCallWithMessage(
            actions = call?.let(::AndroidTelecomCallActions),
            message = message,
            callSupportsTextResponse =
                call?.details?.can(Call.Details.CAPABILITY_RESPOND_VIA_TEXT) == true,
            supportsDeclinedReason = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
        )
    }

    fun mergeCalls(): Boolean {
        DiagnosticLog.debug(DiagnosticEvent.CALL_MERGE_REQUESTED)
        return mergeTelecomCalls(DefaultCallRepository.getAllCalls()) { callId ->
            DefaultCallRepository.getPlatformCall(callId)?.let(::AndroidTelecomCallActions)
        }
    }

    fun selectPhoneAccount(callId: String, accountId: String): Boolean {
        val svc = serviceRef?.get() ?: return false
        val call = getCallById(callId) ?: return false
        val handle = PhoneAccountDirectory.handleFor(svc, accountId) ?: return false
        call.phoneAccountSelected(handle, false)
        return true
    }

    fun end(callId: String? = null) {
        val call = getCallById(callId)
        DiagnosticLog.debug(DiagnosticEvent.CALL_END_REQUESTED)
        endTelecomCall(call?.let(::AndroidTelecomCallActions))
    }

    fun hold(callId: String? = null) {
        val call = getCallById(callId)
        DiagnosticLog.debug(DiagnosticEvent.CALL_HOLD_REQUESTED)
        holdTelecomCall(call?.let(::AndroidTelecomCallActions))
    }

    fun unhold(callId: String? = null) {
        val call = getCallById(callId)
        DiagnosticLog.debug(DiagnosticEvent.CALL_RESUME_REQUESTED)
        unholdTelecomCall(call?.let(::AndroidTelecomCallActions))
    }

    fun mute(muted: Boolean) {
        serviceRef?.get()?.setMuted(muted)
        DiagnosticLog.debug(DiagnosticEvent.CALL_MUTE_REQUESTED)
    }

    fun startDtmf(callId: String?, digit: Char) {
        val actions = getCallActionsById(callId)
        DiagnosticLog.debug(DiagnosticEvent.CALL_DTMF_REQUESTED)
        startTelecomDtmf(actions, digit)
    }

    fun stopDtmf(callId: String?) {
        stopTelecomDtmf(getCallActionsById(callId))
    }

    fun swapCalls() {
        val calls = DefaultCallRepository.getAllCalls()
        if (calls.size >= 2) {
            swapTelecomCalls(calls) { callId ->
                DefaultCallRepository.getPlatformCall(callId)?.let(::AndroidTelecomCallActions)
            }
            DiagnosticLog.debug(DiagnosticEvent.CALL_SWAP_REQUESTED)
        }
    }

    fun setAudioRoute(targetRoute: AudioRoute) {
        changeAudioRoute(targetRoute, updatePreference = true)
    }

    fun initializeDefaultAudioRoute() {
        preferredRoute = AudioRoute.EARPIECE
        preferredEndpoint = null
        applyPreferredRoute()
    }

    fun onSystemAudioStateChanged(route: AudioRoute, isMuted: Boolean = lastReportedMuted) {
        val muteChanged = isMuted != lastReportedMuted
        lastReportedMuted = isMuted
        if (muteChanged) {
            return
        }
        val matchesPreference = when {
            preferredRoute == AudioRoute.BLUETOOTH && preferredEndpoint != null -> route == AudioRoute.BLUETOOTH
            else -> route == preferredRoute
        }
        if (!matchesPreference) {
            applyPreferredRoute()
        }
    }

    @androidx.annotation.VisibleForTesting
    fun getLastReportedMutedForTest(): Boolean = lastReportedMuted

    @androidx.annotation.VisibleForTesting
    fun resetLastReportedMutedForTest() {
        lastReportedMuted = false
    }

    fun onNewCallStarted() {
        resetAudioPreferences()
    }

    private fun resetAudioPreferences() {
        preferredRoute = AudioRoute.EARPIECE
        preferredEndpoint = null
    }

    private fun applyPreferredRoute() {
        preferredEndpoint?.let {
            changeAudioEndpoint(it, updatePreference = false)
        } ?: changeAudioRoute(preferredRoute, updatePreference = false)
    }

    private fun changeAudioRoute(targetRoute: AudioRoute, updatePreference: Boolean) {
        if (updatePreference) {
            preferredRoute = targetRoute
            preferredEndpoint = null
        }
        val svc = serviceRef?.get() as? FunPhoneInCallService ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val targetType = platformEndpointType(targetRoute)
            val endpoint = svc.getAvailableEndpointsSnapshot().firstOrNull { it.endpointType == targetType }
            if (endpoint != null) {
                val executor = ContextCompat.getMainExecutor(svc)
                svc.requestCallEndpointChange(
                    endpoint,
                    executor,
                    object : android.os.OutcomeReceiver<Void?, android.telecom.CallEndpointException> {
                        override fun onResult(result: Void?) {
                            DiagnosticLog.debug(DiagnosticEvent.CALL_AUDIO_ROUTE_CHANGED)
                            DefaultCallRepository.setAudioRoute(targetRoute)
                        }

                        override fun onError(_error: android.telecom.CallEndpointException) {
                            DiagnosticLog.error(DiagnosticEvent.CALL_AUDIO_ROUTE_CHANGE_FAILED)
                            resetPreferenceToCurrentState(svc)
                            if (legacySetAudioRoute(svc, targetRoute)) {
                                preferredRoute = targetRoute
                                preferredEndpoint = null
                                DefaultCallRepository.setAudioRoute(targetRoute)
                            }
                        }
                    }
                )
            } else {
                DiagnosticLog.warning(DiagnosticEvent.CALL_AUDIO_ENDPOINT_UNAVAILABLE)
                if (legacySetAudioRoute(svc, targetRoute)) {
                    DefaultCallRepository.setAudioRoute(targetRoute)
                }
            }
        } else {
            if (legacySetAudioRoute(svc, targetRoute)) {
                DefaultCallRepository.setAudioRoute(targetRoute)
            }
        }
    }

    private fun collectConnectedBluetoothDeviceNames(context: Context): Set<String> {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return emptySet()
        val outs = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val names = outs.filter { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
            .mapNotNull { it.productName?.toString()?.takeIf { n -> n.isNotBlank() } }
            .toSet()
        return names
    }

    private fun collectWiredPresent(context: Context): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { d ->
            d.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    d.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    d.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }
    }

    fun getAvailableAudioEndpoints(): List<AudioEndpoint> {
        val svc = serviceRef?.get() as? FunPhoneInCallService ?: return listOf(AudioEndpoint(AudioRoute.EARPIECE), AudioEndpoint(AudioRoute.SPEAKER))
        val context = svc.applicationContext
        val endpoints = mutableListOf<AudioEndpoint>()
        endpoints.add(AudioEndpoint(AudioRoute.EARPIECE))
        endpoints.add(AudioEndpoint(AudioRoute.SPEAKER))
        val btNamesDetected = (collectConnectedBluetoothDeviceNames(context) + collectBluetoothProfileNames(context)).toSet()
        if (btNamesDetected.isNotEmpty()) {
            cachedBluetoothNames.clear()
            cachedBluetoothNames.addAll(btNamesDetected)
        }
        val wiredPresent = collectWiredPresent(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val availableEndpoints = svc.getAvailableEndpointsSnapshot()
            availableEndpoints.forEach { e ->
                when (e.endpointType) {
                    CallEndpoint.TYPE_BLUETOOTH -> {
                        val raw = e.endpointName.toString()
                        val display = raw.ifBlank { btNamesDetected.firstOrNull() ?: "Bluetooth" }
                        if (display.isNotBlank()) cachedBluetoothNames.add(display)
                        endpoints.add(AudioEndpoint(AudioRoute.BLUETOOTH, display, raw))
                    }
                    CallEndpoint.TYPE_WIRED_HEADSET -> {
                        if (endpoints.none { it.route == AudioRoute.WIRED }) endpoints.add(AudioEndpoint(AudioRoute.WIRED))
                    }
                    else -> {}
                }
            }
        } else {
            @Suppress("DEPRECATION") val audioState = svc.callAudioState
            val supported = audioState?.supportedRouteMask ?: (CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
            if (supported and CallAudioState.ROUTE_BLUETOOTH != 0 || btNamesDetected.isNotEmpty() || cachedBluetoothNames.isNotEmpty()) {
                val list = (btNamesDetected + cachedBluetoothNames).ifEmpty { setOf("Bluetooth") }
                list.forEach { n -> endpoints.add(AudioEndpoint(AudioRoute.BLUETOOTH, n, n)) }
            }
            if (supported and CallAudioState.ROUTE_WIRED_HEADSET != 0 || wiredPresent) {
                if (endpoints.none { it.route == AudioRoute.WIRED }) endpoints.add(AudioEndpoint(AudioRoute.WIRED))
            }
        }
        DefaultCallRepository.activeCall.value?.audioEndpoint?.takeIf { it.route == AudioRoute.BLUETOOTH }?.let { activeEndpoint ->
            val id = activeEndpoint.identifier ?: activeEndpoint.name
            val exists = endpoints.any { it.route == AudioRoute.BLUETOOTH && (it.identifier == id || it.name == activeEndpoint.name) }
            if (!exists) endpoints.add(activeEndpoint)
        }
        val bluetoothGroup = endpoints.filter { it.route == AudioRoute.BLUETOOTH }.distinctBy { it.identifier ?: it.name }
        val others = endpoints.filter { it.route != AudioRoute.BLUETOOTH }
        val ordered = others.filter { it.route == AudioRoute.EARPIECE } + bluetoothGroup + others.filter { it.route == AudioRoute.WIRED } + others.filter { it.route == AudioRoute.SPEAKER }
        DiagnosticLog.debug(DiagnosticEvent.CALL_AUDIO_ENDPOINTS_RESOLVED)
        return ordered
    }

    @Suppress("DEPRECATION")
    private fun legacySetAudioRoute(svc: InCallService, targetRoute: AudioRoute): Boolean {
        val audioRoute = platformAudioRoute(targetRoute)
        return runCatching {
            svc.setAudioRoute(audioRoute)
            DiagnosticLog.debug(DiagnosticEvent.CALL_AUDIO_ROUTE_CHANGED)
            true
        }.getOrDefault(false)
    }

    private fun resetPreferenceToCurrentState(svc: InCallService) {
        val currentRoute = audioRouteFromState(
            @Suppress("DEPRECATION")
            svc.callAudioState
        )
        preferredRoute = currentRoute
        preferredEndpoint = null
    }

    private fun audioRouteFromState(state: CallAudioState?): AudioRoute =
        audioRouteFromPlatform(state?.route)

    fun setAudioEndpoint(endpoint: AudioEndpoint) {
        changeAudioEndpoint(endpoint, updatePreference = true)
    }

    private fun changeAudioEndpoint(endpoint: AudioEndpoint, updatePreference: Boolean) {
        if (updatePreference) {
            preferredRoute = endpoint.route
            preferredEndpoint = endpoint
        }
        val svc = serviceRef?.get() as? FunPhoneInCallService ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val targetType = platformEndpointType(endpoint.route)
            val available = svc.getAvailableEndpointsSnapshot()
            val target = if (endpoint.route == AudioRoute.BLUETOOTH && endpoint.identifier != null) {
                available.firstOrNull { it.endpointType == targetType && it.endpointName.toString() == endpoint.identifier }
            } else available.firstOrNull { it.endpointType == targetType }
            if (target != null) {
                val executor = ContextCompat.getMainExecutor(svc)
                svc.requestCallEndpointChange(
                    target,
                    executor,
                    object : android.os.OutcomeReceiver<Void?, android.telecom.CallEndpointException> {
                        override fun onResult(result: Void?) {
                            DefaultCallRepository.setAudioEndpoint(endpoint)
                        }

                        override fun onError(_error: android.telecom.CallEndpointException) {
                            DiagnosticLog.error(DiagnosticEvent.CALL_AUDIO_ENDPOINT_CHANGE_FAILED)
                            resetPreferenceToCurrentState(svc)
                        }
                    }
                )
            } else {
                DiagnosticLog.warning(DiagnosticEvent.CALL_AUDIO_ENDPOINT_STALE)
                if (updatePreference) {
                    preferredEndpoint = null
                }
                changeAudioRoute(endpoint.route, updatePreference = false)
            }
        } else {
            if (legacySetAudioRoute(svc, endpoint.route)) {
                DefaultCallRepository.setAudioEndpoint(endpoint)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun collectBluetoothProfileNames(context: Context): Set<String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            return emptySet()
        }
        return runCatching {
            val manager = context.getSystemService(BluetoothManager::class.java)
            val headset = manager?.getConnectedDevices(BluetoothProfile.HEADSET).orEmpty()
            val a2dp = manager?.getConnectedDevices(BluetoothProfile.A2DP).orEmpty()
            (headset + a2dp).mapNotNull { it.name?.takeIf { n -> n.isNotBlank() } }.toSet()
        }.getOrDefault(emptySet())
    }
}
