package com.bookverse.contacts.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

@Composable
internal fun setupActionContainerColor(accentColor: Color): Color {
    val colors = MaterialTheme.colorScheme
    val candidate = listOf(colors.background, colors.onBackground).maxBy {
        setupContrastRatio(it.compositeOver(accentColor), accentColor)
    }
    return if (setupContrastRatio(candidate.compositeOver(accentColor), accentColor) >= 4.5f) accentColor else colors.background
}

@Composable
internal fun setupActionContentColor(accentColor: Color): Color {
    val container = setupActionContainerColor(accentColor)
    val colors = MaterialTheme.colorScheme
    return listOf(colors.background, colors.onBackground).maxBy {
        setupContrastRatio(it.compositeOver(container), container)
    }
}

internal fun setupContrastRatio(first: Color, second: Color): Float {
    val light = maxOf(first.luminance(), second.luminance())
    val dark = minOf(first.luminance(), second.luminance())
    return (light + 0.05f) / (dark + 0.05f)
}
