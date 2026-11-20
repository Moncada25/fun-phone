package com.bookverse.contacts.service

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.DisconnectCause
import android.telecom.InCallService
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telephony.PhoneNumberUtils
import androidx.annotation.RequiresApi
import androidx.core.os.BundleCompat
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import com.bookverse.contacts.data.model.RecentOperation
import com.bookverse.contacts.data.model.RecentType
import com.bookverse.contacts.data.repository.ContactRepository
import com.bookverse.contacts.data.repository.RecentOperationsRepository
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.domain.call.AudioRoute
import com.bookverse.contacts.domain.call.CallState
import com.bookverse.contacts.domain.call.SimDecision
import com.bookverse.contacts.domain.call.SimPreference
import com.bookverse.contacts.domain.call.SimSelectionPolicy
import com.bookverse.contacts.domain.call.UiCall
import com.bookverse.contacts.domain.review.ReviewEligibilityStore
import com.bookverse.contacts.settings.CallPreferences
import com.bookverse.contacts.utils.AppVisibility
import com.bookverse.contacts.utils.PermissionUtils
import contacts.core.entities.PhoneEntity
import android.graphics.Bitmap
import com.bookverse.contacts.utils.ContactUtils
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.scale
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel

class FunPhoneInCallService : InCallService() {
    private var lastAudioState: CallAudioState? = null
    private val previousStates = mutableMapOf<String, Int>()
    private val incomingCallIds = mutableSetOf<String>()
    private val handledIncomingCallIds = mutableSetOf<String>()
    private val answeredFromNotification = mutableSetOf<String>()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var availableEndpoints: List<CallEndpoint> = emptyList()

    override fun onAvailableCallEndpointsChanged(endpoints: List<CallEndpoint>) {
        availableEndpoints = endpoints
    }

    fun getAvailableEndpointsSnapshot(): List<CallEndpoint> = availableEndpoints

    private val callback = object : Call.Callback() {
        @RequiresApi(Build.VERSION_CODES.R)
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            val id = callId(call)
            val number = call.details.handle?.schemeSpecificPart ?: ""
            val prev = previousStates[id]
            if (prev == state) return
            previousStates[id] = state
            val existingCall = DefaultCallRepository.calls.value.firstOrNull { it.id == id }
            val currentAudioRoute = existingCall?.audioRoute ?: audioRouteFrom(lastAudioState)
            val currentAudioEndpoint = existingCall?.audioEndpoint
            val ui = UiCall(
                id = id,
                number = number,
                state = mapPlatformState(state),
                isMuted = lastAudioState?.isMuted ?: false,
                isOnHold = state == Call.STATE_HOLDING,
                connectTimeMillis = call.details.connectTimeMillis,
                audioRoute = currentAudioRoute,
                audioEndpoint = currentAudioEndpoint,
            ).withTelecomTraits(call)
            DefaultCallRepository.upsert(id, ui, call)
            when (state) {
                Call.STATE_SELECT_PHONE_ACCOUNT -> handlePhoneAccountSelection(call, id, number)

                Call.STATE_RINGING -> {
                    incomingCallIds.add(id)
                    CallController.initializeDefaultAudioRoute()
                    if (handledIncomingCallIds.add(id)) {
                        handleIncomingCall(call, id, number)
                    }
                }

                Call.STATE_CONNECTING, Call.STATE_DIALING, Call.STATE_ACTIVE, Call.STATE_HOLDING -> {
                    incomingCallIds.remove(id)
                    answeredFromNotification.remove(id)
                    endRinging(id)

                    when (
                        resolveCallSurface(
                            state = ui.state,
                            appInForeground = AppVisibility.isForeground,
                            notificationAvailability = CallNotificationManager.availability(
                                this@FunPhoneInCallService,
                            ),
                        )
                    ) {
                        CallSurface.IN_APP_ONGOING -> startInCallActivity(id, number)
                        CallSurface.ONGOING_NOTIFICATION -> {
                            DiagnosticLog.debug(DiagnosticEvent.CALL_ONGOING_NOTIFICATION_SHOWN)
                            CallNotificationManager.showOngoing(
                                this@FunPhoneInCallService,
                                id,
                                number,
                            )
                        }
                        CallSurface.NONE ->
                            DiagnosticLog.warning(DiagnosticEvent.CALL_ONGOING_NOTIFICATION_UNAVAILABLE)
                        else -> Unit
                    }
                }

                Call.STATE_DISCONNECTED -> {
                    DefaultCallRepository.upsert(id, ui, call)
                    val wasTrackedIncoming = id in handledIncomingCallIds
                    val isIncomingCall =
                        call.details.callDirection == Call.Details.DIRECTION_INCOMING ||
                            wasTrackedIncoming
                    val wasMissed = isMissedCall(
                        isIncomingCall = isIncomingCall,
                        previousState = prev,
                        connectTimeMillis = call.details.connectTimeMillis,
                        disconnectCode = call.details.disconnectCause?.code ?: DisconnectCause.UNKNOWN,
                    )
                    if (call.details.connectTimeMillis > 0L) {
                        ReviewEligibilityStore.recordConnectedCall(this@FunPhoneInCallService)
                    }
                    if (wasMissed && PermissionUtils.hasNotificationPermission(this@FunPhoneInCallService)) {
                        try {
                            CallNotificationManager.showMissed(this@FunPhoneInCallService, number)
                        } catch (_: SecurityException) {
                            DiagnosticLog.error(DiagnosticEvent.CALL_MISSED_NOTIFICATION_FAILED)
                        }
                    }
                    CallNotificationManager.stopOngoing(this@FunPhoneInCallService)
                    endRinging(id)
                    incomingCallIds.remove(id)
                    answeredFromNotification.remove(id)
                    handledIncomingCallIds.remove(id)
                }

                else -> {}
            }
        }

