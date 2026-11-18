package com.bookverse.contacts.ui.screens.notes

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.bookverse.contacts.feature.notes.R
import com.bookverse.contacts.core.ui.R as CoreUiR
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import com.bookverse.contacts.ui.components.withoutTop
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.data.notes.ChecklistItem
import com.bookverse.contacts.data.notes.NOTE_FOLDER_FILTER_ALL
import com.bookverse.contacts.data.notes.Note
import com.bookverse.contacts.integration.NotesViewModelFactory
import com.bookverse.contacts.data.notes.NoteFolder
import com.bookverse.contacts.data.notes.NoteTag
import com.bookverse.contacts.data.notes.RecordingData
import com.bookverse.contacts.data.notes.ReminderData
import com.bookverse.contacts.data.notes.ReminderType
import com.bookverse.contacts.data.notes.effectiveRepeat
import com.bookverse.contacts.data.notes.nextOccurrence
import com.bookverse.contacts.data.notes.repeats
import com.bookverse.contacts.data.notes.searchNotes
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.ui.auth.PasswordAuthDialogs
import com.bookverse.contacts.ui.auth.rememberPasswordAuthState
import com.bookverse.contacts.ui.components.AppSnackbarHost
import com.bookverse.contacts.ui.components.ConfirmDialog
import com.bookverse.contacts.ui.components.FastScroller
import com.bookverse.contacts.ui.components.FloatingOptionItem
import com.bookverse.contacts.ui.components.FloatingOptions
import com.bookverse.contacts.ui.components.NoteItem
import com.bookverse.contacts.ui.components.OptionsDialog
import com.bookverse.contacts.ui.components.OptionsDialogItem
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.SelectBottomSheet
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.ui.components.TagFilterBar
import com.bookverse.contacts.ui.components.TopBarSearchField
import com.bookverse.contacts.ui.components.floatingOptionsBlur
import com.bookverse.contacts.ui.components.rememberAppSnackbarState
import com.bookverse.contacts.ui.components.rememberFloatingOptionsState
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.NotesViewModel
import com.bookverse.contacts.utils.NoteShareUtils
import com.composables.icons.lucide.ListCheck
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mic
import com.composables.icons.lucide.NotebookPen
import com.composables.icons.lucide.Tags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    onBackClick: () -> Unit,
    onEditNoteClick: (Note, List<ChecklistItem>, String) -> Unit,
    onEditReminderClick: (Note, ReminderData?) -> Unit,
    onEditRecordingClick: (Note, RecordingData?) -> Unit,
    onCreateNoteClick: (String) -> Unit,
    onNavigateToRecycleBin: () -> Unit,
    onManageFolders: () -> Unit,
    onTransformToAudio: ((String) -> Unit)? = null,
    notesViewModel: NotesViewModel = viewModel(
        factory = NotesViewModelFactory(LocalContext.current)
    ),
    appSettingsViewModel: AppSettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val notes by notesViewModel.notes.collectAsStateWithLifecycle()
    val noteFolders by notesViewModel.noteFolders.collectAsStateWithLifecycle()
    val checklistsByNoteId by notesViewModel.checklistsByNoteId.collectAsStateWithLifecycle()
    val remindersByNoteId by notesViewModel.remindersByNoteId.collectAsStateWithLifecycle()
    val recordingsByNoteId by notesViewModel.recordingsByNoteId.collectAsStateWithLifecycle()
    val noteTags by notesViewModel.noteTags.collectAsStateWithLifecycle()
    val tagIdsByNoteId by notesViewModel.tagIdsByNoteId.collectAsStateWithLifecycle()
    val selectedTagFilter by notesViewModel.selectedTagFilter.collectAsStateWithLifecycle()
    val sharedTags = remember(noteTags) { noteTags.map { it.asSharedTag() } }
    val tagFilterActive = selectedTagFilter.isNotEmpty()
    var noteForTags by remember { mutableStateOf<Note?>(null) }
    var showTagManager by remember { mutableStateOf(false) }

    fun matchesTagFilter(noteId: Long): Boolean =
        selectedTagFilter.isEmpty() ||
            tagIdsByNoteId[noteId]?.any { it in selectedTagFilter } == true
    val folderOrder by appSettingsViewModel.noteFoldersOrder.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    val accentColor = MaterialTheme.colorScheme.primary
    val accentBarStyle by appSettingsViewModel.accentBarStyle.collectAsStateWithLifecycle()
    val listDensity by appSettingsViewModel.listDensity.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackbar = rememberAppSnackbarState()
    val floatingOptionsState = rememberFloatingOptionsState()
    val blurRadius = floatingOptionsBlur(floatingOptionsState)
    val shareNoteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}
    var pendingSavePayload by remember { mutableStateOf<NoteShareUtils.NoteSharePayload?>(null) }
    val saveDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val payload = pendingSavePayload
        if (result.resultCode == Activity.RESULT_OK && payload != null) {
            val uri = result.data?.data
            if (uri != null) {
                coroutineScope.launch {
                    val writeResult = runCatching {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(uri, "w")
                                ?.use { output -> output.write(payload.bytes) }
                                ?: throw IOException("Unable to open output stream")
                        }
                    }
                    if (writeResult.isSuccess) {
                        snackbar.show(
                            message = resources.getString(R.string.notes_save_success),
                            type = SnackbarType.Success
                        )
                    } else {
                        snackbar.show(
                            message = resources.getString(R.string.notes_save_error),
                            type = SnackbarType.Error
                        )
                    }
                }
            }
        }
        pendingSavePayload = null
    }
    var backupInProgress by remember { mutableStateOf(false) }
    val exportDateFormatter = remember { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT) }
    val reminderOccurrenceFormatter = remember(context) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", LocaleController.effectiveLocale(context))
    }
    val importMimeTypes = remember {
        arrayOf(
            "application/zip",
            "application/octet-stream",
            "application/json",
            "text/html",
            "application/xhtml+xml",
            "text/plain"
        )
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                backupInProgress = true
                floatingOptionsState.collapse()
                val result = notesViewModel.exportNotes(uri)
                backupInProgress = false
                result.fold(onSuccess = { summary ->
                    val noteCount = resources.getQuantityString(
                        R.plurals.notes_backup_note_count,
                        summary.notes,
                        summary.notes,
                    )
                    val checklistCount = resources.getQuantityString(
                        R.plurals.notes_backup_checklist_count,
                        summary.checklists,
                        summary.checklists,
                    )
                    snackbar.show(
                        message = resources.getString(
                            R.string.notes_export_success,
                            noteCount,
                            checklistCount,
                        ), type = SnackbarType.Success
                    )
                }, onFailure = {
                    DiagnosticLog.error(DiagnosticEvent.NOTE_EXPORT_FAILED)
                    snackbar.show(
                        message = resources.getString(R.string.notes_export_error),
                        type = SnackbarType.Error
                    )
                })
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                backupInProgress = true
                floatingOptionsState.collapse()
                val result = notesViewModel.importNotes(uri)
                backupInProgress = false
                result.fold(onSuccess = { summary ->
                    val noteCount = resources.getQuantityString(
                        R.plurals.notes_backup_note_count,
                        summary.notes,
                        summary.notes,
                    )
                    val checklistCount = resources.getQuantityString(
                        R.plurals.notes_backup_checklist_count,
                        summary.checklists,
                        summary.checklists,
                    )
                    snackbar.show(
                        message = resources.getString(
                            R.string.notes_import_success,
                            noteCount,
                            checklistCount,
                        ), type = SnackbarType.Success
                    )
                }, onFailure = { error ->
                    DiagnosticLog.error(DiagnosticEvent.NOTE_IMPORT_FAILED)
                    val messageRes = if (error is IllegalArgumentException) {
                        R.string.notes_import_error_invalid
                    } else {
                        R.string.notes_import_error
                    }
                    snackbar.show(
                        message = resources.getString(messageRes), type = SnackbarType.Error
                    )
                })
            }
        }
    }

    var searchActive by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var showNotesDialog by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showCreateOptionsDialog by remember { mutableStateOf(false) }
    var selectedFolderId by rememberSaveable {
        mutableLongStateOf(notesViewModel.lastOpenedFolderId)
    }
    var noteToMove by remember { mutableStateOf<Note?>(null) }
    var folderTargetForQuickAdd by remember { mutableStateOf<NoteFolder?>(null) }
    val passwordAuthState = rememberPasswordAuthState(appSettingsViewModel)
    val passwordAuthMethod by appSettingsViewModel.passwordAuthMethod.collectAsStateWithLifecycle()
    val passwordManagerEnabled by appSettingsViewModel.passwordManagerEnabled.collectAsStateWithLifecycle()

    val hasExportableNotes by remember(notes) {
        derivedStateOf { notes.any { !it.isReminder && !it.isRecording } }
    }
    val requiresAuthForExport by remember(notes) {
        derivedStateOf { notes.any { it.isLocked && !it.isReminder && !it.isRecording } }
    }

    fun openExportPicker() {
        val fileName = "notes_backup_${exportDateFormatter.format(Date())}.zip"
        exportLauncher.launch(fileName)
    }

    fun launchSaveRequest(payload: NoteShareUtils.NoteSharePayload) {
        pendingSavePayload = payload
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = payload.mimeType
            putExtra(Intent.EXTRA_TITLE, payload.fileName)
        }
        saveDocumentLauncher.launch(intent)
    }

    fun handleImportRequest() {
        if (backupInProgress) return
        floatingOptionsState.collapse()
        importLauncher.launch(importMimeTypes)
    }

    fun showTagNameConflict() {
        Toast.makeText(
            context,
            resources.getString(R.string.note_tag_name_exists),
            Toast.LENGTH_SHORT,
        ).show()
    }

    fun showDeleteConfirmation(target: Note) {
        noteToDelete = target
        showDeleteDialog = true
    }

    fun requestAuth(
        onSuccess: () -> Unit, onRequireSetup: (() -> Unit)? = {
            snackbar.show(
                message = resources.getString(R.string.note_lock_setup_required),
                type = SnackbarType.Info
            )
        }
    ) {
        if (!passwordManagerEnabled) {
            onRequireSetup?.invoke()
            passwordAuthState.startSetupFlow(onConfigured = onSuccess)
            return
        }

        passwordAuthState.handleProtectedAction(
            passwordManagerEnabled = passwordManagerEnabled,
            authMethod = passwordAuthMethod,
            onSuccess = onSuccess
        )
    }

    fun handleExportRequest() {
        if (backupInProgress) return
        if (!hasExportableNotes) {
            snackbar.show(
                message = resources.getString(R.string.notes_export_empty), type = SnackbarType.Info
            )
            floatingOptionsState.collapse()
            return
        }
        val proceed = {
            floatingOptionsState.collapse()
            openExportPicker()
        }
        if (requiresAuthForExport) {
            requestAuth(onSuccess = proceed)
        } else {
            proceed()
        }
    }

    fun requestDelete(note: Note) {
        if (note.isLocked) {
            requestAuth(onSuccess = { showDeleteConfirmation(note) })
        } else {
            showDeleteConfirmation(note)
        }
    }

    fun lockPromptRes(note: Note): Int = when {
        note.isReminder -> R.string.note_lock_open_prompt_reminder
        note.isRecording -> R.string.note_lock_open_prompt_recording
        note.isChecklist -> R.string.note_lock_open_prompt_checklist
        else -> R.string.note_lock_open_prompt_note
    }

    fun lockMessageRes(note: Note, shouldLock: Boolean): Int = when {
        note.isReminder -> if (shouldLock) R.string.note_lock_enabled_reminder else R.string.note_lock_disabled_reminder
        note.isRecording -> if (shouldLock) R.string.note_lock_enabled_recording else R.string.note_lock_disabled_recording
        note.isChecklist -> if (shouldLock) R.string.note_lock_enabled_checklist else R.string.note_lock_disabled_checklist
        else -> if (shouldLock) R.string.note_lock_enabled_note else R.string.note_lock_disabled_note
    }

    fun noteTagsFor(noteId: Long): List<NoteTag> {
        val ids = tagIdsByNoteId[noteId] ?: return emptyList()
        return noteTags.filter { it.id in ids }
    }

    LaunchedEffect(searchActive) { if (searchActive) focusRequester.requestFocus() }
    LaunchedEffect(notes.isEmpty()) {
        if (notes.isEmpty()) {
            floatingOptionsState.collapse()
        }
    }

    LaunchedEffect(selectedFolderId) {
        notesViewModel.setLastOpenedFolder(selectedFolderId)
    }
    LaunchedEffect(noteFolders, selectedFolderId) {
        if (selectedFolderId != NOTE_FOLDER_FILTER_ALL && noteFolders.none { it.id == selectedFolderId }) {
            selectedFolderId = NOTE_FOLDER_FILTER_ALL
            notesViewModel.setLastOpenedFolder(NOTE_FOLDER_FILTER_ALL)
        }
    }

    val searchIndex by notesViewModel.noteSearchIndex.collectAsStateWithLifecycle()
    val globalSearchActive = searchActive && search.isNotBlank()
    val globalResults = remember(
        notes,
        search,
        searchActive,
        searchIndex,
        selectedTagFilter,
        tagIdsByNoteId,
    ) {
        if (!searchActive || search.isBlank()) {
            emptyList()
        } else {
            searchNotes(notes, search, searchIndex).filter { matchesTagFilter(it.id) }
        }
    }
    val folderNamesById = remember(noteFolders) { noteFolders.associate { it.id to it.name } }
    val filtered = remember(
        notes,
        globalSearchActive,
        globalResults,
        selectedFolderId,
        selectedTagFilter,
        tagIdsByNoteId,
    ) {
        val folderFiltered = notes.filter { note ->
            val inFolder = if (selectedFolderId == NOTE_FOLDER_FILTER_ALL) {
                note.folderId == null
            } else {
                note.folderId == selectedFolderId
            }
            inFolder && matchesTagFilter(note.id)
        }
        if (globalSearchActive) globalResults else folderFiltered
    }
    val folderNoteCount by remember(notes, selectedFolderId) {
        derivedStateOf {
            if (selectedFolderId == NOTE_FOLDER_FILTER_ALL) notes.count { it.folderId == null }
            else notes.count { it.folderId == selectedFolderId }
        }
    }
    val orderedFolders = remember(noteFolders, folderOrder) {
        if (folderOrder.isEmpty()) noteFolders else noteFolders.sortedBy { folder ->
            val idx = folderOrder.indexOf(folder.id)
            if (idx == -1) Int.MAX_VALUE else idx
        }
    }
    val folderPageIds = remember(orderedFolders) {
        orderedFolders.map { it.id } + NOTE_FOLDER_FILTER_ALL
    }
    val initialFolderPage = folderPageIds.indexOf(selectedFolderId).let { index ->
        if (index >= 0) index else folderPageIds.lastIndex
    }
    val folderPagerState = rememberPagerState(initialPage = initialFolderPage) {
        folderPageIds.size
    }
    val focusedFolderId by remember(folderPageIds) {
        derivedStateOf {
            val indicatorProgress =
                folderPagerState.currentPage + folderPagerState.currentPageOffsetFraction
            val focusedPage = (indicatorProgress + 0.5f)
                .toInt()
                .coerceIn(folderPageIds.indices)
            folderPageIds[focusedPage]
        }
    }
    val folderListStates = remember { mutableMapOf<Long, LazyListState>() }
    val scrollRegistry = com.bookverse.contacts.ui.components.LocalAppBarStateRegistry.current
    val scrollRoute = com.bookverse.contacts.ui.components.LocalAppBarRoute.current
    val listState = remember(selectedFolderId) {
        folderListStates.getOrPut(selectedFolderId) { scrollRegistry?.listFor("$scrollRoute/folder/$selectedFolderId") ?: LazyListState() }
    }
    var pagerReady by remember { mutableStateOf(false) }
    LaunchedEffect(folderPageIds, selectedFolderId) {
        val targetPage = folderPageIds.indexOf(selectedFolderId).let { index ->
            if (index >= 0) index else folderPageIds.lastIndex
        }
        pagerReady = false
        if (folderPagerState.currentPage != targetPage) {
            folderPagerState.animateScrollToPage(targetPage)
        }
        pagerReady = true
    }
    LaunchedEffect(
        folderPagerState.currentPage,
        folderPagerState.isScrollInProgress,
        folderPageIds,
        pagerReady
    ) {
        if (!pagerReady || folderPagerState.isScrollInProgress) return@LaunchedEffect
        folderPageIds.getOrNull(folderPagerState.currentPage)?.let { folderId ->
            if (folderId != selectedFolderId) {
                selectedFolderId = folderId
                notesViewModel.setLastOpenedFolder(folderId)
            }
        }
    }
    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior { !searchActive }
    val shouldShowFab = !searchActive
    val hideOnScroll by remember(listState) {
        derivedStateOf {
            listState.isScrollInProgress && listState.firstVisibleItemScrollOffset > 0
        }
    }
    val fabOffset by animateDpAsState(
        targetValue = if (hideOnScroll) 88.dp else 0.dp, label = "notesFabOffset"
    )
    LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity
        if (activity != null) {
            val intent = activity.intent
            val showExpiredNoteDialog = intent.getBooleanExtra("showExpiredNoteDialog", false)
            val expiredNoteId = intent.getLongExtra("expiredNoteId", 0L)
            val expiredNoteTitle = intent.getStringExtra("expiredNoteTitle") ?: ""
            val expiredNoteMessage = intent.getStringExtra("expiredNoteMessage") ?: ""
            if (showExpiredNoteDialog && expiredNoteId > 0L) {
                coroutineScope.launch {
                    val reminderData = notesViewModel.getReminderDataForNote(expiredNoteId)
                    if (reminderData != null) {
                        val note = Note(
                            id = expiredNoteId,
                            title = expiredNoteTitle,
                            content = expiredNoteMessage,
                            isChecklist = false,
                            isReminder = true
                        )
                        onEditReminderClick(note, reminderData)
                    }
                }
                intent.removeExtra("showExpiredNoteDialog")
                intent.removeExtra("expiredNoteId")
                intent.removeExtra("expiredNoteTitle")
                intent.removeExtra("expiredNoteMessage")
            }
        }
    }
    BackHandler { onBackClick() }

    val focusManager = LocalFocusManager.current

    BackHandler(enabled = searchActive) {
        searchActive = false
        search = ""
        focusManager.clearFocus(force = true)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                expandable = true,
                title = stringResource(CoreUiR.string.notes_title),
                count = folderNoteCount,
                onBackClick = onBackClick,
                searchAction = if (notes.isNotEmpty()) com.bookverse.contacts.ui.components.AppBarSearchAction(
                    stringResource(R.string.search_note), { searchActive = true }, feedback = true,
                ) else null,
                actions = {
                    IconAction(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(CoreUiR.string.more_options),
                        onClick = { showOptionsDialog = true },
                        feedback = true
                    )
                },
                onCountClick = { showNotesDialog = true },
                scrollBehavior = scrollBehavior,
                titleContent = if (searchActive) {
                    {
                        TopBarSearchField(
                            value = search,
                            onValueChange = { search = it },
                            placeholder = stringResource(R.string.search_note),
                            focusRequester = focusRequester,
                            onClose = {
                                search = ""
                                focusManager.clearFocus(force = true)
                                searchActive = false
                            },
                            leadingContentDescription = stringResource(R.string.notes_search_open),
                            clearContentDescription = stringResource(CoreUiR.string.clear),
                        )
                    }
                } else null)
        },
        topBarFooter = {
            Column(Modifier.fillMaxWidth().blur(blurRadius)) {
                if (backupInProgress) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = stringResource(R.string.notes_backup_in_progress),
                            color = textColor.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }
                if (globalSearchActive) {
                    Text(
                        text = stringResource(R.string.notes_search_all_folders),
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else if (noteFolders.isNotEmpty()) {
                    NoteFolderFilterRow(
                        folders = orderedFolders,
                        selectedFolderId = focusedFolderId,
                        onFolderSelected = {
                            selectedFolderId = it
                            notesViewModel.setLastOpenedFolder(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        accentColor = accentColor
                    )
                }
                TagFilterBar(
                    tags = sharedTags,
                    selectedTagIds = selectedTagFilter,
                    onTagToggle = { notesViewModel.toggleTagFilter(it) },
                    onClearFilter = { notesViewModel.clearTagFilter() },
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        },
        contentOverlay = { padding ->
            val floatingActions = listOf(
                FloatingOptionItem(
                    label = stringResource(R.string.note), icon = Lucide.NotebookPen
                ) {
                    onCreateNoteClick("note")
                }, FloatingOptionItem(
                    label = stringResource(R.string.checklist), icon = Lucide.ListCheck
                ) {
                    onCreateNoteClick("checklist")
                }, FloatingOptionItem(
                    label = stringResource(R.string.recording), icon = Lucide.Mic
                ) {
                    onCreateNoteClick("recording")
                }, FloatingOptionItem(
                    label = stringResource(R.string.reminder), icon = Icons.Default.Notifications
                ) {
                    onCreateNoteClick("reminder")
                })
            FloatingOptions(
                state = floatingOptionsState,
                actions = floatingActions,
                accentColor = accentColor,
                accentBarStyle = accentBarStyle,
                modifier = Modifier.fillMaxSize().padding(padding),
                isVisible = shouldShowFab,
                hideOffset = fabOffset,
                iconAdd = Icons.AutoMirrored.Filled.NoteAdd,
                buttonDescription = stringResource(R.string.notes_empty_create),
            )
        },
        snackbarHost = { AppSnackbarHost(snackbar.hostState) })
    { padding ->
        val modern = accentBarStyle == com.bookverse.contacts.ui.theme.AccentBarStyle.MODERN
        val contentTopPadding = if (modern) padding.calculateTopPadding() else 0.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(if (modern) padding.withoutTop() else padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurRadius)
            ) {
                HorizontalPager(
                    state = folderPagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = noteFolders.isNotEmpty() && !globalSearchActive,
                ) { page ->
                    val pageFolderId = folderPageIds[page]
                    val filtered = remember(
                        notes,
                        globalSearchActive,
                        globalResults,
                        pageFolderId,
                        selectedTagFilter,
                        tagIdsByNoteId,
                    ) {
                        val folderFiltered = notes.filter { note ->
                            val inFolder = if (pageFolderId == NOTE_FOLDER_FILTER_ALL) {
                                note.folderId == null
                            } else {
                                note.folderId == pageFolderId
                            }
                            inFolder && matchesTagFilter(note.id)
                        }
                        if (globalSearchActive) globalResults else folderFiltered
                    }
                    val selectedFolder = noteFolders.firstOrNull { it.id == pageFolderId }
                    val pageListState = remember(pageFolderId) {
                        folderListStates.getOrPut(pageFolderId) { scrollRegistry?.listFor("$scrollRoute/folder/$pageFolderId") ?: LazyListState() }
                    }

                    Box(Modifier.fillMaxSize().padding(top = if (filtered.isEmpty()) contentTopPadding else 0.dp)) {
                    if (notes.isEmpty() && !searchActive) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                            tint = accentColor.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.no_notes),
                            color = textColor.copy(alpha = 0.8f),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.no_notes_description),
                            color = textColor.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                } else if (filtered.isEmpty() && (searchActive || tagFilterActive)) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.notes_no_results),
                            color = textColor.copy(alpha = 0.8f),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (filtered.isEmpty() && pageFolderId != NOTE_FOLDER_FILTER_ALL) {
                    val availableNotes = notes.filter { it.folderId != pageFolderId }
                    FolderEmptyState(
                        folder = selectedFolder,
                        accentColor = accentColor,
                        availableNotesCount = availableNotes.size,
                        onAddNoteClick = {
                            if (availableNotes.isEmpty()) {
                                snackbar.show(
                                    message = resources.getString(R.string.note_folder_empty_no_notes),
                                    type = SnackbarType.Info
                                )
                            } else {
                                selectedFolder?.let { folderTargetForQuickAdd = it }
                            }
                        },
                        onViewAllClick = {
                            selectedFolderId = NOTE_FOLDER_FILTER_ALL
                            notesViewModel.setLastOpenedFolder(NOTE_FOLDER_FILTER_ALL)
                        })
                } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = pageListState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = contentTopPadding),
                        ) {
                            items(filtered, key = { it.id }) { note ->
                                val checklist = checklistsByNoteId[note.id] ?: emptyList()
                                val reminderData = remindersByNoteId[note.id]
                                val recordingData = recordingsByNoteId[note.id]
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
                                        val data = reminderData
                                        if (data.type == ReminderType.PERIODIC) {
                                            val intervalText = when (data.interval) {
                                                60 -> "1 min"
                                                600 -> "10 min"
                                                1800 -> "30 min"
                                                3600 -> "60 min"
                                                else -> "${data.interval} sec"
                                            }
                                            stringResource(R.string.interval_list, intervalText)
                                        } else if (data.repeats) {
                                            val nextOccurrence = data.nextOccurrence(System.currentTimeMillis())
                                            if (nextOccurrence == null) {
                                                reminderRepeatLabel(data.effectiveRepeat)
                                            } else {
                                                stringResource(
                                                    R.string.reminder_repeat_list,
                                                    reminderRepeatLabel(data.effectiveRepeat),
                                                    reminderOccurrenceFormatter.format(Date(nextOccurrence)),
                                                )
                                            }
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
                                        val durationSeconds = recordingData.duration / 1000
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

                                val folderLabel = if (globalSearchActive) {
                                    folderNamesById[note.folderId]
                                        ?.let { stringResource(R.string.notes_search_in_folder, it) }
                                        ?: stringResource(R.string.note_move_to_folder_none)
                                } else {
                                    null
                                }

                                val subtitle = listOfNotNull(
                                    folderLabel, detailSubtitle, lockBadge
                                ).takeIf { it.isNotEmpty() }?.joinToString(" • ")

                                val openNoteAction: () -> Unit = {
                                    when {
                                        note.isReminder -> onEditReminderClick(note, reminderData)
                                        note.isRecording -> onEditRecordingClick(
                                            note, recordingData
                                        )

                                        else -> {
                                            val noteType =
                                                if (note.isChecklist) "checklist" else "note"
                                            onEditNoteClick(note, checklist, noteType)
                                        }
                                    }
                                }

                                NoteItem(
                                    icon = icon,
                                    title = note.title,
                                    onClick = {
                                        if (note.isLocked) {
                                            snackbar.show(
                                                message = resources.getString(lockPromptRes(note)),
                                                type = SnackbarType.Info
                                            )
                                            requestAuth(onSuccess = openNoteAction)
                                        } else {
                                            openNoteAction()
                                        }
                                    },
                                    onDeleteClick = {
                                        requestDelete(note)
                                    },
                                    onShareClick = {
                                        val performShare = {
                                            coroutineScope.launch {
                                                val result = runCatching {
                                                    when {
                                                        note.isReminder -> {
                                                            val data = reminderData
                                                                ?: notesViewModel.getReminderDataForNote(
                                                                    note.id
                                                                )
                                                                ?: throw IllegalStateException("Reminder data not available")
                                                            notesViewModel.shareReminder(
                                                                note, data, shareNoteLauncher
                                                            )
                                                        }

                                                        note.isRecording -> {
                                                            val data = recordingData
                                                                ?: notesViewModel.getRecordingDataForNote(
                                                                    note.id
                                                                )
                                                                ?: throw IllegalStateException("Recording data not available")
                                                            notesViewModel.shareRecording(
                                                                note, data, shareNoteLauncher
                                                            )
                                                        }

                                                        note.isChecklist -> {
                                                            val items = checklist.ifEmpty {
                                                                notesViewModel.getChecklistForNote(
                                                                    note.id
                                                                )
                                                            }
                                                            notesViewModel.shareChecklist(
                                                                note, items, shareNoteLauncher
                                                            )
                                                        }

                                                        else -> {
                                                            notesViewModel.shareNote(
                                                                note = note,
                                                                htmlContent = note.content,
                                                                shareNoteLauncher = shareNoteLauncher
                                                            )
                                                        }
                                                    }
                                                }
                                                if (result.isFailure) {
                                                    DiagnosticLog.error(DiagnosticEvent.NOTE_SHARE_FAILED)
                                                    snackbar.show(
                                                        message = resources.getString(R.string.notes_share_error),
                                                        type = SnackbarType.Error
                                                    )
                                                }
                                            }
                                        }
                                        if (note.isLocked) {
                                            requestAuth(onSuccess = { performShare() })
                                        } else {
                                            performShare()
                                        }
                                    },
                                    onSaveClick = {
                                        val performSave = {
                                            coroutineScope.launch {
                                                val payloadResult = runCatching {
                                                    when {
                                                        note.isReminder -> {
                                                            val data = reminderData
                                                                ?: notesViewModel.getReminderDataForNote(
                                                                    note.id
                                                                )
                                                                ?: throw IllegalStateException("Reminder data not available")
                                                            notesViewModel.buildReminderPayload(
                                                                note, data
                                                            )
                                                        }

                                                        note.isRecording -> {
                                                            val data = recordingData
                                                                ?: notesViewModel.getRecordingDataForNote(
                                                                    note.id
                                                                )
                                                                ?: throw IllegalStateException("Recording data not available")
                                                            notesViewModel.buildRecordingPayload(
                                                                note, data
                                                            )
                                                        }

                                                        note.isChecklist -> {
                                                            val items = checklist.ifEmpty {
                                                                notesViewModel.getChecklistForNote(
                                                                    note.id
                                                                )
                                                            }
                                                            notesViewModel.buildChecklistPayload(
                                                                note, items
                                                            )
                                                        }

                                                        else -> {
                                                            notesViewModel.buildNotePayload(
                                                                note, note.content
                                                            )
                                                        }
                                                    }
                                                }

                                                payloadResult.onSuccess { payload ->
                                                    launchSaveRequest(payload)
                                                }.onFailure {
                                                    DiagnosticLog.error(
                                                        DiagnosticEvent.NOTE_SAVE_PREPARATION_FAILED,
                                                    )
                                                    snackbar.show(
                                                        message = resources.getString(R.string.notes_save_error),
                                                        type = SnackbarType.Error
                                                    )
                                                }
                                            }
                                        }

                                        if (note.isLocked) {
                                            requestAuth(onSuccess = { performSave() })
                                        } else {
                                            performSave()
                                        }
                                    },
                                    onLockClick = {
                                        val shouldLock = !note.isLocked
                                        val applyLock = {
                                            notesViewModel.setNoteLock(note.id, shouldLock)
                                            snackbar.show(
                                                message = resources.getString(
                                                    lockMessageRes(note, shouldLock)
                                                ), type = SnackbarType.Success
                                            )
                                        }
                                        requestAuth(onSuccess = applyLock)
                                    },
                                    isLocked = note.isLocked,
                                    isPinned = note.isPinned,
                                    onPinClick = {
                                        notesViewModel.setNotePin(note.id, !note.isPinned)
                                        snackbar.show(
                                            message = resources.getString(
                                                if (note.isPinned) R.string.note_unpin else R.string.note_pin
                                            ),
                                            type = SnackbarType.Success
                                        )
                                    },
                                    listDensity = listDensity,
                                    timestamp = note.timestamp,
                                    subtitle = subtitle,
                                    isNormalNote = !note.isChecklist && !note.isReminder && !note.isRecording,
                                    onTransformToAudio = if (!note.isChecklist && !note.isReminder && !note.isRecording && onTransformToAudio != null) {
                                        {
                                            val performTransform = {
                                                val plainText = android.text.Html.fromHtml(
                                                    note.content,
                                                    android.text.Html.FROM_HTML_MODE_COMPACT
                                                ).toString().trim()

                                                if (plainText.isNotEmpty()) {
                                                    onTransformToAudio(plainText)
                                                } else {
                                                    snackbar.show(
                                                        message = resources.getString(R.string.note_empty_content),
                                                        type = SnackbarType.Warning
                                                    )
                                                }
                                            }

                                            if (note.isLocked) {
                                                requestAuth(onSuccess = { performTransform() })
                                            } else {
                                                performTransform()
                                            }
                                        }
                                    } else null,
                                    onMoveToFolderClick = if (noteFolders.isNotEmpty()) {
                                        {
                                            noteToMove = note
                                        }
                                    } else null,
                                    createdAt = note.createdAt,
                                    updatedAt = note.updatedAt,
                                    onTagsClick = { noteForTags = note },
                                    tagsContent = noteTagsFor(note.id).takeIf { it.isNotEmpty() }?.let { tags ->
                                        { NoteTagsRow(tags = tags) }
                                    },
                                )
                            }
                        }
                        val noteSections = remember(filtered) {
                            filtered.map { note ->
                                note.timestamp.let {
                                    val date = SimpleDateFormat(
                                        "yyyy-MM-dd", Locale.ROOT
                                    ).format(
                                        Date(it)
                                    )
                                    date
                                } ?: "#"
                            }.distinct().map { date ->
                                val index = filtered.indexOfFirst { note ->
                                    note.timestamp.let {
                                        val noteDate = SimpleDateFormat(
                                            "yyyy-MM-dd", Locale.ROOT
                                        ).format(Date(it))
                                        noteDate == date
                                    }
                                }
                                date to index
                            }
                        }
                        if (search.isEmpty() && filtered.size > 8) {
                            FastScroller(
                                listState = pageListState,
                                sections = noteSections.map { it.first to it.second },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(top = contentTopPadding, end = 8.dp),
                                accentColor = accentColor,
                                isDate = true
                            )
                        }
                        }
                    }
                    }
                }
            }

        }
        PasswordAuthDialogs(
            state = passwordAuthState,
        )


        if (showCreateOptionsDialog) {
            val createOptions = listOf(
                stringResource(R.string.note),
                stringResource(R.string.checklist),
                stringResource(R.string.recording),
                stringResource(R.string.reminder)
            )
            OptionsDialog(
                title = stringResource(R.string.what_type_note),
                options = createOptions,
                onOptionSelected = { index ->
                    when (index) {
                        0 -> onCreateNoteClick("note")
                        1 -> onCreateNoteClick("checklist")
                        2 -> onCreateNoteClick("recording")
                        3 -> onCreateNoteClick("reminder")
                    }
                    showCreateOptionsDialog = false
                },
                onDismiss = { showCreateOptionsDialog = false },
            )
        }
        if (noteToMove != null && noteFolders.isNotEmpty()) {
            val currentNote = noteToMove!!
            val folderOptions = listOf(
                FolderSelectionOption(null, stringResource(R.string.note_move_to_folder_none))
            ) + noteFolders.map { folder ->
                FolderSelectionOption(folder.id, folder.name)
            }
            val selectedOption =
                folderOptions.firstOrNull { it.id == currentNote.folderId } ?: folderOptions.first()

            SelectBottomSheet(
                onDismiss = { noteToMove = null },
                title = stringResource(R.string.note_move_to_folder),
                options = folderOptions,
                selectedOption = selectedOption,
                onSelect = { option ->
                    notesViewModel.moveNoteToFolder(currentNote.id, option.id)
                    val message = if (option.id == null) {
                        resources.getString(R.string.note_move_to_folder_removed)
                    } else {
                        resources.getString(R.string.note_move_to_folder_success, option.label)
                    }
                    snackbar.show(
                        message = message, type = SnackbarType.Success
                    )
                    noteToMove = null
                },
                optionKey = { it.id ?: -1L },
                optionLabel = { it.label },
            )
        } else if (noteToMove != null) {
            noteToMove = null
        }
        folderTargetForQuickAdd?.let { targetFolder ->
            val availableNotes = notes.filter { it.folderId != targetFolder.id }
            if (availableNotes.isEmpty()) {
                folderTargetForQuickAdd = null
            } else {
                val defaultSelection = availableNotes.first()
                SelectBottomSheet(
                    onDismiss = { folderTargetForQuickAdd = null },
                    title = stringResource(
                        R.string.note_folder_empty_select_note_title, targetFolder.name
                    ),
                    options = availableNotes,
                    selectedOption = defaultSelection,
                    onSelect = { note ->
                        notesViewModel.moveNoteToFolder(note.id, targetFolder.id)
                        snackbar.show(
                            message = resources.getString(
                                R.string.note_move_to_folder_success, targetFolder.name
                            ), type = SnackbarType.Success
                        )
                        folderTargetForQuickAdd = null
                    },
                    optionKey = { it.id },
                    optionLabel = { note ->
                        note.title.ifBlank { stringResource(R.string.note_without_title) }
                    },
                )
            }
        }
        if (showOptionsDialog) {
            val deletedNotes by notesViewModel.deletedNotes.collectAsStateWithLifecycle()
            val hasLockedDeletedNotes = remember(deletedNotes) {
                deletedNotes.any { it.isLocked }
            }

            val optionItems = listOf(
                OptionsDialogItem(
                    label = stringResource(CoreUiR.string.note_folders_title),
                    icon = Icons.Default.Folder,
                    enabled = true,
                    onClick = {
                        showOptionsDialog = false
                        onManageFolders()
                    }
                ),
                OptionsDialogItem(
                    label = stringResource(CoreUiR.string.tag_manager_title),
                    icon = Lucide.Tags,
                    enabled = true,
                    onClick = {
                        showOptionsDialog = false
                        showTagManager = true
                    }
                ),
                OptionsDialogItem(
                    label = stringResource(CoreUiR.string.recycle_bin_item),
                    icon = Icons.Default.Delete,
                    enabled = true,
                    onClick = {
                        showOptionsDialog = false
                        if (hasLockedDeletedNotes) {
                            requestAuth(onSuccess = { onNavigateToRecycleBin() }, onRequireSetup = {
                                snackbar.show(
                                    message = resources.getString(R.string.recycle_bin_auth_failed),
                                    type = SnackbarType.Warning
                                )
                            })
                        } else {
                            onNavigateToRecycleBin()
                        }
                    }
                ),
                OptionsDialogItem(
                    label = stringResource(CoreUiR.string.restore),
                    icon = Icons.Default.RestorePage,
                    enabled = !backupInProgress,
                    onClick = { handleImportRequest() }), OptionsDialogItem(
                    label = stringResource(CoreUiR.string.action_export_short),
                    icon = Icons.Default.Backup,
                    enabled = !backupInProgress,
                    onClick = { handleExportRequest() }
                )
            )
            OptionsDialog(
                title = stringResource(CoreUiR.string.more_options),
                options = optionItems,
                onDismiss = { showOptionsDialog = false },
            )
        }
        if (showDeleteDialog && noteToDelete != null) {
            DiagnosticLog.debug(DiagnosticEvent.NOTE_DELETE_DIALOG_OPENED)
            val message = when {
                noteToDelete!!.isChecklist -> stringResource(R.string.checklist_deleted)
                noteToDelete!!.isReminder -> stringResource(R.string.reminder_deleted)
                noteToDelete!!.isRecording -> stringResource(R.string.recording_deleted)
                else -> stringResource(R.string.note_deleted)
            }
            ConfirmDialog(
                onDismiss = { showDeleteDialog = false },
                onConfirm = {
                    noteToDelete?.let { notesViewModel.deleteNote(it) }
                    noteToDelete = null
                    showDeleteDialog = false
                    snackbar.show(
                        message = message, type = SnackbarType.Success
                    )
                },
                title = stringResource(CoreUiR.string.delete),
                message = stringResource(R.string.delete_confirm_note, noteToDelete!!.title),
                toastMessage = null,
            )
        }

        NoteListTagAndCountSheets(
            noteForTags = noteForTags,
            onNoteForTagsDismiss = { noteForTags = null },
            showTagManager = showTagManager,
            onTagManagerDismiss = { showTagManager = false },
            showTypeCounts = showNotesDialog,
            onTypeCountsDismiss = { showNotesDialog = false },
            notes = filtered,
            noteTags = noteTags,
            tagIdsByNoteId = tagIdsByNoteId,
            notesViewModel = notesViewModel,
            appSettingsViewModel = appSettingsViewModel,
            onTagNameConflict = { showTagNameConflict() },
        )
    }
}
