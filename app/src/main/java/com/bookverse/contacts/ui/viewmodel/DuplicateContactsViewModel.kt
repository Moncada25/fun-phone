package com.bookverse.contacts.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookverse.contacts.data.contacts.ContactMergeGateway
import com.bookverse.contacts.data.contacts.IgnoredDuplicateStore
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.DuplicateGroup
import com.bookverse.contacts.utils.DuplicateDetector
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class DuplicateEvent {
    data class ScanComplete(val count: Int) : DuplicateEvent()
    data object MergeSuccess : DuplicateEvent()
    data object MergeError : DuplicateEvent()
    data object IgnoreSuccess : DuplicateEvent()
}

class DuplicateContactsViewModel(
    private val ignoredDuplicateStore: IgnoredDuplicateStore,
    private val contactMergeGateway: ContactMergeGateway,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _duplicateGroups = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val duplicateGroups: StateFlow<List<DuplicateGroup>> = _duplicateGroups.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanComplete = MutableStateFlow(false)
    val scanComplete: StateFlow<Boolean> = _scanComplete.asStateFlow()

    private val _mergeInProgress = MutableStateFlow(false)
    val mergeInProgress: StateFlow<Boolean> = _mergeInProgress.asStateFlow()

    private val _events = MutableSharedFlow<DuplicateEvent>(extraBufferCapacity = 5)
    val events: SharedFlow<DuplicateEvent> = _events.asSharedFlow()

    fun scanForDuplicates(contacts: List<Contact>) {
        viewModelScope.launch(workerDispatcher) {
            _isScanning.value = true
            try {
                val detector = DuplicateDetector()
                val allGroups = detector.findDuplicates(contacts)
                val ignoredPairs = loadIgnoredPairs()
                val filteredGroups = filterIgnoredGroups(allGroups, ignoredPairs)
                _duplicateGroups.value = filteredGroups
                _scanComplete.value = true
                _events.emit(DuplicateEvent.ScanComplete(filteredGroups.size))
            } catch (_: Exception) {
                _duplicateGroups.value = emptyList()
                _scanComplete.value = true
                _events.emit(DuplicateEvent.ScanComplete(0))
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun mergeContacts(
        group: DuplicateGroup,
        primaryContactId: String,
        onMerged: () -> Unit = {},
    ) {
        viewModelScope.launch(workerDispatcher) {
            _mergeInProgress.value = true
            try {
                if (contactMergeGateway.merge(group, primaryContactId)) {
                    _duplicateGroups.update { groups ->
                        groups.filter { it != group }
                    }
                    _events.emit(DuplicateEvent.MergeSuccess)
                    withContext(Dispatchers.Main) { onMerged() }
                } else {
                    _events.emit(DuplicateEvent.MergeError)
                }
            } catch (_: Exception) {
                _events.emit(DuplicateEvent.MergeError)
            } finally {
                _mergeInProgress.value = false
            }
        }
    }

    fun ignoreDuplicateGroup(group: DuplicateGroup) {
        viewModelScope.launch(workerDispatcher) {
            val contactIds = group.contacts.map { it.id }
            if (ignoredDuplicateStore.ignorePairs(contactIds)) {
                _duplicateGroups.update { groups ->
                    groups.filter { it != group }
                }
                _events.emit(DuplicateEvent.IgnoreSuccess)
            }
        }
    }

    private suspend fun loadIgnoredPairs(): Set<Set<String>> =
        ignoredDuplicateStore.loadPairs()

    private fun filterIgnoredGroups(
        groups: List<DuplicateGroup>,
        ignoredPairs: Set<Set<String>>,
    ): List<DuplicateGroup> {
        return groups.filter { group ->
            val contactIds = group.contacts.map { it.id }
            var hasIgnoredPair = false

            for (i in contactIds.indices) {
                for (j in i + 1 until contactIds.size) {
                    val pair = setOf(contactIds[i], contactIds[j])
                    if (pair in ignoredPairs) {
                        hasIgnoredPair = true
                        break
                    }
                }
                if (hasIgnoredPair) break
            }

            !hasIgnoredPair
        }
    }
}
