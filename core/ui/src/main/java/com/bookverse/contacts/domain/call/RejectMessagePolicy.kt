package com.bookverse.contacts.domain.call

object RejectMessagePolicy {
    const val MAX_LENGTH = 160

    fun limitInput(text: String): String = text.take(MAX_LENGTH)

    fun sanitize(text: String?): String? =
        text
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.take(MAX_LENGTH)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    fun canSend(text: String?, callSupportsTextResponse: Boolean): Boolean =
        callSupportsTextResponse && sanitize(text) != null
}
