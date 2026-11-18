package com.bookverse.contacts.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
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
fun TagFilterBar(
    tags: List<ContactTag>,
    selectedTagIds: Set<Long>,
    onTagToggle: (Long) -> Unit,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = tags.isNotEmpty(),
        modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background),
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            if (selectedTagIds.isNotEmpty()) {
                item {
                    FilterChip(
                        selected = false,
                        onClick = onClearFilter,
                         label = { Text(stringResource(R.string.all_filter)) },
                    )
                }
            }

            items(
                items = tags,
                key = { it.id },
            ) { tag ->
                val tagColor = try {
                    Color(tag.colorHex.toColorInt())
                } catch (_: IllegalArgumentException) {
                    Color(0xFF2196F3)
                }

                val isSelected = selectedTagIds.contains(tag.id)

                FilterChip(
                    selected = isSelected,
                    onClick = { onTagToggle(tag.id) },
                    label = { Text(tag.name) },
                    leadingIcon = {
                        Canvas(
                            modifier = Modifier.size(12.dp),
                        ) {
                            drawCircle(color = tagColor)
                        }
                    },
                    colors = if (isSelected) {
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tagColor.copy(alpha = 0.3f),
                        )
                    } else {
                        FilterChipDefaults.filterChipColors()
                    },
                )
            }
        }
    }
}
