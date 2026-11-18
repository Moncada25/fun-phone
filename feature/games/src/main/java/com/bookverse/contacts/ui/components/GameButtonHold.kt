package com.bookverse.contacts.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable

@Composable
fun GameButtonHold(
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 70.dp,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed, enabled) {
        if (isPressed && enabled) {
            onPressStart()
        } else {
            onPressEnd()
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(if (isPressed) 0.90f else 1f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (!enabled)
                    accentColor.copy(alpha = 0.08f)
                else if (isPressed)
                    accentColor.copy(alpha = 0.7f)
                else
                    accentColor.copy(alpha = 0.3f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
            ) { },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> accentColor.copy(alpha = 0.3f)
                isPressed -> Color.White
                else -> accentColor
            },
            modifier = Modifier.size(size * 0.7f)
        )
    }
}
