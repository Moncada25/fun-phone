package com.bookverse.contacts.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionIcon(
    tooltip: String,
    accentColor: Color,
    icon: ImageVector,
    contentDescription: String,
    textColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        label = "action_icon_scale"
    )
    val scope = rememberCoroutineScope()
    val tooltipState = rememberTooltipState()

    TooltipBox(
        state = tooltipState,
        tooltip = {
            PlainTooltip {
                Text(
                    text = tooltip,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        },
        focusable = false,
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider()
    ) {
        Box(
            modifier = Modifier
                .size(55.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.5f), CircleShape)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, radius = 34.dp),
                    onClick = onClick,
                    onLongClick = { scope.launch { tooltipState.show() } }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = textColor,
                modifier = Modifier.size(35.dp)
            )
        }
    }
}