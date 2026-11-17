package com.bookverse.contacts.ui.screens.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.bookverse.contacts.R
import com.bookverse.contacts.data.model.ContactTag
import com.bookverse.contacts.data.notes.NoteTag
import com.bookverse.contacts.ui.components.ButtonSheetScaffold
import com.bookverse.contacts.ui.components.ConfirmDialog
import com.bookverse.contacts.ui.components.TagFormDialog
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel

internal const val NOTE_TAGS_VISIBLE_ON_ITEM = 3

internal fun NoteTag.asSharedTag(): ContactTag = ContactTag(
    id = id,
    name = name,
    colorHex = colorHex,
    createdAt = createdAt,
)

@Composable
internal fun noteTagColor(colorHex: String): Color {
    val fallback = MaterialTheme.colorScheme.primary
    return remember(colorHex, fallback) {
        runCatching { Color(colorHex.toColorInt()) }.getOrDefault(fallback)
    }
}

@Composable
internal fun NoteTagsRow(
    tags: List<NoteTag>,
    modifier: Modifier = Modifier,
) {
    if (tags.isEmpty()) return
    val visible = tags.take(NOTE_TAGS_VISIBLE_ON_ITEM)
    val hidden = tags.size - visible.size
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        visible.forEach { tag ->
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(noteTagColor(tag.colorHex)),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (hidden > 0) {
            Text(
                text = stringResource(R.string.note_tags_more, hidden),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun NoteTagsManagerSheet(
    tags: List<NoteTag>,
    appSettingsViewModel: AppSettingsViewModel,
    onCreate: (name: String, colorHex: String, onResult: (Boolean) -> Unit) -> Unit,
    onUpdate: (tag: NoteTag, onResult: (Boolean) -> Unit) -> Unit,
    onDelete: (NoteTag) -> Unit,
    onNameConflict: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }
    var tagToEdit by remember { mutableStateOf<NoteTag?>(null) }
    var tagToDelete by remember { mutableStateOf<NoteTag?>(null) }
    val accentColor = MaterialTheme.colorScheme.primary

    ButtonSheetScaffold(
        title = stringResource(R.string.tag_manager_title),
        subtitle = stringResource(R.string.note_tags_manager_description),
        onDismiss = onDismiss,
    ) {
        if (tags.isEmpty()) {
            Text(
                text = stringResource(R.string.no_tags_available),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(tags, key = { it.id }) { tag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(noteTagColor(tag.colorHex)),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = tag.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        IconButton(onClick = { tagToEdit = tag }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit_tag),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        IconButton(onClick = { tagToDelete = tag }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_tag),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.size(12.dp))
        Button(
            onClick = { showCreate = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(999.dp),
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.create_tag))
        }
    }

    if (showCreate) {
        TagFormDialog(
            tag = null,
            appSettingsViewModel = appSettingsViewModel,
            onDismiss = { showCreate = false },
            onSave = { name, colorHex ->
                onCreate(name, colorHex) { created ->
                    if (created) showCreate = false else onNameConflict()
                }
            },
        )
    }

    tagToEdit?.let { editing ->
        TagFormDialog(
            tag = editing.asSharedTag(),
            appSettingsViewModel = appSettingsViewModel,
            onDismiss = { tagToEdit = null },
            onSave = { name, colorHex ->
                onUpdate(editing.copy(name = name, colorHex = colorHex)) { updated ->
                    if (updated) tagToEdit = null else onNameConflict()
                }
            },
        )
    }

    tagToDelete?.let { deleting ->
        ConfirmDialog(
            onDismiss = { tagToDelete = null },
            onConfirm = {
                onDelete(deleting)
                tagToDelete = null
            },
            title = stringResource(R.string.delete_tag),
            message = stringResource(R.string.note_tag_delete_confirmation, deleting.name),
            toastMessage = null,
            appSettingsViewModel = appSettingsViewModel,
        )
    }
}
