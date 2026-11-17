package com.bookverse.contacts.ui.screens.contacts

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.R
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.settings.Country
import com.bookverse.contacts.ui.components.ContactFormScreen
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.ui.viewmodel.ContactTagViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateContactScreen(
    onContactSaved: (Contact) -> Unit,
    onBackClick: () -> Unit,
    phoneNumber: String? = null,
    country: Country?,
    appSettingsViewModel: AppSettingsViewModel = viewModel(),
    contactsViewModel: ContactsViewModel = viewModel(),
    contactTagViewModel: ContactTagViewModel? = null
) {
    ContactFormScreen(
        stringResource(R.string.add_contact_title),
        onSave = { contactCreated -> contactCreated?.let { onContactSaved(contactCreated) }},
        onCancel = onBackClick,
        phoneNumber = phoneNumber,
        appSettingsViewModel = appSettingsViewModel,
        contactsViewModel = contactsViewModel,
        successMessage = stringResource(R.string.add_contact_success),
        country = country,
        contactTagViewModel = contactTagViewModel
    )
}
