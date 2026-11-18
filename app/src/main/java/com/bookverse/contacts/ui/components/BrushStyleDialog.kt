package com.bookverse.contacts.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.bookverse.contacts.core.ui.R

enum class BrushStyle {
    NORMAL,
    DASHED,
    DOTTED,
    CALLIGRAPHY,
    MARKER,
    SPRAY,
    SKETCH,
    GLOW
}

@Composable
fun BrushStyle.getLabel(): String {
    return when (this) {
        BrushStyle.NORMAL -> stringResource(R.string.brush_style_normal)
        BrushStyle.DASHED -> stringResource(R.string.brush_style_dashed)
        BrushStyle.DOTTED -> stringResource(R.string.brush_style_dotted)
        BrushStyle.CALLIGRAPHY -> stringResource(R.string.brush_style_calligraphy)
        BrushStyle.MARKER -> stringResource(R.string.brush_style_marker)
        BrushStyle.SPRAY -> stringResource(R.string.brush_style_spray)
        BrushStyle.SKETCH -> stringResource(R.string.brush_style_sketch)
        BrushStyle.GLOW -> stringResource(R.string.brush_style_glow)
    }
}

fun BrushStyle.toPathEffect(brushSize: Float): PathEffect? {
    return when (this) {
        BrushStyle.DASHED -> PathEffect.dashPathEffect(
            floatArrayOf(
                brushSize * 2f,
                brushSize * 1.5f
            ), 0f
        )

        BrushStyle.DOTTED -> PathEffect.dashPathEffect(
            floatArrayOf(brushSize * 0.5f, brushSize),
            0f
        )

        BrushStyle.SKETCH -> PathEffect.dashPathEffect(
            floatArrayOf(
                brushSize * 1.5f,
                brushSize * 0.3f
            ), 0f
        )

        else -> null
    }
}

fun BrushStyle.toStrokeCap(): StrokeCap {
    return when (this) {
        BrushStyle.CALLIGRAPHY -> StrokeCap.Square
        BrushStyle.MARKER -> StrokeCap.Square
        BrushStyle.DOTTED -> StrokeCap.Round
        else -> StrokeCap.Round
    }
}

@Composable
fun BrushStyleDialog(
    initialStyle: BrushStyle,
    onStyleSelected: (BrushStyle) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color
) {
    var selectedStyle by remember { mutableStateOf(initialStyle) }
    val backgroundColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = backgroundColor,
        shape = RoundedCornerShape(24.dp),
        properties = DialogProperties(dismissOnClickOutside = true),
        title = { Text(text = stringResource(R.string.brush_style)) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(BrushStyle.entries, key = { it.name }) { style ->
                    BrushStyleItem(
                        style = style,
                        isSelected = selectedStyle == style,
                        accentColor = accentColor,
                        onClick = { selectedStyle = style }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onStyleSelected(selectedStyle) },
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
                Text(
                    text = stringResource(R.string.cancel),
                    color = accentColor
                )
            }
        }
    )
}

@Composable
private fun BrushStyleItem(
    style: BrushStyle,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) accentColor else Color.Gray.copy(alpha = 0.3f)
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            BrushStylePreview(
                style = style,
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = style.getLabel(),
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BrushStylePreview(
    style: BrushStyle,
    color: Color
) {
    Canvas(
        modifier = Modifier
            .size(width = 80.dp, height = 32.dp)
    ) {
        val strokeWidth = 8f
        val startY = size.height / 2
        val startX = size.width * 0.1f
        val endX = size.width * 0.9f

        when (style) {
            BrushStyle.NORMAL -> {
                drawLine(
                    color = color,
                    start = Offset(startX, startY),
                    end = Offset(endX, startY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            BrushStyle.DASHED -> {
                drawLine(
                    color = color,
                    start = Offset(startX, startY),
                    end = Offset(endX, startY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                )
            }

            BrushStyle.DOTTED -> {
                drawLine(
                    color = color,
                    start = Offset(startX, startY),
                    end = Offset(endX, startY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)
                )
            }

            BrushStyle.CALLIGRAPHY -> {
                drawLine(
                    color = color,
                    start = Offset(startX, startY),
                    end = Offset(endX, startY),
                    strokeWidth = strokeWidth * 1.5f,
                    cap = StrokeCap.Square
                )
            }

            BrushStyle.MARKER -> {
                drawLine(
                    color = color.copy(alpha = 0.7f),
                    start = Offset(startX, startY),
                    end = Offset(endX, startY),
                    strokeWidth = strokeWidth * 1.3f,
                    cap = StrokeCap.Square
                )
            }

            BrushStyle.SPRAY -> {
                val random = java.util.Random(42)
                repeat(30) {
                    val x = startX + (endX - startX) * random.nextFloat()
                    val y = startY + (random.nextFloat() - 0.5f) * strokeWidth * 1.5f
                    drawCircle(
                        color = color.copy(alpha = 0.3f + random.nextFloat() * 0.3f),
                        radius = 1f + random.nextFloat() * 2f,
                        center = Offset(x, y)
                    )
                }
            }

            BrushStyle.SKETCH -> {
                drawLine(
                    color = color,
                    start = Offset(startX, startY),
                    end = Offset(endX, startY),
                    strokeWidth = strokeWidth * 0.7f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 2.4f), 0f)
                )
            }

            BrushStyle.GLOW -> {
                drawLine(
                    color = color.copy(alpha = 0.3f),
                    start = Offset(startX, startY),
                    end = Offset(endX, startY),
                    strokeWidth = strokeWidth * 2.5f,
                    cap = StrokeCap.Round,
                    blendMode = BlendMode.Plus
                )
                drawLine(
                    color = color,
                    start = Offset(startX, startY),
                    end = Offset(endX, startY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
