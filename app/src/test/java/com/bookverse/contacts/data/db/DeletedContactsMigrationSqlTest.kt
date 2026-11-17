package com.bookverse.contacts.data.db

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DeletedContactsMigrationSqlTest {
    private val schemaDirectory = File("schemas/com.bookverse.contacts.data.db.AppDatabase")

    @Test
    fun manualMigrationCreatesTheExactTableRoomExpectsInSchema31() {
        val entity = entities(31).single { it.get("tableName").asString == "deleted_contacts" }
        val expected = entity.get("createSql").asString.replace("\${TABLE_NAME}", "deleted_contacts")

        assertEquals(expected, AppDatabase.DELETED_CONTACTS_TABLE_SQL)
        assertEquals(30, AppDatabase.MIGRATION_30_31.startVersion)
        assertEquals(31, AppDatabase.MIGRATION_30_31.endVersion)
        assertTrue(AppDatabase.ALL_MANUAL_MIGRATIONS.contains(AppDatabase.MIGRATION_30_31))
    }

    @Test
    fun schema31OnlyAddsTheRecycleBinTable() {
        fun createStatements(version: Int): Map<String, String> = entities(version).associate {
            it.get("tableName").asString to it.get("createSql").asString
        }

        val previous = createStatements(30)
        val current = createStatements(31)

        assertEquals(previous.keys + "deleted_contacts", current.keys)
        assertEquals(previous, current - "deleted_contacts")
    }

    private fun entities(version: Int) = JsonParser
        .parseString(File(schemaDirectory, "$version.json").readText())
        .asJsonObject
        .getAsJsonObject("database")
        .getAsJsonArray("entities")
        .map { it.asJsonObject }
}
