package com.bookverse.contacts.integration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bookverse.contacts.domain.holidays.CalendarEventSource
import com.bookverse.contacts.domain.holidays.HolidayPreferences
import com.bookverse.contacts.domain.holidays.HolidaySource
import com.bookverse.contacts.settings.Country
import com.bookverse.contacts.ui.viewmodel.HolidaysViewModel
import java.time.YearMonth

class HolidaysViewModelFactory(
    private val holidaySource: HolidaySource,
    private val calendarEventSource: CalendarEventSource,
    private val holidayPreferences: HolidayPreferences,
    private val initialCountry: Country?,
    private val initialMonth: YearMonth,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HolidaysViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return HolidaysViewModel(
            holidaySource = holidaySource,
            calendarEventSource = calendarEventSource,
            holidayPreferences = holidayPreferences,
            initialCountry = initialCountry,
            initialMonth = initialMonth,
        ) as T
    }
}
