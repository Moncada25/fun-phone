package com.bookverse.contacts.security

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLockPolicyTest {
    private fun surface(
        enabled: Boolean? = true,
        locked: Boolean = true,
        onCallScreen: Boolean = false,
        callActive: Boolean = false,
        callGraceElapsed: Boolean = false,
    ) = AppLockPolicy.surface(enabled, locked, onCallScreen, callActive, callGraceElapsed)

    @Test
    fun locksWhenEnabledAndLocked() {
        assertEquals(AppLockSurface.LOCK, surface())
        assertEquals(AppLockSurface.NONE, surface(locked = false))
        assertEquals(AppLockSurface.NONE, surface(enabled = false))
    }

    @Test
    fun coversContentUntilTheSettingIsKnown() {
        assertEquals(AppLockSurface.COVER, surface(enabled = null))
        assertEquals(AppLockSurface.COVER, surface(enabled = null, locked = false))
    }

    @Test
    fun callScreensAreNeverCovered() {
        assertEquals(AppLockSurface.NONE, surface(onCallScreen = true))
        assertEquals(AppLockSurface.NONE, surface(enabled = null, onCallScreen = true))
        assertEquals(AppLockSurface.NONE, surface(onCallScreen = true, callActive = true, callGraceElapsed = true))
    }

    @Test
    fun activeCallGetsAGracePeriodToReachItsScreenThenLocksElsewhere() {
        assertEquals(AppLockSurface.NONE, surface(callActive = true))
        assertEquals(AppLockSurface.NONE, surface(enabled = null, callActive = true))
        assertEquals(AppLockSurface.LOCK, surface(callActive = true, callGraceElapsed = true))
        assertEquals(AppLockSurface.NONE, surface(locked = false, callActive = true, callGraceElapsed = true))
    }

    @Test
    fun returningFromAnActivityTheAppLaunchedDoesNotLock() {
        val tracker = AppLockResultTracker(graceMillis = 1_000)
        tracker.onLaunched()
        assertEquals(false, tracker.shouldLockOnStop(nowMillis = 100))
        assertEquals(false, tracker.shouldLockOnResult(nowMillis = 900))
        assertEquals(true, tracker.shouldLockOnStop(nowMillis = 2_000))
    }

    @Test
    fun aLateResultOrAnUnrelatedStopStillLocks() {
        val tracker = AppLockResultTracker(graceMillis = 1_000)
        assertEquals(true, tracker.shouldLockOnStop(nowMillis = 0))
        tracker.onLaunched()
        assertEquals(false, tracker.shouldLockOnStop(nowMillis = 100))
        assertEquals(true, tracker.shouldLockOnResult(nowMillis = 5_000))
        assertEquals(false, tracker.shouldLockOnResult(nowMillis = 5_100))
    }
}
