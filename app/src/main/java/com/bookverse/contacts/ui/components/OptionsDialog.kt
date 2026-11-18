package com.bookverse.contacts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.core.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsDialog(
    title: String,
    options: List<String>,
    onOptionSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                color = textColor,
                thickness = 1.dp
            )

            options.forEachIndexed { index, option ->
                Button(
                    onClick = { onOptionSelected(index) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = option,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }
                if (index < options.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Immutable
data class OptionsDialogItem(
    val label: String,
    val icon: ImageVector?,
    val description: String? = null,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val destructive: Boolean = false,
    val onClick: () -> Unit
)

@Composable
fun OptionsDialog(
    title: String,
    options: List<OptionsDialogItem>,
    onDismiss: () -> Unit,
) {
    ButtonSheetScaffold(
        title = title,
        onDismiss = onDismiss,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(
                items = options,
                key = { index, option -> "${option.label}-$index" },
            ) { _, option ->
                OptionRow(
                    option = option,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

@Composable
internal fun OptionRow(
    option: OptionsDialogItem,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val actionColor = if (option.destructive) {
        colors.error
    } else if (option.selected) {
        colors.onPrimaryContainer
    } else {
        colors.onSurface
    }
    val contentColor = if (option.enabled) {
        if (option.destructive) colors.error
        else if (option.selected) colors.onPrimaryContainer
        else colors.onSurface
    } else {
        colors.onSurface.copy(alpha = 0.4f)
    }
    val iconTint = if (option.enabled) actionColor else actionColor.copy(alpha = 0.4f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                color = if (option.selected) colors.primaryContainer else Color.Transparent,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(
                enabled = option.enabled,
                role = Role.Button,
            ) {
                onDismiss()
                option.onClick()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        option.icon?.let { icon ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = iconTint.copy(alpha = 0.12f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = option.label,
                color = contentColor,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (option.selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            option.description?.takeIf(String::isNotBlank)?.let { desc ->
                Text(
                    text = desc,
                    color = contentColor.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (option.selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.cd_selected),
                tint = colors.onPrimaryContainer,
            )
        }
    }
}
