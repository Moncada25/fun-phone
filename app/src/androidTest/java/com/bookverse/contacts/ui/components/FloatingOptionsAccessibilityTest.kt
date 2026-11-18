package com.bookverse.contacts.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.settings.AnimationSpeed
import com.bookverse.contacts.settings.AnimationSpeed.Companion.LocalAnimationSpeed
import com.bookverse.contacts.ui.theme.AccentBarStyle
import com.bookverse.contacts.ui.theme.ThemeMode
import com.bookverse.contacts.ui.theme.ContactsAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FloatingOptionsAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun overlayDispatchesCreationAndReleasesContentAfterCollapsing() {
        var created = 0
        var contentClicks = 0
        compose.setContent {
            ContactsAppTheme(themeMode = ThemeMode.LIGHT, accentBarStyle = AccentBarStyle.MODERN) {
                CompositionLocalProvider(LocalAnimationSpeed provides AnimationSpeed.OFF, LocalBackdropAllowed provides false) {
                    AppScreenScaffold(
                        contentOverlay = {
                            FloatingOptions(
                                state = rememberFloatingOptionsState(),
                                actions = listOf(FloatingOptionItem("New synthetic note", Icons.Default.Add) { created++ }),
                                accentColor = MaterialTheme.colorScheme.primary,
                                accentBarStyle = AccentBarStyle.MODERN,
                                buttonDescription = "Create synthetic note",
                            )
                        },
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Button(onClick = { contentClicks++ }) { Text("Content action") }
                        }
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Create synthetic note").performClick()
        compose.onNodeWithText("New synthetic note").performClick()
        compose.onNodeWithText("New synthetic note").assertDoesNotExist()
        compose.onNodeWithText("Content action").performClick()
        compose.runOnIdle { assertEquals(1, created); assertEquals(1, contentClicks) }
    }

    @Test
    fun labelledCreateButtonExpandsAndCancelsWithMotionDisabled() {
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalAnimationSpeed provides AnimationSpeed.OFF) {
                    FloatingOptions(
                        state = rememberFloatingOptionsState(),
                        actions = listOf(FloatingOptionItem("New sample note", Icons.Default.Add, onClick = {})),
                        accentColor = MaterialTheme.colorScheme.primary,
                        accentBarStyle = AccentBarStyle.MODERN,
                        buttonDescription = "Create sample note",
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("Create sample note").performClick()
        compose.onNodeWithText("New sample note").assertIsDisplayed()
        compose.onNodeWithContentDescription(InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.cancel)).performClick()
        compose.onNodeWithText("New sample note").assertDoesNotExist()
    }
}
