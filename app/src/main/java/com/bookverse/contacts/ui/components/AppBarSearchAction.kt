package com.bookverse.contacts.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.ui.utils.rememberHapticClick
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search

/** Search state and focus remain owned by the destination. */
data class AppBarSearchAction(val label: String, val onClick: () -> Unit, val feedback: Boolean = false)

@Composable
internal fun AppBarSearchEntry(action: AppBarSearchAction) {
    val onClick = if (action.feedback) rememberHapticClick(action.onClick) else action.onClick
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant
    SearchFieldSurface(
        legacyColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Lucide.Search, contentDescription = null, tint = hintColor, modifier = Modifier.size(20.dp))
            Text(action.label, color = hintColor, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
