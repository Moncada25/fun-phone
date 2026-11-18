package com.bookverse.contacts.ui.screens.dialer

import com.bookverse.contacts.data.model.Contact

internal data class DialerEditValue(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int = selectionStart,
)

internal sealed interface DialerActionResolution {
    data object Empty : DialerActionResolution
    data class RestoreLastDialed(val number: String) : DialerActionResolution
    data class Execute(val targetNumber: String) : DialerActionResolution
}

internal fun sanitizeDialerText(value: String): String = buildString {
    value.forEach { character ->
        if (isAllowedDialerCharacter(character) || (character == '+' && isEmpty())) {
            append(character)
        }
    }
}

internal fun sanitizePastedDialerText(
    rawText: String,
    rawCursor: Int,
    maxLength: Int = MAX_DIALED_NUMBERS,
): DialerEditValue {
    val safeCursor = rawCursor.coerceIn(0, rawText.length)
    val filtered = sanitizeDialerText(rawText).take(maxLength)
    val filteredCursor = sanitizeDialerText(rawText.take(safeCursor)).length
        .coerceAtMost(filtered.length)
    return DialerEditValue(
        text = filtered,
        selectionStart = filteredCursor,
    )
}

internal fun insertDialerText(
    current: DialerEditValue,
    insertedText: String,
    maxLength: Int = MAX_DIALED_NUMBERS,
): DialerEditValue {
    val safeStart = minOf(current.selectionStart, current.selectionEnd)
        .coerceIn(0, current.text.length)
    val safeEnd = maxOf(current.selectionStart, current.selectionEnd)
        .coerceIn(safeStart, current.text.length)
    val safeInsertion = sanitizeDialerText(insertedText)
    if (safeInsertion.isEmpty()) return current
    val prospective =
        current.text.take(safeStart) + safeInsertion + current.text.substring(safeEnd)
    if (prospective.length > maxLength) return current
    return sanitizePastedDialerText(prospective, safeStart + safeInsertion.length, maxLength)
}

internal fun deleteDialerText(
    current: DialerEditValue,
    deleteAll: Boolean = false,
): DialerEditValue {
    if (deleteAll) return DialerEditValue("", 0)
    val safeStart = minOf(current.selectionStart, current.selectionEnd)
        .coerceIn(0, current.text.length)
    val safeEnd = maxOf(current.selectionStart, current.selectionEnd)
        .coerceIn(safeStart, current.text.length)
    return when {
        safeStart != safeEnd -> DialerEditValue(
            text = current.text.removeRange(safeStart, safeEnd),
            selectionStart = safeStart,
        )
        safeStart > 0 -> DialerEditValue(
            text = current.text.removeRange(safeStart - 1, safeStart),
            selectionStart = safeStart - 1,
        )
        else -> current
    }
}

internal fun dialerCallingCodePrefix(
    rawNumber: String,
    countryCode: String?,
): String? {
    if (countryCode.isNullOrBlank()) return null
    val sanitized = sanitizeDialerText(rawNumber)
    val hasOwnPrefix = sanitized.startsWith("+") || sanitized.startsWith("*") || sanitized.startsWith("#")
    val digitCount = sanitized.count(Char::isDigit)
    val isShortNumber = digitCount in 1 until MIN_DIGITS_FOR_CALLING_CODE
    return countryCode.takeUnless { hasOwnPrefix || isShortNumber }
}

internal const val MIN_DIGITS_FOR_CALLING_CODE = 7

internal fun buildDialerTarget(
    rawNumber: String,
    countryCode: String?,
): String {
    val sanitized = sanitizeDialerText(rawNumber)
    return dialerCallingCodePrefix(sanitized, countryCode).orEmpty() + sanitized
}

internal fun areDialerNumbersEquivalent(
    candidateNumber: String,
    normalizedTarget: String,
    countryCallingCodeDigits: Collection<String>,
): Boolean {
    val normalizedCandidate = candidateNumber.filter(Char::isDigit)
    if (normalizedCandidate == normalizedTarget) return true
    val (longer, shorter) = if (normalizedCandidate.length >= normalizedTarget.length) {
        normalizedCandidate to normalizedTarget
    } else {
        normalizedTarget to normalizedCandidate
    }
    if (shorter.isEmpty() || longer.length == shorter.length || !longer.endsWith(shorter)) return false
    return longer.dropLast(shorter.length) in countryCallingCodeDigits
}

internal fun findExactDialerContact(
    targetNumber: String,
    contacts: List<Contact>,
    countryCallingCodeDigits: Collection<String>,
): Contact? {
    if (targetNumber.any { it == '*' || it == '#' }) return null
    val normalizedTarget = targetNumber.filter(Char::isDigit)
    if (normalizedTarget.isEmpty()) return null
    return contacts.firstOrNull { contact ->
        contact.phones.any { phone ->
            phone.number.isNotBlank() &&
                areDialerNumbersEquivalent(phone.number, normalizedTarget, countryCallingCodeDigits)
        }
    }
}

internal fun resolveDialerAction(
    currentNumber: String,
    lastDialedNumber: String?,
    countryCode: String?,
): DialerActionResolution {
    val current = sanitizeDialerText(currentNumber)
    if (current.isNotBlank()) {
        return DialerActionResolution.Execute(
            buildDialerTarget(current, countryCode),
        )
    }
    val previous = sanitizeDialerText(lastDialedNumber.orEmpty())
    return if (previous.isBlank()) {
        DialerActionResolution.Empty
    } else {
        DialerActionResolution.RestoreLastDialed(previous)
    }
}

private fun isAllowedDialerCharacter(character: Char): Boolean =
    character.isDigit() || character == '*' || character == '#'
