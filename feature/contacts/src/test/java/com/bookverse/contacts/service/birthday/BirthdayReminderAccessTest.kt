package com.bookverse.contacts.service.birthday

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BirthdayReminderAccessTest {
    @Test
    fun shouldBeReady_whenEveryAccessIsEnabled() {
        assertNull(BirthdayReminderAccess.resolve(true, true, true, true))
    }

    @Test
    fun shouldRequestNotificationPermission_beforeOtherRecoveryActions() {
        assertEquals(
            BirthdayReminderAccessIssue.NOTIFICATION_PERMISSION,
            BirthdayReminderAccess.resolve(false, false, false, false),
        )
    }

    @Test
    fun shouldRequestExactAlarmAccess_whenNotificationsAreAvailable() {
        assertEquals(
            BirthdayReminderAccessIssue.EXACT_ALARM_PERMISSION,
            BirthdayReminderAccess.resolve(true, true, false, true),
        )
    }

    @Test
    fun shouldOpenNotificationSettings_whenNotificationsAreDisabled() {
        assertEquals(
            BirthdayReminderAccessIssue.NOTIFICATION_SETTINGS,
            BirthdayReminderAccess.resolve(true, false, true, true),
        )
        assertEquals(
            BirthdayReminderAccessIssue.NOTIFICATION_SETTINGS,
            BirthdayReminderAccess.resolve(true, true, true, false),
        )
    }
}
