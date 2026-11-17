package com.bookverse.contacts.service

fun interface MissedCallContactLookup {
    suspend fun findName(number: String): String?
}
