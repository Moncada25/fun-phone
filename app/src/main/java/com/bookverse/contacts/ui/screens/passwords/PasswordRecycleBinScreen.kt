package com.bookverse.contacts.ui.screens.passwords

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
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookverse.contacts.R
import com.bookverse.contacts.data.password.PasswordCredential
import com.bookverse.contacts.ui.components.AppSnackbarHost
import com.bookverse.contacts.ui.components.ConfirmDialog
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.ui.components.rememberAppSnackbarState
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.PasswordsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordRecycleBinScreen(
    onBackClick: () -> Unit,
    passwordsViewModel: PasswordsViewModel,
    appSettingsViewModel: AppSettingsViewModel,
) {
    val deletedCredentials by passwordsViewModel.deletedCredentials.collectAsStateWithLifecycle()
    val folders by passwordsViewModel.folders.collectAsStateWithLifecycle()
    val accentColor = MaterialTheme.colorScheme.primary
    val resources = LocalResources.current
    val snackbar = rememberAppSnackbarState()
    var credentialToRestore by remember { mutableStateOf<PasswordCredential?>(null) }
    var credentialToDelete by remember { mutableStateOf<PasswordCredential?>(null) }
    var showEmptyConfirmation by remember { mutableStateOf(false) }
    var showStrengthSheet by remember { mutableStateOf(false) }
    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior()

    BackHandler { onBackClick() }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                expandable = true,
                title = stringResource(R.string.recycle_bin_title),
                count = deletedCredentials.size,
                onBackClick = onBackClick,
                onCountClick = { showStrengthSheet = true },
                actions = {
                    if (deletedCredentials.isNotEmpty()) {
                        IconAction(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = stringResource(R.string.empty_recycle_bin),
                            onClick = { showEmptyConfirmation = true },
                        )
                    }
                },
                appSettingsViewModel = appSettingsViewModel,
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { AppSnackbarHost(snackbar.hostState) },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
        ) {
            if (deletedCredentials.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(120.dp), tint = accentColor.copy(alpha = 0.6f))
                    Spacer(Modifier.height(24.dp))
                    Text(stringResource(R.string.recycle_bin_empty), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.password_recycle_bin_empty_description),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                state = com.bookverse.contacts.ui.components.rememberDestinationListState(),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(deletedCredentials, key = { it.id }) { credential ->
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
                                    modifier = Modifier.size(48.dp).background(accentColor.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = accentColor)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(credential.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    folders.firstOrNull { it.id == credential.folderId }?.let { folder ->
                                        Text(folder.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                IconButton(onClick = { credentialToRestore = credential }) {
                                    Icon(Icons.Default.RestoreFromTrash, contentDescription = stringResource(R.string.restore), tint = accentColor)
                                }
                                IconButton(onClick = { credentialToDelete = credential }) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = stringResource(R.string.delete_permanently), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    credentialToRestore?.let { credential ->
        ConfirmDialog(
            onDismiss = { credentialToRestore = null },
            onConfirm = {
                passwordsViewModel.restore(credential.id)
                credentialToRestore = null
                snackbar.show(resources.getString(R.string.password_restored, credential.name), SnackbarType.Success)
            },
            title = stringResource(R.string.restore),
            message = stringResource(R.string.restore_confirm, credential.name),
            toastMessage = null,
            confirmButtonLabel = stringResource(R.string.restore),
            isDestructive = false,
            appSettingsViewModel = appSettingsViewModel,
        )
    }
    credentialToDelete?.let { credential ->
        ConfirmDialog(
            onDismiss = { credentialToDelete = null },
            onConfirm = {
                passwordsViewModel.permanentlyDelete(credential.id)
                credentialToDelete = null
                snackbar.show(resources.getString(R.string.password_permanently_deleted, credential.name), SnackbarType.Success)
            },
            title = stringResource(R.string.delete_permanently),
            message = stringResource(R.string.delete_permanently_confirm, credential.name),
            toastMessage = null,
            appSettingsViewModel = appSettingsViewModel,
        )
    }
    if (showEmptyConfirmation) {
        ConfirmDialog(
            onDismiss = { showEmptyConfirmation = false },
            onConfirm = {
                deletedCredentials.forEach { passwordsViewModel.permanentlyDelete(it.id) }
                showEmptyConfirmation = false
                snackbar.show(resources.getString(R.string.recycle_bin_emptied), SnackbarType.Success)
            },
            title = stringResource(R.string.empty_recycle_bin),
            message = stringResource(R.string.empty_recycle_bin_confirm),
            toastMessage = null,
            appSettingsViewModel = appSettingsViewModel,
        )
    }

    if (showStrengthSheet) {
        PasswordStrengthCountsSheet(
            passwords = deletedCredentials.map { it.password },
            accentColor = accentColor,
            onDismiss = { showStrengthSheet = false },
        )
    }
}
