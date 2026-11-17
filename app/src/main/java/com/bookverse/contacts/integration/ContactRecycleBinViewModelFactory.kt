package com.bookverse.contacts.integration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bookverse.contacts.data.contacts.ContactRecycleBin
import com.bookverse.contacts.ui.viewmodel.ContactRecycleBinViewModel

class ContactRecycleBinViewModelFactory(
    private val recycleBin: ContactRecycleBin,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ContactRecycleBinViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return ContactRecycleBinViewModel(recycleBin = recycleBin) as T
    }
}
