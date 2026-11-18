package com.bookverse.contacts.ui.screens.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneStatusRequirementTest {
    @Test
    fun shouldPrioritizeDefaultDialerRole() {
        assertEquals(
            PhoneStatusRequirement.DEFAULT_DIALER,
            resolveNextPhoneStatusRequirement(
                isDefaultDialer = false,
                hasCallNotifications = false,
                hasContactsPermission = false,
                hasCallLogPermission = false,
                hasPhoneStatePermission = false,
                hasCallPhonePermission = false
            )
        )
    }

    @Test
    fun shouldReturnFirstMissingPermissionInActivationOrder() {
        assertEquals(
            PhoneStatusRequirement.CALL_LOG,
            resolveNextPhoneStatusRequirement(
                isDefaultDialer = true,
                hasCallNotifications = true,
                hasContactsPermission = true,
                hasCallLogPermission = false,
                hasPhoneStatePermission = false,
                hasCallPhonePermission = false
            )
        )
    }

    @Test
    fun shouldReturnCallPhoneWhenItIsOnlyMissingRequirement() {
        assertEquals(
            PhoneStatusRequirement.CALL_PHONE,
            resolveNextPhoneStatusRequirement(
                isDefaultDialer = true,
                hasCallNotifications = true,
                hasContactsPermission = true,
                hasCallLogPermission = true,
                hasPhoneStatePermission = true,
                hasCallPhonePermission = false
            )
        )
    }

    @Test
    fun shouldReturnNullWhenPhoneSetupIsComplete() {
        assertNull(
            resolveNextPhoneStatusRequirement(
                isDefaultDialer = true,
                hasCallNotifications = true,
                hasContactsPermission = true,
                hasCallLogPermission = true,
                hasPhoneStatePermission = true,
                hasCallPhonePermission = true
            )
        )
    }

    @Test
    fun shouldReturnEveryMissingRequirementForLimitedModeSummary() {
        assertEquals(
            listOf(
                PhoneStatusRequirement.DEFAULT_DIALER,
                PhoneStatusRequirement.CALL_LOG,
                PhoneStatusRequirement.PHONE_STATE,
            ),
            resolveMissingPhoneStatusRequirements(
                isDefaultDialer = false,
                hasCallNotifications = true,
                hasContactsPermission = true,
                hasCallLogPermission = false,
                hasPhoneStatePermission = false,
                hasCallPhonePermission = true,
            ),
        )
    }

    @Test
    fun shouldRequireCallNotificationsBeforeOptionalContactData() {
        assertEquals(
            PhoneStatusRequirement.NOTIFICATIONS,
            resolveNextPhoneStatusRequirement(
                isDefaultDialer = true,
                hasCallNotifications = false,
                hasContactsPermission = false,
                hasCallLogPermission = true,
                hasPhoneStatePermission = true,
                hasCallPhonePermission = true,
            ),
        )
    }

    @Test
    fun shouldKeepExactAlarmsInsideTheSetupAfterCorePhoneAccess() {
        assertEquals(
            PhoneStatusRequirement.EXACT_ALARMS,
            resolveNextPhoneStatusRequirement(
                isDefaultDialer = true,
                hasCallNotifications = true,
                hasContactsPermission = true,
                hasCallLogPermission = true,
                hasPhoneStatePermission = true,
                hasCallPhonePermission = true,
                hasExactAlarmsPermission = false,
            ),
        )
    }

    @Test
    fun shouldIncludeExactAlarmsInTheMissingRequirementsSummary() {
        assertEquals(
            listOf(PhoneStatusRequirement.EXACT_ALARMS),
            resolveMissingPhoneStatusRequirements(
                isDefaultDialer = true,
                hasCallNotifications = true,
                hasContactsPermission = true,
                hasCallLogPermission = true,
                hasPhoneStatePermission = true,
                hasCallPhonePermission = true,
                hasExactAlarmsPermission = false,
            ),
        )
    }
}
