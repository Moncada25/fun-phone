package com.bookverse.contacts.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.core.ui.R

@Immutable
internal data class SettingsToggleModel(
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
)

@Immutable
internal data class SettingsItemModel(
    val key: String,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val onClick: () -> Unit,
    val currentValue: String? = null,
    val toggle: SettingsToggleModel? = null,
)

@Immutable
internal data class SettingsSectionModel(
    val key: String,
    val title: String,
    val description: String,
    val items: List<SettingsItemModel>,
)

@Composable
internal fun SettingsSectionCard(
    section: SettingsSectionModel,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(section.key) { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "settings section arrow",
    )
    val expandedLabel = stringResource(if (expanded) R.string.collapse else R.string.expand)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { stateDescription = expandedLabel }
                    .clickable(role = Role.Button) { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = section.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = expandedLabel,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(arrowRotation),
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                    )
                    section.items.forEachIndexed { index, item ->
                        if (item.toggle == null) {
                            SettingsItemRow(
                                item = item,
                            )
                        } else {
                            SettingsToggleItem(
                                item = item,
                            )
                        }
                        if (index != section.items.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsItemRow(
    item: SettingsItemModel,
    modifier: Modifier = Modifier,
) {
    SettingsItemContent(
        item = item,
        modifier = modifier.clickable(role = Role.Button, onClick = item.onClick),
        toggle = null,
    )
}

@Composable
internal fun SettingsToggleItem(
    item: SettingsItemModel,
    modifier: Modifier = Modifier,
) {
    val toggle = requireNotNull(item.toggle) { "SettingsToggleItem requires toggle state" }
    SettingsItemContent(
        item = item,
        modifier = modifier.toggleable(
            value = toggle.checked,
            role = Role.Switch,
            onValueChange = toggle.onCheckedChange,
        ),
        toggle = toggle,
    )
}

@Composable
private fun SettingsItemContent(
    item: SettingsItemModel,
    modifier: Modifier,
    toggle: SettingsToggleModel?,
) {

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        val fontScale = LocalDensity.current.fontScale
        val valueBelowText = item.currentValue != null && (maxWidth < 340.dp || fontScale >= 1.3f)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsIconContainer(
                icon = item.icon,
                compact = true,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (valueBelowText) 3 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (valueBelowText) {
                    SettingsValueBadge(
                        value = item.currentValue,
                    )
                }
            }

            when {
                toggle != null -> SettingsSwitch(
                    checked = toggle.checked,
                )

                else -> {
                    if (!valueBelowText && item.currentValue != null) {
                        SettingsValueBadge(
                            value = item.currentValue,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsIconContainer(
    icon: ImageVector,
    compact: Boolean = false,
) {
    val containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val contentColor = MaterialTheme.colorScheme.onSurface
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.size(if (compact) 40.dp else 44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(if (compact) 21.dp else 23.dp),
            )
        }
    }
}

@Composable
private fun SettingsValueBadge(
    value: String,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
        ),
    ) {
        Text(
            text = value,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SettingsSwitch(
    checked: Boolean,
) {
    Spacer(modifier = Modifier.width(4.dp))
    Switch(
        checked = checked,
        onCheckedChange = null,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
            uncheckedTrackColor = MaterialTheme.colorScheme.surface,
            uncheckedBorderColor = MaterialTheme.colorScheme.outline,
        ),
    )
}
