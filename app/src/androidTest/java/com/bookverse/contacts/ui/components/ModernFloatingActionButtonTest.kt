package com.bookverse.contacts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.bookverse.contacts.ui.theme.ContactsAppTheme
import com.bookverse.contacts.ui.viewmodel.AccentBarStyle
import com.bookverse.contacts.ui.viewmodel.ThemeMode
import dev.chrisbanes.haze.HazeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

class ModernFloatingActionButtonTest {
    @get:Rule val compose = createComposeRule()
    private var backdropColor by mutableStateOf(Color.Red)
    private var overlayBackdrop: HazeState? = null
    private var iconColor = Color.Unspecified
    private var theme by mutableStateOf(ThemeMode.LIGHT)

    @Test
    fun glassOverlaySamplesTheContentWhileTheIconStaysSharp() {
        showScreen(allowBackdrop = true)
        val before = pixels()
        assertNotNull(overlayBackdrop)
        compose.runOnIdle { backdropColor = Color.Blue }
        val after = pixels()
        assertNotEquals(before[before.width / 2, before.height / 4], after[after.width / 2, after.height / 4])
        assertEquals(iconColor, after[after.width / 2, after.height / 2])
    }

    @Test
    fun protectedOverlayNeverSamplesContentEvenWithAnInheritedBackdrop() {
        showScreen(allowBackdrop = false)
        val before = pixels()
        assertNull(overlayBackdrop)
        compose.runOnIdle { backdropColor = Color.Blue }
        val after = pixels()
        assertEquals(before[before.width / 2, before.height / 4], after[after.width / 2, after.height / 4])
        assertEquals(iconColor, after[after.width / 2, after.height / 2])
    }

    @Test
    fun emptyContentStillSamplesTheScaffoldBackground() {
        showScreen(allowBackdrop = true, paintContent = false)
        val before = pixels()
        compose.runOnIdle { backdropColor = Color.Blue }
        val after = pixels()
        assertNotEquals("Empty space must sample the screen base, not an opaque white fallback",
            before[before.width / 4, before.height / 2], after[after.width / 4, after.height / 2])
    }

    @Test
    fun whiteBackdropKeepsTheGlassBodyAndItsReflectionVisible() {
        backdropColor = Color.White
        showScreen(allowBackdrop = true, paintContent = false)
        val image = pixels()
        val body = image[(image.width * 0.22f).toInt(), image.height / 2].luminance()
        val inset = (image.width * 0.15f).toInt()
        val glint = image[inset, inset].luminance()
        assertTrue("White-on-white glass keeps a translucent neutral body", body in 0.65f..0.92f)
        assertTrue("The reflected rim stays visible above a uniform white backdrop", glint > body + 0.04f)
    }

    @Test
    fun glassUsesDirectionalHighlightsWithoutAnInsetRing() {
        backdropColor = Color.Black
        showScreen(allowBackdrop = false)
        for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            compose.runOnIdle { theme = mode }
            val image = pixels()
            val y = image.height / 2
            val interior = image[(image.width * 0.22f).toInt(), y]
            for (x in (image.width * 0.05f).toInt() until (image.width * 0.22f).toInt()) {
                val pixel = image[x, y]
                val delta = maxOf(abs(interior.red - pixel.red), abs(interior.green - pixel.green), abs(interior.blue - pixel.blue))
                assertTrue("No inset ring in $mode at x=$x", delta <= 1f / 255f + 0.0001f)
            }
        }
        val image = pixels()
        val inset = (image.width * 0.15f).toInt()
        val highlight = image[inset, inset].luminance()
        val faded = image[image.width - inset - 1, inset].luminance()
        assertTrue("The edge glint must fade away on the opposite arc", highlight > faded + 0.025f)
    }

    private fun showScreen(allowBackdrop: Boolean, paintContent: Boolean = true) {
        compose.setContent {
            ContactsAppTheme(themeMode = theme, accentBarStyle = AccentBarStyle.MODERN) {
                iconColor = MaterialTheme.colorScheme.onSurface
                CompositionLocalProvider(
                    LocalAppBackdrop provides remember { HazeState(initialBlurEnabled = true) },
                    LocalBackdropAllowed provides allowBackdrop,
                ) {
                    AppScreenScaffold(
                        containerColor = if (paintContent) MaterialTheme.colorScheme.background else backdropColor,
                        contentOverlay = {
                            overlayBackdrop = LocalAppBackdrop.current
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                ModernFloatingActionButton(Icons.Default.Add, "Create synthetic item", {}, Modifier.testTag("glass-fab"))
                            }
                        },
                    ) {
                        Box(Modifier.fillMaxSize().then(if (paintContent) Modifier.background(backdropColor) else Modifier))
                    }
                }
            }
        }
    }

    private fun pixels() = compose.onNodeWithTag("glass-fab").captureToImage().toPixelMap()
}
