package com.bookverse.contacts.integration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bookverse.contacts.settings.AppSettingsStore
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel

class AppSettingsViewModelFactory(
    private val store: AppSettingsStore,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AppSettingsViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return AppSettingsViewModel(store) as T
    }
}
