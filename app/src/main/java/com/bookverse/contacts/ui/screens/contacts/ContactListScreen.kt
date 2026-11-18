package com.bookverse.contacts.ui.screens.contacts

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactEmergency
import androidx.compose.material.icons.filled.ContactMail
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Whatsapp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.domain.contacts.ContactRecycleBinPolicy
import com.bookverse.contacts.domain.contacts.ContactSearchPolicy
import com.bookverse.contacts.domain.contacts.ContactSortPolicy
import com.bookverse.contacts.data.model.ContactAccount
import com.bookverse.contacts.data.model.ContactTag
import com.bookverse.contacts.ui.components.AppSnackbarHost
import com.bookverse.contacts.ui.components.ConfirmDialog
import com.bookverse.contacts.ui.components.CountBreakdownSheet
import com.bookverse.contacts.ui.components.CountInfoRow
import com.bookverse.contacts.ui.components.FastScroller
import com.bookverse.contacts.ui.components.FloatingIconPill
import com.bookverse.contacts.ui.components.FloatingPill
import com.bookverse.contacts.ui.components.LoaderDialog
import com.bookverse.contacts.ui.components.MainTopBar
import com.bookverse.contacts.ui.components.MessageDialog
import com.bookverse.contacts.ui.components.PermissionBanner
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.ui.components.TagFilterBar
import com.bookverse.contacts.ui.components.TopBarSearchField
import com.bookverse.contacts.ui.components.rememberAppSnackbarState
import com.bookverse.contacts.ui.components.topBarBackground
import com.bookverse.contacts.ui.components.topBarColors
import com.bookverse.contacts.settings.ListDensity
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.theme.AccentBarStyle
import com.bookverse.contacts.ui.viewmodel.ContactTagViewModel
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.utils.ContactUtils
import com.bookverse.contacts.utils.avatarLabel
import com.composables.icons.lucide.ArrowDownAZ
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings
import androidx.core.graphics.toColorInt

