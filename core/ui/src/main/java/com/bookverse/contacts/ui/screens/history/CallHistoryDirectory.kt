package com.bookverse.contacts.ui.screens.history

import android.graphics.Bitmap
import android.net.Uri
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.RecentOperation
import com.bookverse.contacts.data.repository.BlockedNumberInfo
import com.bookverse.contacts.ui.screens.dialer.DialerDirectory
import com.bookverse.contacts.ui.viewmodel.ContactSnackEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface CallHistoryDirectory : DialerDirectory {
    val selectedRecentOperations: StateFlow<Set<String>>
    val isLoadingMoreCallLogs: StateFlow<Boolean>
    val hasMoreCallLogs: StateFlow<Boolean>
    val blockedNumbers: StateFlow<List<BlockedNumberInfo>>
    val snackbarEvents: SharedFlow<ContactSnackEvent>

    fun loadMoreRecentOperations()

    fun deleteSelectedRecentOperations()

    fun deleteRecentOperations(operations: List<RecentOperation>)

    fun toggleRecentOperationSelection(operation: RecentOperation)

    fun clearRecentOperationSelection()

    fun selectAllRecentOperations(list: List<RecentOperation>)

    fun blockContact(contact: Contact)

    fun unblockContact(contact: Contact)

    fun unblockNumber(number: String)

    fun uriToBitmap(uri: Uri): Bitmap?
}
