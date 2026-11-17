package com.bookverse.contacts.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrateRealSchema28To29PreservesUserData() {
        migrationHelper.createDatabase(REAL_SCHEMA_DB, 28).use { database ->
            database.execSQL(
                """
                INSERT INTO notes
                    (id, title, content, timestamp, isChecklist, isReminder, isRecording, folderId)
                VALUES
                    (401, 'release-upgrade', 'keep-me', 1234, 0, 0, 0, NULL)
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO expense_categories
                    (id, name, colorHex, type, isDefault, createdAt)
                VALUES
                    (402, 'Upgrade category', '#123456', 'EXPENSE', 0, 1234)
                """.trimIndent(),
            )
        }

        migrationHelper.runMigrationsAndValidate(
            REAL_SCHEMA_DB,
            29,
            true,
            AppDatabase.MIGRATION_28_29,
        ).use { database ->
            database.query("SELECT title, content FROM notes WHERE id = 401").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("release-upgrade", cursor.getString(0))
                assertEquals("keep-me", cursor.getString(1))
            }
            database.query("SELECT name FROM expense_categories WHERE id = 402").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Upgrade category", cursor.getString(0))
            }
            database.query(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name LIKE 'academic_%'",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrateRealSchema29To30PreservesEncryptedPasswordsAndAddsFolders() {
        migrationHelper.createDatabase(PASSWORD_SCHEMA_DB, 29).use { database ->
            database.execSQL(
                """
                INSERT INTO passwords
                    (id, name, usernameEnc, passwordEnc, notesEnc, updatedAt, createdAt)
                VALUES
                    (501, 'Bank', X'010203', X'040506', X'070809', 2000, 1000)
                """.trimIndent(),
            )
        }

        migrationHelper.runMigrationsAndValidate(
            PASSWORD_SCHEMA_DB,
            30,
            true,
            AppDatabase.MIGRATION_29_30,
        ).use { database ->
            database.query(
                "SELECT usernameEnc, passwordEnc, notesEnc, folderId, isDeleted " +
                    "FROM passwords WHERE id = 501",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.getBlob(0).contentEquals(byteArrayOf(1, 2, 3)))
                assertTrue(cursor.getBlob(1).contentEquals(byteArrayOf(4, 5, 6)))
                assertTrue(cursor.getBlob(2).contentEquals(byteArrayOf(7, 8, 9)))
                assertTrue(cursor.isNull(3))
                assertEquals(0, cursor.getInt(4))
            }
            database.query(
                "SELECT COUNT(*) FROM sqlite_master " +
                    "WHERE type = 'table' AND name = 'password_folders'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrateRealSchema30To31AddsContactRecycleBinAndPreservesUserData() {
        migrationHelper.createDatabase(CONTACT_BIN_SCHEMA_DB, 30).use { database ->
            database.execSQL(
                """
                INSERT INTO passwords
                    (id, name, usernameEnc, passwordEnc, notesEnc, updatedAt, createdAt, isDeleted)
                VALUES
                    (601, 'Bank', X'010203', X'040506', X'070809', 2000, 1000, 0)
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO notes
                    (id, title, content, timestamp, isChecklist, isReminder, isRecording, folderId)
                VALUES
                    (602, 'bin-upgrade', 'keep-me', 1234, 0, 0, 0, NULL)
                """.trimIndent(),
            )
        }

        migrationHelper.runMigrationsAndValidate(
            CONTACT_BIN_SCHEMA_DB,
            31,
            true,
            AppDatabase.MIGRATION_30_31,
        ).use { database ->
            database.query("SELECT passwordEnc FROM passwords WHERE id = 601").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.getBlob(0).contentEquals(byteArrayOf(4, 5, 6)))
            }
            database.query("SELECT content FROM notes WHERE id = 602").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("keep-me", cursor.getString(0))
            }
            database.execSQL(
                """
                INSERT INTO deleted_contacts
                    (name, phones, additionalEmails, isFavorite, socialMedia, deletedAt)
                VALUES
                    ('Ana', '[]', '[]', 0, '[]', 5000)
                """.trimIndent(),
            )
            database.query("SELECT COUNT(*) FROM deleted_contacts").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrateRealSchema31To32PreservesVaultAndReminderDataAndAddsNoteTables() {
        migrationHelper.createDatabase(VAULT_NOTES_SCHEMA_DB, 31).use { database ->
            database.execSQL(
                """
                INSERT INTO passwords
                    (id, name, usernameEnc, passwordEnc, notesEnc, updatedAt, createdAt, folderId, isDeleted)
                VALUES
                    (601, 'Mail', X'0A0B', X'0C0D', X'0E0F', 2000, 1000, NULL, 0)
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO notes
                    (id, title, content, timestamp, isChecklist, isReminder, isRecording, folderId)
                VALUES
                    (602, 'pay rent', '', 1234, 0, 1, 0, NULL)
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO reminder_data
                    (id, noteId, date, interval, message, creationDate, type, advanceNotificationMinutes)
                VALUES
                    (603, 602, 5000, 0, 'rent', 1000, 'SINGLE_EVENT', 10)
                """.trimIndent(),
            )
        }

        migrationHelper.runMigrationsAndValidate(
            VAULT_NOTES_SCHEMA_DB,
            32,
            true,
            AppDatabase.MIGRATION_31_32,
        ).use { database ->
            database.query(
                "SELECT usernameEnc, passwordEnc, notesEnc, urlEnc, totpEnc FROM passwords WHERE id = 601",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.getBlob(0).contentEquals(byteArrayOf(10, 11)))
                assertTrue(cursor.getBlob(1).contentEquals(byteArrayOf(12, 13)))
                assertTrue(cursor.getBlob(2).contentEquals(byteArrayOf(14, 15)))
                assertTrue(cursor.isNull(3))
                assertTrue(cursor.isNull(4))
            }
            database.query(
                "SELECT date, type, advanceNotificationMinutes, repeatMode FROM reminder_data WHERE id = 603",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(5000L, cursor.getLong(0))
                assertEquals("SINGLE_EVENT", cursor.getString(1))
                assertEquals(10, cursor.getInt(2))
                assertEquals("NONE", cursor.getString(3))
            }
            database.query(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' " +
                    "AND name IN ('note_tags', 'note_tag_cross_ref', 'note_images')",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(3, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrateEveryLegacySchema2Through7To29PreservesAvailableUserData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (version in 2..7) {
            val databaseName = "legacy-schema-$version-to-29"
            context.deleteDatabase(databaseName)
            migrationHelper.createDatabase(databaseName, version).use { database ->
                seedLegacyDatabase(database, version)
            }

            migrationHelper.runMigrationsAndValidate(
                databaseName,
                29,
                true,
                *AppDatabase.ALL_MANUAL_MIGRATIONS,
            ).use { database ->
                assertLegacyDataSurvived(database, version)
            }
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun migrate28To29RemovesAcademicTablesOnly() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DB)
        createHelper(version = 28).use { helper ->
            helper.writableDatabase
        }

        createHelper(version = 29).use { helper ->
            val database = helper.writableDatabase
            database.query(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name LIKE 'academic_%'",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            database.query(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'retained_data'",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            database.query("SELECT value FROM retained_data").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("keep", cursor.getString(0))
            }
        }
        context.deleteDatabase(TEST_DB)
    }

    private fun createHelper(version: Int): SupportSQLiteOpenHelper {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(TEST_DB)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(version) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        ACADEMIC_TABLES.forEach { table ->
                            db.execSQL("CREATE TABLE $table (id INTEGER PRIMARY KEY)")
                        }
                        db.execSQL("CREATE TABLE retained_data (value TEXT NOT NULL)")
                        db.execSQL("INSERT INTO retained_data (value) VALUES ('keep')")
                    }

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) {
                        assertEquals(28, oldVersion)
                        assertEquals(29, newVersion)
                        AppDatabase.MIGRATION_28_29.migrate(db)
                    }
                },
            )
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(configuration)
    }

    private fun seedLegacyDatabase(database: SupportSQLiteDatabase, version: Int) {
        val noteColumns = buildList {
            add("id")
            add("title")
            add("content")
            add("timestamp")
            if (version >= 3) add("isChecklist")
            if (version >= 5) add("isReminder")
            if (version >= 7) add("isRecording")
        }
        val noteValues = buildList<Any> {
            add(LEGACY_NOTE_ID)
            add("legacy-note-$version")
            add("preserve-content-$version")
            add(1_000L + version)
            if (version >= 3) add(1)
            if (version >= 5) add(1)
            if (version >= 7) add(1)
        }
        database.execSQL(
            "INSERT INTO notes (${noteColumns.joinToString()}) " +
                "VALUES (${noteColumns.joinToString { "?" }})",
            noteValues.toTypedArray(),
        )
        database.execSQL(
            """
            INSERT INTO checklist_items (id, noteId, text, isChecked)
            VALUES (?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any>(
                LEGACY_CHECKLIST_ID,
                LEGACY_NOTE_ID,
                "preserve-checklist-$version",
                1,
            ),
        )

        when (version) {
            2 -> database.execSQL(
                """
                INSERT INTO recent_operations (id, contact, date, type)
                VALUES (?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(
                    LEGACY_RECENT_ID,
                    """{"id":"legacy-contact","name":"Legacy Person","phoneNumber":"+15550002"}""",
                    2_000L,
                    "CALL_SENT",
                ),
            )
            3 -> database.execSQL(
                """
                INSERT INTO recent_operations (
                    id, date, type, contact_id, contact_name, contact_phoneNumber
                ) VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(
                    LEGACY_RECENT_ID,
                    2_000L,
                    "CALL_SENT",
                    "legacy-contact",
                    "Legacy Person",
                    "+15550003",
                ),
            )
            4, 5 -> {
                database.execSQL(
                    """
                    INSERT INTO recent_operations (
                        id, date, type, target, contact_id, contact_name,
                        contact_phoneNumber, contact_isFavorite
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any>(
                        LEGACY_RECENT_ID,
                        2_000L,
                        "CALL_SENT",
                        "+1555000$version",
                        "legacy-contact",
                        "Legacy Person",
                        "+1555000$version",
                        1,
                    ),
                )
                database.execSQL(
                    "INSERT INTO favorite_contacts (contactId) VALUES (?)",
                    arrayOf("legacy-contact"),
                )
            }
            6, 7 -> database.execSQL(
                """
                INSERT INTO recent_operations (
                    id, date, type, target, contact_id, contact_name, contact_phones,
                    contact_isFavorite, contact_socialMedia
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(
                    LEGACY_RECENT_ID,
                    2_000L,
                    "CALL_SENT",
                    "+1555000$version",
                    "legacy-contact",
                    "Legacy Person",
                    """[{"number":"+1555000$version","type":null,"countryCode":null}]""",
                    1,
                    "[]",
                ),
            )
        }

        if (version >= 5) {
            database.execSQL(
                """
                INSERT INTO reminder_data (
                    id, noteId, date, interval, message, creationDate
                ) VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(
                    LEGACY_REMINDER_ID,
                    LEGACY_NOTE_ID,
                    3_000L,
                    0L,
                    "keep",
                    2_500L,
                ),
            )
        }
        if (version >= 7) {
            database.execSQL(
                """
                INSERT INTO recording_data (
                    id, noteId, filePath, duration, createdAt, transcription, isTranscribed
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(
                    LEGACY_RECORDING_ID,
                    LEGACY_NOTE_ID,
                    "legacy-recording.m4a",
                    10L,
                    4_000L,
                    "keep",
                    1,
                ),
            )
        }
    }

    private fun assertLegacyDataSurvived(database: SupportSQLiteDatabase, sourceVersion: Int) {
        database.query(
            "SELECT title, content FROM notes WHERE id = $LEGACY_NOTE_ID",
        ).use { cursor ->
            assertTrue("Missing note migrated from schema $sourceVersion", cursor.moveToFirst())
            assertEquals("legacy-note-$sourceVersion", cursor.getString(0))
            assertEquals("preserve-content-$sourceVersion", cursor.getString(1))
        }
        database.query(
            "SELECT text, isChecked FROM checklist_items WHERE id = $LEGACY_CHECKLIST_ID",
        ).use { cursor ->
            assertTrue(
                "Missing checklist item migrated from schema $sourceVersion",
                cursor.moveToFirst(),
            )
            assertEquals("preserve-checklist-$sourceVersion", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
        }
        database.query(
            """
            SELECT contact_name, contact_phones, contact_isFavorite
            FROM recent_operations WHERE id = $LEGACY_RECENT_ID
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(
                "Missing recent operation migrated from schema $sourceVersion",
                cursor.moveToFirst(),
            )
            assertEquals("Legacy Person", cursor.getString(0))
            assertTrue(cursor.getString(1).contains("+1555000"))
            if (sourceVersion >= 4) {
                assertEquals(1, cursor.getInt(2))
            }
        }
        if (sourceVersion >= 5) {
            assertRowExists(database, "reminder_data", LEGACY_REMINDER_ID)
        }
        if (sourceVersion >= 7) {
            assertRowExists(database, "recording_data", LEGACY_RECORDING_ID)
        }
    }

    private fun assertRowExists(
        database: SupportSQLiteDatabase,
        table: String,
        id: Long,
    ) {
        database.query("SELECT COUNT(*) FROM $table WHERE id = $id").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
    }

    private companion object {
        const val TEST_DB = "academic-removal-migration"
        const val REAL_SCHEMA_DB = "release-upgrade-migration"
        const val PASSWORD_SCHEMA_DB = "password-folders-migration"
        const val CONTACT_BIN_SCHEMA_DB = "contact-recycle-bin-migration"
        const val VAULT_NOTES_SCHEMA_DB = "vault-notes-migration"
        const val LEGACY_NOTE_ID = 501L
        const val LEGACY_CHECKLIST_ID = 502L
        const val LEGACY_RECENT_ID = 503L
        const val LEGACY_REMINDER_ID = 504L
        const val LEGACY_RECORDING_ID = 505L
        val ACADEMIC_TABLES = listOf(
            "academic_grades",
            "academic_attendance",
            "academic_tasks",
            "academic_students",
            "academic_subjects",
            "academic_grade_groups",
        )
    }
}