@SuppressLint("StringFormatInvalid")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    contactsViewModel: ContactsViewModel,
    contactTagViewModel: ContactTagViewModel,
    onContactClick: (Contact) -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
    listState: LazyListState = rememberLazyListState(),
    isLimitedMode: Boolean = false,
    onCreateContact: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onReviewPhoneStatus: () -> Unit = {},
    onChooseTools: () -> Unit = {},
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val accentBarStyle by appSettingsViewModel.accentBarStyle.collectAsStateWithLifecycle()
    val toolsOrder by appSettingsViewModel.toolsOrder.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = colorScheme.background
    val textColor = colorScheme.onBackground
    val resources = LocalResources.current
    val snackbar = rememberAppSnackbarState()

    val contacts by contactsViewModel.contacts.collectAsStateWithLifecycle()
    val selectedContacts by contactsViewModel.selectedContacts.collectAsStateWithLifecycle()
    val listDensity by appSettingsViewModel.listDensity.collectAsStateWithLifecycle()
    val isDeleting by contactsViewModel.isDeleting.collectAsStateWithLifecycle()
    val accounts by contactsViewModel.accounts.collectAsStateWithLifecycle()
    val isImporting by contactsViewModel.isImporting.collectAsStateWithLifecycle()
    val importResult by contactsViewModel.importResult.collectAsStateWithLifecycle()
    val vCardResult by contactsViewModel.vCard.result.collectAsStateWithLifecycle()
    val sortOrder by appSettingsViewModel.contactSortOrder.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sortLocale = LocaleController.effectiveLocale(context)
    var showSortSheet by remember { mutableStateOf(false) }

    val allTags by contactTagViewModel.allTags.collectAsStateWithLifecycle()
    val contactTagsMap by contactTagViewModel.contactTags.collectAsStateWithLifecycle()
    val selectedFilterTags by contactTagViewModel.selectedFilterTags.collectAsStateWithLifecycle()
    val filteredContactIds by contactTagViewModel.filteredContactIds.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        contactsViewModel.snackbarEvents.collect { event ->
            snackbar.show(
                message = event.message,
                type = event.kind,
                actionLabel = event.actionLabel
            )
        }
    }

    LaunchedEffect(contacts) {
        contactTagViewModel.loadTagsForContacts(contacts.map { it.id })
    }
    val defaultAccount by appSettingsViewModel.defaultContactAccount.collectAsStateWithLifecycle()

    var showAccountsDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
        focusManager.clearFocus(force = true)
    }

    BackHandler(enabled = selectedContacts.isNotEmpty()) {
        contactsViewModel.clearContactSelection()
    }

    val restoreJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                contactsViewModel.vCard.importFile(uri, defaultAccount) {
                    contactsViewModel.importContacts(uri, contactsViewModel, defaultAccount)
                }
            }
        }
    )

    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior { !isSearchActive && selectedContacts.isEmpty() }
    val showTitle by remember(accentBarStyle) {
        derivedStateOf { accentBarStyle == AccentBarStyle.MODERN || scrollBehavior.state.collapsedFraction < 0.995f }
    }

    val filteredContacts = remember(contacts, searchQuery, filteredContactIds, sortOrder, sortLocale) {
        val matches = ContactSearchPolicy.filterAndSort(contacts, searchQuery, filteredContactIds)
        if (searchQuery.isBlank()) ContactSortPolicy.sort(matches, sortOrder, sortLocale) else matches
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteResultCount by remember { mutableStateOf<Int?>(null) }
    var pendingDeleteCount by remember { mutableIntStateOf(0) }
    var prevIsDeleting by remember { mutableStateOf(false) }

    LaunchedEffect(isDeleting) {
        if (prevIsDeleting && !isDeleting && deleteResultCount == null) {
            deleteResultCount = selectedContacts.size
        }
        prevIsDeleting = isDeleting
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                pendingDeleteCount = selectedContacts.size
                showDeleteConfirm = false
                contactsViewModel.deleteSelectedContacts()
            },
            title = stringResource(R.string.delete_contacts_title),
            message = pluralStringResource(
                R.plurals.delete_contacts_to_bin_message,
                selectedContacts.size,
                selectedContacts.size,
                ContactRecycleBinPolicy.RETENTION_DAYS,
            ),
            toastMessage = null,
            appSettingsViewModel = appSettingsViewModel
        )
    }

    if (isDeleting) {
        LoaderDialog(
            message = stringResource(R.string.deleting_contacts),
        )
    }

    if (pendingDeleteCount > 0 && deleteResultCount == null && !isDeleting) {
        deleteResultCount = pendingDeleteCount
    }
    LaunchedEffect(deleteResultCount, isDeleting) {
        val count = deleteResultCount
        if (count != null && !isDeleting) {
            val message = resources.getQuantityString(R.plurals.delete_success, count, count)
            snackbar.show(message = message, type = SnackbarType.Success)
            deleteResultCount = null
            pendingDeleteCount = 0
        }
    }

    val groupedContacts = remember(filteredContacts, searchQuery, sortOrder) {
        if (searchQuery.isEmpty()) {
            ContactSortPolicy.sections(filteredContacts, sortOrder)
        } else {
            emptyList()
        }
    }

    val alphabetSections = remember(groupedContacts) {
        if (groupedContacts.isNotEmpty()) {
            val sections = mutableListOf<Pair<String, Int>>()
            var position = 0
            groupedContacts.forEach { group ->
                sections.add(group.label to position)
                position += group.contacts.size + 1
            }
            sections
        } else {
            emptyList()
        }
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { AppSnackbarHost(snackbar.hostState) },
        topBar = {
            Column {
                val pinnedHeaderContent = allTags.isNotEmpty() || isLimitedMode
                if (selectedContacts.isNotEmpty()) {
                    val count = selectedContacts.size
                    val label =
                        if (count == 1) stringResource(R.string.selected_single) else stringResource(
                            R.string.selected_plural
                        )
                    val isMinimalAccent =
                        accentBarStyle == AccentBarStyle.MINIMAL
                    val colors = topBarColors(accentBarStyle)
                    val selectionContentColor = colors.titleContentColor
                    val pillBackground = colorScheme.surface
                    val fontSize = if (isMinimalAccent) 20.sp else 24.sp

                    TopAppBar(
                        modifier = Modifier.topBarBackground(accentBarStyle, scrollBehavior, pinnedHeaderContent),
                        title = {
                            AnimatedVisibility(
                                visible = showTitle,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                val titleContent: @Composable () -> Unit = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$count $label",
                                            color = selectionContentColor,
                                            fontSize = fontSize,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                if (isMinimalAccent) {
                                    FloatingPill(
                                        backgroundColor = pillBackground,
                                        contentColor = selectionContentColor
                                    ) {
                                        titleContent()
                                    }
                                } else {
                                    titleContent()
                                }
                            }
                        },
                        navigationIcon = {
                            if (isMinimalAccent) {
                                FloatingIconPill(
                                    modifier = Modifier.padding(start = 8.dp),
                                    onClick = { contactsViewModel.clearContactSelection() },
                                    backgroundColor = pillBackground,
                                    contentColor = selectionContentColor
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = stringResource(R.string.cancel),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                IconButton(onClick = { contactsViewModel.clearContactSelection() }) {
                                    Icon(
                                        Icons.Default.Cancel,
                                        contentDescription = stringResource(R.string.cancel),
                                        tint = selectionContentColor
                                    )
                                }
                            }
                        },
                        actions = {
                            val allSelected =
                                selectedContacts.size == filteredContacts.size && filteredContacts.isNotEmpty()
                            if (isMinimalAccent) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    FloatingIconPill(
                                        onClick = {
                                            if (allSelected) {
                                                contactsViewModel.clearContactSelection()
                                            } else {
                                                contactsViewModel.selectAllContacts(filteredContacts)
                                            }
                                        },
                                        backgroundColor = pillBackground,
                                        contentColor = selectionContentColor
                                    ) {
                                        Icon(
                                            imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                            contentDescription = if (allSelected) stringResource(R.string.deselect_all) else stringResource(
                                                R.string.select_all
                                            ),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    FloatingIconPill(
                                        onClick = { showDeleteConfirm = true },
                                        backgroundColor = pillBackground,
                                        contentColor = selectionContentColor
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteSweep,
                                            contentDescription = stringResource(R.string.delete),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            } else {
                                IconButton(onClick = {
                                    if (allSelected) contactsViewModel.clearContactSelection() else contactsViewModel.selectAllContacts(
                                        filteredContacts
                                    )
                                }) {
                                    Icon(
                                        imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                        contentDescription = if (allSelected) stringResource(R.string.deselect_all) else stringResource(
                                            R.string.select_all
                                        ),
                                        tint = selectionContentColor
                                    )
                                }
                                IconButton(onClick = { showDeleteConfirm = true }) {
                                    Icon(
                                        Icons.Default.DeleteSweep,
                                        contentDescription = stringResource(R.string.delete),
                                        tint = selectionContentColor
                                    )
                                }
                            }
                        },
                        colors = colors,
                        scrollBehavior = if (accentBarStyle == AccentBarStyle.MODERN) null else scrollBehavior
                    )
                } else {
                    MainTopBar(
                        title = stringResource(R.string.contacts_title),
                        appSettingsViewModel = appSettingsViewModel,
                        count = filteredContacts.size,
                        scrollBehavior = scrollBehavior,
                        searchAction = if (contacts.isNotEmpty()) com.bookverse.contacts.ui.components.AppBarSearchAction(
                            stringResource(R.string.search_contacts), { isSearchActive = true },
                        ) else null,
                        titleContent = if (isSearchActive) {
                            {
                                TopBarSearchField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = stringResource(R.string.search_contacts),
                                    focusRequester = focusRequester,
                                    onClose = {
                                        searchQuery = ""
                                        focusManager.clearFocus(force = true)
                                        isSearchActive = false
                                    },
                                    leadingContentDescription = stringResource(R.string.search_contacts),
                                    clearContentDescription = stringResource(R.string.clear),
                                )
                            }
                        } else null,
                        actions = {
                            if (contacts.isNotEmpty()) {
                                IconAction(
                                    imageVector = Lucide.ArrowDownAZ,
                                    contentDescription = stringResource(R.string.contact_sort_title),
                                    onClick = { showSortSheet = true },
                                )
                            }
                            if (toolsOrder.toolsMenuHidden) {
                                IconAction(
                                    imageVector = Lucide.Settings,
                                    contentDescription = stringResource(R.string.settings),
                                    onClick = onNavigateToSettings
                                )
                            }
                        },
                        onCountClick = { showAccountsDialog = true },
                        solidWhenCollapsed = pinnedHeaderContent,
                    )
                }

                if (isLimitedMode && !(accentBarStyle == AccentBarStyle.MODERN && isSearchActive)) {
                    PermissionBanner(
                        onReviewPhoneStatus = onReviewPhoneStatus,
                        onChooseTools = onChooseTools,
                    )
                }

                if (allTags.isNotEmpty()) {
                    TagFilterBar(
                        tags = allTags,
                        selectedTagIds = selectedFilterTags,
                        onTagToggle = { contactTagViewModel.toggleFilterTag(it) },
                        onClearFilter = { contactTagViewModel.clearFilterTags() }
                    )
                }
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            if (contacts.isEmpty() && !isLimitedMode) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = accentColor.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(R.string.contacts_empty),
                        color = textColor.copy(alpha = 0.8f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.contacts_empty_description),
                        color = textColor.copy(alpha = 0.6f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(100.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = accentColor.copy(alpha = 0.2f)
                            ),
                            onClick = onCreateContact
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.create_contact_cta),
                                    color = textColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(100.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = accentColor.copy(alpha = 0.2f)
                            ),
                            onClick = {
                                restoreJsonLauncher.launch(arrayOf("application/json", *VCARD_OPEN_MIME_TYPES))
                            }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.extras_restore_contacts),
                                    color = textColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                if (filteredContacts.isEmpty() && !isLimitedMode) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.no_contacts_found),
                            color = textColor.copy(alpha = 0.7f),
                            fontSize = 20.sp,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(top = paddingValues.calculateTopPadding(), bottom = com.bookverse.contacts.ui.components.LocalMainContentBottomPadding.current),
                    ) {
                        if (groupedContacts.isNotEmpty()) {
                            groupedContacts.forEach { group ->
                                stickyHeader {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(backgroundColor)
                                            .padding(horizontal = 16.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = group.label,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                val hasMultiple = group.contacts.size > 1

                                itemsIndexed(
                                    group.contacts,
                                    key = { _, c -> c.id }) { index, contact ->
                                    val isSelected = selectedContacts.contains(contact.id)
                                    val isFirst = index == 0
                                    val isLast = index == group.contacts.lastIndex

                                    val shape = RoundedCornerShape(
                                        topStart = if (isFirst) 20.dp else 0.dp,
                                        topEnd = if (isFirst) 20.dp else 0.dp,
                                        bottomStart = if (isLast) 20.dp else 0.dp,
                                        bottomEnd = if (isLast) 20.dp else 0.dp
                                    )

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                start = 16.dp,
                                                end = 16.dp,
                                                top = 0.dp,
                                                bottom = if (isLast || hasMultiple) 2.dp else 0.dp
                                            ),
                                        shape = shape,
                                        color = MaterialTheme.colorScheme.surface
                                    ) {
                                        ContactItem(
                                            contact = contact,
                                            onContactClick = {
                                                if (selectedContacts.isNotEmpty()) {
                                                    contactsViewModel.toggleContactSelection(contact.id)
                                                } else {
                                                    onContactClick(contact)
                                                }
                                            },
                                            onLongPress = {
                                                contactsViewModel.toggleContactSelection(
                                                    contact.id
                                                )
                                            },
                                            isSelected = isSelected,
                                            accentColor = accentColor,
                                            textColor = textColor,
                                            searchQuery = searchQuery,
                                            contactsViewModel = contactsViewModel,
                                            listDensity = listDensity,
                                            tags = contactTagsMap[contact.id] ?: emptyList(),
                                        )
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(filteredContacts, key = { _, contact -> contact.id }) { index, contact ->
                                val isSelected = selectedContacts.contains(contact.id)
                                val isFirst = index == 0
                                val isLast = index == filteredContacts.lastIndex

                                val shape = RoundedCornerShape(
                                    topStart = if (isFirst) 20.dp else 0.dp,
                                    topEnd = if (isFirst) 20.dp else 0.dp,
                                    bottomStart = if (isLast) 20.dp else 0.dp,
                                    bottomEnd = if (isLast) 20.dp else 0.dp
                                )

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = 16.dp,
                                            end = 16.dp,
                                            top = if (isFirst) 8.dp else 0.dp,
                                            bottom = 4.dp
                                        ),
                                    shape = shape,
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    ContactItem(
                                        contact = contact,
                                        onContactClick = {
                                            if (selectedContacts.isNotEmpty()) {
                                                contactsViewModel.toggleContactSelection(contact.id)
                                            } else {
                                                onContactClick(contact)
                                            }
                                        },
                                        onLongPress = {
                                            contactsViewModel.toggleContactSelection(contact.id)
                                        },
                                        isSelected = isSelected,
                                        accentColor = accentColor,
                                        textColor = textColor,
                                        searchQuery = searchQuery,
                                        contactsViewModel = contactsViewModel,
                                        listDensity = if (searchQuery.isEmpty()) listDensity else ListDensity.NORMAL,
                                        tags = contactTagsMap[contact.id] ?: emptyList(),
                                    )
                                }
                            }
                        }
                    }

                    if (!isSearchActive && searchQuery.isEmpty() && alphabetSections.isNotEmpty() && contacts.size > 10) {
                        FastScroller(
                            listState = listState,
                            sections = alphabetSections,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 8.dp),
                            accentColor = accentColor,
                            isDate = false
                        )
                    }
                }
            }
        }

        if (showAccountsDialog) {
            val accountContactCounts = accounts.associateWith { account ->
                filteredContacts.count { contact ->
                    contact.accounts?.any { it.name == account.name } == true
                }
            }

            val knownAccountIds = accounts.map { it.name }.toSet()
            val unknownContactsCount = filteredContacts.count { contact ->
                contact.accounts.orEmpty().none { it.name in knownAccountIds }
            }
            val accountsSheetTitle = stringResource(R.string.contacts_accounts_sheet_title)
            val localAccountLabel = stringResource(R.string.local_account)
            val systemContactsLabel = stringResource(R.string.system_contacts)
            CountBreakdownSheet(
                title = accountsSheetTitle,
                rows = buildList {
                    accountContactCounts.forEach { (account, count) ->
                        add(
                            CountInfoRow(
                                icon = if (account.isLocal) {
                                    Icons.Default.PhoneAndroid
                                } else if (account.type == ContactAccount.WHATSAPP) {
                                    Icons.Default.Whatsapp
                                } else {
                                    Icons.Default.ContactMail
                                },
                                label = if (account.isLocal) {
                                    localAccountLabel
                                } else {
                                    account.displayName
                                },
                                count = count,
                            ),
                        )
                    }
                    add(
                        CountInfoRow(
                            icon = Icons.Default.ContactEmergency,
                            label = systemContactsLabel,
                            count = unknownContactsCount,
                        ),
                    )
                },
                accentColor = accentColor,
                onDismiss = { showAccountsDialog = false },
            )
        }

        if (showSortSheet) {
            ContactSortSheet(
                selected = sortOrder,
                onSelect = { order -> appSettingsViewModel.setContactSortOrder(context, order) },
                onDismiss = { showSortSheet = false },
            )
        }

        ContactVCardResultHost(
            result = vCardResult,
            isImporting = isImporting,
            onConsumed = contactsViewModel.vCard::clearResult,
            onMessage = { message, type -> snackbar.show(message, type) },
        )

        if (isImporting) {
            LoaderDialog(
                message = stringResource(R.string.importing_contacts),
            )
        }

        if (importResult != null && !isImporting) {
            MessageDialog(
                title = stringResource(R.string.import_success),
                message1 = pluralStringResource(
                    R.plurals.imported_count,
                    importResult ?: 0,
                    importResult ?: 0,
                ),
                onDismiss = { contactsViewModel.clearImportResult() },
                onButtonClick = { contactsViewModel.clearImportResult() },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactItem(
    contact: Contact,
    onContactClick: () -> Unit,
    onLongPress: () -> Unit,
    isSelected: Boolean,
    accentColor: Color,
    textColor: Color,
    searchQuery: String = "",
    contactsViewModel: ContactsViewModel,
    listDensity: ListDensity = ListDensity.NORMAL,
    tags: List<ContactTag> = emptyList(),
) {
    val isNumericSearch = searchQuery.isNotEmpty() && searchQuery.all { it.isDigit() }
    val photoUri = contact.photoUri
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = photoUri) {
        value = photoUri?.let { contactsViewModel.loadContactPhoto(it) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onContactClick,
                onLongClick = onLongPress
            )
            .padding(horizontal = 16.dp, vertical = listDensity.itemPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(listDensity.avatarSize)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                    .background(ContactUtils.getRandomColor(contact.name))
                ) {
                    if (photoUri != null) {
                        val loadedBitmap = bitmap
                        if (loadedBitmap != null) {
                            Image(
                                bitmap = loadedBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = contact.avatarLabel(),
                                color = textColor,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    } else {
                        Text(
                            text = contact.avatarLabel(),
                            color = textColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (searchQuery.isEmpty()) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.Normal,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else if (isNumericSearch) {
                Text(
                    text = ContactUtils.highlightText(
                        contact.phones.first().number,
                        searchQuery,
                        accentColor
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Text(
                    text = ContactUtils.highlightText(contact.name, searchQuery, accentColor),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            if (tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tags.take(3).forEach { tag ->
                        val tagColor = runCatching {
                            Color(tag.colorHex.toColorInt())
                        }.getOrElse { accentColor }
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(tagColor, CircleShape),
                        )
                    }
                }
            }
        }
    }
}
