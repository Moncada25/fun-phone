package com.bookverse.contacts.data.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderOccurrencesTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val bogota = ZoneId.of("America/Bogota")

    private fun at(
        zone: ZoneId,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int = 0,
    ): Long = LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun futureAnchorIsTheNextOccurrenceForEveryMode() {
        val anchor = at(bogota, 2026, 3, 10, 9)
        ReminderRepeat.entries.forEach { mode ->
            assertEquals(anchor, nextReminderOccurrence(anchor, mode, anchor - 1L, bogota))
        }
    }

    @Test
    fun nonRepeatingReminderHasNoOccurrenceOnceTheAnchorPassed() {
        val anchor = at(bogota, 2026, 3, 10, 9)
        assertNull(nextReminderOccurrence(anchor, ReminderRepeat.NONE, anchor, bogota))
        assertNull(nextReminderOccurrence(anchor, ReminderRepeat.NONE, anchor + 86_400_000L, bogota))
    }

    @Test
    fun dailyFiresAgainTheNextDayAtTheSameTime() {
        val anchor = at(bogota, 2026, 3, 10, 9, 30)
        assertEquals(
            at(bogota, 2026, 3, 11, 9, 30),
            nextReminderOccurrence(anchor, ReminderRepeat.DAILY, anchor, bogota),
        )
        assertEquals(
            at(bogota, 2026, 3, 12, 9, 30),
            nextReminderOccurrence(anchor, ReminderRepeat.DAILY, at(bogota, 2026, 3, 11, 9, 30), bogota),
        )
    }

    @Test
    fun weeklyKeepsTheWeekday() {
        val anchor = at(bogota, 2026, 3, 10, 18)
        assertEquals(
            at(bogota, 2026, 3, 17, 18),
            nextReminderOccurrence(anchor, ReminderRepeat.WEEKLY, at(bogota, 2026, 3, 12, 0), bogota),
        )
        assertEquals(
            at(bogota, 2026, 3, 24, 18),
            nextReminderOccurrence(anchor, ReminderRepeat.WEEKLY, at(bogota, 2026, 3, 17, 18), bogota),
        )
    }

    @Test
    fun monthlyClampsToTheLastDayAndReturnsToTheAnchorDay() {
        val anchor = at(bogota, 2026, 1, 31, 8)
        val february = nextReminderOccurrence(anchor, ReminderRepeat.MONTHLY, anchor, bogota)
        assertEquals(at(bogota, 2026, 2, 28, 8), february)
        val march = nextReminderOccurrence(anchor, ReminderRepeat.MONTHLY, february!!, bogota)
        assertEquals(at(bogota, 2026, 3, 31, 8), march)
        val april = nextReminderOccurrence(anchor, ReminderRepeat.MONTHLY, march!!, bogota)
        assertEquals(at(bogota, 2026, 4, 30, 8), april)
        assertEquals(
            at(bogota, 2028, 2, 29, 8),
            nextReminderOccurrence(anchor, ReminderRepeat.MONTHLY, at(bogota, 2028, 2, 1, 0), bogota),
        )
    }

    @Test
    fun anchorFarInThePastRollsForwardWithoutDrift() {
        val anchor = at(bogota, 2020, 1, 31, 7, 15)
        assertEquals(
            at(bogota, 2026, 11, 14, 7, 15),
            nextReminderOccurrence(anchor, ReminderRepeat.DAILY, at(bogota, 2026, 11, 13, 12), bogota),
        )
        assertEquals(
            at(bogota, 2026, 11, 30, 7, 15),
            nextReminderOccurrence(anchor, ReminderRepeat.MONTHLY, at(bogota, 2026, 11, 13, 12), bogota),
        )
        val weekly = nextReminderOccurrence(
            anchor,
            ReminderRepeat.WEEKLY,
            at(bogota, 2026, 11, 13, 12),
            bogota,
        )
        assertEquals(at(bogota, 2026, 11, 20, 7, 15), weekly)
    }

    @Test
    fun dailyKeepsTheWallClockTimeAcrossDaylightSavingChanges() {
        val winterAnchor = at(madrid, 2026, 3, 27, 9)
        assertEquals(
            at(madrid, 2026, 3, 29, 9),
            nextReminderOccurrence(winterAnchor, ReminderRepeat.DAILY, at(madrid, 2026, 3, 28, 9), madrid),
        )
        assertEquals(
            23L * 3_600_000L,
            at(madrid, 2026, 3, 29, 9) - at(madrid, 2026, 3, 28, 9),
        )
        val summerAnchor = at(madrid, 2026, 10, 20, 9)
        assertEquals(
            at(madrid, 2026, 10, 25, 9),
            nextReminderOccurrence(summerAnchor, ReminderRepeat.DAILY, at(madrid, 2026, 10, 24, 9), madrid),
        )
        assertEquals(
            at(madrid, 2026, 10, 27, 9),
            nextReminderOccurrence(summerAnchor, ReminderRepeat.WEEKLY, at(madrid, 2026, 10, 24, 9), madrid),
        )
    }

    @Test
    fun occurrenceInsideTheSpringGapMovesForwardOnlyThatDay() {
        val anchor = at(madrid, 2026, 3, 27, 2, 30)
        val gapDay = nextReminderOccurrence(anchor, ReminderRepeat.DAILY, at(madrid, 2026, 3, 28, 2, 30), madrid)
        assertEquals(at(madrid, 2026, 3, 29, 3, 30), gapDay)
        assertEquals(
            at(madrid, 2026, 3, 30, 2, 30),
            nextReminderOccurrence(anchor, ReminderRepeat.DAILY, gapDay!!, madrid),
        )
    }

    @Test
    fun periodicRemindersNeverRepeatByCalendar() {
        val reminder = ReminderData(
            noteId = 1L,
            date = at(bogota, 2026, 3, 10, 9),
            interval = 60,
            message = "",
            type = ReminderType.PERIODIC,
            repeatMode = ReminderRepeat.DAILY,
        )
        assertEquals(ReminderRepeat.NONE, reminder.effectiveRepeat)
        assertNull(reminder.nextOccurrence(reminder.date, bogota))
        val event = reminder.copy(type = ReminderType.SINGLE_EVENT)
        assertEquals(at(bogota, 2026, 3, 11, 9), event.nextOccurrence(event.date, bogota))
    }
}
