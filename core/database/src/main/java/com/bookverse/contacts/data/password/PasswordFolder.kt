package com.bookverse.contacts.data.password

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

const val PASSWORD_FOLDER_FILTER_ALL = -1L

@Immutable
@Entity(tableName = "password_folders")
data class PasswordFolder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    @ColumnInfo(defaultValue = "#90CAF9") val colorHex: String = "#90CAF9",
    @ColumnInfo(defaultValue = "0") val createdAt: Long = System.currentTimeMillis(),
)
