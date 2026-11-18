package com.bookverse.contacts.ui.screens.notes

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.notes.ReminderRepeat

@Composable
internal fun reminderRepeatLabel(mode: ReminderRepeat): String = stringResource(
    when (mode) {
        ReminderRepeat.NONE -> R.string.reminder_repeat_none
        ReminderRepeat.DAILY -> R.string.reminder_repeat_daily
        ReminderRepeat.WEEKLY -> R.string.reminder_repeat_weekly
        ReminderRepeat.MONTHLY -> R.string.reminder_repeat_monthly
    }
)
