package com.bookverse.contacts.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AnimatedColorSchemeTest {
    @get:Rule val compose = createComposeRule()

    private val light = lightColorScheme(background = Color.White)
    private val dark = darkColorScheme(background = Color.Black)

    @Test
    fun themeChangeCrossfadesInsteadOfJumping() {
        var isDark by mutableStateOf(false)
        var background = Color.Unspecified
        compose.mainClock.autoAdvance = false
        compose.setContent {
            background = rememberAnimatedColorScheme(if (isDark) dark else light, isDark, 400).background
        }
        compose.mainClock.advanceTimeBy(32)
        assertEquals(Color.White, background)

        isDark = true
        compose.mainClock.advanceTimeBy(200)
        assertTrue("mid-transition was $background", background.red in 0.05f..0.95f)

        compose.mainClock.advanceTimeBy(600)
        assertEquals(Color.Black, background)
    }

    @Test
    fun animationsOffSwitchesImmediately() {
        var isDark by mutableStateOf(false)
        var background = Color.Unspecified
        compose.mainClock.autoAdvance = false
        compose.setContent {
            background = rememberAnimatedColorScheme(if (isDark) dark else light, isDark, 0).background
        }
        compose.mainClock.advanceTimeBy(32)
        isDark = true
        compose.mainClock.advanceTimeBy(48)
        assertEquals(Color.Black, background)
    }
}
