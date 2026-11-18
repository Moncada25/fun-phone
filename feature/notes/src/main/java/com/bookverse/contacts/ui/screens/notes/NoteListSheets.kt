package com.bookverse.contacts.ui.screens.notes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.notes.Note
import com.bookverse.contacts.data.notes.NoteTag
import com.bookverse.contacts.ui.components.TagAssignmentDialog
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.NotesViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mic
import com.composables.icons.lucide.NotebookPen

@Composable
internal fun NoteListTagAndCountSheets(
    noteForTags: Note?,
    onNoteForTagsDismiss: () -> Unit,
    showTagManager: Boolean,
    onTagManagerDismiss: () -> Unit,
    showTypeCounts: Boolean,
    onTypeCountsDismiss: () -> Unit,
    notes: List<Note>,
    noteTags: List<NoteTag>,
    tagIdsByNoteId: Map<Long, Set<Long>>,
    notesViewModel: NotesViewModel,
    appSettingsViewModel: AppSettingsViewModel,
    onTagNameConflict: () -> Unit,
) {
    val sharedTags = remember(noteTags) { noteTags.map { it.asSharedTag() } }
    noteForTags?.let { target ->
        TagAssignmentDialog(
            allTags = sharedTags,
            assignedTagIds = tagIdsByNoteId[target.id].orEmpty(),
            onApply = { tagIds ->
                notesViewModel.setNoteTags(target.id, tagIds)
                onNoteForTagsDismiss()
            },
            onCreateTag = { name, colorHex ->
                notesViewModel.createNoteTag(name, colorHex) { created ->
                    if (!created) onTagNameConflict()
                }
            },
            onDismiss = onNoteForTagsDismiss,
            appSettingsViewModel = appSettingsViewModel,
        )
    }

    if (showTagManager) {
        NoteTagsManagerSheet(
            tags = noteTags,
            appSettingsViewModel = appSettingsViewModel,
            onCreate = { name, colorHex, onResult ->
                notesViewModel.createNoteTag(name, colorHex, onResult)
            },
            onUpdate = { tag, onResult -> notesViewModel.updateNoteTag(tag, onResult) },
            onDelete = { notesViewModel.deleteNoteTag(it) },
            onNameConflict = onTagNameConflict,
            onDismiss = onTagManagerDismiss,
        )
    }

    if (showTypeCounts) {
        NoteTypeCountsSheet(
            typeCounts = listOf(
                NoteTypeCount(
                    icon = Lucide.NotebookPen,
                    label = stringResource(R.string.note),
                    count = notes.count { !it.isChecklist && !it.isReminder && !it.isRecording },
                ),
                NoteTypeCount(
                    icon = Icons.Default.Checklist,
                    label = stringResource(R.string.checklist),
                    count = notes.count { it.isChecklist },
                ),
                NoteTypeCount(
                    icon = Icons.Default.Notifications,
                    label = stringResource(R.string.reminder),
                    count = notes.count { it.isReminder },
                ),
                NoteTypeCount(
                    icon = Lucide.Mic,
                    label = stringResource(R.string.recording),
                    count = notes.count { it.isRecording },
                ),
            ),
            accentColor = MaterialTheme.colorScheme.primary,
            onDismiss = onTypeCountsDismiss,
        )
    }
}
