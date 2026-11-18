package com.bookverse.contacts.ui.screens.notes

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.bookverse.contacts.feature.notes.R
import com.bookverse.contacts.ui.components.CountBreakdownSheet
import com.bookverse.contacts.ui.components.CountInfoRow

internal data class NoteTypeCount(
    val icon: ImageVector,
    val label: String,
    val count: Int,
)

@Composable
internal fun NoteTypeCountsSheet(
    typeCounts: List<NoteTypeCount>,
    accentColor: Color,
    onDismiss: () -> Unit,
) {
    CountBreakdownSheet(
        title = stringResource(R.string.notes_type_sheet_title),
        rows = typeCounts.map { item ->
            CountInfoRow(
                icon = item.icon,
                label = item.label,
                count = item.count,
            )
        },
        accentColor = accentColor,
        onDismiss = onDismiss,
    )
}
