package com.bookverse.contacts.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable

@Composable
fun SettingSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    val onSurface = MaterialTheme.colorScheme.onSurface
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.onPrimary,
            uncheckedThumbColor = onSurface,
            checkedTrackColor = colors.primary,
            uncheckedTrackColor = onSurface.copy(alpha = 0.5f),
            disabledCheckedThumbColor = colors.onSurface.copy(alpha = 0.38f),
            disabledUncheckedThumbColor = onSurface.copy(alpha = 0.4f),
            disabledCheckedTrackColor = colors.primaryContainer.copy(alpha = 0.38f),
            disabledUncheckedTrackColor = onSurface.copy(alpha = 0.2f)
        )
    )
}
