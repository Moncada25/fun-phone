package com.bookverse.contacts.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.settings.ListDensity
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.ui.utils.rememberHapticFeedback
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Tags
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit),
    onShareClick: (() -> Unit)?,
    onSaveClick: (() -> Unit)?,
    onLockClick: (() -> Unit)?,
    isLocked: Boolean = false,
    isPinned: Boolean = false,
    onPinClick: (() -> Unit)? = null,
    listDensity: ListDensity = ListDensity.NORMAL,
    createdAt: Long = 0L,
    updatedAt: Long = 0L,
    timestamp: Long = System.currentTimeMillis(),
    subtitle: String? = null,
    isNormalNote: Boolean = false,
    onTransformToAudio: (() -> Unit)? = null,
    onMoveToFolderClick: (() -> Unit)? = null,
    restoreActionIcon: ImageVector? = null,
    restoreActionDescription: String? = null,
    onRestoreActionClick: (() -> Unit)? = null,
    replaceDeleteWithCustom: Boolean = false,
    deleteForeverActionIcon: ImageVector? = null,
    deleteForeverActionDescription: String? = null,
    onTagsClick: (() -> Unit)? = null,
    tagsContent: (@Composable () -> Unit)? = null,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val performHaptic = rememberHapticFeedback(HapticFeedbackType.Confirm)
    val textColor = MaterialTheme.colorScheme.onBackground
    val locale = LocaleController.effectiveLocale(LocalContext.current)

    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", locale)
    val timeFormatter = SimpleDateFormat("HH:mm", locale)
    val effectiveCreatedAt = when {
        createdAt > 0L -> createdAt
        timestamp > 0L -> timestamp
        else -> System.currentTimeMillis()
    }
    val effectiveUpdatedAt = when {
        updatedAt > 0L -> updatedAt
        timestamp > 0L -> timestamp
        else -> effectiveCreatedAt
    }
    val formattedCreatedDate = dateFormatter.format(Date(effectiveCreatedAt))
    val formattedCreatedTime = timeFormatter.format(Date(effectiveCreatedAt))
    val formattedUpdatedDate = dateFormatter.format(Date(effectiveUpdatedAt))
    val formattedUpdatedTime = timeFormatter.format(Date(effectiveUpdatedAt))

    var showOptionsSheet by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showOptionsSheet = true }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(listDensity.itemPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(listDensity.avatarSize)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (isPinned) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(accentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PushPin,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = textColor,
                            maxLines = 1
                        )

                        if (subtitle != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp
                                ),
                                color = accentColor,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                        }

                        if (tagsContent != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            tagsContent()
                        }
                    }
                }

                Box(
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            performHaptic()
                            showOptionsSheet = true
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.more_options),
                            tint = textColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }

    if (showOptionsSheet) {
        val restoreIcon = restoreActionIcon
        val restoreLabel = restoreActionDescription
        val restoreClick = onRestoreActionClick
        val deleteForeverIcon = deleteForeverActionIcon
        val deleteForeverLabel = deleteForeverActionDescription
        val options = listOfNotNull(
            if (restoreIcon != null && restoreLabel != null && restoreClick != null) {
                OptionsDialogItem(
                    label = restoreLabel,
                    icon = restoreIcon,
                    onClick = restoreClick,
                )
            } else {
                null
            },
            onPinClick?.let { pinClick ->
                OptionsDialogItem(
                    label = if (isPinned) stringResource(R.string.note_unpin)
                    else stringResource(R.string.note_pin),
                    icon = if (isPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                    onClick = pinClick,
                )
            },
            onLockClick?.let { lockClick ->
                OptionsDialogItem(
                    label = if (isLocked) stringResource(R.string.note_lock_action_unlock)
                    else stringResource(R.string.note_lock_action_lock),
                    icon = if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                    onClick = lockClick,
                )
            },
            onSaveClick?.let { saveClick ->
                OptionsDialogItem(
                    label = stringResource(R.string.save),
                    icon = Icons.Default.Download,
                    onClick = saveClick,
                )
            },
            onShareClick?.let { shareClick ->
                OptionsDialogItem(
                    label = stringResource(R.string.share),
                    icon = Icons.Default.Share,
                    onClick = shareClick,
                )
            },
            onMoveToFolderClick?.let { moveClick ->
                OptionsDialogItem(
                    label = stringResource(R.string.note_move_to_folder),
                    icon = Icons.Default.Folder,
                    onClick = moveClick,
                )
            },
            onTagsClick?.let { tagsClick ->
                OptionsDialogItem(
                    label = stringResource(R.string.tag_manager_title),
                    icon = Lucide.Tags,
                    onClick = tagsClick,
                )
            },
            if (isNormalNote && onTransformToAudio != null) {
                OptionsDialogItem(
                    label = stringResource(R.string.note_transform_to_audio),
                    icon = Icons.Default.GraphicEq,
                    onClick = { onTransformToAudio.invoke() },
                )
            } else {
                null
            },
            if (!replaceDeleteWithCustom) {
                OptionsDialogItem(
                    label = stringResource(R.string.edit),
                    icon = Icons.Default.EditNote,
                    onClick = onClick,
                )
            } else {
                null
            },
            if (!replaceDeleteWithCustom) {
                OptionsDialogItem(
                    label = stringResource(R.string.delete),
                    icon = Icons.Default.Delete,
                    destructive = true,
                    onClick = onDeleteClick,
                )
            } else if (deleteForeverIcon != null && deleteForeverLabel != null) {
                OptionsDialogItem(
                    label = deleteForeverLabel,
                    icon = deleteForeverIcon,
                    destructive = true,
                    onClick = onDeleteClick,
                )
            } else {
                null
            },
            OptionsDialogItem(
                label = "${stringResource(R.string.created_label)}: $formattedCreatedDate $formattedCreatedTime",
                icon = null,
                description = "${stringResource(R.string.updated_label)}: $formattedUpdatedDate $formattedUpdatedTime",
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
}
