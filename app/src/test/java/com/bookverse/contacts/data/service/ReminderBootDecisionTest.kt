package com.bookverse.contacts.data.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderBootDecisionTest {
    @Test
    fun shouldRescheduleFutureReminderAfterBoot() {
        assertTrue(
            shouldRescheduleReminderAfterBoot(
                isReminder = true,
                isDeleted = false,
                reminderDate = 2_000L,
                currentTime = 1_000L,
            )
        )
    }

    @Test
    fun shouldSkipExpiredReminderAfterBoot() {
        assertFalse(
            shouldRescheduleReminderAfterBoot(
                isReminder = true,
                isDeleted = false,
                reminderDate = 1_000L,
                currentTime = 1_000L,
            )
        )
    }

    @Test
    fun shouldSkipDeletedReminderAfterBoot() {
        assertFalse(
            shouldRescheduleReminderAfterBoot(
                isReminder = true,
                isDeleted = true,
                reminderDate = 2_000L,
                currentTime = 1_000L,
            )
        )
    }

    @Test
    fun shouldSkipReminderWithoutScheduleAfterBoot() {
        assertFalse(
            shouldRescheduleReminderAfterBoot(
                isReminder = true,
                isDeleted = false,
                reminderDate = null,
                currentTime = 1_000L,
            )
        )
    }

    @Test
    fun shouldRescheduleRepeatingReminderWhoseFirstOccurrencePassed() {
        assertTrue(
            shouldRescheduleReminderAfterBoot(
                isReminder = true,
                isDeleted = false,
                reminderDate = 1_000L,
                currentTime = 5_000L,
                repeats = true,
            )
        )
    }

    @Test
    fun shouldSkipDeletedRepeatingReminderAfterBoot() {
        assertFalse(
            shouldRescheduleReminderAfterBoot(
                isReminder = true,
                isDeleted = true,
                reminderDate = 1_000L,
                currentTime = 5_000L,
                repeats = true,
            )
        )
    }
}
