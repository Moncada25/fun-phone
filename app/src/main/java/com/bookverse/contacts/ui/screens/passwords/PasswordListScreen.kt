package com.bookverse.contacts.ui.screens.passwords

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import com.bookverse.contacts.ui.components.withoutTop
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.R
import com.bookverse.contacts.data.password.PasswordCredential
import com.bookverse.contacts.data.password.PASSWORD_FOLDER_FILTER_ALL
import com.bookverse.contacts.data.password.PasswordFolder
import com.bookverse.contacts.domain.passwords.CredentialMatchPolicy
import com.bookverse.contacts.domain.passwords.Totp
import com.bookverse.contacts.ui.components.AppSnackbarHost
import com.bookverse.contacts.ui.components.AppSnackbarState
import com.bookverse.contacts.ui.components.ConfirmDialog
import com.bookverse.contacts.ui.components.FloatingOptionItem
import com.bookverse.contacts.ui.components.FloatingOptions
import com.bookverse.contacts.ui.components.OptionsDialog
import com.bookverse.contacts.ui.components.OptionsDialogItem
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.SelectBottomSheet
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.ui.components.TopBarSearchField
import com.bookverse.contacts.ui.components.floatingOptionsBlur
import com.bookverse.contacts.ui.components.rememberAppSnackbarState
import com.bookverse.contacts.ui.components.rememberFloatingOptionsState
import com.bookverse.contacts.ui.components.folders.FolderEmptyState
import com.bookverse.contacts.ui.components.folders.FolderFilterRow
import com.bookverse.contacts.ui.components.folders.FolderUiModel
import com.bookverse.contacts.ui.utils.rememberHapticFeedback
import com.bookverse.contacts.settings.ListDensity
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.settings.LocaleController

import com.bookverse.contacts.ui.viewmodel.PasswordOp
import com.bookverse.contacts.ui.viewmodel.PasswordsViewModel
import com.bookverse.contacts.utils.ContactUtils
import com.bookverse.contacts.utils.JsonUtils

