package com.bookverse.contacts.ui.screens.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.R
import com.bookverse.contacts.data.notes.ChecklistItem
import com.bookverse.contacts.data.notes.Note
import com.bookverse.contacts.integration.NotesViewModelFactory
import com.bookverse.contacts.data.notes.RecordingData
import com.bookverse.contacts.data.notes.ReminderData
import com.bookverse.contacts.data.notes.ReminderType
import com.bookverse.contacts.ui.components.AppSnackbarHost
import com.bookverse.contacts.ui.components.ConfirmDialog
import com.bookverse.contacts.ui.components.NoteItem
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.ui.components.rememberAppSnackbarState
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.NotesViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mic
import com.composables.icons.lucide.NotebookPen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    onBackClick: () -> Unit,
    notesViewModel: NotesViewModel = viewModel(
        factory = NotesViewModelFactory(LocalContext.current)
    ),
    appSettingsViewModel: AppSettingsViewModel = viewModel()
) {
    val resources = LocalResources.current
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val deletedNotes by notesViewModel.deletedNotes.collectAsStateWithLifecycle()
    val accentColor = MaterialTheme.colorScheme.primary
    val listDensity by appSettingsViewModel.listDensity.collectAsStateWithLifecycle()
    val snackbar = rememberAppSnackbarState()
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showDeletePermanentlyDialog by remember { mutableStateOf(false) }
    var showEmptyRecycleBinDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var noteToRestore by remember { mutableStateOf<Note?>(null) }
    var noteToDeletePermanently by remember { mutableStateOf<Note?>(null) }

    fun deletedPermanentlyMessageRes(note: Note): Int = when {
        note.isReminder -> R.string.reminder_permanently_deleted
        note.isRecording -> R.string.recording_permanently_deleted
        note.isChecklist -> R.string.checklist_permanently_deleted
        else -> R.string.note_permanently_deleted
    }

    BackHandler { onBackClick() }

    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                expandable = true,
                title = stringResource(R.string.recycle_bin_title),
                count = deletedNotes.size,
                onBackClick = onBackClick,
                onCountClick = { showNotesDialog = true },
                actions = {
                    if (deletedNotes.isNotEmpty()) {
                        IconAction(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = stringResource(R.string.empty_recycle_bin),
                            onClick = { showEmptyRecycleBinDialog = true }
                        )
                    }
                },
                appSettingsViewModel = appSettingsViewModel,
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { AppSnackbarHost(snackbar.hostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding)
        ) {
            if (deletedNotes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.recycle_bin_empty),
                        modifier = Modifier.size(120.dp),
                        tint = accentColor.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.recycle_bin_empty),
                        color = textColor.copy(alpha = 0.8f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.recycle_bin_empty_description),
                        color = textColor.copy(alpha = 0.6f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            } else {
                LazyColumn(
                state = com.bookverse.contacts.ui.components.rememberDestinationListState(),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    items(deletedNotes, key = { it.id }) { note ->
                        var checklist by remember { mutableStateOf(emptyList<ChecklistItem>()) }
                        var reminderData by remember { mutableStateOf<ReminderData?>(null) }
                        var recordingData by remember { mutableStateOf<RecordingData?>(null) }

                        LaunchedEffect(note.id) {
                            checklist = notesViewModel.getChecklistForNote(note.id)
                            if (note.isReminder) {
                                reminderData = notesViewModel.getReminderDataForNote(note.id)
                            }
                            if (note.isRecording) {
                                recordingData = notesViewModel.getRecordingDataForNote(note.id)
                            }
                        }

                        val icon = when {
                            note.isChecklist -> Icons.Default.Checklist
                            note.isReminder -> Icons.Default.Notifications
                            note.isRecording -> Lucide.Mic
                            else -> Lucide.NotebookPen
                        }

                        val detailSubtitle = when {
                            note.isChecklist && checklist.isNotEmpty() -> {
                                val completedCount = checklist.count { it.isChecked }
                                val totalCount = checklist.size
                                val completionPercentage =
                                    if (totalCount > 0) (completedCount * 100) / totalCount else 0
                                "$completedCount/$totalCount ($completionPercentage %)"
                            }

                            note.isReminder && reminderData != null -> {
                                val data = reminderData!!
                                if (data.type == ReminderType.PERIODIC) {
                                    val intervalText = when (data.interval) {
                                        60 -> "1 min"
                                        600 -> "10 min"
                                        1800 -> "30 min"
                                        3600 -> "60 min"
                                        else -> "${data.interval} sec"
                                    }
                                    stringResource(R.string.interval_list, intervalText)
                                } else {
                                    when (val minutes = data.advanceNotificationMinutes) {
                                        0 -> stringResource(R.string.no_advance_notification)
                                        5 -> stringResource(R.string.advance_5_min)
                                        10 -> stringResource(R.string.advance_10_min)
                                        15 -> stringResource(R.string.advance_15_min)
                                        30 -> stringResource(R.string.advance_30_min)
                                        60 -> stringResource(R.string.advance_1_hour)
                                        else -> "$minutes ${stringResource(R.string.advance_notification_suffix)}"
                                    }
                                }
                            }

                            note.isRecording && recordingData != null -> {
                                val durationSeconds = recordingData!!.duration / 1000
                                val minutes = durationSeconds / 60
                                val seconds = durationSeconds % 60
                                stringResource(
                                    R.string.recording_duration,
                                    "${minutes}:${seconds.toString().padStart(2, '0')}",
                                    seconds.toString().padStart(2, '0')
                                )
                            }

                            else -> null
                        }

                        val lockBadge = when {
                            !note.isLocked -> null
                            else -> stringResource(R.string.note_locked_badge)
                        }

                        val subtitle = listOfNotNull(
                            lockBadge,
                            detailSubtitle
                        ).takeIf { it.isNotEmpty() }?.joinToString(" • ")

                        NoteItem(
                            icon = icon,
                            title = note.title,
                            onClick = {},
                            onDeleteClick = {
                                noteToDeletePermanently = note
                                showDeletePermanentlyDialog = true
                            },
                            onShareClick = null,
                            onSaveClick = null,
                            onLockClick = null,
                            isLocked = note.isLocked,
                            listDensity = listDensity,
                            createdAt = note.createdAt,
                            updatedAt = note.updatedAt,
                            timestamp = note.timestamp,
                            subtitle = subtitle,
                            isNormalNote = false,
                            onTransformToAudio = null,
                            restoreActionIcon = Icons.Default.RestoreFromTrash,
                            restoreActionDescription = stringResource(R.string.restore),
                            onRestoreActionClick = {
                                noteToRestore = note
                                showRestoreDialog = true
                            },
                            replaceDeleteWithCustom = true,
                            deleteForeverActionIcon = Icons.Default.DeleteForever,
                            deleteForeverActionDescription = stringResource(R.string.delete_permanently)
                        )
                    }
                }
            }
        }

        if (showRestoreDialog && noteToRestore != null) {
            val message = when {
                noteToRestore!!.isChecklist -> stringResource(R.string.checklist_restored)
                noteToRestore!!.isReminder -> stringResource(R.string.reminder_restored)
                noteToRestore!!.isRecording -> stringResource(R.string.recording_restored)
                else -> stringResource(R.string.note_restored)
            }
            ConfirmDialog(
                onDismiss = { showRestoreDialog = false },
                onConfirm = {
                    noteToRestore?.let { notesViewModel.restoreNote(it.id) }
                    noteToRestore = null
                    showRestoreDialog = false
                    snackbar.show(
                        message = message,
                        type = SnackbarType.Success
                    )
                },
                title = stringResource(R.string.restore),
                message = stringResource(R.string.restore_confirm, noteToRestore!!.title),
                toastMessage = null,
                appSettingsViewModel = appSettingsViewModel,
                confirmButtonLabel = stringResource(R.string.restore),
                isDestructive = false,
            )
        }

        if (showDeletePermanentlyDialog && noteToDeletePermanently != null) {
            val message = deletedPermanentlyMessageRes(noteToDeletePermanently!!)
            ConfirmDialog(
                onDismiss = { showDeletePermanentlyDialog = false },
                onConfirm = {
                    noteToDeletePermanently?.let { notesViewModel.permanentlyDeleteNote(it) }
                    noteToDeletePermanently = null
                    showDeletePermanentlyDialog = false
                    snackbar.show(
                        message = resources.getString(message),
                        type = SnackbarType.Success
                    )
                },
                title = stringResource(R.string.delete_permanently),
                message = stringResource(
                    R.string.delete_permanently_confirm,
                    noteToDeletePermanently!!.title
                ),
                toastMessage = null,
                appSettingsViewModel = appSettingsViewModel
            )
        }

        if (showEmptyRecycleBinDialog) {
            ConfirmDialog(
                onDismiss = { showEmptyRecycleBinDialog = false },
                onConfirm = {
                    deletedNotes.forEach { note ->
                        notesViewModel.permanentlyDeleteNote(note)
                    }
                    showEmptyRecycleBinDialog = false
                    snackbar.show(
                        message = resources.getString(R.string.recycle_bin_emptied),
                        type = SnackbarType.Success
                    )
                },
                title = stringResource(R.string.empty_recycle_bin),
                message = stringResource(R.string.empty_recycle_bin_confirm),
                toastMessage = null,
                appSettingsViewModel = appSettingsViewModel
            )
        }
    }

    if (showNotesDialog) {
        NoteTypeCountsSheet(
            typeCounts = listOf(
                NoteTypeCount(
                    icon = Lucide.NotebookPen,
                    label = stringResource(R.string.note),
                    count = deletedNotes.count {
                        !it.isChecklist && !it.isReminder && !it.isRecording
                    },
                ),
                NoteTypeCount(
                    icon = Icons.Default.Checklist,
                    label = stringResource(R.string.checklist),
                    count = deletedNotes.count { it.isChecklist },
                ),
                NoteTypeCount(
                    icon = Icons.Default.Notifications,
                    label = stringResource(R.string.reminder),
                    count = deletedNotes.count { it.isReminder },
                ),
                NoteTypeCount(
                    icon = Lucide.Mic,
                    label = stringResource(R.string.recording),
                    count = deletedNotes.count { it.isRecording },
                ),
            ),
            accentColor = accentColor,
            onDismiss = { showNotesDialog = false },
        )
    }
}
