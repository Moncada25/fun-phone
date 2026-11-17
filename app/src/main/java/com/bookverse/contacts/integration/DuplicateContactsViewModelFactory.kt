package com.bookverse.contacts.integration

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bookverse.contacts.ui.viewmodel.DuplicateContactsViewModel

class DuplicateContactsViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DuplicateContactsViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return DuplicateContactsViewModel(
            ignoredDuplicateStore = RoomIgnoredDuplicateStore(appContext),
            contactMergeGateway = AndroidContactMergeGateway(appContext),
        ) as T
    }
}
