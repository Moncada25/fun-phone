package com.bookverse.contacts.settings

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ListDensityTest {
    @Test
    fun presetsKeepTheirStoredNamesAndOriginalSizes() {
        assertEquals(listOf("COMPACT", "NORMAL", "SPACIOUS"), ListDensity.presets.map(ListDensity::name))
        assertEquals(listOf(6.dp, 12.dp, 16.dp), ListDensity.presets.map(ListDensity::itemPadding))
        assertEquals(listOf(32.dp, 40.dp, 52.dp), ListDensity.presets.map(ListDensity::avatarSize))
    }

    @Test
    fun everyStepSurvivesAStorageRoundTrip() {
        (ListDensity.MIN_SPACING..ListDensity.MAX_SPACING).forEach { spacing ->
            val density = ListDensity.of(spacing)

            assertEquals(spacing, density.spacing)
            assertEquals(density, ListDensity.fromName(density.name))
        }
    }

    @Test
    fun unknownOrOutOfRangeValuesFallBackSafely() {
        assertEquals(ListDensity.NORMAL, ListDensity.fromName(null))
        assertEquals(ListDensity.NORMAL, ListDensity.fromName("unknown"))
        assertEquals(ListDensity.MIN_SPACING, ListDensity.fromName("-3").spacing)
        assertEquals(ListDensity.MAX_SPACING, ListDensity.fromName("99").spacing)
    }

    @Test
    fun avatarsGrowWithSpacing() {
        val sizes = (ListDensity.MIN_SPACING..ListDensity.MAX_SPACING).map { ListDensity.of(it).avatarSize }

        assertEquals(sizes.sorted(), sizes)
        assertEquals(sizes.size, sizes.toSet().size)
    }
}
