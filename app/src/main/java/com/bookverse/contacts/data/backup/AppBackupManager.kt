package com.bookverse.contacts.data.backup

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bookverse.contacts.App
import com.bookverse.contacts.BuildConfig
import com.bookverse.contacts.data.db.AppDatabase
import com.bookverse.contacts.data.expenses.ExpenseBudgetEntity
import com.bookverse.contacts.data.expenses.ExpenseCategoryEntity
import com.bookverse.contacts.data.expenses.ExpenseTransactionEntity
import com.bookverse.contacts.data.expenses.ExpenseType
import com.bookverse.contacts.data.notes.Note
import com.bookverse.contacts.data.notes.RecordingData
import com.bookverse.contacts.data.notes.ReminderData
import com.bookverse.contacts.data.notes.ReminderRepeat
import com.bookverse.contacts.data.notes.ReminderType
import com.bookverse.contacts.data.password.PasswordFolder
import com.bookverse.contacts.data.password.PasswordRepository
import com.bookverse.contacts.data.qr.QRHistoryEntity
import com.bookverse.contacts.data.qr.QRType
import com.bookverse.contacts.domain.backup.BackupArchive
import com.bookverse.contacts.domain.backup.BackupContentException
import com.bookverse.contacts.domain.passwords.PasswordImportPolicy
import com.bookverse.contacts.service.NotificationService
import com.bookverse.contacts.settings.AppDataStore
import com.bookverse.contacts.utils.PasswordJsonUtils
import com.bookverse.contacts.utils.NotesBackupUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class AppBackupSummary(
    val passwords: Int = 0,
    val notes: Int = 0,
    val reminders: Int = 0,
    val recordings: Int = 0,
    val transactions: Int = 0,
    val qrCodes: Int = 0,
    val includesVault: Boolean = false,
) {
    val total: Int get() = passwords + notes + reminders + recordings + transactions + qrCodes
}

class AppBackupManager(context: Context) {
    private val appContext = context.applicationContext
    private val database get() = AppDatabase.getInstance(appContext)
    private val passwordStore by lazy { PasswordRepository(database.passwordDao(), appContext) }
    private val gameStats get() = (appContext as App).container.gameStatsRepository
    private val menstrualStartKey = stringPreferencesKey("menstrual_start_date")
    private val menstrualCycleKey = intPreferencesKey("menstrual_cycle_length")
    private val menstrualLutealKey = intPreferencesKey("menstrual_luteal_length")

