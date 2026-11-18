package com.bookverse.contacts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.ui.theme.ContactsAppTheme
import com.bookverse.contacts.ui.theme.AccentBarStyle
import com.bookverse.contacts.ui.theme.DarkModeStyle
import com.bookverse.contacts.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DialerFloatingActionButtonTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun minimalPreviewUsesNeutralCircleAcrossAllThemesAndAccents() {
        var accent by mutableStateOf(Color(0xFF2196F3))
        var theme by mutableStateOf(ThemeMode.LIGHT)
        var dark by mutableStateOf(DarkModeStyle.AMOLED)
        var surface = Color.Unspecified
        compose.setContent {
            ContactsAppTheme(accentSeed = accent, accentBarStyle = AccentBarStyle.MINIMAL, themeMode = theme, darkModeStyle = dark) {
                surface = MaterialTheme.colorScheme.surface
                Box(Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
                    DialerFloatingActionButton(AccentBarStyle.MINIMAL, {}, Modifier.testTag("dialer"))
                }
            }
        }
        val fab = compose.onNodeWithTag("dialer")
        fab.assertWidthIsEqualTo(56.dp).assertHeightIsEqualTo(56.dp)
        for ((mode, variant) in listOf(ThemeMode.LIGHT to DarkModeStyle.AMOLED) + DarkModeStyle.entries.map { ThemeMode.DARK to it }) {
            compose.runOnIdle { theme = mode; dark = variant; accent = Color(0xFF2196F3) }
            val before = fab.captureToImage().toPixelMap()
            assertEquals(surface, before[before.width / 2, before.height / 5])
            // A circular edge must separate the surface from its surrounding background.
            assertTrue(before[before.width / 2, before.height / 5] != before[0, 0])
            compose.runOnIdle { accent = Color(0xFFB76E56) }
            val after = fab.captureToImage().toPixelMap()
            for (y in 0 until before.height step 3) {
                for (x in 0 until before.width step 3) {
                    assertEquals("Minimal must remain neutral in $mode/$variant", before[x, y], after[x, y])
                }
            }
        }
    }

    @Test
    fun retroKeepsItsAccentAndModernKeepsItsLargerNeutralSurface() {
        var style by mutableStateOf(AccentBarStyle.RETRO)
        var primary = Color.Unspecified
        compose.setContent {
            ContactsAppTheme(accentBarStyle = style, themeMode = ThemeMode.LIGHT) {
                primary = MaterialTheme.colorScheme.primary
                CompositionLocalProvider(LocalBackdropAllowed provides false) {
                    Box(Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
                        DialerFloatingActionButton(style, {}, Modifier.testTag("dialer"))
                    }
                }
            }
        }
        val fab = compose.onNodeWithTag("dialer")
        val retro = fab.captureToImage().toPixelMap()
        assertEquals(primary, retro[retro.width / 2, retro.height / 5])
        compose.runOnIdle { style = AccentBarStyle.MODERN }
        fab.assertWidthIsEqualTo(64.dp).assertHeightIsEqualTo(64.dp)
        val modern = fab.captureToImage().toPixelMap()
        assertTrue(primary != modern[modern.width / 2, modern.height / 5])
    }
}
