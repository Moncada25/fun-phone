package com.bookverse.contacts.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.domain.call.SimDecision
import com.bookverse.contacts.domain.call.SimPreference
import com.bookverse.contacts.domain.call.SimSelectionPolicy
import com.bookverse.contacts.service.PhoneAccountDirectory
import com.bookverse.contacts.service.SimSelectionCoordinator
import com.bookverse.contacts.settings.CallPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object PhoneActions {

    private sealed interface PendingCallRequest {
        data class Number(val value: String) : PendingCallRequest
        data object Voicemail : PendingCallRequest
    }

    private var callPermissionLauncher: ActivityResultLauncher<String>? = null
    private var defaultDialerLauncher: ActivityResultLauncher<Intent>? = null
    private var pendingCallRequest: PendingCallRequest? = null
    private val callScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private enum class DialType { NORMAL, USSD, SECRET }

    private fun classify(number: String): DialType {
        val n = number
        return when {
            n == "*#06#" -> DialType.SECRET
            n.contains('*') && n.endsWith('#') && n.any { it.isDigit() } -> DialType.USSD
            else -> DialType.NORMAL
        }
    }

    private fun buildDialUri(raw: String): Uri {
        val sanitized = raw.replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
        return Uri.fromParts("tel", sanitized, null)
    }

    fun setupCallPermissionLauncher(activity: ComponentActivity) {
        callPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted && pendingCallRequest != null) {
                resolvePendingCall(activity)
            } else {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.call_permission_required),
                    Toast.LENGTH_SHORT,
                ).show()
                pendingCallRequest = null
            }
        }

        defaultDialerLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            PermissionUtils.handleDefaultDialerResult(
                context = activity,
                resultCode = result.resultCode,
                onSuccess = {
                    resolvePendingCall(activity)
                },
                onFailure = {
                    pendingCallRequest = null
                }
            )
        }
    }

    private fun resolvePendingCall(context: Context) {
        val request = pendingCallRequest ?: return
        pendingCallRequest = null
        when (request) {
            is PendingCallRequest.Number -> executeCall(context, request.value)
            PendingCallRequest.Voicemail -> executeVoicemailCall(context)
        }
    }

    fun callPhoneNumber(context: Context, phoneNumber: String) {
        DiagnosticLog.debug(DiagnosticEvent.CALL_PHONE_REQUESTED)
        if (PermissionUtils.isDefaultDialer(context)) {
            executeCall(context, phoneNumber)
        } else {
            DiagnosticLog.debug(DiagnosticEvent.CALL_DEFAULT_DIALER_REQUIRED)
            pendingCallRequest = PendingCallRequest.Number(phoneNumber)
            defaultDialerLauncher?.let { launcher ->
                PermissionUtils.requestSetAsDefaultDialer(
                    context = context,
                    launcher = launcher,
                    onFailure = {
                        DiagnosticLog.warning(DiagnosticEvent.CALL_DEFAULT_DIALER_REQUEST_FAILED)
                        pendingCallRequest = null
                        Toast.makeText(
                            context,
                            context.getString(R.string.call_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } ?: run {
                DiagnosticLog.error(DiagnosticEvent.CALL_DEFAULT_DIALER_LAUNCHER_UNAVAILABLE)
                Toast.makeText(
                    context,
                    context.getString(R.string.call_initialization_error),
                    Toast.LENGTH_SHORT,
                )
                    .show()
            }
        }
    }

    fun callVoicemail(context: Context) {
        DiagnosticLog.debug(DiagnosticEvent.CALL_VOICEMAIL_REQUESTED)
        if (PermissionUtils.isDefaultDialer(context)) {
            executeVoicemailCall(context)
        } else {
            DiagnosticLog.debug(DiagnosticEvent.CALL_DEFAULT_DIALER_REQUIRED)
            pendingCallRequest = PendingCallRequest.Voicemail
            defaultDialerLauncher?.let { launcher ->
                PermissionUtils.requestSetAsDefaultDialer(
                    context = context,
                    launcher = launcher,
                    onFailure = {
                        DiagnosticLog.warning(DiagnosticEvent.CALL_DEFAULT_DIALER_REQUEST_FAILED)
                        pendingCallRequest = null
                        Toast.makeText(
                            context,
                            context.getString(R.string.voicemail_call_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } ?: run {
                DiagnosticLog.error(DiagnosticEvent.CALL_DEFAULT_DIALER_LAUNCHER_UNAVAILABLE)
                Toast.makeText(
                    context,
                    context.getString(R.string.voicemail_call_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun executeVoicemailCall(context: Context) {
        try {
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val configuredNumber = try {
                telephonyManager?.voiceMailNumber
            } catch (_: SecurityException) {
                DiagnosticLog.warning(DiagnosticEvent.CALL_VOICEMAIL_FAILED)
                null
            }

            if (!configuredNumber.isNullOrBlank()) {
                DiagnosticLog.debug(DiagnosticEvent.CALL_VOICEMAIL_NUMBER_USED)
                executeCall(context, configuredNumber)
                return
            }

            val displayLabel = context.getString(R.string.voicemail)
            val voicemailUri = Uri.fromParts(PhoneAccount.SCHEME_VOICEMAIL, "", null)
            val telecomManager =
                context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

            if (PermissionUtils.isDefaultDialer(context) && telecomManager != null) {
                try {
                    InCallNavigator.open(context, displayLabel)
                    telecomManager.placeCall(voicemailUri, Bundle())
                    DiagnosticLog.debug(DiagnosticEvent.CALL_VOICEMAIL_URI_USED)
                    return
                } catch (_: SecurityException) {
                    DiagnosticLog.warning(DiagnosticEvent.CALL_PLACE_FAILED)
                } catch (_: Exception) {
                    DiagnosticLog.warning(DiagnosticEvent.CALL_PLACE_FAILED)
                }
            }

            InCallNavigator.open(context, displayLabel)
            val intent = Intent(Intent.ACTION_CALL).apply { data = voicemailUri }
            addNewTaskIfNeeded(context, intent)
            val pm = context.packageManager
            val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo != null) {
                context.startActivity(intent)
                DiagnosticLog.debug(DiagnosticEvent.CALL_VOICEMAIL_URI_USED)
            } else {
                DiagnosticLog.warning(DiagnosticEvent.CALL_VOICEMAIL_HANDLER_UNAVAILABLE)
                Toast.makeText(
                    context,
                    context.getString(R.string.voicemail_call_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.CALL_VOICEMAIL_FAILED)
            Toast.makeText(
                context,
                context.getString(R.string.voicemail_call_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun addNewTaskIfNeeded(context: Context, intent: Intent) {
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun isImeiSpecialCode(input: String): Boolean =
        input.trim().replace(" ", "") == "*#06#"

    private fun Context.openImeiInfoOrAboutPhone(): Boolean {
        val candidates = listOf(
            Intent("android.settings.IMEI_INFO_SETTINGS"),
            Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS)
        )
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (packageManager.resolveActivity(intent, 0) != null) {
                startActivity(intent)
                return true
            }
        }
        return false
    }

    private fun executeCall(context: Context, phoneNumber: String) {
        try {
            DiagnosticLog.debug(DiagnosticEvent.CALL_PLACE_REQUESTED)

            if (isImeiSpecialCode(phoneNumber)) {
                val opened = context.openImeiInfoOrAboutPhone()
                DiagnosticLog.debug(DiagnosticEvent.CALL_SPECIAL_CODE_REQUESTED)
                if (!opened) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.device_info_open_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }

            val accounts = PhoneAccountDirectory.callCapableAccounts(context)
            if (accounts.size < 2 || PhoneAccountDirectory.isEmergencyNumber(context, phoneNumber)) {
                placeCall(context, phoneNumber, null)
                return
            }

            callScope.launch {
                val preference = runCatching { CallPreferences.currentSimPreference(context) }
                    .getOrDefault(SimPreference.AskEveryTime)
                when (val decision = SimSelectionPolicy.decide(accounts, preference)) {
                    SimDecision.SystemDefault -> placeCall(context, phoneNumber, null)
                    is SimDecision.Use -> placeCall(context, phoneNumber, decision.accountId)
                    SimDecision.Ask -> {
                        val requested = SimSelectionCoordinator.requestForNewCall(phoneNumber, accounts)
                        if (!requested) placeCall(context, phoneNumber, null)
                    }
                }
            }
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.CALL_EXECUTION_FAILED)
            Toast.makeText(
                context,
                context.getString(R.string.call_execution_error),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    fun callPhoneNumberWithSim(context: Context, phoneNumber: String, accountId: String) {
        placeCall(context, phoneNumber, accountId)
    }

    private fun placeCall(context: Context, phoneNumber: String, accountId: String?) {
        try {
            val type = classify(phoneNumber)
            val uri = buildDialUri(phoneNumber)
            val accountHandle = accountId?.let { PhoneAccountDirectory.handleFor(context, it) }
            DiagnosticLog.debug(DiagnosticEvent.CALL_PLACE_REQUESTED)

            if (type == DialType.NORMAL) {
                InCallNavigator.open(context, phoneNumber)
            }

            if (PermissionUtils.isDefaultDialer(context)) {
                try {
                    val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                    val extras = Bundle().apply {
                        accountHandle?.let {
                            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it)
                        }
                    }
                    tm.placeCall(uri, extras)
                    DiagnosticLog.debug(DiagnosticEvent.CALL_PLACE_REQUESTED)
                    return
                } catch (_: SecurityException) {
                    DiagnosticLog.warning(DiagnosticEvent.CALL_PLACE_FAILED)
                } catch (_: Exception) {
                    DiagnosticLog.warning(DiagnosticEvent.CALL_PLACE_FAILED)
                }
            }

            val intent = Intent(Intent.ACTION_CALL).apply {
                data = uri
                accountHandle?.let { putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it) }
            }
            addNewTaskIfNeeded(context, intent)
            val pm = context.packageManager
            val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo != null) {
                context.startActivity(intent)
                DiagnosticLog.debug(DiagnosticEvent.CALL_PLACE_REQUESTED)
            } else {
                DiagnosticLog.warning(DiagnosticEvent.CALL_HANDLER_UNAVAILABLE)
                Toast.makeText(
                    context,
                    context.getString(R.string.call_failed),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.CALL_EXECUTION_FAILED)
            Toast.makeText(
                context,
                context.getString(R.string.call_execution_error),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    fun openWhatsApp(context: Context, phoneNumber: String) {
        val formattedNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        val url = "https://wa.me/$formattedNumber"
        UrlUtils.openUrl(context, url)
    }

    fun openSmsApp(context: Context, phoneNumber: String) {
        val uri = "smsto:$phoneNumber".toUri()
        val intent = Intent(Intent.ACTION_SENDTO, uri)
        context.startActivity(intent)
    }
}
