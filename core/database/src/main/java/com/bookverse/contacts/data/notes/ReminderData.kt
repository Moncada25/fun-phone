package com.bookverse.contacts.data.notes

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

enum class ReminderType {
    PERIODIC,
    SINGLE_EVENT
}

enum class ReminderRepeat {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
}

@Immutable
@Entity(
    tableName = "reminder_data",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["noteId"])]
)
data class ReminderData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val date: Long,
    val interval: Int,
    val message: String,
    val creationDate: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "PERIODIC")
    val type: ReminderType = ReminderType.PERIODIC,
    @ColumnInfo(defaultValue = "0")
    val advanceNotificationMinutes: Int = 0,
    @ColumnInfo(defaultValue = "NONE")
    val repeatMode: ReminderRepeat = ReminderRepeat.NONE,
)
