package com.bookverse.contacts.utils

private val nameWord = Regex("[^\\s\\p{Z}]+")
private val initialLetter = Regex("\\p{L}\\p{M}*")

fun String.contactInitials(): String? {
    val initials = nameWord.findAll(this).mapNotNull { word ->
        initialLetter.find(word.value)?.value
    }.toList()
    val first = initials.firstOrNull() ?: return null
    return buildString {
        appendInitial(first)
        if (initials.size > 1) appendInitial(initials.last())
    }
}

private fun StringBuilder.appendInitial(letter: String) {
    val codePoint = letter.codePointAt(0)
    appendCodePoint(Character.toUpperCase(codePoint))
    append(letter, Character.charCount(codePoint), letter.length)
}

fun String.contactAvatarLabel(emptyLabel: String = "#"): String {
    contactInitials()?.let { return it }
    val trimmed = trim()
    return if (trimmed.isEmpty()) emptyLabel else String(Character.toChars(trimmed.codePointAt(0)))
}
