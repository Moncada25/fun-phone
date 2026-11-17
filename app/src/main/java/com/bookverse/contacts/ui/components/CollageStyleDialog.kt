package com.bookverse.contacts.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.bookverse.contacts.R

@Composable
fun CollageStyleDialog(
    onLayoutSelected: (CollageLayout) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color
) {
    var selected by remember { mutableStateOf(CollageLayout.FOUR_GRID) }
    val backgroundColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val layouts = CollageLayout.entries

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = backgroundColor,
        shape = RoundedCornerShape(24.dp),
        properties = DialogProperties(dismissOnClickOutside = true),
        title = { Text(text = stringResource(R.string.paint_collage_title)) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth().height(280.dp),
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(layouts, key = { it.name }) { layout ->
                    val isSelected = layout == selected
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selected = layout }
                            .then(
                                if (isSelected) Modifier.border(
                                    2.dp,
                                    accentColor,
                                    RoundedCornerShape(12.dp)
                                ) else Modifier
                            )
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.08f)
                                else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(6.dp)
                    ) {
                        Canvas(modifier = Modifier.size(64.dp)) {
                            drawCollagePreview(layout, accentColor, textColor)
                        }
                        Text(
                            text = stringResource(layout.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) accentColor else textColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onLayoutSelected(selected) },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(text = stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
            ) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

private fun DrawScope.drawCollagePreview(
    layout: CollageLayout,
    accentColor: Color,
    borderColor: Color
) {
    val w = size.width
    val h = size.height
    val fill = accentColor.copy(alpha = 0.25f)
    val stroke = borderColor.copy(alpha = 0.5f)

    collageCellRects(layout, w, h, gap = 3f).forEach { rect ->
        drawRect(
            color = fill,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width(), rect.height())
        )
        drawRect(
            color = stroke,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width(), rect.height()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
        )
    }
}
