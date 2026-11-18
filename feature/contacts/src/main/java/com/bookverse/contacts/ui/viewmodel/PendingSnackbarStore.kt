package com.bookverse.contacts.ui.viewmodel

import com.bookverse.contacts.ui.components.SnackbarType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class PendingSnackbarStore {
    private val _events = MutableStateFlow<List<ContactSnackEvent>>(emptyList())
    val events = _events.asStateFlow()

    fun enqueue(events: List<ContactSnackEvent>) {
        if (events.isNotEmpty()) {
            _events.value += events
        }
    }

    fun enqueueSaveResult(successMessage: String, photoWarningMessage: String?) {
        enqueue(
            buildList {
                if (successMessage.isNotEmpty()) {
                    add(ContactSnackEvent(successMessage, SnackbarType.Success))
                }
                if (photoWarningMessage != null) {
                    add(ContactSnackEvent(photoWarningMessage, SnackbarType.Warning))
                }
            },
        )
    }

    fun consume(events: List<ContactSnackEvent>) {
        _events.compareAndSet(events, emptyList())
    }
}
