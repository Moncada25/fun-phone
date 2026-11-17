package com.bookverse.contacts.data.db

import android.content.ContentValues
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject

internal object LegacyDatabaseMigrations {
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val rows = mutableListOf<VersionTwoRecent>()
            db.query("SELECT id, contact, date, type FROM recent_operations").use { cursor ->
                while (cursor.moveToNext()) {
                    rows += VersionTwoRecent(
                        id = cursor.getLong(0),
                        contact = cursor.getString(1),
                        date = cursor.getLong(2),
                        type = cursor.getString(3),
                    )
                }
            }

            db.execSQL("ALTER TABLE recent_operations RENAME TO recent_operations_legacy")
            db.execSQL(RECENTS_V3)
            rows.forEach { row ->
                val legacyContact = parseVersionTwoContact(row.contact, row.id)
                db.insert(
                    "recent_operations",
                    0,
                    ContentValues().apply {
                        put("id", row.id)
                        put("date", row.date)
                        put("type", row.type)
                        put("contact_id", legacyContact.id)
                        put("contact_name", legacyContact.name)
                        put("contact_phoneNumber", legacyContact.phoneNumber)
                        put("contact_phoneNumber2", legacyContact.phoneNumber2)
                        put("contact_photoUri", legacyContact.photoUri)
                        put("contact_email", legacyContact.email)
                        put("contact_company", legacyContact.company)
                    },
                )
            }
            db.execSQL("DROP TABLE recent_operations_legacy")
            rebuildNotes(
                db = db,
                notesCreateSql = NOTES_V3,
                noteProjection = "id, title, content, timestamp, 0",
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE recent_operations RENAME TO recent_operations_legacy")
            db.execSQL(RECENTS_V4)
            db.execSQL(
                """
                INSERT INTO recent_operations (
                    id, date, type, target, contact_id, contact_name,
                    contact_phoneNumber, contact_phoneNumber2, contact_photoUri,
                    contact_email, contact_company, contact_isFavorite
                )
                SELECT
                    id, date, type, contact_phoneNumber, contact_id, contact_name,
                    contact_phoneNumber, contact_phoneNumber2, contact_photoUri,
                    contact_email, contact_company, 0
                FROM recent_operations_legacy
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE recent_operations_legacy")
            db.execSQL(FAVORITES_V4)
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            rebuildNotes(
                db = db,
                notesCreateSql = NOTES_V5,
                noteProjection = "id, title, content, timestamp, isChecklist, 0",
            )
            db.execSQL(REMINDERS_V5)
            db.execSQL(REMINDERS_NOTE_INDEX)
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val favoriteIds = mutableSetOf<String>()
            db.query("SELECT contactId FROM favorite_contacts").use { cursor ->
                while (cursor.moveToNext()) {
                    favoriteIds += cursor.getString(0)
                }
            }
            val rows = mutableListOf<VersionFiveRecent>()
            db.query(
                """
                SELECT
                    id, date, type, target, contact_id, contact_name,
                    contact_phoneNumber, contact_phoneNumber2, contact_photoUri,
                    contact_email, contact_company, contact_isFavorite
                FROM recent_operations
                """.trimIndent(),
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    rows += VersionFiveRecent(
                        id = cursor.getLong(0),
                        date = cursor.getLong(1),
                        type = cursor.getString(2),
                        target = cursor.getString(3),
                        contactId = cursor.getString(4),
                        contactName = cursor.getString(5),
                        phoneNumber = cursor.getString(6),
                        phoneNumber2 = cursor.getStringOrNull(7),
                        photoUri = cursor.getStringOrNull(8),
                        email = cursor.getStringOrNull(9),
                        company = cursor.getStringOrNull(10),
                        isFavorite = cursor.getInt(11) != 0,
                    )
                }
            }

            db.execSQL("ALTER TABLE recent_operations RENAME TO recent_operations_legacy")
            db.execSQL(RECENTS_V6)
            rows.forEach { row ->
                db.insert(
                    "recent_operations",
                    0,
                    ContentValues().apply {
                        put("id", row.id)
                        put("date", row.date)
                        put("type", row.type)
                        put("target", row.target)
                        putNull("time")
                        put("contact_id", row.contactId)
                        put("contact_name", row.contactName)
                        put("contact_phones", phonesJson(row.phoneNumber, row.phoneNumber2))
                        putNull("contact_accounts")
                        putNull("contact_birthday")
                        put("contact_photoUri", row.photoUri)
                        putNull("contact_remotePhotoUri")
                        put("contact_email", row.email)
                        put("contact_company", row.company)
                        put(
                            "contact_isFavorite",
                            row.isFavorite || row.contactId in favoriteIds,
                        )
                        put("contact_socialMedia", "[]")
                    },
                )
            }
            db.execSQL("DROP TABLE recent_operations_legacy")
            db.execSQL("DROP TABLE favorite_contacts")
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            rebuildNotes(
                db = db,
                notesCreateSql = NOTES_V7,
                noteProjection = "id, title, content, timestamp, isChecklist, isReminder, 0",
            )
            db.execSQL(RECORDINGS_V7)
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(PASSWORDS_V8)
        }
    }

    val ALL = arrayOf(
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
    )

    private fun rebuildNotes(
        db: SupportSQLiteDatabase,
        notesCreateSql: String,
        noteProjection: String,
    ) {
        db.execSQL("ALTER TABLE checklist_items RENAME TO checklist_items_legacy")
        db.execSQL("DROP INDEX IF EXISTS index_checklist_items_noteId")
        db.execSQL("ALTER TABLE notes RENAME TO notes_legacy")
        db.execSQL(notesCreateSql)
        db.execSQL("INSERT INTO notes SELECT $noteProjection FROM notes_legacy")
        db.execSQL(CHECKLIST_ITEMS)
        db.execSQL(CHECKLIST_NOTE_INDEX)
        db.execSQL(
            """
            INSERT INTO checklist_items (id, noteId, text, isChecked)
            SELECT id, noteId, text, isChecked FROM checklist_items_legacy
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE checklist_items_legacy")
        db.execSQL("DROP TABLE notes_legacy")
    }

    private fun parseVersionTwoContact(raw: String, rowId: Long): LegacyContact {
        return runCatching {
            val contact = JSONObject(raw)
            LegacyContact(
                id = contact.optString("id").ifBlank { "legacy-$rowId" },
                name = contact.optString("name").ifBlank { raw },
                phoneNumber = contact.optString("phoneNumber"),
                phoneNumber2 = contact.optNullableString("phoneNumber2"),
                photoUri = contact.optNullableString("photoUri"),
                email = contact.optNullableString("email"),
                company = contact.optNullableString("company"),
            )
        }.getOrElse {
            LegacyContact(
                id = "legacy-$rowId",
                name = raw,
                phoneNumber = "",
            )
        }
    }

    private fun phonesJson(primary: String, secondary: String?): String {
        val phones = JSONArray()
        listOf(primary, secondary).filterNotNull().filter(String::isNotBlank).forEach { number ->
            phones.put(
                JSONObject()
                    .put("number", number)
                    .put("type", JSONObject.NULL)
                    .put("countryCode", JSONObject.NULL),
            )
        }
        return phones.toString()
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf(String::isNotBlank)
    }

    private fun android.database.Cursor.getStringOrNull(column: Int): String? =
        if (isNull(column)) null else getString(column)

    private data class VersionTwoRecent(
        val id: Long,
        val contact: String,
        val date: Long,
        val type: String,
    )

    private data class LegacyContact(
        val id: String,
        val name: String,
        val phoneNumber: String,
        val phoneNumber2: String? = null,
        val photoUri: String? = null,
        val email: String? = null,
        val company: String? = null,
    )

    private data class VersionFiveRecent(
        val id: Long,
        val date: Long,
        val type: String,
        val target: String,
        val contactId: String,
        val contactName: String,
        val phoneNumber: String,
        val phoneNumber2: String?,
        val photoUri: String?,
        val email: String?,
        val company: String?,
        val isFavorite: Boolean,
    )

    private const val CHECKLIST_ITEMS =
        "CREATE TABLE IF NOT EXISTS `checklist_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT " +
            "NOT NULL, `noteId` INTEGER NOT NULL, `text` TEXT NOT NULL, `isChecked` INTEGER " +
            "NOT NULL, FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON " +
            "DELETE CASCADE )"

    private const val NOTES_V3 =
        "CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`title` TEXT NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, " +
            "`isChecklist` INTEGER NOT NULL)"

    private const val CHECKLIST_NOTE_INDEX =
        "CREATE INDEX IF NOT EXISTS `index_checklist_items_noteId` ON `checklist_items` (`noteId`)"

    private const val NOTES_V5 =
        "CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`title` TEXT NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, " +
            "`isChecklist` INTEGER NOT NULL, `isReminder` INTEGER NOT NULL)"

    private const val NOTES_V7 =
        "CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`title` TEXT NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, " +
            "`isChecklist` INTEGER NOT NULL, `isReminder` INTEGER NOT NULL, `isRecording` " +
            "INTEGER NOT NULL)"

    private const val RECENTS_V3 =
        "CREATE TABLE IF NOT EXISTS `recent_operations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT " +
            "NOT NULL, `date` INTEGER NOT NULL, `type` TEXT NOT NULL, `contact_id` TEXT NOT NULL, " +
            "`contact_name` TEXT NOT NULL, `contact_phoneNumber` TEXT NOT NULL, " +
            "`contact_phoneNumber2` TEXT, `contact_photoUri` TEXT, `contact_email` TEXT, " +
            "`contact_company` TEXT)"

    private const val RECENTS_V4 =
        "CREATE TABLE IF NOT EXISTS `recent_operations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT " +
            "NOT NULL, `date` INTEGER NOT NULL, `type` TEXT NOT NULL, `target` TEXT NOT NULL, " +
            "`contact_id` TEXT NOT NULL, `contact_name` TEXT NOT NULL, `contact_phoneNumber` TEXT " +
            "NOT NULL, `contact_phoneNumber2` TEXT, `contact_photoUri` TEXT, `contact_email` " +
            "TEXT, `contact_company` TEXT, `contact_isFavorite` INTEGER NOT NULL)"

    private const val RECENTS_V6 =
        "CREATE TABLE IF NOT EXISTS `recent_operations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT " +
            "NOT NULL, `date` INTEGER NOT NULL, `type` TEXT NOT NULL, `target` TEXT NOT NULL, " +
            "`time` INTEGER, `contact_id` TEXT NOT NULL, `contact_name` TEXT NOT NULL, " +
            "`contact_phones` TEXT NOT NULL, `contact_accounts` TEXT, `contact_birthday` INTEGER, " +
            "`contact_photoUri` TEXT, `contact_remotePhotoUri` TEXT, `contact_email` TEXT, " +
            "`contact_company` TEXT, `contact_isFavorite` INTEGER NOT NULL, " +
            "`contact_socialMedia` TEXT NOT NULL)"

    private const val FAVORITES_V4 =
        "CREATE TABLE IF NOT EXISTS `favorite_contacts` (`contactId` TEXT NOT NULL, " +
            "PRIMARY KEY(`contactId`))"

    private const val REMINDERS_V5 =
        "CREATE TABLE IF NOT EXISTS `reminder_data` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT " +
            "NULL, `noteId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `interval` INTEGER NOT " +
            "NULL, `message` TEXT NOT NULL, `creationDate` INTEGER NOT NULL, FOREIGN KEY(`noteId`) " +
            "REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"

    private const val REMINDERS_NOTE_INDEX =
        "CREATE INDEX IF NOT EXISTS `index_reminder_data_noteId` ON `reminder_data` (`noteId`)"

    private const val RECORDINGS_V7 =
        "CREATE TABLE IF NOT EXISTS `recording_data` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT " +
            "NULL, `noteId` INTEGER NOT NULL, `filePath` TEXT NOT NULL, `duration` INTEGER NOT " +
            "NULL, `createdAt` INTEGER NOT NULL, `transcription` TEXT NOT NULL, `isTranscribed` " +
            "INTEGER NOT NULL)"

    private const val PASSWORDS_V8 =
        "CREATE TABLE IF NOT EXISTS `passwords` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`name` TEXT, `usernameEnc` BLOB, `passwordEnc` BLOB, `notesEnc` BLOB, `updatedAt` " +
            "INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
}
