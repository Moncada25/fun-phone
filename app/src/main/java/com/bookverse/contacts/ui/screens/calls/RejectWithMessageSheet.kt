package com.bookverse.contacts.ui.screens.calls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.R
import com.bookverse.contacts.domain.call.RejectMessagePolicy
import com.bookverse.contacts.ui.components.ButtonSheetScaffold
import com.bookverse.contacts.ui.screens.settings.SettingsChoiceRow

internal val rejectQuickReplies = listOf(
    R.string.reject_message_quick_busy,
    R.string.reject_message_quick_call_back,
    R.string.reject_message_quick_meeting,
    R.string.reject_message_quick_driving,
)

@Composable
internal fun RejectWithMessageSheet(
    onSend: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var customMessage by rememberSaveable { mutableStateOf("") }
    val canSendCustom = RejectMessagePolicy.sanitize(customMessage) != null

    fun sendCustom() {
        RejectMessagePolicy.sanitize(customMessage)?.let(onSend)
    }

    ButtonSheetScaffold(
        title = stringResource(R.string.reject_with_message),
        subtitle = stringResource(R.string.reject_with_message_subtitle),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rejectQuickReplies.forEachIndexed { index, replyRes ->
                val reply = stringResource(replyRes)
                SettingsChoiceRow(
                    label = reply,
                    selected = false,
                    onClick = { onSend(reply) },
                    modifier = Modifier.testTag(rejectQuickReplyTag(index)),
                )
            }
            OutlinedTextField(
                value = customMessage,
                onValueChange = { customMessage = RejectMessagePolicy.limitInput(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(REJECT_MESSAGE_INPUT_TAG),
                label = { Text(stringResource(R.string.reject_message_custom_label)) },
                supportingText = {
                    Text(
                        stringResource(
                            R.string.reject_message_counter,
                            customMessage.length,
                            RejectMessagePolicy.MAX_LENGTH,
                        ),
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = ::sendCustom,
                        enabled = canSendCustom,
                        modifier = Modifier.testTag(REJECT_MESSAGE_SEND_TAG),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.reject_message_send),
                        )
                    }
                },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send,
                ),
                keyboardActions = KeyboardActions(onSend = { sendCustom() }),
                shape = RoundedCornerShape(16.dp),
            )
        }
    }
}

internal fun rejectQuickReplyTag(index: Int): String = "reject_quick_reply_$index"

internal const val REJECT_MESSAGE_BUTTON_TAG = "reject_with_message_button"
internal const val REJECT_MESSAGE_INPUT_TAG = "reject_message_input"
internal const val REJECT_MESSAGE_SEND_TAG = "reject_message_send"
