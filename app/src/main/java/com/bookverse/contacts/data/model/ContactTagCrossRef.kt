package com.bookverse.contacts.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "contact_tag_cross_ref",
    primaryKeys = ["contactId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = ContactTag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["tagId"])],
)
data class ContactTagCrossRef(
    val contactId: String,
    val tagId: Long,
)
