package com.bookverse.contacts.ui.screens.settings

import androidx.compose.runtime.Immutable

@Immutable
data class PhoneStatusUiState(
    val isDefaultDialer: Boolean,
    val hasCallNotifications: Boolean,
    val hasContactsPermission: Boolean,
    val hasCallLogPermission: Boolean,
    val hasPhoneStatePermission: Boolean,
    val hasCallPhonePermission: Boolean,
    val notificationsRequireSettings: Boolean = false,
    val hasExactAlarmsPermission: Boolean = true,
    val hasCallScreening: Boolean = true,
    val canRequestCallScreening: Boolean = false,
) {
    fun isGranted(requirement: PhoneStatusRequirement): Boolean = when (requirement) {
        PhoneStatusRequirement.DEFAULT_DIALER -> isDefaultDialer
        PhoneStatusRequirement.NOTIFICATIONS -> hasCallNotifications
        PhoneStatusRequirement.CONTACTS -> hasContactsPermission
        PhoneStatusRequirement.CALL_LOG -> hasCallLogPermission
        PhoneStatusRequirement.PHONE_STATE -> hasPhoneStatePermission
        PhoneStatusRequirement.CALL_PHONE -> hasCallPhonePermission
        PhoneStatusRequirement.EXACT_ALARMS -> hasExactAlarmsPermission
    }

    val coreRequirementsGranted: Boolean
        get() = isDefaultDialer &&
            hasCallNotifications &&
            hasContactsPermission &&
            hasCallLogPermission &&
            hasPhoneStatePermission &&
            hasCallPhonePermission

    val allRequirementsGranted: Boolean
        get() = coreRequirementsGranted && hasExactAlarmsPermission

    val nextRequirement: PhoneStatusRequirement?
        get() = resolveNextPhoneStatusRequirement(
            isDefaultDialer = isDefaultDialer,
            hasCallNotifications = hasCallNotifications,
            hasContactsPermission = hasContactsPermission,
            hasCallLogPermission = hasCallLogPermission,
            hasPhoneStatePermission = hasPhoneStatePermission,
            hasCallPhonePermission = hasCallPhonePermission,
            hasExactAlarmsPermission = hasExactAlarmsPermission,
        )
}
