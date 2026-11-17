package com.bookverse.contacts.utils

internal const val NOTES_BACKUP_FOLDERS_ENTRY = "folders.json"
internal const val NOTES_BACKUP_TAGS_ENTRY = "tags.json"
internal const val NOTES_BACKUP_CHECKLISTS_ENTRY = "checklists.json"
internal const val NOTES_BACKUP_IMAGES_DIRECTORY = "images/"

internal data class NoteBackupMetadata(
    val tagIds: List<Long>,
    val imageFileNames: List<String>,
)

internal fun buildNoteBackupMetaTags(
    tagIds: Collection<Long>,
    imageFileNames: Collection<String>,
): String = buildString {
    if (tagIds.isNotEmpty()) {
        append("<meta name=\"note-tag-ids\" content=\"")
        append(tagIds.joinToString(","))
        append("\">\n")
    }
    val safeImages = imageFileNames.filter(NotesImageStore::isValidFileName)
    if (safeImages.isNotEmpty()) {
        append("<meta name=\"note-images\" content=\"")
        append(safeImages.joinToString(","))
        append("\">\n")
    }
}

internal fun parseNoteBackupMetadata(html: String): NoteBackupMetadata {
    val head = html.substringBefore("<body", html)
    val tagIds = META_TAG_IDS_REGEX.find(head)?.groupValues?.get(1).orEmpty()
        .split(',')
        .mapNotNull { it.trim().toLongOrNull() }
        .distinct()
    val imageFileNames = META_IMAGES_REGEX.find(head)?.groupValues?.get(1).orEmpty()
        .split(',')
        .map { it.trim() }
        .filter(NotesImageStore::isValidFileName)
        .distinct()
    return NoteBackupMetadata(tagIds = tagIds, imageFileNames = imageFileNames)
}

private val META_TAG_IDS_REGEX = Regex(
    "<meta\\s+name=\"note-tag-ids\"\\s+content=\"([^\"]*)\"[^>]*>",
    RegexOption.IGNORE_CASE,
)
private val META_IMAGES_REGEX = Regex(
    "<meta\\s+name=\"note-images\"\\s+content=\"([^\"]*)\"[^>]*>",
    RegexOption.IGNORE_CASE,
)
