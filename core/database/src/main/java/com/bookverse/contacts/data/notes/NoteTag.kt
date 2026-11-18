package com.bookverse.contacts.data.notes

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "note_tags",
    indices = [Index(value = ["name"], unique = true)],
)
data class NoteTag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(defaultValue = "#2196F3") val colorHex: String = "#2196F3",
    @ColumnInfo(defaultValue = "0") val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "note_tag_cross_ref",
    primaryKeys = ["noteId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = NoteTag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["tagId"])],
)
data class NoteTagCrossRef(
    val noteId: Long,
    val tagId: Long,
)
