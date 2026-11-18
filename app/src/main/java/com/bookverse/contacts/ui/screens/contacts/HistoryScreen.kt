package com.bookverse.contacts.ui.screens.contacts

import com.bookverse.contacts.ui.theme.SecondaryTextAlpha
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallMissedOutgoing
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Whatsapp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.RecentType
import com.bookverse.contacts.data.model.historyKey
import com.bookverse.contacts.ui.components.AppSnackbarHost
import com.bookverse.contacts.ui.components.ConfirmDialog
import com.bookverse.contacts.ui.components.FastScroller
import com.bookverse.contacts.ui.components.FloatingIconPill
import com.bookverse.contacts.ui.components.FloatingPill
import com.bookverse.contacts.ui.components.HistoryItem
import com.bookverse.contacts.ui.components.MainTopBar
import com.bookverse.contacts.ui.components.OptionsDialog
import com.bookverse.contacts.ui.components.PermissionBanner
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.ui.components.TopBarSearchField
import com.bookverse.contacts.ui.components.rememberAppSnackbarState
import com.bookverse.contacts.ui.components.topBarBackground
import com.bookverse.contacts.ui.components.topBarColors
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.theme.AccentBarStyle
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.ui.viewmodel.NavigationViewModel
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.utils.ContactUtils
import com.bookverse.contacts.utils.ContactUtils.toRelativeDate
import com.bookverse.contacts.utils.SecondaryScreen
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    appSettingsViewModel: AppSettingsViewModel,
    contactsViewModel: ContactsViewModel,
    navigationViewModel: NavigationViewModel,
    onDialerPhoneNumberChange: (String) -> Unit,
    onContactClick: ((Contact) -> Unit)? = null,
    isLimitedMode: Boolean = false,
    listState: LazyListState
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = colorScheme.background
    val textColor = colorScheme.onBackground
    val secondaryTextColor = textColor.copy(alpha = SecondaryTextAlpha)
    val localOperations by contactsViewModel.localOperations.collectAsStateWithLifecycle()
    val callLogs by contactsViewModel.callLogs.collectAsStateWithLifecycle()
    val hasMoreCallLogs by contactsViewModel.hasMoreCallLogs.collectAsStateWithLifecycle()
    val isLoadingMoreCallLogs by contactsViewModel.isLoadingMoreCallLogs.collectAsStateWithLifecycle()
    val blockedNumbers by contactsViewModel.blockedNumbers.collectAsStateWithLifecycle()
    val contacts by contactsViewModel.contacts.collectAsStateWithLifecycle()
    val selectedOperations by contactsViewModel.selectedRecentOperations.collectAsStateWithLifecycle()
    val accentColor = MaterialTheme.colorScheme.primary
    val accentBarStyle by appSettingsViewModel.accentBarStyle.collectAsStateWithLifecycle()
    val locale = LocaleController.effectiveLocale(context)
    val dateFormat = SimpleDateFormat("EEE dd/MM/yyyy", locale)
    val callLogPermissions =
        arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG)

    var hasCallLogPermissions by remember {
        mutableStateOf(callLogPermissions.all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        })
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            hasCallLogPermissions = permissions.all { it.value }
            if (hasCallLogPermissions) {
                contactsViewModel.fetchAllRecentOperations()
            }
        }

    LaunchedEffect(hasCallLogPermissions) {
        if (!hasCallLogPermissions) {
            if (!isLimitedMode) {
                permissionLauncher.launch(callLogPermissions)
            }
        } else {
            contactsViewModel.fetchAllRecentOperations()
        }
    }

    LaunchedEffect(listState, hasMoreCallLogs, isLoadingMoreCallLogs, hasCallLogPermissions) {
        if (!hasCallLogPermissions) return@LaunchedEffect
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            totalItems to lastVisible
        }
            .distinctUntilChanged()
            .collect { (totalItems, lastVisible) ->
                val shouldLoadMore = totalItems > 0 && lastVisible >= totalItems - 3
                if (shouldLoadMore && hasMoreCallLogs && !isLoadingMoreCallLogs) {
                    contactsViewModel.loadMoreRecentOperations()
                }
            }
    }

    val filterOptions = listOf(
        Triple(
            RecentType.CALL_RECEIVED,
            Icons.AutoMirrored.Filled.CallReceived,
            stringResource(R.string.recent_type_call_received)
        ),
        Triple(
            RecentType.CALL_SENT,
            Icons.AutoMirrored.Filled.CallMade,
            stringResource(R.string.recent_type_call_sent)
        ),
        Triple(
            RecentType.CALL_MISSED,
            Icons.AutoMirrored.Filled.CallMissed,
            stringResource(R.string.recent_type_call_missed)
        ),
        Triple(
            RecentType.CALL_REJECTED,
            Icons.AutoMirrored.Filled.CallMissedOutgoing,
            stringResource(R.string.recent_type_call_rejected)
        ),
        Triple(
            RecentType.CALL_BLOCKED,
            Icons.Filled.HideSource,
            stringResource(R.string.recent_type_call_blocked)
        ),
        Triple(
            RecentType.WHATSAPP_OPEN,
            Icons.Default.Whatsapp,
            stringResource(R.string.recent_type_whatsapp)
        ),
        Triple(
            RecentType.CALL_UNKNOWN,
            Icons.Default.DeviceUnknown,
            stringResource(R.string.recent_type_call_unknown)
        )
    )
    var selectedFilter by remember { mutableStateOf<RecentType?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior { !isSearchActive && selectedOperations.isEmpty() }
    val showTitle by remember(accentBarStyle) { derivedStateOf { accentBarStyle == AccentBarStyle.MODERN || scrollBehavior.state.collapsedFraction < 0.995f } }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    val historyProjection = remember(localOperations, callLogs, selectedFilter, searchQuery) {
        buildHistoryProjection(
            localOperations = localOperations,
            callLogs = callLogs,
            selectedFilter = selectedFilter,
            searchQuery = searchQuery,
            formatDate = { timestamp -> dateFormat.format(Date(timestamp)) },
        )
    }
    val allRecents = historyProjection.allRecents
    val filteredRecents = historyProjection.filteredRecents
    val groupedOperations = historyProjection.groupedOperations

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDeleteContactRecentsConfirm by remember { mutableStateOf(false) }
    var deleteResultCount by remember { mutableStateOf<Int?>(null) }
    var pendingDeleteCount by remember { mutableIntStateOf(0) }
    var contactToDeleteRecents by remember { mutableStateOf<Contact?>(null) }
    var recentContact by remember { mutableStateOf<Contact?>(null) }
    var showPhoneSelectionDialog by remember { mutableStateOf(false) }
    var phoneAction by remember { mutableStateOf<(String) -> Unit>({}) }

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedContactId by remember { mutableStateOf("") }
    var selectedPhoneNumber by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }

    val snackbar = rememberAppSnackbarState()
    val optimisticBlockStates = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(Unit) {
        contactsViewModel.snackbarEvents.collect { event ->
            snackbar.show(
                message = event.message,
                type = event.kind,
                actionLabel = event.actionLabel
            )
        }
    }

    LaunchedEffect(contacts, blockedNumbers) {
        val actualStates = mutableMapOf<String, Boolean>()
        contacts.forEach { actualStates[it.id] = it.isBlocked }
        blockedNumbers.forEach { info ->
            val raw = info.normalizedNumber ?: info.displayNumber
            val norm = normalizeRecentPhoneNumber(raw)
            val digits = recentDigitsOnly(norm) ?: recentDigitsOnly(raw)
            val keys = buildSet {
                info.contact?.id?.takeIf { it.isNotBlank() }?.let { add(it) }
                norm?.let { add(it) }
                digits?.let { add(it) }
                if (digits != null) {
                    if (digits.length > 10) add(digits.takeLast(10))
                    if (digits.length > 7) add(digits.takeLast(7))
                }
            }
            keys.forEach { key -> actualStates[key] = true }
        }

        val keysSnapshot = optimisticBlockStates.keys.toList()
        keysSnapshot.forEach { key ->
            val optimisticValue = optimisticBlockStates[key]
            val actual = actualStates[key] ?: false
            if (optimisticValue == actual) {
                optimisticBlockStates.remove(key)
            } else {
                optimisticBlockStates[key] = actual
            }
        }
    }

    val contactOperations =
        remember(allRecents, selectedContactId, selectedPhoneNumber, selectedDate) {
            if (selectedContactId.isNotEmpty()) {
                allRecents.filter { it.contact.id == selectedContactId && dateFormat.format(Date(it.date)) == selectedDate }
                    .sortedByDescending { it.date }
            } else if (selectedPhoneNumber.isNotEmpty()) {
                allRecents.filter {
                    it.contact.id.isEmpty() && it.contact.phones.firstOrNull()?.number == selectedPhoneNumber && dateFormat.format(
                        Date(it.date)
                    ) == selectedDate
                }.sortedByDescending { it.date }
            } else emptyList()
        }

    if (showDeleteConfirm) {
        ConfirmDialog(
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                pendingDeleteCount = selectedOperations.size
                showDeleteConfirm = false
                contactsViewModel.deleteSelectedRecentOperations()
            },
            title = stringResource(R.string.delete_recents_title),
            message = stringResource(R.string.delete_recents_message),
            toastMessage = null,
            appSettingsViewModel = appSettingsViewModel
        )
    }

    if (showDeleteContactRecentsConfirm && contactToDeleteRecents != null) {
        val unknown = stringResource(R.string.unknown)
        var title: String
        var message: String

        if (unknown != contactToDeleteRecents!!.name) {
            title = stringResource(
                R.string.delete_contact_recents_title,
                ContactUtils.getFirstName(contactToDeleteRecents!!.name)
            )
            message = stringResource(
                R.string.delete_contact_recents_message,
                ContactUtils.getFirstName(contactToDeleteRecents!!.name)
            )
        } else {
            title = stringResource(R.string.delete_unknown_recents_title)
            message = stringResource(R.string.delete_unknown_recents_message)
        }

        ConfirmDialog(
            onDismiss = { showDeleteContactRecentsConfirm = false; contactToDeleteRecents = null },
            onConfirm = {
                if (contactOperations.isNotEmpty()) {
                    contactsViewModel.deleteRecentOperations(contactOperations)
                }
                showDeleteContactRecentsConfirm = false
                showBottomSheet = false
                contactToDeleteRecents = null
                pendingDeleteCount = contactOperations.size
            },
            title = title,
            message = message,
            toastMessage = null,
            appSettingsViewModel = appSettingsViewModel
        )
    }

    if (pendingDeleteCount > 0 && deleteResultCount == null) {
        deleteResultCount = pendingDeleteCount
    }

    LaunchedEffect(deleteResultCount) {
        val count = deleteResultCount
        if (count != null) {
            snackbar.show(
                message = resources.getString(R.string.delete_recents_success),
                type = SnackbarType.Success
            )
            deleteResultCount = null
            pendingDeleteCount = 0
        }
    }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
        focusManager.clearFocus(force = true)
    }

    BackHandler(enabled = selectedOperations.isNotEmpty()) {
        contactsViewModel.clearRecentOperationSelection()
    }

    var showTypeCountsSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                val pinnedHeaderContent = isLimitedMode || (allRecents.isNotEmpty() && showFilters)
                if (selectedOperations.isNotEmpty()) {
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
                                val count = selectedOperations.size
                                val label =
                                    if (count == 1) stringResource(R.string.selected_single) else stringResource(
                                        R.string.selected_plural
                                    )
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
                                    onClick = { contactsViewModel.clearRecentOperationSelection() },
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
                                IconButton(onClick = { contactsViewModel.clearRecentOperationSelection() }) {
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
                                selectedOperations.size == allRecents.size && allRecents.isNotEmpty()
                            if (isMinimalAccent) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    FloatingIconPill(
                                        onClick = {
                                            if (allSelected) {
                                                contactsViewModel.clearRecentOperationSelection()
                                            } else {
                                                contactsViewModel.selectAllRecentOperations(
                                                    allRecents
                                                )
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
                                        onClick = {
                                            showDeleteConfirm = true
                                        },
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
                                    if (allSelected) contactsViewModel.clearRecentOperationSelection() else contactsViewModel.selectAllRecentOperations(
                                        allRecents
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
                                IconButton(onClick = {
                                    showDeleteConfirm = true
                                }) {
                                    Icon(
                                        Icons.Default.DeleteSweep,
                                        contentDescription = stringResource(R.string.delete),
                                        tint = selectionContentColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        },
                        colors = colors,
                        scrollBehavior = if (accentBarStyle == AccentBarStyle.MODERN) null else scrollBehavior
                    )
                } else {
                    MainTopBar(
                        title = stringResource(R.string.recents_title),
                        count = if (selectedFilter != null || searchQuery.isNotBlank()) filteredRecents.size else allRecents.size,
                        appSettingsViewModel = appSettingsViewModel,
                        scrollBehavior = scrollBehavior,
                        searchAction = if (allRecents.isNotEmpty()) com.bookverse.contacts.ui.components.AppBarSearchAction(
                            stringResource(R.string.search_history), { isSearchActive = true },
                        ) else null,
                        titleContent = if (isSearchActive) {
                            {
                                TopBarSearchField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = stringResource(R.string.search_history),
                                    focusRequester = focusRequester,
                                    onClose = {
                                        searchQuery = ""
                                        focusManager.clearFocus(force = true)
                                        isSearchActive = false
                                    },
                                    leadingContentDescription = stringResource(R.string.search_history),
                                    clearContentDescription = stringResource(R.string.clear),
                                )
                            }
                        } else {
                            null
                        },
                        actions = {
                            if (allRecents.isNotEmpty()) {
                                IconAction(
                                    imageVector = if (showFilters) Icons.Default.Cancel else Icons.Default.FilterList,
                                    contentDescription = stringResource(R.string.filter_history),
                                    onClick = {
                                        showFilters = !showFilters
                                        if (!showFilters) {
                                            selectedFilter = null
                                        }
                                    }
                                )
                            }
                        },
                        onCountClick = { showTypeCountsSheet = true },
                        solidWhenCollapsed = pinnedHeaderContent,
                    )
                }
                if (isLimitedMode && !(accentBarStyle == AccentBarStyle.MODERN && isSearchActive)) {
                    PermissionBanner(
                        onReviewPhoneStatus = { navigationViewModel.openSecondary(SecondaryScreen.PHONE_STATUS) },
                        onChooseTools = { navigationViewModel.openSecondary(SecondaryScreen.TOOLS_ORDER) },
                    )
                }
                AnimatedVisibility(
                    visible = allRecents.isNotEmpty() && showFilters,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(Modifier.background(backgroundColor)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            filterOptions.forEach { (type, icon, label) ->
                                val isSelected = selectedFilter == type
                                IconButton(
                                    onClick = { selectedFilter = if (isSelected) null else type },
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) accentColor.copy(
                                                alpha = 0.2f
                                            ) else Color.Transparent,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) accentColor else textColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(
                            color = secondaryTextColor.copy(alpha = 0.2f),
                            thickness = 1.dp
                        )
                    }
                }
            }
        },
        snackbarHost = { AppSnackbarHost(snackbar.hostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            if (allRecents.isEmpty() && !isLimitedMode) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = accentColor.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.history_empty_title),
                        color = textColor.copy(alpha = 0.8f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.history_empty_description),
                        color = textColor.copy(alpha = 0.6f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            } else if (
                filteredRecents.isEmpty() &&
                (selectedFilter != null || searchQuery.isNotBlank())
            ) {
                HistoryNoResults(
                    isSearch = searchQuery.isNotBlank(),
                    accentColor = accentColor,
                    textColor = textColor,
                )
            } else {
                val dateSections = remember(groupedOperations, locale) {
                    var index = 0
                    groupedOperations.map { (date, operations) ->
                        val inputFormat = SimpleDateFormat("EEE dd/MM/yyyy", locale)
                        val outputFormat = SimpleDateFormat("dd/MM/yy", locale)
                        val d = inputFormat.parse(date)
                        val section = outputFormat.format(d!!) to index
                        index += 2 + operations.size
                        section
                    }
                }
                LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(top = paddingValues.calculateTopPadding(), bottom = com.bookverse.contacts.ui.components.LocalMainContentBottomPadding.current),
                    ) {
                    groupedOperations.forEach { (date, operations) ->
                        item { Spacer(modifier = Modifier.height(6.dp)) }
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth(0.5f)
                                        .background(
                                            color = accentColor.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(4.dp)
                                        ),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = ContactUtils.capitalizeFirstLetter(
                                            date.toRelativeDate(context)
                                        ),
                                        color = textColor.copy(alpha = 0.8f),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        val hasMultiple = operations.size > 1
                        itemsIndexed(operations, key = { _, op -> op.historyKey }) { index, op ->
                            val isSelected = selectedOperations.contains(op.historyKey)
                            val isFirst = index == 0
                            val isLast = index == operations.lastIndex
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
                                        top = if (isFirst) 6.dp else 0.dp,
                                        bottom = if (isLast || hasMultiple) 2.dp else 0.dp
                                    ),
                                shape = shape,
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                RecentOperationItem(
                                    operation = op,
                                    textColor = textColor,
                                    isSelected = isSelected,
                                    accentColor = accentColor,
                                    onOperationClick = {
                                        if (selectedOperations.isNotEmpty()) {
                                            contactsViewModel.toggleRecentOperationSelection(op)
                                        } else {
                                            selectedContactId = op.contact.id
                                            selectedPhoneNumber =
                                                op.contact.phones.firstOrNull()?.number ?: ""
                                            selectedDate = date
                                            showBottomSheet = true
                                        }
                                    },
                                    onLongPress = {
                                        contactsViewModel.toggleRecentOperationSelection(op)
                                    },
                                    contactsViewModel = contactsViewModel,
                                    secondaryTextColor = secondaryTextColor
                                )
                            }
                        }
                    }
                    if (isLoadingMoreCallLogs || hasMoreCallLogs) {
                        item("history_load_more_indicator") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoadingMoreCallLogs) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = accentColor,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = stringResource(R.string.history_loading_more),
                                            color = secondaryTextColor,
                                            fontSize = 12.sp
                                        )
                                    }
                                } else if (hasMoreCallLogs && !isLimitedMode) {
                                    Text(
                                        text = stringResource(R.string.history_load_more_hint),
                                        color = secondaryTextColor,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
                if (groupedOperations.size >= 10) {
                    FastScroller(
                        listState = listState,
                        sections = dateSections,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp),
                        accentColor = accentColor,
                        isDate = true
                    )
                }

            }
        }
    }
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                val contact = contactOperations.firstOrNull()?.contact

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(0.5f), horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (contact != null && onContactClick != null) {
                                Text(
                                    text = contact.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .clickable {
                                            if (contact.id.isNotEmpty()) {
                                                onContactClick(contact); showBottomSheet = false
                                            }
                                        })
                            } else {
                                Text(
                                    text = contact?.name ?: "",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    if (contact != null) {
                        val normalizedNumbers =
                            contact.phones.mapNotNull {
                                normalizeRecentPhoneNumber(it.number)
                            }.toSet()
                        val blockKey = contact.id.takeIf { it.isNotBlank() }
                            ?: normalizedNumbers.firstOrNull()?.let {
                                recentDigitsOnly(it) ?: it
                            }
                        val overrideState = blockKey?.let { optimisticBlockStates[it] }
                        val contactFromState = if (contact.id.isNotBlank()) {
                            contacts.find { it.id == contact.id }
                        } else {
                            null
                        }
                        val contactBlockedFromData =
                            (contactFromState?.isBlocked ?: contact.isBlocked) ||
                                    (normalizedNumbers.isNotEmpty() && blockedNumbers.any { info ->
                                        val candidate = info.normalizedNumber ?: info.displayNumber
                                        normalizedNumbers.any { num ->
                                            recentNumbersMatch(
                                                num,
                                                candidate
                                            )
                                        }
                                    })
                        val contactIsBlocked = overrideState ?: contactBlockedFromData
                        val blockTint =
                            if (contactIsBlocked) MaterialTheme.colorScheme.error else accentColor

                        Column(
                            modifier = Modifier.weight(0.5f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (!contact.phones.getOrNull(1)?.number.isNullOrBlank()) {
                                            phoneAction = { phoneNumber ->
                                                contactsViewModel.addRecentOperation(
                                                    contact = contact,
                                                    type = RecentType.WHATSAPP_OPEN,
                                                    target = phoneNumber
                                                )
                                                ContactUtils.openWhatsApp(context, phoneNumber)
                                            }
                                            recentContact = contact
                                            showPhoneSelectionDialog = true
                                        } else {
                                            contactsViewModel.addRecentOperation(
                                                contact = contact,
                                                type = RecentType.WHATSAPP_OPEN,
                                                target = contact.phones.first().number
                                            ); ContactUtils.openWhatsApp(
                                                context,
                                                contact.phones.first().number
                                            )
                                        }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Whatsapp,
                                        contentDescription = stringResource(R.string.whatsapp_action),
                                        tint = accentColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (contactIsBlocked) {
                                            blockKey?.let { optimisticBlockStates[it] = false }
                                            if (contact.id.isNotBlank()) {
                                                contactsViewModel.unblockContact(contact)
                                            } else {
                                                val primaryNumber =
                                                    contact.phones.firstOrNull()?.number
                                                val normalized =
                                                    normalizeRecentPhoneNumber(primaryNumber)
                                                if (normalized != null) {
                                                    contactsViewModel.unblockNumber(normalized)
                                                } else {
                                                    snackbar.show(
                                                        message = resources.getString(R.string.contact_block_no_numbers),
                                                        type = SnackbarType.Warning
                                                    )
                                                }
                                            }
                                        } else {
                                            blockKey?.let { optimisticBlockStates[it] = true }
                                            contactsViewModel.blockContact(contact)
                                        }
                                        showBottomSheet = false
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = if (contactIsBlocked) {
                                            stringResource(R.string.contact_unblock_action)
                                        } else {
                                            stringResource(R.string.contact_block_action)
                                        },
                                        tint = blockTint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        contactToDeleteRecents = contact
                                        showDeleteContactRecentsConfirm = true
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    items(contactOperations, key = { it.historyKey }) { op ->
                        HistoryItem(
                            operation = op,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor,
                            onEditBeforeDial = {
                                onDialerPhoneNumberChange(it)
                                navigationViewModel.openSecondary(SecondaryScreen.DIALER)
                                showBottomSheet = false
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    if (showPhoneSelectionDialog) {
        val phoneOptions = listOf(
            "#1: ${recentContact!!.phones.getOrNull(0)?.number ?: ""}",
            "#2: ${recentContact!!.phones.getOrNull(1)?.number ?: ""}"
        )
        OptionsDialog(
            title = stringResource(R.string.select_phone_number),
            options = phoneOptions,
            onOptionSelected = { index: Int ->
                when (index) {
                    0 -> phoneAction(recentContact!!.phones.getOrNull(0)?.number ?: "")
                    1 -> phoneAction(recentContact!!.phones.getOrNull(1)?.number ?: "")
                }
                showPhoneSelectionDialog = false
            },
            onDismiss = { showPhoneSelectionDialog = false },
        )
    }
    if (showTypeCountsSheet) {
        val recentsToCount = if (selectedFilter != null) filteredRecents else allRecents
        val typeCounts = filterOptions.map { (type, icon, label) ->
            HistoryTypeCount(
                icon = icon,
                label = label,
                count = recentsToCount.count { it.type == type },
            )
        }
        HistoryTypeCountsSheet(
            typeCounts = typeCounts,
            accentColor = accentColor,
            onDismiss = { showTypeCountsSheet = false },
        )
    }
}
