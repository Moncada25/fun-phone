package com.bookverse.contacts.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class GlassEffectsTest {
    private val eightGigabytePhone = 7_856_328_704L
    private val sixGigabytePhone = 5_720_000_000L
    private val fourGigabytePhone = 3_740_000_000L

    @Test
    fun automaticGivesFullGlassToDeclaredPerformanceClassDevices() {
        assertEquals(GlassLevel.FULL, GlassEffects.AUTO.level(35, 33, fourGigabytePhone))
        assertEquals(GlassLevel.FULL, GlassEffects.AUTO.level(31, 31, fourGigabytePhone))
    }

    @Test
    fun automaticGivesBlurOnlyToMidTierDevicesWithoutPerformanceClass() {
        assertEquals(GlassLevel.BLUR, GlassEffects.AUTO.level(35, 0, eightGigabytePhone))
        assertEquals(GlassLevel.BLUR, GlassEffects.AUTO.level(35, 30, sixGigabytePhone))
    }

    @Test
    fun automaticTurnsGlassOffOnLowerMemoryDevicesAndBeforeAndroid12() {
        assertEquals(GlassLevel.NONE, GlassEffects.AUTO.level(35, 0, fourGigabytePhone))
        assertEquals(GlassLevel.NONE, GlassEffects.AUTO.level(30, 31, eightGigabytePhone))
    }

    @Test
    fun manualOnGivesFullGlassOnlyWithTheAndroid12BlurPipeline() {
        assertEquals(GlassLevel.FULL, GlassEffects.ON.level(31, 0, fourGigabytePhone))
        assertEquals(GlassLevel.NONE, GlassEffects.ON.level(30, 0, eightGigabytePhone))
    }

    @Test
    fun offNeverEnablesGlass() {
        assertEquals(GlassLevel.NONE, GlassEffects.OFF.level(36, 35, eightGigabytePhone))
    }

    @Test
    fun unknownOrMissingStoredValuesFallBackToAutomatic() {
        assertEquals(GlassEffects.AUTO, GlassEffects.fromName(null))
        assertEquals(GlassEffects.AUTO, GlassEffects.fromName("LEGACY"))
        GlassEffects.entries.forEach { assertEquals(it, GlassEffects.fromName(it.name)) }
    }
}
