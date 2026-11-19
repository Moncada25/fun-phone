package com.bookverse.contacts.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeRevealTest {
    private val screen = Size(300f, 400f)

    @Test
    fun radiusFromTheCenterReachesEveryCorner() {
        assertEquals(250f, revealRadius(Offset(150f, 200f), screen), 0.01f)
    }

    @Test
    fun radiusFromACornerReachesTheOppositeCorner() {
        assertEquals(500f, revealRadius(Offset.Zero, screen), 0.01f)
        assertEquals(500f, revealRadius(Offset(300f, 400f), screen), 0.01f)
    }

    @Test
    fun revealDurationFollowsAnimationSpeed() {
        assertEquals(0, themeRevealDurationMs(0))
        assertEquals(600, themeRevealDurationMs(500))
    }
}
