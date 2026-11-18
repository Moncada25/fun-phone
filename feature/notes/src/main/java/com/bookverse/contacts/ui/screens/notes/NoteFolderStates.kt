package com.bookverse.contacts.ui.screens.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.data.notes.NOTE_FOLDER_FILTER_ALL
import com.bookverse.contacts.data.notes.NoteFolder
import com.bookverse.contacts.feature.notes.R
import com.bookverse.contacts.ui.components.folders.FolderFilterRow
import com.bookverse.contacts.ui.components.folders.FolderUiModel

internal data class NoteFolderFilterState(
    val options: List<NoteFolder?>,
    val selectedIndex: Int,
)

internal fun buildNoteFolderFilterState(
    folders: List<NoteFolder>,
    selectedFolderId: Long,
): NoteFolderFilterState {
    val options = folders + null
    val selectedIndex = options.indexOfFirst { option ->
        (option == null && selectedFolderId == NOTE_FOLDER_FILTER_ALL) ||
            option?.id == selectedFolderId
    }.takeIf { it >= 0 } ?: 0
    return NoteFolderFilterState(
        options = options,
        selectedIndex = selectedIndex,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteFolderFilterRow(
    folders: List<NoteFolder>,
    selectedFolderId: Long,
    onFolderSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color,
) {
    FolderFilterRow(
        folders = folders.map { folder ->
            FolderUiModel(folder.id, folder.name, folder.description, folder.colorHex, folder.createdAt)
        },
        selectedFolderId = selectedFolderId,
        unfiledId = NOTE_FOLDER_FILTER_ALL,
        unfiledLabel = stringResource(R.string.note_folders_filter_all),
        onFolderSelected = onFolderSelected,
        modifier = modifier,
        accentColor = accentColor,
    )
}

@Composable
internal fun FolderEmptyState(
    folder: NoteFolder?,
    accentColor: Color,
    availableNotesCount: Int,
    onAddNoteClick: () -> Unit,
    onViewAllClick: () -> Unit,
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.NoteAdd,
            contentDescription = null,
            modifier = Modifier.size(90.dp),
            tint = accentColor.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.note_folder_empty_title, folder?.name.orEmpty()),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.note_folder_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddNoteClick,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            enabled = availableNotesCount > 0,
        ) {
            Text(
                text = stringResource(R.string.note_folder_empty_add_button),
                color = textColor,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onViewAllClick) {
            Text(
                text = stringResource(R.string.note_folder_empty_view_all),
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (availableNotesCount == 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.note_folder_empty_hint),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
