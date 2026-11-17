package com.bookverse.contacts.integration

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bookverse.contacts.data.db.AppDatabase
import com.bookverse.contacts.data.qr.QRHistoryRepository
import com.bookverse.contacts.ui.viewmodel.QRHistoryViewModel

class QRHistoryViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(QRHistoryViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        val database = AppDatabase.getInstance(appContext)
        @Suppress("UNCHECKED_CAST")
        return QRHistoryViewModel(QRHistoryRepository(database.qrHistoryDao())) as T
    }
}
