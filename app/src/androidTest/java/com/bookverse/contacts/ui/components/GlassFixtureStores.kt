package com.bookverse.contacts.ui.components

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.bookverse.contacts.data.expenses.ExpenseBudget
import com.bookverse.contacts.data.expenses.ExpenseCategory
import com.bookverse.contacts.data.expenses.ExpenseCsvHeaders
import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.data.expenses.ExpenseType
import com.bookverse.contacts.data.expenses.ExpensesStore
import com.bookverse.contacts.data.notes.ChecklistItem
import com.bookverse.contacts.data.notes.Note
import com.bookverse.contacts.data.notes.NoteFolder
import com.bookverse.contacts.data.notes.NotesRepository
import com.bookverse.contacts.data.notes.RecordingData
import com.bookverse.contacts.data.notes.ReminderData
import com.bookverse.contacts.data.password.PasswordCredential
import com.bookverse.contacts.data.password.PasswordFolder
import com.bookverse.contacts.data.password.PasswordStore
import com.bookverse.contacts.settings.AppSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/** Synthetic, isolated data only. Unexpected writes fail instead of reaching app persistence. */
internal class GlassNotesRepository : NotesRepository {
    override val notes = MutableStateFlow(List(60) { Note(id = it + 1L, title = "QA note ${it + 1}", content = "Synthetic preview", timestamp = 1_700_000_000_000L) })
    override val deletedNotes = MutableStateFlow(emptyList<Note>())
    override val noteFolders = MutableStateFlow(emptyList<NoteFolder>())
    override val checklistItems = MutableStateFlow(emptyList<ChecklistItem>())
    override val reminderData = MutableStateFlow(emptyList<ReminderData>())
    override val recordingData = MutableStateFlow(emptyList<RecordingData>())
    override val noteTags = MutableStateFlow(emptyList<com.bookverse.contacts.data.notes.NoteTag>())
    override val noteTagCrossRefs = MutableStateFlow(emptyList<com.bookverse.contacts.data.notes.NoteTagCrossRef>())
    override suspend fun findById(noteId: Long) = notes.value.firstOrNull { it.id == noteId }
    override suspend fun getChecklistForNote(noteId: Long) = emptyList<ChecklistItem>()
    override suspend fun getReminderDataForNote(noteId: Long): ReminderData? = null
    override suspend fun getRecordingDataForNote(noteId: Long): RecordingData? = null
    override suspend fun markAsDeleted(noteId: Long) = unexpectedWrite()
    override suspend fun restoreNote(noteId: Long) = unexpectedWrite()
    override suspend fun permanentlyDeleteNote(noteId: Long) = unexpectedWrite()
    override suspend fun addNoteWithChecklist(note: Note, checklist: List<ChecklistItem>): Long = unexpectedWrite()
    override suspend fun updateNoteWithChecklist(note: Note, checklist: List<ChecklistItem>) = unexpectedWrite()
    override suspend fun addNoteWithReminder(note: Note, reminderData: ReminderData?): Long = unexpectedWrite()
    override suspend fun addNoteWithRecording(note: Note, recordingData: RecordingData?): Long = unexpectedWrite()
    override suspend fun updateNoteWithRecording(note: Note, recordingData: RecordingData?) = unexpectedWrite()
    override suspend fun exportNotes(uri: Uri) = unexpectedWrite()
    override suspend fun importNotes(uri: Uri) = unexpectedWrite()
    override suspend fun setNoteLock(noteId: Long, locked: Boolean) = unexpectedWrite()
    override suspend fun setNotePin(noteId: Long, pinned: Boolean) = unexpectedWrite()
    override suspend fun unlockAllNotes() = unexpectedWrite()
    override suspend fun insertNoteFolder(folder: NoteFolder): Long = unexpectedWrite()
    override suspend fun updateNoteFolder(folder: NoteFolder) = unexpectedWrite()
    override suspend fun deleteNoteFolder(folder: NoteFolder) = unexpectedWrite()
    override suspend fun clearFolderAssignments(folderId: Long) = unexpectedWrite()
    override suspend fun moveNoteToFolder(noteId: Long, folderId: Long?) = unexpectedWrite()
    override suspend fun createNoteTag(name: String, colorHex: String): Long? = unexpectedWrite()
    override suspend fun updateNoteTag(tag: com.bookverse.contacts.data.notes.NoteTag): Boolean = unexpectedWrite()
    override suspend fun deleteNoteTag(tagId: Long) = unexpectedWrite()
    override suspend fun setNoteTags(noteId: Long, tagIds: Set<Long>) = unexpectedWrite()
    override suspend fun getNoteImageFileNames(noteId: Long) = emptyList<String>()
    override suspend fun importNoteImages(uris: List<Uri>): List<String> = unexpectedWrite()
    override suspend fun saveNoteImages(noteId: Long, fileNames: List<String>) = unexpectedWrite()
    override suspend fun discardUnsavedNoteImages(fileNames: List<String>) = unexpectedWrite()
    override suspend fun pruneOrphanNoteImages() = Unit
}

