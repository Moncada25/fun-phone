package com.bookverse.contacts.data.notes

fun interface NoteLookup {
    suspend fun findById(noteId: Long): Note?
}
