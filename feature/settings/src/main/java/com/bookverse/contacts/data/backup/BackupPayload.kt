package com.bookverse.contacts.data.backup

import com.bookverse.contacts.data.password.PasswordCredential
import com.bookverse.contacts.data.password.PasswordFolder
import com.bookverse.contacts.domain.passwords.Totp
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

data class BackupManifest(
    val format: Int = BackupPayload.FORMAT,
    val createdAt: Long = 0L,
    val appVersion: String = "",
    val sections: List<String> = emptyList(),
)

data class ExpenseCategoryBackup(
    val id: Long = 0,
    val name: String = "",
    val colorHex: String = "",
    val isDefault: Boolean = false,
    val createdAt: Long = 0,
    val type: String = "EXPENSE",
    val nameResKey: String? = null,
)

data class ExpenseTransactionBackup(
    val type: String = "EXPENSE",
    val amount: Double = 0.0,
    val categoryId: Long? = null,
    val categoryName: String = "",
    val categoryColor: String = "",
    val epochDay: Long = 0,
    val note: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isFixed: Boolean = false,
)

data class ExpenseBudgetBackup(
    val categoryId: Long = 0,
    val limit: Double = 0.0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

data class ExpensesBackup(
    val categories: List<ExpenseCategoryBackup> = emptyList(),
    val transactions: List<ExpenseTransactionBackup> = emptyList(),
    val budgets: List<ExpenseBudgetBackup> = emptyList(),
)

data class QrHistoryItemBackup(
    val content: String = "",
    val isUrl: Boolean = false,
    val type: String = "GENERATED",
    val timestamp: Long = 0,
)

data class QrHistoryBackup(val items: List<QrHistoryItemBackup> = emptyList())

data class GameStatsBackup(
    val ints: Map<String, Int> = emptyMap(),
    val longs: Map<String, Long> = emptyMap(),
)

data class MenstrualBackup(
    val startDate: String? = null,
    val cycleLength: Int? = null,
    val lutealLength: Int? = null,
)

data class ReminderBackup(
    val title: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val isPinned: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val date: Long = 0,
    val interval: Int = 0,
    val message: String = "",
    val creationDate: Long = 0,
    val type: String = "PERIODIC",
    val advanceNotificationMinutes: Int = 0,
    val repeatMode: String = "NONE",
)

data class RemindersBackup(val items: List<ReminderBackup> = emptyList())

data class RecordingBackup(
    val title: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val isPinned: Boolean = false,
    val noteCreatedAt: Long = 0,
    val noteUpdatedAt: Long = 0,
    val audioEntry: String? = null,
    val duration: Long = 0,
    val createdAt: Long = 0,
    val transcription: String = "",
    val isTranscribed: Boolean = false,
)

data class RecordingsBackup(val items: List<RecordingBackup> = emptyList())

data class VaultBackup(
    val folders: List<PasswordFolder>,
    val credentials: List<PasswordCredential>,
)

object BackupPayload {
    const val FORMAT = 1
    const val MANIFEST = "manifest.json"
    const val PASSWORDS = "passwords.json"
    const val EXPENSES = "expenses.json"
    const val QR_HISTORY = "qr_history.json"
    const val GAME_STATS = "game_stats.json"
    const val MENSTRUAL = "menstrual.json"
    const val REMINDERS = "reminders.json"
    const val RECORDINGS = "recordings.json"
    const val RECORDINGS_PREFIX = "recordings/"
    const val NOTES_PREFIX = "notes/"

    private val gson = Gson()

    fun encode(value: Any): ByteArray = gson.toJson(value).toByteArray(Charsets.UTF_8)

    fun <T : Any> decode(bytes: ByteArray, type: Class<T>): T =
        gson.fromJson(String(bytes, Charsets.UTF_8), type)
            ?: throw IllegalArgumentException("Empty backup section")

    fun decodeVault(bytes: ByteArray): VaultBackup {
        val root = JsonParser.parseString(String(bytes, Charsets.UTF_8)).asJsonObject
        val folders = root.getAsJsonArray("folders")?.mapNotNull { element ->
            val item = element.asJsonObject
            val name = item.string("name")?.trim().orEmpty()
            val id = item.long("id")
            if (name.isEmpty() || id == null) return@mapNotNull null
            PasswordFolder(
                id = id,
                name = name,
                description = item.string("description"),
                colorHex = item.string("colorHex") ?: "#90CAF9",
                createdAt = item.long("createdAt") ?: 0L,
            )
        }.orEmpty()
        val credentials = root.getAsJsonArray("credentials")?.mapNotNull { element ->
            val item = element.asJsonObject
            val name = item.string("name")?.trim().orEmpty()
            if (name.isEmpty()) return@mapNotNull null
            val updatedAt = item.long("updatedAt") ?: 0L
            PasswordCredential(
                name = name,
                username = item.string("username").orEmpty(),
                password = item.string("password").orEmpty(),
                note = item.string("note"),
                updatedAt = updatedAt,
                createdAt = item.long("createdAt") ?: updatedAt,
                folderId = item.long("folderId"),
                url = item.string("url"),
                totp = item.string("totp")?.let(Totp::parse)?.let(Totp::toUri),
            )
        }.orEmpty()
        return VaultBackup(folders, credentials)
    }

    private fun JsonObject.string(key: String): String? =
        get(key)?.takeIf { !it.isJsonNull }?.asString?.takeIf { it.isNotEmpty() }

    private fun JsonObject.long(key: String): Long? =
        get(key)?.takeIf { !it.isJsonNull }?.asLong
}

data class ExpenseTransactionIdentity(
    val type: String,
    val amount: Double,
    val epochDay: Long,
    val categoryName: String,
    val note: String,
    val isFixed: Boolean,
)

object BackupMergePolicy {
    fun identity(transaction: ExpenseTransactionBackup) = ExpenseTransactionIdentity(
        type = transaction.type,
        amount = transaction.amount,
        epochDay = transaction.epochDay,
        categoryName = transaction.categoryName.trim().lowercase(),
        note = transaction.note.orEmpty().trim(),
        isFixed = transaction.isFixed,
    )

    fun newTransactions(
        existing: List<ExpenseTransactionBackup>,
        incoming: List<ExpenseTransactionBackup>,
    ): List<ExpenseTransactionBackup> {
        val remaining = existing.groupingBy(::identity).eachCount().toMutableMap()
        return incoming.filter { transaction ->
            val key = identity(transaction)
            val count = remaining[key] ?: 0
            if (count > 0) {
                remaining[key] = count - 1
                false
            } else {
                true
            }
        }
    }

    fun newQrItems(
        existing: List<QrHistoryItemBackup>,
        incoming: List<QrHistoryItemBackup>,
    ): List<QrHistoryItemBackup> {
        val known = existing.toHashSet()
        return incoming.filter { it.content.isNotEmpty() && known.add(it) }
    }

    fun categoryKey(category: ExpenseCategoryBackup): String =
        category.type + "|" + category.name.trim().lowercase()
}
