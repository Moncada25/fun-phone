package com.bookverse.contacts.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GroupedCardList(items: List<@Composable () -> Unit>) {
    if (items.isEmpty()) return

    val hasMultiple = items.size > 1
    val corner = if (com.bookverse.contacts.ui.theme.LocalAppChromeStyle.current == com.bookverse.contacts.ui.theme.AccentBarStyle.MODERN) 24.dp else 20.dp

    items.forEachIndexed { index, content ->
        val isFirst = index == 0
        val isLast = index == items.lastIndex

        val shape = RoundedCornerShape(
            topStart = if (isFirst) corner else 0.dp,
            topEnd = if (isFirst) corner else 0.dp,
            bottomStart = if (isLast) corner else 0.dp,
            bottomEnd = if (isLast) corner else 0.dp
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 0.dp,
                    bottom = if (isLast || hasMultiple) 2.dp else 0.dp
                ),
            shape = shape,
            color = MaterialTheme.colorScheme.surface
        ) {
            content()
        }
    }
}
