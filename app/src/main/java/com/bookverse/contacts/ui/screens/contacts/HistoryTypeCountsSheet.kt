package com.bookverse.contacts.ui.screens.contacts

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.bookverse.contacts.R
import com.bookverse.contacts.ui.components.CountBreakdownSheet
import com.bookverse.contacts.ui.components.CountInfoRow

internal data class HistoryTypeCount(
    val icon: ImageVector,
    val label: String,
    val count: Int,
)

@Composable
internal fun HistoryTypeCountsSheet(
    typeCounts: List<HistoryTypeCount>,
    accentColor: Color,
    onDismiss: () -> Unit,
) {
    CountBreakdownSheet(
        title = stringResource(R.string.history_recent_operations_sheet_title),
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
