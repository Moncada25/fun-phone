package com.bookverse.contacts.integration

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bookverse.contacts.ui.viewmodel.NotesViewModel

class NotesViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val application = context.applicationContext as Application

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return NotesViewModel(
            app = application,
            repository = RoomNotesRepository(application),
        ) as T
    }
}
