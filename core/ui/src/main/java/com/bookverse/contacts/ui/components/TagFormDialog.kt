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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.toColorInt
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.model.ContactTag
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import kotlin.math.roundToInt

@Composable
fun TagFormDialog(
    tag: ContactTag?,
    appSettingsViewModel: AppSettingsViewModel,
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String) -> Unit,
) {
    val accentColor = MaterialTheme.colorScheme.primary

    var name by rememberSaveable(tag?.id) { mutableStateOf(tag?.name.orEmpty()) }
    var colorHex by rememberSaveable(tag?.id) {
        mutableStateOf(tag?.colorHex ?: accentColor.toTagHex())
    }
    var showColorPicker by remember { mutableStateOf(false) }
    val selectedColor = remember(colorHex, accentColor) {
        parseTagColorOrDefault(colorHex, accentColor)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(
                        if (tag == null) R.string.create_tag
                        else R.string.edit_tag
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.tag_name),
                    modifier = Modifier.fillMaxWidth(),
                )
                Column {
                    Text(
                        text = stringResource(R.string.tag_color),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showColorPicker = true }
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(selectedColor),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedColor.toTagHex(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.edit),
                                style = MaterialTheme.typography.labelMedium,
                                color = accentColor,
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Button(
                        onClick = {
                            onSave(
                                name.trim(),
                                colorHex.ifBlank { accentColor.toTagHex() },
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = selectedColor,
            onColorSelected = {
                colorHex = it.toTagHex()
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false },
            appSettingsViewModel = appSettingsViewModel,
        )
    }
}

private fun parseTagColorOrDefault(colorHex: String?, fallback: Color): Color {
    return runCatching { Color((colorHex ?: "").toColorInt()) }.getOrElse { fallback }
}

private fun Color.toTagHex(): String {
    val r = (red * 255).roundToInt().coerceIn(0, 255)
    val g = (green * 255).roundToInt().coerceIn(0, 255)
    val b = (blue * 255).roundToInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", r, g, b)
}
