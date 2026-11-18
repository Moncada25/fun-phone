package com.bookverse.contacts.ui.viewmodel

import com.bookverse.contacts.ui.components.SnackbarType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class PendingSnackbarStore {
    private val _events = MutableStateFlow<List<ContactsViewModel.SnackEvent>>(emptyList())
    val events = _events.asStateFlow()

    fun enqueue(events: List<ContactsViewModel.SnackEvent>) {
        if (events.isNotEmpty()) {
            _events.value += events
        }
    }

    fun enqueueSaveResult(successMessage: String, photoWarningMessage: String?) {
        enqueue(
            buildList {
                if (successMessage.isNotEmpty()) {
                    add(ContactsViewModel.SnackEvent(successMessage, SnackbarType.Success))
                }
                if (photoWarningMessage != null) {
                    add(ContactsViewModel.SnackEvent(photoWarningMessage, SnackbarType.Warning))
                }
            },
        )
    }

    fun consume(events: List<ContactsViewModel.SnackEvent>) {
        _events.compareAndSet(events, emptyList())
    }
}
