package com.bookverse.contacts.ui.screens.notes

enum class ReminderPermissionAction {
    REQUEST_NOTIFICATION_PERMISSION,
    OPEN_NOTIFICATION_SETTINGS,
    REQUEST_EXACT_ALARM_PERMISSION,
    SAVE
}

fun resolveReminderPermissionAction(
    requiresNotificationPermission: Boolean,
    notificationPermissionGranted: Boolean,
    notificationChannelsEnabled: Boolean = true,
    requiresExactAlarmPermission: Boolean,
    exactAlarmPermissionGranted: Boolean
): ReminderPermissionAction = when {
    requiresNotificationPermission && !notificationPermissionGranted ->
        ReminderPermissionAction.REQUEST_NOTIFICATION_PERMISSION

    !notificationChannelsEnabled -> ReminderPermissionAction.OPEN_NOTIFICATION_SETTINGS

    requiresExactAlarmPermission && !exactAlarmPermissionGranted ->
        ReminderPermissionAction.REQUEST_EXACT_ALARM_PERMISSION

    else -> ReminderPermissionAction.SAVE
}
