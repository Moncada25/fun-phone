package com.bookverse.contacts.data.repository

import android.content.Context
import android.net.Uri
import com.bookverse.contacts.data.model.ContactAccount
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.utils.JsonUtils

class ContactImportRepository(private val context: Context) {

    suspend fun importContactsFromJson(
        jsonUri: Uri,
        contactsViewModel: ContactsViewModel,
        account: ContactAccount
    ): Int = JsonUtils.importContactsFromJson(jsonUri, context, contactsViewModel, account)
}
