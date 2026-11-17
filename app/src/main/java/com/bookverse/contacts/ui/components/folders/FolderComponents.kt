package com.bookverse.contacts.ui.components.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.toColorInt
import com.bookverse.contacts.ui.components.ColorPickerDialog
import com.bookverse.contacts.ui.components.TextField
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import kotlin.math.roundToInt

data class FolderUiModel(
    val id: Long,
    val name: String,
    val description: String?,
    val colorHex: String,
    val createdAt: Long,
)

data class FolderDraft(
    val id: Long,
    val name: String,
    val description: String?,
    val colorHex: String,
    val createdAt: Long,
)

fun parseFolderColor(colorHex: String?, fallback: Color): Color =
    runCatching { Color((colorHex ?: "").toColorInt()) }.getOrElse { fallback }

fun Color.toFolderColorHex(): String {
    val redValue = (red * 255).roundToInt().coerceIn(0, 255)
    val greenValue = (green * 255).roundToInt().coerceIn(0, 255)
    val blueValue = (blue * 255).roundToInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", redValue, greenValue, blueValue)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderFilterRow(
    folders: List<FolderUiModel>,
    selectedFolderId: Long,
    unfiledId: Long,
    unfiledLabel: String,
    onFolderSelected: (Long) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val options = remember(folders) { folders + null }
    val selectedIndex = options.indexOfFirst { option ->
        (option == null && selectedFolderId == unfiledId) || option?.id == selectedFolderId
    }.takeIf { it >= 0 } ?: 0
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        options.forEachIndexed { index, folder ->
            val activeColor = folder?.colorHex?.let { parseFolderColor(it, accentColor) } ?: accentColor
            SegmentedButton(
                selected = index == selectedIndex,
                onClick = { onFolderSelected(folder?.id ?: unfiledId) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = activeColor,
                    activeContentColor = contentColorFor(activeColor),
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Text(
                    text = folder?.name ?: unfiledLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
fun FolderCard(
    folder: FolderUiModel,
    itemCountLabel: String,
    accentColor: Color,
    editDescription: String,
    deleteDescription: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
) {
    val folderColor = parseFolderColor(folder.colorHex, accentColor)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (isDragging) 0.95f else 1f },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                        .background(folderColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = folderColor)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        itemCountLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = editDescription)
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = deleteDescription,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            folder.description?.takeIf { it.isNotBlank() }?.let { description ->
                Spacer(Modifier.height(12.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun FolderEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    primaryActionLabel: String,
    accentColor: Color,
    onPrimaryAction: () -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    hint: String? = null,
    primaryActionEnabled: Boolean = true,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(90.dp), tint = accentColor.copy(alpha = 0.6f))
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(description, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onPrimaryAction,
            enabled = primaryActionEnabled,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
        ) {
            Text(primaryActionLabel, color = contentColorFor(accentColor), fontWeight = FontWeight.Bold)
        }
        if (secondaryActionLabel != null && onSecondaryAction != null) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onSecondaryAction) {
                Text(secondaryActionLabel, color = accentColor, fontWeight = FontWeight.SemiBold)
            }
        }
        hint?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun FolderFormDialog(
    folder: FolderUiModel?,
    title: String,
    nameLabel: String,
    descriptionLabel: String,
    colorLabel: String,
    editLabel: String,
    cancelLabel: String,
    saveLabel: String,
    accentColor: Color,
    appSettingsViewModel: AppSettingsViewModel,
    onDismiss: () -> Unit,
    onSave: (FolderDraft) -> Unit,
) {
    var name by rememberSaveable(folder?.id) { mutableStateOf(folder?.name.orEmpty()) }
    var description by rememberSaveable(folder?.id) { mutableStateOf(folder?.description.orEmpty()) }
    var colorHex by rememberSaveable(folder?.id) {
        mutableStateOf(folder?.colorHex ?: accentColor.toFolderColorHex())
    }
    var showColorPicker by remember { mutableStateOf(false) }
    val selectedColor = remember(colorHex, accentColor) { parseFolderColor(colorHex, accentColor) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = nameLabel,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = descriptionLabel,
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                Column {
                    Text(colorLabel, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .clickable { showColorPicker = true }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(selectedColor))
                        Column(Modifier.weight(1f)) {
                            Text(selectedColor.toFolderColorHex(), fontWeight = FontWeight.SemiBold)
                            Text(editLabel, style = MaterialTheme.typography.labelMedium, color = accentColor)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(18.dp),
                    ) { Text(cancelLabel, color = MaterialTheme.colorScheme.onSurface) }
                    Button(
                        onClick = {
                            onSave(
                                FolderDraft(
                                    id = folder?.id ?: 0,
                                    name = name.trim(),
                                    description = description.trim().takeIf { it.isNotBlank() },
                                    colorHex = colorHex.ifBlank { accentColor.toFolderColorHex() },
                                    createdAt = folder?.createdAt ?: System.currentTimeMillis(),
                                ),
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(18.dp),
                    ) { Text(saveLabel, color = contentColorFor(accentColor), fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = selectedColor,
            onColorSelected = { colorHex = it.toFolderColorHex(); showColorPicker = false },
            onDismiss = { showColorPicker = false },
            appSettingsViewModel = appSettingsViewModel,
        )
    }
}
