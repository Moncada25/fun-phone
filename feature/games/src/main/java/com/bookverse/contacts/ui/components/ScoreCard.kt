package com.bookverse.contacts.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ScoreCardEntry(
    val title: String,
    val value: String
)

@Composable
fun ScoreCard(
    entries: List<ScoreCardEntry>,
    modifier: Modifier = Modifier
) {
    require(entries.isNotEmpty()) { "ScoreCard requires at least one entry" }
    require(entries.size <= 3) { "ScoreCard supports up to three entries" }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = if (entries.size == 1) {
                Arrangement.Center
            } else {
                Arrangement.SpaceBetween
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            entries.forEachIndexed { index, entry ->
                val alignment = when {
                    entries.size == 1 -> Alignment.CenterHorizontally
                    index == 0 -> Alignment.Start
                    index == entries.lastIndex -> Alignment.End
                    else -> Alignment.CenterHorizontally
                }
                val columnModifier = if (entries.size == 1) {
                    Modifier
                } else {
                    Modifier.weight(1f)
                }
                Column(
                    modifier = columnModifier,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = alignment
                ) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = entry.value,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}
