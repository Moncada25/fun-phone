package com.bookverse.contacts.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.bookverse.contacts.feature.settings.R
import com.bookverse.contacts.settings.FONT_SCALE_MAX_PERCENT
import com.bookverse.contacts.settings.FONT_SCALE_MIN_PERCENT
import com.bookverse.contacts.settings.TransitionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import com.bookverse.contacts.core.ui.R as CoreUiR

class SettingsPreferencePolicyTest {
    @Test
    fun shouldResolveEveryTransitionToADistinctLabel() {
        val labels = TransitionType.entries.map(::transitionTypeLabelRes)

        assertEquals(TransitionType.entries.size, labels.toSet().size)
    }

    @Test
    fun shouldPreserveSafeFallbacksForStoredStringValues() {
        assertEquals(CoreUiR.string.tab_contacts, startupScreenLabelRes("unknown"))
        assertEquals(R.string.call_answer_style_swipe, callAnswerStyleLabelRes("unknown"))
        assertEquals(R.string.vibration_pattern_simple, vibrationPatternLabelRes("unknown"))
        assertNotEquals(startupScreenLabelRes("favorites"), startupScreenLabelRes("contacts"))
    }

    @Test
    fun shouldConvertSupportedFontScalesToWholePercentages() {
        assertEquals(listOf(80, 90, 100, 110, 120), listOf(0.8f, 0.9f, 1f, 1.1f, 1.2f).map(::fontScalePercent))
    }

    @Test
    fun shouldKeepEveryOnePercentFontScaleStepExact() {
        val percents = (FONT_SCALE_MIN_PERCENT..FONT_SCALE_MAX_PERCENT).toList()

        assertEquals(percents, percents.map { percent -> fontScalePercent(percent / 100f) })
    }

    @Test
    fun shouldSearchTitlesDescriptionsAndCurrentValuesFromTheSameModels() {
        val section = SettingsSectionModel(
            key = "appearance",
            title = "Appearance",
            description = "Visual options",
            items = listOf(
                SettingsItemModel(
                    key = "theme",
                    icon = Icons.Default.Settings,
                    title = "Theme",
                    description = "Choose the interface theme",
                    currentValue = "System · AMOLED",
                    onClick = {},
                ),
            ),
        )

        assertEquals("theme", filterSettingsItems(listOf(section), "amoled").single().key)
        assertEquals("theme", filterSettingsItems(listOf(section), "interface").single().key)
        assertEquals("theme", filterSettingsItems(listOf(section), "theme").single().key)
    }
}
