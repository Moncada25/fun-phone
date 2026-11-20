package com.bookverse.contacts.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide

@Composable
fun OutlinedListCard(
    items: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val outline = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, outline),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, content ->
                content()
                if (index != items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                        color = outline,
                    )
                }
            }
        }
    }
}

@Composable
fun OutlinedListRow(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = Lucide.ChevronRight,
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
