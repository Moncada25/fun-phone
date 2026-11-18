package com.bookverse.contacts.ui.screens.tools

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.ui.viewmodel.PomodoroSettings
import com.bookverse.contacts.ui.viewmodel.PomodoroTimerState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class PomodoroScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun timerDisplayAnnouncesTimeAndReadyState() {
        showContent {
            TimerDisplay(
                timeLeftSeconds = 25 * 60,
                totalSeconds = 25 * 60,
                timerState = PomodoroTimerState.IDLE,
                accentColor = Color(0xFF6750A4),
            )
        }

        composeRule.onNodeWithText("25:00").assertIsDisplayed()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.pomodoro_ready))
            .assertIsDisplayed()
    }

    @Test
    fun idleControlsKeepWorkAndBreakActions() {
        val workCount = AtomicInteger(0)
        val breakCount = AtomicInteger(0)
        showContent {
            ControlButtons(
                timerState = PomodoroTimerState.IDLE,
                accentColor = Color(0xFF6750A4),
                onStartWork = { workCount.incrementAndGet() },
                onStartBreak = { breakCount.incrementAndGet() },
                onPause = {},
                onResume = {},
                onReset = {},
                onSkip = {},
                expanded = false,
            )
        }

        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.pomodoro_start_work))
            .performClick()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.pomodoro_start_break))
            .performClick()

        assertEquals(1, workCount.get())
        assertEquals(1, breakCount.get())
    }

    @Test
    fun activeControlsKeepPauseResetAndSkipActions() {
        val pauseCount = AtomicInteger(0)
        val resetCount = AtomicInteger(0)
        val skipCount = AtomicInteger(0)
        showContent {
            ControlButtons(
                timerState = PomodoroTimerState.WORK,
                accentColor = Color(0xFF6750A4),
                onStartWork = {},
                onStartBreak = {},
                onPause = { pauseCount.incrementAndGet() },
                onResume = {},
                onReset = { resetCount.incrementAndGet() },
                onSkip = { skipCount.incrementAndGet() },
                expanded = false,
            )
        }

        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.pause_label))
            .performClick()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.reset_label))
            .performClick()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.pomodoro_skip))
            .performClick()

        assertEquals(1, pauseCount.get())
        assertEquals(1, resetCount.get())
        assertEquals(1, skipCount.get())
    }

    @Test
    fun settingsSheetShowsAllFourConfigurableValuesAndSavesThem() {
        val savedSettings = AtomicReference<PomodoroSettings>()
        val settings = PomodoroSettings(
            workMinutes = 25,
            breakMinutes = 5,
            longBreakMinutes = 15,
            sessionsUntilLongBreak = 4,
        )
        showContent {
            PomodoroSettingsSheet(
                settings = settings,
                accentColor = Color(0xFF6750A4),
                onDismiss = {},
                onSave = savedSettings::set,
            )
        }

        listOf(
            R.string.pomodoro_work_minutes,
            R.string.pomodoro_break_minutes,
            R.string.pomodoro_long_break_minutes,
            R.string.pomodoro_sessions_until_long_break,
        ).forEach { resourceId ->
            composeRule.onNodeWithText(composeRule.activity.getString(resourceId)).assertIsDisplayed()
        }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.cancel)).assertDoesNotExist()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.save)).performClick()

        assertEquals(settings, savedSettings.get())
    }

    private fun showContent(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent {
            MaterialTheme(content = content)
        }
        composeRule.waitForIdle()
    }
}
