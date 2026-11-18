package com.bookverse.contacts.ui.screens.notes

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderPermissionActionTest {
    @Test
    fun shouldRequestNotificationPermissionFirstWhenBothPermissionsAreMissing() {
        assertEquals(
            ReminderPermissionAction.REQUEST_NOTIFICATION_PERMISSION,
            resolveReminderPermissionAction(
                requiresNotificationPermission = true,
                notificationPermissionGranted = false,
                requiresExactAlarmPermission = true,
                exactAlarmPermissionGranted = false
            )
        )
    }

    @Test
    fun shouldRequestExactAlarmPermissionAfterNotificationPermissionIsGranted() {
        assertEquals(
            ReminderPermissionAction.REQUEST_EXACT_ALARM_PERMISSION,
            resolveReminderPermissionAction(
                requiresNotificationPermission = true,
                notificationPermissionGranted = true,
                notificationChannelsEnabled = true,
                requiresExactAlarmPermission = true,
                exactAlarmPermissionGranted = false
            )
        )
    }

    @Test
    fun shouldOpenNotificationSettingsWhenReminderChannelIsDisabled() {
        assertEquals(
            ReminderPermissionAction.OPEN_NOTIFICATION_SETTINGS,
            resolveReminderPermissionAction(
                requiresNotificationPermission = true,
                notificationPermissionGranted = true,
                notificationChannelsEnabled = false,
                requiresExactAlarmPermission = true,
                exactAlarmPermissionGranted = true
            )
        )
    }

    @Test
    fun shouldSaveWhenRequiredPermissionsAreGranted() {
        assertEquals(
            ReminderPermissionAction.SAVE,
            resolveReminderPermissionAction(
                requiresNotificationPermission = true,
                notificationPermissionGranted = true,
                requiresExactAlarmPermission = true,
                exactAlarmPermissionGranted = true
            )
        )
    }

    @Test
    fun shouldSaveOnOlderAndroidWhenRuntimePermissionsAreNotRequired() {
        assertEquals(
            ReminderPermissionAction.SAVE,
            resolveReminderPermissionAction(
                requiresNotificationPermission = false,
                notificationPermissionGranted = false,
                requiresExactAlarmPermission = false,
                exactAlarmPermissionGranted = false
            )
        )
    }
}
