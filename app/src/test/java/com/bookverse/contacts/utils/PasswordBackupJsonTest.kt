package com.bookverse.contacts.utils

import com.bookverse.contacts.data.password.PasswordCredential
import com.bookverse.contacts.data.password.PasswordFolder
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PasswordBackupJsonTest {
    @Test
    fun buildsVersionedPayloadWithFoldersAndExcludesDeletedCredentials() {
        val folder = PasswordFolder(id = 7, name = "Work", colorHex = "#123456")
        val active = credential(id = 10, folderId = folder.id, isDeleted = false)
        val deleted = credential(id = 11, folderId = folder.id, isDeleted = true)

        val root = JsonParser.parseString(
            JsonUtils.buildPasswordsBackupJson(listOf(active, deleted), listOf(folder)),
        ).asJsonObject

        assertEquals("passwords", root.get("type").asString)
        assertEquals(2, root.get("version").asInt)
        assertEquals("Work", root.getAsJsonArray("folders")[0].asJsonObject.get("name").asString)
        val credentials = root.getAsJsonArray("credentials")
        assertEquals(1, credentials.size())
        assertEquals(7L, credentials[0].asJsonObject.get("folderId").asLong)
    }

    @Test
    fun includesWebsiteAndTotpSetupInBackupButNotInSingleShare() {
        val totp = "otpauth://totp/Example:alice?secret=JBSWY3DPEHPK3PXP&algorithm=SHA1&digits=6&period=30"
        val withExtras = credential(id = 12, folderId = null, isDeleted = false)
            .copy(url = "https://example.com", totp = totp)

        val backup = JsonParser.parseString(
            JsonUtils.buildPasswordsBackupJson(listOf(withExtras), emptyList()),
        ).asJsonObject.getAsJsonArray("credentials")[0].asJsonObject
        assertEquals("https://example.com", backup.get("url").asString)
        assertEquals(totp, backup.get("totp").asString)

        val shared = JsonUtils.passwordCredentialToMap(withExtras)
        assertEquals("https://example.com", shared["url"])
        assertFalse(shared.containsKey("totp"))
    }

    private fun credential(
        id: Long,
        folderId: Long?,
        isDeleted: Boolean,
    ) = PasswordCredential(
        id = id,
        name = "Account",
        username = "user",
        password = "secret",
        note = null,
        updatedAt = 2,
        createdAt = 1,
        folderId = folderId,
        isDeleted = isDeleted,
    )
}