    suspend fun export(uri: Uri, passphrase: CharArray, includeVault: Boolean): AppBackupSummary =
        withContext(Dispatchers.IO) {
            val noteDao = database.noteDao()
            val expenseDao = database.expenseDao()
            val vaultJson = if (includeVault) {
                val credentials = passwordStore.getAllForBackup()
                PasswordJsonUtils.buildPasswordsBackupJson(credentials, passwordStore.getFoldersForBackup()) to
                    credentials.size
            } else {
                null
            }
            val categories = expenseDao.getCategories()
            val transactions = expenseDao.getTransactionsForExport()
            val budgets = expenseDao.observeBudgets().first()
            val qrItems = database.qrHistoryDao().getAllQRHistory().first()
            val (statInts, statLongs) = gameStats.exportRaw()
            val preferences = AppDataStore.getDataStore(appContext).data.first()
            val allNotes = noteDao.getAllNotesSync()
            val reminders = allNotes.filter { it.isReminder }.mapNotNull { note ->
                noteDao.getReminderDataForNote(note.id)?.let { note to it }
            }
            val recordings = allNotes.filter { it.isRecording }.mapNotNull { note ->
                noteDao.getRecordingDataForNote(note.id)?.let { note to it }
            }
            val recordingItems = recordings.mapIndexed { index, (note, data) ->
                val file = File(data.filePath)
                val entry = if (data.filePath.isNotEmpty() && file.isFile) {
                    "${BackupPayload.RECORDINGS_PREFIX}$index.${file.extension.ifEmpty { "m4a" }}"
                } else {
                    null
                }
                Triple(note, data, entry)
            }
            val sections = buildList {
                if (vaultJson != null) add("passwords")
                addAll(listOf("notes", "reminders", "recordings", "expenses", "qr_history", "game_stats", "menstrual"))
            }
            var noteCount = 0
            val output = appContext.contentResolver.openOutputStream(uri, "wt")
                ?: throw IOException("Unable to open backup destination")
            output.use { stream ->
                BackupArchive.open(stream, passphrase).use { zip ->
                    put(zip, BackupPayload.MANIFEST, BackupManifest(createdAt = System.currentTimeMillis(), appVersion = BuildConfig.VERSION_NAME, sections = sections))
                    vaultJson?.let { BackupArchive.putEntry(zip, BackupPayload.PASSWORDS, it.first.toByteArray(Charsets.UTF_8)) }
                    put(
                        zip,
                        BackupPayload.EXPENSES,
                        ExpensesBackup(
                            categories = categories.map { it.toBackup() },
                            transactions = transactions.map { it.toBackup() },
                            budgets = budgets.map { ExpenseBudgetBackup(it.categoryId, it.limit, it.createdAt, it.updatedAt) },
                        ),
                    )
                    put(zip, BackupPayload.QR_HISTORY, QrHistoryBackup(qrItems.map { it.toBackup() }))
                    put(zip, BackupPayload.GAME_STATS, GameStatsBackup(statInts, statLongs))
                    put(
                        zip,
                        BackupPayload.MENSTRUAL,
                        MenstrualBackup(
                            startDate = preferences[menstrualStartKey],
                            cycleLength = preferences[menstrualCycleKey],
                            lutealLength = preferences[menstrualLutealKey],
                        ),
                    )
                    put(zip, BackupPayload.REMINDERS, RemindersBackup(reminders.map { (note, data) -> reminderBackup(note, data) }))
                    put(
                        zip,
                        BackupPayload.RECORDINGS,
                        RecordingsBackup(recordingItems.map { (note, data, entry) -> recordingBackup(note, data, entry) }),
                    )
                    recordingItems.forEach { (_, data, entry) ->
                        if (entry != null) {
                            zip.putNextEntry(ZipEntry(entry))
                            File(data.filePath).inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                    val notesSummary = NotesBackupUtils.writeBackupEntries(
                        appContext,
                        noteDao,
                        zip,
                        BackupPayload.NOTES_PREFIX,
                    )
                    noteCount = notesSummary.notes + notesSummary.checklists
                }
            }
            AppBackupSummary(
                passwords = vaultJson?.second ?: 0,
                notes = noteCount,
                reminders = reminders.size,
                recordings = recordings.size,
                transactions = transactions.size,
                qrCodes = qrItems.size,
                includesVault = vaultJson != null,
            )
        }

    suspend fun restore(uri: Uri, passphrase: CharArray, restoreVault: Boolean): AppBackupSummary =
        withContext(Dispatchers.IO) {
            val staging = File(appContext.cacheDir, "restore_${UUID.randomUUID()}").apply { mkdirs() }
            try {
                val sections = mutableMapOf<String, ByteArray>()
                val audio = mutableMapOf<String, File>()
                val notesZipFile = File(staging, "notes.zip")
                var notesZip: ZipOutputStream? = null
                val input = appContext.contentResolver.openInputStream(uri)
                    ?: throw IOException("Unable to open backup")
                try {
                    input.use { stream ->
                        BackupArchive.read(stream, passphrase) { name, content ->
                            when {
                                name.startsWith(BackupPayload.NOTES_PREFIX) -> {
                                    val zip = notesZip ?: ZipOutputStream(notesZipFile.outputStream()).also { notesZip = it }
                                    zip.putNextEntry(ZipEntry(name))
                                    copyLimited(content, zip, MAX_FILE_ENTRY_BYTES)
                                    zip.closeEntry()
                                }
                                name.startsWith(BackupPayload.RECORDINGS_PREFIX) -> {
                                    val target = File(staging, "audio_${audio.size}")
                                    target.outputStream().use { copyLimited(content, it, MAX_FILE_ENTRY_BYTES) }
                                    audio[name] = target
                                }
                                name in JSON_SECTIONS -> {
                                    val bytes = ByteArrayOutputStream()
                                    copyLimited(content, bytes, MAX_JSON_ENTRY_BYTES)
                                    sections[name] = bytes.toByteArray()
                                }
                            }
                        }
                    }
                } finally {
                    notesZip?.close()
                }
                val manifest = sections[BackupPayload.MANIFEST]
                    ?.let { decode(it, BackupManifest::class.java) }
                    ?: throw BackupContentException("Backup manifest missing")
                if (manifest.format > BackupPayload.FORMAT) {
                    throw BackupContentException("Backup created by a newer version")
                }
                val vaultBytes = sections[BackupPayload.PASSWORDS]
                val passwords = if (restoreVault && vaultBytes != null) restoreVault(vaultBytes) else 0
                val transactions = sections[BackupPayload.EXPENSES]
                    ?.let { restoreExpenses(decode(it, ExpensesBackup::class.java)) } ?: 0
                val qrCodes = sections[BackupPayload.QR_HISTORY]
                    ?.let { restoreQrHistory(decode(it, QrHistoryBackup::class.java)) } ?: 0
                sections[BackupPayload.GAME_STATS]?.let {
                    val stats = decode(it, GameStatsBackup::class.java)
                    if (stats.ints.isNotEmpty() || stats.longs.isNotEmpty()) {
                        gameStats.importRaw(stats.ints, stats.longs)
                    }
                }
                sections[BackupPayload.MENSTRUAL]?.let { restoreMenstrual(decode(it, MenstrualBackup::class.java)) }
                val reminders = sections[BackupPayload.REMINDERS]
                    ?.let { restoreReminders(decode(it, RemindersBackup::class.java)) } ?: 0
                val recordings = sections[BackupPayload.RECORDINGS]
                    ?.let { restoreRecordings(decode(it, RecordingsBackup::class.java), audio) } ?: 0
                val notes = if (notesZipFile.isFile) {
                    val summary = NotesBackupUtils.importFromZipFile(
                        appContext,
                        database.noteDao(),
                        notesZipFile,
                        BackupPayload.NOTES_PREFIX,
                    )
                    summary.notes + summary.checklists
                } else {
                    0
                }
                AppBackupSummary(
                    passwords = passwords,
                    notes = notes,
                    reminders = reminders,
                    recordings = recordings,
                    transactions = transactions,
                    qrCodes = qrCodes,
                    includesVault = vaultBytes != null,
                )
            } finally {
                staging.deleteRecursively()
            }
        }

    private fun put(zip: ZipOutputStream, name: String, value: Any) =
        BackupArchive.putEntry(zip, name, BackupPayload.encode(value))

    private fun <T : Any> decode(bytes: ByteArray, type: Class<T>): T = try {
        BackupPayload.decode(bytes, type)
    } catch (error: RuntimeException) {
        throw BackupContentException("Unreadable backup section", error)
    }

    private fun copyLimited(input: InputStream, output: java.io.OutputStream, limit: Long) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > limit) throw BackupContentException("Backup entry too large")
            output.write(buffer, 0, read)
        }
    }

    private suspend fun restoreVault(bytes: ByteArray): Int {
        val vault = try {
            BackupPayload.decodeVault(bytes)
        } catch (error: RuntimeException) {
            throw BackupContentException("Unreadable vault section", error)
        }
        val existingFolders = passwordStore.getFoldersForBackup().toMutableList()
        val folderIds = mutableMapOf<Long, Long>()
        for (folder in vault.folders) {
            val match = existingFolders.firstOrNull { it.name.equals(folder.name, ignoreCase = true) }
            folderIds[folder.id] = match?.id ?: passwordStore.insertFolder(folder.copy(id = 0)).also { id ->
                existingFolders += PasswordFolder(id, folder.name, folder.description, folder.colorHex, folder.createdAt)
            }
        }
        val candidates = vault.credentials.map { it.copy(id = 0, folderId = it.folderId?.let(folderIds::get)) }
        val plan = PasswordImportPolicy.plan(passwordStore.getAllForBackup(), candidates)
        plan.toImport.forEach { passwordStore.upsert(it) }
        return plan.toImport.size
    }

    private suspend fun restoreExpenses(backup: ExpensesBackup): Int {
        val dao = database.expenseDao()
        val existingCategories = dao.getCategories().associateBy { BackupMergePolicy.categoryKey(it.toBackup()) }
        val categoryIds = mutableMapOf<Long, Long>()
        for (category in backup.categories.orEmpty()) {
            if (category.name.isBlank()) continue
            val existing = existingCategories[BackupMergePolicy.categoryKey(category)]
                ?: category.nameResKey?.let { key -> existingCategories.values.firstOrNull { it.nameResKey == key } }
            categoryIds[category.id] = existing?.id ?: dao.insertCategory(
                ExpenseCategoryEntity(
                    name = category.name.trim(),
                    colorHex = category.colorHex,
                    isDefault = category.isDefault,
                    createdAt = category.createdAt,
                    type = expenseType(category.type),
                    nameResKey = category.nameResKey,
                ),
            )
        }
        val existingTransactions = dao.getTransactionsForExport().map { it.toBackup() }
        val fresh = BackupMergePolicy.newTransactions(existingTransactions, backup.transactions.orEmpty())
        fresh.forEach { transaction ->
            dao.insertTransaction(
                ExpenseTransactionEntity(
                    type = expenseType(transaction.type),
                    amount = transaction.amount.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
                    categoryId = transaction.categoryId?.let(categoryIds::get),
                    categoryName = transaction.categoryName,
                    categoryColor = transaction.categoryColor,
                    date = LocalDate.ofEpochDay(transaction.epochDay),
                    note = transaction.note,
                    createdAt = transaction.createdAt,
                    updatedAt = transaction.updatedAt,
                    isFixed = transaction.isFixed,
                ),
            )
        }
        for (budget in backup.budgets.orEmpty()) {
            val categoryId = categoryIds[budget.categoryId] ?: continue
            if (dao.getBudgetForCategory(categoryId) == null) {
                dao.insertBudget(
                    ExpenseBudgetEntity(
                        categoryId = categoryId,
                        limit = budget.limit.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
                        createdAt = budget.createdAt,
                        updatedAt = budget.updatedAt,
                    ),
                )
            }
        }
        return fresh.size
    }

    private suspend fun restoreQrHistory(backup: QrHistoryBackup): Int {
        val dao = database.qrHistoryDao()
        val existing = dao.getAllQRHistory().first().map { it.toBackup() }
        val fresh = BackupMergePolicy.newQrItems(existing, backup.items.orEmpty())
        fresh.forEach { item ->
            dao.insertQRHistory(
                QRHistoryEntity(
                    content = item.content,
                    isUrl = item.isUrl,
                    type = QRType.entries.firstOrNull { it.name == item.type } ?: QRType.GENERATED,
                    timestamp = item.timestamp,
                ),
            )
        }
        return fresh.size
    }

    private suspend fun restoreMenstrual(backup: MenstrualBackup) {
        val startDate = backup.startDate?.takeIf { runCatching { LocalDate.parse(it) }.isSuccess }
        if (startDate == null && backup.cycleLength == null && backup.lutealLength == null) return
        AppDataStore.getDataStore(appContext).edit { preferences ->
            startDate?.let { preferences[menstrualStartKey] = it }
            backup.cycleLength?.let { preferences[menstrualCycleKey] = it }
            backup.lutealLength?.let { preferences[menstrualLutealKey] = it }
        }
    }

    private suspend fun restoreReminders(backup: RemindersBackup): Int {
        val dao = database.noteDao()
        val known = dao.getAllNotesSync().filter { it.isReminder }.mapNotNull { note ->
            dao.getReminderDataForNote(note.id)?.let { Triple(note.title, it.message, it.date) }
        }.toHashSet()
        var restored = 0
        for (item in backup.items.orEmpty()) {
            if (!known.add(Triple(item.title, item.message, item.date))) continue
            val note = Note(
                title = item.title,
                content = item.content,
                timestamp = item.timestamp,
                isReminder = true,
                isPinned = item.isPinned,
                createdAt = item.createdAt,
                updatedAt = item.updatedAt,
            )
            val reminder = ReminderData(
                noteId = 0,
                date = item.date,
                interval = item.interval,
                message = item.message,
                creationDate = item.creationDate,
                type = ReminderType.entries.firstOrNull { it.name == item.type } ?: ReminderType.PERIODIC,
                advanceNotificationMinutes = item.advanceNotificationMinutes,
                repeatMode = ReminderRepeat.entries.firstOrNull { it.name == item.repeatMode } ?: ReminderRepeat.NONE,
            )
            val noteId = dao.addNoteWithReminder(note, reminder)
            val stored = dao.getReminderDataForNote(noteId)
            if (stored != null) {
                runCatching {
                    NotificationService.scheduleNotification(appContext, note.copy(id = noteId), stored)
                }
            }
            restored++
        }
        return restored
    }

    private suspend fun restoreRecordings(backup: RecordingsBackup, audio: Map<String, File>): Int {
        val dao = database.noteDao()
        val known = dao.getAllNotesSync().filter { it.isRecording }.mapNotNull { note ->
            dao.getRecordingDataForNote(note.id)?.let { Triple(note.title, it.createdAt, it.duration) }
        }.toHashSet()
        val directory = File(appContext.filesDir, "recordings").apply { mkdirs() }
        var restored = 0
        for (item in backup.items.orEmpty()) {
            if (!known.add(Triple(item.title, item.createdAt, item.duration))) continue
            val staged = item.audioEntry?.let(audio::get)
            val extension = item.audioEntry?.substringAfterLast('.', "m4a")
                ?.takeIf { it.length in 1..5 && it.all(Char::isLetterOrDigit) } ?: "m4a"
            val target = staged?.let {
                File(directory, "recording_${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension")
                    .also { file -> it.copyTo(file, overwrite = true) }
            }
            dao.addNoteWithRecording(
                Note(
                    title = item.title,
                    content = item.content,
                    timestamp = item.timestamp,
                    isRecording = true,
                    isPinned = item.isPinned,
                    createdAt = item.noteCreatedAt,
                    updatedAt = item.noteUpdatedAt,
                ),
                RecordingData(
                    filePath = target?.absolutePath.orEmpty(),
                    duration = item.duration,
                    createdAt = item.createdAt,
                    transcription = item.transcription,
                    isTranscribed = item.isTranscribed,
                ),
            )
            restored++
        }
        return restored
    }

    private fun expenseType(name: String): ExpenseType =
        ExpenseType.entries.firstOrNull { it.name == name } ?: ExpenseType.EXPENSE

    private fun ExpenseCategoryEntity.toBackup() =
        ExpenseCategoryBackup(id, name, colorHex, isDefault, createdAt, type.name, nameResKey)

    private fun ExpenseTransactionEntity.toBackup() = ExpenseTransactionBackup(
        type = type.name,
        amount = amount,
        categoryId = categoryId,
        categoryName = categoryName,
        categoryColor = categoryColor,
        epochDay = date.toEpochDay(),
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isFixed = isFixed,
    )

    private fun QRHistoryEntity.toBackup() = QrHistoryItemBackup(content, isUrl, type.name, timestamp)

    private fun reminderBackup(note: Note, data: ReminderData) = ReminderBackup(
        title = note.title,
        content = note.content,
        timestamp = note.timestamp,
        isPinned = note.isPinned,
        createdAt = note.createdAt,
        updatedAt = note.updatedAt,
        date = data.date,
        interval = data.interval,
        message = data.message,
        creationDate = data.creationDate,
        type = data.type.name,
        advanceNotificationMinutes = data.advanceNotificationMinutes,
        repeatMode = data.repeatMode.name,
    )

    private fun recordingBackup(note: Note, data: RecordingData, entry: String?) = RecordingBackup(
        title = note.title,
        content = note.content,
        timestamp = note.timestamp,
        isPinned = note.isPinned,
        noteCreatedAt = note.createdAt,
        noteUpdatedAt = note.updatedAt,
        audioEntry = entry,
        duration = data.duration,
        createdAt = data.createdAt,
        transcription = data.transcription,
        isTranscribed = data.isTranscribed,
    )

    private companion object {
        const val MAX_JSON_ENTRY_BYTES = 64L * 1024 * 1024
        const val MAX_FILE_ENTRY_BYTES = 1024L * 1024 * 1024
        val JSON_SECTIONS = setOf(
            BackupPayload.MANIFEST,
            BackupPayload.PASSWORDS,
            BackupPayload.EXPENSES,
            BackupPayload.QR_HISTORY,
            BackupPayload.GAME_STATS,
            BackupPayload.MENSTRUAL,
            BackupPayload.REMINDERS,
            BackupPayload.RECORDINGS,
        )
    }
}
