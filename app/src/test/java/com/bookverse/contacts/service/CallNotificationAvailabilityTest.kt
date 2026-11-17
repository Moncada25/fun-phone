package com.bookverse.contacts.service

import android.app.NotificationManager
import com.bookverse.contacts.domain.call.CallState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallNotificationAvailabilityTest {
    @Test
    fun shouldBeReady_whenPermissionAppAndBothCallChannelsAreEnabled() {
        val availability = availability()

        assertTrue(availability.canShowIncoming)
        assertTrue(availability.canShowOngoing)
        assertTrue(availability.isReady)
    }

    @Test
    fun shouldNotExposeCallSurfaces_whenNotificationPermissionIsDenied() {
        val availability = availability(permissionGranted = false)

        assertFalse(availability.canShowIncoming)
        assertFalse(availability.canShowOngoing)
        assertFalse(availability.isReady)
    }

    @Test
    fun shouldNotBeReady_whenEitherCallChannelIsBlocked() {
        val incomingBlocked = availability(incomingImportance = NotificationManager.IMPORTANCE_NONE)
        val ongoingBlocked = availability(ongoingImportance = NotificationManager.IMPORTANCE_NONE)

        assertFalse(incomingBlocked.canShowIncoming)
        assertTrue(incomingBlocked.canShowOngoing)
        assertTrue(ongoingBlocked.canShowIncoming)
        assertFalse(ongoingBlocked.canShowOngoing)
    }

    @Test
    fun shouldTreatMissingPreCreationChannelsAsAvailable() {
        val availability = availability(incomingImportance = null, ongoingImportance = null)

        assertTrue(availability.isReady)
    }

    @Test
    fun shouldKeepRingingAndOngoingCallsInsideForegroundApp() {
        val availability = availability()

        assertEquals(
            CallSurface.IN_APP_INCOMING,
            resolveCallSurface(
                CallState.RINGING,
                appInForeground = true,
                notificationAvailability = availability,
            ),
        )
        listOf(CallState.DIALING, CallState.ACTIVE, CallState.ON_HOLD).forEach { state ->
            assertEquals(
                CallSurface.IN_APP_ONGOING,
                resolveCallSurface(
                    state,
                    appInForeground = true,
                    notificationAvailability = availability,
                ),
            )
        }
    }

    @Test
    fun shouldUseCallNotificationsForEveryBackgroundCallState() {
        val availability = availability()

        assertEquals(
            CallSurface.INCOMING_NOTIFICATION,
            resolveCallSurface(
                CallState.RINGING,
                appInForeground = false,
                notificationAvailability = availability,
            ),
        )
        listOf(CallState.DIALING, CallState.ACTIVE, CallState.ON_HOLD).forEach { state ->
            assertEquals(
                CallSurface.ONGOING_NOTIFICATION,
                resolveCallSurface(
                    state,
                    appInForeground = false,
                    notificationAvailability = availability,
                ),
            )
        }
    }

    @Test
    fun shouldExposeNoBackgroundSurface_whenCallNotificationsAreUnavailable() {
        val unavailable = availability(permissionGranted = false)

        CallState.entries.forEach { state ->
            assertEquals(
                CallSurface.NONE,
                resolveCallSurface(
                    state,
                    appInForeground = false,
                    notificationAvailability = unavailable,
                ),
            )
        }
    }

    private fun availability(
        permissionGranted: Boolean = true,
        appNotificationsEnabled: Boolean = true,
        incomingImportance: Int? = NotificationManager.IMPORTANCE_HIGH,
        ongoingImportance: Int? = NotificationManager.IMPORTANCE_HIGH,
    ) = resolveCallNotificationAvailability(
        permissionGranted = permissionGranted,
        appNotificationsEnabled = appNotificationsEnabled,
        incomingChannelImportance = incomingImportance,
        ongoingChannelImportance = ongoingImportance,
    )
}
