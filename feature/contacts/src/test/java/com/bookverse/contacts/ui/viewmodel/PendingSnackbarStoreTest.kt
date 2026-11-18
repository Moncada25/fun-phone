package com.bookverse.contacts.ui.viewmodel

import com.bookverse.contacts.ui.components.SnackbarType
import org.junit.Assert.assertEquals
import org.junit.Test

class PendingSnackbarStoreTest {
    @Test
    fun `events remain pending until destination consumes them`() {
        val store = PendingSnackbarStore()
        val success = ContactSnackEvent("Contact updated", SnackbarType.Success)
        val warning = ContactSnackEvent("Photo warning", SnackbarType.Warning)

        store.enqueue(listOf(success, warning))

        assertEquals(listOf(success, warning), store.events.value)

        store.consume(store.events.value)

        assertEquals(emptyList<ContactSnackEvent>(), store.events.value)
    }

    @Test
    fun `stale consumer cannot clear a newer event`() {
        val store = PendingSnackbarStore()
        val first = ContactSnackEvent("First")
        val second = ContactSnackEvent("Second")
        store.enqueue(listOf(first))
        val staleSnapshot = store.events.value
        store.enqueue(listOf(second))

        store.consume(staleSnapshot)

        assertEquals(listOf(first, second), store.events.value)
    }
}
