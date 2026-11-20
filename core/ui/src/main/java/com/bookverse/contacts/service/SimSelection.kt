package com.bookverse.contacts.service

import android.annotation.SuppressLint
import android.content.Context
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.domain.call.SimAccount
import com.bookverse.contacts.domain.call.SimPreference
import com.bookverse.contacts.domain.call.SipAddressPolicy
import com.bookverse.contacts.settings.CallPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

object PhoneAccountDirectory {
    fun accountId(handle: PhoneAccountHandle): String =
        "${handle.componentName.flattenToShortString()}#${handle.id}"

    fun schemeFor(target: String): String =
        if (SipAddressPolicy.isSipAddress(target)) PhoneAccount.SCHEME_SIP else PhoneAccount.SCHEME_TEL

    fun callCapableHandles(
        context: Context,
        scheme: String = PhoneAccount.SCHEME_TEL,
    ): List<PhoneAccountHandle> = enabledHandles(context) { it.supportsUriScheme(scheme) }

    fun callCapableAccounts(
        context: Context,
        scheme: String = PhoneAccount.SCHEME_TEL,
    ): List<SimAccount> = describe(context, callCapableHandles(context, scheme))

    @SuppressLint("MissingPermission")
    private fun enabledHandles(
        context: Context,
        accepts: (PhoneAccount) -> Boolean,
    ): List<PhoneAccountHandle> {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            ?: return emptyList()
        return try {
            telecomManager.callCapablePhoneAccounts.orEmpty().filter { handle ->
                telecomManager.getPhoneAccount(handle)?.let { account ->
                    account.isEnabled && accepts(account)
                } == true
            }
        } catch (_: SecurityException) {
            DiagnosticLog.warning(DiagnosticEvent.CALL_SIM_ACCOUNTS_UNAVAILABLE)
            emptyList()
        }
    }

    fun describe(context: Context, handles: List<PhoneAccountHandle>): List<SimAccount> {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        return handles.mapIndexed { index, handle ->
            val account = try {
                telecomManager?.getPhoneAccount(handle)
            } catch (_: SecurityException) {
                null
            }
            val label = account?.label?.toString()?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.sim_slot_label, index + 1)
            val detail = account?.address?.schemeSpecificPart?.takeIf { it.isNotBlank() }
                ?: account?.shortDescription?.toString()?.takeIf { it.isNotBlank() && it != label }
            SimAccount(id = accountId(handle), label = label, detail = detail)
        }
    }

    fun handleFor(context: Context, accountId: String): PhoneAccountHandle? =
        enabledHandles(context) { true }.firstOrNull { accountId(it) == accountId }

    fun isEmergencyNumber(context: Context, number: String): Boolean {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        return try {
            telephonyManager?.isEmergencyNumber(number) == true
        } catch (_: RuntimeException) {
            false
        }
    }
}

data class SimSelectionRequest(
    val requestId: Long,
    val number: String,
    val accounts: List<SimAccount>,
    val callId: String? = null,
)

object SimSelectionCoordinator {
    private val nextRequestId = AtomicLong(0)
    private val attachedHosts = AtomicInteger(0)
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _pending = MutableStateFlow<SimSelectionRequest?>(null)
    val pending: StateFlow<SimSelectionRequest?> = _pending.asStateFlow()

    val hasHost: Boolean
        get() = attachedHosts.get() > 0

    fun attachHost() {
        attachedHosts.incrementAndGet()
    }

    fun detachHost() {
        attachedHosts.updateAndGet { count -> (count - 1).coerceAtLeast(0) }
    }

    fun requestForNewCall(number: String, accounts: List<SimAccount>): Boolean {
        if (!hasHost) return false
        publish(number, accounts, callId = null)
        return true
    }

    fun requestForExistingCall(callId: String, number: String, accounts: List<SimAccount>) {
        publish(number, accounts, callId)
    }

    fun rememberChoice(context: Context, accountId: String) {
        val appContext = context.applicationContext
        persistenceScope.launch {
            runCatching {
                CallPreferences.setSimPreference(appContext, SimPreference.Account(accountId))
            }
        }
    }

    fun consume(request: SimSelectionRequest) {
        _pending.compareAndSet(request, null)
    }

    fun cancelForCall(callId: String) {
        val current = _pending.value
        if (current?.callId == callId) _pending.compareAndSet(current, null)
    }

    private fun publish(number: String, accounts: List<SimAccount>, callId: String?) {
        DiagnosticLog.debug(DiagnosticEvent.CALL_SIM_SELECTION_REQUESTED)
        _pending.value = SimSelectionRequest(
            requestId = nextRequestId.incrementAndGet(),
            number = number,
            accounts = accounts,
            callId = callId,
        )
    }
}
