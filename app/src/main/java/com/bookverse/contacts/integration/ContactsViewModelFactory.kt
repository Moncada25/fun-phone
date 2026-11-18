package com.bookverse.contacts.integration

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bookverse.contacts.data.contacts.ContactRecycleBin
import com.bookverse.contacts.data.repository.ContactCacheRepository
import com.bookverse.contacts.data.repository.ContactImportRepository
import com.bookverse.contacts.data.repository.ContactRepository
import com.bookverse.contacts.data.repository.ContactVCardRepository
import com.bookverse.contacts.data.repository.ProfileRepository
import com.bookverse.contacts.data.repository.RecentOperationsRepository
import com.bookverse.contacts.repository.ContactUsageRepository
import com.bookverse.contacts.domain.telemetry.FunnelTelemetry
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.widget.WidgetUpdateHelper

class ContactsViewModelFactory(
    private val application: Application,
    private val contactRepository: ContactRepository,
    private val contactRecycleBin: ContactRecycleBin,
    private val funnelTelemetry: FunnelTelemetry,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ContactsViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return ContactsViewModel(
            application = application,
            contactRepository = contactRepository,
            profileRepository = ProfileRepository(application),
            contactCacheRepository = ContactCacheRepository(application),
            recentOperationsRepository = RecentOperationsRepository(application),
            contactImportRepository = ContactImportRepository(application),
            contactRecycleBin = contactRecycleBin,
            contactVCardRepository = ContactVCardRepository(application, contactRepository),
            contactUsageRepository = ContactUsageRepository(application),
            funnelTelemetry = funnelTelemetry,
            refreshWidgets = { WidgetUpdateHelper.updateAllWidgets(application) },
        ) as T
    }
}