internal class GlassPasswordStore : PasswordStore {
    private val rows = List(60) { PasswordCredential(it + 1L, "QA credential ${it + 1}", "synthetic", "fixture-only", null, 2, 1) }
    override val credentials = MutableStateFlow(rows)
    override val deletedCredentials = MutableStateFlow(emptyList<PasswordCredential>())
    override val folders = MutableStateFlow(emptyList<PasswordFolder>())
    override fun setSearch(query: String) { credentials.value = rows.filter { it.name.contains(query, true) } }
    override suspend fun get(id: Long) = rows.firstOrNull { it.id == id }
    override suspend fun getAllForBackup() = unexpectedWrite()
    override suspend fun getFoldersForBackup() = unexpectedWrite()
    override suspend fun upsert(credential: PasswordCredential) = unexpectedWrite()
    override suspend fun markAsDeleted(id: Long) = unexpectedWrite()
    override suspend fun restore(id: Long) = unexpectedWrite()
    override suspend fun permanentlyDelete(id: Long) = unexpectedWrite()
    override suspend fun insertFolder(folder: PasswordFolder): Long = unexpectedWrite()
    override suspend fun updateFolder(folder: PasswordFolder) = unexpectedWrite()
    override suspend fun deleteFolder(folder: PasswordFolder) = unexpectedWrite()
    override suspend fun clearFolderAssignments(folderId: Long) = unexpectedWrite()
    override suspend fun moveToFolder(id: Long, folderId: Long?) = unexpectedWrite()
}

internal class GlassExpensesStore : ExpensesStore {
    override val categories = MutableStateFlow(listOf(ExpenseCategory(1, "QA category", "#CF5360")))
    override val transactions = MutableStateFlow(List(60) {
        ExpenseTransaction(it + 1L, ExpenseType.EXPENSE, 10.0 + it, 1, "QA category", "#CF5360", LocalDate.now(), "QA expense ${it + 1}")
    })
    override val budgetsWithCategories = MutableStateFlow(emptyList<Pair<ExpenseBudget, ExpenseCategory?>>())
    override suspend fun seedDefaultCategoriesIfNeeded() = Unit
    override suspend fun upsertTransaction(transaction: ExpenseTransaction): Long = unexpectedWrite()
    override suspend fun deleteTransaction(id: Long) = unexpectedWrite()
    override suspend fun upsertCategory(category: ExpenseCategory): Long = unexpectedWrite()
    override suspend fun deleteCategory(categoryId: Long): Boolean = unexpectedWrite()
    override suspend fun upsertBudget(budget: ExpenseBudget): Long = unexpectedWrite()
    override suspend fun deleteBudget(budgetId: Long) = unexpectedWrite()
    override suspend fun getTransaction(id: Long) = transactions.value.firstOrNull { it.id == id }
    override suspend fun getCategory(categoryId: Long) = categories.value.firstOrNull { it.id == categoryId }
    override suspend fun exportFixedTransactions(transactions: List<ExpenseTransaction>, uri: Uri) = unexpectedWrite()
    override suspend fun importFixedTransactions(uri: Uri) = unexpectedWrite()
    override suspend fun exportTransactionsCsv(uri: Uri, headers: ExpenseCsvHeaders, currencyCode: String) = unexpectedWrite()
}

internal class GlassSettingsStore : AppSettingsStore, DataStore<Preferences> {
    override val data = MutableStateFlow(emptyPreferences())
    private val mutex = Mutex()
    override fun dataStore(context: Context): DataStore<Preferences> = this
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = mutex.withLock {
        transform(data.value).also { data.value = it }
    }
}

private fun unexpectedWrite(): Nothing = error("This glass fixture is read-only")
