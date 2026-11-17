package com.bookverse.contacts.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.R
import com.bookverse.contacts.ui.components.ButtonSheetScaffold

@Composable
internal fun SettingsSheetScaffold(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    confirmDismiss: () -> Boolean = { true },
    content: @Composable ColumnScope.() -> Unit,
) {
    ButtonSheetScaffold(
        title = title,
        onDismiss = onDismiss,
        modifier = modifier,
        subtitle = subtitle,
        confirmDismiss = confirmDismiss,
        content = content,
    )
}

@Composable
internal fun <T> SettingsSelectionSheet(
    title: String,
    options: List<T>,
    selectedOption: T?,
    onSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    optionKey: (T) -> Any,
    optionLabel: @Composable (T) -> String,
    subtitle: String? = null,
    optionSupportingText: @Composable (T) -> String? = { null },
    optionFontFamily: (T) -> FontFamily? = { null },
    optionLeadingContent: (@Composable (T) -> Unit)? = null,
) {
    SettingsSheetScaffold(
        title = title,
        subtitle = subtitle,
        onDismiss = onDismiss,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = options,
                key = optionKey,
            ) { option ->
                SettingsChoiceRow(
                    label = optionLabel(option),
                    supportingText = optionSupportingText(option),
                    selected = option == selectedOption,
                    onClick = {
                        onSelected(option)
                        onDismiss()
                    },
                    fontFamily = optionFontFamily(option),
                    leadingContent = optionLeadingContent?.let { content ->
                        { content(option) }
                    },
                )
            }
        }
    }
}

@Composable
internal fun SettingsChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
    fontFamily: FontFamily? = null,
    leadingContent: @Composable (() -> Unit)? = null,
) {
    val selectedContainer = MaterialTheme.colorScheme.primaryContainer
    val selectedContent = MaterialTheme.colorScheme.onPrimaryContainer
    val normalContent = MaterialTheme.colorScheme.onSurface
    val contentColor = if (selected) selectedContent else normalContent

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) selectedContainer else MaterialTheme.colorScheme.surface,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingContent != null) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    leadingContent()
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    fontFamily = fontFamily,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                supportingText?.takeIf(String::isNotBlank)?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.75f),
                        fontFamily = fontFamily,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (selected) {
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    shape = CircleShape,
                    color = contentColor.copy(alpha = 0.14f),
                    contentColor = contentColor,
                ) {
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.cd_selected),
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }
        }
    }
}
