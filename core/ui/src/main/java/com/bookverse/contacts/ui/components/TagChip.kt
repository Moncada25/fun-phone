package com.bookverse.contacts.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.model.ContactTag
import androidx.core.graphics.toColorInt

@Composable
fun TagChip(
    tag: ContactTag,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val tagColor = try {
        Color(tag.colorHex.toColorInt())
    } catch (_: IllegalArgumentException) {
        Color(0xFF2196F3)
    }

    if (onRemove == null && onClick == null) {
        ReadOnlyTagChip(name = tag.name, tagColor = tagColor, modifier = modifier)
        return
    }

    AssistChip(
        onClick = { onClick?.invoke() },
        label = { Text(tag.name) },
        leadingIcon = {
            Canvas(
                modifier = Modifier.size(12.dp),
            ) {
                drawCircle(color = tagColor)
            }
        },
        trailingIcon = if (onRemove != null) {
            {
                IconButton(
                    onClick = { onRemove.invoke() },
                    modifier = Modifier.size(20.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.remove_tag),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        } else null,
        modifier = modifier,
    )
}

@Composable
private fun ReadOnlyTagChip(
    name: String,
    tagColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 32.dp)
                .padding(start = 10.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Canvas(modifier = Modifier.size(10.dp)) {
                drawCircle(color = tagColor)
            }
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
