package com.bookverse.contacts.data.qr

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "qr_history")
data class QRHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val isUrl: Boolean,
    val type: QRType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class QRType {
    GENERATED,
    SCANNED
}
