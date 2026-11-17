package com.bookverse.contacts.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

val DefaultAccentSeed = Color(0xFF1E88E5)

@Immutable
data class AccentPalette(
    val seed: Color,
    val onSeed: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
)

private val DefaultAccentPalette = createAccentPalette(
    seed = DefaultAccentSeed,
    background = LightBackgroundColor,
    surface = LightSurfaceColor,
    onBackground = LightTextColor,
    isDark = false,
)

val LocalAccentPalette = staticCompositionLocalOf { DefaultAccentPalette }

@Suppress("UnusedReceiverParameter")
val androidx.compose.material3.MaterialTheme.accentPalette: AccentPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalAccentPalette.current

internal fun createAccentPalette(
    seed: Color,
    background: Color,
    surface: Color,
    onBackground: Color,
    isDark: Boolean,
): AccentPalette {
    val opaqueBackground = background.opaqueOver(Color.Black)
    val opaqueSurface = surface.opaqueOver(opaqueBackground)
    val opaqueSeed = seed.copy(alpha = 1f)
    val contrastTarget = onBackground.opaqueOver(opaqueBackground)
    val primary = ensureContrast(
        color = opaqueSeed,
        backgrounds = listOf(opaqueBackground, opaqueSurface),
        toward = contrastTarget,
    )
    val primaryContainer = seed
        .copy(alpha = if (isDark) DARK_CONTAINER_ALPHA else LIGHT_CONTAINER_ALPHA)
        .compositeOver(opaqueSurface)
        .copy(alpha = 1f)
    val onPrimaryContainer = ensureContrast(
        color = opaqueSeed,
        backgrounds = listOf(primaryContainer),
        toward = contrastTarget,
    )

    return AccentPalette(
        seed = opaqueSeed,
        onSeed = highestContrastContent(opaqueSeed),
        primary = primary,
        onPrimary = highestContrastContent(primary),
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
    )
}

internal fun contrastRatio(
    foreground: Color,
    background: Color,
): Float {
    val opaqueBackground = background.opaqueOver(Color.Black)
    val opaqueForeground = foreground.opaqueOver(opaqueBackground)
    val lighter = maxOf(opaqueForeground.luminance(), opaqueBackground.luminance())
    val darker = minOf(opaqueForeground.luminance(), opaqueBackground.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

internal fun ColorScheme.withAccentPalette(palette: AccentPalette): ColorScheme {
    val opaqueBackground = background.opaqueOver(Color.Black)
    val opaqueSurface = surface.opaqueOver(opaqueBackground)
    val opaqueOnSurface = onSurface.opaqueOver(opaqueSurface)
    val darkSurfaces = opaqueBackground.luminance() < 0.5f
    val neutralVariant = lerp(
        opaqueSurface,
        opaqueOnSurface,
        if (darkSurfaces) DARK_NEUTRAL_VARIANT_FRACTION else LIGHT_NEUTRAL_VARIANT_FRACTION,
    ).copy(alpha = 1f)
    val neutralOutline = lerp(opaqueSurface, opaqueOnSurface, OUTLINE_FRACTION).copy(alpha = 1f)
    val neutralOutlineVariant = lerp(
        opaqueSurface,
        opaqueOnSurface,
        OUTLINE_VARIANT_FRACTION,
    ).copy(alpha = 1f)

    return copy(
        primary = palette.primary,
        onPrimary = palette.onPrimary,
        primaryContainer = palette.primaryContainer,
        onPrimaryContainer = palette.onPrimaryContainer,
        inversePrimary = palette.primary,
        secondary = palette.primary,
        onSecondary = palette.onPrimary,
        secondaryContainer = palette.primaryContainer,
        onSecondaryContainer = palette.onPrimaryContainer,
        tertiary = palette.primary,
        onTertiary = palette.onPrimary,
        tertiaryContainer = palette.primaryContainer,
        onTertiaryContainer = palette.onPrimaryContainer,
        surfaceDim = opaqueBackground,
        surfaceBright = lerp(opaqueSurface, opaqueOnSurface, 0.08f),
        surfaceContainerLowest = opaqueBackground,
        surfaceContainerLow = lerp(opaqueBackground, opaqueSurface, 0.65f),
        surfaceContainer = opaqueSurface,
        surfaceContainerHigh = lerp(opaqueSurface, opaqueOnSurface, 0.04f),
        surfaceContainerHighest = lerp(opaqueSurface, opaqueOnSurface, 0.08f),
        surfaceVariant = neutralVariant,
        onSurfaceVariant = opaqueOnSurface,
        outline = neutralOutline,
        outlineVariant = neutralOutlineVariant,
        inverseSurface = opaqueOnSurface,
        inverseOnSurface = opaqueSurface,
        surfaceTint = Color.Transparent,
    )
}

private fun ensureContrast(
    color: Color,
    backgrounds: List<Color>,
    toward: Color,
): Color {
    if (backgrounds.all { contrastRatio(color, it) >= MIN_CONTENT_CONTRAST }) return color

    var low = 0f
    var high = 1f
    repeat(CONTRAST_SEARCH_STEPS) {
        val fraction = (low + high) / 2f
        val candidate = lerp(color, toward, fraction).copy(alpha = 1f)
        if (backgrounds.all { contrastRatio(candidate, it) >= MIN_CONTENT_CONTRAST }) {
            high = fraction
        } else {
            low = fraction
        }
    }
    return lerp(color, toward, high).copy(alpha = 1f)
}

internal fun highestContrastContent(background: Color): Color {
    val blackContrast = contrastRatio(Color.Black, background)
    val whiteContrast = contrastRatio(Color.White, background)
    return if (blackContrast >= whiteContrast) Color.Black else Color.White
}

private fun Color.opaqueOver(background: Color): Color =
    if (alpha >= 1f) this else compositeOver(background).copy(alpha = 1f)

private const val MIN_CONTENT_CONTRAST = 4.5f
private const val LIGHT_CONTAINER_ALPHA = 0.12f
private const val DARK_CONTAINER_ALPHA = 0.18f
private const val CONTRAST_SEARCH_STEPS = 24
private const val LIGHT_NEUTRAL_VARIANT_FRACTION = 0.06f
private const val DARK_NEUTRAL_VARIANT_FRACTION = 0.12f
private const val OUTLINE_FRACTION = 0.55f
private const val OUTLINE_VARIANT_FRACTION = 0.18f
