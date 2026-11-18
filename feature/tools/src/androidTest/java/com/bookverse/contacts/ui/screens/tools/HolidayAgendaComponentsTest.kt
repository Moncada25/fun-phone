package com.bookverse.contacts.ui.screens.tools

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bookverse.contacts.domain.holidays.BirthdayAgendaEntry
import com.bookverse.contacts.domain.holidays.CalendarSource
import com.bookverse.contacts.feature.tools.R
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean
import com.bookverse.contacts.core.ui.R as CoreUiR

class HolidayAgendaComponentsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun birthdayRowShowsContactAndLocalizedBadge() {
        composeRule.setContent {
            MaterialTheme {
                AgendaBirthdayRow(
                    birthday = BirthdayAgendaEntry(
                        contactId = "1",
                        contactName = "Ada Lovelace",
                        date = LocalDate.of(2026, 12, 10),
                    ),
                    accentColor = Color.Blue,
                )
            }
        }

        composeRule.onNodeWithText("Ada Lovelace").assertIsDisplayed()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.holidays_birthday_badge),
        ).assertIsDisplayed()
    }

    @Test
    fun calendarDayAnnouncesBirthdayAndCalendarEvent() {
        composeRule.setContent {
            MaterialTheme {
                CalendarDayCell(
                    day = 9,
                    isToday = false,
                    isHoliday = false,
                    hasEvent = true,
                    hasBirthday = true,
                    accentColor = Color.Blue,
                )
            }
        }

        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(
                R.string.holidays_calendar_day_birthday_event,
                9,
            ),
        ).assertIsDisplayed()
    }

    @Test
    fun calendarSelectorShowsSyncedCalendarOnlyOnce() {
        val calendars = listOf(
            CalendarSource(1L, "Personal", "ada@example.com", "com.google"),
            CalendarSource(2L, "Work", "grace@example.com", "com.google"),
            CalendarSource(3L, "Work", "grace@example.com", "com.google"),
        )
        composeRule.setContent {
            MaterialTheme {
                CalendarSelectorSheet(
                    calendars = calendars,
                    selectedIds = emptySet(),
                    isLoading = false,
                    accentColor = Color.Blue,
                    onApply = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onAllNodesWithText("Work").assertCountEquals(1)
    }

    @Test
    fun calendarSelectorCancelDismissesSheet() {
        val dismissed = AtomicBoolean(false)
        composeRule.setContent {
            MaterialTheme {
                CalendarSelectorSheet(
                    calendars = emptyList(),
                    selectedIds = emptySet(),
                    isLoading = false,
                    accentColor = Color.Blue,
                    onApply = {},
                    onDismiss = { dismissed.set(true) },
                )
            }
        }

        composeRule.onNodeWithText(
            composeRule.activity.getString(CoreUiR.string.cancel),
        ).assertIsDisplayed().performClick()
        assertTrue(dismissed.get())
    }

    @Test
    fun pastAgendaToggleInvokesExpansion() {
        val toggled = AtomicBoolean(false)
        composeRule.setContent {
            MaterialTheme {
                PastAgendaToggle(
                    pastItemCount = 3,
                    expanded = false,
                    accentColor = Color.Blue,
                    onToggle = { toggled.set(true) },
                )
            }
        }

        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.holidays_show_past_agenda, 3),
        ).assertIsDisplayed().performClick()
        assertTrue(toggled.get())
    }
}
