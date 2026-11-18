package com.bookverse.contacts.data.password

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passwords")
data class PasswordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String?,
    val usernameEnc: ByteArray?,
    val passwordEnc: ByteArray?,
    val notesEnc: ByteArray?,
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "NULL")
    val folderId: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val isDeleted: Boolean = false,
    @ColumnInfo(defaultValue = "NULL")
    val urlEnc: ByteArray? = null,
    @ColumnInfo(defaultValue = "NULL")
    val totpEnc: ByteArray? = null,
)
