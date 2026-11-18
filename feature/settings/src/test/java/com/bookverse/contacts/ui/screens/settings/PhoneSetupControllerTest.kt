package com.bookverse.contacts.ui.screens.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneSetupControllerTest {
    @Test
    fun roleIsRequestedBeforeHistoryAndDuplicateRequestsAreRejected() {
        val controller = PhoneSetupController(status(defaultDialer = false, history = false))
        assertFalse(controller.beginRequest(PhoneStatusRequirement.CALL_LOG))
        assertTrue(controller.beginRequest(PhoneStatusRequirement.DEFAULT_DIALER))
        assertFalse(controller.beginRequest(PhoneStatusRequirement.DEFAULT_DIALER))
    }

    @Test
    fun alreadyGrantedRoleDoesNotStartAnotherRequest() {
        val controller = PhoneSetupController(status(history = false))
        assertFalse(controller.beginRequest(PhoneStatusRequirement.DEFAULT_DIALER))
        assertEquals(PhoneStatusRequirement.CALL_LOG, controller.uiState.phoneStatus.nextRequirement)
    }

    @Test
    fun deniedRoleLeavesSetupRecoverableWithoutAdvancing() {
        val controller = PhoneSetupController(status(defaultDialer = false))
        controller.beginRequest(PhoneStatusRequirement.DEFAULT_DIALER)
        controller.finishRequest(PhoneStatusRequirement.DEFAULT_DIALER, SetupOutcome.DENIED, status(defaultDialer = false))
        assertNull(controller.uiState.requesting)
        assertEquals(SetupOutcome.DENIED, controller.uiState.outcome(PhoneStatusRequirement.DEFAULT_DIALER))
        assertTrue(controller.beginRequest(PhoneStatusRequirement.DEFAULT_DIALER))
    }

    @Test
    fun permissionResultDistinguishesRetryFromSystemSettingsRecovery() {
        assertEquals(SetupOutcome.GRANTED, permissionResultOutcome(granted = true, canRequestAgain = false))
        assertEquals(SetupOutcome.DENIED, permissionResultOutcome(granted = false, canRequestAgain = true))
        assertEquals(SetupOutcome.BLOCKED, permissionResultOutcome(granted = false, canRequestAgain = false))
        assertEquals(SetupOutcome.PENDING, PhoneSetupController(status(history = false)).uiState.outcome(PhoneStatusRequirement.CALL_LOG))
    }

    @Test
    fun grantedResultRefreshesAllRequirementsWithoutLaunchingTheNextOne() {
        val controller = PhoneSetupController(status(defaultDialer = false, history = false))
        controller.beginRequest(PhoneStatusRequirement.DEFAULT_DIALER)
        controller.finishRequest(PhoneStatusRequirement.DEFAULT_DIALER, SetupOutcome.GRANTED, status(history = false))
        assertNull(controller.uiState.requesting)
        assertEquals(SetupOutcome.GRANTED, controller.uiState.outcome(PhoneStatusRequirement.DEFAULT_DIALER))
        assertEquals(PhoneStatusRequirement.CALL_LOG, controller.uiState.phoneStatus.nextRequirement)
    }

    @Test
    fun returningFromSettingsClearsBlockedOutcomeAndStaleRequest() {
        val controller = PhoneSetupController(status(history = false))
        controller.finishRequest(PhoneStatusRequirement.CALL_LOG, SetupOutcome.BLOCKED, status(history = false))
        controller.beginRequest(PhoneStatusRequirement.CALL_LOG)
        controller.onResume(status())
        assertTrue(controller.uiState.phoneStatus.allRequirementsGranted)
        assertEquals(SetupOutcome.GRANTED, controller.uiState.outcome(PhoneStatusRequirement.CALL_LOG))
        assertNull(controller.uiState.requesting)
    }

    @Test
    fun restorePreservesDenialButUsesLivePermissionSnapshot() {
        val original = PhoneSetupController(status(history = false))
        original.finishRequest(PhoneStatusRequirement.CALL_LOG, SetupOutcome.BLOCKED, status(history = false))
        val restored = PhoneSetupController(status(history = false)).apply { restoreOutcomes(original.saveOutcomes()) }
        assertEquals(SetupOutcome.BLOCKED, restored.uiState.outcome(PhoneStatusRequirement.CALL_LOG))
        assertNull(restored.uiState.requesting)
        val alreadyGranted = PhoneSetupController(status()).apply { restoreOutcomes(original.saveOutcomes()) }
        assertEquals(SetupOutcome.GRANTED, alreadyGranted.uiState.outcome(PhoneStatusRequirement.CALL_LOG))
    }

    @Test
    fun grantedRuntimeNotificationsDoNotOverrideBlockedCallChannels() {
        val controller = PhoneSetupController(status(notifications = false))
        controller.finishRequest(PhoneStatusRequirement.NOTIFICATIONS, SetupOutcome.GRANTED, status(notifications = false))
        assertFalse(controller.uiState.phoneStatus.allRequirementsGranted)
        assertEquals(SetupOutcome.PENDING, controller.uiState.outcome(PhoneStatusRequirement.NOTIFICATIONS))
    }

    @Test
    fun contactsAloneNeverCompletePhoneSetup() {
        assertFalse(status(defaultDialer = false).allRequirementsGranted)
        assertFalse(status(notifications = false).allRequirementsGranted)
        assertFalse(status(history = false).allRequirementsGranted)
        assertFalse(status(phoneState = false).allRequirementsGranted)
        assertFalse(status(calls = false).allRequirementsGranted)
        assertFalse(status(contacts = false).allRequirementsGranted)
    }

    @Test
    fun unavailableActionCanBeRetriedWithoutLockingTheFlow() {
        val controller = PhoneSetupController(status(defaultDialer = false))
        controller.finishRequest(PhoneStatusRequirement.DEFAULT_DIALER, SetupOutcome.UNAVAILABLE, status(defaultDialer = false))
        assertEquals(SetupOutcome.UNAVAILABLE, controller.uiState.outcome(PhoneStatusRequirement.DEFAULT_DIALER))
        assertTrue(controller.beginRequest(PhoneStatusRequirement.DEFAULT_DIALER))
    }

    @Test
    fun notificationRecoveryRespectsAndroidVersionAndBlockedChannels() {
        assertTrue(requiresNotificationSettings(28, false, false))
        assertTrue(requiresNotificationSettings(32, true, false))
        assertFalse(requiresNotificationSettings(33, false, false))
        assertTrue(requiresNotificationSettings(35, true, false))
        assertFalse(requiresNotificationSettings(37, true, true))
    }

    @Test
    fun exactAlarmSettingsReturnKeepsTheRequirementRecoverable() {
        val controller = PhoneSetupController(status(exactAlarms = false))

        assertTrue(controller.beginRequest(PhoneStatusRequirement.EXACT_ALARMS))
        controller.onResume(status(exactAlarms = false))

        assertEquals(SetupOutcome.DENIED, controller.uiState.outcome(PhoneStatusRequirement.EXACT_ALARMS))
        assertTrue(controller.beginRequest(PhoneStatusRequirement.EXACT_ALARMS))

        controller.onResume(status(exactAlarms = true))
        assertTrue(controller.uiState.phoneStatus.allRequirementsGranted)
        assertEquals(SetupOutcome.GRANTED, controller.uiState.outcome(PhoneStatusRequirement.EXACT_ALARMS))
    }

    private fun status(
        defaultDialer: Boolean = true,
        notifications: Boolean = true,
        contacts: Boolean = true,
        history: Boolean = true,
        phoneState: Boolean = true,
        calls: Boolean = true,
        exactAlarms: Boolean = true,
    ) = PhoneStatusUiState(
        isDefaultDialer = defaultDialer,
        hasCallNotifications = notifications,
        hasContactsPermission = contacts,
        hasCallLogPermission = history,
        hasPhoneStatePermission = phoneState,
        hasCallPhonePermission = calls,
        hasExactAlarmsPermission = exactAlarms,
    )
}
