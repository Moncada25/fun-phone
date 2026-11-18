package com.bookverse.contacts.data.notes

import java.text.Normalizer
import java.util.Locale

private val HIDDEN_BLOCK_REGEX = Regex(
    "<!--.*?-->|<(script|style|head|title)\\b[^>]*>.*?</\\1\\s*>",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val BREAKING_TAG_REGEX = Regex(
    "</?(p|div|br|li|ul|ol|h[1-6]|tr|td|th|table|blockquote|pre|hr|section|article|header|footer)\\b[^>]*>",
    RegexOption.IGNORE_CASE,
)
private val ANY_TAG_REGEX = Regex("<[a-zA-Z/!][^>]*>")
private val ENTITY_REGEX = Regex("&(#[xX][0-9a-fA-F]{1,6}|#[0-9]{1,7}|[a-zA-Z][a-zA-Z0-9]{1,10});")
private val WHITESPACE_REGEX = Regex("[\\s\\u00A0\\u2007\\u202F]+")
private val COMBINING_MARKS_REGEX = Regex("\\p{Mn}+")

private val NAMED_ENTITIES: Map<String, String> = mapOf(
    "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
    "iexcl" to "¡", "iquest" to "¿", "copy" to "©", "reg" to "®", "euro" to "€",
    "hellip" to "…", "ndash" to "–", "mdash" to "—", "deg" to "°", "middot" to "·",
    "lsquo" to "‘", "rsquo" to "’", "ldquo" to "“", "rdquo" to "”", "laquo" to "«", "raquo" to "»",
    "aacute" to "á", "eacute" to "é", "iacute" to "í", "oacute" to "ó", "uacute" to "ú",
    "Aacute" to "Á", "Eacute" to "É", "Iacute" to "Í", "Oacute" to "Ó", "Uacute" to "Ú",
    "agrave" to "à", "egrave" to "è", "igrave" to "ì", "ograve" to "ò", "ugrave" to "ù",
    "Agrave" to "À", "Egrave" to "È", "Igrave" to "Ì", "Ograve" to "Ò", "Ugrave" to "Ù",
    "acirc" to "â", "ecirc" to "ê", "icirc" to "î", "ocirc" to "ô", "ucirc" to "û",
    "Acirc" to "Â", "Ecirc" to "Ê", "Icirc" to "Î", "Ocirc" to "Ô", "Ucirc" to "Û",
    "auml" to "ä", "euml" to "ë", "iuml" to "ï", "ouml" to "ö", "uuml" to "ü",
    "Auml" to "Ä", "Euml" to "Ë", "Iuml" to "Ï", "Ouml" to "Ö", "Uuml" to "Ü",
    "atilde" to "ã", "otilde" to "õ", "ntilde" to "ñ",
    "Atilde" to "Ã", "Otilde" to "Õ", "Ntilde" to "Ñ",
    "ccedil" to "ç", "Ccedil" to "Ç", "szlig" to "ß", "oelig" to "œ", "OElig" to "Œ",
    "aelig" to "æ", "AElig" to "Æ", "oslash" to "ø", "Oslash" to "Ø", "aring" to "å", "Aring" to "Å",
)

fun decodeHtmlEntities(text: String): String {
    if ('&' !in text) return text
    return ENTITY_REGEX.replace(text) { match ->
        val body = match.groupValues[1]
        when {
            body.startsWith("#x") || body.startsWith("#X") ->
                body.substring(2).toIntOrNull(16)?.let(::codePointToString)
            body.startsWith("#") -> body.substring(1).toIntOrNull()?.let(::codePointToString)
            else -> NAMED_ENTITIES[body]
        } ?: match.value
    }
}

private fun codePointToString(codePoint: Int): String? =
    if (Character.isValidCodePoint(codePoint) && codePoint != 0) {
        String(Character.toChars(codePoint))
    } else {
        null
    }

fun htmlToVisibleText(html: String): String {
    if (html.isEmpty()) return ""
    val withoutHidden = HIDDEN_BLOCK_REGEX.replace(html, " ")
    val withBreaks = BREAKING_TAG_REGEX.replace(withoutHidden, " ")
    val withoutTags = ANY_TAG_REGEX.replace(withBreaks, "")
    return WHITESPACE_REGEX.replace(decodeHtmlEntities(withoutTags), " ").trim()
}

fun normalizeForSearch(text: String): String {
    if (text.isEmpty()) return ""
    val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
    val stripped = COMBINING_MARKS_REGEX.replace(decomposed, "")
    return WHITESPACE_REGEX.replace(stripped.lowercase(Locale.ROOT), " ").trim()
}

fun buildNoteSearchText(
    note: Note,
    checklist: List<ChecklistItem> = emptyList(),
    reminder: ReminderData? = null,
    recording: RecordingData? = null,
): String {
    if (note.isLocked) return normalizeForSearch(note.title)
    val parts = buildList {
        add(note.title)
        when {
            note.isChecklist -> checklist.forEach { add(it.text) }
            note.isReminder -> add(reminder?.message ?: note.content)
            note.isRecording -> add(recording?.transcription.orEmpty())
            else -> add(htmlToVisibleText(note.content))
        }
    }
    return normalizeForSearch(parts.filter { it.isNotBlank() }.joinToString("\n"))
}

fun buildNoteSearchIndex(
    notes: List<Note>,
    checklistsByNoteId: Map<Long, List<ChecklistItem>>,
    remindersByNoteId: Map<Long, ReminderData>,
    recordingsByNoteId: Map<Long, RecordingData>,
): Map<Long, String> = notes.associate { note ->
    note.id to buildNoteSearchText(
        note = note,
        checklist = checklistsByNoteId[note.id].orEmpty(),
        reminder = remindersByNoteId[note.id],
        recording = recordingsByNoteId[note.id],
    )
}

fun searchQueryTerms(query: String): List<String> =
    normalizeForSearch(query).split(' ').filter { it.isNotEmpty() }

fun matchesNoteSearch(searchText: String, queryTerms: List<String>): Boolean =
    queryTerms.isNotEmpty() && queryTerms.all { it in searchText }

fun searchNotes(
    notes: List<Note>,
    query: String,
    searchIndex: Map<Long, String>,
): List<Note> {
    val terms = searchQueryTerms(query)
    if (terms.isEmpty()) return emptyList()
    return notes.filter { note ->
        matchesNoteSearch(searchIndex[note.id] ?: normalizeForSearch(note.title), terms)
    }
}
