package com.bookverse.contacts.ui.viewmodel

import com.bookverse.contacts.data.model.CalendarAttendee
import com.bookverse.contacts.data.model.CalendarEvent
import com.bookverse.contacts.data.model.PublicHoliday
import com.bookverse.contacts.domain.holidays.CalendarEventSource
import com.bookverse.contacts.domain.holidays.CalendarSource
import com.bookverse.contacts.domain.holidays.HolidayPreferences
import com.bookverse.contacts.domain.holidays.HolidaySource
import com.bookverse.contacts.settings.Country
import com.bookverse.contacts.ui.state.HolidaysUiError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class HolidaysViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun shouldLoadHolidaysWhenSourceIsOnline() = runTest(dispatcher) {
        val source = FakeHolidaySource(isOnline = true)
        val viewModel = viewModel(source = source)

        viewModel.performInitialLoad()
        advanceUntilIdle()

        assertEquals(listOf(source.holiday), viewModel.uiState.value.holidays)
        assertEquals(2026, source.requestedYear)
        assertEquals("CO", source.requestedCountry)
        assertEquals(null, viewModel.uiState.value.holidaysError)
    }

    @Test
    fun shouldStartOnProvidedCurrentMonth() {
        val currentMonth = YearMonth.of(2026, 8)

        val viewModel = viewModel(initialMonth = currentMonth)

        assertEquals(currentMonth, viewModel.uiState.value.selectedMonth)
    }

    @Test
    fun shouldExposeNoInternetWithoutCallingHolidaySource() = runTest(dispatcher) {
        val source = FakeHolidaySource(isOnline = false)
        val viewModel = viewModel(source = source)

        viewModel.performInitialLoad()
        advanceUntilIdle()

        assertEquals(HolidaysUiError.NoInternet, viewModel.uiState.value.holidaysError)
        assertTrue(source.requestedYear == null)
    }

    @Test
    fun previousMonthCrossesIntoThePreviousYearAndReloadsHolidays() = runTest(dispatcher) {
        val source = FakeHolidaySource(isOnline = true)
        val viewModel = viewModel(source = source)

        viewModel.previousMonth()
        advanceUntilIdle()

        assertEquals(2025, viewModel.uiState.value.selectedMonth.year)
        assertEquals(12, viewModel.uiState.value.selectedMonth.monthValue)
        assertEquals(2025, source.requestedYear)
    }

    @Test
    fun shouldPersistSelectedCalendarsAndLoadTheirEvents() = runTest(dispatcher) {
        val preferences = FakeHolidayPreferences()
        val calendarSource = FakeCalendarEventSource()
        val viewModel = viewModel(
            calendarEventSource = calendarSource,
            preferences = preferences,
        )

        viewModel.applyCalendarSelection(setOf(11L, 12L))
        advanceUntilIdle()

        assertEquals(setOf(11L, 12L), preferences.savedCalendarIds)
        assertEquals(setOf(11L, 12L), calendarSource.requestedCalendarIds)
        assertEquals(listOf(calendarSource.event), viewModel.uiState.value.calendarEvents)
    }

    @Test
    fun shouldMigrateEveryCalendarMatchingLegacyAccounts() = runTest(dispatcher) {
        val preferences = FakeHolidayPreferences(
            legacyAccounts = setOf("ada@example.com"),
        )
        val calendarSource = FakeCalendarEventSource()
        val viewModel = viewModel(calendarEventSource = calendarSource, preferences = preferences)

        viewModel.refreshCalendarSources()
        advanceUntilIdle()

        assertEquals(setOf(11L, 12L), viewModel.uiState.value.selectedCalendarIds)
        assertEquals(setOf(11L, 12L), preferences.savedCalendarIds)
        assertTrue(preferences.legacyCleared)
    }

    private fun viewModel(
        source: HolidaySource = FakeHolidaySource(isOnline = true),
        calendarEventSource: CalendarEventSource = FakeCalendarEventSource(),
        preferences: HolidayPreferences = FakeHolidayPreferences(),
        initialMonth: YearMonth = YearMonth.of(2026, 1),
    ) = HolidaysViewModel(
        holidaySource = source,
        calendarEventSource = calendarEventSource,
        holidayPreferences = preferences,
        initialCountry = Country(
            name = "Colombia",
            code = "+57",
            flag = "CO",
            countryCode = "CO",
        ),
        initialMonth = initialMonth,
    )

    private class FakeHolidaySource(
        private val isOnline: Boolean,
    ) : HolidaySource {
        val holiday = PublicHoliday(
            date = "2026-07-20",
            localName = "Día de la Independencia",
            name = "Independence Day",
            countryCode = "CO",
            fixed = true,
            global = true,
            counties = null,
            launchYear = null,
            types = listOf("Public"),
        )
        var requestedYear: Int? = null
        var requestedCountry: String? = null

        override fun hasInternetConnection(): Boolean = isOnline

        override suspend fun getHolidays(
            year: Int,
            countryCode: String,
        ): Result<List<PublicHoliday>> {
            requestedYear = year
            requestedCountry = countryCode
            return Result.success(listOf(holiday))
        }
    }

    private class FakeCalendarEventSource : CalendarEventSource {
        val event = CalendarEvent(
            id = "event-1",
            title = "Calendar event",
            description = null,
            location = null,
            startDate = 20L,
            endDate = 30L,
            calendarId = "calendar-1",
            calendarName = "Personal",
            accountName = "ada@example.com",
            allDay = true,
            attendees = listOf<CalendarAttendee>(),
        )
        private val calendars = listOfNotNull(
            CalendarSource(11L, "Personal", "ada@example.com", "com.google"),
            CalendarSource(
                12L,
                "Work",
                "ada@example.com",
                "com.google",
            ),
            CalendarSource(13L, "Other", "grace@example.com", "local"),
        )
        var requestedCalendarIds: Set<Long> = emptySet()

        override suspend fun getCalendarEvents(
            calendarIds: Set<Long>,
            startDate: Long,
            endDate: Long,
        ): List<CalendarEvent> {
            requestedCalendarIds = calendarIds
            return listOf(event)
        }

        override suspend fun getCalendars(): List<CalendarSource> = calendars
    }

    private class FakeHolidayPreferences(
        private var disclosureAccepted: Boolean = true,
        private val legacyAccounts: Set<String> = emptySet(),
    ) : HolidayPreferences {
        var savedCalendarIds: Set<Long> = emptySet()
        var legacyCleared = false

        override fun isNetworkDisclosureAccepted(): Boolean = disclosureAccepted

        override fun setNetworkDisclosureAccepted(accepted: Boolean) {
            disclosureAccepted = accepted
        }

        override fun loadSelectedCalendarIds(): Set<Long> = savedCalendarIds

        override fun saveSelectedCalendarIds(calendarIds: Set<Long>) {
            savedCalendarIds = calendarIds
        }

        override fun loadLegacySelectedAccounts(): Set<String> = legacyAccounts

        override fun clearLegacySelectedAccounts() {
            legacyCleared = true
        }
    }
}
