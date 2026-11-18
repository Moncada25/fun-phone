package com.bookverse.contacts.ui.screens.dialer

import android.graphics.Color as AndroidColor
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bookverse.contacts.core.ui.R as CoreUiR

@Composable
internal fun DialerNumberEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    registerEditText: (DialerEditText) -> Unit,
    textColor: Int,
    textSizePx: Float,
    cursorColor: Int,
    selectionBackgroundColor: Int,
    modifier: Modifier = Modifier,
) {
    val latestValue by rememberUpdatedState(value)
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
    AndroidView(
        modifier = modifier,
        factory = { context ->
            DialerEditText(context).apply {
                registerEditText(this)
                setBackgroundColor(AndroidColor.TRANSPARENT)
                isSingleLine = true
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 0)
                showSoftInputOnFocus = false
                setTextIsSelectable(true)
                inputType = InputType.TYPE_CLASS_PHONE
                imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_NONE
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
                applySelectionVisuals(cursorColor, selectionBackgroundColor)
                setText(value.text)
                setSelectionWithoutCallback(value.selection.end.coerceIn(0, value.text.length))
                addTextChangedListener(
                    object : TextWatcher {
                        private var rawChangeCursor = value.selection.end

                        override fun beforeTextChanged(
                            text: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int,
                        ) = Unit

                        override fun onTextChanged(
                            text: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int,
                        ) {
                            rawChangeCursor = start + count
                        }

                        override fun afterTextChanged(editable: Editable?) {
                            if (suppressTextWatcher) return
                            val rawText = editable?.toString().orEmpty()
                            val sanitized = sanitizePastedDialerText(
                                rawText = rawText,
                                rawCursor = rawChangeCursor,
                            )
                            if (sanitized.text != rawText) {
                                suppressTextWatcher = true
                                setText(sanitized.text)
                                setSelectionWithoutCallback(sanitized.selectionStart)
                                suppressTextWatcher = false
                            }
                            val updatedValue = TextFieldValue(
                                text = sanitized.text,
                                selection = TextRange(sanitized.selectionStart),
                            )
                            if (latestValue.text != updatedValue.text ||
                                latestValue.selection.end != updatedValue.selection.end
                            ) {
                                latestOnValueChange(updatedValue)
                            }
                        }
                    }
                )
            }
        },
        update = { editText ->
            registerEditText(editText)
            editText.onSelectionChangedListener = { position ->
                val currentText = editText.text?.toString().orEmpty()
                val clamped = position.coerceIn(0, currentText.length)
                if (latestValue.text != currentText ||
                    latestValue.selection.end != clamped
                ) {
                    latestOnValueChange(
                        TextFieldValue(
                            text = currentText,
                            selection = TextRange(clamped),
                        )
                    )
                }
            }
            val currentText = editText.text?.toString().orEmpty()
            if (currentText != value.text) {
                editText.suppressTextWatcher = true
                editText.setText(value.text)
                editText.setSelectionWithoutCallback(
                    value.selection.end.coerceIn(0, value.text.length)
                )
                editText.suppressTextWatcher = false
            } else {
                val selection = value.selection.end.coerceIn(0, currentText.length)
                if (editText.selectionStart != selection || editText.selectionEnd != selection) {
                    editText.setSelectionWithoutCallback(selection)
                }
            }
            editText.setTextColor(textColor)
            editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
            editText.applySelectionVisuals(cursorColor, selectionBackgroundColor)
        },
    )
}

@Composable
internal fun DialerDeleteButton(
    enabled: Boolean,
    tint: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val deleteDescription = stringResource(CoreUiR.string.delete)

    Box(
        modifier = Modifier
            .size(40.dp)
            .then(
                if (enabled) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier
                }
            )
            .semantics(mergeDescendants = true) {
                contentDescription = deleteDescription
                role = Role.Button
                if (!enabled) {
                    disabled()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Backspace,
            contentDescription = null,
            tint = if (enabled) {
                tint
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
            modifier = Modifier.size(30.dp),
        )
    }
}

private fun DialerEditText.setSelectionWithoutCallback(position: Int) {
    suppressSelectionCallback = true
    setSelection(position)
    suppressSelectionCallback = false
}
