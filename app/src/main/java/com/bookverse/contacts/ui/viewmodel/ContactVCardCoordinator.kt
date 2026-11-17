package com.bookverse.contacts.ui.viewmodel

import android.net.Uri
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactAccount
import com.bookverse.contacts.data.repository.ContactVCardRepository
import com.bookverse.contacts.domain.contacts.vcard.VCardImportSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ContactVCardResult {
    data class Imported(val summary: VCardImportSummary) : ContactVCardResult

    data object ImportFailed : ContactVCardResult

    data class Exported(val count: Int) : ContactVCardResult

    data object ExportFailed : ContactVCardResult
}

internal class ContactVCardCoordinator(
    private val repository: ContactVCardRepository,
    private val scope: CoroutineScope,
    private val currentContacts: () -> List<Contact>,
    private val setImporting: (Boolean) -> Unit,
    private val onContactsImported: () -> Unit,
) {
    private val _result = MutableStateFlow<ContactVCardResult?>(null)
    val result: StateFlow<ContactVCardResult?> = _result.asStateFlow()

    fun importFile(uri: Uri, account: ContactAccount, onNotVCard: (() -> Unit)? = null) {
        scope.launch {
            if (onNotVCard != null && !repository.containsVCard(uri)) {
                onNotVCard()
                return@launch
            }
            setImporting(true)
            _result.value = null
            try {
                val summary = repository.importContacts(uri, currentContacts(), account)
                if (summary != null && summary.imported > 0) onContactsImported()
                _result.value = summary?.let(ContactVCardResult::Imported)
                    ?: ContactVCardResult.ImportFailed
            } finally {
                setImporting(false)
            }
        }
    }

    fun exportFile(uri: Uri) {
        scope.launch {
            val contacts = currentContacts()
            _result.value = if (repository.exportContacts(uri, contacts)) {
                ContactVCardResult.Exported(contacts.size)
            } else {
                ContactVCardResult.ExportFailed
            }
        }
    }

    fun clearResult() {
        _result.value = null
    }
}
