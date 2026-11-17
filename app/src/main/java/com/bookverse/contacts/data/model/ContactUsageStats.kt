package com.bookverse.contacts.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contact_usage_stats",
    indices = [Index(value = ["phoneNumber"], unique = true)]
)
data class ContactUsageStats(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val callCount: Int = 0,
    val lastCallTimestamp: Long = 0,
    val totalCallDuration: Long = 0
)
