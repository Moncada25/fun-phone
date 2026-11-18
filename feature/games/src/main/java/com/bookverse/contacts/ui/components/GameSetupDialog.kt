package com.bookverse.contacts.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bookverse.contacts.feature.games.R
import com.bookverse.contacts.ui.theme.AppChrome
import com.bookverse.contacts.core.ui.R as CoreUiR

data class GameSetupOption(
    val title: String,
    val description: String,
)

@Composable
fun GameSetupDialog(
    gameTitle: String,
    title: String,
    instructions: String,
    options: List<GameSetupOption>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    accentColor: Color,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 560.dp)
                .heightIn(max = 640.dp),
            shape = RoundedCornerShape(AppChrome.sheetCorner),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ArcadeDialogHeader(
                    title = gameTitle,
                    eyebrow = stringResource(R.string.games_how_to_play),
                    onClose = null,
                )
                ArcadeScreenPanel {
                    Text(
                        text = instructions,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = title,
                    style = arcadeLabelStyle,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    options.forEachIndexed { index, option ->
                        GameSetupOptionRow(
                            selected = index == selectedIndex,
                            title = option.title,
                            description = option.description,
                            accentColor = accentColor,
                            onClick = { onOptionSelected(index) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = AppChrome.touchTarget),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Text(
                            text = stringResource(CoreUiR.string.cancel),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    ArcadePlayButton(
                        onClick = onStart,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun GameSetupOptionRow(
    selected: Boolean,
    title: String,
    description: String,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) {
                    accentColor
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = shape,
            ),
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = accentColor,
                    unselectedColor = MaterialTheme.colorScheme.outline,
                ),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
