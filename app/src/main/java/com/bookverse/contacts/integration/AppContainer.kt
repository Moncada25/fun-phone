package com.bookverse.contacts.integration

import android.app.Activity
import android.content.Context
import com.bookverse.contacts.data.contacts.ContactRecycleBin
import com.bookverse.contacts.data.repository.CalendarRepository
import com.bookverse.contacts.data.repository.ContactRecycleBinRepository
import com.bookverse.contacts.data.repository.ContactRepository
import com.bookverse.contacts.data.repository.DataStoreGameStatsRepository
import com.bookverse.contacts.data.repository.HolidaysRepository
import com.bookverse.contacts.domain.telemetry.FunnelTelemetry
import com.bookverse.contacts.domain.holidays.CalendarEventSource
import com.bookverse.contacts.domain.holidays.HolidayPreferences
import com.bookverse.contacts.domain.holidays.HolidaySource
import com.bookverse.contacts.service.CallRepository
import com.bookverse.contacts.service.DefaultCallRepository
import com.bookverse.contacts.service.MissedCallContactLookup
import com.bookverse.contacts.settings.AppSettingsStore
import com.bookverse.contacts.settings.Countries
import com.bookverse.contacts.settings.DataStoreAppSettingsStore
import com.bookverse.contacts.utils.ConnectivityChecker
import java.time.YearMonth

interface IntegrityProvider {
    suspend fun initialize()
}

interface ReviewPromptLauncher {
    suspend fun launch(activity: Activity): Boolean
}

class AppContainer(
    context: Context,
    val telemetry: Telemetry,
    val integrityProvider: IntegrityProvider,
    val reviewPromptLauncher: ReviewPromptLauncher,
) {
    private val appContext = context.applicationContext
    private val application = appContext as android.app.Application

    val callRepository: CallRepository = DefaultCallRepository
    val settingsStore: AppSettingsStore = DataStoreAppSettingsStore
    val contactRepository: ContactRepository by lazy { ContactRepository(appContext) }
    val contactRecycleBin: ContactRecycleBin by lazy {
        ContactRecycleBinRepository(appContext, contactRepository)
    }
    val callContactLookupFactory: CallContactLookupFactory by lazy { CallContactLookupFactory() }
    val missedCallContactLookup: MissedCallContactLookup by lazy {
        RepositoryMissedCallContactLookup(contactRepository)
    }
    val holidaySource: HolidaySource by lazy {
        RepositoryHolidaySource(
            repository = HolidaysRepository(),
            connectivityChecker = ConnectivityChecker(appContext),
        )
    }
    val calendarEventSource: CalendarEventSource by lazy { CalendarRepository(appContext) }
    val gameStatsRepository by lazy { DataStoreGameStatsRepository.create(appContext) }
    val holidayPreferences: HolidayPreferences by lazy {
        SharedPreferencesHolidayPreferences(appContext)
    }
    val funnelTelemetry: FunnelTelemetry by lazy { FunnelTelemetry(telemetry) }

    val appSettingsViewModelFactory by lazy {
        AppSettingsViewModelFactory(settingsStore)
    }
    val contactsViewModelFactory by lazy {
        ContactsViewModelFactory(application, contactRepository, contactRecycleBin, funnelTelemetry)
    }
    val contactRecycleBinViewModelFactory by lazy {
        ContactRecycleBinViewModelFactory(contactRecycleBin)
    }
    val contactTagViewModelFactory by lazy {
        ContactTagViewModelFactory(appContext)
    }
    val duplicateContactsViewModelFactory by lazy {
        DuplicateContactsViewModelFactory(appContext)
    }
    val qrHistoryViewModelFactory by lazy {
        QRHistoryViewModelFactory(appContext)
    }
    val holidaysViewModelFactory by lazy {
        HolidaysViewModelFactory(
            holidaySource = holidaySource,
            calendarEventSource = calendarEventSource,
            holidayPreferences = holidayPreferences,
            initialCountry = Countries.getDefaultCountry(appContext),
            initialMonth = YearMonth.now(),
        )
    }
    val snakeViewModelFactory by lazy {
        SnakeViewModelFactory(gameStatsRepository)
    }
    val breakoutViewModelFactory by lazy {
        BreakoutViewModelFactory(gameStatsRepository)
    }
}
