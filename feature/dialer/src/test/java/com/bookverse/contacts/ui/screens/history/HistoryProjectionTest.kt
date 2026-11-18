package com.bookverse.contacts.ui.screens.history

import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import com.bookverse.contacts.data.model.RecentOperation
import com.bookverse.contacts.data.model.RecentOperationSource
import com.bookverse.contacts.data.model.RecentType
import com.bookverse.contacts.data.model.historyKey
import com.bookverse.contacts.data.model.idsBySource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HistoryProjectionTest {
    @Test
    fun combinesSortsAndFiltersRecentOperations() {
        val received = recent(id = 1, contactId = "a", date = 100, type = RecentType.CALL_RECEIVED)
        val missed = recent(id = 2, contactId = "b", date = 300, type = RecentType.CALL_MISSED)

        val projection = buildHistoryProjection(
            localOperations = listOf(received),
            callLogs = listOf(missed),
            selectedFilter = RecentType.CALL_MISSED,
            formatDate = { "day" },
        )

        assertEquals(listOf(missed, received), projection.allRecents)
        assertEquals(listOf(missed), projection.filteredRecents)
    }

    @Test
    fun groupsNewestDayFirstAndKeepsLatestOperationPerContact() {
        val olderKnown = recent(id = 1, contactId = "known", date = 100)
        val latestKnown = recent(id = 2, contactId = "known", date = 200)
        val unknownA = recent(id = 3, contactId = "", phone = "555-0100", date = 180)
        val unknownB = recent(id = 4, contactId = "", phone = "555-0200", date = 300)

        val projection = buildHistoryProjection(
            localOperations = listOf(olderKnown, unknownA),
            callLogs = listOf(latestKnown, unknownB),
            selectedFilter = null,
            formatDate = { timestamp -> if (timestamp >= 250) "new-day" else "old-day" },
        )

        assertEquals(listOf("new-day", "old-day"), projection.groupedOperations.keys.toList())
        assertEquals(listOf(unknownB), projection.groupedOperations.getValue("new-day"))
        assertEquals(
            listOf(latestKnown, unknownA),
            projection.groupedOperations.getValue("old-day"),
        )
    }

    @Test
    fun searchesHistoryByNameAndNormalizedPhoneWithinSelectedType() {
        val alice = recent(
            id = 1,
            contactId = "alice",
            phone = "+57 300-555-0101",
            date = 100,
            type = RecentType.CALL_RECEIVED,
        )
        val bob = recent(
            id = 2,
            contactId = "Bob Builder",
            phone = "555-0202",
            date = 200,
            type = RecentType.CALL_MISSED,
        )

        val byName = buildHistoryProjection(
            localOperations = listOf(alice, bob),
            callLogs = emptyList(),
            selectedFilter = null,
            searchQuery = "builder",
            formatDate = { "day" },
        )
        val byPhoneAndType = buildHistoryProjection(
            localOperations = listOf(alice, bob),
            callLogs = emptyList(),
            selectedFilter = RecentType.CALL_RECEIVED,
            searchQuery = "300555",
            formatDate = { "day" },
        )

        assertEquals(listOf(bob), byName.filteredRecents)
        assertEquals(listOf(alice), byPhoneAndType.filteredRecents)
    }

    @Test
    fun keepsLocalAndCallLogOperationsWithSameIdDistinct() {
        val localBlocked = recent(
            id = 11,
            contactId = "a",
            date = 200,
            type = RecentType.CALL_BLOCKED,
        )
        val callLogMissed = recent(
            id = 11,
            contactId = "b",
            date = 100,
            type = RecentType.CALL_MISSED,
            source = RecentOperationSource.CALL_LOG,
        )

        val projection = buildHistoryProjection(
            localOperations = listOf(localBlocked),
            callLogs = listOf(callLogMissed),
            selectedFilter = null,
            formatDate = { "day" },
        )

        assertEquals(listOf(localBlocked, callLogMissed), projection.allRecents)
        assertNotEquals(localBlocked.historyKey, callLogMissed.historyKey)
        val dayKeys = projection.groupedOperations.getValue("day").map { it.historyKey }
        assertEquals(listOf("local:11", "call_log:11"), dayKeys)
        assertEquals(dayKeys.size, dayKeys.toSet().size)

        val selectedKeys = setOf(callLogMissed.historyKey)
        val selected = projection.allRecents.filter { it.historyKey in selectedKeys }
        assertEquals(listOf(callLogMissed), selected)

        val selectedIds = selected.idsBySource()
        assertEquals(emptyList<Long>(), selectedIds.localIds)
        assertEquals(listOf(11L), selectedIds.callLogIds)

        val allIds = projection.allRecents.idsBySource()
        assertEquals(listOf(11L), allIds.localIds)
        assertEquals(listOf(11L), allIds.callLogIds)
    }

    private fun recent(
        id: Long,
        contactId: String,
        date: Long,
        phone: String = "555-0000",
        type: RecentType = RecentType.CALL_RECEIVED,
        source: RecentOperationSource = RecentOperationSource.LOCAL,
    ) = RecentOperation(
        id = id,
        contact = Contact(
            id = contactId,
            name = contactId.ifEmpty { "Unknown" },
            phones = listOf(ContactPhone(phone, type = null)),
        ),
        date = date,
        type = type,
        target = phone,
        source = source,
    )
}
