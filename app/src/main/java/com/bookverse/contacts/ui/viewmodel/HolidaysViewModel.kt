package com.bookverse.contacts.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookverse.contacts.data.model.CalendarEvent
import com.bookverse.contacts.data.model.PublicHoliday
import com.bookverse.contacts.domain.holidays.CalendarEventSource
import com.bookverse.contacts.domain.holidays.CalendarSource
import com.bookverse.contacts.domain.holidays.HolidayPreferences
import com.bookverse.contacts.domain.holidays.HolidaySource
import com.bookverse.contacts.domain.holidays.HolidaySourceException
import com.bookverse.contacts.settings.Country
import com.bookverse.contacts.ui.state.HolidaysUiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId

data class HolidaysUiState(
    val selectedCountry: Country?,
    val selectedMonth: YearMonth,
    val holidays: List<PublicHoliday> = emptyList(),
    val calendarSources: List<CalendarSource> = emptyList(),
    val selectedCalendarIds: Set<Long> = emptySet(),
    val calendarEvents: List<CalendarEvent> = emptyList(),
    val isNetworkDisclosureAccepted: Boolean = false,
    val isLoadingHolidays: Boolean = false,
    val isLoadingCalendarSources: Boolean = false,
    val isLoadingCalendarEvents: Boolean = false,
    val holidaysError: HolidaysUiError? = null,
    val calendarError: Boolean = false,
)

