package com.bookverse.contacts.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class ContactAvatarTextTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun wideInitialsFitOnOneLineInCompactAvatarAtLargeFontScale() {
        composeRule.setContent {
            val density = Density(LocalDensity.current.density, fontScale = 2f)
            CompositionLocalProvider(LocalDensity provides density) {
                MaterialTheme {
                    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                        ContactAvatarText("WW", Color.Black, FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }
        }

        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText("WW").performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it(layouts)
        }
        val layout = layouts.single()
        assertEquals(1, layout.lineCount)
        assertEquals(2, layout.getLineEnd(0, visibleEnd = true))
        assertFalse(layout.hasVisualOverflow)
    }
}
