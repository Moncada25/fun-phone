package com.bookverse.contacts.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookverse.contacts.data.contacts.ContactTagStore
import com.bookverse.contacts.data.model.ContactTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContactTagViewModel(
    private val store: ContactTagStore,
) : ViewModel() {
    val allTags: StateFlow<List<ContactTag>> = store.allTags
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _contactTags = MutableStateFlow<Map<String, List<ContactTag>>>(emptyMap())
    val contactTags: StateFlow<Map<String, List<ContactTag>>> = _contactTags.asStateFlow()

    private val _selectedFilterTags = MutableStateFlow<Set<Long>>(emptySet())
    val selectedFilterTags: StateFlow<Set<Long>> = _selectedFilterTags.asStateFlow()

    private val _filteredContactIds = MutableStateFlow<Set<String>?>(null)
    val filteredContactIds: StateFlow<Set<String>?> = _filteredContactIds.asStateFlow()

    init {
        loadAllCrossRefs()
    }

    private fun loadAllCrossRefs() {
        viewModelScope.launch {
            _contactTags.value = store.getAllTagsByContact()
        }
    }

    fun createTag(name: String, colorHex: String = "#2196F3") {
        viewModelScope.launch {
            store.createTag(
                ContactTag(
                    name = name.trim(),
                    colorHex = colorHex,
                ),
            )
        }
    }

    fun updateTag(tag: ContactTag) {
        viewModelScope.launch {
            if (store.updateTag(tag)) {
                refreshContactTagsForTag(tag.id)
            }
        }
    }

    fun deleteTag(tag: ContactTag) {
        viewModelScope.launch {
            store.deleteTag(tag)
            _contactTags.update { currentMap ->
                currentMap.mapValues { (_, tags) ->
                    tags.filter { it.id != tag.id }
                }
            }
            _selectedFilterTags.update { it - tag.id }
            recalculateFilteredContactIds()
        }
    }

    fun assignTagToContact(contactId: String, tagId: Long) {
        viewModelScope.launch {
            store.assignTag(contactId, tagId)
            refreshContactTagsForContact(contactId)
        }
    }

    fun removeTagFromContact(contactId: String, tagId: Long) {
        viewModelScope.launch {
            store.removeTagFromContact(contactId, tagId)
            _contactTags.update { currentMap ->
                currentMap.toMutableMap().apply {
                    this[contactId] = this[contactId]?.filter { it.id != tagId } ?: emptyList()
                }
            }
        }
    }

    fun loadTagsForContacts(contactIds: List<String>) {
        viewModelScope.launch {
            contactIds.forEach { contactId ->
                refreshContactTagsForContact(contactId)
            }
        }
    }

    fun toggleFilterTag(tagId: Long) {
        _selectedFilterTags.update { current ->
            if (tagId in current) {
                current - tagId
            } else {
                current + tagId
            }
        }
        recalculateFilteredContactIds()
    }

    fun clearFilterTags() {
        _selectedFilterTags.value = emptySet()
        _filteredContactIds.value = null
    }

    private fun recalculateFilteredContactIds() {
        viewModelScope.launch {
            val selectedTags = _selectedFilterTags.value
            if (selectedTags.isEmpty()) {
                _filteredContactIds.value = null
                return@launch
            }

            val contactIdsForTags = store.getContactIdsForTags(selectedTags.toList())
            _filteredContactIds.value = contactIdsForTags.toSet()
        }
    }

    private suspend fun refreshContactTagsForContact(contactId: String) {
        val tags = store.getTagsForContact(contactId)
        _contactTags.update { currentMap ->
            currentMap.toMutableMap().apply {
                this[contactId] = tags
            }
        }
    }

    private suspend fun refreshContactTagsForTag(tagId: Long) {
        val contactIds = store.getContactIdsForTag(tagId)
        contactIds.forEach { contactId ->
            refreshContactTagsForContact(contactId)
        }
    }
}
