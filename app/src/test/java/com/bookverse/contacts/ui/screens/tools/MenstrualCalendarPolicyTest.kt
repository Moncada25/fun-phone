package com.bookverse.contacts.ui.screens.tools

import com.bookverse.contacts.ui.viewmodel.MenstrualPhaseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

class MenstrualCalendarPolicyTest {
    @Test
    fun phaseForPastDateUsesThePreviousEstimatedCycle() {
        val baseStartDate = LocalDate.of(2026, 7, 1)
        val date = LocalDate.of(2026, 6, 30)

        val snapshot = MenstrualCalendarPolicy.snapshotForDate(
            date = date,
            baseStartDate = baseStartDate,
            cycleLength = 28,
            lutealLength = 14,
        )

        assertEquals(LocalDate.of(2026, 6, 3), snapshot.phases.first().startDate)
        assertEquals(MenstrualPhaseType.LUTEAL, MenstrualCalendarPolicy.phaseForDate(
            date = date,
            baseStartDate = baseStartDate,
            cycleLength = 28,
            lutealLength = 14,
        ))
    }

    @Test
    fun visibleDatesFillCompleteWeeksUsingTheLocalizedWeekStart() {
        val dates = MenstrualCalendarPolicy.visibleDates(
            month = YearMonth.of(2026, 1),
            firstDayOfWeek = DayOfWeek.MONDAY,
        )

        assertEquals(LocalDate.of(2025, 12, 29), dates.first())
        assertEquals(LocalDate.of(2026, 2, 1), dates.last())
        assertEquals(35, dates.size)
        assertEquals(DayOfWeek.SUNDAY, MenstrualCalendarPolicy.firstDayOfWeek(Locale.US))
        assertEquals(DayOfWeek.MONDAY, MenstrualCalendarPolicy.firstDayOfWeek(Locale.GERMANY))
    }

    @Test
    fun startDateCanNeverBeLaterThanToday() {
        val today = LocalDate.of(2026, 9, 14)

        assertTrue(MenstrualCalendarPolicy.isSelectableStartDate(today, today))
        assertTrue(MenstrualCalendarPolicy.isSelectableStartDate(today.minusDays(1), today))
        assertFalse(MenstrualCalendarPolicy.isSelectableStartDate(today.plusDays(1), today))
    }
}
