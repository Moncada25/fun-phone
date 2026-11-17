package com.bookverse.contacts.ui.screens.contacts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.bookverse.contacts.R
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.DuplicateGroup
import com.bookverse.contacts.data.model.MatchType
import com.bookverse.contacts.ui.components.AppSnackbarHost
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.ui.components.rememberAppSnackbarState
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.DuplicateContactsViewModel
import com.bookverse.contacts.ui.viewmodel.DuplicateEvent
import com.bookverse.contacts.utils.avatarLabel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ScanSearch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateContactsScreen(
    contacts: List<Contact>,
    duplicateContactsViewModel: DuplicateContactsViewModel,
    appSettingsViewModel: AppSettingsViewModel,
    onBack: () -> Unit,
    onContactsMerged: () -> Unit = {},
) {
    val duplicateGroups by duplicateContactsViewModel.duplicateGroups.collectAsStateWithLifecycle()
    val isScanning by duplicateContactsViewModel.isScanning.collectAsStateWithLifecycle()
    val scanComplete by duplicateContactsViewModel.scanComplete.collectAsStateWithLifecycle()
    val mergeInProgress by duplicateContactsViewModel.mergeInProgress.collectAsStateWithLifecycle()
    val accentColor = MaterialTheme.colorScheme.primary
    val resources = LocalResources.current

    val snackbarState = rememberAppSnackbarState()

    var showMergeDialog by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf<DuplicateGroup?>(null) }

    val mergeSuccessMsg = stringResource(R.string.merge_success)
    val mergeErrorMsg = stringResource(R.string.merge_error)
    val ignoredMsg = stringResource(R.string.ignored_duplicate_success)
    val noDuplicatesMsg = stringResource(R.string.scan_no_duplicates)

    LaunchedEffect(contacts) {
        if (!scanComplete && contacts.isNotEmpty()) {
            duplicateContactsViewModel.scanForDuplicates(contacts)
        }
    }

    LaunchedEffect(Unit) {
        duplicateContactsViewModel.events.collect { event ->
            when (event) {
                is DuplicateEvent.ScanComplete -> {
                    if (event.count > 0) {
                        snackbarState.show(
                            message = resources.getQuantityString(
                                R.plurals.duplicates_found_count,
                                event.count,
                                event.count,
                            ),
                            type = SnackbarType.Info,
                            accentColor = accentColor,
                        )
                    } else {
                        snackbarState.show(
                            message = noDuplicatesMsg,
                            type = SnackbarType.Success,
                            accentColor = accentColor,
                        )
                    }
                }
                is DuplicateEvent.MergeSuccess -> {
                    snackbarState.show(
                        message = mergeSuccessMsg,
                        type = SnackbarType.Success,
                        accentColor = accentColor,
                    )
                }
                is DuplicateEvent.MergeError -> {
                    snackbarState.show(
                        message = mergeErrorMsg,
                        type = SnackbarType.Error,
                        accentColor = accentColor,
                    )
                }
                is DuplicateEvent.IgnoreSuccess -> {
                    snackbarState.show(
                        message = ignoredMsg,
                        type = SnackbarType.Info,
                        accentColor = accentColor,
                    )
                }
            }
        }
    }

    BackHandler {
        onBack()
    }

    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                expandable = true,
                scrollBehavior = scrollBehavior,
                title = stringResource(R.string.duplicate_contacts_title),
                onBackClick = onBack,
                appSettingsViewModel = appSettingsViewModel,
                actions = {
                    IconAction(
                        imageVector = Lucide.ScanSearch,
                        contentDescription = stringResource(R.string.scan_duplicates),
                        onClick = { duplicateContactsViewModel.scanForDuplicates(contacts) },
                    )
                },
            )
        },
        snackbarHost = { AppSnackbarHost(state = snackbarState.hostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                isScanning -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            CircularProgressIndicator(
                                color = accentColor,
                            )
                            Text(
                                text = stringResource(R.string.scanning_duplicates),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                scanComplete && duplicateGroups.isEmpty() -> {
                    EmptyStateView(
                        icon = Icons.Default.CheckCircle,
                        title = stringResource(R.string.no_duplicates_found),
                        subtitle = stringResource(R.string.no_duplicates_hint),
                        buttonText = stringResource(R.string.scan_duplicates),
                        onButtonClick = { duplicateContactsViewModel.scanForDuplicates(contacts) },
                        accentColor = accentColor,
                    )
                }

                else -> {
                    LazyColumn(
                state = com.bookverse.contacts.ui.components.rememberDestinationListState(),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = duplicateGroups,
                            key = { group -> group.contacts.joinToString("-") { it.id } },
                        ) { group ->
                            DuplicateGroupCard(
                                group = group,
                                accentColor = accentColor,
                                onMergeClick = {
                                    selectedGroup = group
                                    showMergeDialog = true
                                },
                                onIgnoreClick = {
                                    duplicateContactsViewModel.ignoreDuplicateGroup(group)
                                },
                            )
                        }
                    }
                }
            }

            if (mergeInProgress) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = accentColor,
                    )
                }
            }
        }
    }

    if (showMergeDialog && selectedGroup != null) {
        MergeDialog(
            group = selectedGroup!!,
            accentColor = accentColor,
            onConfirm = { primaryContactId ->
                duplicateContactsViewModel.mergeContacts(
                    group = selectedGroup!!,
                    primaryContactId = primaryContactId,
                    onMerged = onContactsMerged,
                )
                showMergeDialog = false
                selectedGroup = null
            },
            onDismiss = {
                showMergeDialog = false
                selectedGroup = null
            },
        )
    }
}

