package com.bookverse.contacts.integration

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bookverse.contacts.data.db.AppDatabase
import com.bookverse.contacts.data.password.PasswordRepository
import com.bookverse.contacts.ui.viewmodel.PasswordsViewModel

class PasswordsViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(PasswordsViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return PasswordsViewModel(
            store = PasswordRepository(
                dao = AppDatabase.getInstance(appContext).passwordDao(),
                context = appContext,
            ),
        ) as T
    }
}
