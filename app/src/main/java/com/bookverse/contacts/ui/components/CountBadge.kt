package com.bookverse.contacts.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CountBadge(
    count: Int,
    accent: Color,
    content: Color,
    onClick: (() -> Unit)? = null,
    isMinimal: Boolean = false
) {
    val modifier = when {
        onClick == null -> Modifier
        isMinimal -> Modifier.clip(RoundedCornerShape(16.dp)).clickable { onClick() }
        else -> Modifier.clickable { onClick() }.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
    }
    Box(modifier, contentAlignment = Alignment.Center) {
    Surface(color = accent, shape = RoundedCornerShape(16.dp)) {
        Text(
            text = count.toString(),
            color = content,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = if (isMinimal) 2.dp else 4.dp)
        )
    }
    }
}
