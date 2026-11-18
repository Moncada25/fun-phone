package com.bookverse.contacts.domain.holidays

interface HolidayPreferences {
    fun isNetworkDisclosureAccepted(): Boolean

    fun setNetworkDisclosureAccepted(accepted: Boolean)

    fun loadSelectedCalendarIds(): Set<Long>

    fun saveSelectedCalendarIds(calendarIds: Set<Long>)

    fun loadLegacySelectedAccounts(): Set<String>

    fun clearLegacySelectedAccounts()
}
