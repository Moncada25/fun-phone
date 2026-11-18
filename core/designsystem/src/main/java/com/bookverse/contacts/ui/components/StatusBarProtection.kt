package com.bookverse.contacts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity

/** Protects system icons as content scrolls underneath or screens transition. */
@Composable
fun StatusBarProtection(retroColor: Color?) {
    if (retroColor != null) {
        Spacer(
            Modifier.fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(retroColor),
        )
        return
    }

    val color = MaterialTheme.colorScheme.background
    val height = with(LocalDensity.current) { (WindowInsets.statusBars.getTop(this) * 1.2f).toDp() }
    Spacer(
        Modifier.fillMaxWidth().height(height).background(
            Brush.verticalGradient(listOf(color.copy(alpha = 0.9f), color.copy(alpha = 0.35f), Color.Transparent)),
        ),
    )
}
