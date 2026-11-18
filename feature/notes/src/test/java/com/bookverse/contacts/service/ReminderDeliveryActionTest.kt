package com.bookverse.contacts.service

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderDeliveryActionTest {
    @Test
    fun shouldCancelPeriodicReminder_whenExpirationReached() {
        assertEquals(
            ReminderDeliveryAction.CANCEL,
            resolveReminderDeliveryAction(
                notificationType = "periodic",
                currentTime = 1_000L,
                expirationDate = 1_000L,
            ),
        )
    }

    @Test
    fun shouldShowPeriodicReminder_whenExpirationIsInFuture() {
        assertEquals(
            ReminderDeliveryAction.SHOW_PERIODIC,
            resolveReminderDeliveryAction(
                notificationType = "periodic",
                currentTime = 999L,
                expirationDate = 1_000L,
            ),
        )
    }

    @Test
    fun shouldShowAlarmReminder_whenDeliveryIsLate() {
        assertEquals(
            ReminderDeliveryAction.SHOW_ALARM,
            resolveReminderDeliveryAction(
                notificationType = "alarm",
                currentTime = 1_001L,
                expirationDate = 1_000L,
            ),
        )
    }

    @Test
    fun shouldShowAdvanceReminder_whenEventIsInFuture() {
        assertEquals(
            ReminderDeliveryAction.SHOW_ADVANCE,
            resolveReminderDeliveryAction(
                notificationType = "advance",
                currentTime = 999L,
                expirationDate = 1_000L,
            ),
        )
    }

    @Test
    fun shouldCancelAdvanceReminder_whenEventHasExpired() {
        assertEquals(
            ReminderDeliveryAction.CANCEL,
            resolveReminderDeliveryAction(
                notificationType = "advance",
                currentTime = 1_000L,
                expirationDate = 1_000L,
            ),
        )
    }

    @Test
    fun shouldIgnoreReminder_whenNotificationTypeIsUnknown() {
        assertEquals(
            ReminderDeliveryAction.IGNORE,
            resolveReminderDeliveryAction(
                notificationType = "unknown",
                currentTime = 0L,
                expirationDate = 1_000L,
            ),
        )
    }

    @Test
    fun shouldRejectDeletedStoredReminder() {
        assertEquals(
            false,
            shouldDeliverStoredReminder(
                noteExists = true,
                isReminder = true,
                isDeleted = true,
                hasReminderData = true,
            ),
        )
    }

    @Test
    fun shouldRejectStoredReminderWithoutReminderData() {
        assertEquals(
            false,
            shouldDeliverStoredReminder(
                noteExists = true,
                isReminder = true,
                isDeleted = false,
                hasReminderData = false,
            ),
        )
    }

    @Test
    fun shouldAcceptActiveStoredReminder() {
        assertEquals(
            true,
            shouldDeliverStoredReminder(
                noteExists = true,
                isReminder = true,
                isDeleted = false,
                hasReminderData = true,
            ),
        )
    }

    @Test
    fun shouldResolveNextPeriodicNotificationBeforeExpiration() {
        assertEquals(
            61_000L,
            resolveNextPeriodicNotificationTime(
                currentTime = 1_000L,
                intervalSeconds = 60,
                expirationDate = 61_000L,
            ),
        )
    }

    @Test
    fun shouldRejectInvalidPeriodicInterval() {
        assertEquals(
            null,
            resolveNextPeriodicNotificationTime(
                currentTime = 1_000L,
                intervalSeconds = 0,
                expirationDate = 61_000L,
            ),
        )
    }

    @Test
    fun shouldStopPeriodicSchedulePastExpiration() {
        assertEquals(
            null,
            resolveNextPeriodicNotificationTime(
                currentTime = 1_000L,
                intervalSeconds = 61,
                expirationDate = 61_000L,
            ),
        )
    }

    @Test
    fun shouldKeepPeriodicCadenceAnchoredWhenDeliveryIsLate() {
        assertEquals(
            181_000L,
            resolveNextPeriodicNotificationTime(
                currentTime = 125_000L,
                intervalSeconds = 60,
                expirationDate = 300_000L,
                anchorTime = 1_000L,
            ),
        )
    }

    @Test
    fun shouldRejectPeriodicScheduleWhenAnchoredResultOverflows() {
        assertEquals(
            null,
            resolveNextPeriodicNotificationTime(
                currentTime = Long.MAX_VALUE - 2_000L,
                intervalSeconds = Int.MAX_VALUE,
                expirationDate = Long.MAX_VALUE,
                anchorTime = Long.MIN_VALUE + 1_000L,
            ),
        )
    }

    @Test
    fun shouldReportExactAlarmUnavailableWhenPermissionIsDenied() {
        assertEquals(
            AlarmSchedulingMode.UNAVAILABLE,
            resolveAlarmSchedulingMode(
                requiresExactAlarmPermission = true,
                canScheduleExactAlarms = false,
            ),
        )
    }

    @Test
    fun shouldUseExactAlarmWhenPermissionIsAvailable() {
        assertEquals(
            AlarmSchedulingMode.EXACT,
            resolveAlarmSchedulingMode(
                requiresExactAlarmPermission = true,
                canScheduleExactAlarms = true,
            ),
        )
    }

    @Test
    fun shouldUseExactAlarmOnOlderAndroidWithoutRuntimeGate() {
        assertEquals(
            AlarmSchedulingMode.EXACT,
            resolveAlarmSchedulingMode(
                requiresExactAlarmPermission = false,
                canScheduleExactAlarms = false,
            ),
        )
    }

    @Test
    fun shouldAllowPostingWhenChannelIsSilent() {
        assertEquals(
            ReminderChannelAvailability(
                canPost = true,
                isAudible = false,
            ),
            resolveReminderChannelAvailability(
                appNotificationsEnabled = true,
                notificationPermissionGranted = true,
                channelExists = true,
                channelEnabled = true,
                channelHasSound = false,
            ),
        )
    }

    @Test
    fun shouldRejectPostingWhenNotificationPermissionIsDenied() {
        assertEquals(
            ReminderChannelAvailability(
                canPost = false,
                isAudible = false,
            ),
            resolveReminderChannelAvailability(
                appNotificationsEnabled = true,
                notificationPermissionGranted = false,
                channelExists = true,
                channelEnabled = true,
                channelHasSound = true,
            ),
        )
    }

    @Test
    fun shouldScheduleAdvanceBeforeTheOccurrence() {
        assertEquals(
            400_000L,
            resolveAdvanceNotificationTime(
                occurrence = 1_000_000L,
                advanceMinutes = 10,
                currentTime = 100_000L,
            ),
        )
    }

    @Test
    fun shouldSkipAdvanceWhenItIsDisabledOrAlreadyPast() {
        assertEquals(
            null,
            resolveAdvanceNotificationTime(
                occurrence = 1_000_000L,
                advanceMinutes = 0,
                currentTime = 100_000L,
            ),
        )
        assertEquals(
            null,
            resolveAdvanceNotificationTime(
                occurrence = 1_000_000L,
                advanceMinutes = 10,
                currentTime = 400_000L,
            ),
        )
    }
}
