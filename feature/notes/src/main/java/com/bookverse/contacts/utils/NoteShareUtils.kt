package com.bookverse.contacts.utils

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import com.bookverse.contacts.data.notes.ChecklistItem
import com.bookverse.contacts.data.notes.Note
import com.bookverse.contacts.data.notes.RecordingData
import com.bookverse.contacts.data.notes.ReminderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import com.bookverse.contacts.core.ui.R as CoreUiR

object NoteShareUtils {

    data class NoteSharePayload(
        val fileName: String,
        val mimeType: String,
        val bytes: ByteArray
    )

    fun buildNoteHtmlDocument(note: Note, htmlContent: String): String {
        val noteType = when {
            note.isChecklist -> "checklist"
            note.isReminder -> "reminder"
            note.isRecording -> "recording"
            else -> "note"
        }
        return """
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="UTF-8">
            <meta name="note-id" content="${note.id}">
            <meta name="note-timestamp" content="${note.timestamp}">
            <meta name="note-type" content="$noteType">
            <meta name="note-locked" content="${note.isLocked}">
            <meta name="note-folder-id" content="${note.folderId}">
            <title>${note.title}</title>
            </head>
            <body>
            $htmlContent
            </body>
            </html>
        """.trimIndent()
    }

    fun buildNotePayload(note: Note, htmlContent: String): NoteSharePayload {
        val content = buildNoteHtmlDocument(note, htmlContent)
        val fileName = buildFileName("note", note.title, note.timestamp, "html")
        return NoteSharePayload(
            fileName = fileName,
            mimeType = "text/html",
            bytes = content.toByteArray(Charsets.UTF_8)
        )
    }

    fun buildChecklistPayload(note: Note, checklist: List<ChecklistItem>): NoteSharePayload {
        val payload = NotesBackupUtils.buildChecklistExportPayload(
            checklists = listOf(note to checklist),
            generatedAtMillis = System.currentTimeMillis()
        )
        val fileName = buildFileName("checklist", note.title, note.timestamp, "json")
        return NoteSharePayload(
            fileName = fileName,
            mimeType = "application/json",
            bytes = payload.toByteArray(Charsets.UTF_8)
        )
    }

    fun buildReminderPayload(note: Note, reminderData: ReminderData): NoteSharePayload {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
        val jsonObject = JSONObject().apply {
            put("title", note.title)
            put("message", reminderData.message)
            put("expirationDate", dateFormatter.format(Date(reminderData.date)))
            put("interval", reminderData.interval)
            put("creationDate", dateFormatter.format(Date(reminderData.creationDate)))
            put("type", "reminder")
        }
        val fileName = buildFileName(
            prefix = "reminder",
            title = note.title,
            timestamp = reminderData.date.takeIf { it > 0 } ?: System.currentTimeMillis(),
            extension = "json"
        )
        return NoteSharePayload(
            fileName = fileName,
            mimeType = "application/json",
            bytes = jsonObject.toString(2).toByteArray(Charsets.UTF_8)
        )
    }

    suspend fun buildRecordingPayload(note: Note, recordingData: RecordingData): NoteSharePayload =
        withContext(Dispatchers.IO) {
            val audioFile = File(recordingData.filePath)
            if (!audioFile.exists()) {
                throw IllegalStateException("Recording file not found")
            }
            val bytes = audioFile.readBytes()
            val ext = audioFile.extension.ifBlank { "m4a" }
            val mime = when (ext.lowercase(Locale.ROOT)) {
                "mp3" -> "audio/mpeg"
                "wav" -> "audio/wav"
                "ogg" -> "audio/ogg"
                "m4a", "aac" -> "audio/mp4"
                else -> "audio/*"
            }
            val timestamp = if (recordingData.createdAt != 0L) recordingData.createdAt else System.currentTimeMillis()
            NoteSharePayload(
                fileName = buildFileName("recording", note.title, timestamp, ext),
                mimeType = mime,
                bytes = bytes
            )
        }

    suspend fun shareChecklist(
        context: Context,
        note: Note,
        checklist: List<ChecklistItem>,
        shareNoteLauncher: ActivityResultLauncher<Intent>
    ) {
        val payload = buildChecklistPayload(note, checklist)
        val jsonFile = writePayloadToTempFile(context, payload)
        launchShareIntent(context, jsonFile, payload.mimeType, shareNoteLauncher)
    }

    suspend fun shareReminder(
        context: Context,
        note: Note,
        reminderData: ReminderData,
        shareNoteLauncher: ActivityResultLauncher<Intent>
    ) {
        val payload = buildReminderPayload(note, reminderData)
        val jsonFile = writePayloadToTempFile(context, payload)
        launchShareIntent(context, jsonFile, payload.mimeType, shareNoteLauncher)
    }

    suspend fun shareRecording(
        context: Context,
        note: Note,
        recordingData: RecordingData,
        shareNoteLauncher: ActivityResultLauncher<Intent>
    ) {
        withContext(Dispatchers.Main) {
            val audioFile = File(recordingData.filePath)
            if (audioFile.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    context.packageName + ".provider",
                    audioFile
                )
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "audio/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, note.title)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                shareNoteLauncher.launch(
                    Intent.createChooser(
                        shareIntent,
                        context.getString(CoreUiR.string.share)
                    )
                )
            }
        }
    }

    suspend fun shareNote(
        context: Context,
        note: Note,
        htmlContent: String,
        shareNoteLauncher: ActivityResultLauncher<Intent>
    ) {
        val payload = buildNotePayload(note, htmlContent)
        val htmlFile = writePayloadToTempFile(context, payload)
        launchShareIntent(context, htmlFile, payload.mimeType, shareNoteLauncher)
    }

    private suspend fun writePayloadToTempFile(
        context: Context,
        payload: NoteSharePayload
    ): File = withContext(Dispatchers.IO) {
        File(context.cacheDir, "${UUID.randomUUID()}_${payload.fileName}").also { file ->
            FileOutputStream(file).use { it.write(payload.bytes) }
        }
    }

    private suspend fun launchShareIntent(
        context: Context,
        file: File,
        mimeType: String,
        shareNoteLauncher: ActivityResultLauncher<Intent>
    ) = withContext(Dispatchers.Main) {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            file
        )
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        shareNoteLauncher.launch(
            Intent.createChooser(
                shareIntent,
                context.getString(CoreUiR.string.share)
            )
        )
    }

    private fun buildFileName(prefix: String, title: String, timestamp: Long, extension: String): String {
        val datePart = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date(timestamp))
        val slug = title.lowercase(Locale.ROOT)
            .replace("[^a-z0-9]+".toRegex(), "_")
            .trim('_')
            .take(40)
            .ifBlank { prefix }
        return "${prefix}_${datePart}_${slug}.$extension"
    }
}
