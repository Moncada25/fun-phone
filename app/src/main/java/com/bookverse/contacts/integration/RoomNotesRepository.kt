package com.bookverse.contacts.integration

import android.content.Context
import android.net.Uri
import com.bookverse.contacts.data.db.AppDatabase
import com.bookverse.contacts.data.notes.ChecklistItem
import com.bookverse.contacts.data.notes.Note
import com.bookverse.contacts.data.notes.NoteFolder
import com.bookverse.contacts.data.notes.NoteTag
import com.bookverse.contacts.data.notes.NotesExportSummary
import com.bookverse.contacts.data.notes.NotesImportSummary
import com.bookverse.contacts.data.notes.NotesRepository
import com.bookverse.contacts.data.notes.ReminderData
import com.bookverse.contacts.data.notes.RecordingData
import com.bookverse.contacts.utils.NotesBackupUtils
import com.bookverse.contacts.utils.NotesImageStore

class RoomNotesRepository(context: Context) : NotesRepository {
    private val appContext = context.applicationContext
    private val noteDao = AppDatabase.getInstance(appContext).noteDao()

    override val notes = noteDao.getAllNotes()
    override val deletedNotes = noteDao.getDeletedNotes()
    override val noteFolders = noteDao.getNoteFolders()
    override val checklistItems = noteDao.getAllChecklistItems()
    override val reminderData = noteDao.getAllReminderData()
    override val recordingData = noteDao.getAllRecordingData()
    override val noteTags = noteDao.getNoteTags()
    override val noteTagCrossRefs = noteDao.getNoteTagCrossRefs()

    override suspend fun findById(noteId: Long): Note? = noteDao.getNoteById(noteId)

    override suspend fun getChecklistForNote(noteId: Long): List<ChecklistItem> =
        noteDao.getChecklistForNote(noteId)

    override suspend fun getReminderDataForNote(noteId: Long): ReminderData? =
        noteDao.getReminderDataForNote(noteId)

    override suspend fun getRecordingDataForNote(noteId: Long): RecordingData? =
        noteDao.getRecordingDataForNote(noteId)

    override suspend fun markAsDeleted(noteId: Long) = noteDao.markAsDeleted(noteId)

    override suspend fun restoreNote(noteId: Long) = noteDao.restoreNote(noteId)

    override suspend fun permanentlyDeleteNote(noteId: Long) {
        val imageFileNames = noteDao.getImagesForNote(noteId).map { it.fileName }
        noteDao.permanentlyDeleteNote(noteId)
        NotesImageStore.delete(appContext, imageFileNames)
    }

    override suspend fun addNoteWithChecklist(
        note: Note,
        checklist: List<ChecklistItem>,
    ): Long = noteDao.addNoteWithChecklist(note, checklist)

    override suspend fun updateNoteWithChecklist(
        note: Note,
        checklist: List<ChecklistItem>,
    ) = noteDao.updateNoteWithChecklist(note, checklist)

    override suspend fun addNoteWithReminder(
        note: Note,
        reminderData: ReminderData?,
    ): Long = noteDao.addNoteWithReminder(note, reminderData)

    override suspend fun addNoteWithRecording(
        note: Note,
        recordingData: RecordingData?,
    ): Long = noteDao.addNoteWithRecording(note, recordingData)

    override suspend fun updateNoteWithRecording(
        note: Note,
        recordingData: RecordingData?,
    ) = noteDao.updateNoteWithRecording(note, recordingData)

    override suspend fun exportNotes(uri: Uri): NotesExportSummary {
        val summary = NotesBackupUtils.exportNotes(appContext, noteDao, uri)
        return NotesExportSummary(summary.notes, summary.checklists)
    }

    override suspend fun importNotes(uri: Uri): NotesImportSummary {
        val summary = NotesBackupUtils.importNotes(appContext, noteDao, uri)
        return NotesImportSummary(summary.notes, summary.checklists)
    }

    override suspend fun setNoteLock(noteId: Long, locked: Boolean) =
        noteDao.setNoteLock(noteId, locked)

    override suspend fun setNotePin(noteId: Long, pinned: Boolean) =
        noteDao.setNotePin(noteId, pinned)

    override suspend fun unlockAllNotes() = noteDao.unlockAllNotes()

    override suspend fun insertNoteFolder(folder: NoteFolder): Long =
        noteDao.insertNoteFolder(folder)

    override suspend fun updateNoteFolder(folder: NoteFolder) =
        noteDao.updateNoteFolder(folder)

    override suspend fun deleteNoteFolder(folder: NoteFolder) =
        noteDao.deleteNoteFolder(folder)

    override suspend fun clearFolderAssignments(folderId: Long) =
        noteDao.clearFolderAssignments(folderId)

    override suspend fun moveNoteToFolder(noteId: Long, folderId: Long?) =
        noteDao.moveNoteToFolder(noteId, folderId)

    override suspend fun createNoteTag(name: String, colorHex: String): Long? {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || noteDao.findNoteTagByName(trimmed) != null) return null
        return noteDao.insertNoteTag(NoteTag(name = trimmed, colorHex = colorHex))
            .takeIf { it > 0L }
    }

    override suspend fun updateNoteTag(tag: NoteTag): Boolean {
        val trimmed = tag.name.trim()
        if (trimmed.isEmpty()) return false
        val existing = noteDao.findNoteTagByName(trimmed)
        if (existing != null && existing.id != tag.id) return false
        return noteDao.updateNoteTag(tag.copy(name = trimmed)) > 0
    }

    override suspend fun deleteNoteTag(tagId: Long) =
        noteDao.deleteNoteTagWithAssignments(tagId)

    override suspend fun setNoteTags(noteId: Long, tagIds: Set<Long>) =
        noteDao.setNoteTags(noteId, tagIds)

    override suspend fun getNoteImageFileNames(noteId: Long): List<String> =
        noteDao.getImagesForNote(noteId).map { it.fileName }

    override suspend fun importNoteImages(uris: List<Uri>): List<String> =
        uris.mapNotNull { uri -> NotesImageStore.importFromUri(appContext, uri) }

    override suspend fun saveNoteImages(noteId: Long, fileNames: List<String>) {
        val existing = fileNames.distinct().filter { fileName ->
            NotesImageStore.fileFor(appContext, fileName)?.isFile == true
        }
        val removed = noteDao.replaceNoteImages(noteId, existing)
        NotesImageStore.delete(appContext, removed)
    }

    override suspend fun discardUnsavedNoteImages(fileNames: List<String>) {
        if (fileNames.isEmpty()) return
        val referenced = noteDao.findReferencedImageFileNames(fileNames).toSet()
        NotesImageStore.delete(appContext, fileNames.filter { it !in referenced })
    }

    override suspend fun pruneOrphanNoteImages() =
        NotesImageStore.deleteOrphans(appContext, noteDao.getAllNoteImageFileNames().toSet())
}
