package com.bookverse.contacts.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.R
import com.bookverse.contacts.ui.viewmodel.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class IncomingCallBackgroundFieldTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyFieldExplainsPurposeAndInvokesSelection() {
        val clicks = AtomicInteger(0)
        showField(onClick = { clicks.incrementAndGet() })

        composeRule.onNodeWithText(text(R.string.incoming_call_background_title))
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithText(text(R.string.incoming_call_background_description))
            .assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.incoming_call_background_not_configured))
            .assertIsDisplayed()

        assertEquals(1, clicks.get())
    }

    @Test
    fun configuredFieldRemainsUsableAtTwoHundredPercentFontScale() {
        showField(
            backgroundUri = Uri.parse("file:///does-not-need-to-load-for-status.img"),
            fontScale = 2f,
        )

        composeRule.onNodeWithText(text(R.string.incoming_call_background_title))
            .assertIsDisplayed()
            .assertHasClickAction()
        composeRule.onNodeWithText(text(R.string.incoming_call_background_configured))
            .assertIsDisplayed()
    }

    @Test
    fun previewShowsTheContactIdentityAndConfiguredCallActions() {
        composeRule.setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .width(360.dp)
                        .height(720.dp),
                ) {
                    IncomingCallPreview(
                        displayName = "Ada Lovelace",
                        phoneLabel = "Mobile 555 0100",
                        photoUri = null,
                        backgroundUri = Uri.parse("file:///does-not-need-to-load.img"),
                        callAnswerStyle = "buttons",
                        themeMode = ThemeMode.LIGHT,
                        accentColor = Color(0xFF3367D6),
                    )
                }
            }
        }

        composeRule.onNodeWithText("Ada Lovelace").assertIsDisplayed()
        composeRule.onNodeWithText("Mobile 555 0100").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.answer)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.reject)).assertIsDisplayed()
    }

    private fun showField(
        backgroundUri: Uri? = null,
        fontScale: Float = 1f,
        onClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale),
            ) {
                MaterialTheme {
                    Box(
                        modifier = Modifier
                            .width(320.dp)
                            .height(280.dp),
                    ) {
                        IncomingCallBackgroundField(
                            backgroundUri = backgroundUri,
                            isLoading = false,
                            accentColor = Color(0xFF3367D6),
                            onClick = onClick,
                        )
                    }
                }
            }
        }
    }

    private fun text(resourceId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId)
}
