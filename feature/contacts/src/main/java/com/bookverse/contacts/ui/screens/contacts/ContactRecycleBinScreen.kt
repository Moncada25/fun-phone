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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookverse.contacts.data.model.DeletedContact
import com.bookverse.contacts.domain.contacts.ContactRecycleBinPolicy
import com.bookverse.contacts.feature.contacts.R
import com.bookverse.contacts.core.ui.R as CoreUiR
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import com.bookverse.contacts.ui.components.AppSnackbarHost
import com.bookverse.contacts.ui.components.ConfirmDialog
import com.bookverse.contacts.ui.components.ContactAvatarText
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior
import com.bookverse.contacts.ui.components.rememberAppSnackbarState
import com.bookverse.contacts.ui.components.rememberDestinationListState
import com.bookverse.contacts.ui.viewmodel.ContactRecycleBinEvent
import com.bookverse.contacts.ui.viewmodel.ContactRecycleBinViewModel
import com.bookverse.contacts.utils.contactAvatarLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactRecycleBinScreen(
    onBackClick: () -> Unit,
    recycleBinViewModel: ContactRecycleBinViewModel,
    onContactRestored: () -> Unit,
) {
    val deletedContacts by recycleBinViewModel.deletedContacts.collectAsStateWithLifecycle()
    val isWorking by recycleBinViewModel.isWorking.collectAsStateWithLifecycle()
    val accentColor = MaterialTheme.colorScheme.primary
    val resources = LocalResources.current
    val snackbar = rememberAppSnackbarState()
    var contactToRestore by remember { mutableStateOf<DeletedContact?>(null) }
    var contactToDelete by remember { mutableStateOf<DeletedContact?>(null) }
    var showEmptyConfirmation by remember { mutableStateOf(false) }
    val openedAt = remember { System.currentTimeMillis() }
    val scrollBehavior = rememberAppBarScrollBehavior()

    LaunchedEffect(Unit) {
        recycleBinViewModel.purgeExpired()
        recycleBinViewModel.events.collect { event ->
            when (event) {
                is ContactRecycleBinEvent.Restored -> {
                    onContactRestored()
                    snackbar.show(
                        resources.getString(
                            if (event.photoRestored) {
                                R.string.contact_restored
                            } else {
                                R.string.contact_restored_without_photo
                            },
                            event.name,
                        ),
                        if (event.photoRestored) SnackbarType.Success else SnackbarType.Warning,
                    )
                }

                is ContactRecycleBinEvent.RestoreFailed -> snackbar.show(
                    resources.getString(R.string.contact_restore_error, event.name),
                    SnackbarType.Error,
                )

                is ContactRecycleBinEvent.DeletedForever -> snackbar.show(
                    resources.getString(R.string.contact_permanently_deleted, event.name),
                    SnackbarType.Success,
                )

                ContactRecycleBinEvent.Emptied -> snackbar.show(
                    resources.getString(CoreUiR.string.recycle_bin_emptied),
                    SnackbarType.Success,
                )

                ContactRecycleBinEvent.DeleteFailed -> snackbar.show(
                    resources.getString(R.string.contact_recycle_bin_error),
                    SnackbarType.Error,
                )
            }
        }
    }

    BackHandler { onBackClick() }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                expandable = true,
                title = stringResource(CoreUiR.string.contact_recycle_bin_title),
                count = deletedContacts.size,
                onBackClick = onBackClick,
                actions = {
                    if (deletedContacts.isNotEmpty()) {
                        IconAction(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = stringResource(CoreUiR.string.empty_recycle_bin),
                            onClick = { showEmptyConfirmation = true },
                            enabled = !isWorking,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { AppSnackbarHost(snackbar.hostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
        ) {
            if (deletedContacts.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = accentColor.copy(alpha = 0.6f),
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        stringResource(CoreUiR.string.recycle_bin_empty),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        pluralStringResource(
                            R.plurals.contact_recycle_bin_empty_description,
                            ContactRecycleBinPolicy.RETENTION_DAYS,
                            ContactRecycleBinPolicy.RETENTION_DAYS,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    state = rememberDestinationListState(),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(deletedContacts, key = { it.id }) { entry ->
                        DeletedContactCard(
                            entry = entry,
                            daysRemaining = ContactRecycleBinPolicy
                                .daysRemaining(entry.deletedAt, openedAt)
                                .coerceAtLeast(1),
                            actionsEnabled = !isWorking,
                            onRestore = { contactToRestore = entry },
                            onDeleteForever = { contactToDelete = entry },
                        )
                    }
                }
            }
        }
    }

    contactToRestore?.let { entry ->
        ConfirmDialog(
            onDismiss = { contactToRestore = null },
            onConfirm = {
                recycleBinViewModel.restore(entry)
                contactToRestore = null
            },
            title = stringResource(CoreUiR.string.restore),
            message = stringResource(CoreUiR.string.restore_confirm, entry.displayLabel()),
            toastMessage = null,
            confirmButtonLabel = stringResource(CoreUiR.string.restore),
            isDestructive = false,
        )
    }
    contactToDelete?.let { entry ->
        ConfirmDialog(
            onDismiss = { contactToDelete = null },
            onConfirm = {
                recycleBinViewModel.deleteForever(entry)
                contactToDelete = null
            },
            title = stringResource(CoreUiR.string.delete_permanently),
            message = stringResource(CoreUiR.string.delete_permanently_confirm, entry.displayLabel()),
            toastMessage = null,
        )
    }
    if (showEmptyConfirmation) {
        ConfirmDialog(
            onDismiss = { showEmptyConfirmation = false },
            onConfirm = {
                recycleBinViewModel.emptyBin()
                showEmptyConfirmation = false
            },
            title = stringResource(CoreUiR.string.empty_recycle_bin),
            message = stringResource(CoreUiR.string.empty_recycle_bin_confirm),
            toastMessage = null,
        )
    }
}

private fun DeletedContact.displayLabel(): String =
    contact.name.ifBlank { contact.phones.firstOrNull()?.number.orEmpty() }

@Composable
private fun DeletedContactCard(
    entry: DeletedContact,
    daysRemaining: Int,
    actionsEnabled: Boolean,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val label = entry.displayLabel()
    val phoneNumber = entry.contact.phones.firstOrNull()?.number
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                ContactAvatarText(
                    text = label.contactAvatarLabel(),
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (phoneNumber != null && phoneNumber != label) {
                    Text(
                        phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    pluralStringResource(
                        R.plurals.contact_recycle_bin_days_left,
                        daysRemaining,
                        daysRemaining,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRestore, enabled = actionsEnabled) {
                Icon(
                    Icons.Default.RestoreFromTrash,
                    contentDescription = stringResource(CoreUiR.string.restore),
                    tint = accentColor,
                )
            }
            IconButton(onClick = onDeleteForever, enabled = actionsEnabled) {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = stringResource(CoreUiR.string.delete_permanently),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
