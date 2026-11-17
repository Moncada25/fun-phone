package com.bookverse.contacts.data.backup

import com.bookverse.contacts.data.password.PasswordCredential
import com.bookverse.contacts.data.password.PasswordFolder
import com.bookverse.contacts.domain.backup.BackupArchive
import com.bookverse.contacts.domain.backup.BackupPassphraseException
import com.bookverse.contacts.utils.JsonUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class BackupPayloadTest {
    private val totp = "otpauth://totp/Example:alice?secret=JBSWY3DPEHPK3PXP&issuer=Example&algorithm=SHA1&digits=6&period=30"

    private fun credential(name: String, deleted: Boolean = false) = PasswordCredential(
        id = 9,
        name = name,
        username = "alice",
        password = "pä\$\$\"w0rd",
        note = "línea 1\nlínea 2",
        updatedAt = 20,
        createdAt = 10,
        folderId = 3,
        isDeleted = deleted,
        url = "https://example.com",
        totp = totp,
    )

    @Test
    fun vaultSectionRoundTripsThroughAnEncryptedArchive() {
        val folders = listOf(PasswordFolder(id = 3, name = "Work", description = "d", colorHex = "#112233", createdAt = 5))
        val json = JsonUtils.buildPasswordsBackupJson(listOf(credential("Mail"), credential("Gone", deleted = true)), folders)
        val passphrase = "a long passphrase".toCharArray()
        val file = ByteArrayOutputStream()
        BackupArchive.write(file, passphrase, 10_000) { zip ->
            BackupArchive.putEntry(zip, BackupPayload.MANIFEST, BackupPayload.encode(BackupManifest(createdAt = 1, sections = listOf("passwords"))))
            BackupArchive.putEntry(zip, BackupPayload.PASSWORDS, json.toByteArray())
        }

        val sections = mutableMapOf<String, ByteArray>()
        BackupArchive.read(ByteArrayInputStream(file.toByteArray()), passphrase) { name, content ->
            sections[name] = content.readBytes()
        }
        val manifest = BackupPayload.decode(sections.getValue(BackupPayload.MANIFEST), BackupManifest::class.java)
        assertEquals(listOf("passwords"), manifest.sections)
        val vault = BackupPayload.decodeVault(sections.getValue(BackupPayload.PASSWORDS))
        assertEquals(folders, vault.folders)
        assertEquals(listOf(credential("Mail").copy(id = 0)), vault.credentials)

        assertThrows(BackupPassphraseException::class.java) {
            BackupArchive.read(ByteArrayInputStream(file.toByteArray()), "another passphrase".toCharArray()) { _, _ -> }
        }
    }

    @Test
    fun vaultDecodingToleratesLegacyEntriesWithoutNewFields() {
        val vault = BackupPayload.decodeVault(
            """{"credentials":[{"name":" Bank ","username":"u","password":"p","note":null,"updatedAt":7},{"name":""}]}"""
                .toByteArray(),
        )
        assertEquals(1, vault.credentials.size)
        val credential = vault.credentials.single()
        assertEquals("Bank", credential.name)
        assertEquals(7L, credential.createdAt)
        assertNull(credential.url)
        assertNull(credential.totp)
        assertNull(credential.folderId)
    }

    @Test
    fun dataSectionsRoundTripAsJson() {
        val expenses = ExpensesBackup(
            categories = listOf(ExpenseCategoryBackup(1, "Food", "#fff", true, 3, "EXPENSE", "food")),
            transactions = listOf(ExpenseTransactionBackup("INCOME", 1234.56, 1, "Food", "#fff", 19000, "a \"note\"", 1, 2, true)),
            budgets = listOf(ExpenseBudgetBackup(1, 99.5, 1, 2)),
        )
        assertEquals(expenses, BackupPayload.decode(BackupPayload.encode(expenses), ExpensesBackup::class.java))
        val stats = GameStatsBackup(mapOf("wins_snake" to 3), mapOf("last_started_snake" to 99L))
        assertEquals(stats, BackupPayload.decode(BackupPayload.encode(stats), GameStatsBackup::class.java))
        val menstrual = MenstrualBackup("2026-01-02", 28, 14)
        assertEquals(menstrual, BackupPayload.decode(BackupPayload.encode(menstrual), MenstrualBackup::class.java))
        val reminders = RemindersBackup(listOf(ReminderBackup(title = "Rent", date = 5, type = "SINGLE_EVENT", repeatMode = "MONTHLY")))
        assertEquals(reminders, BackupPayload.decode(BackupPayload.encode(reminders), RemindersBackup::class.java))
    }

    @Test
    fun restoreSkipsTransactionsAndQrItemsThatAlreadyExist() {
        val coffee = ExpenseTransactionBackup("EXPENSE", 3.5, 1, "Food", "#fff", 100, "coffee")
        val existing = listOf(coffee.copy(categoryId = 8, categoryName = " food ", createdAt = 77))
        val incoming = listOf(coffee, coffee, coffee.copy(amount = 4.0), coffee.copy(epochDay = 101))
        assertEquals(
            listOf(coffee, coffee.copy(amount = 4.0), coffee.copy(epochDay = 101)),
            BackupMergePolicy.newTransactions(existing, incoming),
        )
        val qr = QrHistoryItemBackup("https://a", true, "SCANNED", 1)
        assertEquals(
            listOf(qr.copy(timestamp = 2)),
            BackupMergePolicy.newQrItems(listOf(qr), listOf(qr, qr.copy(timestamp = 2), qr.copy(timestamp = 2), qr.copy(content = ""))),
        )
        assertEquals(
            BackupMergePolicy.categoryKey(ExpenseCategoryBackup(name = " Food", type = "EXPENSE")),
            BackupMergePolicy.categoryKey(ExpenseCategoryBackup(name = "food ", type = "EXPENSE")),
        )
    }
}
