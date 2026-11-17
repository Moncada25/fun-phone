package com.bookverse.contacts.domain.contacts.vcard

data class VCardPhone(
    val number: String,
    val types: Set<String> = emptySet(),
)

data class VCardDate(
    val year: Int?,
    val month: Int,
    val day: Int,
)

data class VCardContact(
    val formattedName: String = "",
    val familyName: String = "",
    val givenName: String = "",
    val additionalName: String = "",
    val prefix: String = "",
    val suffix: String = "",
    val phones: List<VCardPhone> = emptyList(),
    val emails: List<String> = emptyList(),
    val organization: String? = null,
    val note: String? = null,
    val birthday: VCardDate? = null,
    val urls: List<String> = emptyList(),
) {
    val structuredName: String
        get() = listOf(prefix, givenName, additionalName, familyName, suffix)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .joinToString(" ")

    val displayName: String
        get() = formattedName.trim()
            .ifEmpty { structuredName }
            .ifEmpty { organization?.trim().orEmpty() }
}

data class VCardParseResult(
    val contacts: List<VCardContact>,
    val failed: Int,
)
