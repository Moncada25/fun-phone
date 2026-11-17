package com.bookverse.contacts.ui.screens.contacts

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.R
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.settings.Countries
import com.bookverse.contacts.ui.components.ContactFormScreen
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateProfileScreen(
    onProfileUpdated: (Contact) -> Unit,
    onBackClick: () -> Unit,
    profileToEdit: Contact,
    appSettingsViewModel: AppSettingsViewModel = viewModel(),
    contactsViewModel: ContactsViewModel = viewModel()
) {
    val detectedCountry = remember(profileToEdit) {
        profileToEdit.phones.firstOrNull()?.let { phone ->
            Countries.detectCountryFromPhoneNumber(phone.number)
        }
    }

    ContactFormScreen(
        title = stringResource(R.string.update_profile_title),
        onSave = { profileUpdated ->
            profileUpdated?.let {
                onProfileUpdated(it)
            }
        },
        onCancel = onBackClick,
        contactToEdit = profileToEdit,
        appSettingsViewModel = appSettingsViewModel,
        contactsViewModel = contactsViewModel,
        successMessage = stringResource(R.string.profile_updated_success),
        isProfileEdit = true,
        country = detectedCountry
    )
}
