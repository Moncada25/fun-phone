package com.bookverse.contacts.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.ui.utils.rememberHapticClick

@Composable
fun FloatingPill(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    contentColor: Color,
    content: @Composable () -> Unit
) {
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = backgroundColor,
        contentColor = contentColor,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, outlineColor)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun FloatingIconPill(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    backgroundColor: Color,
    contentColor: Color,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val semanticsModifier = modifier.semantics { role = Role.Button }
    Surface(
        onClick = onClick,
        modifier = semanticsModifier,
        enabled = enabled,
        shape = CircleShape,
        color = backgroundColor,
        contentColor = contentColor,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, outlineColor),
        interactionSource = resolvedInteractionSource
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                content()
            }
        }
    }
}

class ActionPillScope(
    private val delegate: RowScope,
    private val isMinimal: Boolean,
    private val pillContainerColor: Color,
    private val pillContentColor: Color
) : RowScope by delegate {

    @Composable
    fun IconAction(
        imageVector: ImageVector,
        contentDescription: String?,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        interactionSource: MutableInteractionSource? = null,
        feedback: Boolean = false
    ) {
        val iconSize = if (isMinimal) 20 else 24
        val hapticOnClick = if (feedback) rememberHapticClick(onClick) else onClick
        IconAction(
            onClick = hapticOnClick,
            modifier = modifier,
            enabled = enabled,
            interactionSource = interactionSource
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize.dp)
            )
        }
    }

    @Composable
    fun IconAction(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        feedback: Boolean = false,
        enabled: Boolean = true,
        interactionSource: MutableInteractionSource? = null,
        content: @Composable () -> Unit
    ) {
        val hapticOnClick = if (feedback) rememberHapticClick(onClick) else onClick
        if (isMinimal) {
            FloatingIconPill(
                modifier = modifier,
                onClick = hapticOnClick,
                backgroundColor = pillContainerColor,
                contentColor = pillContentColor,
                enabled = enabled,
                interactionSource = interactionSource,
                content = content
            )
        } else {
            val resolvedInteractionSource =
                interactionSource ?: remember { MutableInteractionSource() }
            IconButton(
                onClick = hapticOnClick,
                modifier = modifier,
                enabled = enabled,
                interactionSource = resolvedInteractionSource
            ) {
                CompositionLocalProvider(LocalContentColor provides pillContentColor) {
                    content()
                }
            }
        }
    }
}
