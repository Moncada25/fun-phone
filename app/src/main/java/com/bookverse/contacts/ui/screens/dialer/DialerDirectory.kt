package com.bookverse.contacts.ui.screens.dialer

import android.graphics.Bitmap
import android.net.Uri
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactUsageStats
import com.bookverse.contacts.data.model.RecentOperation
import com.bookverse.contacts.data.model.RecentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface DialerDirectory {
    val contacts: StateFlow<List<Contact>>
    val profile: StateFlow<Contact?>
    val callLogs: StateFlow<List<RecentOperation>>
    val localOperations: StateFlow<List<RecentOperation>>
    val topUsedContacts: Flow<List<ContactUsageStats>>

    fun addRecentOperation(contact: Contact, type: RecentType, target: String)

    fun recordCallUsage(phoneNumber: String, duration: Long = 0)

    suspend fun findContactByPhoneNumber(phoneNumber: String): Contact?

    fun fetchAllRecentOperations()

    suspend fun loadContactPhoto(uri: Uri): Bitmap?
}
