package com.bookverse.contacts.ui.screens.tools

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bookverse.contacts.R
import com.bookverse.contacts.testing.ComposeTestContent
import com.bookverse.contacts.testing.ComposeTestHostActivity
import com.bookverse.contacts.ui.viewmodel.MenstrualCalendarUiState
import com.bookverse.contacts.ui.viewmodel.MenstrualPhaseType
import com.bookverse.contacts.ui.viewmodel.calculateCyclePhases
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class MenstrualCalendarScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestHostActivity>()

    @Test
    fun currentMonthDayAnnouncesPhaseAndInvokesSelection() {
        val date = LocalDate.of(2026, 9, 14)
        val clicks = AtomicInteger(0)
        val dateLabel = date.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.ENGLISH),
        )
        val description = composeRule.activity.getString(
            R.string.menstrual_calendar_day_description,
            dateLabel,
            composeRule.activity.getString(R.string.menstrual_phase_follicular),
            composeRule.activity.getString(R.string.menstrual_today_marker),
        )

        showContent {
            Row {
                CalendarDayCell(
                    date = date,
                    phase = MenstrualPhaseType.FOLLICULAR,
                    isCurrentMonth = true,
                    isToday = true,
                    isSelected = false,
                    locale = Locale.ENGLISH,
                    onClick = { clicks.incrementAndGet() },
                )
            }
        }

        composeRule.onNodeWithContentDescription(description).assertIsDisplayed().performClick()
        assertEquals(1, clicks.get())
    }

    @Test
    fun calendarControlsKeepMonthNavigationAndTodayActionAvailable() {
        val previous = AtomicInteger(0)
        val next = AtomicInteger(0)
        val today = AtomicInteger(0)

        showContent {
            CycleCalendarCard(
                baseStartDate = LocalDate.of(2026, 9, 1),
                cycleLength = 28,
                lutealLength = 14,
                displayedMonth = YearMonth.of(2026, 9),
                selectedDate = null,
                today = LocalDate.of(2026, 9, 14),
                locale = Locale.ENGLISH,
                onPreviousMonth = { previous.incrementAndGet() },
                onNextMonth = { next.incrementAndGet() },
                onToday = { today.incrementAndGet() },
                onDateSelected = {},
            )
        }

        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.menstrual_calendar_previous_month),
        ).performClick()
        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.menstrual_calendar_next_month),
        ).performClick()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.menstrual_calendar_today),
        ).performClick()

        assertEquals(1, previous.get())
        assertEquals(1, next.get())
        assertEquals(1, today.get())
    }

    @Test
    fun configuredSettingsOfferTheResetAction() {
        val reset = AtomicInteger(0)
        showContent {
            CycleConfigurationSheet(
                state = MenstrualCalendarUiState(
                    selectedStartDate = LocalDate.of(2026, 9, 1),
                    snapshot = calculateCyclePhases(LocalDate.of(2026, 9, 1)),
                    isLoaded = true,
                ),
                isConfigured = true,
                onDateClick = {},
                onCycleChange = {},
                onLutealChange = {},
                onSave = {},
                onReset = { reset.incrementAndGet() },
                onDismiss = {},
            )
        }

        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.menstrual_reset_action),
        ).performClick()

        assertEquals(1, reset.get())
    }

    private fun showContent(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.runOnUiThread {
            ComposeTestContent.show {
                MaterialTheme(content = content)
            }
        }
        composeRule.waitForIdle()
    }
}
