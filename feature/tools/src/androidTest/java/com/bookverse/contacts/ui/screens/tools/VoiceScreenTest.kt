package com.bookverse.contacts.ui.screens.tools

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bookverse.contacts.feature.tools.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class VoiceScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun textToSpeechCardKeepsVoiceAndPlaybackActionsAvailable() {
        val callbackCount = AtomicInteger(0)
        showContent {
            TextToSpeechCard(
                textToSpeak = "Hello",
                onTextChange = {},
                selectedVoice = null,
                isSpeaking = false,
                accentColor = Color(0xFF6750A4),
                onSelectVoice = { callbackCount.incrementAndGet() },
                onShare = { callbackCount.incrementAndGet() },
                onSpeak = { callbackCount.incrementAndGet() },
            )
        }

        composeRule.onNodeWithText(text(R.string.select_voice)).performClick()
        composeRule.onNodeWithText(text(R.string.cd_play_text)).performClick()
        composeRule.onNodeWithText(text(R.string.share_audio)).performClick()

        assertEquals(3, callbackCount.get())
    }

    @Test
    fun speechToTextCardExposesListeningActionAndTranscriptionCanCollapse() {
        val listenCount = AtomicInteger(0)
        val copyCount = AtomicInteger(0)
        val transcription = "A transcribed sentence"

        showContent {
            androidx.compose.foundation.layout.Column {
                SpeechToTextCard(
                    isListening = false,
                    partialTranscription = "",
                    selectedLanguage = null,
                    accentColor = Color(0xFF6750A4),
                    onSelectLanguage = {},
                    onListen = { listenCount.incrementAndGet() },
                )
                TranscriptionCard(
                    transcriptionText = transcription,
                    showTranscription = true,
                    onCopy = { copyCount.incrementAndGet() },
                    onToggle = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription(text(R.string.cd_start_listening))
            .performClick()
        composeRule
            .onNodeWithContentDescription(text(R.string.cd_copy_text))
            .performClick()
        composeRule.onNodeWithText(transcription).assertIsDisplayed()

        assertEquals(1, listenCount.get())
        assertEquals(1, copyCount.get())
    }

    @Test
    fun collapsedTranscriptionDoesNotRenderItsSensitiveContent() {
        showContent {
            MaterialTheme {
                TranscriptionCard(
                    transcriptionText = "Hidden transcription",
                    showTranscription = false,
                    onCopy = {},
                    onToggle = {},
                )
            }
        }

        composeRule.onNodeWithText("Hidden transcription").assertDoesNotExist()
        composeRule
            .onNodeWithContentDescription(text(R.string.cd_show_transcription))
            .assertIsDisplayed()
    }

    private fun showContent(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent {
            MaterialTheme(content = content)
        }
        composeRule.waitForIdle()
    }

    private fun text(resourceId: Int): String = composeRule.activity.getString(resourceId)
}
