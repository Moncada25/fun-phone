package com.bookverse.contacts.ui.screens.settings

enum class PhoneStatusRequirement {
    DEFAULT_DIALER,
    NOTIFICATIONS,
    CONTACTS,
    CALL_LOG,
    PHONE_STATE,
    CALL_PHONE,
    EXACT_ALARMS,
}

fun resolveNextPhoneStatusRequirement(
    isDefaultDialer: Boolean,
    hasCallNotifications: Boolean,
    hasContactsPermission: Boolean,
    hasCallLogPermission: Boolean,
    hasPhoneStatePermission: Boolean,
    hasCallPhonePermission: Boolean,
    hasExactAlarmsPermission: Boolean = true,
): PhoneStatusRequirement? = resolveMissingPhoneStatusRequirements(
    isDefaultDialer = isDefaultDialer,
    hasCallNotifications = hasCallNotifications,
    hasContactsPermission = hasContactsPermission,
    hasCallLogPermission = hasCallLogPermission,
    hasPhoneStatePermission = hasPhoneStatePermission,
    hasCallPhonePermission = hasCallPhonePermission,
    hasExactAlarmsPermission = hasExactAlarmsPermission,
).firstOrNull()

fun resolveMissingPhoneStatusRequirements(
    isDefaultDialer: Boolean,
    hasCallNotifications: Boolean,
    hasContactsPermission: Boolean,
    hasCallLogPermission: Boolean,
    hasPhoneStatePermission: Boolean,
    hasCallPhonePermission: Boolean,
    hasExactAlarmsPermission: Boolean = true,
): List<PhoneStatusRequirement> = buildList {
    if (!isDefaultDialer) add(PhoneStatusRequirement.DEFAULT_DIALER)
    if (!hasCallNotifications) add(PhoneStatusRequirement.NOTIFICATIONS)
    if (!hasContactsPermission) add(PhoneStatusRequirement.CONTACTS)
    if (!hasCallLogPermission) add(PhoneStatusRequirement.CALL_LOG)
    if (!hasPhoneStatePermission) add(PhoneStatusRequirement.PHONE_STATE)
    if (!hasCallPhonePermission) add(PhoneStatusRequirement.CALL_PHONE)
    if (!hasExactAlarmsPermission) add(PhoneStatusRequirement.EXACT_ALARMS)
}

internal fun requiresNotificationSettings(
    sdkInt: Int,
    runtimePermissionGranted: Boolean,
    notificationsReady: Boolean,
): Boolean = !notificationsReady && (sdkInt < 33 || runtimePermissionGranted)
