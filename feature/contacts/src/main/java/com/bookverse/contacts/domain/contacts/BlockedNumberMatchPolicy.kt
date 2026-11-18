package com.bookverse.contacts.domain.contacts

object BlockedNumberMatchPolicy {
    fun keys(rawNumber: String?, normalizedNumber: String? = null): Set<String> {
        if (rawNumber.isNullOrBlank() && normalizedNumber.isNullOrBlank()) return emptySet()
        val raw = rawNumber?.trim().orEmpty()
        val normalized = normalizedNumber?.trim().orEmpty()
        val digits = sequenceOf(normalized, raw)
            .map { value -> value.filter(Char::isDigit) }
            .firstOrNull { it.isNotEmpty() }

        return buildSet {
            raw.takeIf { it.isNotEmpty() }?.let(::add)
            raw.replace("\\s+".toRegex(), "").takeIf { it.isNotEmpty() }?.let(::add)
            normalized.takeIf { it.isNotEmpty() }?.let(::add)
            digits?.let(::add)
            digits?.takeIf { it.length > 10 }?.takeLast(10)?.let(::add)
            digits?.takeIf { it.length > 7 }?.takeLast(7)?.let(::add)
        }
    }

    fun matches(
        firstRaw: String?,
        firstNormalized: String?,
        secondRaw: String?,
        secondNormalized: String?,
    ): Boolean {
        val firstKeys = keys(firstRaw, firstNormalized)
        return firstKeys.isNotEmpty() &&
            keys(secondRaw, secondNormalized).any(firstKeys::contains)
    }

    fun isSameSubscriber(first: String?, second: String?): Boolean {
        val firstDigits = first.orEmpty().filter(Char::isDigit)
        val secondDigits = second.orEmpty().filter(Char::isDigit)
        if (firstDigits.isEmpty() || secondDigits.isEmpty()) return false
        if (firstDigits == secondDigits) return true
        val shorter = if (firstDigits.length <= secondDigits.length) firstDigits else secondDigits
        val longer = if (firstDigits.length <= secondDigits.length) secondDigits else firstDigits
        val national = shorter.trimStart('0')
        return national.length >= MIN_SUBSCRIBER_DIGITS && longer.endsWith(national)
    }

    private const val MIN_SUBSCRIBER_DIGITS = 7
}
