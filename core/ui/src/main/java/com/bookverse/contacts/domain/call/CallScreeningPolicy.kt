package com.bookverse.contacts.domain.call

import androidx.compose.runtime.Immutable

enum class CallScreeningOption {
    BLOCK_LISTED,
    BLOCK_HIDDEN,
    BLOCK_FAILED_VERIFICATION,
    BLOCK_NEIGHBOR_NUMBERS,
    BLOCK_NOT_IN_CONTACTS,
    SILENCE_NOT_IN_CONTACTS,
    ALLOW_RECENTLY_DIALED,
    ALLOW_REPEATED_CALLS,
}

@Immutable
data class CallScreeningSettings(
    val blockListedNumbers: Boolean = false,
    val blockHiddenNumbers: Boolean = false,
    val blockNumbersNotInContacts: Boolean = false,
    val blockFailedVerification: Boolean = false,
    val blockNeighborNumbers: Boolean = false,
    val silenceNumbersNotInContacts: Boolean = false,
    val allowRecentlyDialed: Boolean = false,
    val allowRepeatedCalls: Boolean = false,
) {
    val anyRuleEnabled: Boolean
        get() = enabledRuleCount > 0

    val enabledRuleCount: Int
        get() = listOf(
            blockListedNumbers,
            blockHiddenNumbers,
            blockNumbersNotInContacts,
            blockFailedVerification,
            blockNeighborNumbers,
        ).count { it }

    fun isEnabled(option: CallScreeningOption): Boolean = when (option) {
        CallScreeningOption.BLOCK_LISTED -> blockListedNumbers
        CallScreeningOption.BLOCK_HIDDEN -> blockHiddenNumbers
        CallScreeningOption.BLOCK_FAILED_VERIFICATION -> blockFailedVerification
        CallScreeningOption.BLOCK_NEIGHBOR_NUMBERS -> blockNeighborNumbers
        CallScreeningOption.BLOCK_NOT_IN_CONTACTS -> blockNumbersNotInContacts
        CallScreeningOption.SILENCE_NOT_IN_CONTACTS -> silenceNumbersNotInContacts
        CallScreeningOption.ALLOW_RECENTLY_DIALED -> allowRecentlyDialed
        CallScreeningOption.ALLOW_REPEATED_CALLS -> allowRepeatedCalls
    }
}

enum class CallerVerification {
    PASSED,
    FAILED,
    NOT_VERIFIED,
}

enum class CallScreeningVerdict {
    ALLOW,
    BLOCK_LISTED,
    BLOCK_HIDDEN,
    BLOCK_FAILED_VERIFICATION,
    BLOCK_NEIGHBOR_NUMBER,
    BLOCK_NOT_IN_CONTACTS,
    SILENCE_NOT_IN_CONTACTS,
    ;

    val blocksCall: Boolean
        get() = this != ALLOW && this != SILENCE_NOT_IN_CONTACTS

    val silencesCall: Boolean
        get() = this == SILENCE_NOT_IN_CONTACTS
}

object CallScreeningPolicy {
    const val RECENTLY_DIALED_DAYS = 30
    const val REPEATED_CALL_MINUTES = 15
    const val RECENTLY_DIALED_WINDOW_MILLIS = RECENTLY_DIALED_DAYS * 24L * 60L * 60L * 1_000L
    const val REPEATED_CALL_WINDOW_MILLIS = REPEATED_CALL_MINUTES * 60L * 1_000L

    fun isHiddenNumber(number: String?, presentationAllowed: Boolean): Boolean =
        !presentationAllowed || number.isNullOrBlank()

    fun screen(
        settings: CallScreeningSettings,
        isIncoming: Boolean,
        number: String?,
        presentationAllowed: Boolean,
        isOnBlockedList: (String) -> Boolean,
        isInContacts: (String) -> Boolean?,
        verification: CallerVerification = CallerVerification.NOT_VERIFIED,
        wasRecentlyDialed: (String) -> Boolean = { false },
        isRepeatedCall: (String) -> Boolean = { false },
        resemblesOwnNumber: (String) -> Boolean = { false },
    ): CallScreeningVerdict {
        if (!isIncoming || !settings.anyRuleEnabled) return CallScreeningVerdict.ALLOW
        if (isHiddenNumber(number, presentationAllowed)) {
            return if (settings.blockHiddenNumbers) {
                CallScreeningVerdict.BLOCK_HIDDEN
            } else {
                CallScreeningVerdict.ALLOW
            }
        }
        val visibleNumber = number.orEmpty()
        if (settings.blockListedNumbers && isOnBlockedList(visibleNumber)) {
            return CallScreeningVerdict.BLOCK_LISTED
        }
        if (settings.blockFailedVerification && verification == CallerVerification.FAILED) {
            return CallScreeningVerdict.BLOCK_FAILED_VERIFICATION
        }
        val inContacts by lazy { isInContacts(visibleNumber) }
        if (settings.blockNeighborNumbers && inContacts != true && resemblesOwnNumber(visibleNumber)) {
            return CallScreeningVerdict.BLOCK_NEIGHBOR_NUMBER
        }
        if (settings.blockNumbersNotInContacts && inContacts == false) {
            return unknownCallerVerdict(settings, visibleNumber, wasRecentlyDialed, isRepeatedCall)
        }
        return CallScreeningVerdict.ALLOW
    }

    private fun unknownCallerVerdict(
        settings: CallScreeningSettings,
        number: String,
        wasRecentlyDialed: (String) -> Boolean,
        isRepeatedCall: (String) -> Boolean,
    ): CallScreeningVerdict = when {
        settings.allowRecentlyDialed && wasRecentlyDialed(number) -> CallScreeningVerdict.ALLOW
        settings.allowRepeatedCalls && isRepeatedCall(number) -> CallScreeningVerdict.ALLOW
        settings.silenceNumbersNotInContacts -> CallScreeningVerdict.SILENCE_NOT_IN_CONTACTS
        else -> CallScreeningVerdict.BLOCK_NOT_IN_CONTACTS
    }
}
