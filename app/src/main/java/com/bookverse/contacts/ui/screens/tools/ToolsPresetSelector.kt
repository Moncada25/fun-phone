package com.bookverse.contacts.ui.screens.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.R
import com.bookverse.contacts.data.model.ToolsOrderConfig
import com.bookverse.contacts.data.model.ToolsPreset

@Composable
fun ToolsPresetSelector(
    toolsOrder: ToolsOrderConfig,
    onPresetSelected: (ToolsPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedPreset = toolsOrder.matchingPreset()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.tools_presets_title),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
        )
        Text(
            text = stringResource(R.string.tools_presets_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        ToolsPreset.entries.forEach { preset ->
            val selected = selectedPreset == preset
            val colors = if (selected) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }
            Button(
                onClick = { onPresetSelected(preset) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = colors
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(preset.titleRes()),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = stringResource(preset.descriptionRes()),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (selectedPreset == null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { selected = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.tools_preset_custom),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = stringResource(R.string.tools_preset_custom_description),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun ToolsPreset.titleRes(): Int = when (this) {
    ToolsPreset.ESSENTIAL_PHONE -> R.string.tools_preset_essential_phone
    ToolsPreset.PHONE_AND_ORGANIZE -> R.string.tools_preset_phone_and_organize
    ToolsPreset.EVERYTHING -> R.string.tools_preset_everything
}

private fun ToolsPreset.descriptionRes(): Int = when (this) {
    ToolsPreset.ESSENTIAL_PHONE -> R.string.tools_preset_essential_phone_description
    ToolsPreset.PHONE_AND_ORGANIZE -> R.string.tools_preset_phone_and_organize_description
    ToolsPreset.EVERYTHING -> R.string.tools_preset_everything_description
}
