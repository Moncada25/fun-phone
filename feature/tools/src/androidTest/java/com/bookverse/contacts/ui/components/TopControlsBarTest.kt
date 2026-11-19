package com.bookverse.contacts.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import com.bookverse.contacts.feature.tools.R
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.PathType
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TopControlsBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectingBlurShowsContextualControls() {
        val appSettingsViewModel = AppSettingsViewModel()
        composeRule.setContent {
            var state by remember { mutableStateOf(defaultState()) }
            MaterialTheme {
                TopControlsBar(
                    state = state,
                    onEvent = { event ->
                        if (event is PaintControlsEvent.SelectCategory) {
                            state = state.copy(category = event.category)
                        }
                    },
                    appSettingsViewModel = appSettingsViewModel,
                )
            }
        }

        composeRule.onNodeWithContentDescription(text(R.string.paint_tool_blur)).performClick()

        composeRule.onNodeWithContentDescription(text(R.string.paint_blur_size)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.paint_blur_intensity)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.paint_blur_medium)).assertIsDisplayed()
    }

    @Test
    fun pencilPanelExposesCompactBrushStylePreviews() {
        val appSettingsViewModel = AppSettingsViewModel()
        composeRule.setContent {
            MaterialTheme {
                TopControlsBar(
                    state = defaultState(),
                    onEvent = {},
                    appSettingsViewModel = appSettingsViewModel,
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.brush_style_normal)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(R.string.brush_color)).assertIsDisplayed()
    }

    @Test
    fun contextualControlsCanBeCollapsedAndExpanded() {
        val appSettingsViewModel = AppSettingsViewModel()
        composeRule.setContent {
            MaterialTheme {
                TopControlsBar(
                    state = defaultState(),
                    onEvent = {},
                    appSettingsViewModel = appSettingsViewModel,
                )
            }
        }

        composeRule.onNodeWithContentDescription(text(R.string.paint_collapse_controls)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(text(R.string.brush_color)).assertDoesNotExist()

        composeRule.onNodeWithContentDescription(text(R.string.paint_expand_controls)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.brush_color)).assertIsDisplayed()
    }

    @Test
    fun contextualControlsCanBeCollapsedAndExpandedWithSwipe() {
        val appSettingsViewModel = AppSettingsViewModel()
        composeRule.setContent {
            MaterialTheme {
                TopControlsBar(
                    state = defaultState(),
                    onEvent = {},
                    appSettingsViewModel = appSettingsViewModel,
                )
            }
        }

        composeRule.onNodeWithTag(PAINT_CONTROLS_HEADER_TAG)
            .performTouchInput { swipeDown() }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(text(R.string.brush_color)).assertDoesNotExist()

        composeRule.onNodeWithTag(PAINT_CONTROLS_HEADER_TAG)
            .performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(text(R.string.brush_color)).assertIsDisplayed()
    }

    @Test
    fun selectingTextEmitsCategoryWithoutShowingAnAdditionalAction() {
        val appSettingsViewModel = AppSettingsViewModel()
        var selectedCategory: PaintToolCategory? = null
        composeRule.setContent {
            MaterialTheme {
                TopControlsBar(
                    state = defaultState(),
                    onEvent = { event ->
                        if (event is PaintControlsEvent.SelectCategory) {
                            selectedCategory = event.category
                        }
                    },
                    appSettingsViewModel = appSettingsViewModel,
                )
            }
        }

        composeRule.onNodeWithContentDescription(text(R.string.paint_tool_text)).performClick()

        assertEquals(PaintToolCategory.TEXT, selectedCategory)
        composeRule.onNodeWithText(text(R.string.add_text)).assertDoesNotExist()
    }

    private fun defaultState() = PaintControlsState(
        category = PaintToolCategory.PENCIL,
        selectedColor = Color.Blue,
        brushSize = 10f,
        brushStyle = BrushStyle.NORMAL,
        blurBrushSize = 32f,
        blurIntensity = BlurIntensity.MEDIUM,
        selectedShape = PathType.CIRCLE,
        shapeFilled = false,
        enabled = true,
    )

    private fun text(resource: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resource)
}
