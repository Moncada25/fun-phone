package com.bookverse.contacts.ui.screens.tools

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.data.model.ToolsOrderConfig
import com.bookverse.contacts.data.model.ToolsPreset
import com.bookverse.contacts.testing.ComposeTestContent
import com.bookverse.contacts.testing.ComposeTestHostActivity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference
import com.bookverse.contacts.feature.tools.R as ToolsR

class ToolsPresetSelectorTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestHostActivity>()

    @Test
    fun selectingEverythingEmitsThePreset() {
        val selectedPreset = AtomicReference<ToolsPreset>()
        showSelector(onPresetSelected = selectedPreset::set)

        composeRule
            .onNodeWithText(composeRule.activity.getString(ToolsR.string.tools_preset_everything))
            .performClick()

        assertEquals(ToolsPreset.EVERYTHING, selectedPreset.get())
    }

    @Test
    fun compactWidthAtTwoHundredPercentFontKeepsEveryPresetAvailable() {
        composeRule.runOnUiThread {
            ComposeTestContent.show {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale = 2f),
                ) {
                    MaterialTheme {
                        Box(
                            modifier = androidx.compose.ui.Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                        ) {
                            Box(modifier = androidx.compose.ui.Modifier.width(320.dp)) {
                                ToolsPresetSelector(
                                    toolsOrder = ToolsOrderConfig.default(),
                                    onPresetSelected = {},
                                )
                            }
                        }
                    }
                }
            }
        }

        listOf(
            ToolsR.string.tools_preset_essential_phone,
            ToolsR.string.tools_preset_phone_and_organize,
            ToolsR.string.tools_preset_everything,
        ).forEach { title ->
            composeRule
                .onNodeWithText(composeRule.activity.getString(title))
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun customConfigurationShowsSelectedCustomPreset() {
        val customToolsOrder = ToolsOrderConfig.default()
            .applyPreset(ToolsPreset.EVERYTHING)
            .withToolHidden("notes", true)

        composeRule.runOnUiThread {
            ComposeTestContent.show {
                MaterialTheme {
                    ToolsPresetSelector(
                        toolsOrder = customToolsOrder,
                        onPresetSelected = {},
                    )
                }
            }
        }

        composeRule
            .onNodeWithText(composeRule.activity.getString(ToolsR.string.tools_preset_custom))
            .assertIsDisplayed()
    }

    private fun showSelector(onPresetSelected: (ToolsPreset) -> Unit) {
        composeRule.runOnUiThread {
            ComposeTestContent.show {
                MaterialTheme {
                    ToolsPresetSelector(
                        toolsOrder = ToolsOrderConfig.default(),
                        onPresetSelected = onPresetSelected,
                    )
                }
            }
        }
    }
}
