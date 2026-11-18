package com.bookverse.contacts.data.model

import androidx.room.Entity

@Entity(
    tableName = "ignored_duplicate_pairs",
    primaryKeys = ["contactId1", "contactId2"],
)
data class IgnoredDuplicatePair(
    val contactId1: String,
    val contactId2: String,
)
