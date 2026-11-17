package com.bookverse.contacts.integration

import android.content.Context
import androidx.core.content.edit
import com.bookverse.contacts.data.model.PublicHoliday
import com.bookverse.contacts.data.repository.HolidaysRepository
import com.bookverse.contacts.data.repository.HolidaysRepositoryException
import com.bookverse.contacts.domain.holidays.HolidayPreferences
import com.bookverse.contacts.domain.holidays.HolidaySource
import com.bookverse.contacts.domain.holidays.HolidaySourceException
import com.bookverse.contacts.utils.ConnectivityChecker

class RepositoryHolidaySource(
    private val repository: HolidaysRepository,
    private val connectivityChecker: ConnectivityChecker,
) : HolidaySource {
    override fun hasInternetConnection(): Boolean = connectivityChecker.hasInternetConnection()

    override suspend fun getHolidays(
        year: Int,
        countryCode: String,
    ): Result<List<PublicHoliday>> =
        repository.getHolidays(year, countryCode).fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(it.toHolidaySourceException()) },
        )

    private fun Throwable.toHolidaySourceException(): HolidaySourceException = when (this) {
        is HolidaysRepositoryException.ClientError -> HolidaySourceException.ClientError(code)
        is HolidaysRepositoryException.ServerError -> HolidaySourceException.ServerError(code)
        is HolidaysRepositoryException.Unknown -> HolidaySourceException.Unknown(detail, original)
        else -> HolidaySourceException.Unknown(original = this)
    }
}

class SharedPreferencesHolidayPreferences(context: Context) : HolidayPreferences {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    override fun isNetworkDisclosureAccepted(): Boolean =
        preferences.getBoolean(NETWORK_DISCLOSURE_ACCEPTED_KEY, false)

    override fun setNetworkDisclosureAccepted(accepted: Boolean) {
        preferences.edit {
            putBoolean(NETWORK_DISCLOSURE_ACCEPTED_KEY, accepted)
        }
    }

    override fun loadSelectedCalendarIds(): Set<Long> =
        preferences.getStringSet(SELECTED_CALENDAR_IDS_KEY, emptySet())
            .orEmpty()
            .mapNotNull(String::toLongOrNull)
            .toSet()

    override fun saveSelectedCalendarIds(calendarIds: Set<Long>) {
        preferences.edit {
            putStringSet(SELECTED_CALENDAR_IDS_KEY, calendarIds.map(Long::toString).toSet())
        }
    }

    override fun loadLegacySelectedAccounts(): Set<String> {
        val accounts = preferences.getString(SELECTED_ACCOUNTS_KEY, "").orEmpty()
        return accounts
            .takeIf { it.isNotBlank() }
            ?.split(",")
            ?.toSet()
            ?: emptySet()
    }

    override fun clearLegacySelectedAccounts() {
        preferences.edit {
            remove(SELECTED_ACCOUNTS_KEY)
        }
    }

    private companion object {
        const val PREFS_NAME = "holidays_prefs"
        const val SELECTED_ACCOUNTS_KEY = "selected_accounts"
        const val SELECTED_CALENDAR_IDS_KEY = "selected_calendar_ids"
        const val NETWORK_DISCLOSURE_ACCEPTED_KEY = "network_disclosure_accepted"
    }
}
