package com.bookverse.contacts.ui.screens.contacts

import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.settings.Countries
import com.bookverse.contacts.ui.components.ContactFormScreen
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProfileScreen(
    onProfileCreated: (Contact) -> Unit,
    onBackClick: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel = viewModel(),
    contactsViewModel: ContactsViewModel = viewModel()
) {
    val context = LocalContext.current
    ContactFormScreen(
        title = stringResource(R.string.create_profile_title),
        onSave = { profileCreated ->
            profileCreated?.let {
                onProfileCreated(it)
            }
        },
        onCancel = onBackClick,
        contactToEdit = null,
        appSettingsViewModel = appSettingsViewModel,
        contactsViewModel = contactsViewModel,
        successMessage = stringResource(R.string.add_profile_success),
        isProfileEdit = true,
        country = Countries.getDefaultCountry(context)
    )
}
