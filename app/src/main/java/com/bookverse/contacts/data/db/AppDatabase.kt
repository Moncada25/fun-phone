package com.bookverse.contacts.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bookverse.contacts.data.expenses.ExpenseBudgetEntity
import com.bookverse.contacts.data.expenses.ExpenseCategoryEntity
import com.bookverse.contacts.data.expenses.ExpenseDao
import com.bookverse.contacts.data.expenses.ExpenseTransactionEntity
import com.bookverse.contacts.data.model.ContactTag
import com.bookverse.contacts.data.model.ContactTagCrossRef
import com.bookverse.contacts.data.model.ContactUsageStats
import com.bookverse.contacts.data.model.IgnoredDuplicatePair
import com.bookverse.contacts.data.model.RecentOperation
import com.bookverse.contacts.data.notes.ChecklistItem
import com.bookverse.contacts.data.notes.Note
import com.bookverse.contacts.data.notes.NoteDao
import com.bookverse.contacts.data.notes.NoteFolder
import com.bookverse.contacts.data.notes.NoteImage
import com.bookverse.contacts.data.notes.NoteTag
import com.bookverse.contacts.data.notes.NoteTagCrossRef
import com.bookverse.contacts.data.notes.ReminderData
import com.bookverse.contacts.data.notes.RecordingData
import com.bookverse.contacts.data.password.PasswordDao
import com.bookverse.contacts.data.password.PasswordEntity
import com.bookverse.contacts.data.password.PasswordFolder
import com.bookverse.contacts.data.qr.QRHistoryDao
import com.bookverse.contacts.data.qr.QRHistoryEntity

@Database(
    entities = [
        RecentOperation::class,
        Note::class,
        ChecklistItem::class,
        ReminderData::class,
        RecordingData::class,
        PasswordEntity::class,
        CachedContactEntity::class,
        ContactUsageStats::class,
        ExpenseCategoryEntity::class,
        ExpenseTransactionEntity::class,
        ExpenseBudgetEntity::class,
        QRHistoryEntity::class,
        NoteFolder::class,
        ContactTag::class,
        ContactTagCrossRef::class,
        IgnoredDuplicatePair::class,
        PasswordFolder::class,
        DeletedContactEntity::class,
        NoteTag::class,
        NoteTagCrossRef::class,
        NoteImage::class,
    ],
    version = 32,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 11, to = 12),
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 14, to = 15),
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 17),
        AutoMigration(from = 17, to = 18),
        AutoMigration(from = 18, to = 19),
        AutoMigration(from = 19, to = 20),
        AutoMigration(from = 20, to = 21),
        AutoMigration(from = 21, to = 22),
        AutoMigration(from = 22, to = 23),
        AutoMigration(from = 23, to = 24),
        AutoMigration(from = 24, to = 25),
        AutoMigration(from = 25, to = 26),
        AutoMigration(from = 26, to = 27),
        AutoMigration(from = 27, to = 28),
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recentOperationDao(): RecentOperationDao
    abstract fun noteDao(): NoteDao
    abstract fun passwordDao(): PasswordDao
    abstract fun cachedContactDao(): CachedContactDao
    abstract fun contactUsageStatsDao(): ContactUsageStatsDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun qrHistoryDao(): QRHistoryDao
    abstract fun contactTagDao(): ContactTagDao
    abstract fun ignoredDuplicatePairDao(): IgnoredDuplicatePairDao
    abstract fun deletedContactDao(): DeletedContactDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        internal val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS academic_grades")
                db.execSQL("DROP TABLE IF EXISTS academic_attendance")
                db.execSQL("DROP TABLE IF EXISTS academic_tasks")
                db.execSQL("DROP TABLE IF EXISTS academic_students")
                db.execSQL("DROP TABLE IF EXISTS academic_subjects")
                db.execSQL("DROP TABLE IF EXISTS academic_grade_groups")
            }
        }

        internal val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE passwords ADD COLUMN folderId INTEGER DEFAULT NULL")
                db.execSQL(
                    "ALTER TABLE passwords ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS password_folders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT,
                        colorHex TEXT NOT NULL DEFAULT '#90CAF9',
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
            }
        }

        internal const val DELETED_CONTACTS_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS `deleted_contacts` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`phones` TEXT NOT NULL, " +
                "`accounts` TEXT, " +
                "`birthday` INTEGER, " +
                "`email` TEXT, " +
                "`additionalEmails` TEXT NOT NULL, " +
                "`company` TEXT, " +
                "`isFavorite` INTEGER NOT NULL, " +
                "`socialMedia` TEXT NOT NULL, " +
                "`notes` TEXT, " +
                "`ringtone` TEXT, " +
                "`photoFileName` TEXT, " +
                "`deletedAt` INTEGER NOT NULL)"

        internal val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(DELETED_CONTACTS_TABLE_SQL)
            }
        }

        internal val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE passwords ADD COLUMN urlEnc BLOB DEFAULT NULL")
                db.execSQL("ALTER TABLE passwords ADD COLUMN totpEnc BLOB DEFAULT NULL")
                db.execSQL(
                    "ALTER TABLE reminder_data ADD COLUMN repeatMode TEXT NOT NULL DEFAULT 'NONE'",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS note_tags (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        colorHex TEXT NOT NULL DEFAULT '#2196F3',
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_note_tags_name ON note_tags (name)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS note_tag_cross_ref (
                        noteId INTEGER NOT NULL,
                        tagId INTEGER NOT NULL,
                        PRIMARY KEY(noteId, tagId),
                        FOREIGN KEY(tagId) REFERENCES note_tags(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_note_tag_cross_ref_tagId " +
                        "ON note_tag_cross_ref (tagId)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS note_images (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        noteId INTEGER NOT NULL,
                        fileName TEXT NOT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_note_images_noteId ON note_images (noteId)",
                )
            }
        }

        internal val ALL_MANUAL_MIGRATIONS = arrayOf(
            *LegacyDatabaseMigrations.ALL,
            MIGRATION_28_29,
            MIGRATION_29_30,
            MIGRATION_30_31,
            MIGRATION_31_32,
        )

        fun getInstance(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "contacts_db"
            )
                .addMigrations(*ALL_MANUAL_MIGRATIONS)
                .build()
                .also { INSTANCE = it }
        }
    }
}
