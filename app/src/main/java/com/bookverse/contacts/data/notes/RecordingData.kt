package com.bookverse.contacts.data.notes

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "recording_data")
data class RecordingData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long = 0,
    val filePath: String = "",
    val duration: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val transcription: String = "",
    val isTranscribed: Boolean = false
)