class HolidaysViewModel(
    private val holidaySource: HolidaySource,
    private val calendarEventSource: CalendarEventSource,
    private val holidayPreferences: HolidayPreferences,
    initialCountry: Country?,
    initialMonth: YearMonth,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HolidaysUiState(
            selectedCountry = initialCountry,
            selectedMonth = initialMonth,
            selectedCalendarIds = holidayPreferences.loadSelectedCalendarIds(),
            isNetworkDisclosureAccepted = holidayPreferences.isNetworkDisclosureAccepted(),
        ),
    )
    val uiState: StateFlow<HolidaysUiState> = _uiState.asStateFlow()

    private var holidayLoadJob: Job? = null
    private var calendarLoadJob: Job? = null
    private var hasInitialLoad = false

    fun performInitialLoad() {
        if (hasInitialLoad || !_uiState.value.isNetworkDisclosureAccepted) return
        hasInitialLoad = true
        loadHolidays()
    }

    fun acceptNetworkDisclosure() {
        holidayPreferences.setNetworkDisclosureAccepted(true)
        _uiState.update { it.copy(isNetworkDisclosureAccepted = true) }
        performInitialLoad()
    }

    fun selectCountry(country: Country?) {
        if (_uiState.value.selectedCountry?.countryCode == country?.countryCode) return
        _uiState.update { it.copy(selectedCountry = country) }
        loadHolidays()
    }

    fun selectMonth(month: YearMonth) {
        val previous = _uiState.value.selectedMonth
        if (previous == month) return
        _uiState.update { it.copy(selectedMonth = month) }
        if (previous.year != month.year) loadHolidays()
        loadCalendarEvents()
    }

    fun selectYear(year: Int) {
        selectMonth(_uiState.value.selectedMonth.withYear(year))
    }

    fun previousMonth() = selectMonth(_uiState.value.selectedMonth.minusMonths(1))

    fun nextMonth() = selectMonth(_uiState.value.selectedMonth.plusMonths(1))

    fun goToToday() = selectMonth(YearMonth.now())

    fun retryHolidays() = loadHolidays()

    fun clearHolidaysError() {
        _uiState.update { it.copy(holidaysError = null) }
    }

    fun refreshCalendarSources() {
        _uiState.update { it.copy(isLoadingCalendarSources = true, calendarError = false) }
        viewModelScope.launch {
            try {
                val sources = calendarEventSource.getCalendars().distinctBy {
                    Triple(it.name.trim(), it.accountName.trim(), it.accountType.trim())
                }
                val migratedIds = migrateLegacyAccounts(sources)
                val availableIds = sources.mapTo(mutableSetOf(), CalendarSource::id)
                val selectedIds = (migratedIds ?: _uiState.value.selectedCalendarIds)
                    .intersect(availableIds)
                if (selectedIds != _uiState.value.selectedCalendarIds) {
                    holidayPreferences.saveSelectedCalendarIds(selectedIds)
                }
                _uiState.update {
                    it.copy(
                        calendarSources = sources,
                        selectedCalendarIds = selectedIds,
                        isLoadingCalendarSources = false,
                    )
                }
                loadCalendarEvents()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isLoadingCalendarSources = false, calendarError = true)
                }
            }
        }
    }

    fun applyCalendarSelection(calendarIds: Set<Long>) {
        _uiState.update { it.copy(selectedCalendarIds = calendarIds, calendarError = false) }
        holidayPreferences.saveSelectedCalendarIds(calendarIds)
        loadCalendarEvents()
    }

    fun refreshCalendarEvents() = loadCalendarEvents()

    private fun migrateLegacyAccounts(sources: List<CalendarSource>): Set<Long>? {
        if (_uiState.value.selectedCalendarIds.isNotEmpty()) return null
        val legacyAccounts = holidayPreferences.loadLegacySelectedAccounts()
        if (legacyAccounts.isEmpty()) return null
        val migrated = sources
            .filter { it.accountName in legacyAccounts }
            .mapTo(mutableSetOf(), CalendarSource::id)
        holidayPreferences.saveSelectedCalendarIds(migrated)
        holidayPreferences.clearLegacySelectedAccounts()
        return migrated
    }

    private fun loadHolidays() {
        val state = _uiState.value
        if (!state.isNetworkDisclosureAccepted) return
        val country = state.selectedCountry ?: return
        val year = state.selectedMonth.year

        holidayLoadJob?.cancel()
        holidayLoadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHolidays = true, holidaysError = null) }
            if (!holidaySource.hasInternetConnection()) {
                _uiState.update {
                    it.copy(isLoadingHolidays = false, holidaysError = HolidaysUiError.NoInternet)
                }
                return@launch
            }

            try {
                holidaySource.getHolidays(year, country.countryCode)
                    .onSuccess { holidays ->
                        _uiState.update {
                            it.copy(
                                holidays = holidays.sortedBy(PublicHoliday::date),
                                isLoadingHolidays = false,
                            )
                        }
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoadingHolidays = false,
                                holidaysError = mapErrorToUi(throwable),
                            )
                        }
                    }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingHolidays = false,
                        holidaysError = mapErrorToUi(error),
                    )
                }
            }
        }
    }

    private fun loadCalendarEvents() {
        val state = _uiState.value
        val selectedIds = state.selectedCalendarIds
        if (selectedIds.isEmpty()) {
            calendarLoadJob?.cancel()
            _uiState.update {
                it.copy(calendarEvents = emptyList(), isLoadingCalendarEvents = false)
            }
            return
        }

        val zone = ZoneId.systemDefault()
        val startDate = state.selectedMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val endDate = state.selectedMonth.plusMonths(1)
            .atDay(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli() - 1L

        calendarLoadJob?.cancel()
        calendarLoadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isLoadingCalendarEvents = true, calendarError = false)
            }
            try {
                val events = calendarEventSource.getCalendarEvents(selectedIds, startDate, endDate)
                _uiState.update {
                    it.copy(
                        calendarEvents = events.sortedBy(CalendarEvent::startDate),
                        isLoadingCalendarEvents = false,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isLoadingCalendarEvents = false, calendarError = true)
                }
            }
        }
    }

    private fun mapErrorToUi(throwable: Throwable): HolidaysUiError = when (throwable) {
        is HolidaySourceException.ClientError -> HolidaysUiError.Client(throwable.code)
        is HolidaySourceException.ServerError -> HolidaysUiError.Server(throwable.code)
        is HolidaySourceException.Unknown -> HolidaysUiError.Unknown(throwable.message)
        else -> HolidaysUiError.Unknown(throwable.message)
    }
}
