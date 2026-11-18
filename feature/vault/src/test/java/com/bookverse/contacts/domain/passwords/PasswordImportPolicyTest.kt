package com.bookverse.contacts.domain.passwords

import com.bookverse.contacts.data.password.PasswordCredential
import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordImportPolicyTest {
    @Test
    fun importsEverythingIntoAnEmptyVault() {
        val incoming = listOf(credential("Mail"), credential("Bank"))

        val plan = PasswordImportPolicy.plan(emptyList(), incoming)

        assertEquals(incoming, plan.toImport)
        assertEquals(0, plan.skipped)
    }

    @Test
    fun importingTheSameBackupTwiceAddsNothing() {
        val backup = listOf(credential("Mail"), credential("Bank"))
        val first = PasswordImportPolicy.plan(emptyList(), backup)
        val vault = first.toImport.mapIndexed { index, item -> item.copy(id = index + 1L) }

        val second = PasswordImportPolicy.plan(vault, backup)

        assertEquals(emptyList<PasswordCredential>(), second.toImport)
        assertEquals(2, second.skipped)
    }

    @Test
    fun skipsOnlyEntriesMatchingTitleUsernameAndPassword() {
        val vault = listOf(credential("Mail", username = "ana", password = "one", id = 1))
        val sameEntry = credential("Mail", username = "ana", password = "one")
        val otherPassword = credential("Mail", username = "ana", password = "two")
        val otherUsername = credential("Mail", username = "luis", password = "one")
        val otherTitle = credential("Work mail", username = "ana", password = "one")

        val plan = PasswordImportPolicy.plan(
            vault,
            listOf(sameEntry, otherPassword, otherUsername, otherTitle),
        )

        assertEquals(listOf(otherPassword, otherUsername, otherTitle), plan.toImport)
        assertEquals(1, plan.skipped)
    }

    @Test
    fun ignoresNoteFolderTimestampsAndSurroundingTitleSpaces() {
        val vault = listOf(
            credential("Mail", id = 4, note = "old", folderId = 9, updatedAt = 50),
        )
        val incoming = listOf(credential(" Mail ", note = "new", folderId = null, updatedAt = 99))

        val plan = PasswordImportPolicy.plan(vault, incoming)

        assertEquals(emptyList<PasswordCredential>(), plan.toImport)
        assertEquals(1, plan.skipped)
    }

    @Test
    fun treatsPasswordAndUsernameAsCaseSensitive() {
        val vault = listOf(credential("Mail", username = "ana", password = "secret", id = 1))
        val incoming = listOf(
            credential("Mail", username = "ana", password = "Secret"),
            credential("Mail", username = "Ana", password = "secret"),
        )

        val plan = PasswordImportPolicy.plan(vault, incoming)

        assertEquals(incoming, plan.toImport)
        assertEquals(0, plan.skipped)
    }

    @Test
    fun restoresEntriesThatOnlyExistInTheRecycleBin() {
        val vault = listOf(credential("Mail", id = 1, isDeleted = true))
        val incoming = listOf(credential("Mail"))

        val plan = PasswordImportPolicy.plan(vault, incoming)

        assertEquals(incoming, plan.toImport)
        assertEquals(0, plan.skipped)
    }

    @Test
    fun keepsRepeatedEntriesFromTheBackupUntilTheVaultHasAsMany() {
        val repeated = listOf(credential("Mail"), credential("Mail"))

        val intoEmptyVault = PasswordImportPolicy.plan(emptyList(), repeated)
        val intoVaultWithOne = PasswordImportPolicy.plan(listOf(credential("Mail", id = 1)), repeated)
        val intoVaultWithTwo = PasswordImportPolicy.plan(
            listOf(credential("Mail", id = 1), credential("Mail", id = 2)),
            repeated,
        )

        assertEquals(2, intoEmptyVault.toImport.size)
        assertEquals(0, intoEmptyVault.skipped)
        assertEquals(1, intoVaultWithOne.toImport.size)
        assertEquals(1, intoVaultWithOne.skipped)
        assertEquals(0, intoVaultWithTwo.toImport.size)
        assertEquals(2, intoVaultWithTwo.skipped)
    }

    private fun credential(
        name: String,
        username: String = "user",
        password: String = "secret",
        id: Long = 0,
        note: String? = null,
        folderId: Long? = null,
        updatedAt: Long = 2,
        isDeleted: Boolean = false,
    ) = PasswordCredential(
        id = id,
        name = name,
        username = username,
        password = password,
        note = note,
        updatedAt = updatedAt,
        createdAt = 1,
        folderId = folderId,
        isDeleted = isDeleted,
    )
}
