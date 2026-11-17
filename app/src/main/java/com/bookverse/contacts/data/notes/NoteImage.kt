package com.bookverse.contacts.data.notes

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "note_images",
    indices = [Index(value = ["noteId"])],
)
data class NoteImage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val fileName: String,
    @ColumnInfo(defaultValue = "0") val createdAt: Long = System.currentTimeMillis(),
)
