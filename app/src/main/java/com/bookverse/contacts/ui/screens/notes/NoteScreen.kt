package com.bookverse.contacts.ui.screens.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.R
import com.bookverse.contacts.data.notes.Note
import com.bookverse.contacts.integration.NotesViewModelFactory
import com.bookverse.contacts.ui.components.AppSnackbarHost
import com.bookverse.contacts.ui.components.ColorPickerDialog
import com.bookverse.contacts.ui.components.MessageDialog
import com.bookverse.contacts.ui.components.OptionsDialog
import com.bookverse.contacts.ui.components.OptionsDialogItem
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.SelectBottomSheet
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.ui.components.rememberAppSnackbarState
import com.bookverse.contacts.ui.components.TextField
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.NotesViewModel
import com.composables.icons.lucide.Code
import com.composables.icons.lucide.Lucide
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichText
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(
    note: Note?,
    onSave: (Note) -> Unit,
    onCancel: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel = viewModel()
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val editorState = if (note?.isLocked == true) remember { com.mohamedrejeb.richeditor.model.RichTextState() } else rememberRichTextState()
    val protectedDraft = if (note?.isLocked == true) com.bookverse.contacts.ui.auth.encryptedDraft("note-draft-${note.id}", appSettingsViewModel) else null
    val draftFields = protectedDraft?.fields?.collectAsStateWithLifecycle()?.value
    if (protectedDraft != null) com.bookverse.contacts.ui.auth.ReportDraftFailure(protectedDraft)
    var restoredDraft by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val accentColor = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val resources = LocalResources.current
    val notesViewModel: NotesViewModel = viewModel(
        factory = NotesViewModelFactory(context)
    )
    val noteFolders by notesViewModel.noteFolders.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val baselineHtmlState = remember { mutableStateOf<String?>(null) }
    var isEditing by rememberSaveable { mutableStateOf(note == null) }
    var selectedFolderId by rememberSaveable(note?.id) { mutableStateOf(note?.folderId) }
    var showFolderDialog by remember { mutableStateOf(false) }

    val isBold = editorState.currentSpanStyle.fontWeight == FontWeight.Bold
    val isItalic = editorState.currentSpanStyle.fontStyle == FontStyle.Italic
    val isUnderline = editorState.currentSpanStyle.textDecoration == TextDecoration.Underline
    val isRed = editorState.currentSpanStyle.color == Color.Red
    val isAlignLeft = editorState.currentParagraphStyle.textAlign == TextAlign.Start
    val isAlignCenter = editorState.currentParagraphStyle.textAlign == TextAlign.Center
    val isAlignRight = editorState.currentParagraphStyle.textAlign == TextAlign.End
    val isLink = editorState.isLink

    val normalHeadingLabel = stringResource(R.string.normal)
    val headingOptions = remember(normalHeadingLabel) {
        listOf(
            HeadingOption(
                key = HEADING_OPTION_NORMAL,
                    label = normalHeadingLabel,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal
            ),
            HeadingOption(key = "h1", label = "h1", fontSize = 36.sp, fontWeight = FontWeight.Bold),
            HeadingOption(key = "h2", label = "h2", fontSize = 33.sp, fontWeight = FontWeight.Bold),
            HeadingOption(key = "h3", label = "h3", fontSize = 30.sp, fontWeight = FontWeight.Bold),
            HeadingOption(key = "h4", label = "h4", fontSize = 28.sp, fontWeight = FontWeight.Bold),
            HeadingOption(key = "h5", label = "h5", fontSize = 26.sp, fontWeight = FontWeight.Bold),
            HeadingOption(key = "h6", label = "h6", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        )
    }
    val currentFontSize = editorState.currentSpanStyle.fontSize
    val currentFontWeight = editorState.currentSpanStyle.fontWeight ?: FontWeight.Normal
    val selectedHeading = headingOptions.firstOrNull {
        it.matches(currentFontSize, currentFontWeight)
    } ?: headingOptions.first()
    val isHeadingActive = selectedHeading.key != HEADING_OPTION_NORMAL

    var showColorDialog by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkUrl by remember { mutableStateOf("") }

    var showValidationDialog by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf("") }
    var showMarkdownPreview by rememberSaveable { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }
    val snackbar = rememberAppSnackbarState()
    val imagePicker = LocalNoteImagePicker.current ?: rememberNoteImagePicker()
    val imageRequestKey = rememberSaveable(note?.id) {
        "note-${note?.id ?: java.util.UUID.randomUUID()}"
    }
    var imageFileNames by rememberSaveable(note?.id) { mutableStateOf(listOf<String>()) }
    var savedImageFileNames by rememberSaveable(note?.id) { mutableStateOf(listOf<String>()) }
    var imagesLoaded by rememberSaveable(note?.id) { mutableStateOf(note == null) }
    var importingImages by remember { mutableStateOf(false) }
    var openedImage by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(note?.id, imagesLoaded) {
        val noteId = note?.id
        if (!imagesLoaded && noteId != null) {
            val stored = notesViewModel.getNoteImageFileNames(noteId)
            savedImageFileNames = stored
            imageFileNames = stored
            imagesLoaded = true
        }
    }

    LaunchedEffect(imagePicker.pendingRequestKey, imagePicker.pendingUris, imagesLoaded) {
        val uris = imagePicker.pendingUris
        if (uris.isEmpty() || !imagesLoaded) return@LaunchedEffect
        if (imagePicker.pendingRequestKey != imageRequestKey) {
            imagePicker.consume()
            return@LaunchedEffect
        }
        importingImages = true
        try {
            val imported = notesViewModel.importNoteImages(uris)
            imageFileNames = imageFileNames + imported
            imagePicker.consume()
            if (imported.size < uris.size) {
                snackbar.show(
                    message = resources.getString(R.string.note_image_import_error),
                    type = SnackbarType.Error,
                )
            }
        } finally {
            importingImages = false
        }
    }

    fun unsavedImageFileNames(): List<String> =
        imageFileNames.filter { it !in savedImageFileNames }

    fun removeImage(fileName: String) {
        imageFileNames = imageFileNames - fileName
        if (fileName !in savedImageFileNames) {
            notesViewModel.discardUnsavedNoteImages(listOf(fileName))
        }
    }

    fun cancelEditing() {
        notesViewModel.discardUnsavedNoteImages(unsavedImageFileNames())
        onCancel()
    }
    val markdownPreviewState = if (note?.isLocked == true) remember { com.mohamedrejeb.richeditor.model.RichTextState() } else rememberRichTextState()

    fun normalizeHtml(html: String): String = html.trim().replace(Regex("\\s+"), " ")

    fun hasChanges(): Boolean {
        val baseline = baselineHtmlState.value
        val currentPlain = editorState.toText().trim()
        val imagesChanged = imagesLoaded && imageFileNames != savedImageFileNames
        if (note == null) return currentPlain.isNotEmpty() || imagesChanged
        if (baseline == null) return false
        val current = normalizeHtml(editorState.toHtml())
        return current != baseline || imagesChanged
    }

    fun performSave() {
        val contentPlain = editorState.toText().trim()
        if (contentPlain.isEmpty()) {
            validationMessage = resources.getString(R.string.note_validation_empty_content_message)
            showValidationDialog = true
            return
        }
        val trimmedTitle = if (contentPlain.length > 30) contentPlain.take(30) else contentPlain
        val now = System.currentTimeMillis()
        val updatedNote = note?.copy(
            title = trimmedTitle,
            content = editorState.toHtml(),
            timestamp = now,
            createdAt = note.createdAt.takeIf { it > 0L } ?: now,
            updatedAt = now,
            folderId = selectedFolderId
        ) ?: Note(
            title = trimmedTitle,
            content = editorState.toHtml(),
            isChecklist = false,
            folderId = selectedFolderId,
            createdAt = now,
            updatedAt = now
        )
        protectedDraft?.clear()
        if (imagesLoaded) {
            if (updatedNote.id == 0L) {
                notesViewModel.attachImagesToNextNewNote(imageFileNames)
            } else if (imageFileNames != savedImageFileNames) {
                notesViewModel.saveNoteImages(updatedNote.id, imageFileNames)
            }
        }
        onSave(updatedNote)
    }

    fun handleBack() {
        if (hasChanges()) {
            performSave()
        } else {
            cancelEditing()
        }
    }

    fun toggleMarkdownPreview() {
        showMarkdownPreview = !showMarkdownPreview
        if (showMarkdownPreview) {
            markdownPreviewState.setMarkdown(editorState.toText())
        }
    }

    LaunchedEffect(note?.id, draftFields != null) {
        if (protectedDraft != null && draftFields == null) return@LaunchedEffect
        val html = draftFields?.get("html") ?: note?.content.orEmpty()
        editorState.setHtml(html)
        baselineHtmlState.value = normalizeHtml(note?.content.orEmpty())
        protectedDraft?.initialize(mapOf("html" to html))
        restoredDraft = true
    }
    LaunchedEffect(protectedDraft, restoredDraft) {
        if (protectedDraft != null && restoredDraft) {
            androidx.compose.runtime.snapshotFlow { editorState.toHtml() }.collect { html ->
                protectedDraft.update(mapOf("html" to html))
            }
        }
    }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            showMarkdownPreview = false
            focusRequester.requestFocus()
            editorState.selection = TextRange(editorState.annotatedString.length)
            keyboardController?.show()
        } else {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            showColorDialog = false
            showLinkDialog = false
        }
    }

    BackHandler {
        handleBack()
    }

    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior(expandable = false)

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { AppSnackbarHost(snackbar.hostState) },
        topBar = {
            SecondaryTopBar(
                title = if (note != null) stringResource(R.string.edit_note) else stringResource(R.string.create_note),
                onBackClick = { handleBack() },
                appSettingsViewModel = appSettingsViewModel,
                actions = {
                    if (note != null && isEditing) {
                        IconAction(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(R.string.lock),
                            onClick = { isEditing = false }
                        )
                    }
                    if (note != null && !isEditing) {
                        IconAction(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = stringResource(R.string.unlock),
                            onClick = { isEditing = true }
                        )
                    }
                    IconAction(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.more_options),
                        onClick = { showMoreOptions = true },
                        feedback = true
                    )
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding)
        ) {
            if (isEditing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val controlsEnabled = isEditing
                    IconButton(
                        onClick = { editorState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
                        enabled = controlsEnabled,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isBold) accentColor.copy(
                                alpha = 0.2f
                            ) else Color.Transparent
                        )
                    ) {
                        Icon(
                            Icons.Filled.FormatBold,
                            contentDescription = stringResource(R.string.note_format_bold),
                            tint = if (isBold) accentColor else textColor
                        )
                    }
                    IconButton(
                        onClick = { editorState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
                        enabled = controlsEnabled,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isItalic) accentColor.copy(
                                alpha = 0.2f
                            ) else Color.Transparent
                        )
                    ) {
                        Icon(
                            Icons.Filled.FormatItalic,
                            contentDescription = stringResource(R.string.note_format_italic),
                            tint = if (isItalic) accentColor else textColor
                        )
                    }
                    IconButton(
                        onClick = { editorState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) },
                        enabled = controlsEnabled,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isUnderline) accentColor.copy(
                                alpha = 0.2f
                            ) else Color.Transparent
                        )
                    ) {
                        Icon(
                            Icons.Filled.FormatUnderlined,
                            contentDescription = stringResource(R.string.note_format_underline),
                            tint = if (isUnderline) accentColor else textColor
                        )
                    }
                    var showTitleMenu by remember { mutableStateOf(false) }
                    if (!controlsEnabled && showTitleMenu) {
                        showTitleMenu = false
                    }
                    Box {
                        IconButton(
                            onClick = { showTitleMenu = true },
                            enabled = controlsEnabled,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isHeadingActive) accentColor.copy(alpha = 0.2f) else Color.Transparent
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FormatSize,
                                contentDescription = stringResource(R.string.note_format_heading),
                                tint = if (isHeadingActive) accentColor else textColor
                            )
                        }
                        DropdownMenu(
                            expanded = showTitleMenu,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface),
                            onDismissRequest = { showTitleMenu = false }) {
                            headingOptions.forEach { option ->
                                val isSelected = option.matches(currentFontSize, currentFontWeight)
                                DropdownMenuItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) accentColor.copy(alpha = 0.15f)
                                            else Color.Transparent
                                        ),
                                    text = {
                                        Text(
                                            text = option.label,
                                            fontSize = option.fontSize,
                                            fontWeight = option.fontWeight,
                                            color = if (isSelected) accentColor else textColor
                                        )
                                    },
                                    trailingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = stringResource(R.string.selected_single),
                                                tint = accentColor
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        editorState.toggleSpanStyle(
                                            SpanStyle(
                                                fontSize = option.fontSize,
                                                fontWeight = option.fontWeight
                                            )
                                        )
                                        showTitleMenu = false
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = if (isSelected) accentColor else textColor
                                    )
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { showColorDialog = true },
                        enabled = controlsEnabled,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isRed) accentColor.copy(alpha = 0.2f) else Color.Transparent
                        )
                    ) {
                        Box {
                            Icon(
                                imageVector = Icons.Filled.ColorLens,
                                contentDescription = stringResource(R.string.note_format_color),
                                tint = textColor
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(selectedColor)
                                    .align(Alignment.BottomEnd)
                            )
                        }
                    }
                    IconButton(
                        onClick = { editorState.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.Start)) },
                        enabled = controlsEnabled,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isAlignLeft) accentColor.copy(
                                alpha = 0.2f
                            ) else Color.Transparent
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.FormatAlignLeft,
                            contentDescription = stringResource(R.string.note_format_align_left),
                            tint = if (isAlignLeft) accentColor else textColor
                        )
                    }
                    IconButton(
                        onClick = { editorState.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.Center)) },
                        enabled = controlsEnabled,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isAlignCenter) accentColor.copy(
                                alpha = 0.2f
                            ) else Color.Transparent
                        )
                    ) {
                        Icon(
                            Icons.Filled.FormatAlignCenter,
                            contentDescription = stringResource(R.string.note_format_align_center),
                            tint = if (isAlignCenter) accentColor else textColor
                        )
                    }
                    IconButton(
                        onClick = { editorState.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.End)) },
                        enabled = controlsEnabled,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isAlignRight) accentColor.copy(
                                alpha = 0.2f
                            ) else Color.Transparent
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.FormatAlignRight,
                            contentDescription = stringResource(R.string.note_format_align_right),
                            tint = if (isAlignRight) accentColor else textColor
                        )
                    }
                    IconButton(
                        onClick = { editorState.toggleOrderedList() },
                        enabled = controlsEnabled
                    ) {
                        Icon(
                            Icons.Filled.FormatListNumbered,
                            contentDescription = stringResource(R.string.note_format_ordered_list)
                        )
                    }
                    IconButton(
                        onClick = { editorState.toggleUnorderedList() },
                        enabled = controlsEnabled
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.FormatListBulleted,
                            contentDescription = stringResource(R.string.note_format_unordered_list)
                        )
                    }
                    IconButton(
                        onClick = { imagePicker.launch(imageRequestKey) },
                        enabled = controlsEnabled && imagesLoaded && !importingImages,
                    ) {
                        Icon(
                            Icons.Filled.AddPhotoAlternate,
                            contentDescription = stringResource(R.string.note_image_add),
                            tint = textColor
                        )
                    }
                    IconButton(
                        onClick = { showLinkDialog = true },
                        enabled = controlsEnabled,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isLink) accentColor.copy(
                                alpha = 0.2f
                            ) else Color.Transparent
                        )
                    ) {
                        Icon(
                            Icons.Filled.Link,
                            contentDescription = stringResource(R.string.note_format_link),
                            tint = if (isLink) accentColor else textColor
                        )
                    }
                }
            }
            if (showColorDialog && isEditing) {
                ColorPickerDialog(
                    initialColor = selectedColor,
                    onColorSelected = {
                        selectedColor = it; editorState.toggleSpanStyle(
                        SpanStyle(
                            color = it
                        )
                    ); showColorDialog = false
                    },
                    onDismiss = { showColorDialog = false },
                    appSettingsViewModel = appSettingsViewModel
                )
            }
            if (showLinkDialog && isEditing) {
                AlertDialog(
                    onDismissRequest = { showLinkDialog = false },
                    title = { Text(stringResource(R.string.dialog_link_title)) },
                    text = {
                        TextField(
                            value = linkUrl,
                            onValueChange = { linkUrl = it },
                            label = stringResource(R.string.dialog_link_label),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (linkUrl.isNotBlank()) {
                                    if (isLink) editorState.updateLink(url = linkUrl) else editorState.addLinkToSelection(
                                        url = linkUrl
                                    )
                                }
                                showLinkDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = backgroundColor
                            ),
                            shape = RoundedCornerShape(999.dp)
                        ) { Text(stringResource(R.string.apply), color = textColor) }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            if (isLink) editorState.removeLink(); showLinkDialog = false
                        }) {
                            Text(stringResource(R.string.dialog_link_remove), color = accentColor)
                        }
                    }
                )
            }
            if (showValidationDialog) {
                MessageDialog(
                    title = stringResource(R.string.reminder_validation_error_title),
                    message1 = validationMessage,
                    buttonText = stringResource(R.string.understood),
                    onButtonClick = { showValidationDialog = false },
                    onDismiss = { showValidationDialog = false },
                )
            }
            NoteImagesStrip(
                fileNames = imageFileNames,
                editable = isEditing,
                importing = importingImages,
                onOpen = { openedImage = it },
                onRemove = { removeImage(it) },
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp)
                    .imePadding()
                    .then(
                        if (!isEditing) {
                            Modifier.pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(
                                        requireUnconsumed = false,
                                        pass = PointerEventPass.Initial
                                    )
                                    if (waitForUpOrCancellation(pass = PointerEventPass.Initial) == null) {
                                        return@awaitEachGesture
                                    }

                                    val secondDown = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
                                        awaitFirstDown(
                                            requireUnconsumed = false,
                                            pass = PointerEventPass.Initial
                                        )
                                    } ?: return@awaitEachGesture

                                    secondDown.consume()
                                    val secondUp = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                                    if (secondUp != null) {
                                        secondUp.consume()
                                        isEditing = true
                                    }
                                }
                            }
                        } else {
                            Modifier
                        }
                    )
            ) {
                val richTextStyle = TextStyle(
                    fontSize = 18.sp,
                    color = textColor,
                    fontFamily = MaterialTheme.typography.headlineLarge.fontFamily
                )
                if (isEditing) {
                    RichTextEditor(
                        state = editorState,
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(focusRequester),
                        textStyle = richTextStyle,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            autoCorrectEnabled = true,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Default
                        )
                    )
                } else if (showMarkdownPreview) {
                    SelectionContainer {
                        BasicRichText(
                            state = markdownPreviewState,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            style = richTextStyle
                        )
                    }
                } else {
                    SelectionContainer {
                        BasicRichText(
                            state = editorState,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            style = richTextStyle
                        )
                    }
                }
            }
        }
    }

    openedImage?.let { fileName ->
        NoteImageViewer(fileName = fileName, onDismiss = { openedImage = null })
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

    if (showMoreOptions) {
        val options = listOfNotNull(
            if (noteFolders.isNotEmpty() && note == null) {
                OptionsDialogItem(
                    label = stringResource(R.string.note_folder_action_description),
                    icon = Icons.Default.Folder,
                    onClick = { showFolderDialog = true }
                )
            } else {
                null
            },
            if (note != null && !isEditing) {
                OptionsDialogItem(
                    label = stringResource(R.string.note_markdown_preview),
                    icon = Lucide.Code,
                    selected = showMarkdownPreview,
                    onClick = { toggleMarkdownPreview() }
                )
            } else {
                null
            },
            if (isEditing && imagesLoaded && !importingImages) {
                OptionsDialogItem(
                    label = stringResource(R.string.note_image_add),
                    icon = Icons.Filled.AddPhotoAlternate,
                    onClick = { imagePicker.launch(imageRequestKey) }
                )
            } else {
                null
            },
            if (isEditing) {
                OptionsDialogItem(
                    label = stringResource(R.string.save),
                    icon = Icons.Default.Check,
                    onClick = { performSave() }
                )
            } else {
                null
            },
            if (isEditing) {
                OptionsDialogItem(
                    label = stringResource(R.string.note_close_without_saving),
                    icon = Icons.Default.Close,
                    onClick = { cancelEditing() }
                )
            } else {
                null
            }
        )
        OptionsDialog(
            title = stringResource(R.string.more_options),
            options = options,
            onDismiss = { showMoreOptions = false },
        )
    }
}

private const val HEADING_OPTION_NORMAL = "normal"

private data class HeadingOption(
    val key: String,
    val label: String,
    val fontSize: TextUnit,
    val fontWeight: FontWeight
)

private fun HeadingOption.matches(fontSize: TextUnit, fontWeight: FontWeight): Boolean {
    return if (key == HEADING_OPTION_NORMAL) {
        fontSize.isUnspecified || (fontSize == this.fontSize && fontWeight == this.fontWeight)
    } else {
        fontSize == this.fontSize && fontWeight == this.fontWeight
    }
}
