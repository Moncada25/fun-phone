package com.bookverse.contacts.domain.contacts

sealed interface BlockedNumberInputResult {
    data class Valid(val number: String) : BlockedNumberInputResult

    data object Empty : BlockedNumberInputResult

    data object InvalidCharacters : BlockedNumberInputResult

    data object TooShort : BlockedNumberInputResult

    data object TooLong : BlockedNumberInputResult

    data object Duplicate : BlockedNumberInputResult
}

object BlockedNumberInputPolicy {
    const val MIN_DIGITS = 3
    const val MAX_DIGITS = 17
    const val MAX_INPUT_LENGTH = 32

    private val separators = setOf(' ', '-', '(', ')', '.')

    fun limitInput(input: String): String = input.take(MAX_INPUT_LENGTH)

    fun validate(input: String, existingNumbers: Collection<String?>): BlockedNumberInputResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return BlockedNumberInputResult.Empty
        val hasPlus = trimmed.startsWith('+')
        val body = if (hasPlus) trimmed.drop(1) else trimmed
        if (body.any { !it.isAsciiDigit() && it !in separators }) {
            return BlockedNumberInputResult.InvalidCharacters
        }
        val digits = body.filter { it.isAsciiDigit() }
        if (digits.length < MIN_DIGITS) return BlockedNumberInputResult.TooShort
        if (digits.length > MAX_DIGITS) return BlockedNumberInputResult.TooLong
        val number = if (hasPlus) "+$digits" else digits
        val isDuplicate = existingNumbers.any { existing ->
            BlockedNumberMatchPolicy.isSameSubscriber(number, existing)
        }
        return if (isDuplicate) {
            BlockedNumberInputResult.Duplicate
        } else {
            BlockedNumberInputResult.Valid(number)
        }
    }

    private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
}
