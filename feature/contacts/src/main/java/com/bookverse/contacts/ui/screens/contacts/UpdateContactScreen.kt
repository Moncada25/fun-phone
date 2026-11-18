package com.bookverse.contacts.ui.screens.contacts

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.settings.Countries
import com.bookverse.contacts.ui.components.ContactFormScreen
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.ui.viewmodel.ContactTagViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateContactScreen(
    onContactUpdated: (Contact) -> Unit,
    onBackClick: () -> Unit,
    contactToEdit: Contact,
    appSettingsViewModel: AppSettingsViewModel = viewModel(),
    contactsViewModel: ContactsViewModel = viewModel(),
    contactTagViewModel: ContactTagViewModel? = null
) {
    var profileContact by remember { mutableStateOf<Contact?>(null) }

    LaunchedEffect(Unit) {
        profileContact = contactsViewModel.getProfileContact()
    }

    val isProfileEdit = profileContact?.id == contactToEdit.id

    val detectedCountry = remember(contactToEdit) {
        contactToEdit.phones.firstOrNull()?.let { phone ->
            Countries.detectCountryFromPhoneNumber(phone.number)
        }
    }

    ContactFormScreen(
        title = stringResource(R.string.edit_contact_title),
        onSave = { contactUpdated ->
            contactUpdated?.let {
                val latestContact = when {
                    it.id.isNotBlank() -> contactsViewModel.findContactById(it.id)
                    else -> contactsViewModel.findContactByPhones(it.phones)
                } ?: it
                onContactUpdated(latestContact)
            }
        },
        onCancel = onBackClick,
        contactToEdit = contactToEdit,
        appSettingsViewModel = appSettingsViewModel,
        contactsViewModel = contactsViewModel,
        successMessage = stringResource(R.string.edit_contact_success),
        isProfileEdit = isProfileEdit,
        country = detectedCountry,
        contactTagViewModel = contactTagViewModel
    )
}
