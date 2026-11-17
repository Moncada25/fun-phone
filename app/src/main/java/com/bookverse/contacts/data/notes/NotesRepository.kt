package com.bookverse.contacts.data.notes

import android.net.Uri
import kotlinx.coroutines.flow.Flow

data class NotesExportSummary(
    val notes: Int,
    val checklists: Int,
)

data class NotesImportSummary(
    val notes: Int,
    val checklists: Int,
)

interface NotesRepository : NoteLookup {
    val notes: Flow<List<Note>>
    val deletedNotes: Flow<List<Note>>
    val noteFolders: Flow<List<NoteFolder>>
    val checklistItems: Flow<List<ChecklistItem>>
    val reminderData: Flow<List<ReminderData>>
    val recordingData: Flow<List<RecordingData>>
    val noteTags: Flow<List<NoteTag>>
    val noteTagCrossRefs: Flow<List<NoteTagCrossRef>>

    suspend fun getChecklistForNote(noteId: Long): List<ChecklistItem>
    suspend fun getReminderDataForNote(noteId: Long): ReminderData?
    suspend fun getRecordingDataForNote(noteId: Long): RecordingData?
    suspend fun markAsDeleted(noteId: Long)
    suspend fun restoreNote(noteId: Long)
    suspend fun permanentlyDeleteNote(noteId: Long)
    suspend fun addNoteWithChecklist(note: Note, checklist: List<ChecklistItem>): Long
    suspend fun updateNoteWithChecklist(note: Note, checklist: List<ChecklistItem>)
    suspend fun addNoteWithReminder(note: Note, reminderData: ReminderData?): Long
    suspend fun addNoteWithRecording(note: Note, recordingData: RecordingData?): Long
    suspend fun updateNoteWithRecording(note: Note, recordingData: RecordingData?)
    suspend fun exportNotes(uri: Uri): NotesExportSummary
    suspend fun importNotes(uri: Uri): NotesImportSummary
    suspend fun setNoteLock(noteId: Long, locked: Boolean)
    suspend fun setNotePin(noteId: Long, pinned: Boolean)
    suspend fun unlockAllNotes()
    suspend fun insertNoteFolder(folder: NoteFolder): Long
    suspend fun updateNoteFolder(folder: NoteFolder)
    suspend fun deleteNoteFolder(folder: NoteFolder)
    suspend fun clearFolderAssignments(folderId: Long)
    suspend fun moveNoteToFolder(noteId: Long, folderId: Long?)
    suspend fun createNoteTag(name: String, colorHex: String): Long?
    suspend fun updateNoteTag(tag: NoteTag): Boolean
    suspend fun deleteNoteTag(tagId: Long)
    suspend fun setNoteTags(noteId: Long, tagIds: Set<Long>)
    suspend fun getNoteImageFileNames(noteId: Long): List<String>
    suspend fun importNoteImages(uris: List<Uri>): List<String>
    suspend fun saveNoteImages(noteId: Long, fileNames: List<String>)
    suspend fun discardUnsavedNoteImages(fileNames: List<String>)
    suspend fun pruneOrphanNoteImages()
}
