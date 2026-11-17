package com.bookverse.contacts.ui.screens.contacts.dialer

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.bookverse.contacts.testing.ComposeTestContent
import com.bookverse.contacts.testing.ComposeTestHostActivity
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DialerNumberEditorTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestHostActivity>()

    @Test
    fun nativeEditorSanitizesPasteAndSelectedReplacement() {
        val editor = AtomicReference<DialerEditText>()
        val observed = AtomicReference(TextFieldValue())
        showEditor(editor, observed)
        composeRule.waitUntil { editor.get() != null }

        composeRule.runOnUiThread {
            editor.get().setText("+1 (555) 010-099 ext")
        }
        composeRule.waitUntil { observed.get().text == "+1555010099" }
        assertEquals(11, observed.get().selection.end)

        composeRule.runOnUiThread {
            editor.get().apply {
                setSelection(2, 5)
                text?.replace(2, 5, "*#")
            }
        }
        composeRule.waitUntil { observed.get().text == "+1*#010099" }

        assertEquals("+1*#010099", observed.get().text)
        assertEquals(4, observed.get().selection.end)

        composeRule.runOnUiThread {
            editor.get().apply {
                val currentLength = text?.length ?: 0
                setSelection(currentLength)
                text?.delete(currentLength - 1, currentLength)
            }
        }
        composeRule.waitUntil { observed.get().text == "+1*#01009" }

        assertEquals("+1*#01009", observed.get().text)
        assertEquals(9, observed.get().selection.end)
    }

    @Test
    fun externalNumberReplacementKeepsCaretAtEndWithoutEmittingEdits() {
        val initial = TextFieldValue("12345", TextRange(5))
        val value = mutableStateOf(initial)
        val editor = AtomicReference<DialerEditText>()
        val observed = AtomicReference(initial)
        showEditor(editor, observed, value)
        composeRule.waitUntil { editor.get()?.text?.toString() == initial.text }

        val number = "+15550987654"
        composeRule.runOnIdle {
            value.value = TextFieldValue(number, TextRange(number.length))
        }
        composeRule.waitUntil { editor.get()?.text?.toString() == number }
        composeRule.runOnIdle {
            assertEquals(number.length, editor.get().selectionStart)
            assertEquals(number.length, editor.get().selectionEnd)
            assertEquals(initial, observed.get())
        }
    }

    private fun showEditor(
        editor: AtomicReference<DialerEditText>,
        observed: AtomicReference<TextFieldValue>,
        valueState: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue("", TextRange.Zero)),
    ) {
        composeRule.runOnUiThread {
            ComposeTestContent.show {
                var value by valueState
                MaterialTheme {
                    DialerNumberEditor(
                        value = value,
                        onValueChange = {
                            value = it
                            observed.set(it)
                        },
                        registerEditText = editor::set,
                        textColor = MaterialTheme.colorScheme.onSurface.toArgb(),
                        textSizePx = 32f,
                        cursorColor = Color(0xFF2196F3).toArgb(),
                        selectionBackgroundColor =
                            Color(0xFF2196F3).copy(alpha = 0.4f).toArgb(),
                    )
                }
            }
        }
    }
}
