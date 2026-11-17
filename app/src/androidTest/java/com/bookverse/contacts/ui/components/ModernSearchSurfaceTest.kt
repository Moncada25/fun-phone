package com.bookverse.contacts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.ui.theme.ContactsAppTheme
import com.bookverse.contacts.ui.viewmodel.AccentBarStyle
import com.bookverse.contacts.ui.viewmodel.ThemeMode
import dev.chrisbanes.haze.HazeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

class ModernSearchSurfaceTest {
    @get:Rule val compose = createComposeRule()
    private var backdropColor by mutableStateOf(Color.Red)
    private var active by mutableStateOf(false)
    private var query by mutableStateOf("")
    private var textColor = Color.Unspecified

    @Test
    fun idleAndEditingShareGlassWhileInputAndClearingKeepWorking() {
        showSearch(allowBackdrop = true)
        val red = pixels().surfaceColor()
        compose.runOnIdle { backdropColor = Color.Blue }
        val blue = pixels().surfaceColor()
        assertNotEquals(red, blue)

        compose.onNodeWithText("Search synthetic items").performClick()
        compose.onNode(hasSetTextAction()).assertIsFocused().performTextInput("A")
        compose.runOnIdle { assertEquals("A", query) }
        val editing = pixels()
        assertEquals(blue, editing.surfaceColor())
        assertTrue("Input stays sharp above the optical layer", editing.containsForegroundColor(textColor))
        compose.runOnIdle { backdropColor = Color.Red }
        assertEquals(red, pixels().surfaceColor())

        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.runOnIdle { assertEquals("", query); assertTrue(active) }
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.runOnIdle { assertTrue(!active) }
    }

    @Test
    fun protectedSearchKeepsItsFinishWithoutSamplingContent() {
        showSearch(allowBackdrop = false)
        val before = pixels().surfaceColor()
        compose.runOnIdle { backdropColor = Color.Blue }
        assertEquals(before, pixels().surfaceColor())
        compose.onNodeWithText("Search synthetic items").performClick()
        compose.onNode(hasSetTextAction()).assertIsFocused().performTextInput("A")
        assertEquals(before, pixels().surfaceColor())
    }

    @Test
    fun elongatedGlassHighlightsOpposingCornersAndFadesBetweenThem() {
        backdropColor = Color.Black
        showSearch(allowBackdrop = false, theme = ThemeMode.DARK)
        val image = pixels()
        val left = (image.width * 0.2f).toInt().coerceAtLeast(image.height / 2)
        val right = image.width - left - 1
        assertTrue("The top highlight fades before the right corner",
            image[left, 1].luminance() > image[right, 1].luminance() + 0.02f)
        assertTrue("The lower highlight fades before the left corner",
            image[right, image.height - 2].luminance() > image[left, image.height - 2].luminance() + 0.02f)
    }

    private fun showSearch(allowBackdrop: Boolean, theme: ThemeMode = ThemeMode.SYSTEM) {
        compose.setContent {
            ContactsAppTheme(themeMode = theme, accentBarStyle = AccentBarStyle.MODERN) {
                textColor = MaterialTheme.colorScheme.onSurface
                CompositionLocalProvider(
                    LocalAppBackdrop provides remember { HazeState(initialBlurEnabled = true) },
                    LocalBackdropAllowed provides allowBackdrop,
                ) {
                    val focus = remember { FocusRequester() }
                    LaunchedEffect(active) { if (active) focus.requestFocus() }
                    AppScreenScaffold(
                        contentOverlay = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Box(Modifier.fillMaxWidth().padding(16.dp).testTag("search-surface")) {
                                    if (active) {
                                        TopBarSearchField(
                                            value = query, onValueChange = { query = it }, placeholder = "Search synthetic items",
                                            focusRequester = focus, onClose = { active = false }, clearContentDescription = "Clear search",
                                        )
                                    } else {
                                        AppBarSearchEntry(AppBarSearchAction("Search synthetic items", { active = true }))
                                    }
                                }
                            }
                        },
                    ) { Box(Modifier.fillMaxSize().background(backdropColor)) }
                }
            }
        }
    }

    private fun pixels() = compose.onNodeWithTag("search-surface").captureToImage().toPixelMap()
    private fun PixelMap.surfaceColor() = this[width / 2, height - 4]
    private fun PixelMap.containsForegroundColor(color: Color) = (0 until height).any { y ->
        // AMOLED text has alpha; compare its final color over the same row's empty glass.
        val expected = color.compositeOver(this[width * 3 / 4, y])
        (0 until width / 2).any { x ->
            val pixel = this[x, y]
            maxOf(abs(pixel.red - expected.red), abs(pixel.green - expected.green), abs(pixel.blue - expected.blue)) <= 1f / 255f + 0.0001f
        }
    }
}
