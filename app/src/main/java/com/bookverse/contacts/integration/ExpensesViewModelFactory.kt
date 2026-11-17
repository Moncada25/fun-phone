package com.bookverse.contacts.integration

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bookverse.contacts.data.db.AppDatabase
import com.bookverse.contacts.data.expenses.ExpensesRepository
import com.bookverse.contacts.ui.viewmodel.ExpensesViewModel

class ExpensesViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ExpensesViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return ExpensesViewModel(
            store = ExpensesRepository(
                dao = AppDatabase.getInstance(appContext).expenseDao(),
                context = appContext,
            ),
        ) as T
    }
}
