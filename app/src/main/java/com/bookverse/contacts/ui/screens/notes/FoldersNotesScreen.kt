package com.bookverse.contacts.ui.screens.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.R
import com.bookverse.contacts.data.notes.NoteFolder
import com.bookverse.contacts.integration.NotesViewModelFactory
import com.bookverse.contacts.ui.components.AppSnackbarHost
import com.bookverse.contacts.ui.components.ColorPickerDialog
import com.bookverse.contacts.ui.components.ConfirmDialog
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.ui.components.TextField
import com.bookverse.contacts.ui.components.rememberAppSnackbarState
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.NotesViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersNotesScreen(
    onBackClick: () -> Unit,
    notesViewModel: NotesViewModel = viewModel(
        factory = NotesViewModelFactory(LocalContext.current)
    ),
    appSettingsViewModel: AppSettingsViewModel = viewModel()
) {
    val folders by notesViewModel.noteFolders.collectAsStateWithLifecycle()
    val notes by notesViewModel.notes.collectAsStateWithLifecycle()
    val accentColor = MaterialTheme.colorScheme.primary
    val folderOrder by appSettingsViewModel.noteFoldersOrder.collectAsStateWithLifecycle()
    val snackbar = rememberAppSnackbarState()
    val context = LocalContext.current
    val resources = LocalResources.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    var showForm by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<NoteFolder?>(null) }
    var folderToDelete by remember { mutableStateOf<NoteFolder?>(null) }
    val listState = rememberLazyListState()
    val backgroundColor = MaterialTheme.colorScheme.background
    val reorderItems = remember { mutableStateListOf<NoteFolder>() }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(folders, folderOrder) {
        reorderItems.clear()
        val sorted = if (folderOrder.isEmpty()) {
            folders
        } else {
            folders.sortedBy { folder ->
                val idx = folderOrder.indexOf(folder.id)
                if (idx == -1) Int.MAX_VALUE else idx
            }
        }
        reorderItems.addAll(sorted)
        initialized = true
    }
    val currentOrderIds by remember {
        derivedStateOf { reorderItems.map { it.id } }
    }
    LaunchedEffect(initialized, currentOrderIds) {
        if (initialized) {
            appSettingsViewModel.setNoteFoldersOrder(context, currentOrderIds)
        }
    }

    BackHandler { onBackClick() }

    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                expandable = true,
                scrollBehavior = scrollBehavior,
                title = stringResource(R.string.note_folders_title),
                onBackClick = onBackClick,
                actions = {
                    if (folders.isNotEmpty()) {
                        IconAction(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = stringResource(R.string.note_folders_create),
                            onClick = {
                                editingFolder = null
                                showForm = true
                            }
                        )
                    }
                },
                appSettingsViewModel = appSettingsViewModel
            )
        },
        snackbarHost = { AppSnackbarHost(snackbar.hostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding)
        ) {
            if (folders.isEmpty()) {
                EmptyFoldersState(
                    onCreateFolder = {
                        editingFolder = null
                        showForm = true
                    },
                    accentColor = accentColor
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(reorderItems, key = { _, folder -> folder.id }) { _, folder ->
                        val noteCount = notes.count { it.folderId == folder.id }
                        val isDragging = draggingId == folder.id
                        val shadow = if (isDragging) with(density) { 12.dp.toPx() } else 0f
                        NoteFolderCard(
                            folder = folder,
                            noteCount = noteCount,
                            accentColor = accentColor,
                            onEdit = {
                                editingFolder = folder
                                showForm = true
                            },
                            onDelete = { folderToDelete = folder },
                            modifier = Modifier
                                .graphicsLayer {
                                    translationY = if (isDragging) dragOffsetY else 0f
                                    scaleX = if (isDragging) 1.02f else 1f
                                    scaleY = if (isDragging) 1.02f else 1f
                                    shadowElevation = shadow
                                }
                                .pointerInput(folder.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingId = folder.id
                                            dragOffsetY = 0f
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            if (draggingId != folder.id) return@detectDragGesturesAfterLongPress
                                            dragOffsetY += dragAmount.y

                                            val draggedIndex =
                                                reorderItems.indexOfFirst { it.id == folder.id }
                                            if (draggedIndex == -1) return@detectDragGesturesAfterLongPress

                                            val layoutInfo = listState.layoutInfo
                                            val draggedItemInfo = layoutInfo.visibleItemsInfo
                                                .firstOrNull { it.key == folder.id }
                                                ?: return@detectDragGesturesAfterLongPress

                                            val draggedCenter = draggedItemInfo.offset +
                                                    draggedItemInfo.size / 2 +
                                                    dragOffsetY.roundToInt()

                                            val targetItem =
                                                layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                                    if (item.key == folder.id) return@firstOrNull false
                                                    val top = item.offset
                                                    val bottom = item.offset + item.size
                                                    draggedCenter in top..bottom
                                                } ?: return@detectDragGesturesAfterLongPress

                                            val targetIndex =
                                                reorderItems.indexOfFirst { it.id == targetItem.key }
                                            if (targetIndex == -1 || targetIndex == draggedIndex) return@detectDragGesturesAfterLongPress

                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val moved = reorderItems.removeAt(draggedIndex)
                                            reorderItems.add(targetIndex, moved)
                                            dragOffsetY = 0f
                                        },
                                        onDragEnd = {
                                            draggingId = null
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggingId = null
                                            dragOffsetY = 0f
                                        }
                                    )
                                },
                            isDragging = isDragging
                        )
                    }
                }
            }
        }
    }

    if (showForm) {
        val isEditing = editingFolder != null
        NoteFolderFormDialog(
            folder = editingFolder,
            accentColor = accentColor,
            onDismiss = {
                showForm = false
                editingFolder = null
            },
            onSave = { folder ->
                notesViewModel.saveNoteFolder(folder)
                snackbar.show(
                    message = resources.getString(
                        if (isEditing) R.string.note_folder_updated else R.string.note_folder_created,
                        folder.name
                    ),
                    type = SnackbarType.Success
                )
                showForm = false
                editingFolder = null
            },
            appSettingsViewModel = appSettingsViewModel
        )
    }

    folderToDelete?.let { target ->
        ConfirmDialog(
            onDismiss = { folderToDelete = null },
            onConfirm = {
                notesViewModel.deleteNoteFolder(target)
                snackbar.show(
                    message = resources.getString(R.string.note_folder_deleted, target.name),
                    type = SnackbarType.Success
                )
                folderToDelete = null
            },
            title = stringResource(R.string.note_folders_delete_title),
            message = stringResource(R.string.note_folders_delete_message, target.name),
            toastMessage = null,
            appSettingsViewModel = appSettingsViewModel
        )
    }
}

