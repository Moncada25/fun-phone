package com.bookverse.contacts.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimationSpeedTest {
    @Test
    fun presetsKeepTheirStoredNamesAndDurations() {
        assertEquals(listOf("OFF", "FAST", "NORMAL", "SLOW"), AnimationSpeed.presets.map(AnimationSpeed::name))
        assertEquals(listOf(0, 250, 500, 750), AnimationSpeed.presets.map(AnimationSpeed::durationMs))
    }

    @Test
    fun legacyStoredNamesResolveToTheSamePresets() {
        assertEquals(AnimationSpeed.OFF, AnimationSpeed.fromName("OFF"))
        assertEquals(AnimationSpeed.FAST, AnimationSpeed.fromName("FAST"))
        assertEquals(AnimationSpeed.NORMAL, AnimationSpeed.fromName("NORMAL"))
        assertEquals(AnimationSpeed.SLOW, AnimationSpeed.fromName("SLOW"))
    }

    @Test
    fun everyStepSurvivesAStorageRoundTrip() {
        (AnimationSpeed.MIN_STEP..AnimationSpeed.MAX_STEP).forEach { step ->
            val speed = AnimationSpeed.ofStep(step)

            assertEquals(step * AnimationSpeed.STEP_MS, speed.durationMs)
            assertEquals(speed, AnimationSpeed.fromName(speed.name))
        }
    }

    @Test
    fun rangeReachesBeyondTheOldSlowestPreset() {
        assertTrue(AnimationSpeed.MAX_DURATION_MS > AnimationSpeed.SLOW.durationMs)
        assertEquals(AnimationSpeed.MAX_DURATION_MS, AnimationSpeed.of(Int.MAX_VALUE).durationMs)
    }

    @Test
    fun unknownOrOffGridValuesFallBackSafely() {
        assertEquals(AnimationSpeed.NORMAL, AnimationSpeed.fromName(null))
        assertEquals(AnimationSpeed.NORMAL, AnimationSpeed.fromName("unknown"))
        assertEquals(AnimationSpeed.OFF, AnimationSpeed.fromName("-40"))
        assertEquals(400, AnimationSpeed.fromName("390").durationMs)
    }
}
