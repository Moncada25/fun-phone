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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.bookverse.contacts.feature.contacts.R
import com.bookverse.contacts.core.ui.R as CoreUiR
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookverse.contacts.data.model.ContactTag
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.TagFormDialog
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.ContactTagViewModel
import androidx.core.graphics.toColorInt
import com.bookverse.contacts.ui.components.ConfirmDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagerScreen(
    contactTagViewModel: ContactTagViewModel,
    appSettingsViewModel: AppSettingsViewModel,
    onBack: () -> Unit,
) {
    val allTags by contactTagViewModel.allTags.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedTag by remember { mutableStateOf<ContactTag?>(null) }

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
                title = stringResource(CoreUiR.string.tag_manager_title),
                onBackClick = onBack,
                actions = {
                    IconAction(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(CoreUiR.string.create_tag),
                        onClick = { showCreateDialog = true },
                    )
                },
            )
        },
    ) { padding ->
        if (allTags.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Label,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                    Text(
                        text = stringResource(R.string.tags_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.tags_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
        } else {
            LazyColumn(
                state = com.bookverse.contacts.ui.components.rememberDestinationListState(),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = allTags,
                    key = { it.id },
                ) { tag ->
                    TagListItem(
                        tag = tag,
                        onEdit = {
                            selectedTag = tag
                            showEditDialog = true
                        },
                        onDelete = {
                            selectedTag = tag
                            showDeleteDialog = true
                        },
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        TagFormDialog(
            tag = null,
            appSettingsViewModel = appSettingsViewModel,
            onDismiss = { showCreateDialog = false },
            onSave = { name, colorHex ->
                contactTagViewModel.createTag(name, colorHex)
                showCreateDialog = false
            },
        )
    }

    if (showEditDialog && selectedTag != null) {
        TagFormDialog(
            tag = selectedTag,
            appSettingsViewModel = appSettingsViewModel,
            onDismiss = {
                showEditDialog = false
                selectedTag = null
            },
            onSave = { name, colorHex ->
                selectedTag?.let { tag ->
                    contactTagViewModel.updateTag(tag.copy(name = name, colorHex = colorHex))
                }
                showEditDialog = false
                selectedTag = null
            },
        )
    }

    if (showDeleteDialog && selectedTag != null) {


        ConfirmDialog(
            onDismiss = {
                showDeleteDialog = false
                selectedTag = null
            },
            onConfirm = {
                selectedTag?.let { contactTagViewModel.deleteTag(it) }
                showDeleteDialog = false
                selectedTag = null
            },
            title = stringResource(CoreUiR.string.delete_tag),
            message = stringResource(R.string.delete_tag_confirmation, selectedTag?.name ?: ""),
        )
    }
}

@Composable
private fun TagListItem(
    tag: ContactTag,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(tag.colorHex.toColorInt())),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = tag.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                ),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(CoreUiR.string.edit_tag),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(CoreUiR.string.delete_tag),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