@Composable
private fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    accentColor: androidx.compose.ui.graphics.Color,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = accentColor.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onButtonClick,
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(18.dp),
        ) {
            Icon(
                imageVector = Lucide.ScanSearch,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = buttonText,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    accentColor: androidx.compose.ui.graphics.Color,
    onMergeClick: () -> Unit,
    onIgnoreClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = when (group.matchType) {
                                MatchType.EXACT_NAME -> stringResource(R.string.match_exact_name)
                                MatchType.SIMILAR_NAME -> stringResource(R.string.match_similar_name)
                                MatchType.PHONE_MATCH -> stringResource(R.string.match_phone)
                                MatchType.NAME_AND_PHONE -> stringResource(R.string.match_name_and_phone)
                            },
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                )
                Text(
                    text = stringResource(R.string.confidence_label, (group.confidence * 100).toInt()),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            group.contacts.forEach { contact ->
                ContactRow(contact = contact, accentColor = accentColor)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onMergeClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = stringResource(R.string.merge_contacts),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                TextButton(
                    onClick = onIgnoreClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.ignore_duplicates),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: Contact,
    accentColor: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (contact.photoUri != null) {
            AsyncImage(
                model = contact.photoUri,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            val initials = contact.avatarLabel()
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (contact.phones.isNotEmpty()) {
                Text(
                    text = contact.phones.first().number,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            contact.email?.let { email ->
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MergeDialog(
    group: DuplicateGroup,
    accentColor: androidx.compose.ui.graphics.Color,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedContactId by remember { mutableStateOf(group.contacts.firstOrNull()?.id ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.select_primary_contact),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.merge_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(16.dp))
                group.contacts.forEach { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(
                                selected = selectedContactId == contact.id,
                                onClick = { selectedContactId = contact.id },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(
                            selected = selectedContactId == contact.id,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = accentColor,
                            ),
                        )
                        if (contact.photoUri != null) {
                            AsyncImage(
                                model = contact.photoUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            val initials = contact.avatarLabel()
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contact.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (contact.phones.isNotEmpty()) {
                                Text(
                                    text = contact.phones.first().number,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedContactId) },
                enabled = selectedContactId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.merge_contacts),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
