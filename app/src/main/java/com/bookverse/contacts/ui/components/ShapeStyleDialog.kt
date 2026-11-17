package com.bookverse.contacts.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.bookverse.contacts.R
import com.bookverse.contacts.ui.viewmodel.PathType
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun ShapeStyleDialog(
    initialShape: PathType,
    initialFillMode: Boolean,
    onShapeSelected: (PathType, Boolean) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color
) {
    var selectedShape by remember { mutableStateOf(initialShape) }
    var fillMode by remember { mutableStateOf(initialFillMode) }
    val backgroundColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground

    val availableShapes = listOf(
        PathType.CIRCLE,
        PathType.SQUARE,
        PathType.RECTANGLE,
        PathType.TRIANGLE,
        PathType.HEART,
        PathType.STAR,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = backgroundColor,
        shape = RoundedCornerShape(24.dp),
        properties = DialogProperties(dismissOnClickOutside = true),
        title = { Text(text = stringResource(R.string.select_shape)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableShapes, key = { it.name }) { shape ->
                        ShapeStyleItem(
                            shape = shape,
                            isSelected = selectedShape == shape,
                            isFilled = fillMode,
                            accentColor = accentColor,
                            onClick = { selectedShape = shape }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.fill_shape),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                    Switch(
                        checked = fillMode,
                        onCheckedChange = { fillMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = backgroundColor,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onShapeSelected(selectedShape, fillMode) },
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
private fun ShapeStyleItem(
    shape: PathType,
    isSelected: Boolean,
    isFilled: Boolean,
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
                .height(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            ShapePreview(
                shape = shape,
                isFilled = isFilled,
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = getShapeLabel(shape),
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ShapePreview(
    shape: PathType,
    isFilled: Boolean,
    color: Color
) {
    Canvas(
        modifier = Modifier.size(40.dp)
    ) {
        val strokeWidth = 3f
        val drawStyle = if (isFilled) Fill else Stroke(width = strokeWidth)

        when (shape) {
            PathType.CIRCLE -> {
                drawCircle(
                    color = color,
                    radius = size.minDimension / 2.5f,
                    style = drawStyle
                )
            }
            PathType.SQUARE -> {
                val squareSize = size.minDimension / 1.5f
                val topLeft = Offset(
                    (size.width - squareSize) / 2,
                    (size.height - squareSize) / 2
                )
                drawRect(
                    color = color,
                    topLeft = topLeft,
                    size = Size(squareSize, squareSize),
                    style = drawStyle
                )
            }
            PathType.RECTANGLE -> {
                val rectWidth = size.width * 0.7f
                val rectHeight = size.height * 0.5f
                val topLeft = Offset(
                    (size.width - rectWidth) / 2,
                    (size.height - rectHeight) / 2
                )
                drawRect(
                    color = color,
                    topLeft = topLeft,
                    size = Size(rectWidth, rectHeight),
                    style = drawStyle
                )
            }
            PathType.TRIANGLE -> {
                drawTriangle(color, drawStyle)
            }
            PathType.HEART -> {
                drawHeart(color, drawStyle)
            }
            PathType.STAR -> {
                drawStar(color, drawStyle)
            }
            else -> {}
        }
    }
}

private fun DrawScope.drawTriangle(color: Color, drawStyle: androidx.compose.ui.graphics.drawscope.DrawStyle) {
    val path = Path().apply {
        val width = size.width * 0.7f
        val height = size.height * 0.6f
        val centerX = size.width / 2
        val top = (size.height - height) / 2
        val bottom = top + height

        moveTo(centerX, top)
        lineTo(centerX + width / 2, bottom)
        lineTo(centerX - width / 2, bottom)
        close()
    }
    drawPath(path, color, style = drawStyle)
}

private fun DrawScope.drawHeart(color: Color, drawStyle: androidx.compose.ui.graphics.drawscope.DrawStyle) {
    val path = Path().apply {
        val width = size.width * 0.7f
        val height = size.height * 0.65f
        val centerX = size.width / 2
        val top = (size.height - height) / 2

        moveTo(centerX, top + height * 0.3f)
        cubicTo(
            centerX - width / 2, top - height * 0.1f,
            centerX - width / 2, top + height * 0.5f,
            centerX, top + height
        )
        cubicTo(
            centerX + width / 2, top + height * 0.5f,
            centerX + width / 2, top - height * 0.1f,
            centerX, top + height * 0.3f
        )
        close()
    }
    drawPath(path, color, style = drawStyle)
}

private fun DrawScope.drawStar(color: Color, drawStyle: androidx.compose.ui.graphics.drawscope.DrawStyle) {
    val path = Path().apply {
        val outerRadius = min(size.width, size.height) * 0.4f
        val innerRadius = outerRadius * 0.4f
        val centerX = size.width / 2
        val centerY = size.height / 2
        val angleStep = Math.PI / 5

        for (i in 0 until 10) {
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            val angle = i * angleStep - Math.PI / 2
            val x = centerX + (radius * cos(angle)).toFloat()
            val y = centerY + (radius * sin(angle)).toFloat()

            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, color, style = drawStyle)
}

@Composable
private fun getShapeLabel(shape: PathType): String {
    return when (shape) {
        PathType.CIRCLE -> stringResource(R.string.shape_circle)
        PathType.SQUARE -> stringResource(R.string.shape_square)
        PathType.RECTANGLE -> stringResource(R.string.shape_rectangle)
        PathType.TRIANGLE -> stringResource(R.string.shape_triangle)
        PathType.HEART -> stringResource(R.string.shape_heart)
        PathType.STAR -> stringResource(R.string.shape_star)
        else -> ""
    }
}
