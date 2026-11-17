package com.bookverse.contacts.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.bookverse.contacts.R

@Composable
fun BrushSizeDialog(
    initialSize: Float,
    onSizeSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color
) {
    var brushSize by remember { mutableFloatStateOf(initialSize) }
    val backgroundColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = backgroundColor,
        shape = RoundedCornerShape(24.dp),
        properties = DialogProperties(dismissOnClickOutside = true),
        title = { Text(text = stringResource(R.string.brush_size)) },
        text = {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "5",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Slider(
                        value = brushSize,
                        onValueChange = { brushSize = it },
                        valueRange = 5f..50f,
                        steps = 9,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = Color.LightGray,
                            activeTickColor = accentColor,
                            inactiveTickColor = Color.LightGray
                        )
                    )

                    Text(
                        text = "50",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = stringResource(R.string.brush_size_value, brushSize.toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSizeSelected(brushSize) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = backgroundColor
                ),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(text = stringResource(R.string.apply), color = textColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), color = accentColor)
            }
        }
    )
}
