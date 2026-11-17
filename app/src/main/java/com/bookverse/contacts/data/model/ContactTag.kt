package com.bookverse.contacts.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "contact_tags",
    indices = [Index(value = ["name"], unique = true)],
)
data class ContactTag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#2196F3",
    val createdAt: Long = System.currentTimeMillis(),
)
