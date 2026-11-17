package com.bookverse.contacts.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bookverse.contacts.data.notes.ChecklistItem
import com.bookverse.contacts.data.notes.NOTE_FOLDER_FILTER_ALL
import com.bookverse.contacts.data.notes.Note
import com.bookverse.contacts.data.notes.NoteFolder
import com.bookverse.contacts.data.notes.NoteTag
import com.bookverse.contacts.data.notes.NotesExportSummary
import com.bookverse.contacts.data.notes.NotesImportSummary
import com.bookverse.contacts.data.notes.NotesRepository
import com.bookverse.contacts.data.notes.ReminderData
import com.bookverse.contacts.data.notes.RecordingData
import com.bookverse.contacts.data.notes.buildNoteSearchIndex
import com.bookverse.contacts.data.notes.nextOccurrence
import com.bookverse.contacts.service.NotificationService
import com.bookverse.contacts.utils.NoteShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotesViewModel(
    app: Application,
    private val repository: NotesRepository,
) : AndroidViewModel(app) {
    private var lastOpenedFolderInternal: Long = NOTE_FOLDER_FILTER_ALL

    val lastOpenedFolderId: Long
        get() = lastOpenedFolderInternal

    fun setLastOpenedFolder(folderId: Long) {
        lastOpenedFolderInternal = folderId
    }

    val notes: StateFlow<List<Note>> = repository.notes.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val deletedNotes: StateFlow<List<Note>> = repository.deletedNotes.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val noteFolders: StateFlow<List<NoteFolder>> = repository.noteFolders.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val checklistsByNoteId: StateFlow<Map<Long, List<ChecklistItem>>> =
        repository.checklistItems.map { items ->
            items.groupBy { it.noteId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val remindersByNoteId: StateFlow<Map<Long, ReminderData>> =
        repository.reminderData.map { items ->
            items.associateBy { it.noteId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val recordingsByNoteId: StateFlow<Map<Long, RecordingData>> =
        repository.recordingData.map { items ->
            items.associateBy { it.noteId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val noteSearchIndex: StateFlow<Map<Long, String>> = combine(
        repository.notes,
        repository.checklistItems,
        repository.reminderData,
        repository.recordingData,
    ) { allNotes, checklistItems, reminders, recordings ->
        buildNoteSearchIndex(
            notes = allNotes,
            checklistsByNoteId = checklistItems.groupBy { it.noteId },
            remindersByNoteId = reminders.associateBy { it.noteId },
            recordingsByNoteId = recordings.associateBy { it.noteId },
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val noteTags: StateFlow<List<NoteTag>> = repository.noteTags.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val tagIdsByNoteId: StateFlow<Map<Long, Set<Long>>> =
        repository.noteTagCrossRefs.map { refs ->
            refs.groupBy({ it.noteId }, { it.tagId }).mapValues { it.value.toSet() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val selectedTagFilterInternal = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTagFilter: StateFlow<Set<Long>> = selectedTagFilterInternal.asStateFlow()

    fun toggleTagFilter(tagId: Long) {
        selectedTagFilterInternal.update { current ->
            if (tagId in current) current - tagId else current + tagId
        }
    }

    fun clearTagFilter() {
        selectedTagFilterInternal.value = emptySet()
    }

    fun createNoteTag(name: String, colorHex: String, onResult: (Boolean) -> Unit = {}) =
        viewModelScope.launch {
            onResult(repository.createNoteTag(name, colorHex) != null)
        }

    fun updateNoteTag(tag: NoteTag, onResult: (Boolean) -> Unit = {}) = viewModelScope.launch {
        onResult(repository.updateNoteTag(tag))
    }

    fun deleteNoteTag(tag: NoteTag) = viewModelScope.launch {
        repository.deleteNoteTag(tag.id)
        selectedTagFilterInternal.update { it - tag.id }
    }

    fun setNoteTags(noteId: Long, tagIds: Set<Long>) = viewModelScope.launch {
        repository.setNoteTags(noteId, tagIds)
    }

    suspend fun getChecklistForNote(noteId: Long): List<ChecklistItem> {
        return repository.getChecklistForNote(noteId)
    }

    suspend fun getReminderDataForNote(noteId: Long): ReminderData? {
        return repository.getReminderDataForNote(noteId)
    }

    suspend fun getRecordingDataForNote(noteId: Long): RecordingData? {
        return repository.getRecordingDataForNote(noteId)
    }

    fun deleteNote(note: Note) = viewModelScope.launch {
        NotificationService.cancelNotification(getApplication(), note.id)
        repository.markAsDeleted(note.id)
    }

    fun restoreNote(noteId: Long) = viewModelScope.launch {
        repository.restoreNote(noteId)
        val note = repository.findById(noteId) ?: return@launch
        if (!note.isReminder) return@launch
        val reminder = repository.getReminderDataForNote(noteId) ?: return@launch
        NotificationService.scheduleNotification(getApplication(), note, reminder)
    }

    fun permanentlyDeleteNote(note: Note) = viewModelScope.launch {
        NotificationService.cancelNotification(getApplication(), note.id)
        repository.permanentlyDeleteNote(note.id)
    }

    private var imagesForNextNewNote: List<String> = emptyList()

    init {
        viewModelScope.launch {
            runCatching { repository.pruneOrphanNoteImages() }
        }
    }

    fun attachImagesToNextNewNote(fileNames: List<String>) {
        imagesForNextNewNote = fileNames
    }

    suspend fun getNoteImageFileNames(noteId: Long): List<String> =
        repository.getNoteImageFileNames(noteId)

    suspend fun importNoteImages(uris: List<Uri>): List<String> =
        repository.importNoteImages(uris)

    fun saveNoteImages(noteId: Long, fileNames: List<String>) = viewModelScope.launch {
        repository.saveNoteImages(noteId, fileNames)
    }

    fun discardUnsavedNoteImages(fileNames: List<String>) = viewModelScope.launch {
        repository.discardUnsavedNoteImages(fileNames)
    }

    fun addNoteWithChecklist(note: Note, checklist: List<ChecklistItem>): Job {
        val imageFileNames = imagesForNextNewNote
        imagesForNextNewNote = emptyList()
        return viewModelScope.launch {
            val noteId = repository.addNoteWithChecklist(note, checklist)
            if (imageFileNames.isNotEmpty() && !note.isChecklist) {
                repository.saveNoteImages(noteId, imageFileNames)
            }
        }
    }

    fun updateNoteWithChecklist(note: Note, checklist: List<ChecklistItem>) =
        viewModelScope.launch {
            repository.updateNoteWithChecklist(note, checklist)
        }

    fun addNoteWithReminder(note: Note, reminderData: ReminderData?) = viewModelScope.launch {
        val noteId = repository.addNoteWithReminder(note, reminderData)

        reminderData?.let { reminder ->
            val savedNote = note.copy(id = noteId)
            val savedReminderData = reminder.copy(noteId = noteId)

            if (savedReminderData.nextOccurrence(System.currentTimeMillis()) != null) {
                NotificationService.scheduleNotification(getApplication(), savedNote, savedReminderData)
            }
        }
    }

    fun addNoteWithRecording(note: Note, recordingData: RecordingData?) = viewModelScope.launch {
        repository.addNoteWithRecording(note, recordingData)
    }

    fun updateNoteWithRecording(note: Note, recordingData: RecordingData?) = viewModelScope.launch {
        repository.updateNoteWithRecording(note, recordingData)
    }

    suspend fun shareNote(
        note: Note,
        htmlContent: String,
        shareNoteLauncher: ActivityResultLauncher<Intent>
    ) {
        NoteShareUtils.shareNote(getApplication(), note, htmlContent, shareNoteLauncher)
    }

    suspend fun shareChecklist(
        note: Note,
        checklist: List<ChecklistItem>,
        shareNoteLauncher: ActivityResultLauncher<Intent>
    ) {
        NoteShareUtils.shareChecklist(getApplication(), note, checklist, shareNoteLauncher)
    }

    suspend fun shareReminder(
        note: Note,
        reminderData: ReminderData,
        shareNoteLauncher: ActivityResultLauncher<Intent>
    ) {
        NoteShareUtils.shareReminder(getApplication(), note, reminderData, shareNoteLauncher)
    }

    suspend fun shareRecording(
        note: Note,
        recordingData: RecordingData,
        shareNoteLauncher: ActivityResultLauncher<Intent>
    ) {
        NoteShareUtils.shareRecording(getApplication(), note, recordingData, shareNoteLauncher)
    }

    fun buildNotePayload(note: Note, htmlContent: String): NoteShareUtils.NoteSharePayload {
        return NoteShareUtils.buildNotePayload(note, htmlContent)
    }

    fun buildChecklistPayload(
        note: Note,
        checklist: List<ChecklistItem>
    ): NoteShareUtils.NoteSharePayload {
        return NoteShareUtils.buildChecklistPayload(note, checklist)
    }

    fun buildReminderPayload(
        note: Note,
        reminderData: ReminderData
    ): NoteShareUtils.NoteSharePayload {
        return NoteShareUtils.buildReminderPayload(note, reminderData)
    }

    suspend fun buildRecordingPayload(
        note: Note,
        recordingData: RecordingData
    ): NoteShareUtils.NoteSharePayload {
        return NoteShareUtils.buildRecordingPayload(note, recordingData)
    }

    suspend fun exportNotes(uri: Uri): Result<NotesExportSummary> {
        return runCatching { repository.exportNotes(uri) }
    }

    suspend fun importNotes(uri: Uri): Result<NotesImportSummary> {
        return runCatching { repository.importNotes(uri) }
    }

    fun setNoteLock(noteId: Long, locked: Boolean) = viewModelScope.launch {
        repository.setNoteLock(noteId, locked)
    }

    fun setNotePin(noteId: Long, pinned: Boolean) = viewModelScope.launch {
        repository.setNotePin(noteId, pinned)
    }

    fun unlockAllLockedNotes() = viewModelScope.launch {
        repository.unlockAllNotes()
    }

    fun saveNoteFolder(folder: NoteFolder) = viewModelScope.launch {
        val prepared = if (folder.id == 0L) {
            folder.copy(createdAt = folder.createdAt.takeIf { it != 0L } ?: System.currentTimeMillis())
        } else {
            folder
        }
        if (prepared.id == 0L) {
            repository.insertNoteFolder(prepared)
        } else {
            repository.updateNoteFolder(prepared)
        }
    }

    fun deleteNoteFolder(folder: NoteFolder) = viewModelScope.launch {
        repository.deleteNoteFolder(folder)
        repository.clearFolderAssignments(folder.id)
    }

    fun moveNoteToFolder(noteId: Long, folderId: Long?) = viewModelScope.launch {
        repository.moveNoteToFolder(noteId, folderId)
    }
}
