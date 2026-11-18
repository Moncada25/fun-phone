package com.bookverse.contacts.ui.screens.tools

import com.bookverse.contacts.ui.viewmodel.DEFAULT_MENSTRUATION_LENGTH
import com.bookverse.contacts.ui.viewmodel.MenstrualCycleSnapshot
import com.bookverse.contacts.ui.viewmodel.MenstrualPhaseType
import com.bookverse.contacts.ui.viewmodel.calculateCyclePhases
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

/** Pure prediction helpers used by the calendar's transient presentation state. */
object MenstrualCalendarPolicy {
    fun firstDayOfWeek(locale: Locale): DayOfWeek = WeekFields.of(locale).firstDayOfWeek

    fun visibleDates(
        month: YearMonth,
        firstDayOfWeek: DayOfWeek,
    ): List<LocalDate> {
        val monthStart = month.atDay(1)
        val leadingDays = dayOffset(monthStart.dayOfWeek, firstDayOfWeek)
        val monthEnd = month.atEndOfMonth()
        val trailingDays = (6 - dayOffset(monthEnd.dayOfWeek, firstDayOfWeek) + 7) % 7
        return generateSequence(monthStart.minusDays(leadingDays.toLong())) { it.plusDays(1) }
            .takeWhile { !it.isAfter(monthEnd.plusDays(trailingDays.toLong())) }
            .toList()
    }

    fun snapshotForDate(
        date: LocalDate,
        baseStartDate: LocalDate,
        cycleLength: Int,
        lutealLength: Int,
    ): MenstrualCycleSnapshot {
        val safeCycleLength = cycleLength.coerceAtLeast(1)
        val elapsedDays = ChronoUnit.DAYS.between(baseStartDate, date)
        val cycleOffset = Math.floorDiv(elapsedDays, safeCycleLength.toLong())
        val cycleStart = baseStartDate.plusDays(cycleOffset * safeCycleLength)
        return calculateCyclePhases(
            startDate = cycleStart,
            cycleLength = safeCycleLength,
            lutealLength = lutealLength,
            menstruationLength = DEFAULT_MENSTRUATION_LENGTH,
        )
    }

    fun phaseForDate(
        date: LocalDate,
        baseStartDate: LocalDate,
        cycleLength: Int,
        lutealLength: Int,
    ): MenstrualPhaseType? = snapshotForDate(
        date = date,
        baseStartDate = baseStartDate,
        cycleLength = cycleLength,
        lutealLength = lutealLength,
    ).phases.firstOrNull { date in it.startDate..it.endDate }?.type

    fun phasesForRange(
        range: Iterable<LocalDate>,
        baseStartDate: LocalDate,
        cycleLength: Int,
        lutealLength: Int,
    ): Map<LocalDate, MenstrualPhaseType> = range.associateWith { date ->
        requireNotNull(
            phaseForDate(
                date = date,
                baseStartDate = baseStartDate,
                cycleLength = cycleLength,
                lutealLength = lutealLength,
            ),
        )
    }

    fun isSelectableStartDate(
        date: LocalDate,
        today: LocalDate,
    ): Boolean = !date.isAfter(today)

    private fun dayOffset(day: DayOfWeek, firstDayOfWeek: DayOfWeek): Int {
        val difference = day.value - firstDayOfWeek.value
        return (difference + DayOfWeek.entries.size) % DayOfWeek.entries.size
    }
}
