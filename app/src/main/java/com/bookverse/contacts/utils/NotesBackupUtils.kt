package com.bookverse.contacts.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import com.bookverse.contacts.R
import com.bookverse.contacts.data.notes.ChecklistItem
import com.bookverse.contacts.data.notes.Note
import com.bookverse.contacts.data.notes.NoteDao
import com.bookverse.contacts.data.notes.NoteFolder
import com.bookverse.contacts.data.notes.NoteImage
import com.bookverse.contacts.data.notes.NoteTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object NotesBackupUtils {

    data class ExportSummary(
        val notes: Int,
        val checklists: Int,
        val tags: Int = 0,
        val images: Int = 0,
    )

    data class ImportSummary(
        val notes: Int,
        val checklists: Int,
        val tags: Int = 0,
        val images: Int = 0,
    )

    private data class ParsedNote(
        val note: Note,
        val tagIds: List<Long>,
        val imageFileNames: List<String>,
    )

    private data class ParsedChecklist(
        val note: Note,
        val items: List<ChecklistItem>,
        val tagIds: List<Long>,
    )

    private const val MAX_IMPORT_BYTES = 1024L * 1024L * 1024L
    private const val MAX_TEXT_ENTRY_BYTES = 32L * 1024L * 1024L

    @SuppressLint("ConstantLocale")
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT)
    @SuppressLint("ConstantLocale")
    private val readableDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)

    suspend fun exportNotes(context: Context, noteDao: NoteDao, uri: Uri): ExportSummary =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val outputStream = resolver.openOutputStream(uri, "w")
                ?: resolver.openOutputStream(uri)
                ?: throw IOException("Unable to open output stream for backup")

            outputStream.use { stream ->
                ZipOutputStream(BufferedOutputStream(stream)).use { zip ->
                    writeBackupEntries(context, noteDao, zip)
                }
            }
        }

    suspend fun writeBackupEntries(
        context: Context,
        noteDao: NoteDao,
        zip: ZipOutputStream,
        prefix: String = "",
    ): ExportSummary = withContext(Dispatchers.IO) {
        val allNotes = noteDao.getAllNotesSync()
        val textNotes = allNotes.filter { !it.isChecklist && !it.isReminder && !it.isRecording }
        val checklistNotes = allNotes.filter { it.isChecklist }
        val exportedNoteIds = (textNotes + checklistNotes).map { it.id }.toSet()
        val tags = noteDao.getNoteTagsSync()
        val tagIdsByNoteId = noteDao.getNoteTagCrossRefsSync()
            .filter { it.noteId in exportedNoteIds }
            .groupBy({ it.noteId }, { it.tagId })
        val textNoteIds = textNotes.map { it.id }.toSet()
        val imagesByNoteId = noteDao.getAllNoteImagesSync()
            .filter { image ->
                image.noteId in textNoteIds &&
                    NotesImageStore.fileFor(context, image.fileName)?.isFile == true
            }
            .groupBy({ it.noteId }, { it.fileName })

        zip.writeTextEntry(
            prefix + NOTES_BACKUP_FOLDERS_ENTRY,
            buildFoldersExportPayload(noteDao.getNoteFoldersSync()),
        )
        zip.writeTextEntry(prefix + NOTES_BACKUP_TAGS_ENTRY, buildTagsExportPayload(tags))
        zip.writeTextEntry(
            prefix + NOTES_BACKUP_CHECKLISTS_ENTRY,
            buildChecklistExportPayload(
                checklists = checklistNotes.map { note ->
                    note to noteDao.getChecklistForNote(note.id)
                },
                tagIdsByNoteId = tagIdsByNoteId,
            ),
        )
        val usedNames = mutableSetOf<String>()
        textNotes.forEach { note ->
            val document = NoteShareUtils.buildNoteHtmlDocument(note, note.content)
                .replaceFirst(
                    "<title>",
                    buildNoteBackupMetaTags(
                        tagIds = tagIdsByNoteId[note.id].orEmpty(),
                        imageFileNames = imagesByNoteId[note.id].orEmpty(),
                    ) + "<title>",
                )
            zip.writeTextEntry(prefix + uniqueNoteFileName(note, usedNames), document)
        }
        var exportedImages = 0
        imagesByNoteId.values.flatten().distinct().forEach { fileName ->
            val file = NotesImageStore.fileFor(context, fileName) ?: return@forEach
            zip.putNextEntry(ZipEntry(prefix + NOTES_BACKUP_IMAGES_DIRECTORY + fileName))
            file.inputStream().use { input -> input.copyTo(zip) }
            zip.closeEntry()
            exportedImages++
        }

        ExportSummary(
            notes = textNotes.size,
            checklists = checklistNotes.size,
            tags = tags.size,
            images = exportedImages,
        )
    }

    suspend fun importNotes(context: Context, noteDao: NoteDao, uri: Uri): ImportSummary =
        withContext(Dispatchers.IO) {
            val temp = File.createTempFile("notes_import", ".tmp", context.cacheDir)
            try {
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("Unable to open input stream for backup")
                input.use { stream ->
                    temp.outputStream().use { output ->
                        try {
                            stream.copyBounded(output, MAX_IMPORT_BYTES)
                        } catch (error: IOException) {
                            throw IllegalArgumentException("Import file is too large", error)
                        }
                    }
                }
                if (temp.length() == 0L) throw IllegalArgumentException("Import file is empty")

                val header = temp.inputStream().use { stream ->
                    val buffer = ByteArray(256)
                    val count = stream.read(buffer).coerceAtLeast(0)
                    buffer.copyOf(count)
                }
                when {
                    header.isZipArchive() -> importFromZipFile(context, noteDao, temp)
                    temp.length() > MAX_TEXT_ENTRY_BYTES ->
                        throw IllegalArgumentException("Import file is too large")
                    header.looksLikeJson() ->
                        importFromJson(temp.readText(Charsets.UTF_8), noteDao, context)
                    header.looksLikeHtml() ->
                        importFromHtml(temp.readText(Charsets.UTF_8), noteDao, context)
                    else -> throw IllegalArgumentException("Unsupported notes backup format")
                }
            } finally {
                temp.delete()
            }
        }

    suspend fun importFromZipFile(
        context: Context,
        noteDao: NoteDao,
        file: File,
        prefix: String = "",
    ): ImportSummary = withContext(Dispatchers.IO) {
        val importedNotes = mutableListOf<ParsedNote>()
        val importedChecklists = mutableListOf<ParsedChecklist>()
        var importedImages = 0
        var tagMapping: Map<Long, Long> = emptyMap()

        ZipFile(file).use { zip ->
            val entries = zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.startsWith(prefix) }
                .associateBy { it.name.removePrefix(prefix) }

            val folderMapping = entries.findIgnoreCase(NOTES_BACKUP_FOLDERS_ENTRY)
                ?.let { importFolders(zip.readEntryAsString(it), noteDao) }
                .orEmpty()
            tagMapping = entries.findIgnoreCase(NOTES_BACKUP_TAGS_ENTRY)
                ?.let { importTags(zip.readEntryAsString(it), noteDao) }
                .orEmpty()

            entries.forEach { (name, entry) ->
                when {
                    name.startsWith(NOTES_BACKUP_IMAGES_DIRECTORY) -> Unit

                    name.endsWith(".html", ignoreCase = true) -> {
                        val html = zip.readEntryAsString(entry)
                        parseNoteHtml(html, context)?.let { parsed ->
                            importedNotes += parsed.copy(
                                note = parsed.note.copy(
                                    folderId = parsed.note.folderId?.let { folderMapping[it] },
                                ),
                            )
                        }
                    }

                    name.endsWith(".json", ignoreCase = true) &&
                        !name.equals(NOTES_BACKUP_FOLDERS_ENTRY, ignoreCase = true) &&
                        !name.equals(NOTES_BACKUP_TAGS_ENTRY, ignoreCase = true) -> {
                        importedChecklists += parseChecklistsJson(
                            zip.readEntryAsString(entry),
                            context,
                        ).map { parsed ->
                            parsed.copy(
                                note = parsed.note.copy(
                                    folderId = parsed.note.folderId?.let { folderMapping[it] },
                                ),
                            )
                        }
                    }
                }
            }

            if (importedNotes.isEmpty() && importedChecklists.isEmpty()) {
                throw IllegalArgumentException("No importable notes found in backup")
            }

            importedNotes.forEach { parsed ->
                val noteId = noteDao.insertNote(parsed.note.copy(id = 0))
                assignImportedTags(noteDao, noteId, parsed.tagIds, tagMapping)
                parsed.imageFileNames.forEach { fileName ->
                    val entry = entries[NOTES_BACKUP_IMAGES_DIRECTORY + fileName]
                    val extension = NotesImageStore.extensionOf(fileName)
                    if (entry != null && extension != null) {
                        val stored = zip.getInputStream(entry).use { input ->
                            NotesImageStore.importFromStream(context, input, extension)
                        }
                        if (stored != null) {
                            noteDao.insertNoteImage(NoteImage(noteId = noteId, fileName = stored))
                            importedImages++
                        }
                    }
                }
            }
        }

        importedChecklists.forEach { parsed ->
            val preparedItems = parsed.items.map { it.copy(id = 0, noteId = 0) }
            val noteId = noteDao.addNoteWithChecklist(parsed.note.copy(id = 0), preparedItems)
            assignImportedTags(noteDao, noteId, parsed.tagIds, tagMapping)
        }

        ImportSummary(
            notes = importedNotes.size,
            checklists = importedChecklists.size,
            tags = tagMapping.size,
            images = importedImages,
        )
    }

    private suspend fun assignImportedTags(
        noteDao: NoteDao,
        noteId: Long,
        backupTagIds: List<Long>,
        tagMapping: Map<Long, Long>,
    ) {
        val tagIds = backupTagIds.mapNotNull { tagMapping[it] }.distinct()
        if (tagIds.isNotEmpty()) noteDao.setNoteTags(noteId, tagIds)
    }

    private suspend fun importFromJson(json: String, noteDao: NoteDao, context: Context): ImportSummary {
        val checklists = parseChecklistsJson(json, context)
        if (checklists.isEmpty()) {
            throw IllegalArgumentException("No checklists found in JSON")
        }

        checklists.forEach { parsed ->
            val preparedItems = parsed.items.map { it.copy(id = 0, noteId = 0) }
            noteDao.addNoteWithChecklist(parsed.note.copy(id = 0), preparedItems)
        }

        return ImportSummary(notes = 0, checklists = checklists.size)
    }

    private suspend fun importFromHtml(html: String, noteDao: NoteDao, context: Context): ImportSummary {
        val parsed = parseNoteHtml(html, context) ?: throw IllegalArgumentException("Invalid note HTML")
        noteDao.insertNote(parsed.note.copy(id = 0, folderId = null))
        return ImportSummary(notes = 1, checklists = 0)
    }

    private fun buildFoldersExportPayload(folders: List<NoteFolder>): String {
        val foldersArray = JSONArray()
        folders.forEach { folder ->
            val folderObject = JSONObject().apply {
                put("id", folder.id)
                put("name", folder.name)
                put("description", folder.description ?: JSONObject.NULL)
                put("colorHex", folder.colorHex)
                put("createdAt", folder.createdAt)
            }
            foldersArray.put(folderObject)
        }

        val root = JSONObject().apply {
            put("type", "folders")
            put("version", 1)
            put("generatedAt", readableDateFormat.format(Date()))
            put("folders", foldersArray)
        }

        return root.toString(2)
    }

    private fun buildTagsExportPayload(tags: List<NoteTag>): String {
        val tagsArray = JSONArray()
        tags.forEach { tag ->
            tagsArray.put(
                JSONObject().apply {
                    put("id", tag.id)
                    put("name", tag.name)
                    put("colorHex", tag.colorHex)
                    put("createdAt", tag.createdAt)
                },
            )
        }
        return JSONObject().apply {
            put("type", "note_tags")
            put("version", 1)
            put("generatedAt", readableDateFormat.format(Date()))
            put("tags", tagsArray)
        }.toString(2)
    }

    private suspend fun importFolders(json: String, noteDao: NoteDao): Map<Long, Long> {
        val mapping = mutableMapOf<Long, Long>()
        val foldersArray = try {
            JSONObject(json).optJSONArray("folders")
        } catch (_: Exception) {
            null
        } ?: return mapping

        val existingFolders = noteDao.getNoteFoldersSync()

        for (i in 0 until foldersArray.length()) {
            val folderObj = foldersArray.optJSONObject(i) ?: continue
            val oldId = folderObj.optLong("id", -1L)
            if (oldId == -1L) continue

            val name = folderObj.optString("name")
            if (name.isBlank()) continue

            val descriptionStr = folderObj.optString("description")
            val description = if (descriptionStr.isNotEmpty() && descriptionStr != "null") descriptionStr else null

            val colorHex = folderObj.optString("colorHex", "#90CAF9")
            val createdAt = folderObj.optLong("createdAt", System.currentTimeMillis())

            val existingFolder = existingFolders.find { it.name.equals(name, ignoreCase = true) }
            if (existingFolder != null) {
                mapping[oldId] = existingFolder.id
            } else {
                val newFolder = NoteFolder(
                    name = name,
                    description = description,
                    colorHex = colorHex,
                    createdAt = createdAt
                )
                val newId = noteDao.insertNoteFolder(newFolder)
                mapping[oldId] = newId
            }
        }
        return mapping
    }

    private suspend fun importTags(json: String, noteDao: NoteDao): Map<Long, Long> {
        val mapping = mutableMapOf<Long, Long>()
        val tagsArray = try {
            JSONObject(json).optJSONArray("tags")
        } catch (_: Exception) {
            null
        } ?: return mapping

        for (i in 0 until tagsArray.length()) {
            val tagObject = tagsArray.optJSONObject(i) ?: continue
            val oldId = tagObject.optLong("id", -1L)
            val name = tagObject.optString("name").trim()
            if (oldId == -1L || name.isEmpty()) continue

            val existing = noteDao.findNoteTagByName(name)
            val newId = existing?.id ?: noteDao.insertNoteTag(
                NoteTag(
                    name = name,
                    colorHex = tagObject.optString("colorHex", "#2196F3"),
                    createdAt = tagObject.optLong("createdAt", System.currentTimeMillis()),
                ),
            )
            if (newId > 0L) mapping[oldId] = newId
        }
        return mapping
    }

    fun buildChecklistExportPayload(
        checklists: List<Pair<Note, List<ChecklistItem>>>,
        generatedAtMillis: Long = System.currentTimeMillis(),
        tagIdsByNoteId: Map<Long, Collection<Long>> = emptyMap(),
    ): String {
        val listsArray = JSONArray()
        checklists.forEach { (note, items) ->
            val itemsArray = JSONArray().apply {
                items.forEach { item ->
                    put(
                        JSONObject().apply {
                            put("text", item.text)
                            put("isChecked", item.isChecked)
                        }
                    )
                }
            }
            val noteObject = JSONObject().apply {
                put("title", note.title)
                put("timestamp", note.timestamp)
                put("formattedDate", readableDateFormat.format(Date(note.timestamp)))
                put("isLocked", note.isLocked)
                note.folderId?.let { put("folderId", it) }
                tagIdsByNoteId[note.id]?.takeIf { it.isNotEmpty() }?.let { tagIds ->
                    put("tagIds", JSONArray().apply { tagIds.forEach { put(it) } })
                }
                put("items", itemsArray)
            }
            listsArray.put(noteObject)
        }

        val root = JSONObject().apply {
            put("type", "checklists")
            put("version", 1)
            put("generatedAt", readableDateFormat.format(Date(generatedAtMillis)))
            put("lists", listsArray)
        }

        return root.toString(2)
    }

    private fun uniqueNoteFileName(note: Note, usedNames: MutableSet<String>): String {
        val prefix = fileDateFormat.format(Date(note.timestamp))
        val slug = note.title.lowercase(Locale.ROOT)
            .replace("[^a-z0-9]+".toRegex(), "_")
            .trim('_')
            .take(40)
        val base = "${prefix}_${slug.ifBlank { "note" }}"
        var candidate = "$base.html"
        var suffix = 2
        while (!usedNames.add(candidate)) {
            candidate = "${base}_$suffix.html"
            suffix++
        }
        return candidate
    }

    private fun ZipOutputStream.writeTextEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun ZipFile.readEntryAsString(entry: ZipEntry): String {
        val output = ByteArrayOutputStream()
        getInputStream(entry).use { input ->
            try {
                input.copyBounded(output, MAX_TEXT_ENTRY_BYTES)
            } catch (error: IOException) {
                throw IllegalArgumentException("Backup entry is too large", error)
            }
        }
        return output.toString(Charsets.UTF_8.name())
    }

    private fun Map<String, ZipEntry>.findIgnoreCase(name: String): ZipEntry? =
        this[name] ?: entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value

    private fun parseNoteHtml(html: String, context: Context): ParsedNote? {
        val bodyMatch = BODY_REGEX.find(html)
        val content = bodyMatch?.groupValues?.get(1)?.trim() ?: return null

        val title = TITLE_REGEX.find(html)?.groupValues?.get(1)?.trim().orEmpty()
        val timestamp = META_TIMESTAMP_REGEX.find(html)?.groupValues?.get(1)?.toLongOrNull()
            ?: System.currentTimeMillis()
        val locked = META_LOCKED_REGEX.find(html)?.groupValues?.get(1)?.toBooleanStrictOrNull()
            ?: false
        val folderId = META_FOLDER_REGEX.find(html)?.groupValues?.get(1)?.toLongOrNull()

        val plainTitle = title.ifBlank { extractPlainSummary(content) }
            .ifBlank { context.getString(R.string.imported_note_title) }
        val metadata = parseNoteBackupMetadata(html)

        return ParsedNote(
            note = Note(
                title = plainTitle,
                content = content,
                timestamp = timestamp,
                isChecklist = false,
                isReminder = false,
                isRecording = false,
                isLocked = locked,
                folderId = folderId
            ),
            tagIds = metadata.tagIds,
            imageFileNames = metadata.imageFileNames,
        )
    }

    private fun parseChecklistsJson(json: String, context: Context): List<ParsedChecklist> {
        return try {
            val list = mutableListOf<ParsedChecklist>()
            val root = JSONObject(json)
            val listsArray = root.optJSONArray("lists") ?: JSONArray()
            for (i in 0 until listsArray.length()) {
                val noteObject = listsArray.optJSONObject(i) ?: continue
                val title = noteObject.optString("title")
                val timestamp = when {
                    noteObject.has("timestamp") -> noteObject.optLong("timestamp")
                    noteObject.has("formattedDate") -> parseDate(noteObject.optString("formattedDate"))
                    else -> System.currentTimeMillis()
                }
                val locked = noteObject.optBoolean("isLocked", false)
                val folderId = if (noteObject.has("folderId")) noteObject.optLong("folderId") else null
                val tagIdsArray = noteObject.optJSONArray("tagIds") ?: JSONArray()
                val tagIds = (0 until tagIdsArray.length()).map { tagIdsArray.optLong(it, -1L) }
                    .filter { it > 0L }
                val itemsArray = noteObject.optJSONArray("items") ?: JSONArray()
                val items = mutableListOf<ChecklistItem>()
                for (j in 0 until itemsArray.length()) {
                    val itemObject = itemsArray.optJSONObject(j) ?: continue
                    val text = itemObject.optString("text")
                    if (text.isBlank()) continue
                    val checked = itemObject.optBoolean("isChecked", false)
                    items += ChecklistItem(id = 0, noteId = 0, text = text, isChecked = checked)
                }
                if (title.isBlank() && items.isEmpty()) continue
                val derivedTitle =
                    title.ifBlank {
                        items.firstOrNull()?.text?.take(30)
                            ?: context.getString(R.string.imported_checklist_title)
                    }
                val note = Note(
                    title = derivedTitle,
                    content = "",
                    timestamp = timestamp,
                    isChecklist = true,
                    isReminder = false,
                    isRecording = false,
                    isLocked = locked,
                    folderId = folderId
                )
                list += ParsedChecklist(note = note, items = items, tagIds = tagIds)
            }
            list
        } catch (error: Exception) {
            if (error is IllegalArgumentException) throw error else throw IllegalArgumentException(
                "Invalid checklist JSON",
                error
            )
        }
    }

    private fun extractPlainSummary(html: String): String {
        val plain = html
            .replace("<[^>]+>".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
        return plain.take(30)
    }

    private fun String.toBooleanStrictOrNull(): Boolean? {
        return when (lowercase(Locale.ROOT)) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }

    private fun parseDate(raw: String): Long {
        return try {
            readableDateFormat.parse(raw)?.time ?: System.currentTimeMillis()
        } catch (_: ParseException) {
            System.currentTimeMillis()
        }
    }

    private val TITLE_REGEX =
        Regex("<title>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val BODY_REGEX = Regex(
        "<body[^>]*>([\\s\\S]*)</body>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val META_TIMESTAMP_REGEX = Regex(
        "<meta\\s+name=\"note-timestamp\"\\s+content=\"(.*?)\"[^>]*>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val META_LOCKED_REGEX = Regex(
        "<meta\\s+name=\"note-locked\"\\s+content=\"(.*?)\"[^>]*>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val META_FOLDER_REGEX = Regex(
        "<meta\\s+name=\"note-folder-id\"\\s+content=\"(.*?)\"[^>]*>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    private fun ByteArray.isZipArchive(): Boolean {
        return size >= 4 && this[0] == 'P'.code.toByte() && this[1] == 'K'.code.toByte()
    }

    private fun ByteArray.looksLikeJson(): Boolean {
        val preview = String(this, 0, minOf(size, 64), Charsets.UTF_8).trimStart()
        return preview.startsWith("{") || preview.startsWith("[")
    }

    private fun ByteArray.looksLikeHtml(): Boolean {
        val preview = String(this, 0, minOf(size, 256), Charsets.UTF_8).trimStart().lowercase(Locale.ROOT)
        return preview.startsWith("<!doctype") || preview.startsWith("<html") || preview.contains("<html")
    }
}
