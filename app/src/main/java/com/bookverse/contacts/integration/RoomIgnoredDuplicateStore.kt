package com.bookverse.contacts.integration

import android.content.Context
import com.bookverse.contacts.data.contacts.IgnoredDuplicateStore
import com.bookverse.contacts.data.db.AppDatabase
import com.bookverse.contacts.data.model.IgnoredDuplicatePair

class RoomIgnoredDuplicateStore(context: Context) : IgnoredDuplicateStore {
    private val dao = AppDatabase.getInstance(context.applicationContext).ignoredDuplicatePairDao()

    override suspend fun loadPairs(): Set<Set<String>> = try {
        dao.getAll()
            .map { setOf(it.contactId1, it.contactId2) }
            .toSet()
    } catch (_: Exception) {
        emptySet()
    }

    override suspend fun ignorePairs(contactIds: List<String>): Boolean = try {
        for (firstIndex in contactIds.indices) {
            for (secondIndex in firstIndex + 1 until contactIds.size) {
                val firstId = contactIds[firstIndex]
                val secondId = contactIds[secondIndex]
                val pair = if (firstId < secondId) {
                    IgnoredDuplicatePair(firstId, secondId)
                } else {
                    IgnoredDuplicatePair(secondId, firstId)
                }
                dao.insert(pair)
            }
        }
        true
    } catch (_: Exception) {
        false
    }
}