        @RequiresApi(Build.VERSION_CODES.R)
        override fun onDetailsChanged(call: Call, details: Call.Details) {
            super.onDetailsChanged(call, details)
            val currentState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                call.details.state
            } else {
                @Suppress("DEPRECATION") call.state
            }
            val id = callId(call)
            val prev = previousStates[id]
            if (prev == currentState) {
                val existingCall = DefaultCallRepository.calls.value.firstOrNull { it.id == id }
                if (existingCall != null) {
                    val number = call.details.handle?.schemeSpecificPart ?: ""
                    val updated = existingCall.copy(
                        connectTimeMillis = call.details.connectTimeMillis,
                        number = number.ifEmpty { existingCall.number }
                    ).withTelecomTraits(call)
                    DefaultCallRepository.upsert(id, updated, call)
                }
                return
            }
            onStateChanged(call, currentState)
        }

        override fun onParentChanged(call: Call, parent: Call?) {
            super.onParentChanged(call, parent)
            refreshTelecomTraits(call)
        }

        override fun onChildrenChanged(call: Call, children: MutableList<Call>?) {
            super.onChildrenChanged(call, children)
            refreshTelecomTraits(call)
        }

        override fun onConferenceableCallsChanged(
            call: Call,
            conferenceableCalls: MutableList<Call>?,
        ) {
            super.onConferenceableCallsChanged(call, conferenceableCalls)
            refreshTelecomTraits(call)
        }

        override fun onCallDestroyed(call: Call) {
            super.onCallDestroyed(call)
            val id = callId(call)
            SimSelectionCoordinator.cancelForCall(id)
            DefaultCallRepository.remove(id)
            incomingCallIds.remove(id)
            handledIncomingCallIds.remove(id)
            endRinging(id)
            CallNotificationManager.stopOngoing(this@FunPhoneInCallService)
        }
    }

    override fun onCreate() {
        super.onCreate()
        CallController.attach(this)
    }

    override fun onDestroy() {
        CallRinger.stopAll()
        IncomingCallService.stop(this)
        IncomingCallService.cancelNotification(this)
        CallController.detach()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onSilenceRinger() {
        super.onSilenceRinger()
        CallRinger.silence()
    }

    override fun onBringToForeground(showDialpad: Boolean) {
        super.onBringToForeground(showDialpad)
        val call = DefaultCallRepository.activeCall.value ?: return
        if (call.state == CallState.RINGING) {
            startIncomingCallActivity(call.id, call.number)
        } else {
            startInCallActivity(call.id, call.number)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        DiagnosticLog.debug(DiagnosticEvent.CALL_ADDED)
        CallController.onNewCallStarted()
        CallController.initializeDefaultAudioRoute()
        call.registerCallback(callback)
        val currentState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            call.details.state
        } else {
            @Suppress("DEPRECATION") call.state
        }
        @SuppressLint("MissingPermission")
        callback.onStateChanged(call, currentState)
        if (currentState != Call.STATE_RINGING) {
            val id = callId(call)
            val number = call.details.handle?.schemeSpecificPart ?: ""
            if (!AppVisibility.isForeground) {
                val shown = CallNotificationManager.showOngoing(this, id, number)
                if (!shown) {
                    DiagnosticLog.warning(DiagnosticEvent.CALL_ONGOING_NOTIFICATION_UNAVAILABLE)
                }
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        DiagnosticLog.debug(DiagnosticEvent.CALL_REMOVED)
        call.unregisterCallback(callback)
        val id = callId(call)
        SimSelectionCoordinator.cancelForCall(id)
        DefaultCallRepository.remove(id)
        previousStates.remove(id)
        incomingCallIds.remove(id)
        answeredFromNotification.remove(id)
        handledIncomingCallIds.remove(id)
        endRinging(id)
        CallNotificationManager.stopOngoing(this)
    }

    @Deprecated("Use onCallAudioStateChanged with explicit handling")
    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        @Suppress("DEPRECATION")
        super.onCallAudioStateChanged(audioState)
        lastAudioState = audioState
        val resolvedRoute = audioRouteFrom(audioState)
        DiagnosticLog.debug(DiagnosticEvent.CALL_AUDIO_ROUTE_CHANGED)
        DefaultCallRepository.setMuted(audioState.isMuted)
        DefaultCallRepository.setAudioRoute(resolvedRoute)
        CallController.onSystemAudioStateChanged(resolvedRoute, audioState.isMuted)
        val activeCall = DefaultCallRepository.activeCall.value
        if (!AppVisibility.isForeground && activeCall != null && activeCall.state != CallState.RINGING) {
            CallNotificationManager.showOngoing(this, activeCall.id, activeCall.number)
        }
    }

    private fun audioRouteFrom(audioState: CallAudioState?): AudioRoute =
        audioRouteFromPlatform(audioState?.route)

    private fun mapPlatformState(state: Int): CallState = when (state) {
        Call.STATE_RINGING -> CallState.RINGING
        Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_SELECT_PHONE_ACCOUNT -> CallState.DIALING
        Call.STATE_ACTIVE -> CallState.ACTIVE
        Call.STATE_HOLDING -> CallState.ON_HOLD
        Call.STATE_DISCONNECTED -> CallState.DISCONNECTED
        else -> CallState.DISCONNECTED
    }

    private fun startInCallActivity(callId: String, number: String) {
        try {
            val intent = Intent(this, com.bookverse.contacts.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("open_incall", true)
                putExtra("incall_call_id", callId)
                putExtra("incall_number", number)
            }
            startActivity(intent)
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.CALL_ACTIVITY_START_FAILED)
        }
    }

    private fun callId(call: Call): String = System.identityHashCode(call).toString()

    private fun UiCall.withTelecomTraits(call: Call): UiCall {
        val details = call.details
        val isConference = details.hasProperty(Call.Details.PROPERTY_CONFERENCE)
        return copy(
            displayName = displayName
                ?: getString(R.string.conference_call).takeIf { isConference && number.isBlank() },
            isConference = isConference,
            parentId = call.parent?.let(::callId),
            canMerge = details.can(Call.Details.CAPABILITY_MERGE_CONFERENCE) ||
                call.conferenceableCalls.isNotEmpty(),
            canRespondViaText = details.can(Call.Details.CAPABILITY_RESPOND_VIA_TEXT),
        )
    }

    private fun refreshTelecomTraits(call: Call) {
        val id = callId(call)
        val existing = DefaultCallRepository.calls.value.firstOrNull { it.id == id } ?: return
        val updated = existing.withTelecomTraits(call)
        if (updated != existing) DefaultCallRepository.upsert(id, updated, call)
    }

    private fun handlePhoneAccountSelection(call: Call, id: String, number: String) {
        val handles = call.details.intentExtras?.let { extras ->
            BundleCompat.getParcelableArrayList(
                extras,
                Call.AVAILABLE_PHONE_ACCOUNTS,
                PhoneAccountHandle::class.java,
            )
        }.orEmpty().ifEmpty {
            PhoneAccountDirectory.callCapableHandles(
                this,
                call.details.handle?.scheme ?: PhoneAccount.SCHEME_TEL,
            )
        }
        val accounts = PhoneAccountDirectory.describe(this, handles)
        if (accounts.isEmpty()) {
            call.disconnect()
            return
        }
        serviceScope.launch {
            val preference = runCatching {
                CallPreferences.currentSimPreference(this@FunPhoneInCallService)
            }.getOrDefault(SimPreference.AskEveryTime)
            val decision = SimSelectionPolicy.decide(accounts, preference)
            withContext(Dispatchers.Main) {
                val selectedId = when {
                    decision is SimDecision.Use -> decision.accountId
                    accounts.size == 1 -> accounts.first().id
                    else -> null
                }
                val selectedHandle = selectedId?.let { accountId ->
                    handles.firstOrNull { PhoneAccountDirectory.accountId(it) == accountId }
                }
                if (selectedHandle != null) {
                    call.phoneAccountSelected(selectedHandle, false)
                } else {
                    SimSelectionCoordinator.requestForExistingCall(id, number, accounts)
                    startInCallActivity(id, number)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun handleIncomingCall(call: Call, id: String, number: String) {
        serviceScope.launch {
            val repository = ContactRepository(this@FunPhoneInCallService)
            val blockedInfo = withContext(Dispatchers.IO) {
                val contacts = repository.getContacts()
                val infos = repository.getBlockedNumberInfos(contacts)
                val normalizedIncoming = PhoneNumberUtils.normalizeNumber(number)
                infos.firstOrNull { info ->
                    val infoNormalized =
                        info.normalizedNumber ?: PhoneNumberUtils.normalizeNumber(info.displayNumber)
                    when {
                        normalizedIncoming != null && infoNormalized != null -> infoNormalized == normalizedIncoming
                        normalizedIncoming == null && infoNormalized == null -> info.displayNumber == number
                        normalizedIncoming == null -> info.displayNumber == number
                        else -> false
                    }
                }
            }
            DiagnosticLog.debug(DiagnosticEvent.CALL_BLOCKED_LOOKUP_COMPLETED)

            if (blockedInfo != null) {
                DiagnosticLog.debug(DiagnosticEvent.CALL_BLOCKED_REJECTED)
                withContext(Dispatchers.Main) {
                    try {
                        call.reject(Call.REJECT_REASON_DECLINED)
                    } catch (_: Throwable) {
                        call.disconnect()
                    }
                }

                withContext(Dispatchers.IO) {
                    val contactForLog = blockedInfo.contact ?: createPlaceholderContact(number)
                    val recentsRepository = RecentOperationsRepository(this@FunPhoneInCallService)
                    recentsRepository.addRecentOperation(
                        RecentOperation(
                            contact = contactForLog,
                            date = System.currentTimeMillis(),
                            type = RecentType.CALL_BLOCKED,
                            target = number
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    CallNotificationManager.stopOngoing(this@FunPhoneInCallService)
                    endRinging(id)
                }
                return@launch
            }

            val immersiveIncomingCallsEnabled = AppSettingsViewModel.isImmersiveIncomingCallsEnabled(
                applicationContext
            )
            val volumeKeyActionEnabled = AppSettingsViewModel.isIncomingCallVolumeKeyActionEnabled(
                applicationContext
            )
            withContext(Dispatchers.Main) {
                presentIncomingCall(id, number, immersiveIncomingCallsEnabled, volumeKeyActionEnabled)
            }

            val displayContact = withContext(Dispatchers.IO) {
                val contacts = repository.getContacts()
                repository.findContactByPhoneNumber(contacts, number)
            }
            val preloadedAvatarBytes = withContext(Dispatchers.IO) {
                displayContact?.photoUri?.let { uri ->
                    runCatching {
                        val bmp = ContactUtils.uriToBitmap(uri, this@FunPhoneInCallService)
                            ?: return@let null
                        val targetSize = 196
                        val scaled = if (bmp.width > targetSize || bmp.height > targetSize) {
                            bmp.scale(targetSize, targetSize)
                        } else bmp
                        ByteArrayOutputStream().use { stream ->
                            scaled.compress(Bitmap.CompressFormat.PNG, 90, stream)
                            stream.toByteArray()
                        }
                    }.getOrNull()
                }
            }
            withContext(Dispatchers.Main) {
                val existingCall = DefaultCallRepository.calls.value.firstOrNull { it.id == id }
                    ?: return@withContext
                val enrichedCall = existingCall.copy(
                    displayName = displayContact?.name ?: existingCall.displayName,
                    photoUri = displayContact?.photoUri?.toString() ?: existingCall.photoUri,
                    avatarBytes = preloadedAvatarBytes ?: existingCall.avatarBytes,
                )
                DefaultCallRepository.upsert(id, enrichedCall, call)
            }
            DiagnosticLog.debug(DiagnosticEvent.CALL_CONTACT_LOOKUP_COMPLETED)
        }
    }

    private fun isStillRinging(id: String): Boolean = previousStates[id] == Call.STATE_RINGING

    private fun presentIncomingCall(
        id: String,
        number: String,
        immersive: Boolean,
        volumeKeyActionEnabled: Boolean,
    ) {
        if (!isStillRinging(id)) return
        val callWaiting = DefaultCallRepository.calls.value.any { other ->
            other.id != id && other.state != CallState.RINGING && other.state != CallState.DISCONNECTED
        }
        CallRinger.start(this, id, callWaiting)
        val notificationShown = CallNotificationManager.showIncoming(this, id, number)
        if (!notificationShown) {
            DiagnosticLog.warning(DiagnosticEvent.CALL_SURFACE_UNAVAILABLE)
        }
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        val deviceLocked = keyguardManager?.isKeyguardLocked == true
        val launchActivity = shouldLaunchIncomingCallActivity(
            immersive = immersive,
            volumeKeyActionEnabled = volumeKeyActionEnabled,
            deviceLocked = deviceLocked,
            appInForeground = AppVisibility.isForeground,
            notificationShown = notificationShown,
        )
        DiagnosticLog.debug(DiagnosticEvent.CALL_SURFACE_SELECTED)
        if (launchActivity) {
            runCatching { com.bookverse.contacts.utils.WakeUpHelper.wakeUp(applicationContext) }
            startIncomingCallActivity(id, number)
        }
    }

    private fun endRinging(id: String) {
        CallRinger.stop(id)
        if (!CallRinger.isRinging() && !hasRingingCall(id)) {
            IncomingCallService.stop(this)
            IncomingCallService.cancelNotification(this)
        }
    }

    private fun hasRingingCall(excludedId: String): Boolean =
        previousStates.any { (callId, state) -> callId != excludedId && state == Call.STATE_RINGING }

    private fun startIncomingCallActivity(id: String, number: String) {
        try {
            val intent = Intent(
                this,
                com.bookverse.contacts.ui.activities.IncomingCallActivity::class.java
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); addFlags(
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            ); addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION); putExtra(
                "call_id",
                id
            ); putExtra("phone_number", number)
            }; startActivity(intent)
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.CALL_INCOMING_ACTIVITY_START_FAILED)
        }
    }

    private fun createPlaceholderContact(number: String): Contact {
        return Contact(
            id = number,
            name = getString(R.string.blocked_numbers_unknown),
            phones = listOf(ContactPhone(number, PhoneEntity.Type.MOBILE)),
            accounts = emptyList()
        )
    }

    fun markCallAnsweredFromNotification(callId: String) {
        answeredFromNotification.add(callId)
        DiagnosticLog.debug(DiagnosticEvent.CALL_ANSWERED_FROM_NOTIFICATION)
    }
}

internal fun isMissedCall(
    isIncomingCall: Boolean,
    previousState: Int?,
    connectTimeMillis: Long,
    disconnectCode: Int = DisconnectCause.UNKNOWN,
): Boolean = when (disconnectCode) {
    DisconnectCause.MISSED -> isIncomingCall
    DisconnectCause.REJECTED,
    DisconnectCause.LOCAL,
    DisconnectCause.ANSWERED_ELSEWHERE,
    DisconnectCause.CALL_PULLED,
    -> false
    else ->
        isIncomingCall &&
            (previousState == Call.STATE_RINGING || previousState == Call.STATE_CONNECTING) &&
            connectTimeMillis == 0L
}

internal fun shouldLaunchIncomingCallActivity(
    immersive: Boolean,
    deviceLocked: Boolean,
    appInForeground: Boolean,
    notificationShown: Boolean,
    volumeKeyActionEnabled: Boolean = false,
): Boolean = immersive ||
    deviceLocked ||
    (!appInForeground && (volumeKeyActionEnabled || !notificationShown))
