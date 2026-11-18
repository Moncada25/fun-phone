package com.bookverse.contacts.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookverse.contacts.data.contacts.ContactRecycleBin
import com.bookverse.contacts.data.contacts.ContactRestoreResult
import com.bookverse.contacts.data.model.DeletedContact
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ContactRecycleBinEvent {
    data class Restored(val name: String, val photoRestored: Boolean) : ContactRecycleBinEvent

    data class RestoreFailed(val name: String) : ContactRecycleBinEvent

    data class DeletedForever(val name: String) : ContactRecycleBinEvent

    data object Emptied : ContactRecycleBinEvent

    data object DeleteFailed : ContactRecycleBinEvent
}

class ContactRecycleBinViewModel(
    private val recycleBin: ContactRecycleBin,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _deletedContacts = MutableStateFlow<List<DeletedContact>>(emptyList())
    val deletedContacts: StateFlow<List<DeletedContact>> = _deletedContacts.asStateFlow()

    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    private val _events = MutableSharedFlow<ContactRecycleBinEvent>(extraBufferCapacity = 5)
    val events: SharedFlow<ContactRecycleBinEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch(workerDispatcher) {
            recycleBin.observeDeletedContacts().collect { contacts ->
                _deletedContacts.value = contacts
            }
        }
    }

    fun purgeExpired() {
        viewModelScope.launch(workerDispatcher) {
            recycleBin.purgeExpired(clock())
        }
    }

    fun restore(entry: DeletedContact) {
        launchWork {
            val event = when (recycleBin.restore(entry.id)) {
                ContactRestoreResult.RESTORED ->
                    ContactRecycleBinEvent.Restored(entry.contact.name, photoRestored = true)

                ContactRestoreResult.RESTORED_WITHOUT_PHOTO ->
                    ContactRecycleBinEvent.Restored(entry.contact.name, photoRestored = false)

                ContactRestoreResult.FAILED -> ContactRecycleBinEvent.RestoreFailed(entry.contact.name)
            }
            _events.emit(event)
        }
    }

    fun deleteForever(entry: DeletedContact) {
        launchWork {
            val deleted = recycleBin.deleteForever(listOf(entry.id))
            _events.emit(
                if (deleted) {
                    ContactRecycleBinEvent.DeletedForever(entry.contact.name)
                } else {
                    ContactRecycleBinEvent.DeleteFailed
                },
            )
        }
    }

    fun emptyBin() {
        launchWork {
            val deleted = recycleBin.deleteForever(_deletedContacts.value.map { it.id })
            _events.emit(
                if (deleted) ContactRecycleBinEvent.Emptied else ContactRecycleBinEvent.DeleteFailed,
            )
        }
    }

    private fun launchWork(block: suspend () -> Unit) {
        if (_isWorking.value) return
        _isWorking.value = true
        viewModelScope.launch(workerDispatcher) {
            try {
                block()
            } finally {
                _isWorking.value = false
            }
        }
    }
}
