package com.bookverse.contacts.ui.screens.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.bookverse.contacts.feature.notes.R
import com.bookverse.contacts.core.ui.R as CoreUiR
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.data.notes.ChecklistItem
import com.bookverse.contacts.data.notes.Note
import com.bookverse.contacts.integration.NotesViewModelFactory
import com.bookverse.contacts.ui.components.MessageDialog
import com.bookverse.contacts.ui.components.NoteTitle
import com.bookverse.contacts.ui.components.OptionsDialog
import com.bookverse.contacts.ui.components.OptionsDialogItem
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.SelectBottomSheet
import com.bookverse.contacts.ui.components.TopBarSearchField
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.NotesViewModel
import com.bookverse.contacts.utils.ClipboardUtils
import com.bookverse.contacts.utils.VibrationUtils
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.SearchX

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChecklistScreen(
    note: Note?,
    checklist: List<ChecklistItem> = emptyList(),
    onSave: (Note, List<ChecklistItem>) -> Unit,
    onCancel: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel = viewModel()
) {
    val hapticFeedback = LocalHapticFeedback.current
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val accentColor = MaterialTheme.colorScheme.primary
    val accentBarStyle by appSettingsViewModel.accentBarStyle.collectAsStateWithLifecycle()
    val vibrationDuration by appSettingsViewModel.vibrationDuration.collectAsStateWithLifecycle()
    val vibrationPattern by appSettingsViewModel.vibrationPattern.collectAsStateWithLifecycle()

    val originalTitle = remember(note?.id) { note?.title ?: "" }
    val originalChecklist = remember(note?.id) { checklist.map { it.copy() } }

    val checklistState =
        remember { mutableStateListOf<ChecklistItem>().apply { addAll(checklist) } }
    var title by remember { mutableStateOf(note?.title ?: "") }
    var newChecklistText by remember { mutableStateOf("") }
    val protectedDraft = if (note?.isLocked == true) com.bookverse.contacts.ui.auth.encryptedDraft("checklist-draft-${note.id}", appSettingsViewModel) else null
    com.bookverse.contacts.ui.auth.BindProtectedDraft(
        draft = protectedDraft,
        snapshot = {
            buildMap {
                put("title", title)
                put("newItem", newChecklistText)
                put("count", checklistState.size.toString())
                checklistState.forEachIndexed { index, item ->
                    put("$index.id", item.id.toString())
                    put("$index.noteId", item.noteId.toString())
                    put("$index.text", item.text)
                    put("$index.checked", item.isChecked.toString())
                }
            }
        },
        restore = { fields ->
            title = fields["title"].orEmpty()
            newChecklistText = fields["newItem"].orEmpty()
            checklistState.clear()
            repeat((fields["count"]?.toIntOrNull() ?: 0).coerceIn(0, 10_000)) { index ->
                checklistState.add(ChecklistItem(
                    id = fields["$index.id"]?.toLongOrNull() ?: 0,
                    noteId = fields["$index.noteId"]?.toLongOrNull() ?: note?.id ?: 0,
                    text = fields["$index.text"].orEmpty(),
                    isChecked = fields["$index.checked"].toBoolean(),
                ))
            }
        },
    )
    val addItemFocusRequester = remember { FocusRequester() }
    val titleFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val notesViewModel: NotesViewModel = viewModel(
        factory = NotesViewModelFactory(context)
    )
    val noteFolders by notesViewModel.noteFolders.collectAsStateWithLifecycle()
    var selectedFolderId by rememberSaveable(note?.id) { mutableStateOf(note?.folderId) }
    var showFolderDialog by remember { mutableStateOf(false) }

    var showValidationDialog by remember { mutableStateOf(false) }
    var validationMessageRes by remember { mutableStateOf<Int?>(null) }
    val validationMessage = validationMessageRes?.let { stringResource(it) } ?: ""
    var searchActive by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    var isAddItemFieldActive by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }
    var showIncompleteOnly by remember { mutableStateOf(false) }
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }
    val filteredChecklist by remember(searchText, showIncompleteOnly) {
        derivedStateOf {
            val base = if (searchText.isBlank()) {
                checklistState.mapIndexed { index, item -> index to item }
            } else {
                checklistState.mapIndexedNotNull { index, item ->
                    if (item.text.contains(searchText, ignoreCase = true)) {
                        index to item
                    } else {
                        null
                    }
                }
            }
            if (showIncompleteOnly) base.filter { (_, item) -> !item.isChecked }
            else base
        }
    }

    fun hasChanges(): Boolean {
        if (note == null) return title.isNotEmpty() || checklistState.isNotEmpty()
        if (title != originalTitle) return true
        if (checklistState.size != originalChecklist.size) return true
        for (i in checklistState.indices) {
            val current = checklistState[i]
            val original = originalChecklist.getOrNull(i) ?: return true
            if (current.text != original.text || current.isChecked != original.isChecked) return true
        }
        return false
    }

    fun handleSave() {
        if (title.isBlank()) {
            validationMessageRes = R.string.checklist_validation_empty_title_message
            showValidationDialog = true
            return
        }

        if (checklistState.isEmpty()) {
            validationMessageRes = R.string.checklist_validation_empty_items_message
            showValidationDialog = true
            return
        }

        val now = System.currentTimeMillis()
        val updatedNote = note?.copy(
            title = title,
            content = "",
            timestamp = now,
            createdAt = note.createdAt.takeIf { it > 0L } ?: now,
            updatedAt = now,
            isChecklist = true,
            folderId = selectedFolderId
        ) ?: Note(
            title = title,
            content = "",
            isChecklist = true,
            folderId = selectedFolderId,
            createdAt = now,
            updatedAt = now
        )

        protectedDraft?.clear()
        onSave(updatedNote, checklistState.toList())
    }

    fun openItemEditor(index: Int) {
        editingItemIndex = index
    }

    fun handleBack() {
        if (searchActive) {
            searchActive = false
            searchText = ""
            focusManager.clearFocus(force = true)
            return
        }
        if (hasChanges()) {
            handleSave()
        } else {
            onCancel()
        }
    }

    LaunchedEffect(searchActive) {
        if (searchActive) {
            searchFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(checklistState.size, isAddItemFieldActive) {
        if (checklistState.isEmpty()) {
            titleFocusRequester.requestFocus()
        } else if (isAddItemFieldActive) {
            addItemFocusRequester.requestFocus()
        }
    }

    BackHandler {
        handleBack()
    }

    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior(expandable = false)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                title = if (note == null) stringResource(R.string.create_checklist) else stringResource(
                    R.string.edit_checklist
                ),
                onBackClick = { handleBack() },
                searchAction = if (checklistState.isNotEmpty() && note != null) com.bookverse.contacts.ui.components.AppBarSearchAction(
                    stringResource(R.string.search_checklist_items), { searchActive = true }, feedback = true,
                ) else null,
                actions = {
                    if (noteFolders.isNotEmpty() && note == null) {
                        IconAction(
                            imageVector = Icons.Default.Folder,
                            contentDescription = stringResource(R.string.note_folder_action_description),
                            onClick = { showFolderDialog = true }
                        )
                    }
                    IconAction(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(CoreUiR.string.more_options),
                        onClick = { showMoreOptions = true },
                        feedback = true
                    )
                },
                scrollBehavior = scrollBehavior,
                titleContent = if (searchActive) {
                    {
                        TopBarSearchField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            placeholder = stringResource(R.string.search_checklist_items),
                            focusRequester = searchFocusRequester,
                            onClose = {
                                searchText = ""
                                focusManager.clearFocus(force = true)
                                searchActive = false
                            },
                            leadingContentDescription = stringResource(CoreUiR.string.search),
                            clearContentDescription = stringResource(CoreUiR.string.clear),
                        )
                    }
                } else null
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                NoteTitle(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.focusRequester(titleFocusRequester),
                    accentColor = accentColor
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp, start = 12.dp, end = 12.dp)
            ) {
                if (searchText.isBlank() || filteredChecklist.isNotEmpty()) {
                    item {
                        DisposableEffect(Unit) {
                            isAddItemFieldActive = true
                            onDispose { isAddItemFieldActive = false }
                        }
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.imePadding()
                            ) {
                                OutlinedTextField(
                                    value = newChecklistText,
                                    onValueChange = { newChecklistText = it },
                                    placeholder = {
                                        Text(
                                            stringResource(R.string.add_item),
                                            color = Color.Gray
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(addItemFocusRequester),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        selectionColors = TextSelectionColors(
                                            handleColor = accentColor,
                                            backgroundColor = accentColor.copy(alpha = 0.4f)
                                        ),
                                        cursorColor = accentColor
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Sentences,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (newChecklistText.isNotBlank()) {
                                                checklistState.add(
                                                    ChecklistItem(
                                                        0,
                                                        note?.id ?: 0,
                                                        newChecklistText,
                                                        false
                                                    )
                                                )
                                                newChecklistText = ""
                                                addItemFocusRequester.requestFocus()
                                                keyboardController?.show()
                                            } else {
                                                addItemFocusRequester.requestFocus()
                                                keyboardController?.show()
                                            }
                                        }
                                    )
                                )

                                if (newChecklistText.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            checklistState.add(
                                                ChecklistItem(
                                                    0,
                                                    note?.id ?: 0,
                                                    newChecklistText,
                                                    false
                                                )
                                            )
                                            newChecklistText = ""
                                            addItemFocusRequester.requestFocus()
                                            keyboardController?.show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = stringResource(R.string.add_item),
                                            tint = textColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (searchText.isNotBlank() && filteredChecklist.isEmpty()) {
                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Lucide.SearchX,
                                        contentDescription = stringResource(R.string.checklist_no_results_icon),
                                        tint = accentColor,
                                        modifier = Modifier.size(40.dp)
                                    )
                                Text(
                                    text = stringResource(R.string.checklist_no_results),
                                    color = textColor,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = stringResource(R.string.checklist_no_results_subtitle),
                                    color = textColor.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                val trimmedQuery = searchText.trim()
                                Button(
                                    onClick = {
                                        if (trimmedQuery.isNotEmpty()) {
                                            checklistState.add(
                                                ChecklistItem(
                                                    id = 0,
                                                    noteId = note?.id ?: 0,
                                                    text = trimmedQuery,
                                                    isChecked = false
                                                )
                                            )
                                            searchText = trimmedQuery
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accentColor.copy(0.5f)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = stringResource(R.string.add_item),
                                        tint = textColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(
                                            R.string.checklist_add_search_item,
                                            trimmedQuery
                                        ),
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(
                        items = filteredChecklist,
                        key = { (originalIndex, item) ->
                            item.id.takeIf { it != 0L } ?: originalIndex
                        }
                    ) { (originalIndex, item) ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isChecked,
                                    onCheckedChange = { checked ->
                                        checklistState[originalIndex] =
                                            item.copy(isChecked = checked)
                                        VibrationUtils.vibrate(
                                            context, vibrationDuration, vibrationPattern
                                        )
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = accentColor,
                                        checkmarkColor = backgroundColor
                                    )
                                )
                                androidx.compose.runtime.CompositionLocalProvider(
                                    LocalTextSelectionColors provides TextSelectionColors(
                                        handleColor = accentColor,
                                        backgroundColor = accentColor.copy(alpha = 0.4f)
                                    )
                                ) {
                                    BasicTextField(
                                        value = item.text,
                                        onValueChange = { newText ->
                                            checklistState[originalIndex] =
                                                item.copy(text = newText)
                                        },
                                        cursorBrush = SolidColor(accentColor),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = if (item.isChecked) textColor.copy(alpha = 0.5f) else textColor,
                                            fontFamily = MaterialTheme.typography.headlineLarge.fontFamily,
                                            textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        decorationBox = { innerTextField ->
                                            Box(
                                                Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                innerTextField()
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Sentences
                                        )
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        openItemEditor(originalIndex)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(CoreUiR.string.edit),
                                        tint = textColor
                                    )
                                }
                                IconButton(onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                    checklistState.removeAt(originalIndex)
                                }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(CoreUiR.string.delete),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showFolderDialog && noteFolders.isNotEmpty()) {
            val folderOptions = listOf(FolderSelectionOption(null, stringResource(R.string.note_move_to_folder_none))) +
                noteFolders.map { FolderSelectionOption(it.id, it.name) }
            val selectedOption = folderOptions.firstOrNull { it.id == selectedFolderId } ?: folderOptions.first()
            SelectBottomSheet(
                onDismiss = { showFolderDialog = false },
                title = stringResource(R.string.note_folder_picker_title),
                options = folderOptions,
                selectedOption = selectedOption,
                onSelect = { option -> selectedFolderId = option.id },
                optionKey = { it.id ?: -1L },
                optionLabel = { it.label },
            )
        }

        if (showValidationDialog) {
            MessageDialog(
                title = stringResource(R.string.reminder_validation_error_title),
                message1 = validationMessage,
                buttonText = stringResource(CoreUiR.string.understood),
                onButtonClick = { showValidationDialog = false },
                onDismiss = { showValidationDialog = false },
            )
        }

        val itemIndex = editingItemIndex
        val editingItem = itemIndex?.let { checklistState.getOrNull(it) }
        if (itemIndex != null && editingItem != null) {
            val itemContentLabel = stringResource(R.string.checklist_item_content)
            ChecklistItemEditSheet(
                item = editingItem,
                accentColor = accentColor,
                accentBarStyle = accentBarStyle,
                onTextChange = { newText ->
                    checklistState[itemIndex] = checklistState[itemIndex].copy(text = newText)
                },
                onToggleChecked = {
                    val currentItem = checklistState[itemIndex]
                    checklistState[itemIndex] = currentItem.copy(
                        isChecked = !currentItem.isChecked
                    )
                    VibrationUtils.vibrate(
                        context, vibrationDuration, vibrationPattern
                    )
                },
                onCopy = {
                    ClipboardUtils.copyToClipboard(
                        context = context,
                        text = checklistState[itemIndex].text,
                        label = itemContentLabel
                    )
                },
                onDismiss = { editingItemIndex = null }
            )
        }

        if (showMoreOptions) {
            OptionsDialog(
                title = stringResource(CoreUiR.string.more_options),
                options = listOf(
                    OptionsDialogItem(
                        label = stringResource(CoreUiR.string.save),
                        icon = Icons.Default.Check,
                        onClick = { showMoreOptions = false; handleSave() }
                    ),
                    OptionsDialogItem(
                        label = stringResource(
                            if (showIncompleteOnly) R.string.show_all
                            else R.string.show_incomplete_only
                        ),
                        icon = if (showIncompleteOnly) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                        onClick = {
                            showMoreOptions = false
                            showIncompleteOnly = !showIncompleteOnly
                        }
                    ),
                    OptionsDialogItem(
                        label = stringResource(R.string.note_close_without_saving),
                        icon = Icons.Default.Close,
                        onClick = onCancel
                    )
                ),
                onDismiss = { showMoreOptions = false },
            )
        }
    }
}