import com.composables.icons.lucide.KeyRound
import com.composables.icons.lucide.Lucide
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordListScreen(
    onBackClick: () -> Unit,
    onEdit: (PasswordCredential) -> Unit,
    onCreateNew: () -> Unit,
    onManageFolders: () -> Unit,
    onNavigateToRecycleBin: () -> Unit,
    passwordsViewModel: PasswordsViewModel,
    appSettingsViewModel: AppSettingsViewModel,
    backupActions: PasswordBackupActions,
) {
    val credentials by passwordsViewModel.credentials.collectAsStateWithLifecycle()
    val loadFailed by passwordsViewModel.loadFailed.collectAsStateWithLifecycle()
    val folders by passwordsViewModel.folders.collectAsStateWithLifecycle()
    val reusedIds by passwordsViewModel.reusedPasswordIds.collectAsStateWithLifecycle()
    val accentColor = MaterialTheme.colorScheme.primary
    val accentBarStyle by appSettingsViewModel.accentBarStyle.collectAsStateWithLifecycle()
    val listDensity by appSettingsViewModel.listDensity.collectAsStateWithLifecycle()
    val folderOrder by appSettingsViewModel.passwordFoldersOrder.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val textColor = MaterialTheme.colorScheme.onBackground
    val backgroundColor = MaterialTheme.colorScheme.background
    val search by passwordsViewModel.search.collectAsStateWithLifecycle()
    val op by passwordsViewModel.operation.collectAsStateWithLifecycle()
    var searchActive by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var credentialToMove by remember { mutableStateOf<PasswordCredential?>(null) }
    var quickAddFolder by remember { mutableStateOf<PasswordFolder?>(null) }
    var selectedFolderId by rememberSaveable {
        mutableLongStateOf(passwordsViewModel.lastOpenedFolderId)
    }
    val revealMap = remember { mutableStateMapOf<Long, Boolean>() }
    val snackHost = rememberAppSnackbarState()
    val focusRequester = remember { FocusRequester() }
    var showStrengthSheet by remember { mutableStateOf(false) }
    var showExportWarning by rememberSaveable { mutableStateOf(false) }
    val userCopiedStr = stringResource(R.string.user_copied)
    val passCopyStr = stringResource(R.string.password_copy_text)
    val totpCopiedStr = stringResource(R.string.password_totp_copied)
    val openWebsiteErrorStr = stringResource(R.string.password_open_website_error)
    var showAutofillInfo by remember { mutableStateOf(false) }
    val passwordCreatedSuccess = stringResource(R.string.password_created_success)
    val passwordUpdatedSuccess = stringResource(R.string.password_updated_success)

    LaunchedEffect(searchActive) { if (searchActive) focusRequester.requestFocus() }

    LaunchedEffect(op) {
        if (op != null) {
            val snackMsg = if (op == PasswordOp.CREATED) passwordCreatedSuccess else passwordUpdatedSuccess
            snackHost.show(snackMsg, SnackbarType.Success)
            passwordsViewModel.clearOperation()
        }
    }

    val scope = rememberCoroutineScope()
    val floatingOptionsState = rememberFloatingOptionsState()
    val orderedFolders = remember(folders, folderOrder) {
        if (folderOrder.isEmpty()) folders else folders.sortedBy { folder ->
            folderOrder.indexOf(folder.id).takeIf { it >= 0 } ?: Int.MAX_VALUE
        }
    }
    val folderPageIds = remember(orderedFolders) {
        orderedFolders.map { it.id } + PASSWORD_FOLDER_FILTER_ALL
    }
    val initialPage = folderPageIds.indexOf(selectedFolderId).takeIf { it >= 0 }
        ?: folderPageIds.lastIndex
    val pagerState = rememberPagerState(initialPage = initialPage) { folderPageIds.size }
    val focusedFolderId by remember(folderPageIds) {
        derivedStateOf {
            val page = (pagerState.currentPage + pagerState.currentPageOffsetFraction + 0.5f)
                .toInt()
                .coerceIn(folderPageIds.indices)
            folderPageIds[page]
        }
    }
    val folderListStates = remember { mutableMapOf<Long, LazyListState>() }
    val scrollRegistry = com.bookverse.contacts.ui.components.LocalAppBarStateRegistry.current
    val scrollRoute = com.bookverse.contacts.ui.components.LocalAppBarRoute.current
    val listState = remember(selectedFolderId) {
        folderListStates.getOrPut(selectedFolderId) { scrollRegistry?.listFor("$scrollRoute/folder/$selectedFolderId") ?: LazyListState() }
    }
    var pagerReady by remember { mutableStateOf(false) }
    LaunchedEffect(selectedFolderId) {
        passwordsViewModel.setLastOpenedFolder(selectedFolderId)
    }
    LaunchedEffect(folders, selectedFolderId) {
        if (selectedFolderId != PASSWORD_FOLDER_FILTER_ALL && folders.none { it.id == selectedFolderId }) {
            selectedFolderId = PASSWORD_FOLDER_FILTER_ALL
        }
    }
    LaunchedEffect(folderPageIds, selectedFolderId) {
        val target = folderPageIds.indexOf(selectedFolderId).takeIf { it >= 0 }
            ?: folderPageIds.lastIndex
        pagerReady = false
        if (pagerState.currentPage != target) pagerState.animateScrollToPage(target)
        pagerReady = true
    }
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress, folderPageIds, pagerReady) {
        if (!pagerReady || pagerState.isScrollInProgress) return@LaunchedEffect
        folderPageIds.getOrNull(pagerState.currentPage)?.let { folderId ->
            if (folderId != selectedFolderId) selectedFolderId = folderId
        }
    }
    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior { !searchActive }
    val blurRadius = floatingOptionsBlur(floatingOptionsState)
    val hideOnScroll by remember(listState) {
        derivedStateOf {
            listState.isScrollInProgress && listState.firstVisibleItemScrollOffset > 0
        }
    }
    val fabOffset by animateDpAsState(
        targetValue = if (hideOnScroll) 88.dp else 0.dp,
        label = "fabOffset"
    )
    val shouldShowFab = !searchActive
    val currentCount = credentials.count { credential ->
        if (selectedFolderId == PASSWORD_FOLDER_FILTER_ALL) credential.folderId == null
        else credential.folderId == selectedFolderId
    }
    val currentCredentials = remember(credentials, selectedFolderId) {
        credentials.filter { credential ->
            if (selectedFolderId == PASSWORD_FOLDER_FILTER_ALL) credential.folderId == null
            else credential.folderId == selectedFolderId
        }
    }

    val focusManager = LocalFocusManager.current

    BackHandler(enabled = searchActive) {
        searchActive = false
        passwordsViewModel.setSearch("")
        focusManager.clearFocus(force = true)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                expandable = true,
                title = stringResource(R.string.passwords_title),
                count = currentCount,
                onBackClick = onBackClick,
                onCountClick = { showStrengthSheet = true },
                searchAction = if (credentials.isNotEmpty()) com.bookverse.contacts.ui.components.AppBarSearchAction(
                    stringResource(R.string.search_password), { searchActive = true }, feedback = true,
                ) else null,
                actions = {
                    IconAction(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.more_options),
                        onClick = { showOptionsDialog = true },
                        feedback = true,
                    )
                },
                appSettingsViewModel = appSettingsViewModel,
                scrollBehavior = scrollBehavior,
                titleContent = if (searchActive) {
                    {
                        TopBarSearchField(
                            value = search,
                            onValueChange = { passwordsViewModel.setSearch(it) },
                            placeholder = stringResource(R.string.search_password),
                            focusRequester = focusRequester,
                            onClose = {
                                passwordsViewModel.setSearch("")
                                focusManager.clearFocus(force = true)
                                searchActive = false
                            },
                            leadingContentDescription = stringResource(R.string.passwords_search_open),
                            clearContentDescription = stringResource(R.string.clear),
                        )
                    }
                } else null
            )
        },
        topBarFooter = {
            Column(Modifier.fillMaxWidth().blur(blurRadius)) {
                if (loadFailed) {
                    Text(stringResource(R.string.vault_load_failed), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                    androidx.compose.material3.TextButton(onClick = passwordsViewModel::unlock) { Text(stringResource(R.string.social_preview_retry)) }
                }
                if (folders.isNotEmpty()) {
                    FolderFilterRow(
                        folders = orderedFolders.map { it.toUiModel() },
                        selectedFolderId = focusedFolderId,
                        unfiledId = PASSWORD_FOLDER_FILTER_ALL,
                        unfiledLabel = stringResource(R.string.password_folders_filter_all),
                        onFolderSelected = { selectedFolderId = it },
                        accentColor = accentColor,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        },
        contentOverlay = { padding ->
            val floatingActions = listOf(
                FloatingOptionItem(
                    label = stringResource(R.string.restore),
                    icon = Icons.Default.RestorePage
                ) {
                    backupActions.importBackup()
                },
                FloatingOptionItem(
                    label = stringResource(R.string.action_export_short),
                    icon = Icons.Default.Backup
                ) {
                    showExportWarning = true
                },
                FloatingOptionItem(
                    label = stringResource(R.string.action_create_short),
                    icon = Icons.Default.Add
                ) {
                    onCreateNew()
                }
            )
            FloatingOptions(
                buttonDescription = stringResource(R.string.password_new),
                state = floatingOptionsState,
                actions = floatingActions,
                accentColor = accentColor,
                accentBarStyle = accentBarStyle,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                isVisible = shouldShowFab,
                hideOffset = fabOffset,
                iconAdd = Lucide.KeyRound
            )
        },
        snackbarHost = { AppSnackbarHost(snackHost.hostState) }
    ) { pv ->
        val modern = accentBarStyle == com.bookverse.contacts.ui.viewmodel.AccentBarStyle.MODERN
        val contentTopPadding = if (modern) pv.calculateTopPadding() else 0.dp
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (modern) pv.withoutTop() else pv)
                    .blur(blurRadius)
            ) {
                if (!loadFailed) HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = folders.isNotEmpty(),
                ) { page ->
                    val pageFolderId = folderPageIds[page]
                    val pageCredentials = remember(credentials, pageFolderId) {
                        credentials.filter { credential ->
                            if (pageFolderId == PASSWORD_FOLDER_FILTER_ALL) credential.folderId == null
                            else credential.folderId == pageFolderId
                        }
                    }
                    val selectedFolder = folders.firstOrNull { it.id == pageFolderId }
                    val pageListState = remember(pageFolderId) {
                        folderListStates.getOrPut(pageFolderId) { scrollRegistry?.listFor("$scrollRoute/folder/$pageFolderId") ?: LazyListState() }
                    }
                    Box(Modifier.fillMaxSize().padding(top = if (pageCredentials.isEmpty()) contentTopPadding else 0.dp)) {
                    when {
                        credentials.isEmpty() && !searchActive -> PasswordGlobalEmptyState(accentColor, textColor)
                        pageCredentials.isEmpty() && searchActive -> PasswordNoResultsState(textColor)
                        pageCredentials.isEmpty() && selectedFolder != null -> FolderEmptyState(
                            icon = Icons.Default.VpnKey,
                            title = stringResource(R.string.password_folder_empty_title, selectedFolder.name),
                            description = stringResource(R.string.password_folder_empty_description),
                            primaryActionLabel = stringResource(R.string.password_folder_empty_add_button),
                            accentColor = accentColor,
                            onPrimaryAction = { quickAddFolder = selectedFolder },
                            primaryActionEnabled = credentials.any { it.folderId != selectedFolder.id },
                            secondaryActionLabel = stringResource(R.string.password_folder_empty_view_all),
                            onSecondaryAction = { selectedFolderId = PASSWORD_FOLDER_FILTER_ALL },
                            hint = if (credentials.none { it.folderId != selectedFolder.id }) {
                                stringResource(R.string.password_folder_empty_hint)
                            } else null,
                        )
                        else -> LazyColumn(
                            state = pageListState,
                            modifier = Modifier.fillMaxSize().background(backgroundColor),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = contentTopPadding),
                        ) {
                            items(pageCredentials, key = { it.id }) { cred ->
                                val revealed = revealMap[cred.id] ?: false
                                PasswordCard(
                                    credential = cred,
                                    passwordsViewModel = passwordsViewModel,
                                    appSettingsViewModel = appSettingsViewModel,
                                    listDensity = listDensity,
                                    revealed = revealed,
                                    reused = cred.id in reusedIds,
                                    onCopyTotp = { code ->
                                        com.bookverse.contacts.security.SensitiveClipboard.copy(context, code)
                                        snackHost.show(totpCopiedStr, SnackbarType.Success)
                                    },
                                    onOpenWebsite = { website ->
                                        val result = com.bookverse.contacts.utils.ExternalUriLauncher.open(context, website)
                                        if (result != com.bookverse.contacts.utils.ExternalUriLauncher.Result.OPENED) {
                                            snackHost.show(openWebsiteErrorStr, SnackbarType.Error)
                                        }
                                    },
                                    onToggleReveal = { revealMap[cred.id] = !revealed },
                                    onCopyUser = {
                                        ContactUtils.copyToClipboard(context, cred.username, false)
                                        snackHost.show(userCopiedStr, SnackbarType.Success)
                                    },
                                    onCopyPass = {
                                        com.bookverse.contacts.security.SensitiveClipboard.copy(context, cred.password)
                                        snackHost.show(passCopyStr, SnackbarType.Success)
                                    },
                                    onShare = { scope.launch { JsonUtils.sharePasswordCredential(context, cred) } },
                                    onEdit = { credential ->
                                        passwordsViewModel.setSearch("")
                                        onEdit(credential)
                                    },
                                    onMoveToFolder = if (folders.isNotEmpty()) {
                                        { credentialToMove = cred }
                                    } else null,
                                    snackHost = snackHost,
                                )
                            }
                        }
                    }
                    }
                }
            }

        }
    }

    if (showStrengthSheet) {
        PasswordStrengthCountsSheet(
            passwords = currentCredentials.map { it.password },
            reusedCount = currentCredentials.count { it.id in reusedIds },
            accentColor = accentColor,
            onDismiss = { showStrengthSheet = false },
        )
    }

    if (showExportWarning) {
        ConfirmDialog(
            onDismiss = { showExportWarning = false },
            onConfirm = { backupActions.exportBackup() },
            title = stringResource(R.string.passwords_export_unencrypted_title),
            message = stringResource(R.string.passwords_export_unencrypted_message),
            appSettingsViewModel = appSettingsViewModel,
            confirmButtonLabel = stringResource(R.string.passwords_export_unencrypted_confirm),
            isDestructive = false,
        )
    }

    if (showOptionsDialog) {
        OptionsDialog(
            title = stringResource(R.string.more_options),
            options = listOfNotNull(
                OptionsDialogItem(
                    label = stringResource(R.string.note_folders_title),
                    icon = Icons.Default.Folder,
                    onClick = {
                        showOptionsDialog = false
                        passwordsViewModel.setSearch("")
                        onManageFolders()
                    },
                ),
                OptionsDialogItem(
                    label = stringResource(R.string.recycle_bin_item),
                    icon = Icons.Default.Delete,
                    onClick = {
                        showOptionsDialog = false
                        passwordsViewModel.setSearch("")
                        onNavigateToRecycleBin()
                    },
                ),
                OptionsDialogItem(
                    label = stringResource(R.string.autofill_option_title),
                    icon = Icons.Default.Password,
                    onClick = {
                        showOptionsDialog = false
                        showAutofillInfo = true
                    },
                ),
            ),
            onDismiss = { showOptionsDialog = false },
        )
    }

    if (showAutofillInfo) {
        AutofillServiceDialog(onDismiss = { showAutofillInfo = false })
    }

    credentialToMove?.let { credential ->
        val options = listOf(PasswordFolderOption(null, stringResource(R.string.password_move_to_folder_none))) +
            folders.map { PasswordFolderOption(it.id, it.name) }
        SelectBottomSheet(
            onDismiss = { credentialToMove = null },
            title = stringResource(R.string.password_move_to_folder),
            options = options,
            selectedOption = options.firstOrNull { it.id == credential.folderId },
            onSelect = { option ->
                passwordsViewModel.moveToFolder(credential.id, option.id)
                snackHost.show(
                    if (option.id == null) resources.getString(R.string.password_move_to_folder_removed)
                    else resources.getString(R.string.password_move_to_folder_success, option.label),
                    SnackbarType.Success,
                )
                credentialToMove = null
            },
            optionKey = { it.id ?: -1L },
            optionLabel = { it.label },
        )
    }

    quickAddFolder?.let { folder ->
        val available = credentials.filter { it.folderId != folder.id }
        SelectBottomSheet(
            onDismiss = { quickAddFolder = null },
            title = stringResource(R.string.password_folder_empty_select_title, folder.name),
            options = available,
            selectedOption = null,
            onSelect = { credential ->
                passwordsViewModel.moveToFolder(credential.id, folder.id)
                snackHost.show(
                    resources.getString(R.string.password_move_to_folder_success, folder.name),
                    SnackbarType.Success,
                )
                quickAddFolder = null
            },
            optionKey = { it.id },
            optionLabel = { it.name },
        )
    }
}

