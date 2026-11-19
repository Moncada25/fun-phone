package com.bookverse.contacts.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.lerp

@Composable
fun rememberAnimatedColorScheme(
    target: ColorScheme,
    key: Any,
    durationMs: Int,
    snapKey: Any? = null,
): ColorScheme {
    val progress = remember { Animatable(1f) }
    val from = remember { mutableStateOf(target) }
    val to = remember { mutableStateOf(target) }
    val appliedKey = remember { mutableStateOf(key) }
    val appliedSnapKey = remember { mutableStateOf(snapKey) }
    LaunchedEffect(key) {
        if (appliedKey.value == key) return@LaunchedEffect
        appliedKey.value = key
        val snap = snapKey != null && appliedSnapKey.value != snapKey
        appliedSnapKey.value = snapKey
        if (durationMs <= 0 || snap) {
            from.value = target
            to.value = target
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        from.value = from.value.lerpTo(to.value, progress.value)
        to.value = target
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMs, easing = FastOutSlowInEasing))
    }
    val fraction = progress.value
    return if (fraction >= 1f) to.value else from.value.lerpTo(to.value, fraction)
}

internal fun ColorScheme.lerpTo(target: ColorScheme, fraction: Float): ColorScheme = target.copy(
    primary = lerp(primary, target.primary, fraction),
    onPrimary = lerp(onPrimary, target.onPrimary, fraction),
    primaryContainer = lerp(primaryContainer, target.primaryContainer, fraction),
    onPrimaryContainer = lerp(onPrimaryContainer, target.onPrimaryContainer, fraction),
    inversePrimary = lerp(inversePrimary, target.inversePrimary, fraction),
    secondary = lerp(secondary, target.secondary, fraction),
    onSecondary = lerp(onSecondary, target.onSecondary, fraction),
    secondaryContainer = lerp(secondaryContainer, target.secondaryContainer, fraction),
    onSecondaryContainer = lerp(onSecondaryContainer, target.onSecondaryContainer, fraction),
    tertiary = lerp(tertiary, target.tertiary, fraction),
    onTertiary = lerp(onTertiary, target.onTertiary, fraction),
    tertiaryContainer = lerp(tertiaryContainer, target.tertiaryContainer, fraction),
    onTertiaryContainer = lerp(onTertiaryContainer, target.onTertiaryContainer, fraction),
    background = lerp(background, target.background, fraction),
    onBackground = lerp(onBackground, target.onBackground, fraction),
    surface = lerp(surface, target.surface, fraction),
    onSurface = lerp(onSurface, target.onSurface, fraction),
    surfaceVariant = lerp(surfaceVariant, target.surfaceVariant, fraction),
    onSurfaceVariant = lerp(onSurfaceVariant, target.onSurfaceVariant, fraction),
    surfaceTint = lerp(surfaceTint, target.surfaceTint, fraction),
    inverseSurface = lerp(inverseSurface, target.inverseSurface, fraction),
    inverseOnSurface = lerp(inverseOnSurface, target.inverseOnSurface, fraction),
    error = lerp(error, target.error, fraction),
    onError = lerp(onError, target.onError, fraction),
    errorContainer = lerp(errorContainer, target.errorContainer, fraction),
    onErrorContainer = lerp(onErrorContainer, target.onErrorContainer, fraction),
    outline = lerp(outline, target.outline, fraction),
    outlineVariant = lerp(outlineVariant, target.outlineVariant, fraction),
    scrim = lerp(scrim, target.scrim, fraction),
    surfaceBright = lerp(surfaceBright, target.surfaceBright, fraction),
    surfaceDim = lerp(surfaceDim, target.surfaceDim, fraction),
    surfaceContainer = lerp(surfaceContainer, target.surfaceContainer, fraction),
    surfaceContainerHigh = lerp(surfaceContainerHigh, target.surfaceContainerHigh, fraction),
    surfaceContainerHighest = lerp(surfaceContainerHighest, target.surfaceContainerHighest, fraction),
    surfaceContainerLow = lerp(surfaceContainerLow, target.surfaceContainerLow, fraction),
    surfaceContainerLowest = lerp(surfaceContainerLowest, target.surfaceContainerLowest, fraction),
)
