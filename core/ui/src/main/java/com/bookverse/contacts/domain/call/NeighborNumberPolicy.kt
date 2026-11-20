package com.bookverse.contacts.domain.call

object NeighborNumberPolicy {
    const val MIN_NATIONAL_DIGITS = 9
    const val VARYING_TRAILING_DIGITS = 4

    fun resemblesOwnNumber(incoming: String?, ownNumbers: List<String>): Boolean {
        val incomingDigits = significantDigits(incoming)
        if (incomingDigits.length < MIN_NATIONAL_DIGITS) return false
        return ownNumbers.any { own -> sharesLeadingDigits(incomingDigits, significantDigits(own)) }
    }

    private fun sharesLeadingDigits(first: String, second: String): Boolean {
        val length = minOf(first.length, second.length)
        if (length < MIN_NATIONAL_DIGITS) return false
        val sharedLength = length - VARYING_TRAILING_DIGITS
        return first.takeLast(length).take(sharedLength) == second.takeLast(length).take(sharedLength)
    }

    private fun significantDigits(number: String?): String =
        number.orEmpty().filter(Char::isDigit).trimStart('0')
}
