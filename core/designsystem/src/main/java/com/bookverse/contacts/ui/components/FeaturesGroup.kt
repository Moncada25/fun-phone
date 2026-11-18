package com.bookverse.contacts.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class FeatureItem(
    val label: String,
    val icon: ImageVector,
)

@Composable
fun FeaturesGroup(
    features: List<FeatureItem>,
    modifier: Modifier = Modifier
) {
    var highlightedIndex by remember { mutableIntStateOf(-1) }

    FeaturesGrid(
        features = features,
        columns = 4,
        highlightedIndex = highlightedIndex,
        onItemClick = { index ->
            highlightedIndex = if (highlightedIndex == index) -1 else index
        },
        modifier = modifier
    )
}

@Composable
private fun FeaturesGrid(
    features: List<FeatureItem>,
    columns: Int,
    highlightedIndex: Int,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = (features.size + columns - 1) / columns
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(rows) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(columns) { columnIndex ->
                    val featureIndex = rowIndex * columns + columnIndex
                    if (featureIndex < features.size) {
                        val feature = features[featureIndex]
                        FeatureOrb(
                            icon = feature.icon,
                            description = feature.label,
                            isSelected = highlightedIndex == featureIndex,
                            onClick = { onItemClick(featureIndex) },
                            modifier = Modifier.weight(1f, fill = true)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeatureOrb(
    icon: ImageVector,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val tooltipState = rememberTooltipState(isPersistent = true)

    LaunchedEffect(isSelected) {
        if (isSelected) {
            tooltipState.show()
        } else {
            tooltipState.dismiss()
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TooltipBox(
            state = tooltipState,
            focusable = false,
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = {
                PlainTooltip(
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .background(
                            color = colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            colorScheme.primaryContainer
                        } else {
                            colorScheme.onSurface.copy(alpha = 0.06f)
                        },
                    )
                    .border(
                        BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) {
                                colorScheme.primary
                            } else {
                                colorScheme.outline.copy(alpha = 0.5f)
                            },
                        ),
                        CircleShape
                    )
                    .clickable { onClick() }
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = description,
                    tint = if (isSelected) {
                        colorScheme.onPrimaryContainer
                    } else {
                        colorScheme.onSurface
                    },
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}
