package com.bookverse.contacts.ui.screens.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.R
import com.bookverse.contacts.data.notes.ChecklistItem
import com.bookverse.contacts.ui.theme.AccentBarStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChecklistItemEditSheet(
    item: ChecklistItem,
    accentColor: Color,
    accentBarStyle: AccentBarStyle,
    onTextChange: (String) -> Unit,
    onToggleChecked: () -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val colorScheme = MaterialTheme.colorScheme
    val isRetro = accentBarStyle == AccentBarStyle.RETRO
    val accentContentColor = contentColorFor(accentColor)
    val headerColor = if (isRetro) accentColor else accentColor.copy(alpha = 0.12f)
    val headerContentColor = if (isRetro) accentContentColor else colorScheme.onSurface
    val actionColor = if (isRetro) accentColor else accentColor.copy(alpha = 0.16f)
    val actionContentColor = if (isRetro) accentContentColor else accentColor
    val actionShape = if (isRetro) RoundedCornerShape(999.dp) else RoundedCornerShape(16.dp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = headerColor,
                contentColor = headerContentColor,
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(
                    text = stringResource(R.string.edit_checklist_item),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            OutlinedTextField(
                value = item.text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 144.dp, max = 280.dp),
                label = { Text(stringResource(R.string.checklist_item_content)) },
                textStyle = MaterialTheme.typography.bodyLarge,
                minLines = 5,
                maxLines = 10,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    focusedLabelColor = accentColor,
                    cursorColor = accentColor,
                    selectionColors = TextSelectionColors(
                        handleColor = accentColor,
                        backgroundColor = accentColor.copy(alpha = 0.4f),
                    ),
                ),
            )

            Button(
                onClick = onCopy,
                enabled = item.text.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.surface,
                    contentColor = colorScheme.onSurface,
                    disabledContainerColor = colorScheme.surface.copy(alpha = 0.5f),
                    disabledContentColor = colorScheme.onSurface.copy(alpha = 0.5f),
                ),
                shape = actionShape,
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.checklist_copy_item),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.checklist_copy_item))
            }

            Button(
                onClick = onToggleChecked,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = actionColor,
                    contentColor = actionContentColor,
                ),
                shape = actionShape,
            ) {
                Icon(
                    imageVector = if (item.isChecked) {
                        Icons.Outlined.RadioButtonUnchecked
                    } else {
                        Icons.Default.CheckCircle
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        if (item.isChecked) {
                            R.string.checklist_mark_incomplete
                        } else {
                            R.string.checklist_mark_complete
                        },
                    ),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