@Composable
private fun EmptyFoldersState(
    onCreateFolder: () -> Unit,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = accentColor
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.note_folders_empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.note_folders_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreateFolder,
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(
                text = stringResource(R.string.note_folders_create),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NoteFolderCard(
    folder: NoteFolder,
    noteCount: Int,
    accentColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false
) {
    val folderColor = parseFolderColorOrDefault(folder.colorHex, accentColor)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (isDragging) 0.95f else 1f },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(folderColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = folderColor
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.note_folder_note_count,
                            noteCount,
                            noteCount
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            folder.description?.takeIf { it.isNotBlank() }?.let { description ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun NoteFolderFormDialog(
    folder: NoteFolder?,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSave: (NoteFolder) -> Unit,
    appSettingsViewModel: AppSettingsViewModel
) {
    var name by rememberSaveable(folder?.id) { mutableStateOf(folder?.name.orEmpty()) }
    var description by rememberSaveable(folder?.id) { mutableStateOf(folder?.description.orEmpty()) }
    var colorHex by rememberSaveable(folder?.id) {
        mutableStateOf(folder?.colorHex ?: accentColor.toFolderHex())
    }
    var showColorPicker by remember { mutableStateOf(false) }
    val selectedColor = remember(colorHex, accentColor) {
        parseFolderColorOrDefault(colorHex, accentColor)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(
                        if (folder == null) R.string.note_folders_form_title_add
                        else R.string.note_folders_form_title_edit
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.note_folders_name_label),
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = stringResource(R.string.note_folders_description_label),
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
                Column {
                    Text(
                        text = stringResource(R.string.note_folders_color_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showColorPicker = true }
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(selectedColor)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedColor.toFolderHex(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.edit),
                                style = MaterialTheme.typography.labelMedium,
                                color = accentColor
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Button(
                        onClick = {
                            val timestamp = folder?.createdAt ?: System.currentTimeMillis()
                            onSave(
                                NoteFolder(
                                    id = folder?.id ?: 0,
                                    name = name.trim(),
                                    description = description.trim().takeIf { it.isNotBlank() },
                                    colorHex = colorHex.ifBlank { accentColor.toFolderHex() },
                                    createdAt = timestamp
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = selectedColor,
            onColorSelected = {
                colorHex = it.toFolderHex()
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false },
            appSettingsViewModel = appSettingsViewModel
        )
    }
}
