package com.bookverse.contacts.domain.call

object SipAddressPolicy {
    private val SCHEME_PREFIXES = listOf("sip:", "sips:")

    fun addressOrNull(raw: String): String? {
        val trimmed = raw.trim()
        val prefix = SCHEME_PREFIXES.firstOrNull { trimmed.startsWith(it, ignoreCase = true) }
        val address = prefix?.let { trimmed.substring(it.length) } ?: trimmed
        return when {
            address.isEmpty() || address.any(Char::isWhitespace) -> null
            prefix == null && !isUserAtHost(address) -> null
            else -> address
        }
    }

    fun isSipAddress(raw: String): Boolean = addressOrNull(raw) != null

    fun dialTarget(address: String): String = "sip:$address"

    private fun isUserAtHost(value: String): Boolean {
        val separator = value.indexOf('@')
        return separator > 0 && separator == value.lastIndexOf('@') && separator < value.lastIndex
    }
}
