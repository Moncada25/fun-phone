package com.bookverse.contacts.domain.call

import androidx.compose.runtime.Immutable

@Immutable
data class CallScreeningSettings(
    val blockListedNumbers: Boolean = false,
    val blockHiddenNumbers: Boolean = false,
    val blockNumbersNotInContacts: Boolean = false,
) {
    val anyRuleEnabled: Boolean
        get() = blockListedNumbers || blockHiddenNumbers || blockNumbersNotInContacts

    val enabledRuleCount: Int
        get() = listOf(blockListedNumbers, blockHiddenNumbers, blockNumbersNotInContacts).count { it }
}

enum class CallScreeningVerdict {
    ALLOW,
    BLOCK_LISTED,
    BLOCK_HIDDEN,
    BLOCK_NOT_IN_CONTACTS,
    ;

    val blocksCall: Boolean
        get() = this != ALLOW
}

object CallScreeningPolicy {
    fun isHiddenNumber(number: String?, presentationAllowed: Boolean): Boolean =
        !presentationAllowed || number.isNullOrBlank()

    fun screen(
        settings: CallScreeningSettings,
        isIncoming: Boolean,
        number: String?,
        presentationAllowed: Boolean,
        isOnBlockedList: (String) -> Boolean,
        isInContacts: (String) -> Boolean?,
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
        if (settings.blockNumbersNotInContacts && isInContacts(visibleNumber) == false) {
            return CallScreeningVerdict.BLOCK_NOT_IN_CONTACTS
        }
        return CallScreeningVerdict.ALLOW
    }
}
