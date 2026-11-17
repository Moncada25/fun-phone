package com.bookverse.contacts.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.bookverse.contacts.data.repository.ContactRepository
import com.bookverse.contacts.domain.call.UiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberCallDisplayName(activeCall: UiCall?): String {
    var displayName by remember(activeCall?.id) { mutableStateOf(activeCall?.number ?: "") }
    val context = LocalContext.current

    LaunchedEffect(activeCall) {
        if (activeCall != null) {
            displayName = withContext(Dispatchers.IO) {
                val repository = ContactRepository(context.applicationContext)
                val contacts = repository.getContacts()
                val contact = repository.findContactByPhoneNumber(contacts, activeCall.number)
                contact?.name ?: activeCall.number
            }
        }
    }

    return displayName
}
