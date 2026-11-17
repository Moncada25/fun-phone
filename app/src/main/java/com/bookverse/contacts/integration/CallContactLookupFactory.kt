package com.bookverse.contacts.integration

import com.bookverse.contacts.domain.call.CallContactLookup
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel

class CallContactLookupFactory {
    fun create(contactsViewModel: ContactsViewModel): CallContactLookup =
        ContactsCallContactLookup(contactsViewModel)
}
