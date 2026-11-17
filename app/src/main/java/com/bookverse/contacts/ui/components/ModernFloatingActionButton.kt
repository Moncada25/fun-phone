package com.bookverse.contacts.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.settings.AnimationSpeed.Companion.LocalAnimationSpeed
import com.bookverse.contacts.ui.theme.AppChrome

@Composable
internal fun ModernFloatingActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.94f else 1f,
        animationSpec = tween(LocalAnimationSpeed.current.durationMs.coerceAtMost(160)),
        label = "Glass action press",
    )
    LiquidGlassSurface(
        modifier = modifier.size(AppChrome.floatingActionSize).graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape).clickable(
                interactionSource = interaction, indication = LocalIndication.current,
                role = Role.Button, onClick = onClick,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
        }
    }
}
