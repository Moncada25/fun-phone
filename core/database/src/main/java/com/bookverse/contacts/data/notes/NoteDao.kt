package com.bookverse.contacts.data.notes

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, timestamp DESC")
    fun getAllNotesSync(): List<Note>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY timestamp DESC")
    fun getDeletedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): Note?

    @Query("SELECT * FROM note_folders ORDER BY name COLLATE NOCASE")
    fun getNoteFolders(): Flow<List<NoteFolder>>

    @Query("SELECT * FROM note_folders ORDER BY name COLLATE NOCASE")
    fun getNoteFoldersSync(): List<NoteFolder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteFolder(folder: NoteFolder): Long

    @Update
    suspend fun updateNoteFolder(folder: NoteFolder)

    @Delete
    suspend fun deleteNoteFolder(folder: NoteFolder)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM checklist_items WHERE noteId = :noteId")
    fun getChecklistItems(noteId: Long): Flow<List<ChecklistItem>>

    @Query("SELECT * FROM checklist_items")
    fun getAllChecklistItems(): Flow<List<ChecklistItem>>

    @Query("SELECT * FROM reminder_data")
    fun getAllReminderData(): Flow<List<ReminderData>>

    @Query("SELECT * FROM recording_data")
    fun getAllRecordingData(): Flow<List<RecordingData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItem(item: ChecklistItem): Long

    @Update
    suspend fun updateChecklistItem(item: ChecklistItem)

    @Delete
    suspend fun deleteChecklistItem(item: ChecklistItem)

    @Query("DELETE FROM checklist_items WHERE noteId = :noteId")
    suspend fun deleteChecklistForNote(noteId: Long)

    @Query("SELECT * FROM checklist_items WHERE noteId = :noteId")
    suspend fun getChecklistForNote(noteId: Long): List<ChecklistItem>

    @Query("SELECT * FROM reminder_data WHERE noteId = :noteId")
    fun getReminderData(noteId: Long): Flow<ReminderData?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminderData(reminderData: ReminderData): Long

    @Update
    suspend fun updateReminderData(reminderData: ReminderData)

    @Delete
    suspend fun deleteReminderData(reminderData: ReminderData)

    @Query("DELETE FROM reminder_data WHERE noteId = :noteId")
    suspend fun deleteReminderDataForNote(noteId: Long)

    @Query("SELECT * FROM reminder_data WHERE noteId = :noteId")
    suspend fun getReminderDataForNote(noteId: Long): ReminderData?

    @Query("SELECT * FROM recording_data WHERE noteId = :noteId")
    fun getRecordingData(noteId: Long): Flow<RecordingData?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecordingData(recordingData: RecordingData): Long

    @Update
    suspend fun updateRecordingData(recordingData: RecordingData)

    @Delete
    suspend fun deleteRecordingData(recordingData: RecordingData)

    @Query("DELETE FROM recording_data WHERE noteId = :noteId")
    suspend fun deleteRecordingDataForNote(noteId: Long)

    @Query("SELECT * FROM recording_data WHERE noteId = :noteId")
    suspend fun getRecordingDataForNote(noteId: Long): RecordingData?

    @Transaction
    suspend fun updateNoteWithChecklist(note: Note, checklist: List<ChecklistItem>) {
        updateNote(note)
        deleteChecklistForNote(note.id)
        checklist.forEach { item ->
            insertChecklistItem(item.copy(noteId = note.id))
        }
    }

    @Transaction
    suspend fun addNoteWithChecklist(note: Note, checklist: List<ChecklistItem>): Long {
        val noteId = insertNote(note)
        checklist.forEach { item ->
            insertChecklistItem(item.copy(noteId = noteId))
        }
        return noteId
    }

    @Transaction
    suspend fun updateNoteWithReminder(note: Note, reminderData: ReminderData?) {
        updateNote(note)
        deleteReminderDataForNote(note.id)
        reminderData?.let {
            insertReminderData(it.copy(noteId = note.id))
        }
    }

    @Transaction
    suspend fun addNoteWithReminder(note: Note, reminderData: ReminderData?): Long {
        val noteId = insertNote(note)
        reminderData?.let {
            insertReminderData(it.copy(noteId = noteId))
        }
        return noteId
    }

    @Transaction
    suspend fun addNoteWithRecording(note: Note, recordingData: RecordingData?): Long {
        val noteId = insertNote(note)
        recordingData?.let {
            insertRecordingData(it.copy(noteId = noteId))
        }
        return noteId
    }

    @Transaction
    suspend fun updateNoteWithRecording(note: Note, recordingData: RecordingData?) {
        updateNote(note)
        deleteRecordingDataForNote(note.id)
        recordingData?.let {
            insertRecordingData(it.copy(noteId = note.id))
        }
    }

    @Query("UPDATE notes SET isPinned = :pinned WHERE id = :noteId")
    suspend fun setNotePin(noteId: Long, pinned: Boolean)

    @Query("UPDATE notes SET isLocked = :locked WHERE id = :noteId")
    suspend fun setNoteLock(noteId: Long, locked: Boolean)

    @Query("UPDATE notes SET isLocked = 0 WHERE isLocked = 1")
    suspend fun unlockAllNotes()

    @Query("UPDATE notes SET isDeleted = 1 WHERE id = :noteId")
    suspend fun markAsDeleted(noteId: Long)

    @Query("UPDATE notes SET isDeleted = 0 WHERE id = :noteId")
    suspend fun restoreNote(noteId: Long)

    @Query("UPDATE notes SET folderId = NULL WHERE folderId = :folderId")
    suspend fun clearFolderAssignments(folderId: Long)

    @Query("UPDATE notes SET folderId = :folderId WHERE id = :noteId")
    suspend fun moveNoteToFolder(noteId: Long, folderId: Long?)

    @Transaction
    suspend fun permanentlyDeleteNote(noteId: Long) {
        val note = getNoteById(noteId)
        note?.let {
            deleteChecklistForNote(noteId)
            deleteReminderDataForNote(noteId)
            deleteRecordingDataForNote(noteId)
            deleteNoteTagCrossRefsForNote(noteId)
            deleteImagesForNote(noteId)
            deleteNote(it)
        }
    }

    @Query("SELECT * FROM note_tags ORDER BY name COLLATE NOCASE")
    fun getNoteTags(): Flow<List<NoteTag>>

    @Query("SELECT * FROM note_tags ORDER BY name COLLATE NOCASE")
    suspend fun getNoteTagsSync(): List<NoteTag>

    @Query("SELECT * FROM note_tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findNoteTagByName(name: String): NoteTag?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTag(tag: NoteTag): Long

    @Update(onConflict = OnConflictStrategy.IGNORE)
    suspend fun updateNoteTag(tag: NoteTag): Int

    @Query("DELETE FROM note_tags WHERE id = :tagId")
    suspend fun deleteNoteTag(tagId: Long)

    @Query("SELECT * FROM note_tag_cross_ref")
    fun getNoteTagCrossRefs(): Flow<List<NoteTagCrossRef>>

    @Query("SELECT * FROM note_tag_cross_ref")
    suspend fun getNoteTagCrossRefsSync(): List<NoteTagCrossRef>

    @Query("SELECT tagId FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun getTagIdsForNote(noteId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTagCrossRef(crossRef: NoteTagCrossRef)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun deleteNoteTagCrossRefsForNote(noteId: Long)

    @Query("DELETE FROM note_tag_cross_ref WHERE tagId = :tagId")
    suspend fun deleteNoteTagCrossRefsForTag(tagId: Long)

    @Transaction
    suspend fun deleteNoteTagWithAssignments(tagId: Long) {
        deleteNoteTagCrossRefsForTag(tagId)
        deleteNoteTag(tagId)
    }

    @Query("SELECT * FROM note_images ORDER BY createdAt, id")
    fun getAllNoteImages(): Flow<List<NoteImage>>

    @Query("SELECT * FROM note_images ORDER BY createdAt, id")
    suspend fun getAllNoteImagesSync(): List<NoteImage>

    @Query("SELECT * FROM note_images WHERE noteId = :noteId ORDER BY createdAt, id")
    suspend fun getImagesForNote(noteId: Long): List<NoteImage>

    @Query("SELECT fileName FROM note_images")
    suspend fun getAllNoteImageFileNames(): List<String>

    @Query("SELECT fileName FROM note_images WHERE fileName IN (:fileNames)")
    suspend fun findReferencedImageFileNames(fileNames: List<String>): List<String>

    @Insert
    suspend fun insertNoteImage(image: NoteImage): Long

    @Query("DELETE FROM note_images WHERE noteId = :noteId")
    suspend fun deleteImagesForNote(noteId: Long)

    @Query("DELETE FROM note_images WHERE noteId = :noteId AND fileName = :fileName")
    suspend fun deleteNoteImage(noteId: Long, fileName: String)

    @Transaction
    suspend fun replaceNoteImages(noteId: Long, fileNames: List<String>): List<String> {
        val current = getImagesForNote(noteId).map { it.fileName }
        val removed = current.filter { it !in fileNames }
        removed.forEach { fileName -> deleteNoteImage(noteId, fileName) }
        val baseTime = System.currentTimeMillis()
        fileNames.filter { it !in current }.forEachIndexed { index, fileName ->
            insertNoteImage(
                NoteImage(noteId = noteId, fileName = fileName, createdAt = baseTime + index),
            )
        }
        return removed
    }

    @Transaction
    suspend fun setNoteTags(noteId: Long, tagIds: Collection<Long>) {
        deleteNoteTagCrossRefsForNote(noteId)
        tagIds.forEach { tagId ->
            insertNoteTagCrossRef(NoteTagCrossRef(noteId = noteId, tagId = tagId))
        }
    }
}
