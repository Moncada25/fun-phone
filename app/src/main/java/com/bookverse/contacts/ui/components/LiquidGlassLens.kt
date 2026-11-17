package com.bookverse.contacts.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/** A neutral selection lens; specular highlights belong to the dock's outer surface. */
internal fun DrawScope.drawLiquidGlassLens(
    topLeft: Offset,
    size: Size,
    contentColor: Color,
    dark: Boolean,
) {
    val radius = CornerRadius(size.minDimension / 2f)
    drawRoundRect(
        color = contentColor.copy(alpha = if (dark) 0.14f else 0.08f),
        topLeft = topLeft,
        size = size,
        cornerRadius = radius,
    )
}
