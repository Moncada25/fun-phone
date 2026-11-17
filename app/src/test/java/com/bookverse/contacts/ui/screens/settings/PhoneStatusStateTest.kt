package com.bookverse.contacts.ui.screens.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneStatusStateTest {
    @Test
    fun readyStateHasNoNextRequirement() {
        val state = phoneStatus()

        assertTrue(state.allRequirementsGranted)
        assertNull(state.nextRequirement)
    }

    @Test
    fun nextRequirementUsesTheCoreSetupPriority() {
        val state = phoneStatus(
            isDefaultDialer = true,
            hasContactsPermission = true,
            hasCallLogPermission = false,
            hasPhoneStatePermission = false,
        )

        assertFalse(state.allRequirementsGranted)
        assertEquals(PhoneStatusRequirement.CALL_LOG, state.nextRequirement)
    }

    @Test
    fun exactAlarmsDoNotPutCorePhoneSetupIntoLimitedMode() {
        val state = phoneStatus(hasExactAlarmsPermission = false)

        assertTrue(state.coreRequirementsGranted)
        assertFalse(state.allRequirementsGranted)
        assertEquals(PhoneStatusRequirement.EXACT_ALARMS, state.nextRequirement)
    }

    @Test
    fun controllerRefreshReplacesTheWholeSnapshot() {
        val controller = PhoneSetupController(phoneStatus(isDefaultDialer = false))

        controller.refresh(phoneStatus(hasCallPhonePermission = false))

        assertTrue(controller.uiState.phoneStatus.isDefaultDialer)
        assertFalse(controller.uiState.phoneStatus.hasCallPhonePermission)
        assertEquals(PhoneStatusRequirement.CALL_PHONE, controller.uiState.phoneStatus.nextRequirement)
    }

    private fun phoneStatus(
        isDefaultDialer: Boolean = true,
        hasContactsPermission: Boolean = true,
        hasCallLogPermission: Boolean = true,
        hasPhoneStatePermission: Boolean = true,
        hasCallPhonePermission: Boolean = true,
        hasCallNotifications: Boolean = true,
        hasExactAlarmsPermission: Boolean = true,
    ) = PhoneStatusUiState(
        isDefaultDialer = isDefaultDialer,
        hasCallNotifications = hasCallNotifications,
        hasContactsPermission = hasContactsPermission,
        hasCallLogPermission = hasCallLogPermission,
        hasPhoneStatePermission = hasPhoneStatePermission,
        hasCallPhonePermission = hasCallPhonePermission,
        hasExactAlarmsPermission = hasExactAlarmsPermission,
    )
}
