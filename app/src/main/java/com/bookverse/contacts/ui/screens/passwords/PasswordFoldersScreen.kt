package com.bookverse.contacts.ui.screens.passwords

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookverse.contacts.R
import com.bookverse.contacts.data.password.PasswordFolder
import com.bookverse.contacts.ui.components.AppSnackbarHost
import com.bookverse.contacts.ui.components.ConfirmDialog
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.ui.components.folders.FolderCard
import com.bookverse.contacts.ui.components.folders.FolderEmptyState
import com.bookverse.contacts.ui.components.folders.FolderFormDialog
import com.bookverse.contacts.ui.components.folders.FolderUiModel
import com.bookverse.contacts.ui.components.rememberAppSnackbarState
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.PasswordsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordFoldersScreen(
    onBackClick: () -> Unit,
    passwordsViewModel: PasswordsViewModel,
    appSettingsViewModel: AppSettingsViewModel,
) {
    val folders by passwordsViewModel.folders.collectAsStateWithLifecycle()
    val credentials by passwordsViewModel.credentials.collectAsStateWithLifecycle()
    val accentColor = MaterialTheme.colorScheme.primary
    val folderOrder by appSettingsViewModel.passwordFoldersOrder.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val snackbar = rememberAppSnackbarState()
    val listState = rememberLazyListState()
    val reorderItems = remember { mutableStateListOf<PasswordFolder>() }
    var showForm by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<PasswordFolder?>(null) }
    var folderToDelete by remember { mutableStateOf<PasswordFolder?>(null) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(folders, folderOrder) {
        reorderItems.clear()
        reorderItems.addAll(
            if (folderOrder.isEmpty()) folders else folders.sortedBy { folder ->
                folderOrder.indexOf(folder.id).takeIf { it >= 0 } ?: Int.MAX_VALUE
            },
        )
        initialized = true
    }
    val currentOrderIds by remember { derivedStateOf { reorderItems.map { it.id } } }
    LaunchedEffect(initialized, currentOrderIds) {
        if (initialized) appSettingsViewModel.setPasswordFoldersOrder(context, currentOrderIds)
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
                            onClick = { editingFolder = null; showForm = true },
                        )
                    }
                },
                appSettingsViewModel = appSettingsViewModel,
            )
        },
        snackbarHost = { AppSnackbarHost(snackbar.hostState) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
        ) {
            if (folders.isEmpty()) {
                FolderEmptyState(
                    icon = Icons.Default.Folder,
                    title = stringResource(R.string.password_folders_empty_title),
                    description = stringResource(R.string.password_folders_empty_description),
                    primaryActionLabel = stringResource(R.string.note_folders_create),
                    accentColor = accentColor,
                    onPrimaryAction = { editingFolder = null; showForm = true },
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(reorderItems, key = { _, folder -> folder.id }) { _, folder ->
                        val isDragging = draggingId == folder.id
                        FolderCard(
                            folder = folder.toUiModel(),
                            itemCountLabel = pluralStringResource(
                                R.plurals.password_folder_password_count,
                                credentials.count { it.folderId == folder.id },
                                credentials.count { it.folderId == folder.id },
                            ),
                            accentColor = accentColor,
                            editDescription = stringResource(R.string.edit),
                            deleteDescription = stringResource(R.string.delete),
                            onEdit = { editingFolder = folder; showForm = true },
                            onDelete = { folderToDelete = folder },
                            isDragging = isDragging,
                            modifier = Modifier
                                .graphicsLayer {
                                    translationY = if (isDragging) dragOffsetY else 0f
                                    scaleX = if (isDragging) 1.02f else 1f
                                    scaleY = if (isDragging) 1.02f else 1f
                                    shadowElevation = if (isDragging) with(density) { 12.dp.toPx() } else 0f
                                }
                                .pointerInput(folder.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingId = folder.id
                                            dragOffsetY = 0f
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffsetY += amount.y
                                            val from = reorderItems.indexOfFirst { it.id == folder.id }
                                            val itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == folder.id }
                                                ?: return@detectDragGesturesAfterLongPress
                                            val center = itemInfo.offset + itemInfo.size / 2 + dragOffsetY.roundToInt()
                                            val targetInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                                                it.key != folder.id && center in it.offset..(it.offset + it.size)
                                            } ?: return@detectDragGesturesAfterLongPress
                                            val to = reorderItems.indexOfFirst { it.id == targetInfo.key }
                                            if (from >= 0 && to >= 0 && from != to) {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                reorderItems.add(to, reorderItems.removeAt(from))
                                                dragOffsetY = 0f
                                            }
                                        },
                                        onDragEnd = { draggingId = null; dragOffsetY = 0f },
                                        onDragCancel = { draggingId = null; dragOffsetY = 0f },
                                    )
                                },
                        )
                    }
                }
            }
        }
    }

    if (showForm) {
        val isEditing = editingFolder != null
        FolderFormDialog(
            folder = editingFolder?.toUiModel(),
            title = stringResource(if (isEditing) R.string.note_folders_form_title_edit else R.string.note_folders_form_title_add),
            nameLabel = stringResource(R.string.note_folders_name_label),
            descriptionLabel = stringResource(R.string.note_folders_description_label),
            colorLabel = stringResource(R.string.note_folders_color_label),
            editLabel = stringResource(R.string.edit),
            cancelLabel = stringResource(R.string.cancel),
            saveLabel = stringResource(R.string.save),
            accentColor = accentColor,
            appSettingsViewModel = appSettingsViewModel,
            onDismiss = { showForm = false; editingFolder = null },
            onSave = { draft ->
                passwordsViewModel.saveFolder(
                    PasswordFolder(draft.id, draft.name, draft.description, draft.colorHex, draft.createdAt),
                )
                snackbar.show(
                    resources.getString(
                        if (isEditing) R.string.password_folder_updated else R.string.password_folder_created,
                        draft.name,
                    ),
                    SnackbarType.Success,
                )
                showForm = false
                editingFolder = null
            },
        )
    }

    folderToDelete?.let { folder ->
        ConfirmDialog(
            onDismiss = { folderToDelete = null },
            onConfirm = {
                passwordsViewModel.deleteFolder(folder)
                snackbar.show(resources.getString(R.string.password_folder_deleted, folder.name), SnackbarType.Success)
                folderToDelete = null
            },
            title = stringResource(R.string.note_folders_delete_title),
            message = stringResource(R.string.password_folders_delete_message, folder.name),
            toastMessage = null,
            appSettingsViewModel = appSettingsViewModel,
        )
    }
}

private fun PasswordFolder.toUiModel() = FolderUiModel(id, name, description, colorHex, createdAt)
