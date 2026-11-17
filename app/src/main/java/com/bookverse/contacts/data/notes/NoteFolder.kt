package com.bookverse.contacts.data.notes

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "note_folders")
data class NoteFolder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    @ColumnInfo(defaultValue = "#90CAF9") val colorHex: String = "#90CAF9",
    @ColumnInfo(defaultValue = "0") val createdAt: Long = System.currentTimeMillis()
)
