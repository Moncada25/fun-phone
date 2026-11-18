package com.bookverse.contacts.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.toColorInt
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import kotlin.math.roundToInt

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
    title: String = stringResource(R.string.select_color)
) {
    val dialogBackground = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val themeBackground = MaterialTheme.colorScheme.background
    val themeOnBackground = MaterialTheme.colorScheme.onBackground
    val predefinedColors by remember(themeBackground, themeOnBackground) {
        mutableStateOf(appSettingsViewModel.getDefaultColors(themeBackground, themeOnBackground))
    }

    val initialHsv = FloatArray(3)
    android.graphics.Color.colorToHSV(initialColor.toArgb(), initialHsv)
    val accentColor = MaterialTheme.colorScheme.primary

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    val selectedColor by remember(hue, saturation, value) {
        derivedStateOf {
            val hsv = floatArrayOf(hue, saturation, value)
            Color(android.graphics.Color.HSVToColor(hsv))
        }
    }

    var hexCode by remember { mutableStateOf(colorToHex(selectedColor)) }

    val updateHexFromColor = { color: Color ->
        hexCode = colorToHex(color)
    }

    val updateColorFromHex = { hex: String ->
        try {
            if (hex.length == 6) {
                val color = Color("#$hex".toColorInt())
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(color.toArgb(), hsv)
                hue = hsv[0]
                saturation = hsv[1]
                value = hsv[2]
            }
        } catch (_: Exception) {
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true),
        containerColor = dialogBackground,
        shape = RoundedCornerShape(24.dp),
        title = { Text(text = title) },
        text = {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(65.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(selectedColor)
                            .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        TextField(
                            value = hexCode,
                            onValueChange = { newValue ->
                                if (newValue.length <= 6 && newValue.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
                                    hexCode = newValue.uppercase()
                                    updateColorFromHex(newValue)
                                }
                            },
                            label = stringResource(R.string.hex_code),
                            keyboardType = KeyboardType.Text,
                            singleLine = true,
                            modifier = Modifier.width(120.dp),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.5f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                ) {
                    LucidColorPicker(
                        hue = hue,
                        saturation = saturation,
                        value = value,
                        onHueSaturationChange = { newHue, newSaturation ->
                            hue = newHue
                            saturation = newSaturation
                            updateHexFromColor(selectedColor)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.brightness),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(80.dp)
                    )

                    Slider(
                        value = value,
                        onValueChange = {
                            value = it
                            updateHexFromColor(selectedColor)
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor.copy(alpha = 0.7f),
                            inactiveTrackColor = Color.LightGray
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.predefined_color),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Start)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    predefinedColors.values.take(7).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, Color.Gray, CircleShape)
                                .clickable {
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    value = hsv[2]
                                    updateHexFromColor(color)
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    predefinedColors.values.drop(7).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, Color.Gray, CircleShape)
                                .clickable {
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    value = hsv[2]
                                    updateHexFromColor(color)
                                }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onColorSelected(selectedColor) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = dialogBackground
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

private fun colorToHex(color: Color): String {
    val red = (color.red * 255).roundToInt()
    val green = (color.green * 255).roundToInt()
    val blue = (color.blue * 255).roundToInt()
    return String.format("%02X%02X%02X", red, green, blue)
}

@Composable
fun LucidColorPicker(
    hue: Float,
    saturation: Float,
    value: Float,
    onHueSaturationChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPosition by remember { mutableStateOf(Offset.Zero) }

    val initialPosition = remember(hue, saturation) {
        val x = hue / 360f
        val y = 1f - saturation
        Offset(x, y)
    }

    LaunchedEffect(initialPosition) {
        currentPosition = initialPosition
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val normalizedX = (offset.x / size.width).coerceIn(0f, 1f)
                    val normalizedY = (offset.y / size.height).coerceIn(0f, 1f)
                    currentPosition = Offset(normalizedX, normalizedY)

                    val newHue = normalizedX * 360f
                    val newSaturation = 1f - normalizedY

                    onHueSaturationChange(newHue, newSaturation)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val normalizedX = (change.position.x / size.width).coerceIn(0f, 1f)
                    val normalizedY = (change.position.y / size.height).coerceIn(0f, 1f)
                    currentPosition = Offset(normalizedX, normalizedY)

                    val newHue = normalizedX * 360f
                    val newSaturation = 1f - normalizedY

                    onHueSaturationChange(newHue, newSaturation)
                }
            }
    ) {
        val hueColors = List(7) { i ->
            val hueValue = i * 60f
            hsvToColor(hueValue, value)
        }

        drawRect(
            brush = Brush.horizontalGradient(hueColors)
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White,
                    Color.Transparent
                )
            )
        )

        val selectorRadius = 8.dp.toPx()
        drawCircle(
            color = Color.White,
            radius = selectorRadius,
            center = Offset(
                currentPosition.x * size.width,
                currentPosition.y * size.height
            ),
            style = Stroke(width = 2f)
        )
    }
}

private fun hsvToColor(hue: Float, value: Float): Color {
    val hsv = floatArrayOf(hue, 1f, value)
    return Color(android.graphics.Color.HSVToColor(hsv))
}
