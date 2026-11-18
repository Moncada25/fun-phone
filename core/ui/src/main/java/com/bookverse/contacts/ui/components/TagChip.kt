package com.bookverse.contacts.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        enabled = onClick != null || onRemove != null,
    )
}
