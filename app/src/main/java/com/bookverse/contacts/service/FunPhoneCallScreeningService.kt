package com.bookverse.contacts.service

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.BlockedNumberContract
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection
import android.telecom.TelecomManager
import com.bookverse.contacts.data.repository.ProfilePhoneNumbers
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.domain.call.CallScreeningPolicy
import com.bookverse.contacts.domain.call.CallScreeningVerdict
import com.bookverse.contacts.domain.call.CallerVerification
import com.bookverse.contacts.domain.call.NeighborNumberPolicy
import com.bookverse.contacts.domain.contacts.BlockedNumberMatchPolicy
import com.bookverse.contacts.settings.CallPreferences
import com.bookverse.contacts.utils.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class FunPhoneCallScreeningService : CallScreeningService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        serviceScope.launch {
            val verdict = withTimeoutOrNull(SCREENING_TIMEOUT_MILLIS) {
                runCatching { evaluate(callDetails) }.getOrNull()
            }
            if (verdict == null) DiagnosticLog.warning(DiagnosticEvent.CALL_SCREENING_FAILED)
            respond(callDetails, verdict ?: CallScreeningVerdict.ALLOW)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun evaluate(callDetails: Call.Details): CallScreeningVerdict {
        val settings = CallPreferences.currentScreeningSettings(applicationContext)
        val isIncoming = callDetails.callDirection == Call.Details.DIRECTION_INCOMING
        val now = System.currentTimeMillis()
        return CallScreeningPolicy.screen(
            settings = settings,
            isIncoming = isIncoming,
            number = callDetails.handle?.schemeSpecificPart,
            presentationAllowed =
                callDetails.handlePresentation == TelecomManager.PRESENTATION_ALLOWED,
            isOnBlockedList = { number -> isOnBlockedList(applicationContext, number) },
            isInContacts = { number -> isInContacts(applicationContext, number) },
            verification = callerVerification(callDetails),
            wasRecentlyDialed = { number ->
                hasCallLogEntry(
                    context = applicationContext,
                    number = number,
                    since = now - CallScreeningPolicy.RECENTLY_DIALED_WINDOW_MILLIS,
                    outgoing = true,
                )
            },
            isRepeatedCall = { number ->
                hasCallLogEntry(
                    context = applicationContext,
                    number = number,
                    since = now - CallScreeningPolicy.REPEATED_CALL_WINDOW_MILLIS,
                    outgoing = false,
                )
            },
            resemblesOwnNumber = { number ->
                NeighborNumberPolicy.resemblesOwnNumber(number, ProfilePhoneNumbers.read(applicationContext))
            },
        )
    }

    private fun respond(callDetails: Call.Details, verdict: CallScreeningVerdict) {
        val number = callDetails.handle?.schemeSpecificPart
        if (verdict.silencesCall && number != null) SilencedCalls.mark(number)
        val response = CallResponse.Builder().apply {
            if (verdict.blocksCall) {
                setDisallowCall(true)
                setRejectCall(true)
                setSkipCallLog(false)
                setSkipNotification(true)
            } else if (verdict.silencesCall) {
                setSilenceCall(true)
            }
        }.build()
        try {
            respondToCall(callDetails, response)
            DiagnosticLog.debug(
                when {
                    verdict.blocksCall -> DiagnosticEvent.CALL_SCREENING_BLOCKED
                    verdict.silencesCall -> DiagnosticEvent.CALL_SCREENING_SILENCED
                    else -> DiagnosticEvent.CALL_SCREENING_ALLOWED
                },
            )
        } catch (_: RuntimeException) {
            DiagnosticLog.warning(DiagnosticEvent.CALL_SCREENING_FAILED)
        }
    }

    private fun callerVerification(callDetails: Call.Details): CallerVerification =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            when (callDetails.callerNumberVerificationStatus) {
                Connection.VERIFICATION_STATUS_PASSED -> CallerVerification.PASSED
                Connection.VERIFICATION_STATUS_FAILED -> CallerVerification.FAILED
                else -> CallerVerification.NOT_VERIFIED
            }
        } else {
            CallerVerification.NOT_VERIFIED
        }

    private fun hasCallLogEntry(context: Context, number: String, since: Long, outgoing: Boolean): Boolean {
        if (!PermissionUtils.hasPermission(context, Manifest.permission.READ_CALL_LOG)) return false
        val typeClause = if (outgoing) "${CallLog.Calls.TYPE} = ?" else "${CallLog.Calls.TYPE} != ?"
        return try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER),
                "${CallLog.Calls.DATE} >= ? AND $typeClause",
                arrayOf(since.toString(), CallLog.Calls.OUTGOING_TYPE.toString()),
                "${CallLog.Calls.DATE} DESC",
            )?.use { cursor ->
                var matched = false
                while (!matched && cursor.moveToNext()) {
                    matched = BlockedNumberMatchPolicy.isSameSubscriber(number, cursor.getString(0))
                }
                matched
            } ?: false
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun isOnBlockedList(context: Context, number: String): Boolean = try {
        if (!BlockedNumberContract.canCurrentUserBlockNumbers(context)) {
            false
        } else {
            context.contentResolver.query(
                BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                arrayOf(
                    BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER,
                    BlockedNumberContract.BlockedNumbers.COLUMN_E164_NUMBER,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                var matched = false
                while (!matched && cursor.moveToNext()) {
                    matched = BlockedNumberMatchPolicy.isSameSubscriber(number, cursor.getString(0)) ||
                        BlockedNumberMatchPolicy.isSameSubscriber(number, cursor.getString(1))
                }
                matched
            } ?: false
        }
    } catch (_: SecurityException) {
        false
    } catch (_: UnsupportedOperationException) {
        false
    }

    private fun isInContacts(context: Context, number: String): Boolean? {
        if (!PermissionUtils.hasPermission(context, Manifest.permission.READ_CONTACTS)) return null
        return try {
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number),
            )
            val foundByLookup = context.contentResolver.query(
                lookupUri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null,
                null,
                null,
            )?.use { cursor -> cursor.moveToFirst() } ?: return null
            foundByLookup || matchesAnySavedNumber(context, number)
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun matchesAnySavedNumber(context: Context, number: String): Boolean =
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            null,
            null,
            null,
        )?.use { cursor ->
            var matched = false
            while (!matched && cursor.moveToNext()) {
                matched = BlockedNumberMatchPolicy.isSameSubscriber(number, cursor.getString(0))
            }
            matched
        } ?: false

    private companion object {
        const val SCREENING_TIMEOUT_MILLIS = 3_500L
    }
}