private data class PasswordFolderOption(val id: Long?, val label: String)

private fun PasswordFolder.toUiModel() = FolderUiModel(id, name, description, colorHex, createdAt)

@Composable
private fun PasswordGlobalEmptyState(accentColor: androidx.compose.ui.graphics.Color, textColor: androidx.compose.ui.graphics.Color) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Lock, stringResource(R.string.passwords_empty_icon), Modifier.size(120.dp), accentColor.copy(alpha = 0.6f))
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.passwords_empty), color = textColor.copy(alpha = 0.8f), fontSize = 24.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.passwords_empty_description), color = textColor.copy(alpha = 0.6f), fontSize = 16.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
    }
}

@Composable
private fun PasswordNoResultsState(textColor: androidx.compose.ui.graphics.Color) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(stringResource(R.string.password_no_results), color = textColor.copy(alpha = 0.8f), fontSize = 20.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PasswordCard(
    credential: PasswordCredential,
    passwordsViewModel: PasswordsViewModel,
    appSettingsViewModel: AppSettingsViewModel,
    listDensity: ListDensity = ListDensity.NORMAL,
    revealed: Boolean,
    reused: Boolean,
    onCopyTotp: (String) -> Unit,
    onOpenWebsite: (String) -> Unit,
    onToggleReveal: () -> Unit,
    onCopyUser: () -> Unit,
    onCopyPass: () -> Unit,
    onShare: () -> Unit,
    onEdit: (PasswordCredential) -> Unit,
    onMoveToFolder: (() -> Unit)?,
    snackHost: AppSnackbarState,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    val performHaptic = rememberHapticFeedback(HapticFeedbackType.Confirm)
    val pass = credential.password
    val strength = remember(pass) { PasswordGenerator.strength(pass) }
    val (strengthColor, strengthLabelRes) = remember(strength) {
        ContactUtils.passwordStrengthInfo(
            strength
        )
    }
    val accentColor = MaterialTheme.colorScheme.primary
    val strengthLabel = stringResource(strengthLabelRes)
    val totpConfig = remember(credential.totp) { Totp.parse(credential.totp) }
    val website = remember(credential.url) { CredentialMatchPolicy.webUrl(credential.url) }
    val reusedLabel = stringResource(R.string.password_reused_label)
    var expanded by remember { mutableStateOf(false) }
    val locale = LocaleController.effectiveLocale(LocalContext.current)
    val dateFormatter = remember(locale) { SimpleDateFormat("dd/MM/yyyy", locale) }
    val timeFormatter = remember(locale) { SimpleDateFormat("HH:mm", locale) }
    val formattedCreated = remember(credential.createdAt) {
        dateFormatter.format(Date(credential.createdAt))
    }
    val formattedUpdated = remember(credential.updatedAt) {
        dateFormatter.format(Date(credential.updatedAt))
    }
    val formattedCreatedTime = remember(credential.createdAt) {
        timeFormatter.format(Date(credential.createdAt))
    }
    val formattedUpdatedTime = remember(credential.updatedAt) {
        timeFormatter.format(Date(credential.updatedAt))
    }
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { expanded = !expanded },
                onLongClick = {
                    performHaptic()
                    showOptionsSheet = true
                }
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(listDensity.itemPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(listDensity.avatarSize)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = stringResource(
                                R.string.password_item_icon_description,
                                credential.name
                            ),
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = credential.name,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = strengthLabel,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = strengthColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (reused) {
                                Text(
                                    text = reusedLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                IconButton(onClick = {
                    onCopyPass()
                    copied = true
                }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = if (copied) {
                            stringResource(R.string.password_copied)
                        } else {
                            stringResource(R.string.password_copy_action)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    if (copied) {
                        LaunchedEffect(Unit) {
                            delay(1000.milliseconds)
                            copied = false
                        }
                    }
                }
                Box(contentAlignment = Alignment.TopEnd) {
                    IconButton(onClick = {
                        performHaptic()
                        showOptionsSheet = true
                    }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.more_options),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(250)),
                exit = fadeOut(animationSpec = tween(250))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val user = credential.username
                    val pass = credential.password
                    Text(
                        text = stringResource(
                            R.string.user_home,
                            if (revealed) user else maskPasswordValue(user)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(
                            R.string.password_home,
                            if (revealed) pass else maskPasswordValue(pass)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!credential.note.isNullOrBlank()) {
                        Text(
                            text = stringResource(
                                R.string.note_home,
                                credential.note
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    val storedUrl = credential.url
                    if (!storedUrl.isNullOrBlank()) {
                        val linkedApp = storedUrl.startsWith(CredentialMatchPolicy.APP_SCHEME)
                        Text(
                            text = if (linkedApp) {
                                stringResource(
                                    R.string.password_linked_app_home,
                                    storedUrl.removePrefix(CredentialMatchPolicy.APP_SCHEME),
                                )
                            } else {
                                stringResource(R.string.password_website_home, storedUrl)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (website != null) accentColor else Color.Unspecified,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = if (website != null) {
                                Modifier.clickable(
                                    onClickLabel = stringResource(R.string.password_open_website),
                                ) { onOpenWebsite(website) }
                            } else {
                                Modifier
                            },
                        )
                    }
                    if (totpConfig != null) {
                        TotpCodeRow(config = totpConfig, accentColor = accentColor, onCopy = onCopyTotp)
                    }
                }
            }
        }
    }

    if (showOptionsSheet) {
        val options = listOf(
            OptionsDialogItem(
                label = if (revealed) stringResource(R.string.toggle_hide)
                else stringResource(R.string.toggle_reveal),
                icon = if (revealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                onClick = onToggleReveal,
            ),
            OptionsDialogItem(
                label = stringResource(R.string.share),
                icon = Icons.Default.Share,
                onClick = onShare,
            ),
            OptionsDialogItem(
                label = stringResource(R.string.edit),
                icon = Icons.Default.EditNote,
                onClick = { onEdit(credential) },
            ),
            *if (onMoveToFolder != null) {
                arrayOf(
                    OptionsDialogItem(
                        label = stringResource(R.string.password_move_to_folder),
                        icon = Icons.Default.Folder,
                        onClick = onMoveToFolder,
                    ),
                )
            } else {
                emptyArray()
            },
            OptionsDialogItem(
                label = stringResource(R.string.user_copy_text),
                icon = Icons.Default.CopyAll,
                onClick = onCopyUser,
            ),
            *if (totpConfig != null) {
                arrayOf(
                    OptionsDialogItem(
                        label = stringResource(R.string.password_totp_copy),
                        icon = Icons.Default.Timer,
                        onClick = {
                            onCopyTotp(Totp.code(totpConfig, System.currentTimeMillis() / 1_000L))
                        },
                    ),
                )
            } else {
                emptyArray()
            },
            *if (website != null) {
                arrayOf(
                    OptionsDialogItem(
                        label = stringResource(R.string.password_open_website),
                        icon = Icons.Default.Language,
                        onClick = { onOpenWebsite(website) },
                    ),
                )
            } else {
                emptyArray()
            },
            OptionsDialogItem(
                label = stringResource(R.string.delete),
                icon = Icons.Default.Delete,
                destructive = true,
                onClick = { showDeleteDialog = true },
            ),
            OptionsDialogItem(
                label = "${stringResource(R.string.created_label)}: $formattedCreated $formattedCreatedTime",
                icon = null,
                description = "${stringResource(R.string.updated_label)}: $formattedUpdated $formattedUpdatedTime",
                enabled = false,
                onClick = {},
            ),
        )
        OptionsDialog(
            title = stringResource(R.string.more_options),
            options = options,
            onDismiss = { showOptionsSheet = false },
        )
    }

    if (showDeleteDialog) {
        val passwordDeletedMessage = stringResource(R.string.password_deleted, credential.name)
        ConfirmDialog(
            onConfirm = {
                passwordsViewModel.delete(credential.id)
                showDeleteDialog = false
                snackHost.show(
                    message = passwordDeletedMessage,
                    type = SnackbarType.Success
                )
            },
            onDismiss = { showDeleteDialog = false },
            title = stringResource(R.string.delete_password),
            message = stringResource(R.string.delete_password_confirm, credential.name),
            appSettingsViewModel = appSettingsViewModel
        )
    }
}
