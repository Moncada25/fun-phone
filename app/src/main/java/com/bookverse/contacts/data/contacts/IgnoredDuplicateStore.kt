package com.bookverse.contacts.data.contacts

interface IgnoredDuplicateStore {
    suspend fun loadPairs(): Set<Set<String>>
    suspend fun ignorePairs(contactIds: List<String>): Boolean
}
