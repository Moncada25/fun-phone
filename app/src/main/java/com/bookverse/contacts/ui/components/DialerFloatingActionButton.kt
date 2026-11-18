package com.bookverse.contacts.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.ui.theme.AccentBarStyle

/** The main dialer and appearance preview share the same style-specific rendering. */
@Composable
internal fun DialerFloatingActionButton(
    style: AccentBarStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.dialer)
    val scheme = MaterialTheme.colorScheme
    when (style) {
        AccentBarStyle.MODERN -> ModernFloatingActionButton(
            icon = Icons.Rounded.Dialpad,
            contentDescription = description,
            onClick = onClick,
            modifier = modifier,
        )
        AccentBarStyle.MINIMAL -> Surface(
            onClick = onClick,
            modifier = modifier.size(56.dp),
            shape = CircleShape,
            color = scheme.surface,
            contentColor = scheme.onSurface,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, scheme.outline.copy(alpha = 0.35f)),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Dialpad, description, Modifier.size(26.dp))
            }
        }
        AccentBarStyle.RETRO -> FloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            containerColor = scheme.primary,
            contentColor = scheme.onPrimary,
        ) {
            Icon(Icons.Rounded.Dialpad, description, Modifier.size(26.dp))
        }
    }
}
